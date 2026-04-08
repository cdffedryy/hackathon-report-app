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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
