package com.service.mailsender.controller;

import com.service.mailsender.dto.DoctorApprovalMailDto;
import com.service.mailsender.dto.MailResponseDto;
import com.service.mailsender.service.IDoctorApprovalMailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
@Slf4j
public class DoctorApprovalMailController {

    private final IDoctorApprovalMailService iDoctorApprovalMailService;

    @PostMapping("/doctor-approved")
    public ResponseEntity<MailResponseDto> sendDoctorApprovalMail(
            @Valid @RequestBody DoctorApprovalMailDto request
            ) throws MessagingException {
        log.info("REST API invoked: Sending doctor approval email to: {}", request.recipientEmail());
        MailResponseDto mailResponseDto = iDoctorApprovalMailService.sendDoctorApprovalMail(request);
        return ResponseEntity.ok(mailResponseDto);
    }
}
