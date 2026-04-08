package com.legacy.report.controller;

import com.legacy.report.model.ReportRun;
import com.legacy.report.service.ReportExcelExportService;
import com.legacy.report.service.ReportRunService;
import com.legacy.report.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportRunController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportRunService reportRunService;

    @MockBean
    private ReportExcelExportService reportExcelExportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void updateManualSnapshot_shouldReturnUpdatedRun() throws Exception {
        ReportRun run = new ReportRun();
        run.setId(5L);
        run.setStatus("Generated");
        run.setManualNote("note");

        when(reportRunService.updateManualSnapshot(eq(5L), any(), eq("note"))).thenReturn(run);

        String body = "{" +
                "\"snapshot\": {\"value\": 1}," +
                "\"note\": \"note\"" +
                "}";

        mockMvc.perform(put("/api/report-runs/{id}/manual-snapshot", 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(reportRunService).updateManualSnapshot(eq(5L), any(), eq("note"));
    }

    @Test
    void updateManualSnapshot_shouldReturnBadRequestWhenSnapshotMissing() throws Exception {
        mockMvc.perform(put("/api/report-runs/{id}/manual-snapshot", 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(reportRunService, never()).updateManualSnapshot(any(), any(), any());
    }

    @Test
    void updateManualSnapshot_shouldReturnBadRequestWhenSnapshotMissing2() throws Exception {
        String body = "{" +
                "\"manualNote\": \"note\"" +
                "}";

        mockMvc.perform(put("/api/report-runs/{id}/manual-snapshot", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(reportRunService, never()).updateManualSnapshot(any(), any(), any());
    }

    @Test
    void closeRun_shouldInvokeServiceWithReason() throws Exception {
        ReportRun run = new ReportRun();
        run.setId(7L);
        run.setStatus("Closed");

        when(reportRunService.closeRun(eq(7L), eq("撤回"))).thenReturn(run);

        mockMvc.perform(post("/api/report-runs/{id}/close", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"撤回\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.status").value("Closed"));

        verify(reportRunService).closeRun(eq(7L), eq("撤回"));
    }

    @Test
    void reopenRun_shouldInvokeServiceWithoutBody() throws Exception {
        ReportRun run = new ReportRun();
        run.setId(9L);
        run.setStatus("Generated");

        when(reportRunService.reopenRun(eq(9L), isNull())).thenReturn(run);

        mockMvc.perform(post("/api/report-runs/{id}/reopen", 9L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L))
                .andExpect(jsonPath("$.status").value("Generated"));

        verify(reportRunService).reopenRun(eq(9L), isNull());
    }
}
