package com.legacy.report.service;

import com.legacy.report.dao.ReportDao;
import com.legacy.report.dto.DashboardItemDto;
import com.legacy.report.model.Report;
import com.legacy.report.model.ReportRun;
import com.legacy.report.model.ReportSchedule;
import com.legacy.report.repository.ReportRunRepository;
import com.legacy.report.repository.ReportScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduleService.class);

    @Autowired
    private ReportScheduleRepository reportScheduleRepository;

    @Autowired
    private ReportRunRepository reportRunRepository;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private CurrentUserService currentUserService;

    public List<DashboardItemDto> getDashboardItems() {
        currentUserService.getCurrentUserOrThrow();

        List<ReportSchedule> schedules = reportScheduleRepository.findByEnabledTrue();

        // Build a lookup of report_config by id (only non-deleted)
        List<Report> allReports = reportDao.findAll();
        Map<Long, Report> reportMap = allReports.stream()
                .collect(Collectors.toMap(Report::getId, r -> r, (a, b) -> a));

        LocalDate today = LocalDate.now();

        return schedules.stream()
                .filter(s -> reportMap.containsKey(s.getReportId()))
                .map(s -> buildDashboardItem(s, reportMap.get(s.getReportId()), today))
                .sorted(dashboardSorter())
                .collect(Collectors.toList());
    }

    private DashboardItemDto buildDashboardItem(ReportSchedule schedule, Report report, LocalDate today) {
        DashboardItemDto dto = new DashboardItemDto();
        dto.setScheduleId(schedule.getId());
        dto.setReportId(report.getId());
        dto.setReportName(report.getName());
        dto.setReportDescription(report.getDescription());
        dto.setFrequency(schedule.getFrequency());
        dto.setCurrentDeadline(schedule.getCurrentDeadline());
        dto.setPeriodStart(schedule.getPeriodStart());

        int daysRemaining = (int) ChronoUnit.DAYS.between(today, schedule.getCurrentDeadline());
        dto.setDaysRemaining(daysRemaining);

        // Find the latest run for this report within the current period
        LocalDateTime periodStartTime = schedule.getPeriodStart().atStartOfDay();
        List<ReportRun> runsInPeriod = reportRunRepository
                .findByReportIdAndGeneratedAtAfterOrderByGeneratedAtDesc(
                        report.getId(), periodStartTime);

        ReportRun latestRun = runsInPeriod.isEmpty() ? null : runsInPeriod.get(0);

        String submissionStatus;
        if (latestRun == null) {
            submissionStatus = "NOT_SUBMITTED";
        } else {
            dto.setLatestRunId(latestRun.getId());
            switch (latestRun.getStatus()) {
                case "Generated":
                    submissionStatus = "GENERATED";
                    break;
                case "Submitted":
                    submissionStatus = "SUBMITTED";
                    break;
                case "Approved":
                    submissionStatus = "APPROVED";
                    break;
                case "Rejected":
                    submissionStatus = "REJECTED";
                    break;
                default:
                    submissionStatus = "NOT_SUBMITTED";
            }
        }
        dto.setSubmissionStatus(submissionStatus);

        // Determine urgency level
        String urgencyLevel;
        if ("APPROVED".equals(submissionStatus)) {
            urgencyLevel = "COMPLETED";
        } else if (daysRemaining < 0) {
            urgencyLevel = "OVERDUE";
        } else if (daysRemaining <= 7) {
            urgencyLevel = "APPROACHING";
        } else {
            urgencyLevel = "NORMAL";
        }
        dto.setUrgencyLevel(urgencyLevel);

        return dto;
    }

    private Comparator<DashboardItemDto> dashboardSorter() {
        return (a, b) -> {
            int urgencyA = urgencyOrder(a.getUrgencyLevel());
            int urgencyB = urgencyOrder(b.getUrgencyLevel());
            if (urgencyA != urgencyB) {
                return Integer.compare(urgencyA, urgencyB);
            }
            return Integer.compare(a.getDaysRemaining(), b.getDaysRemaining());
        };
    }

    private int urgencyOrder(String level) {
        switch (level) {
            case "OVERDUE": return 0;
            case "APPROACHING": return 1;
            case "NORMAL": return 2;
            case "COMPLETED": return 3;
            default: return 4;
        }
    }

    @Transactional
    public ReportSchedule createSchedule(Long reportId, String frequency, LocalDate currentDeadline) {
        currentUserService.getCurrentUserOrThrow();

        Report report = reportDao.findById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }

        if (reportScheduleRepository.findByReportId(reportId).isPresent()) {
            throw new RuntimeException("该报表已存在报送计划");
        }

        ReportSchedule schedule = new ReportSchedule();
        schedule.setReportId(reportId);
        schedule.setFrequency(frequency);
        schedule.setCurrentDeadline(currentDeadline);
        schedule.setPeriodStart(calculatePeriodStart(currentDeadline, frequency));
        schedule.setEnabled(true);

        logger.info("event=schedule_create reportId={} frequency={} deadline={}", reportId, frequency, currentDeadline);
        return reportScheduleRepository.save(schedule);
    }

    @Transactional
    public ReportSchedule updateSchedule(Long scheduleId, String frequency, LocalDate currentDeadline, Boolean enabled) {
        currentUserService.getCurrentUserOrThrow();

        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("报送计划不存在"));

        if (frequency != null) {
            schedule.setFrequency(frequency);
        }
        if (currentDeadline != null) {
            schedule.setCurrentDeadline(currentDeadline);
            schedule.setPeriodStart(calculatePeriodStart(currentDeadline, schedule.getFrequency()));
        }
        if (enabled != null) {
            schedule.setEnabled(enabled);
        }

        logger.info("event=schedule_update scheduleId={} frequency={} deadline={} enabled={}",
                scheduleId, schedule.getFrequency(), schedule.getCurrentDeadline(), schedule.getEnabled());
        return reportScheduleRepository.save(schedule);
    }

    @Transactional
    public void rollForwardIfNeeded(Long reportId) {
        reportScheduleRepository.findByReportId(reportId).ifPresent(schedule -> {
            if ("ONCE".equals(schedule.getFrequency())) {
                return;
            }
            LocalDate oldDeadline = schedule.getCurrentDeadline();
            LocalDate newDeadline = calculateNextDeadline(oldDeadline, schedule.getFrequency());
            schedule.setPeriodStart(oldDeadline.plusDays(1));
            schedule.setCurrentDeadline(newDeadline);
            reportScheduleRepository.save(schedule);
            logger.info("event=schedule_roll_forward reportId={} oldDeadline={} newDeadline={}",
                    reportId, oldDeadline, newDeadline);
        });
    }

    private LocalDate calculateNextDeadline(LocalDate current, String frequency) {
        switch (frequency) {
            case "DAILY": return current.plusDays(1);
            case "WEEKLY": return current.plusWeeks(1);
            case "MONTHLY": return current.plusMonths(1);
            case "QUARTERLY": return current.plusMonths(3);
            case "YEARLY": return current.plusYears(1);
            default: return current;
        }
    }

    private LocalDate calculatePeriodStart(LocalDate deadline, String frequency) {
        switch (frequency) {
            case "DAILY": return deadline;
            case "WEEKLY": return deadline.minusWeeks(1).plusDays(1);
            case "MONTHLY": return deadline.minusMonths(1).plusDays(1);
            case "QUARTERLY": return deadline.minusMonths(3).plusDays(1);
            case "YEARLY": return deadline.minusYears(1).plusDays(1);
            case "ONCE": return deadline.minusMonths(1);
            default: return deadline.minusMonths(1);
        }
    }
}
