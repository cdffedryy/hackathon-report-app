package com.legacy.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigurationProperties(prefix = "report.parameters")
public class ReportParameterTemplateProperties {

    private List<Template> templates = new ArrayList<>();

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public Optional<Template> findTemplate(Long reportId) {
        if (reportId == null) {
            return Optional.empty();
        }
        return templates.stream().filter(t -> reportId.equals(t.getReportId())).findFirst();
    }

    public static class Template {
        private Long reportId;
        private List<Filter> filters = new ArrayList<>();

        public Long getReportId() {
            return reportId;
        }

        public void setReportId(Long reportId) {
            this.reportId = reportId;
        }

        public List<Filter> getFilters() {
            return filters;
        }

        public void setFilters(List<Filter> filters) {
            this.filters = filters;
        }
    }

    public static class Filter {
        private String name;
        private String column;
        private FilterOperator operator = FilterOperator.EQUALS;
        private List<String> allowedValues = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public FilterOperator getOperator() {
            return operator;
        }

        public void setOperator(FilterOperator operator) {
            this.operator = operator;
        }

        public List<String> getAllowedValues() {
            return allowedValues;
        }

        public void setAllowedValues(List<String> allowedValues) {
            this.allowedValues = allowedValues;
        }
    }

    public enum FilterOperator {
        EQUALS,
        IN,
        LIKE
    }
}
