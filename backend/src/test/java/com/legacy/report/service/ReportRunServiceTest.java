package com.legacy.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacy.report.model.ReportRun;
import com.legacy.report.model.User;
import com.legacy.report.repository.ReportRunRepository;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportRunServiceTest {

    @Mock
    private ReportService reportService;

    @Mock
    private ReportRunRepository reportRunRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReportRunService reportRunService;

    @Test
    void updateManualSnapshot_shouldPersistForMakerGeneratedRun() {
        User maker = new User();
        maker.setUsername("maker1");
        maker.setRole("MAKER");

        ReportRun run = new ReportRun();
        run.setId(10L);
        run.setReportId(2L);
        run.setStatus("Generated");
        run.setMakerUsername("maker1");
        run.setGeneratedAt(LocalDateTime.now());

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(maker);
        doNothing().when(currentUserService).requireRole(any(User.class), eq("MAKER"));
        when(reportRunRepository.findById(10L)).thenReturn(Optional.of(run));
        when(reportRunRepository.save(any(ReportRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportRun updated = reportRunService.updateManualSnapshot(10L, Map.of("value", 1), "需要修正数值");

        assertEquals("{\"value\":1}", updated.getResultSnapshot());
        assertEquals("需要修正数值", updated.getManualNote());
        assertTrue(Boolean.TRUE.equals(updated.getHasManualEdits()));
        assertNotNull(updated.getManualEditedAt());
        verify(auditService).recordEvent(eq(10L), eq(2L), eq("maker1"), eq("MAKER"), eq("ManualEdited"), eq("需要修正数值"));
    }

    @Test
    void updateManualSnapshot_shouldRejectWhenNotOwner() {
        User maker = new User();
        maker.setUsername("maker1");
        maker.setRole("MAKER");

        ReportRun run = new ReportRun();
        run.setId(11L);
        run.setReportId(3L);
        run.setStatus("Generated");
        run.setMakerUsername("otherMaker");

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(maker);
        doNothing().when(currentUserService).requireRole(any(User.class), eq("MAKER"));
        when(reportRunRepository.findById(11L)).thenReturn(Optional.of(run));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reportRunService.updateManualSnapshot(11L, Map.of("k", "v"), null));
        assertTrue(ex.getMessage().contains("只能编辑由当前 Maker 自己生成的报表运行实例"));
        verify(reportRunRepository, never()).save(any());
    }

    @Test
    void closeRun_shouldCloseGeneratedRun() {
        User maker = new User();
        maker.setUsername("maker1");
        maker.setRole("MAKER");

        ReportRun run = new ReportRun();
        run.setId(20L);
        run.setReportId(6L);
        run.setStatus("Generated");
        run.setMakerUsername("maker1");

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(maker);
        doNothing().when(currentUserService).requireRole(any(User.class), eq("MAKER"));
        when(reportRunRepository.findById(20L)).thenReturn(Optional.of(run));
        when(reportRunRepository.save(any(ReportRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportRun closed = reportRunService.closeRun(20L, "有误");

        assertEquals("Closed", closed.getStatus());
        assertNotNull(closed.getClosedAt());
        assertEquals("有误", closed.getClosedReason());
        assertNull(closed.getSubmittedAt());
        assertNull(closed.getCheckerUsername());
        verify(auditService).recordEvent(eq(20L), eq(6L), eq("maker1"), eq("MAKER"), eq("Closed"), eq("有误"));
    }

    @Test
    void closeRun_shouldRejectApprovedRun() {
        User maker = new User();
        maker.setUsername("maker1");
        maker.setRole("MAKER");

        ReportRun run = new ReportRun();
        run.setId(21L);
        run.setReportId(7L);
        run.setStatus("Approved");
        run.setMakerUsername("maker1");

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(maker);
        doNothing().when(currentUserService).requireRole(any(User.class), eq("MAKER"));
        when(reportRunRepository.findById(21L)).thenReturn(Optional.of(run));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reportRunService.closeRun(21L, null));
        assertEquals("已完成的报表运行无法关闭", ex.getReason());
        verify(reportRunRepository, never()).save(any());
    }

    @Test
    void reopenRun_shouldRestoreGeneratedStatus() {
        User maker = new User();
        maker.setUsername("maker1");
        maker.setRole("MAKER");

        ReportRun run = new ReportRun();
        run.setId(22L);
        run.setReportId(8L);
        run.setStatus("Closed");
        run.setMakerUsername("maker1");
        run.setClosedAt(LocalDateTime.now());
        run.setClosedReason("参数错误");

        when(currentUserService.getCurrentUserOrThrow()).thenReturn(maker);
        doNothing().when(currentUserService).requireRole(any(User.class), eq("MAKER"));
        when(reportRunRepository.findById(22L)).thenReturn(Optional.of(run));
        when(reportRunRepository.save(any(ReportRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportRun reopened = reportRunService.reopenRun(22L, "已修复");

        assertEquals("Generated", reopened.getStatus());
        assertNotNull(reopened.getReopenedAt());
        assertNull(reopened.getClosedAt());
        assertNull(reopened.getClosedReason());
        assertNull(reopened.getSubmittedAt());
        verify(auditService).recordEvent(eq(22L), eq(8L), eq("maker1"), eq("MAKER"), eq("Reopened"), eq("已修复"));
    }
}
