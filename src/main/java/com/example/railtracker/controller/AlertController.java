package com.example.railtracker.controller;

import com.example.railtracker.dto.AlertDto;
import com.example.railtracker.dto.AlertRequest;
import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/threshold")
    public ResponseEntity<ApiResponse<AlertDto>> setAlertThreshold(
            Principal principal,
            @RequestBody AlertRequest request) {
        AlertDto alert = alertService.setAlertThreshold(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDto>>> getActiveAlerts(Principal principal) {
        List<AlertDto> alerts = alertService.getActiveAlerts(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> removeAlert(
            Principal principal,
            @PathVariable Long id) {
        alertService.removeAlert(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Alert settings removed successfully"));
    }
}
