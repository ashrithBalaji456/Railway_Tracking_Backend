package com.example.railtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Email cannot be blank")
    String email,

    @NotBlank(message = "OTP cannot be blank")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    String otp,

    @NotBlank(message = "New password cannot be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String newPassword
) {}

