package com.example.railtracker.service;

import com.example.railtracker.entity.PasswordResetOtp;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.exception.ResourceNotFoundException;
import com.example.railtracker.repository.PasswordResetOtpRepository;
import com.example.railtracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService brevoEmailService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository passwordResetOtpRepository,
            PasswordEncoder passwordEncoder,
            BrevoEmailService brevoEmailService) {
        this.userRepository = userRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoEmailService = brevoEmailService;
    }

    @Transactional
    public void sendResetOtp(String email) {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + cleanEmail));

        // Generate 6-digit OTP
        int number = secureRandom.nextInt(900000) + 100000;
        String otp = String.valueOf(number);

        // Expire in 15 minutes
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        PasswordResetOtp otpEntity = new PasswordResetOtp(cleanEmail, otp, expiry);
        passwordResetOtpRepository.save(otpEntity);

        log.info("[PasswordResetService] Generated reset OTP for user: {} ({})", user.getUsername(), cleanEmail);

        // Send email via Brevo
        boolean sent = brevoEmailService.sendPasswordResetOtpEmail(user.getEmail(), user.getUsername(), otp);
        if (!sent) {
            log.warn("[PasswordResetService] Email dispatch returned false. (If in development without API key, OTP was logged above).");
        }
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + cleanEmail));

        PasswordResetOtp otpEntity = passwordResetOtpRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(cleanEmail)
                .orElseThrow(() -> new BadRequestException("No active password reset request found. Please request a new OTP."));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The OTP has expired. Please request a new OTP.");
        }

        if (!otpEntity.getOtp().trim().equals(otp.trim())) {
            throw new BadRequestException("Invalid verification code. Please check the OTP sent to your email.");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark OTP as used
        otpEntity.setUsed(true);
        passwordResetOtpRepository.save(otpEntity);

        log.info("[PasswordResetService] Password successfully reset for user: {}", user.getUsername());

        // Notify user of successful change
        try {
            brevoEmailService.sendPasswordResetSuccessEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.warn("[PasswordResetService] Could not send password reset success confirmation email: {}", e.getMessage());
        }
    }
}
