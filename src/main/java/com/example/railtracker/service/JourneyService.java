package com.example.railtracker.service;

import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.dto.JourneyOverlayDTO;
import java.util.List;

public interface JourneyService {

    List<JourneyOptionDto> planJourney(String sourceCode, String destinationCode, String date);

    JourneyOverlayDTO getJourneyOverlay(Object requestBody);
}
