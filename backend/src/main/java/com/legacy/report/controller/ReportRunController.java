package com.legacy.report.controller;

import com.legacy.report.model.ReportAuditEvent;
import com.legacy.report.model.ReportRun;
import com.legacy.report.service.ReportExcelExportService;
import com.legacy.report.service.ReportRunService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report-runs")
@CrossOrigin(origins = "*")
public class ReportRunController {

    @Autowired
    private ReportRunService reportRunService;

    @Autowired
    private ReportExcelExportService reportExcelExportService;

    @PostMapping("/{id}/submit")
    public void submit(@PathVariable Long id) {
        reportRunService.submitRun(id);
    }

    @PutMapping("/{id}/manual-snapshot")
    public ReportRun updateManualSnapshot(@PathVariable Long id, @RequestBody ManualSnapshotRequest request) {
        if (request == null || request.getSnapshot() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshot 字段必填");
        }
        return reportRunService.updateManualSnapshot(id, request.getSnapshot(), request.getNote());
    }

    @PostMapping("/{id}/decision")
    public void decide(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String decision = request.get("decision");
        String comment = request.get("comment");

        if (decision == null) {
            throw new RuntimeException("decision 字段必填，取值为 APPROVED 或 REJECTED");
        }

        boolean approve;
        if ("APPROVED".equalsIgnoreCase(decision) || "APPROVE".equalsIgnoreCase(decision)) {
            approve = true;
        } else if ("REJECTED".equalsIgnoreCase(decision) || "REJECT".equalsIgnoreCase(decision)) {
            approve = false;
        } else {
            throw new RuntimeException("decision 必须为 APPROVED/REJECTED");
        }

        reportRunService.decideRun(id, approve, comment);
    }

    @GetMapping("/my-latest")
    public ReportRun getMyLatest(@RequestParam("reportId") Long reportId) {
        return reportRunService.getLatestRunForCurrentMaker(reportId);
    }

    @GetMapping("/my-runs")
    public List<ReportRun> getMyRuns() {
        return reportRunService.getRunsForCurrentMaker();
    }

    @GetMapping("/submitted")
    public List<ReportRun> getSubmitted() {
        return reportRunService.getSubmittedRunsForChecker();
    }

    @GetMapping("/checker/history")
    public List<ReportRun> getCheckerHistory() {
        return reportRunService.getHistoryRunsForCurrentChecker();
    }

    @GetMapping("/{id}/audit")
    public List<ReportAuditEvent> getAudit(@PathVariable Long id) {
        return reportRunService.getAuditEventsForRun(id);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportRun(@PathVariable Long id) {
        byte[] body = reportExcelExportService.exportByRunId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "report-run-" + id + ".xlsx");

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    public static class ManualSnapshotRequest {
        private Object snapshot;
        private String note;

        public Object getSnapshot() {
            return snapshot;
        }

        public void setSnapshot(Object snapshot) {
            this.snapshot = snapshot;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
