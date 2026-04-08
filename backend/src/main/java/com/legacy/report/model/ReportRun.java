package com.legacy.report.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_run")
public class ReportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // 对应的报表模板 ID（来自 report_config 表）
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    // 生成时的报表名称快照，方便审计和查询
    @Column(name = "report_name", nullable = false)
    private String reportName;

    // 报表运行状态：Generated / Submitted / Approved / Rejected
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    // Maker 用户名
    @Column(name = "maker_username", nullable = false)
    private String makerUsername;

    // Checker 用户名（只有在审批后才有值）
    @Column(name = "checker_username")
    private String checkerUsername;

    // 生成时间（执行报表成功时）
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    // 提交审批时间
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // 审批决策时间（通过或拒绝）
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // 本次执行使用的参数（JSON 或简单字符串）
    @Lob
    @Column(name = "parameters_json")
    private String parametersJson;

    // 结果快照（可选，先用 JSON 字符串存储，后续可以演进为外部存储引用）
    @Lob
    @Column(name = "result_snapshot")
    private String resultSnapshot;

    @Lob
    @Column(name = "manual_note")
    private String manualNote;

    @Column(name = "manual_edited_at")
    private LocalDateTime manualEditedAt;

    @Column(name = "has_manual_edits")
    private Boolean hasManualEdits = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMakerUsername() {
        return makerUsername;
    }

    public void setMakerUsername(String makerUsername) {
        this.makerUsername = makerUsername;
    }

    public String getCheckerUsername() {
        return checkerUsername;
    }

    public void setCheckerUsername(String checkerUsername) {
        this.checkerUsername = checkerUsername;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public String getResultSnapshot() {
        return resultSnapshot;
    }

    public void setResultSnapshot(String resultSnapshot) {
        this.resultSnapshot = resultSnapshot;
    }

    public String getManualNote() {
        return manualNote;
    }

    public void setManualNote(String manualNote) {
        this.manualNote = manualNote;
    }

    public LocalDateTime getManualEditedAt() {
        return manualEditedAt;
    }

    public void setManualEditedAt(LocalDateTime manualEditedAt) {
        this.manualEditedAt = manualEditedAt;
    }

    public Boolean getHasManualEdits() {
        return hasManualEdits;
    }

    public void setHasManualEdits(Boolean hasManualEdits) {
        this.hasManualEdits = hasManualEdits;
    }
}
