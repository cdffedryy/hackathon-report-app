package com.legacy.report.service;

import com.legacy.report.dao.ReportDao;
import com.legacy.report.model.Report;
import com.legacy.report.security.ReportParameterBuilder;
import com.legacy.report.security.ReportSqlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    
    @Autowired
    private ReportDao reportDao;
    
    @Autowired
    private ReportSqlValidator reportSqlValidator;

    @Autowired
    private ReportParameterBuilder reportParameterBuilder;

    // 业务逻辑全部堆在这里，一个方法几百行
    public List<Report> getAllReports() {
        return reportDao.findAll();
    }
    
    public Report getReportById(Long id) {
        return reportDao.findById(id);
    }
    
    // 直接执行SQL，没有任何校验，这是严重的安全漏洞
    public List<Map<String, Object>> runReport(String sql) {
        return runReport(sql, Collections.emptyMap());
    }

    public List<Map<String, Object>> runReport(String sql, Map<String, Object> params) {
        reportSqlValidator.validate(sql);
        if (CollectionUtils.isEmpty(params)) {
            return reportDao.executeSql(sql);
        }
        return reportDao.executeSql(sql, params);
    }
    
    // 没有参数校验，没有异常处理
    public void createReport(Report report) {
        if (report.getName() == null || report.getName().isEmpty()) {
            throw new RuntimeException("名称不能为空");
        }
        if (report.getSql() == null || report.getSql().isEmpty()) {
            throw new RuntimeException("SQL不能为空");
        }
        reportSqlValidator.validate(report.getSql());
        reportDao.save(report);
    }
    
    // 复杂的业务逻辑全部在这个方法里，没有拆分
    public Map<String, Object> generateReport(Long reportId, Object params) {
        Report report = reportDao.findById(reportId);
        if (report == null) {
            throw new RuntimeException("报表不存在");
        }
        
        ReportParameterBuilder.BuiltQuery builtQuery = reportParameterBuilder.build(reportId, report.getSql(), params);
        List<Map<String, Object>> data = runReport(builtQuery.sql(), builtQuery.parameters());
        
        // 没有计算逻辑，直接返回原始数据
        return Map.of(
            "reportName", report.getName(),
            "data", data,
            "count", data.size()
        );
    }
}