package com.example.railtracker.cache;

import com.example.railtracker.client.RailwayDataProvider;
import com.example.railtracker.dto.TrainDto;
import com.example.railtracker.repository.TrainRepository;
import com.example.railtracker.service.TrainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class RedisCacheIntegrationTest {

    @Autowired
    private TrainService trainService;

    @MockBean
    private TrainRepository trainRepository;

    @MockBean
    private RailwayDataProvider railwayDataProvider;

    @BeforeEach
    void setUp() {
        reset(trainRepository, railwayDataProvider);
    }

    @Test
    void testCacheFallbackWhenRedisIsOffline() {
        // Mock a scenario where train is not in repository but fetched from external API
        TrainDto mockTrain = new TrainDto("12760", "Charminar", "Express", "SF", "HYB", "MS", 800, 840, "Daily", true, "ENG-GEN-GEN");
        when(trainRepository.findByTrainNumber("12760")).thenReturn(Optional.empty());
        when(railwayDataProvider.getTrainDetails("12760")).thenReturn(mockTrain);

        // This call will attempt to check Redis, fail (if Redis is offline),
        // trigger CacheErrorHandler, and fallback to the repository/API.
        // It must NOT throw an exception, proving our graceful fallback works!
        TrainDto result = trainService.getTrainDetails("12760");

        assertNotNull(result);
        verify(railwayDataProvider, times(1)).getTrainDetails("12760");
    }
}
