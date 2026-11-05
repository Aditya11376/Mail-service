package com.service.mailsender.service.impl;

import com.service.mailsender.dto.DoctorApprovalMailDto;
import com.service.mailsender.dto.MailResponseDto;
import com.service.mailsender.service.IDoctorApprovalMailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorApprovalMailServiceImpl implements IDoctorApprovalMailService {

    private final JavaMailSender mailSender;

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.from.email:COSMOS MEDICALS <onboarding@resend.dev>}")
    private String resendFromEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${mail.from.email:noreply@cosmos.org.in}")
    private String fromEmail;

    @Value("${mail.from.name:COSMOS Health (GUPTA-SOFTWORKS ORGANISATION)}")
    private String fromName;

    @Value("${mail.simulate.if-empty:false}")
    private boolean simulateIfEmpty;

    @Override
    public MailResponseDto sendDoctorApprovalMail(DoctorApprovalMailDto dto) throws MessagingException {
        log.info("Sending doctor approval notification to: {}", dto.recipientEmail());

        String targetLoginUrl = (dto.loginUrl() != null && !dto.loginUrl().isBlank())
                ? dto.loginUrl() : "http://localhost:5173/login";

        String issuedLoginId = (dto.loginID() != null && !dto.loginID().isBlank())
                ? dto.loginID() : "PENDING-ISSUANCE";

        String htmlContent = buildHtmlContent(dto, issuedLoginId, targetLoginUrl);
        String plainText = buildPlainText(dto, issuedLoginId, targetLoginUrl);

        // 1. Resend HTTPS API (Recommended for Cloud / Render over Port 443)
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            try {
                log.info("Dispatching email via Resend HTTPS API (Port 443)...");
                RestClient restClient = RestClient.create("https://api.resend.com");

                Map<String, Object> payload = Map.of(
                        "from", resendFromEmail,
                        "to", List.of(dto.recipientEmail()),
                        "subject", "COSMOS Clinical Portal: Registration Approved & Login ID Issued",
                        "html", htmlContent,
                        "text", plainText
                );

                String resendResponse = restClient.post()
                        .uri("/emails")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey.trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                log.info("Resend HTTPS dispatch succeeded: {}", resendResponse);
                return new MailResponseDto(
                        "SENT",
                        "Doctor approval email successfully sent via Resend HTTPS (Port 443)",
                        dto.recipientEmail(),
                        LocalDateTime.now()
                );
            } catch (Exception e) {
                log.error("Resend dispatch failed: {}", e.getMessage(), e);
                return new MailResponseDto(
                        "FAILED",
                        "Resend email dispatch failed: " + e.getMessage(),
                        dto.recipientEmail(),
                        LocalDateTime.now()
                );
            }
        }

        // 2. Simulation Mode Check
        if (simulateIfEmpty && (mailUsername == null || mailUsername.isBlank() || mailUsername.contains("your-email"))) {
            log.warn("[SIMULATION] Live email bypassed. Approved Dr. {} ({}) with LoginID: {}",
                    dto.recipientName(), dto.recipientEmail(), issuedLoginId);
            return new MailResponseDto(
                    "SIMULATED",
                    "Email simulation successful. Live dispatch bypassed.",
                    dto.recipientEmail(),
                    LocalDateTime.now()
            );
        }

        // 3. Fallback to JavaMailSender (Local SMTP Port 587)
        try {
            log.info("Dispatching email via SMTP JavaMailSender (Port 587)...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername, fromName);
            helper.setReplyTo(fromEmail);
            helper.setTo(dto.recipientEmail());
            helper.setSubject("COSMOS Clinical Portal: Registration Approved & Login ID Issued");
            helper.setText(plainText, htmlContent);

            mailSender.send(message);
            log.info("Approval notification email successfully sent via SMTP to {}", dto.recipientEmail());
            return new MailResponseDto(
                    "SENT",
                    "Doctor approval email successfully sent with Issued Login ID",
                    dto.recipientEmail(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Failed to send approval email via SMTP to {}: {}", dto.recipientEmail(), e.getMessage(), e);
            return new MailResponseDto(
                    "FAILED",
                    "Email dispatch failed: " + e.getMessage(),
                    dto.recipientEmail(),
                    LocalDateTime.now()
            );
        }
    }

    private String buildPlainText(DoctorApprovalMailDto dto, String issuedLoginId, String targetLoginUrl) {
        return """
            Welcome to COSMOS MEDICALS, Dr. %s!
            
            Your medical registration has been formally verified and approved by the System Administrator.
            
            Practitioner Name: Dr. %s
            License Number: %s
            Issued Login ID: %s
            
            Important Notice: Login strictly using your Issued Login ID only (%s). Login with email address is not permitted for verified clinical security.
            
            Sign in to Doctor Dashboard: %s
            
            COSMOS MEDICALS
            (C) 2026 GUPTA SOFTWORKS ORGANISATION. All rights reserved.
            """.formatted(dto.recipientName(), dto.recipientName(), dto.licenseNumber(), issuedLoginId, issuedLoginId, targetLoginUrl);
    }

    private String buildHtmlContent(DoctorApprovalMailDto dto, String issuedLoginId, String targetLoginUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: 'Segoe UI', Arial, sans-serif;">
                <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="padding: 30px 10px;">
                    <tr>
                        <td align="center">
                            <table width="600" border="0" cellspacing="0" cellpadding="0" style="background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.08);">
                                
                                <!-- Header -->
                                <tr style="background: linear-gradient(135deg, #0f172a, #1e3a8a);">
                                    <td style="padding: 35px 40px; text-align: left; color: #ffffff;">
                                        <h1 style="margin: 0; font-size: 24px; letter-spacing: 0.8px; font-weight: 700;">COSMOS MEDICALS</h1>
                                        <p style="margin: 6px 0 0; font-size: 13px; color: #93c5fd; text-transform: uppercase; letter-spacing: 0.6px; font-weight: 600;">GUPTA SOFTWORKS ORGANISATION</p>
                                    </td>
                                </tr>
                                
                                <!-- Content Body -->
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="color: #0f172a; margin-top: 0; font-size: 21px;">Welcome to the Clinical Network, Dr. %s!</h2>
                                        
                                        <p style="color: #475569; font-size: 15px; line-height: 1.6;">
                                            Your medical registration has been formally <strong>verified and approved</strong> by the System Administrator. Your official practitioner credentials have been provisioned below:
                                        </p>
                                        
                                        <!-- Issued Credentials Card -->
                                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-left: 5px solid #2563eb; padding: 20px; margin: 25px 0; border-radius: 8px;">
                                            <table width="100%%" style="border-collapse: collapse; font-size: 14px;">
                                                <tr>
                                                    <td style="padding: 6px 0; color: #64748b; width: 150px;"><strong>Practitioner Name:</strong></td>
                                                    <td style="padding: 6px 0; color: #0f172a;">Dr. %s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 6px 0; color: #64748b;"><strong>License Number:</strong></td>
                                                    <td style="padding: 6px 0; color: #0f172a;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 8px 0; color: #1e3a8a;"><strong>Issued Login ID:</strong></td>
                                                    <td style="padding: 8px 0;">
                                                        <span style="background-color: #dbeafe; color: #1d4ed8; padding: 5px 12px; border-radius: 4px; font-weight: bold; font-family: monospace; font-size: 15px; border: 1px solid #bfdbfe;">%s</span>
                                                    </td>
                                                </tr>
                                            </table>
                                        </div>
                                        
                                        <!-- Login Instruction Alert: ISSUED LOGIN ID ONLY -->
                                        <div style="background-color: #eff6ff; border: 1px solid #bfdbfe; border-left: 4px solid #1d4ed8; border-radius: 6px; padding: 14px 18px; margin-bottom: 25px;">
                                            <p style="margin: 0; color: #1e40af; font-size: 13.5px; line-height: 1.5;">
                                                <strong>Important Notice:</strong> Login strictly using your <strong>Issued Login ID only</strong> (%s). Login with email address is not permitted for verified clinical security.
                                            </p>
                                        </div>
                                        
                                        <!-- Portal Action Button -->
                                        <div style="text-align: center; margin: 30px 0 20px;">
                                            <a href="%s" style="background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 34px; border-radius: 6px; font-weight: 600; font-size: 15px; display: inline-block;">
                                                Sign In with Login ID &rarr;
                                            </a>
                                        </div>
                                        
                                        <p style="color: #94a3b8; font-size: 12px; margin-top: 30px; line-height: 1.4;">
                                            Security Disclaimer: If you did not apply for clinical access on the COSMOS portal, contact hospital compliance immediately at security@cosmos.org.in.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr style="background-color: #0f172a; color: #94a3b8;">
                                    <td style="padding: 22px 40px; text-align: center; font-size: 12px; line-height: 1.6;">
                                        <p style="margin: 0; color: #f8fafc; font-weight: 700; font-size: 14px; letter-spacing: 0.5px;">COSMOS MEDICALS</p>
                                        <p style="margin: 4px 0 0;">&copy; 2026 GUPTA SOFTWORKS ORGANISATION. All rights reserved.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(dto.recipientName(), dto.recipientName(), dto.licenseNumber(), issuedLoginId, issuedLoginId, targetLoginUrl);
    }
}
