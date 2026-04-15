package com.legacy.report.repository;

import com.legacy.report.model.ReportSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByEnabledTrue();

    Optional<ReportSchedule> findByReportId(Long reportId);
}
