package com.legacy.report.security;

import com.legacy.report.config.ReportSecurityProperties;
import com.legacy.report.exception.ReportSecurityException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportSqlValidator {

    private final ReportSecurityProperties securityProperties;

    public ReportSqlValidator(ReportSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public void validate(String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new ReportSecurityException("SQL 不能为空");
        }
        String normalized = sql.trim().toUpperCase();
        if (!normalized.startsWith("SELECT")) {
            throw new ReportSecurityException("只允许执行 SELECT 查询");
        }
        if (sql.length() > securityProperties.getMaxLength()) {
            throw new ReportSecurityException("SQL 超过最大长度限制");
        }
        securityProperties.getForbiddenTokens().forEach(token -> {
            if (normalized.contains(token.toUpperCase())) {
                throw new ReportSecurityException("SQL 包含禁止的关键词: " + token);
            }
        });
        boolean allowedTable = securityProperties.getAllowedTables().stream()
                .anyMatch(table -> normalized.contains(table.toUpperCase()));
        if (!allowedTable) {
            throw new ReportSecurityException("SQL 未引用任何允许的表名");
        }
    }
}
