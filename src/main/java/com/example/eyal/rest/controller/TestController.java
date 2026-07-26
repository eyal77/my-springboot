package com.example.eyal.rest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "System Test", description = "Endpoints for testing")

public class TestController {

    @GetMapping
    public Map<String, String> test() {
        return Map.of(
                "status", "ok",
                "message", "Test endpoint works"
        );
    }
}