package com.example.railtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrevoEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String apiKey;

    @Value("${brevo.from-email:${BREVO_FROM_EMAIL:support@railtrack.com}}")
    private String fromEmail;

    @Value("${brevo.from-name:${BREVO_FROM_NAME:RailTrack Alerts}}")
    private String fromName;

    public BrevoEmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an email via Brevo's v3 transactional SMTP API.
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("[BrevoEmailService] BREVO_API_KEY is not configured! Simulated email to '{}' with subject: '{}'", toEmail, subject);
            return false;
        }

        try {
            Map<String, Object> payload = new HashMap<>();

            // Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("name", (fromName != null && !fromName.isBlank()) ? fromName : "RailTrack Alerts");
            sender.put("email", (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "support@railtrack.com");
            payload.put("sender", sender);

            // Recipient
            List<Map<String, String>> toList = new ArrayList<>();
            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            if (toName != null && !toName.isBlank()) {
                recipient.put("name", toName);
            }
            toList.add(recipient);
            payload.put("to", toList);

            // Subject & HTML Content
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("api-key", apiKey.trim())
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            log.info("[BrevoEmailService] Sending transactional email to '{}' via Brevo API...", toEmail);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[BrevoEmailService] Email successfully sent to '{}'. Status: {}", toEmail, response.statusCode());
                return true;
            } else {
                log.error("[BrevoEmailService] Failed to send email to '{}'. Brevo Status: {}, Response: {}",
                        toEmail, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[BrevoEmailService] Exception while sending email to '{}': {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends a branded Password Reset OTP email.
     */
    public boolean sendPasswordResetOtpEmail(String toEmail, String username, String otp) {
        String subject = "🚆 Your RailTrack Password Reset OTP: " + otp;
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 540px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #4f46e5 0%%, #3b82f6 100%%); padding: 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
                .content { padding: 32px 24px; }
                .greeting { font-size: 16px; color: #e2e8f0; margin-bottom: 16px; }
                .message { font-size: 14px; color: #94a3b8; line-height: 1.6; margin-bottom: 24px; }
                .otp-box { background-color: #0f172a; border: 2px dashed #6366f1; border-radius: 8px; padding: 18px; text-align: center; margin-bottom: 24px; }
                .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 6px; color: #818cf8; font-family: 'Courier New', monospace; }
                .expiry { font-size: 13px; color: #f59e0b; margin-top: 8px; font-weight: 500; }
                .warning { font-size: 12px; color: #64748b; line-height: 1.5; border-top: 1px solid #334155; padding-top: 20px; }
                .footer { background-color: #0f172a; padding: 16px 24px; text-align: center; font-size: 12px; color: #64748b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🚆 RailTrack India</h1>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  <div class="message">
                    We received a request to reset your password for your <strong>RailTrack</strong> account. Use the one-time verification code below to complete your password reset:
                  </div>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                    <div class="expiry">⏱️ Valid for 15 minutes</div>
                  </div>
                  <div class="message" style="margin-bottom: 16px;">
                    Enter this code on the password reset screen along with your new password.
                  </div>
                  <div class="warning">
                    ⚠️ If you did not request this password reset, please ignore this email or contact support. Your password will remain unchanged.
                  </div>
                </div>
                <div class="footer">
                  © 2026 RailTrack India • Live Train Intelligence & Tracking
                </div>
              </div>
            </body>
            </html>
            """.formatted(username != null ? username : "Passenger", otp);

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends a confirmation email after successful password reset.
     */
    public boolean sendPasswordResetSuccessEmail(String toEmail, String username) {
        String subject = "🚆 Your RailTrack Password Has Been Reset";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 540px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; }
                .content { padding: 32px 24px; }
                .greeting { font-size: 16px; color: #e2e8f0; margin-bottom: 16px; }
                .message { font-size: 14px; color: #94a3b8; line-height: 1.6; margin-bottom: 24px; }
                .footer { background-color: #0f172a; padding: 16px 24px; text-align: center; font-size: 12px; color: #64748b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🚆 RailTrack India</h1>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  <div class="message">
                    Your <strong>RailTrack</strong> password was successfully updated just now. You can now log in using your new credentials.
                  </div>
                  <div class="message" style="color: #ef4444; font-size: 13px;">
                    If you did not perform this action, please contact support immediately to secure your account.
                  </div>
                </div>
                <div class="footer">
                  © 2026 RailTrack India • Live Train Intelligence & Tracking
                </div>
              </div>
            </body>
            </html>
            """.formatted(username != null ? username : "Passenger");

        return sendEmail(toEmail, username, subject, htmlContent);
    }
}

