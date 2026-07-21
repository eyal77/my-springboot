package com.example.eyal.rest.dto;

public record DiskInfo(
    String path,
    long totalSpaceBytes,
    long freeSpaceBytes,
    long usableSpaceBytes,
    String freeSpaceFormatted
) {}
