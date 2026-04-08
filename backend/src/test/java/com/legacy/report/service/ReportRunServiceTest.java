package com.legacy.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legacy.report.model.ReportRun;
import com.legacy.report.model.User;
import com.legacy.report.repository.ReportRunRepository;
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
}
