package com.sla.engine;

import com.sla.model.Geolocation;
import com.sla.model.LogRecord;
import com.sla.model.ScanReport;
import com.sla.service.UserAccessControl;
import com.sla.util.AnsiColors;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReportGenerator {
    private final String filePath;
    private final UserAccessControl accessControl;
    private final List<Pattern> confidentialPatterns;
    
    public ReportGenerator(String filePath, UserAccessControl accessControl, List<Pattern> confidentialPatterns) {
        this.filePath = filePath;
        this.accessControl = accessControl;
        this.confidentialPatterns = confidentialPatterns;
    }
    
    public void generate(ScanReport report, ThreatAnalysisEngine analysisEngine) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("=".repeat(80) + "\n");
            writer.write("SECURITY LOG ANALYSIS REPORT\n");
            writer.write("=".repeat(80) + "\n\n");
            
            // Performance Metrics
            writer.write("--- PERFORMANCE METRICS ---\n");
            for (Map.Entry<String, String> entry : report.performanceMetrics.entrySet()) {
                writer.write(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
            writer.write("\n");
            
            // Blacklist Summary
            writer.write("--- BLACKLIST SUMMARY ---\n");
            writer.write(String.format("  Total Blacklisted IPs: %d\n", report.finalBlacklist.size()));
            writer.write(String.format("  Initially Blocked Logs: %d\n", report.initiallyBlockedLogs.size()));
            writer.write(String.format("  Newly Blocked IPs: %d\n", report.newlyBlockedIPs.size()));
            writer.write("\n");
            
            // Newly Blocked IPs
            if (!report.newlyBlockedIPs.isEmpty()) {
                writer.write("--- NEWLY BLOCKED IPs ---\n");
                for (Map.Entry<String, java.time.LocalDateTime> entry : report.newlyBlockedIPs.entrySet()) {
                    String ipDisplay = accessControl.redactIPAddress(entry.getKey());
                    Geolocation geo = report.ipToGeolocationMap.get(entry.getKey());
                    writer.write(String.format("  %s | First Blocked: %s | Location: %s\n",
                        ipDisplay,
                        entry.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        geo != null ? geo : "Unknown"));
                }
                writer.write("\n");
            }
            
            // Top Risk IPs
            writer.write("--- TOP RISK IPs ---\n");
            List<Map.Entry<String, Double>> topRisks = report.ipRiskScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
            
            for (Map.Entry<String, Double> entry : topRisks) {
                String ipDisplay = accessControl.redactIPAddress(entry.getKey());
                Geolocation geo = report.ipToGeolocationMap.get(entry.getKey());
                writer.write(String.format("  %s | Risk Score: %.2f | Location: %s\n",
                    ipDisplay, entry.getValue(), geo != null ? geo : "Unknown"));
            }
            writer.write("\n");
            
            // Suspicious Activity by Country
            if (!report.suspiciousEventsByCountry.isEmpty()) {
                writer.write("--- SUSPICIOUS ACTIVITY BY COUNTRY ---\n");
                report.suspiciousEventsByCountry.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        try {
                            writer.write(String.format("  %s: %d events\n", entry.getKey(), entry.getValue()));
                        } catch (IOException e) {
                            System.err.println(AnsiColors.RED + "Error writing country data: " + e.getMessage() + AnsiColors.RESET);
                        }
                    });
                writer.write("\n");
            }
            
            // Detailed Suspicious IP Activity
            if (!report.suspiciousIPActivity.isEmpty()) {
                writer.write("--- DETAILED SUSPICIOUS IP ACTIVITY ---\n");
                for (Map.Entry<String, List<LogRecord>> entry : report.suspiciousIPActivity.entrySet()) {
                    String ipAddress = entry.getKey();
                    String ipDisplay = accessControl.redactIPAddress(ipAddress);
                    Geolocation geo = report.ipToGeolocationMap.get(ipAddress);
                    Double riskScore = report.ipRiskScores.get(ipAddress);
                    
                    writer.write(String.format("\nIP: %s | Location: %s | Risk Score: %.2f\n",
                        ipDisplay, geo != null ? geo : "Unknown", riskScore != null ? riskScore : 0.0));
                    writer.write("  Activity Log:\n");
                    
                    for (LogRecord log : entry.getValue()) {
                        writer.write(String.format("    [%s] %s %s -> %d | %s\n",
                            log.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            log.httpMethod, log.requestPath, log.httpStatusCode, log.messageDetails));
                    }
                }
                writer.write("\n");
            }
            
            writer.write("=".repeat(80) + "\n");
            writer.write("END OF REPORT\n");
            writer.write("=".repeat(80) + "\n");
            
        } catch (IOException e) {
            System.err.println(AnsiColors.RED_BOLD + "FATAL: Could not write report to " + filePath + AnsiColors.RESET);
            System.err.println(AnsiColors.RED + "Error: " + e.getMessage() + AnsiColors.RESET);
        }
    }
}
