package com.example.railtracker.service;

import com.example.railtracker.dto.AlertDto;
import com.example.railtracker.dto.AlertRequest;
import java.util.List;

public interface AlertService {

    AlertDto setAlertThreshold(String username, AlertRequest request);

    List<AlertDto> getActiveAlerts(String username);

    void removeAlert(String username, Long alertId);
}
