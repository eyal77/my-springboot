package com.example.eyal.rest.service;

import com.example.eyal.rest.dto.DiskInfo;
import com.example.eyal.rest.dto.SystemInfoResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemService {

    public SystemInfoResponse getSystemInfo() {
        String hostname = getHostName();
        ZonedDateTime now = ZonedDateTime.now();

        String dateStr = now.toLocalDate().toString();
        String timeStr = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String timestampStr = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        List<DiskInfo> diskDrives = new ArrayList<>();
        long totalFreeBytes = 0;

        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                long total = root.getTotalSpace();
                long free = root.getFreeSpace();
                long usable = root.getUsableSpace();

                totalFreeBytes += usable;

                diskDrives.add(new DiskInfo(
                    root.getAbsolutePath(),
                    total,
                    free,
                    usable,
                    formatBytes(usable)
                ));
            }
        }

        return new SystemInfoResponse(
            hostname,
            dateStr,
            timeStr,
            timestampStr,
            totalFreeBytes,
            formatBytes(totalFreeBytes),
            diskDrives
        );
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName != null && !computerName.isBlank()) {
                return computerName;
            }
            String hostNameEnv = System.getenv("HOSTNAME");
            if (hostNameEnv != null && !hostNameEnv.isBlank()) {
                return hostNameEnv;
            }
            return "localhost";
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
