package com.example.railtracker.service;

import com.example.railtracker.dto.RailAiRequest;
import com.example.railtracker.dto.RailAiResponse;

public interface RailAiService {

    RailAiResponse getAiResponse(String username, RailAiRequest request);
}
