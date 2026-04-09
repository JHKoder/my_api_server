package com.example.my_api_server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController("/health")
public class HealthCheckController {
    public ResponseEntity<String> health(){
        return ResponseEntity.ok("ok");

    }
}
