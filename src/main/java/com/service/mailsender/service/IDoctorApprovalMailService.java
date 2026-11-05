package com.service.mailsender.service;

import com.service.mailsender.dto.DoctorApprovalMailDto;
import com.service.mailsender.dto.MailResponseDto;
import jakarta.mail.MessagingException;

public interface IDoctorApprovalMailService {
    MailResponseDto sendDoctorApprovalMail(DoctorApprovalMailDto doctorApprovalMailDto) throws MessagingException;
}

