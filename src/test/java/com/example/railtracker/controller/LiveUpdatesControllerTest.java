package com.example.railtracker.controller;

import com.example.railtracker.dto.TrainLiveStatusDto;
import com.example.railtracker.service.TrainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Collections;
import org.springframework.security.test.context.support.WithMockUser;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveUpdatesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrainService trainService;

    @Test
    @WithMockUser(username = "passenger1")
    void testLiveUpdatesStreamMediaType() throws Exception {
        TrainLiveStatusDto statusDto = new TrainLiveStatusDto(
                "12760", "Charminar", "RUNNING", "WL", "Warangal", "BZA", "Vijayawada",
                0, 17.9689, 79.5941, 60.0, 120.0, 45.0, "Diverged near Warangal",
                LocalDateTime.now(), Collections.emptyList()
        );
        when(trainService.getTrainLiveStatus("12760")).thenReturn(statusDto);

        mockMvc.perform(get("/api/v1/live-updates/12760"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
