package com.legacy.report.controller;

import com.legacy.report.dto.DashboardItemDto;
import com.legacy.report.model.ReportSchedule;
import com.legacy.report.service.ReportScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report-schedules")
@CrossOrigin(origins = "*")
public class ReportScheduleController {

    @Autowired
    private ReportScheduleService reportScheduleService;

    @GetMapping("/dashboard")
    public List<DashboardItemDto> getDashboard() {
        return reportScheduleService.getDashboardItems();
    }

    @PostMapping
    public ResponseEntity<ReportSchedule> createSchedule(@RequestBody Map<String, Object> request) {
        Long reportId = Long.valueOf(request.get("reportId").toString());
        String frequency = (String) request.get("frequency");
        LocalDate currentDeadline = LocalDate.parse((String) request.get("currentDeadline"));

        ReportSchedule schedule = reportScheduleService.createSchedule(reportId, frequency, currentDeadline);
        return new ResponseEntity<>(schedule, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ReportSchedule updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String frequency = request.containsKey("frequency") ? (String) request.get("frequency") : null;
        LocalDate currentDeadline = request.containsKey("currentDeadline")
                ? LocalDate.parse((String) request.get("currentDeadline"))
                : null;
        Boolean enabled = request.containsKey("enabled") ? (Boolean) request.get("enabled") : null;

        return reportScheduleService.updateSchedule(id, frequency, currentDeadline, enabled);
    }
}
