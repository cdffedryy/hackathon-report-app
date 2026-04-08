package com.legacy.report.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacy.report.config.ReportParameterTemplateProperties;
import com.legacy.report.config.ReportParameterTemplateProperties.Filter;
import com.legacy.report.exception.ReportSecurityException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReportParameterBuilder {

    private final ReportParameterTemplateProperties templateProperties;
    private final ObjectMapper objectMapper;

    public ReportParameterBuilder(ReportParameterTemplateProperties templateProperties, ObjectMapper objectMapper) {
        this.templateProperties = templateProperties;
        this.objectMapper = objectMapper;
    }

    public BuiltQuery build(Long reportId, String baseSql, Object rawParams) {
        if (rawParams == null || (rawParams instanceof String s && !StringUtils.hasText(s))) {
            return BuiltQuery.of(baseSql, Collections.emptyMap());
        }

        Map<String, Object> paramsMap = convertToMap(rawParams);
        if (paramsMap.isEmpty()) {
            return BuiltQuery.of(baseSql, Collections.emptyMap());
        }

        ReportParameterTemplateProperties.Template template = templateProperties.findTemplate(reportId)
                .orElseThrow(() -> new ReportSecurityException("该报表不支持参数过滤"));

        Map<String, Filter> filterMap = template.getFilters().stream()
                .collect(Collectors.toMap(f -> f.getName().toLowerCase(Locale.ROOT), f -> f));

        List<String> conditions = new ArrayList<>();
        Map<String, Object> namedParams = new HashMap<>();

        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            String filterName = entry.getKey();
            Filter filter = filterMap.get(filterName.toLowerCase(Locale.ROOT));
            if (filter == null) {
                throw new ReportSecurityException("不支持的过滤条件: " + filterName);
            }
            List<Object> normalizedValues = normalizeValues(entry.getValue());
            if (normalizedValues.isEmpty()) {
                continue;
            }
            if (!CollectionUtils.isEmpty(filter.getAllowedValues())) {
                for (Object v : normalizedValues) {
                    String text = String.valueOf(v);
                    boolean allowed = filter.getAllowedValues().stream()
                            .anyMatch(allowedValue -> allowedValue.equalsIgnoreCase(text));
                    if (!allowed) {
                        throw new ReportSecurityException("参数 " + filterName + " 的取值不允许: " + text);
                    }
                }
            }
            String paramKey = "p_" + filter.getName().toLowerCase(Locale.ROOT);
            switch (filter.getOperator()) {
                case EQUALS -> {
                    namedParams.put(paramKey, normalizedValues.get(0));
                    conditions.add(filter.getColumn() + " = :" + paramKey);
                }
                case LIKE -> {
                    namedParams.put(paramKey, normalizedValues.get(0));
                    conditions.add(filter.getColumn() + " LIKE :" + paramKey);
                }
                case IN -> {
                    namedParams.put(paramKey, normalizedValues);
                    conditions.add(filter.getColumn() + " IN (:" + paramKey + ")");
                }
                default -> throw new ReportSecurityException("未知的过滤操作符");
            }
        }

        if (conditions.isEmpty()) {
            return BuiltQuery.of(baseSql, Collections.emptyMap());
        }

        String finalSql = appendConditions(baseSql, conditions);
        return BuiltQuery.of(finalSql, namedParams);
    }

    private Map<String, Object> convertToMap(Object rawParams) {
        if (rawParams instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((k, v) -> {
                if (k != null) {
                    result.put(String.valueOf(k), v);
                }
            });
            return result;
        }
        if (rawParams instanceof String json) {
            try {
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (IOException e) {
                throw new ReportSecurityException("无法解析参数 JSON", e);
            }
        }
        throw new ReportSecurityException("参数格式不支持");
    }

    private List<Object> normalizeValues(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(v -> v != null && StringUtils.hasText(String.valueOf(v)))
                    .map(v -> v instanceof String ? ((String) v).trim() : v)
                    .collect(Collectors.toList());
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object element = java.lang.reflect.Array.get(value, i);
                if (element != null) {
                    list.add(element);
                }
            }
            return list;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        return List.of(text);
    }

    private String appendConditions(String baseSql, List<String> conditions) {
        String trimmed = baseSql.trim();
        boolean hasWhere = trimmed.toUpperCase(Locale.ROOT).contains(" WHERE ");
        StringBuilder builder = new StringBuilder(trimmed);
        builder.append(hasWhere ? " AND " : " WHERE ");
        builder.append(String.join(" AND ", conditions));
        return builder.toString();
    }

    public record BuiltQuery(String sql, Map<String, Object> parameters) {
        private static BuiltQuery of(String sql, Map<String, Object> params) {
            return new BuiltQuery(sql, params == null ? Collections.emptyMap() : params);
        }
    }
}
