package com.legacy.report.repository;

import com.legacy.report.model.ReportRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {

    List<ReportRun> findByMakerUsernameOrderByGeneratedAtDesc(String makerUsername);

    List<ReportRun> findByMakerUsernameAndReportIdOrderByGeneratedAtDesc(String makerUsername, Long reportId);

    List<ReportRun> findByStatusOrderBySubmittedAtAsc(String status);

    List<ReportRun> findByCheckerUsernameOrderByDecidedAtDesc(String checkerUsername);

    List<ReportRun> findByReportIdAndGeneratedAtAfterOrderByGeneratedAtDesc(Long reportId, LocalDateTime generatedAtAfter);
}
