package com.example.eyal.rest.controller;

import com.example.eyal.rest.dto.SystemInfoResponse;
import com.example.eyal.rest.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/system-info")
@Tag(name = "System Information", description = "Endpoints for retrieving host computer status and disk metrics")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);
    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    @Operation(summary = "Get system information", description = "Returns computer hostname, local date, local time, ISO timestamp, and free space breakdown across disk partitions.")
    public SystemInfoResponse getSystemInfo() {
        log.debug("getSystemInfo: GET request received for host metrics.");
        SystemInfoResponse response = systemService.getSystemInfo();
        log.debug("getSystemInfo returning diagnostics payload.");
        return response;
    }
}
