package com.legacy.report.service;

import com.legacy.report.dao.ReportDao;
import com.legacy.report.dto.DashboardItemDto;
import com.legacy.report.model.Report;
import com.legacy.report.model.ReportRun;
import com.legacy.report.model.ReportSchedule;
import com.legacy.report.model.User;
import com.legacy.report.repository.ReportRunRepository;
import com.legacy.report.repository.ReportScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceTest {

    @Mock
    private ReportScheduleRepository reportScheduleRepository;

    @Mock
    private ReportRunRepository reportRunRepository;

    @Mock
    private ReportDao reportDao;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ReportScheduleService service;

    private User testUser;
    private Report testReport;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("maker1");
        testUser.setRole("MAKER");

        testReport = new Report();
        testReport.setId(1L);
        testReport.setName("Test Report");
        testReport.setDescription("Test Description");
    }

    @Test
    void getDashboardItems_returnsOverdueFirst() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);

        ReportSchedule overdueSchedule = makeSchedule(1L, 1L, "MONTHLY",
                LocalDate.now().minusDays(2), LocalDate.now().minusDays(32));
        ReportSchedule approachingSchedule = makeSchedule(2L, 1L, "WEEKLY",
                LocalDate.now().plusDays(3), LocalDate.now().minusDays(4));

        when(reportScheduleRepository.findByEnabledTrue())
                .thenReturn(List.of(approachingSchedule, overdueSchedule));
        when(reportDao.findAll()).thenReturn(List.of(testReport));
        when(reportRunRepository.findByReportIdAndGeneratedAtAfterOrderByGeneratedAtDesc(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        List<DashboardItemDto> result = service.getDashboardItems();

        assertEquals(2, result.size());
        assertEquals("OVERDUE", result.get(0).getUrgencyLevel());
        assertEquals("APPROACHING", result.get(1).getUrgencyLevel());
    }

    @Test
    void getDashboardItems_approvedRunMarkedCompleted() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);

        ReportSchedule schedule = makeSchedule(1L, 1L, "MONTHLY",
                LocalDate.now().plusDays(5), LocalDate.now().minusDays(25));
        when(reportScheduleRepository.findByEnabledTrue()).thenReturn(List.of(schedule));
        when(reportDao.findAll()).thenReturn(List.of(testReport));

        ReportRun approvedRun = new ReportRun();
        approvedRun.setId(10L);
        approvedRun.setStatus("Approved");
        approvedRun.setGeneratedAt(LocalDateTime.now().minusDays(1));
        when(reportRunRepository.findByReportIdAndGeneratedAtAfterOrderByGeneratedAtDesc(anyLong(), any()))
                .thenReturn(List.of(approvedRun));

        List<DashboardItemDto> result = service.getDashboardItems();

        assertEquals(1, result.size());
        assertEquals("COMPLETED", result.get(0).getUrgencyLevel());
        assertEquals("APPROVED", result.get(0).getSubmissionStatus());
        assertEquals(10L, result.get(0).getLatestRunId());
    }

    @Test
    void getDashboardItems_deletedReportFilteredOut() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);

        ReportSchedule schedule = makeSchedule(1L, 99L, "MONTHLY",
                LocalDate.now().plusDays(5), LocalDate.now().minusDays(25));
        when(reportScheduleRepository.findByEnabledTrue()).thenReturn(List.of(schedule));
        when(reportDao.findAll()).thenReturn(List.of(testReport)); // testReport has id=1, not 99

        List<DashboardItemDto> result = service.getDashboardItems();

        assertTrue(result.isEmpty());
    }

    @Test
    void getDashboardItems_deadlineTodayIsApproaching() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);

        ReportSchedule schedule = makeSchedule(1L, 1L, "MONTHLY",
                LocalDate.now(), LocalDate.now().minusDays(30));
        when(reportScheduleRepository.findByEnabledTrue()).thenReturn(List.of(schedule));
        when(reportDao.findAll()).thenReturn(List.of(testReport));
        when(reportRunRepository.findByReportIdAndGeneratedAtAfterOrderByGeneratedAtDesc(anyLong(), any()))
                .thenReturn(Collections.emptyList());

        List<DashboardItemDto> result = service.getDashboardItems();

        assertEquals(1, result.size());
        assertEquals("APPROACHING", result.get(0).getUrgencyLevel());
        assertEquals(0, result.get(0).getDaysRemaining());
    }

    @Test
    void rollForwardIfNeeded_monthlyAdvancesDeadline() {
        ReportSchedule schedule = makeSchedule(1L, 1L, "MONTHLY",
                LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 1));
        when(reportScheduleRepository.findByReportId(1L)).thenReturn(Optional.of(schedule));
        when(reportScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.rollForwardIfNeeded(1L);

        assertEquals(LocalDate.of(2026, 5, 30), schedule.getCurrentDeadline());
        assertEquals(LocalDate.of(2026, 5, 1), schedule.getPeriodStart());
    }

    @Test
    void rollForwardIfNeeded_onceDoesNotAdvance() {
        ReportSchedule schedule = makeSchedule(1L, 1L, "ONCE",
                LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 1));
        when(reportScheduleRepository.findByReportId(1L)).thenReturn(Optional.of(schedule));

        service.rollForwardIfNeeded(1L);

        assertEquals(LocalDate.of(2026, 4, 30), schedule.getCurrentDeadline());
        verify(reportScheduleRepository, never()).save(any());
    }

    @Test
    void rollForwardIfNeeded_noScheduleDoesNothing() {
        when(reportScheduleRepository.findByReportId(99L)).thenReturn(Optional.empty());

        service.rollForwardIfNeeded(99L);

        verify(reportScheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_success() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);
        when(reportDao.findById(1L)).thenReturn(testReport);
        when(reportScheduleRepository.findByReportId(1L)).thenReturn(Optional.empty());
        when(reportScheduleRepository.save(any())).thenAnswer(i -> {
            ReportSchedule s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        ReportSchedule result = service.createSchedule(1L, "MONTHLY", LocalDate.of(2026, 5, 31));

        assertNotNull(result);
        assertEquals(1L, result.getReportId());
        assertEquals("MONTHLY", result.getFrequency());
        assertEquals(LocalDate.of(2026, 5, 31), result.getCurrentDeadline());
    }

    @Test
    void createSchedule_duplicateThrowsException() {
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(testUser);
        when(reportDao.findById(1L)).thenReturn(testReport);
        when(reportScheduleRepository.findByReportId(1L))
                .thenReturn(Optional.of(new ReportSchedule()));

        assertThrows(RuntimeException.class,
                () -> service.createSchedule(1L, "MONTHLY", LocalDate.of(2026, 5, 31)));
    }

    private ReportSchedule makeSchedule(Long id, Long reportId, String frequency,
                                         LocalDate deadline, LocalDate periodStart) {
        ReportSchedule s = new ReportSchedule();
        s.setId(id);
        s.setReportId(reportId);
        s.setFrequency(frequency);
        s.setCurrentDeadline(deadline);
        s.setPeriodStart(periodStart);
        s.setEnabled(true);
        return s;
    }
}
