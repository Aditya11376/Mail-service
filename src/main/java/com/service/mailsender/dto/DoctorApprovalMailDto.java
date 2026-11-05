package com.service.mailsender.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DoctorApprovalMailDto(
        @NotBlank(message = "Recipient email is required")
        @Email(message="Invalid email format")
        String recipientEmail,

        @NotBlank(message = "Doctor name is required")
        String recipientName,

        @NotBlank(message = "License Number is required")
        String licenseNumber,

        String loginID,
        String loginUrl
) {
}
