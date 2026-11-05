package com.service.mailsender.dto;

import java.time.LocalDateTime;

public record MailResponseDto(
        String status, // SENT, FAILED, SIMULATED
        String message,
        String recipient,
        LocalDateTime localDateTime
) {
}
