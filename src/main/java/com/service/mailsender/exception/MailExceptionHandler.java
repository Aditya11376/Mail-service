package com.service.mailsender.exception;

import com.service.mailsender.dto.ErrorResponseDto;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class MailExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException methodArgumentNotValidException) {
        Map<String, String> errorsList = new HashMap<>();
        List<FieldError> list = methodArgumentNotValidException.getBindingResult().getFieldErrors();
        list.forEach(error -> errorsList.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errorsList, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception, WebRequest request) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                request.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Mail Connection / Delivery Errors (SMTP timeout, network issue)
    @ExceptionHandler({MailException.class, MessagingException.class})
    public ResponseEntity<ErrorResponseDto> handleMailDeliveryException(
            Exception ex,
            WebRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                request.getDescription(false).replace("uri=", ""),
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to send email via SMTP server: " + ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // SMTP Authentication Failure (Wrong Google App Password / Username)
    @ExceptionHandler(MailAuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleMailAuthenticationException(
            MailAuthenticationException ex,
            WebRequest request) {
        ErrorResponseDto errorResponse = new ErrorResponseDto(
                request.getDescription(false).replace("uri=", ""),
                HttpStatus.UNAUTHORIZED,
                "SMTP Authentication failed. Please verify your Google App Password and username.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

}
