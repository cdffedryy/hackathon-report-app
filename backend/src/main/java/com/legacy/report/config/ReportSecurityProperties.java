package com.legacy.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "report.security")
public class ReportSecurityProperties {

    private final List<String> allowedTables;
    private final List<String> forbiddenTokens;
    private final int maxLength;

    public ReportSecurityProperties(
            @DefaultValue("customer,transaction,merchant,product,orders,order_items,department,employee,report_config,report_run,report_audit_event") List<String> allowedTables,
            @DefaultValue(";,--,/*,*/,INSERT,UPDATE,DELETE,DROP,ALTER,TRUNCATE,MERGE,EXEC,CALL,GRANT,REVOKE") List<String> forbiddenTokens,
            @DefaultValue("10000") int maxLength) {
        this.allowedTables = new ArrayList<>(allowedTables);
        this.forbiddenTokens = new ArrayList<>(forbiddenTokens);
        this.maxLength = maxLength;
    }

    public List<String> getAllowedTables() {
        return allowedTables;
    }

    public List<String> getForbiddenTokens() {
        return forbiddenTokens;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
