package com.example.railtracker.controller;

import com.example.railtracker.dto.RailAiRequest;
import com.example.railtracker.dto.RailAiResponse;
import com.example.railtracker.service.RailAiService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RailAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RailAiService railAiService;

    @Test
    void testChatRequiresAuthentication() throws Exception {
        RailAiRequest req = new RailAiRequest("Is train delayed?", "12760");
        mockMvc.perform(post("/api/v1/railai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testChatAuthorizedSuccess() throws Exception {
        RailAiRequest req = new RailAiRequest("Is train delayed?", "12760");
        RailAiResponse res = new RailAiResponse("Yes, the train is late by 30 mins.");

        when(railAiService.getAiResponse("passenger1", req)).thenReturn(res);

        mockMvc.perform(post("/api/v1/railai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response").value("Yes, the train is late by 30 mins."));

        verify(railAiService, times(1)).getAiResponse("passenger1", req);
    }
}
