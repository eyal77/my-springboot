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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    public SystemInfoResponse getSystemInfo() {
        log.debug("getSystemInfo: Gathering host diagnostics...");
        String hostname = getHostName();
        ZonedDateTime now = ZonedDateTime.now();

        String dateStr = now.toLocalDate().toString();
        String timeStr = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String timestampStr = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        List<DiskInfo> diskDrives = new ArrayList<>();
        long totalFreeBytes = 0;

        File[] roots = File.listRoots();
        if (roots != null) {
            log.debug("getSystemInfo: Listing {} disk root drives...", roots.length);
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
        } else {
            log.warn("getSystemInfo: File.listRoots() returned null.");
        }

        NetworkInfo networkInfo = getNetworkDetails();

        String brand = getComputerBrand();
        String model = getComputerModel();
        String serial = getSerialNumber();
        String cpu = getCpuModel();

        log.debug("getSystemInfo completed: host={}, brand={}, model={}, serial={}, cpu={}, freeBytes={}, interfacesParsed={}", 
            hostname, brand, model, serial, cpu, totalFreeBytes, networkInfo != null);
            
        return new SystemInfoResponse(
            hostname,
            dateStr,
            timeStr,
            timestampStr,
            totalFreeBytes,
            formatBytes(totalFreeBytes),
            diskDrives,
            networkInfo,
            brand,
            model,
            serial,
            cpu
        );
    }

    private String getHostName() {
        log.debug("getHostName: Attempting to resolve local host details...");
        try {
            String name = InetAddress.getLocalHost().getHostName();
            log.debug("getHostName: InetAddress.getLocalHost() succeeded: {}", name);
            return name;
        } catch (UnknownHostException e) {
            log.warn("getHostName: InetAddress.getLocalHost() failed, looking up env properties. Error: {}", e.getMessage());
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
        log.debug("getNetworkDetails: Querying network interfaces...");
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
            log.error("getNetworkDetails: Exception reading interface addresses: {}", e.getMessage(), e);
        }

        log.debug("getNetworkDetails: Found Local IP: {}, Subnet: {}", localIp, subnetMask);
        String defaultGateway = getDefaultGateway();
        String externalIp = getExternalIp();

        return new NetworkInfo(localIp, subnetMask, defaultGateway, externalIp);
    }

    private String prefixLengthToMask(short prefix) {
        log.debug("prefixLengthToMask conversion input: {}", prefix);
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
            log.error("prefixLengthToMask failed: {}", e.getMessage());
            return "N/A";
        }
    }

    private String getDefaultGateway() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("getDefaultGateway: Detected OS: {}", os);
        if (os.contains("win")) {
            return getDefaultGatewayWindows();
        } else {
            return getDefaultGatewayUnix();
        }
    }

    private String getDefaultGatewayWindows() {
        log.debug("getDefaultGatewayWindows executing 'route print 0.0.0.0'...");
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
                            String gateway = parts[2];
                            log.debug("getDefaultGatewayWindows found gateway: {}", gateway);
                            return gateway;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getDefaultGatewayWindows failed: {}", e.getMessage());
        }
        return "N/A";
    }

    private String getDefaultGatewayUnix() {
        log.debug("getDefaultGatewayUnix executing 'netstat -rn'...");
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
                            String gateway = parts[1];
                            log.debug("getDefaultGatewayUnix found gateway: {}", gateway);
                            return gateway;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("getDefaultGatewayUnix failed: {}", e.getMessage());
        }
        return "N/A";
    }

    private String getExternalIp() {
        log.debug("getExternalIp: Executing HTTP call to api.ipify.org...");
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.ipify.org"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .build();
            String ip = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .body()
                    .trim();
            log.debug("getExternalIp returned: {}", ip);
            return ip;
        } catch (Exception e) {
            log.warn("getExternalIp: HTTP request failed (client is likely offline or site is blocked): {}", e.getMessage());
            return "Offline/Unknown";
        }
    }

    private String getComputerBrand() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("getComputerBrand: OS is {}", os);
        if (os.contains("win")) {
            return executeCommand("wmic csproduct get vendor", "Vendor");
        } else if (os.contains("mac")) {
            return "Apple";
        } else {
            // Linux/Unix
            String brand = readFirstLine("/sys/class/dmi/id/sys_vendor");
            if (brand == null || brand.isBlank()) {
                brand = readFirstLine("/sys/class/dmi/id/chassis_vendor");
            }
            return (brand != null && !brand.isBlank()) ? brand.trim() : "Unknown";
        }
    }

    private String getComputerModel() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("getComputerModel: OS is {}", os);
        if (os.contains("win")) {
            return executeCommand("wmic csproduct get name", "Name");
        } else if (os.contains("mac")) {
            return executeCommandAndFindLine("sysctl -n hw.model", null);
        } else {
            // Linux/Unix
            String model = readFirstLine("/sys/class/dmi/id/product_name");
            return (model != null && !model.isBlank()) ? model.trim() : "Unknown";
        }
    }

    private String getSerialNumber() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("getSerialNumber: OS is {}", os);
        if (os.contains("win")) {
            return executeCommand("wmic bios get serialnumber", "SerialNumber");
        } else if (os.contains("mac")) {
            return executeCommandAndFindLine("system_profiler SPHardwareDataType", "Serial Number");
        } else {
            // Linux/Unix
            String serial = readFirstLine("/sys/class/dmi/id/product_serial");
            return (serial != null && !serial.isBlank()) ? serial.trim() : "Unknown";
        }
    }

    private String getCpuModel() {
        String os = System.getProperty("os.name").toLowerCase();
        log.debug("getCpuModel: OS is {}", os);
        if (os.contains("win")) {
            return executeCommand("wmic cpu get name", "Name");
        } else if (os.contains("mac")) {
            return executeCommandAndFindLine("sysctl -n machdep.cpu.brand_string", null);
        } else {
            // Linux/Unix
            try {
                Path cpuinfo = Paths.get("/proc/cpuinfo");
                if (Files.exists(cpuinfo)) {
                    List<String> lines = Files.readAllLines(cpuinfo);
                    for (String line : lines) {
                        if (line.toLowerCase().contains("model name")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length > 1) {
                                return parts[1].trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("getCpuModel failed on Unix: {}", e.getMessage());
            }
            return "Unknown";
        }
    }

    private String readFirstLine(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Files.readAllLines(path).get(0).trim();
            }
        } catch (Exception e) {
            log.error("readFirstLine failed for {}: {}", filePath, e.getMessage());
        }
        return null;
    }

    private String executeCommand(String command, String skipHeader) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (skipHeader != null && line.equalsIgnoreCase(skipHeader)) {
                        continue;
                    }
                    return line;
                }
            }
        } catch (Exception e) {
            log.error("executeCommand failed for command {}: {}", command, e.getMessage());
        }
        return "Unknown";
    }

    private String executeCommandAndFindLine(String command, String searchKeyword) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (searchKeyword != null) {
                        if (line.toLowerCase().contains(searchKeyword.toLowerCase())) {
                            String[] parts = line.split(":", 2);
                            if (parts.length > 1) {
                                return parts[1].trim();
                            }
                            return line.trim();
                        }
                    } else {
                        line = line.trim();
                        if (!line.isEmpty()) return line;
                    }
                }
            }
        } catch (Exception e) {
            log.error("executeCommandAndFindLine failed for command {}: {}", command, e.getMessage());
        }
        return "Unknown";
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
