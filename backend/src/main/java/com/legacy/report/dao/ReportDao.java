package com.legacy.report.dao;

import com.legacy.report.model.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class ReportDao {
    
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    @Autowired
    public ReportDao(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbc = jdbcTemplate;
        this.namedJdbc = namedParameterJdbcTemplate;
    }
    
    // 业务逻辑在 DAO 里
    public List<Report> findAll() {
        // 没有注释，不知道这个SQL是干什么的
        String sql = "SELECT id, name, sql, description FROM report_config WHERE is_deleted = 0";
        return jdbc.query(sql, new ReportMapper());
    }
    
    public Report findById(Long id) {
        String sql = "SELECT id, name, sql, description FROM report_config WHERE id = :id AND is_deleted = 0";
        Map<String, Object> params = Map.of("id", id);
        List<Report> results = namedJdbc.query(sql, params, new ReportMapper());

        return results.isEmpty() ? null : results.get(0);
    }
    
    // 直接执行传入的SQL，没有任何安全检查
    public List<Map<String, Object>> executeSql(String sql) {
        return executeSql(sql, Map.of());
    }

    public List<Map<String, Object>> executeSql(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return jdbc.queryForList(sql);
        }
        return namedJdbc.queryForList(sql, params);
    }
    
    public void save(Report report) {
        // 使用命名参数
        String sql = "INSERT INTO report_config (name, sql, description) VALUES (:name, :sql, :description)";
        Map<String, Object> params = Map.of(
                "name", report.getName(),
                "sql", report.getSql(),
                "description", report.getDescription()
        );
        namedJdbc.update(sql, params);
    }
    
    // update 和 delete 都没有
    
    private static class ReportMapper implements RowMapper<Report> {
        @Override
        public Report mapRow(ResultSet rs, int rowNum) throws SQLException {
            Report r = new Report();
            r.setId(rs.getLong("id"));
            r.setName(rs.getString("name"));
            r.setSql(rs.getString("sql"));
            r.setDescription(rs.getString("description"));
            return r;
        }
    }
}