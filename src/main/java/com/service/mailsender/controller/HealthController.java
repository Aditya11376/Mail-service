package com.service.mailsender.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    @GetMapping({"/","/mail/health","/health"})
    public ResponseEntity<Map<String,Object>> checkHealth(){
        return ResponseEntity.ok(Map.of(
                "service","MAIL SERVICE",
                "status","UP",
                "port",8095,
                "timestamp",LocalDateTime.now()
        ));
    }

}
