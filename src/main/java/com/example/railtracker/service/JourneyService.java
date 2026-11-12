package com.example.railtracker.service;

import com.example.railtracker.dto.JourneyOptionDto;
import com.example.railtracker.dto.JourneyOverlayDTO;
import java.util.List;

public interface JourneyService {

    default List<JourneyOptionDto> planJourney(String sourceCode, String destinationCode, String date) {
        return planJourney(sourceCode, destinationCode, date, false);
    }

    List<JourneyOptionDto> planJourney(String sourceCode, String destinationCode, String date, boolean nearby);

    JourneyOverlayDTO getJourneyOverlay(Object requestBody);
}
