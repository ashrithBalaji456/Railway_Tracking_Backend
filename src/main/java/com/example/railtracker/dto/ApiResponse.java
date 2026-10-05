package com.example.railtracker.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String timestamp,
    String traceId
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            true,
            message,
            data,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            UUID.randomUUID().toString()
        );
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request processed successfully", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
            false,
            message,
            null,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            UUID.randomUUID().toString()
        );
    }
    
    public static <T> ApiResponse<T> error(String message, String traceId) {
        return new ApiResponse<>(
            false,
            message,
            null,
            LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            traceId
        );
    }
}
