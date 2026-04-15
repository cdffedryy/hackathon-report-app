package com.legacy.report.dto;

import java.time.LocalDate;

public class DashboardItemDto {

    private Long scheduleId;
    private Long reportId;
    private String reportName;
    private String reportDescription;
    private String frequency;
    private LocalDate currentDeadline;
    private LocalDate periodStart;
    private int daysRemaining;
    private String submissionStatus;
    private String urgencyLevel;
    private Long latestRunId;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getReportDescription() { return reportDescription; }
    public void setReportDescription(String reportDescription) { this.reportDescription = reportDescription; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public LocalDate getCurrentDeadline() { return currentDeadline; }
    public void setCurrentDeadline(LocalDate currentDeadline) { this.currentDeadline = currentDeadline; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(String submissionStatus) { this.submissionStatus = submissionStatus; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public Long getLatestRunId() { return latestRunId; }
    public void setLatestRunId(Long latestRunId) { this.latestRunId = latestRunId; }
}
