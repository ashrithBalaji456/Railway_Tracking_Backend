package com.example.railtracker.controller;

import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.service.JourneyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalTime;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JourneyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JourneyService journeyService;

    @Test
    void testPlanJourneyOptionsSuccess() throws Exception {
        JourneyOptionDto direct = new JourneyOptionDto(
                true, "12760", "Charminar", "BZA", "Vijayawada", LocalTime.of(22, 0),
                "NDLS", "New Delhi", LocalTime.of(18, 0),
                null, null, null, null, null, null, 0, null, 1200,
                "mon,tue,wed", null
        );

        when(journeyService.planJourney("BZA", "NDLS", null)).thenReturn(Arrays.asList(direct));

        mockMvc.perform(get("/api/v1/journey/plan")
                .param("source", "BZA")
                .param("destination", "NDLS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].direct").value(true))
                .andExpect(jsonPath("$.data[0].firstTrainNumber").value("12760"));

        verify(journeyService, times(1)).planJourney("BZA", "NDLS", null);
    }
}
