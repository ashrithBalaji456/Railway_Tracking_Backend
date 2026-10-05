package com.example.railtracker.controller;

import com.example.railtracker.dto.AlertDto;
import com.example.railtracker.dto.AlertRequest;
import com.example.railtracker.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Test
    @WithMockUser(username = "passenger1")
    void testSetAlertThreshold() throws Exception {
        AlertRequest req = new AlertRequest("12760", "Charminar Express", 30);
        AlertDto res = new AlertDto(1L, "12760", "Charminar Express", 30, true, false, 5);

        when(alertService.setAlertThreshold("passenger1", req)).thenReturn(res);

        mockMvc.perform(post("/api/v1/alerts/threshold")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.thresholdMinutes").value(30))
                .andExpect(jsonPath("$.data.triggered").value(false));
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testGetActiveAlerts() throws Exception {
        AlertDto a1 = new AlertDto(1L, "12760", "Charminar Express", 15, true, true, 20); // triggered delay
        AlertDto a2 = new AlertDto(2L, "12626", "Kerala Express", 30, true, false, 10);

        when(alertService.getActiveAlerts("passenger1")).thenReturn(Arrays.asList(a1, a2));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].triggered").value(true))
                .andExpect(jsonPath("$.data[1].triggered").value(false));
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testRemoveAlert() throws Exception {
        doNothing().when(alertService).removeAlert("passenger1", 1L);

        mockMvc.perform(delete("/api/v1/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Alert settings removed successfully"));

        verify(alertService, times(1)).removeAlert("passenger1", 1L);
    }
}
