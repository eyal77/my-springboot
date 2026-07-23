package com.example.eyal.rest.dto;

public record NetworkInfo(
    String localIp,
    String subnetMask,
    String defaultGateway,
    String externalIp
) {}
