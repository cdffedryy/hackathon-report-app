package com.legacy.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacy.report.model.Report;
import com.legacy.report.model.ReportAuditEvent;
import com.legacy.report.model.ReportRun;
import com.legacy.report.model.User;
import com.legacy.report.repository.ReportAuditEventRepository;
import com.legacy.report.repository.ReportRunRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReportRunService {

    private static final Logger logger = LoggerFactory.getLogger(ReportRunService.class);
    private static final int MANUAL_NOTE_MAX_LENGTH = 1000;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRunRepository reportRunRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportAuditEventRepository reportAuditEventRepository;

    private Counter generatedCounter;
    private Counter submittedCounter;
    private Counter approvedCounter;
    private Counter rejectedCounter;
    private Timer approvalDurationTimer;

    @Autowired(required = false)
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            this.generatedCounter = Counter.builder("report_run_generated_total")
                    .description("Number of report runs generated")
                    .register(meterRegistry);
            this.submittedCounter = Counter.builder("report_run_submitted_total")
                    .description("Number of report runs submitted for approval")
                    .register(meterRegistry);
            this.approvedCounter = Counter.builder("report_run_approved_total")
                    .description("Number of report runs approved")
                    .register(meterRegistry);
            this.rejectedCounter = Counter.builder("report_run_rejected_total")
                    .description("Number of report runs rejected")
                    .register(meterRegistry);
            this.approvalDurationTimer = Timer.builder("report_run_approval_duration_seconds")
                    .description("Time between report generation and final decision in seconds")
                    .register(meterRegistry);
        }
    }

    @Transactional
    public List<Map<String, Object>> executeReportWithRun(Long reportId) {
        // 获取当前用户并校验 Maker 角色
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "MAKER");

        Report report = reportService.getReportById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        logger.info("event=report_run_execute_start reportId={} maker={}", reportId, currentUser.getUsername());

        // 先执行报表，保持原有行为
        List<Map<String, Object>> data = reportService.runReport(report.getSql());

        // 创建 ReportRun 记录
        ReportRun run = new ReportRun();
        run.setReportId(report.getId());
        run.setReportName(report.getName());
        run.setStatus("Generated");
        run.setMakerUsername(currentUser.getUsername());
        run.setGeneratedAt(LocalDateTime.now());
        run.setParametersJson(null); // 当前 execute 接口没有参数，后续可扩展

        try {
            String snapshot = objectMapper.writeValueAsString(data);
            run.setResultSnapshot(snapshot);
            run.setManualNote(null);
            run.setManualEditedAt(null);
            run.setHasManualEdits(Boolean.FALSE);
        } catch (JsonProcessingException e) {
            // 快照失败不影响主流程
            run.setResultSnapshot(null);
        }

        ReportRun saved = reportRunRepository.save(run);

        // 记录审计事件
        auditService.recordEvent(
                saved.getId(),
                report.getId(),
                currentUser.getUsername(),
                currentUser.getRole(),
                "Generated",
                null
        );

        if (generatedCounter != null) {
            generatedCounter.increment();
        }

        logger.info("event=report_run_execute_success runId={} reportId={} maker={}",
                saved.getId(), saved.getReportId(), currentUser.getUsername());

        return data;
    }

    @Transactional
    public ReportRun submitRun(Long runId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "MAKER");

        ReportRun run = reportRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("报表运行实例不存在"));

        if (!"Generated".equals(run.getStatus())) {
            throw new RuntimeException("只能提交 Generated 状态的报表运行实例");
        }

        if (!currentUser.getUsername().equals(run.getMakerUsername())) {
            throw new RuntimeException("只能提交由当前 Maker 自己生成的报表运行实例");
        }

        run.setStatus("Submitted");
        run.setSubmittedAt(LocalDateTime.now());

        ReportRun saved = reportRunRepository.save(run);

        auditService.recordEvent(
                saved.getId(),
                saved.getReportId(),
                currentUser.getUsername(),
                currentUser.getRole(),
                "Submitted",
                null
        );

        if (submittedCounter != null) {
            submittedCounter.increment();
        }

        logger.info("event=report_run_submit_success runId={} reportId={} maker={}",
                saved.getId(), saved.getReportId(), currentUser.getUsername());

        return saved;
    }

    @Transactional
    public ReportRun updateManualSnapshot(Long runId, Object snapshotPayload, String note) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "MAKER");

        ReportRun run = reportRunRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报表运行实例不存在"));

        if (!"Generated".equals(run.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只能编辑 Generated 状态的报表运行实例");
        }
        if (!currentUser.getUsername().equals(run.getMakerUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能编辑由当前 Maker 自己生成的报表运行实例");
        }
        if (snapshotPayload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手工快照不能为空");
        }

        String normalizedSnapshot = normalizeSnapshot(snapshotPayload);
        String trimmedNote = StringUtils.hasText(note) ? note.trim() : null;
        if (trimmedNote != null && trimmedNote.length() > MANUAL_NOTE_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "备注长度超出限制");
        }

        run.setResultSnapshot(normalizedSnapshot);
        run.setManualNote(trimmedNote);
        run.setManualEditedAt(LocalDateTime.now());
        run.setHasManualEdits(Boolean.TRUE);

        ReportRun saved;
        try {
            saved = reportRunRepository.save(run);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "手工快照已被其他会话更新，请刷新后重试", ex);
        }

        auditService.recordEvent(
                saved.getId(),
                saved.getReportId(),
                currentUser.getUsername(),
                currentUser.getRole(),
                "ManualEdited",
                trimmedNote
        );

        return saved;
    }

    @Transactional
    public ReportRun decideRun(Long runId, boolean approve, String comment) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "CHECKER");

        ReportRun run = reportRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("报表运行实例不存在"));

        if (!"Submitted".equals(run.getStatus())) {
            throw new RuntimeException("只能对 Submitted 状态的报表运行实例进行审批");
        }

        if (!approve) {
            if (comment == null || comment.trim().isEmpty()) {
                throw new RuntimeException("拒绝审批时必须填写 comment");
            }
        }

        run.setCheckerUsername(currentUser.getUsername());
        run.setDecidedAt(LocalDateTime.now());
        run.setStatus(approve ? "Approved" : "Rejected");

        ReportRun saved = reportRunRepository.save(run);

        auditService.recordEvent(
                saved.getId(),
                saved.getReportId(),
                currentUser.getUsername(),
                currentUser.getRole(),
                approve ? "Approved" : "Rejected",
                comment
        );

        if (approve) {
            if (approvedCounter != null) {
                approvedCounter.increment();
            }
        } else {
            if (rejectedCounter != null) {
                rejectedCounter.increment();
            }
        }

        if (approvalDurationTimer != null
                && run.getGeneratedAt() != null
                && run.getDecidedAt() != null) {
            approvalDurationTimer.record(Duration.between(run.getGeneratedAt(), run.getDecidedAt()));
        }

        logger.info("event=report_run_decision_success runId={} reportId={} checker={} decision={} commentPresent={}",
                saved.getId(), saved.getReportId(), currentUser.getUsername(),
                approve ? "Approved" : "Rejected",
                comment != null && !comment.trim().isEmpty());

        return saved;
    }

    public ReportRun getLatestRunForCurrentMaker(Long reportId) {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "MAKER");

        List<ReportRun> runs = reportRunRepository
                .findByMakerUsernameAndReportIdOrderByGeneratedAtDesc(currentUser.getUsername(), reportId);

        if (runs.isEmpty()) {
            throw new RuntimeException("当前用户在该报表下没有执行记录");
        }

        return runs.get(0);
    }

    public List<ReportRun> getRunsForCurrentMaker() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "MAKER");

        return reportRunRepository.findByMakerUsernameOrderByGeneratedAtDesc(currentUser.getUsername());
    }

    public List<ReportRun> getSubmittedRunsForChecker() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "CHECKER");

        return reportRunRepository.findByStatusOrderBySubmittedAtAsc("Submitted");
    }

    public List<ReportRun> getHistoryRunsForCurrentChecker() {
        User currentUser = currentUserService.getCurrentUserOrThrow();
        currentUserService.requireRole(currentUser, "CHECKER");

        return reportRunRepository.findByCheckerUsernameOrderByDecidedAtDesc(currentUser.getUsername());
    }

    public List<ReportAuditEvent> getAuditEventsForRun(Long reportRunId) {
        // 只要是已登录用户即可查看指定 run 的审计轨迹
        currentUserService.getCurrentUserOrThrow();
        return reportAuditEventRepository.findByReportRunIdOrderByEventTimeAsc(reportRunId);
    }

    private String normalizeSnapshot(Object snapshotPayload) {
        try {
            if (snapshotPayload instanceof String strPayload) {
                if (!StringUtils.hasText(strPayload)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手工快照不能为空");
                }
                JsonNode node = objectMapper.readTree(strPayload);
                return objectMapper.writeValueAsString(node);
            }
            JsonNode node = objectMapper.valueToTree(snapshotPayload);
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手工快照必须是合法 JSON", e);
        }
    }
}
