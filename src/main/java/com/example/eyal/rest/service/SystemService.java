package com.example.eyal.rest.service;

import com.example.eyal.rest.dto.DiskInfo;
import com.example.eyal.rest.dto.NetworkInfo;
import com.example.eyal.rest.dto.SystemInfoResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
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

        NetworkInfo networkInfo = getNetworkDetails();

        return new SystemInfoResponse(
            hostname,
            dateStr,
            timeStr,
            timestampStr,
            totalFreeBytes,
            formatBytes(totalFreeBytes),
            diskDrives,
            networkInfo
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

    private NetworkInfo getNetworkDetails() {
        String localIp = "N/A";
        String subnetMask = "N/A";

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (addr instanceof java.net.Inet4Address) {
                        localIp = addr.getHostAddress();
                        short prefix = ia.getNetworkPrefixLength();
                        subnetMask = prefixLengthToMask(prefix);
                        break;
                    }
                }
                if (!"N/A".equals(localIp)) break;
            }
        } catch (Exception e) {
            // fallback
        }

        String defaultGateway = getDefaultGateway();
        String externalIp = getExternalIp();

        return new NetworkInfo(localIp, subnetMask, defaultGateway, externalIp);
    }

    private String prefixLengthToMask(short prefix) {
        int mask = 0xffffffff << (32 - prefix);
        byte[] bytes = new byte[]{
                (byte) ((mask >>> 24) & 0xFF),
                (byte) ((mask >>> 16) & 0xFF),
                (byte) ((mask >>> 8) & 0xFF),
                (byte) (mask & 0xFF)
        };
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String getDefaultGateway() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getDefaultGatewayWindows();
        } else {
            return getDefaultGatewayUnix();
        }
    }

    private String getDefaultGatewayWindows() {
        try {
            Process process = Runtime.getRuntime().exec("route print 0.0.0.0");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("0.0.0.0")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            return parts[2];
                        }
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return "N/A";
    }

    private String getDefaultGatewayUnix() {
        try {
            Process process = Runtime.getRuntime().exec("netstat -rn");
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("default") || line.startsWith("0.0.0.0")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            return parts[1];
                        }
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return "N/A";
    }

    private String getExternalIp() {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.ipify.org"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .build();
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .body()
                    .trim();
        } catch (Exception e) {
            return "Offline/Unknown";
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
