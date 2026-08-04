package com.example.eyal.rest.dto;

import java.util.List;

public record SystemInfoResponse(
    String hostname,
    String date,
    String time,
    String timestamp,
    long freeSpaceBytes,
    String freeSpaceFormatted,
    List<DiskInfo> diskDrives,
    NetworkInfo networkInfo,
    String computerBrand,
    String computerModel,
    String serialNumber,
    String cpuModel
) {}
