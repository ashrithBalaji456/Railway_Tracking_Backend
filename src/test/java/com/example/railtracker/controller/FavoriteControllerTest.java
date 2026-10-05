package com.example.railtracker.controller;

import com.example.railtracker.dto.FavoriteDto;
import com.example.railtracker.dto.FavoriteRequest;
import com.example.railtracker.service.FavoriteService;
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
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FavoriteService favoriteService;

    @Test
    @WithMockUser(username = "passenger1")
    void testAddTrainFavorite() throws Exception {
        FavoriteRequest req = new FavoriteRequest("12760", "Charminar Express");
        FavoriteDto res = new FavoriteDto(1L, "TRAIN", "12760", "Charminar Express");

        when(favoriteService.addFavorite("passenger1", "TRAIN", req)).thenReturn(res);

        mockMvc.perform(post("/api/v1/favorites/train")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.favoriteType").value("TRAIN"))
                .andExpect(jsonPath("$.data.itemCode").value("12760"));
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testAddStationFavorite() throws Exception {
        FavoriteRequest req = new FavoriteRequest("BZA", "Vijayawada");
        FavoriteDto res = new FavoriteDto(2L, "STATION", "BZA", "Vijayawada");

        when(favoriteService.addFavorite("passenger1", "STATION", req)).thenReturn(res);

        mockMvc.perform(post("/api/v1/favorites/station")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.favoriteType").value("STATION"))
                .andExpect(jsonPath("$.data.itemCode").value("BZA"));
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testGetFavorites() throws Exception {
        FavoriteDto t1 = new FavoriteDto(1L, "TRAIN", "12760", "Charminar Express");
        FavoriteDto s1 = new FavoriteDto(2L, "STATION", "BZA", "Vijayawada");

        when(favoriteService.getFavorites("passenger1")).thenReturn(Arrays.asList(t1, s1));

        mockMvc.perform(get("/api/v1/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].favoriteType").value("TRAIN"))
                .andExpect(jsonPath("$.data[1].favoriteType").value("STATION"));
    }

    @Test
    @WithMockUser(username = "passenger1")
    void testRemoveFavorite() throws Exception {
        doNothing().when(favoriteService).removeFavorite("passenger1", 1L);

        mockMvc.perform(delete("/api/v1/favorites/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Favorite item removed successfully"));

        verify(favoriteService, times(1)).removeFavorite("passenger1", 1L);
    }
}
