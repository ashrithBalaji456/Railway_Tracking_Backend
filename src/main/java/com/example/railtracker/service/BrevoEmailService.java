package com.example.railtracker.service;

import com.example.railtracker.dto.JourneyOptionDto;
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

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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

    /**
     * Sends a rich train delay alert notification.
     */
    public boolean sendTrainDelayAlertEmail(
            String toEmail,
            String username,
            String trainNumber,
            String trainName,
            int delayMinutes,
            int thresholdMinutes,
            String currentStationName,
            String nextStationName,
            String runStatus,
            String lastLocationInfo) {

        String subject = "⚠️ RailTrack Alert: Train %s (%s) is Delayed by %d Mins".formatted(
                trainNumber,
                trainName != null ? trainName : "Express",
                delayMinutes);

        String trackUrl = (frontendUrl != null ? frontendUrl.split(",")[0].trim() : "https://railtrack.in")
                + "/trains/" + trainNumber;

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 580px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 100%%); padding: 24px; text-align: center; }
                .header h1 { margin: 0; color: #0f172a; font-size: 22px; font-weight: 800; }
                .header p { margin: 4px 0 0; color: #1e293b; font-size: 14px; font-weight: 600; }
                .content { padding: 30px 24px; }
                .greeting { font-size: 16px; color: #e2e8f0; margin-bottom: 14px; }
                .alert-card { background-color: #0f172a; border-left: 4px solid #f59e0b; border-radius: 6px; padding: 16px 20px; margin-bottom: 24px; }
                .alert-title { font-size: 18px; font-weight: 700; color: #fbbf24; margin-bottom: 6px; }
                .alert-desc { font-size: 13px; color: #94a3b8; }
                .details-table { width: 100%%; border-collapse: collapse; margin-bottom: 24px; }
                .details-table td { padding: 10px 12px; font-size: 14px; border-bottom: 1px solid #334155; }
                .label { color: #94a3b8; width: 40%%; font-weight: 500; }
                .value { color: #f8fafc; font-weight: 600; }
                .delay-badge { background-color: #dc2626; color: #ffffff; padding: 3px 8px; border-radius: 4px; font-weight: 700; }
                .btn-container { text-align: center; margin: 28px 0 16px; }
                .button { background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff !important; padding: 12px 28px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 14px; display: inline-block; }
                .tip { background-color: #0f172a; border-radius: 6px; padding: 14px 18px; font-size: 12px; color: #64748b; line-height: 1.5; }
                .footer { background-color: #0f172a; padding: 16px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🚆 RailTrack Live Alert</h1>
                  <p>Train Delay Notification</p>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  <div class="alert-card">
                    <div class="alert-title">Train Delay Exceeded Your Alert Threshold</div>
                    <div class="alert-desc">Your alert threshold was set for %d mins delay.</div>
                  </div>

                  <table class="details-table">
                    <tr>
                      <td class="label">Train</td>
                      <td class="value"><strong>%s - %s</strong></td>
                    </tr>
                    <tr>
                      <td class="label">Current Delay</td>
                      <td class="value"><span class="delay-badge">+%d minutes</span></td>
                    </tr>
                    <tr>
                      <td class="label">Status</td>
                      <td class="value">%s</td>
                    </tr>
                    <tr>
                      <td class="label">Current Location</td>
                      <td class="value">%s</td>
                    </tr>
                    <tr>
                      <td class="label">Next Station</td>
                      <td class="value">%s</td>
                    </tr>
                  </table>

                  <div class="btn-container">
                    <a href="%s" class="button">Track Train Live on RailTrack</a>
                  </div>

                  <div class="tip">
                    💡 <strong>Traveler Tip:</strong> Station platforms may adjust dynamically for delayed trains. Please verify platform display boards upon arrival at the station.
                  </div>
                </div>
                <div class="footer">
                  You received this email because you subscribed to delay alerts on RailTrack India.<br/>
                  © 2026 RailTrack India • Live Train Intelligence & Tracking
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                username != null ? username : "Passenger",
                thresholdMinutes,
                trainNumber,
                trainName != null ? trainName : "Express",
                delayMinutes,
                runStatus != null ? runStatus : "RUNNING",
                currentStationName != null && !currentStationName.isBlank() ? currentStationName : (lastLocationInfo != null ? lastLocationInfo : "En Route"),
                nextStationName != null && !nextStationName.isBlank() ? nextStationName : "Upcoming Station",
                trackUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends a warm welcome email to newly registered users.
     */
    public boolean sendWelcomeEmail(String toEmail, String username) {
        String subject = "🚆 Welcome aboard RailTrack, %s! Your live train companion".formatted(
                username != null ? username : "Passenger");

        String appUrl = (frontendUrl != null ? frontendUrl.split(",")[0].trim() : "http://localhost:5173");

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 580px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #4f46e5 0%%, #3b82f6 100%%); padding: 32px 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; }
                .header p { margin: 6px 0 0; color: #e0e7ff; font-size: 14px; font-weight: 500; }
                .content { padding: 32px 24px; }
                .greeting { font-size: 18px; color: #f8fafc; font-weight: 700; margin-bottom: 12px; }
                .intro { font-size: 14px; color: #94a3b8; line-height: 1.6; margin-bottom: 24px; }
                .features { margin-bottom: 28px; }
                .feature-item { background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 14px 16px; margin-bottom: 10px; }
                .feature-title { font-size: 14px; font-weight: 700; color: #e2e8f0; margin-bottom: 2px; }
                .feature-desc { font-size: 12px; color: #94a3b8; line-height: 1.4; }
                .btn-container { text-align: center; margin: 28px 0 16px; }
                .button { background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff !important; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 15px; display: inline-block; }
                .footer { background-color: #0f172a; padding: 20px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; line-height: 1.6; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🚆 RailTrack India</h1>
                  <p>Live Train Telemetry & Travel Intelligence</p>
                </div>
                <div class="content">
                  <div class="greeting">Welcome aboard, %s! 🎉</div>
                  <div class="intro">
                    Thank you for joining <strong>RailTrack</strong>. You now have access to real-time train tracking, automated delay warnings, and intelligent journey planning.
                  </div>

                  <div class="features">
                    <div class="feature-item">
                      <div class="feature-title">📍 Live GPS Train Tracking</div>
                      <div class="feature-desc">Track live speed, delay, coordinates, and intermediate stops on an interactive map.</div>
                    </div>

                    <div class="feature-item">
                      <div class="feature-title">⚠️ Automated Delay Notifications</div>
                      <div class="feature-desc">Set custom delay thresholds and receive real-time email alerts straight to your inbox.</div>
                    </div>

                    <div class="feature-item">
                      <div class="feature-title">🚉 Live Station Boards</div>
                      <div class="feature-desc">Check upcoming arrivals and departures with live platform numbers across 8,000+ Indian stations.</div>
                    </div>

                    <div class="feature-item">
                      <div class="feature-title">🤖 RailAI Travel Assistant</div>
                      <div class="feature-desc">Ask questions about coach positions, train schedules, and best transit routes anytime.</div>
                    </div>
                  </div>

                  <div class="btn-container">
                    <a href="%s" class="button">Start Exploring RailTrack</a>
                  </div>
                </div>
                <div class="footer">
                  Happy Journey! 🛤️<br/>
                  © 2026 RailTrack India • All Rights Reserved
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                username != null ? username : "Passenger",
                appUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends a detailed journey itinerary memo.
     */
    public boolean sendJourneyItineraryEmail(
            String toEmail,
            String recipientName,
            String travelDate,
            JourneyOptionDto journey) {

        if (journey == null) return false;

        String origin = journey.originStationCode();
        String dest = journey.destinationStationCode();
        String subject = "🚆 RailTrack Travel Memo: %s ➔ %s (%s)".formatted(
                origin,
                dest,
                journey.firstTrainNumber()
        );

        String trackUrl = (frontendUrl != null ? frontendUrl.split(",")[0].trim() : "https://railtrack.in")
                + "/trains/" + journey.firstTrainNumber();

        int totalHours = journey.totalDurationMinutes() != null ? journey.totalDurationMinutes() / 60 : 0;
        int totalMins = journey.totalDurationMinutes() != null ? journey.totalDurationMinutes() % 60 : 0;
        String durationStr = totalHours + "h " + totalMins + "m";

        StringBuilder legsHtml = new StringBuilder();

        // First Leg
        legsHtml.append("""
            <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px; margin-bottom: 14px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; border-bottom: 1px solid #1e293b; padding-bottom: 8px;">
                <span style="font-size: 15px; font-weight: 800; color: #818cf8;">Train #%s</span>
                <span style="font-size: 13px; font-weight: 600; color: #cbd5e1;">%s</span>
              </div>
              <table style="width: 100%%; font-size: 13px; color: #94a3b8;">
                <tr>
                  <td style="padding: 4px 0;"><strong>Departure:</strong></td>
                  <td style="color: #f8fafc; font-weight: 600;">%s from %s (%s)</td>
                </tr>
                <tr>
                  <td style="padding: 4px 0;"><strong>Arrival:</strong></td>
                  <td style="color: #f8fafc; font-weight: 600;">%s at %s (%s)</td>
                </tr>
              </table>
            </div>
            """.formatted(
                journey.firstTrainNumber(),
                journey.firstTrainName() != null ? journey.firstTrainName() : "Express",
                journey.originDepartureTime() != null ? journey.originDepartureTime().toString() : "--:--",
                journey.originStationName(),
                journey.originStationCode(),
                journey.direct() ? (journey.destinationArrivalTime() != null ? journey.destinationArrivalTime().toString() : "--:--") : (journey.transferArrivalTime() != null ? journey.transferArrivalTime().toString() : "--:--"),
                journey.direct() ? journey.destinationStationName() : journey.transferStationName(),
                journey.direct() ? journey.destinationStationCode() : journey.transferStationCode()
        ));

        // If connecting journey
        if (!journey.direct() && journey.secondTrainNumber() != null) {
            legsHtml.append("""
                <div style="background-color: #451a03; border-left: 4px solid #f59e0b; padding: 12px 16px; border-radius: 6px; margin-bottom: 14px; font-size: 13px; color: #fde68a;">
                  🔄 <strong>Transit at %s (%s):</strong> %d mins layover %s
                </div>
                """.formatted(
                    journey.transferStationName(),
                    journey.transferStationCode(),
                    journey.layoverMinutes() != null ? journey.layoverMinutes() : 0,
                    journey.layoverWarning() != null ? "• " + journey.layoverWarning() : ""
            ));

            legsHtml.append("""
                <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 18px; margin-bottom: 14px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; border-bottom: 1px solid #1e293b; padding-bottom: 8px;">
                    <span style="font-size: 15px; font-weight: 800; color: #818cf8;">Train #%s</span>
                    <span style="font-size: 13px; font-weight: 600; color: #cbd5e1;">%s</span>
                  </div>
                  <table style="width: 100%%; font-size: 13px; color: #94a3b8;">
                    <tr>
                      <td style="padding: 4px 0;"><strong>Departure:</strong></td>
                      <td style="color: #f8fafc; font-weight: 600;">%s from %s (%s)</td>
                    </tr>
                    <tr>
                      <td style="padding: 4px 0;"><strong>Final Arrival:</strong></td>
                      <td style="color: #f8fafc; font-weight: 600;">%s at %s (%s)</td>
                    </tr>
                  </table>
                </div>
                """.formatted(
                    journey.secondTrainNumber(),
                    journey.secondTrainName() != null ? journey.secondTrainName() : "Express",
                    journey.transferDepartureTime() != null ? journey.transferDepartureTime().toString() : "--:--",
                    journey.transferStationName(),
                    journey.transferStationCode(),
                    journey.destinationArrivalTime() != null ? journey.destinationArrivalTime().toString() : "--:--",
                    journey.destinationStationName(),
                    journey.destinationStationCode()
            ));
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 580px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #4f46e5 0%%, #3b82f6 100%%); padding: 28px 24px; text-align: center; }
                .header h1 { margin: 0; color: #ffffff; font-size: 22px; font-weight: 800; }
                .header p { margin: 6px 0 0; color: #e0e7ff; font-size: 14px; font-weight: 500; }
                .content { padding: 30px 24px; }
                .greeting { font-size: 16px; color: #e2e8f0; margin-bottom: 12px; }
                .route-summary { background-color: #0f172a; border-radius: 8px; padding: 16px 20px; margin-bottom: 24px; text-align: center; border: 1px solid #334155; }
                .route-codes { font-size: 22px; font-weight: 800; color: #818cf8; letter-spacing: 1px; }
                .route-names { font-size: 13px; color: #94a3b8; margin-top: 4px; }
                .meta-tags { margin-top: 10px; display: inline-flex; gap: 8px; font-size: 11px; font-weight: 700; }
                .tag { background-color: #1e293b; padding: 3px 10px; border-radius: 9999px; color: #cbd5e1; border: 1px solid #334155; }
                .btn-container { text-align: center; margin: 26px 0 16px; }
                .button { background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff !important; padding: 13px 30px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 14px; display: inline-block; }
                .footer { background-color: #0f172a; padding: 18px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🚆 RailTrack Journey Itinerary</h1>
                  <p>Your Confirmed Travel Memo & Schedule</p>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  <div style="font-size: 13px; color: #94a3b8; margin-bottom: 20px;">
                    Here is your travel itinerary from <strong>%s</strong> to <strong>%s</strong>%s.
                  </div>

                  <div class="route-summary">
                    <div class="route-codes">%s ➔ %s</div>
                    <div class="route-names">%s to %s</div>
                    <div class="meta-tags">
                      <span class="tag">⏱️ Total: %s</span>
                      <span class="tag">%s</span>
                      %s
                    </div>
                  </div>

                  <h3 style="font-size: 14px; font-weight: 700; color: #cbd5e1; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;">Train Schedule Details</h3>
                  %s

                  <div class="btn-container">
                    <a href="%s" class="button">Track Train Live on RailTrack</a>
                  </div>

                  <div style="background-color: #0f172a; border-radius: 6px; padding: 14px 18px; font-size: 12px; color: #64748b; line-height: 1.5; margin-top: 20px;">
                    💡 <strong>Traveler Checklist:</strong> Confirm live platform numbers and train delays before heading to the station. Have your ticket & ID ready for TTE inspection.
                  </div>
                </div>
                <div class="footer">
                  Generated by RailTrack India • Live Train Telemetry & Intelligence<br/>
                  © 2026 RailTrack India • All Rights Reserved
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                recipientName != null ? recipientName : "Passenger",
                journey.originStationName(),
                journey.destinationStationName(),
                travelDate != null && !travelDate.isBlank() ? " scheduled for " + travelDate : "",
                origin,
                dest,
                journey.originStationName(),
                journey.destinationStationName(),
                durationStr,
                journey.direct() ? "Direct Route" : "Connecting Route",
                travelDate != null && !travelDate.isBlank() ? "<span class=\"tag\">📅 " + travelDate + "</span>" : "",
                legsHtml.toString(),
                trackUrl
            );

        return sendEmail(toEmail, recipientName, subject, htmlContent);
    }

    /**
     * Sends the weekly curated rail blog digest.
     */
    public boolean sendWeeklyBlogDigestEmail(
            String toEmail,
            String username,
            BlogDigestRotatorService.WeeklyDigestBundle digest) {

        if (digest == null || digest.spotlight() == null) return false;

        var spotlight = digest.spotlight();
        String subject = "📰 RailTrack Weekly #%d: %s".formatted(
                digest.editionNumber(),
                spotlight.title()
        );

        String baseUrl = (frontendUrl != null ? frontendUrl.split(",")[0].trim() : "https://railtrack.in");
        String spotlightUrl = baseUrl + "/blogs/" + spotlight.slug();

        StringBuilder secondariesHtml = new StringBuilder();
        if (digest.secondaries() != null) {
            for (var article : digest.secondaries()) {
                String articleUrl = baseUrl + "/blogs/" + article.slug();
                secondariesHtml.append("""
                    <div style="background-color: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 16px; margin-bottom: 12px;">
                      <div style="display: inline-block; background-color: #4f46e5; color: #ffffff; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; margin-bottom: 6px;">
                        %s
                      </div>
                      <h4 style="margin: 0 0 6px 0; font-size: 15px; font-weight: 700; color: #f8fafc;">
                        <a href="%s" style="color: #f8fafc; text-decoration: none;">%s</a>
                      </h4>
                      <p style="margin: 0 0 10px 0; font-size: 12px; color: #94a3b8; line-height: 1.5;">
                        %s
                      </p>
                      <div style="font-size: 11px; color: #818cf8; font-weight: 600;">
                        ⏱️ %d min read &bull; <a href="%s" style="color: #818cf8; text-decoration: underline;">Read story &rarr;</a>
                      </div>
                    </div>
                    """.formatted(
                        article.category() != null ? article.category() : "Explore",
                        articleUrl,
                        article.title(),
                        article.subtitle() != null ? article.subtitle() : "",
                        article.readTimeMinutes() != null ? article.readTimeMinutes() : 4,
                        articleUrl
                ));
            }
        }

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 600px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #1e1b4b 0%%, #312e81 100%%); padding: 28px 24px; text-align: center; border-bottom: 2px solid #4f46e5; }
                .header-tag { font-size: 11px; font-weight: 800; letter-spacing: 2px; text-transform: uppercase; color: #818cf8; margin-bottom: 4px; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; }
                .content { padding: 30px 24px; }
                .greeting { font-size: 15px; color: #cbd5e1; margin-bottom: 20px; }
                .spotlight-card { background-color: #0f172a; border: 1px solid #4338ca; border-radius: 10px; padding: 20px; margin-bottom: 28px; }
                .category-badge { display: inline-block; background-color: #4338ca; color: #e0e7ff; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 4px; text-transform: uppercase; margin-bottom: 10px; }
                .spotlight-title { font-size: 20px; font-weight: 800; color: #ffffff; margin: 0 0 8px 0; line-height: 1.3; }
                .spotlight-sub { font-size: 13px; color: #94a3b8; line-height: 1.6; margin: 0 0 16px 0; }
                .spotlight-meta { font-size: 12px; color: #64748b; margin-bottom: 16px; }
                .button { background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff !important; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 700; font-size: 13px; display: inline-block; }
                .section-header { font-size: 13px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; color: #94a3b8; margin: 0 0 14px 0; }
                .tip-box { background-color: #172554; border-left: 4px solid #3b82f6; border-radius: 6px; padding: 14px 18px; margin-top: 24px; font-size: 13px; color: #bfdbfe; line-height: 1.5; }
                .footer { background-color: #0f172a; padding: 20px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="header-tag">RAILTRACK WEEKLY GAZETTE &bull; ISSUE #%d</div>
                  <h1>🚆 Stories Along the Tracks</h1>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s, here is your curated selection of rail journeys, scenic routes, and travel stories for the week:</div>

                  <!-- Cover Story -->
                  <div class="spotlight-card">
                    <div class="category-badge">%s</div>
                    <h2 class="spotlight-title">%s</h2>
                    <p class="spotlight-sub">%s</p>
                    <div class="spotlight-meta">⏱️ %d min read &bull; By %s</div>
                    <a href="%s" class="button">Read Full Story &rarr;</a>
                  </div>

                  <!-- More Stories -->
                  <div class="section-header">More Stories in This Issue</div>
                  %s

                  <!-- Traveler Tip -->
                  <div class="tip-box">
                    💡 <strong>Rail Traveler Tip of the Week:</strong><br/>
                    %s
                  </div>
                </div>
                <div class="footer">
                  Curated with ❤️ by RailTrack India &bull; Live Train Intelligence<br/>
                  &copy; 2026 RailTrack India. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                digest.editionNumber(),
                username != null ? username : "Passenger",
                spotlight.category() != null ? spotlight.category() : "Cover Story",
                spotlight.title(),
                spotlight.subtitle() != null ? spotlight.subtitle() : "",
                spotlight.readTimeMinutes() != null ? spotlight.readTimeMinutes() : 5,
                spotlight.author() != null ? spotlight.author() : "RailTrack Editorial",
                spotlightUrl,
                secondariesHtml.toString(),
                digest.travelerTip() != null ? digest.travelerTip() : "Keep digital copies of tickets handy on mobile."
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends an automated Travel Day Morning Reminder.
     */
    public boolean sendTravelDayReminderEmail(
            String toEmail,
            String username,
            String pnr,
            String trainNumber,
            String trainName,
            java.time.LocalDate travelDate,
            String liveLocation,
            Integer delayMinutes) {

        String trainLabel = (trainName != null && !trainName.isBlank()) ? trainName : "Express";
        String subject = "🚆 Travel Day Reminder: Your train %s (%s) is scheduled for today!".formatted(trainNumber, trainLabel);

        String baseUrl = (frontendUrl != null ? frontendUrl.split(",")[0].trim() : "https://railtrack.in");
        String trackUrl = baseUrl + "/trains/" + trainNumber;
        String stationsUrl = baseUrl + "/stations";

        String delayText = (delayMinutes != null && delayMinutes > 0)
                ? "<span style=\"color: #f87171; font-weight: 700;\">Running " + delayMinutes + " mins late</span>"
                : "<span style=\"color: #34d399; font-weight: 700;\">Expected On-Time</span>";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
                .container { max-width: 580px; margin: 0 auto; background-color: #1e293b; border-radius: 12px; border: 1px solid #334155; overflow: hidden; }
                .header { background: linear-gradient(135deg, #0284c7 0%%, #2563eb 100%%); padding: 28px 24px; text-align: center; }
                .header-tag { font-size: 11px; font-weight: 800; letter-spacing: 2px; text-transform: uppercase; color: #bae6fd; margin-bottom: 4px; }
                .header h1 { margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; }
                .content { padding: 30px 24px; }
                .greeting { font-size: 16px; color: #f8fafc; font-weight: 700; margin-bottom: 12px; }
                .reminder-box { background-color: #0f172a; border: 1px solid #0284c7; border-radius: 10px; padding: 20px; margin-bottom: 24px; }
                .table-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #1e293b; font-size: 13px; }
                .table-label { color: #94a3b8; font-weight: 500; }
                .table-val { color: #f8fafc; font-weight: 700; }
                .pnr-badge { background-color: #1e1b4b; border: 1px solid #4f46e5; color: #a5b4fc; font-family: monospace; font-size: 14px; font-weight: 800; padding: 2px 8px; border-radius: 6px; letter-spacing: 1px; }
                .btn-group { display: flex; gap: 10px; margin: 24px 0 16px; }
                .btn-primary { background: linear-gradient(135deg, #0284c7 0%%, #2563eb 100%%); color: #ffffff !important; padding: 12px 20px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 13px; text-align: center; flex: 1; }
                .btn-secondary { background-color: #1e293b; border: 1px solid #475569; color: #e2e8f0 !important; padding: 12px 20px; text-decoration: none; border-radius: 8px; font-weight: 700; font-size: 13px; text-align: center; flex: 1; }
                .checklist { background-color: #0f172a; border-radius: 8px; padding: 16px; border: 1px solid #334155; font-size: 12px; color: #94a3b8; line-height: 1.6; }
                .footer { background-color: #0f172a; padding: 18px 24px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="header-tag">TRAVEL DAY REMINDER</div>
                  <h1>🚆 Your Journey is Today!</h1>
                </div>
                <div class="content">
                  <div class="greeting">Good morning %s! 🌅</div>
                  <p style="font-size: 13px; color: #94a3b8; margin: 0 0 20px 0; line-height: 1.5;">
                    This is a reminder that your train journey on <strong>Train %s (%s)</strong> is scheduled for today (%s).
                  </p>

                  <div class="reminder-box">
                    <div class="table-row">
                      <span class="table-label">PNR Number:</span>
                      <span class="pnr-badge">%s</span>
                    </div>
                    <div class="table-row">
                      <span class="table-label">Train:</span>
                      <span class="table-val">%s - %s</span>
                    </div>
                    <div class="table-row">
                      <span class="table-label">Expected Status:</span>
                      <span>%s</span>
                    </div>
                    <div class="table-row" style="border-bottom: none;">
                      <span class="table-label">Live Telemetry:</span>
                      <span class="table-val" style="color: #38bdf8;">%s</span>
                    </div>
                  </div>

                  <div class="btn-group">
                    <a href="%s" class="btn-primary">Track Train Live</a>
                    <a href="%s" class="btn-secondary">Station Platform Board</a>
                  </div>

                  <div class="checklist">
                    <strong style="color: #e2e8f0;">✈️ Pre-Departure Checklist:</strong><br/>
                    &bull; Keep a valid original Government ID proof (Aadhaar, Voter ID, Passport, DL).<br/>
                    &bull; Arrive at your boarding station at least 30-45 minutes before departure.<br/>
                    &bull; Re-check the station display boards as platform allocations may dynamically shift.
                  </div>
                </div>
                <div class="footer">
                  Have a pleasant and safe journey!<br/>
                  &copy; 2026 RailTrack India &bull; Live Train Telemetry & Travel Assistance
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                username != null ? username : "Passenger",
                trainNumber,
                trainLabel,
                travelDate != null ? travelDate.toString() : "Today",
                pnr,
                trainNumber,
                trainLabel,
                delayText,
                liveLocation != null && !liveLocation.isBlank() ? liveLocation : "Telemetry live on RailTrack",
                trackUrl,
                stationsUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends an urgent Station Approaching & Wake-Up alarm email via Brevo.
     */
    public boolean sendStationApproachingAlarmEmail(
            String toEmail,
            String username,
            String trainNumber,
            String trainName,
            String stationCode,
            String stationName,
            Integer etaMinutes,
            Integer distanceKm,
            String platform) {

        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }

        String trainLabel = (trainName != null && !trainName.isBlank()) ? trainName : "Express";
        String stationLabel = (stationName != null && !stationName.isBlank()) ? stationName : stationCode;
        String etaText = (etaMinutes != null && etaMinutes > 0) ? etaMinutes + " minutes" : "Very soon (under 15 mins)";
        String distText = (distanceKm != null && distanceKm > 0) ? distanceKm + " km away" : "Approaching outer limits";
        String platformText = (platform != null && !platform.isBlank()) ? "Platform " + platform : "Check Station Indicators";

        String subject = "🚨 WAKE-UP ALARM: Train " + trainNumber + " is approaching " + stationLabel + " (" + stationCode + ") in ~" + etaText + "!";

        String trackUrl = frontendUrl + "/trains/" + trainNumber;
        String stationUrl = frontendUrl + "/stations/" + stationCode;

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f1f5f9; margin: 0; padding: 24px; }
                .container { max-width: 620px; margin: 0 auto; background: #0f172a; border-radius: 18px; border: 2px solid #ef4444; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(239, 68, 68, 0.35); }
                .header { background: linear-gradient(135deg, #7f1d1d 0%%, #991b1b 50%%, #b91c1c 100%%); padding: 28px 24px; text-align: center; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; background: rgba(0, 0, 0, 0.4); color: #fef08a; border: 1px solid rgba(254, 240, 138, 0.4); margin-bottom: 10px; }
                .title { margin: 0; font-size: 26px; font-weight: 900; color: #ffffff; letter-spacing: -0.02em; }
                .content { padding: 28px 24px; }
                .urgent-banner { background: rgba(239, 68, 68, 0.15); border: 1px solid rgba(239, 68, 68, 0.4); border-radius: 12px; padding: 14px 18px; margin-bottom: 22px; text-align: center; }
                .card { background: #1e293b; border-radius: 14px; padding: 18px 20px; margin: 18px 0; border: 1px solid #334155; }
                .table-row { display: flex; justify-content: space-between; padding: 9px 0; border-bottom: 1px solid #334155; font-size: 14px; }
                .table-label { color: #94a3b8; font-weight: 500; }
                .table-val { color: #ffffff; font-weight: 700; text-align: right; }
                .btn-group { display: flex; gap: 12px; margin: 24px 0 10px 0; }
                .btn-primary { flex: 1; display: block; text-align: center; background: #dc2626; color: #ffffff; padding: 14px 18px; border-radius: 10px; font-weight: 800; text-decoration: none; font-size: 14px; }
                .btn-secondary { flex: 1; display: block; text-align: center; background: #1e293b; color: #cbd5e1; padding: 14px 18px; border-radius: 10px; font-weight: 700; text-decoration: none; font-size: 14px; border: 1px solid #475569; }
                .luggage-box { background: rgba(245, 158, 11, 0.1); border: 1px dashed rgba(245, 158, 11, 0.4); border-radius: 12px; padding: 14px 16px; margin-top: 20px; font-size: 13px; color: #fbbf24; line-height: 1.6; }
                .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; background: #0b0f19; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="badge">⏰ WAKE-UP CALL &bull; ARRIVAL IMMINENT</div>
                  <h1 class="title">Approaching %s</h1>
                </div>
                <div class="content">
                  <div class="urgent-banner">
                    <strong style="color: #fca5a5; font-size: 16px;">Train %s (%s) is arriving in ~%s!</strong><br/>
                    <span style="font-size: 12px; color: #fecaca;">Distance remaining: %s &bull; Expected Platform: %s</span>
                  </div>

                  <p style="font-size: 14px; color: #cbd5e1; margin-top: 0; line-height: 1.6;">
                    Hello %s, your station approaching alarm has triggered! Please wake up, double-check your belongings beneath the berth, and prepare for deboarding.
                  </p>

                  <div class="card">
                    <div class="table-row">
                      <span class="table-label">Destination Station:</span>
                      <span class="table-val" style="color: #f87171;">%s (%s)</span>
                    </div>
                    <div class="table-row">
                      <span class="table-label">Train Number:</span>
                      <span class="table-val">%s - %s</span>
                    </div>
                    <div class="table-row">
                      <span class="table-label">Estimated Time to Arrival:</span>
                      <span class="table-val" style="color: #fde047;">~%s</span>
                    </div>
                    <div class="table-row">
                      <span class="table-label">Distance to Station:</span>
                      <span class="table-val">%s</span>
                    </div>
                    <div class="table-row" style="border-bottom: none;">
                      <span class="table-label">Assigned Platform:</span>
                      <span class="table-val" style="color: #38bdf8;">%s</span>
                    </div>
                  </div>

                  <div class="btn-group">
                    <a href="%s" class="btn-primary">View Live Train Coordinates</a>
                    <a href="%s" class="btn-secondary">Station Live Board</a>
                  </div>

                  <div class="luggage-box">
                    <strong>🧳 Deboarding Checklist:</strong><br/>
                    &bull; Check under the berth for bags, shoes, and chargers.<br/>
                    &bull; Collect personal electronics, glasses, and wallet/purse.<br/>
                    &bull; Move toward the coach vestibule / door before the train completely halts.
                  </div>
                </div>
                <div class="footer">
                  Safe travels &bull; Automated Night Alarm by RailTrack Telemetry
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                stationLabel,
                trainNumber,
                trainLabel,
                etaText,
                distText,
                platformText,
                username != null && !username.isBlank() ? username : "Passenger",
                stationLabel,
                stationCode,
                trainNumber,
                trainLabel,
                etaText,
                distText,
                platformText,
                trackUrl,
                stationUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends a real-time Station Live Board Timetable snapshot via Brevo.
     */
    public boolean sendStationBoardSnapshotEmail(
            String toEmail,
            String username,
            com.example.railtracker.dto.LiveStationBoardResponse boardData) {

        if (toEmail == null || toEmail.isBlank() || boardData == null || boardData.station() == null) {
            return false;
        }

        String stationCode = boardData.station().code();
        String stationName = boardData.station().name() != null ? boardData.station().name() : stationCode;
        Integer hours = boardData.window() != null && boardData.window().hours() != null ? boardData.window().hours() : 4;
        String windowText = (boardData.window() != null && boardData.window().from() != null && boardData.window().to() != null)
                ? boardData.window().from() + " - " + boardData.window().to()
                : "Next " + hours + " Hours";

        String subject = "📊 Station Live Board: " + stationName + " (" + stationCode + ") - Next " + hours + " Hours";
        String stationUrl = frontendUrl + "/stations?code=" + stationCode;

        StringBuilder tableRows = new StringBuilder();
        var trains = boardData.trains();
        int displayCount = trains != null ? Math.min(trains.size(), 20) : 0;

        if (trains == null || trains.isEmpty()) {
            tableRows.append("""
                <tr>
                  <td colspan="5" style="text-align: center; padding: 18px; color: #94a3b8; font-style: italic;">
                    No trains scheduled in this %d-hour window.
                  </td>
                </tr>
            """.formatted(hours));
        } else {
            for (int i = 0; i < displayCount; i++) {
                var t = trains.get(i);
                String trainNum = t.trainNumber() != null ? t.trainNumber() : "---";
                String trainName = t.trainName() != null ? t.trainName() : "Express";
                String src = t.source() != null && t.source().code() != null ? t.source().code() : "";
                String dest = t.destination() != null && t.destination().code() != null ? t.destination().code() : "";
                String route = (!src.isBlank() && !dest.isBlank()) ? src + " &rarr; " + dest : "En Route";

                String schTime = t.scheduledArrival() != null ? t.scheduledArrival() : (t.scheduledDeparture() != null ? t.scheduledDeparture() : "--:--");
                String expTime = t.expectedArrival() != null ? t.expectedArrival() : (t.expectedDeparture() != null ? t.expectedDeparture() : schTime);

                Integer delay = t.delayMinutes() != null ? t.delayMinutes() : 0;
                String delayBadge;
                if (delay <= 0) {
                    delayBadge = "<span style=\"color: #34d399; font-weight: 700;\">On Time</span>";
                } else if (delay < 30) {
                    delayBadge = "<span style=\"color: #fbbf24; font-weight: 700;\">+" + delay + "m Late</span>";
                } else {
                    delayBadge = "<span style=\"color: #f87171; font-weight: 700;\">+" + delay + "m Late</span>";
                }

                String pf = (t.platform() != null && !t.platform().isBlank()) ? "PF " + t.platform() : "TBD";

                tableRows.append("""
                    <tr style="border-bottom: 1px solid #1e293b;">
                      <td style="padding: 10px 8px; font-weight: 700; color: #f1f5f9;">
                        %s<br/><span style="font-size: 11px; color: #94a3b8; font-weight: 500;">%s</span>
                      </td>
                      <td style="padding: 10px 8px; font-size: 12px; color: #cbd5e1; font-weight: 600;">%s</td>
                      <td style="padding: 10px 8px; font-family: monospace; font-size: 12px; color: #e2e8f0;">
                        <span style="color: #94a3b8; text-decoration: line-through;">%s</span><br/>
                        <strong style="color: #38bdf8;">%s</strong>
                      </td>
                      <td style="padding: 10px 8px; font-size: 12px;">%s</td>
                      <td style="padding: 10px 8px; text-align: center;">
                        <span style="display: inline-block; padding: 3px 8px; border-radius: 6px; background: #0f172a; border: 1px solid #38bdf8; color: #38bdf8; font-weight: 800; font-size: 11px;">
                          %s
                        </span>
                      </td>
                    </tr>
                """.formatted(trainNum, trainName, route, schTime, expTime, delayBadge, pf));
            }
        }

        String recipient = (username != null && !username.isBlank()) ? username : "Passenger";

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f1f5f9; margin: 0; padding: 24px; }
                .container { max-width: 680px; margin: 0 auto; background: #0f172a; border-radius: 18px; border: 1px solid #334155; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); }
                .header { background: linear-gradient(135deg, #1e1b4b 0%%, #312e81 50%%, #4338ca 100%%); padding: 26px 24px; text-align: center; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; background: rgba(0, 0, 0, 0.4); color: #a5b4fc; border: 1px solid rgba(165, 180, 252, 0.3); margin-bottom: 8px; }
                .title { margin: 0; font-size: 24px; font-weight: 900; color: #ffffff; }
                .subtitle { margin-top: 6px; font-size: 13px; color: #c7d2fe; }
                .content { padding: 24px; }
                .meta-bar { display: flex; justify-content: space-between; align-items: center; background: #1e293b; border-radius: 12px; padding: 12px 18px; margin-bottom: 20px; font-size: 13px; border: 1px solid #334155; }
                table { width: 100%%; border-collapse: collapse; margin-top: 12px; font-size: 13px; }
                th { text-align: left; padding: 10px 8px; background: #1e293b; color: #94a3b8; font-weight: 700; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; border-bottom: 2px solid #334155; }
                .btn { display: block; text-align: center; background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff; padding: 14px 20px; border-radius: 10px; font-weight: 800; text-decoration: none; font-size: 14px; margin-top: 24px; }
                .footer { padding: 18px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; background: #0b0f19; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="badge">📊 LIVE STATION DEPARTURE & ARRIVAL BOARD</div>
                  <h1 class="title">%s (%s)</h1>
                  <div class="subtitle">Live Timetable Snapshot &bull; %s</div>
                </div>
                <div class="content">
                  <p style="font-size: 14px; color: #cbd5e1; margin-top: 0;">
                    Hello %s, here is your requested live timetable snapshot for <strong>%s (%s)</strong>.
                  </p>

                  <div class="meta-bar">
                    <span><strong>Coverage Window:</strong> %s (%d Hours)</span>
                    <span style="color: #38bdf8; font-weight: 700;">Showing %d Upcoming Trains</span>
                  </div>

                  <table>
                    <thead>
                      <tr>
                        <th>Train</th>
                        <th>Route</th>
                        <th>Sch / Exp</th>
                        <th>Status</th>
                        <th style="text-align: center;">Platform</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <a href="%s" class="btn">View Real-Time Interactive Board</a>

                  <p style="font-size: 11px; color: #94a3b8; margin-top: 18px; text-align: center; line-height: 1.5;">
                    💡 <em>Note: Platform numbers are subject to dynamic station master operational changes. Please verify station audio announcements upon arrival.</em>
                  </p>
                </div>
                <div class="footer">
                  Automated Station Intelligence &bull; &copy; 2026 RailTrack Telemetry
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                stationName,
                stationCode,
                windowText,
                recipient,
                stationName,
                stationCode,
                windowText,
                hours,
                displayCount,
                tableRows.toString(),
                stationUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }

    /**
     * Sends an automated or test IRCTC Chart Preparation Notification email.
     */
    public boolean sendChartPreparationEmail(
            String toEmail,
            String username,
            String pnr,
            String trainNumber,
            String trainName,
            String chartType,
            String departureTime,
            String travelDate,
            String confirmationProbability) {

        String recipient = (username != null && !username.isBlank()) ? username : "Passenger";
        String effectiveTrainName = (trainName != null && !trainName.isBlank()) ? trainName : "Express";
        String effectiveChartType = (chartType != null && !chartType.isBlank()) ? chartType : "Chart 1 (First Reservation Chart)";
        String effectiveDep = (departureTime != null && !departureTime.isBlank()) ? departureTime : "Scheduled Time";
        String probText = (confirmationProbability != null && !confirmationProbability.isBlank()) ? confirmationProbability : "High / Confirmed";

        String subject = "📊 IRCTC %s Finalized: Train %s (%s) • PNR %s".formatted(
                effectiveChartType.contains("Chart 2") ? "Final Chart (Chart 2)" : "Chart 1",
                trainNumber,
                effectiveTrainName,
                pnr
        );

        String trackUrl = "%s/trains/%s".formatted(
                frontendUrl != null ? frontendUrl.replaceAll("/$", "") : "http://localhost:5173",
                trainNumber
        );

        String pnrUrl = "%s/pnr?pnr=%s".formatted(
                frontendUrl != null ? frontendUrl.replaceAll("/$", "") : "http://localhost:5173",
                pnr
        );

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0b0f19; color: #f1f5f9; margin: 0; padding: 24px; }
                .container { max-width: 600px; margin: 0 auto; background: #0f172a; border-radius: 18px; border: 1px solid #334155; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5); }
                .header { background: linear-gradient(135deg, #1e1b4b 0%%, #312e81 50%%, #4338ca 100%%); padding: 28px 24px; text-align: center; }
                .badge { display: inline-block; padding: 6px 14px; border-radius: 9999px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; background: rgba(0, 0, 0, 0.4); color: #a5b4fc; border: 1px solid rgba(165, 180, 252, 0.3); margin-bottom: 8px; }
                .title { margin: 0; font-size: 24px; font-weight: 900; color: #ffffff; }
                .subtitle { margin-top: 6px; font-size: 13px; color: #c7d2fe; }
                .content { padding: 24px; }
                .card { background: #1e293b; border-radius: 14px; padding: 18px; margin-bottom: 20px; border: 1px solid #334155; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 10px; font-size: 13px; }
                .stat-box { background: #0b0f19; padding: 12px; border-radius: 10px; border: 1px solid #1e293b; }
                .stat-label { font-size: 11px; text-transform: uppercase; color: #94a3b8; font-weight: 700; margin-bottom: 4px; }
                .stat-value { font-size: 15px; font-weight: 800; color: #f8fafc; }
                .prob-tag { color: #34d399; font-weight: 800; }
                .btn { display: block; text-align: center; background: linear-gradient(135deg, #4f46e5 0%%, #6366f1 100%%); color: #ffffff; padding: 14px 20px; border-radius: 10px; font-weight: 800; text-decoration: none; font-size: 14px; margin-top: 20px; }
                .tips { background: rgba(59, 130, 246, 0.1); border: 1px solid rgba(59, 130, 246, 0.2); border-radius: 12px; padding: 14px; margin-top: 20px; font-size: 12px; color: #bfdbfe; line-height: 1.6; }
                .footer { padding: 18px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #1e293b; background: #0b0f19; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <div class="badge">📊 IRCTC RESERVATION CHART ALERT</div>
                  <h1 class="title">%s PREPARED</h1>
                  <div class="subtitle">Train %s &bull; %s</div>
                </div>
                <div class="content">
                  <p style="font-size: 14px; color: #cbd5e1; margin-top: 0;">
                    Hello %s, Indian Railways has finalized <strong>%s</strong> for your upcoming journey on Train <strong>%s</strong>.
                  </p>

                  <div class="card">
                    <div style="font-size: 11px; font-weight: 800; text-transform: uppercase; color: #818cf8; letter-spacing: 0.05em; margin-bottom: 8px;">
                      JOURNEY & CHARTING DETAILS
                    </div>
                    <div class="grid">
                      <div class="stat-box">
                        <div class="stat-label">PNR Number</div>
                        <div class="stat-value" style="font-family: monospace; color: #a5b4fc;">%s</div>
                      </div>
                      <div class="stat-box">
                        <div class="stat-label">Travel Date & Time</div>
                        <div class="stat-value">%s (%s)</div>
                      </div>
                      <div class="stat-box">
                        <div class="stat-label">Chart Status</div>
                        <div class="stat-value" style="color: #38bdf8;">Finalized & Locked</div>
                      </div>
                      <div class="stat-box">
                        <div class="stat-label">Confirmation Forecast</div>
                        <div class="stat-value prob-tag">%s</div>
                      </div>
                    </div>
                  </div>

                  <div class="tips">
                    <strong>📌 Important Next Steps for Passengers:</strong>
                    <ul style="margin: 8px 0 0; padding-left: 18px;">
                      <li>Check your confirmed <strong>Coach (e.g. B3) and Berth (e.g. 47 LB)</strong> on IRCTC or at the station charting board.</li>
                      <li>For waitlisted e-tickets that remain fully unconfirmed after Chart 2, tickets are auto-cancelled with 100%% refund.</li>
                      <li>Current Booking counter remains open until 30 minutes prior to departure for any unallocated berths.</li>
                    </ul>
                  </div>

                  <a href="%s" class="btn">View Live Train GPS Tracking & Platform</a>

                  <p style="text-align: center; margin-top: 14px;">
                    <a href="%s" style="color: #818cf8; font-size: 12px; text-decoration: underline;">Re-verify PNR on RailTrack</a>
                  </p>
                </div>
                <div class="footer">
                  Automated Indian Railways Charting Intelligence &bull; &copy; 2026 RailTrack Telemetry
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                effectiveChartType.toUpperCase(),
                trainNumber,
                effectiveTrainName,
                recipient,
                effectiveChartType,
                trainNumber,
                pnr,
                travelDate,
                effectiveDep,
                probText,
                trackUrl,
                pnrUrl
            );

        return sendEmail(toEmail, username, subject, htmlContent);
    }
}

