package com.legacy.report.controller;

import com.legacy.report.model.Report;
import com.legacy.report.service.ReportExcelExportService;
import com.legacy.report.service.ReportRunService;
import com.legacy.report.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReportController {
    
    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportRunService reportRunService;

    @Autowired
    private ReportExcelExportService reportExcelExportService;
    
    // Test endpoint to verify basic functionality
    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "ok", "message", "Backend is working");
    }
    
    // Test database connectivity
    @GetMapping("/test-db")
    public Map<String, Object> testDatabase() {
        try {
            System.out.println("Testing database connection...");
            List<Report> reports = reportService.getAllReports();
            System.out.println("Database test successful, found " + reports.size() + " reports");
            return Map.of("status", "ok", "message", "Database connection working", "count", reports.size());
        } catch (Exception e) {
            System.out.println("Database test failed: " + e.getMessage());
            e.printStackTrace();
            return Map.of("status", "error", "message", "Database connection failed: " + e.getMessage());
        }
    }
    
    // 没有统一的返回格式
    @GetMapping(value = "/reports", produces = "application/json;charset=UTF-8")
    public List<Report> getAllReports() {
        return reportService.getAllReports();
    }
    
    @GetMapping("/reports/{id}")
    public Report getReport(@PathVariable Long id) {
        return reportService.getReportById(id);
    }
    
    // 直接暴露SQL执行接口，这是严重的安全问题
    @PostMapping("/reports/run")
    public List<Map<String, Object>> runReport(@RequestBody Map<String, Object> request) {
        String sql = (String) request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("sql 字段必填");
        }
        Object rawParams = request.get("params");
        Map<String, Object> params = convertToParamMap(rawParams);
        return reportService.runReport(sql, params);
    }
    
    @PostMapping("/reports/generate")
    public Map<String, Object> generateReport(@RequestBody Map<String, Object> request) {
        Long reportId = Long.valueOf(request.get("reportId").toString());
        Object params = request.get("params");
        return reportService.generateReport(reportId, params);
    }
    
    @PostMapping("/reports")
    public void createReport(@RequestBody Report report) {
        reportService.createReport(report);
    }
    
    @PostMapping("/reports/{id}/execute")
    public List<Map<String, Object>> executeReport(@PathVariable Long id) {
        // 通过 ReportRunService 执行报表并创建 ReportRun + 审计事件，
        // 对前端返回值保持不变：仍然返回查询结果列表。
        return reportRunService.executeReportWithRun(id);
    }
    
    @GetMapping("/reports/{id}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long id) {
        byte[] body = reportExcelExportService.exportLatestByReportId(id);

        String filename = URLEncoder.encode("report-" + id + ".xlsx", StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename);

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
    
    // 没有更新和删除的接口

    private Map<String, Object> convertToParamMap(Object rawParams) {
        if (rawParams == null) {
            return Map.of();
        }
        if (rawParams instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue));
        }
        throw new RuntimeException("params 必须为对象");
    }
}