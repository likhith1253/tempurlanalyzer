package com.sla.engine;

import com.sla.app.AppConfiguration;
import com.sla.dsa.SelfBalancingSearchTree;
import com.sla.model.Geolocation;
import com.sla.model.LogRecord;
import com.sla.model.ScanReport;
import com.sla.service.BlacklistManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ThreatAnalysisEngine {
    private final BlacklistManager blacklistManager;
    private final Map<String, Geolocation> geoData;
    private final List<Pattern> confidentialPatterns;
    private final AppConfiguration appConfig;
    private Map<String, Double> ipRiskScores;
    private LiveAlertCallback liveAlertCallback;
    
    public ThreatAnalysisEngine(BlacklistManager blacklistManager, Map<String, Geolocation> geoData,
                                List<Pattern> confidentialPatterns, AppConfiguration appConfig) {
        this.blacklistManager = blacklistManager;
        this.geoData = geoData;
        this.confidentialPatterns = confidentialPatterns;
        this.appConfig = appConfig;
    }
    
    public void setLiveAlertCallback(LiveAlertCallback callback) {
        this.liveAlertCallback = callback;
    }
    
    public ScanReport analyze(SelfBalancingSearchTree<LogRecord> logTree) {
        ipRiskScores = new HashMap<>();
        ScanReport report = new ScanReport();
        Map<String, List<LogRecord>> allLogsByIP = new HashMap<>();
        
        for (LogRecord log : logTree.inOrderTraversal()) {
            report.ipToGeolocationMap.putIfAbsent(log.ipSourceAddress, 
                geoData.get(BlacklistManager.getIpPrefix(log.ipSourceAddress)));
            allLogsByIP.computeIfAbsent(log.ipSourceAddress, k -> new ArrayList<>()).add(log);
            
            if (blacklistManager.isBlacklisted(log.ipSourceAddress)) {
                report.initiallyBlockedLogs.add(log);
                continue;
            }
            
            calculateSingleLogRiskScore(log, report);
            updateScanSummaries(report, log);
        }
        
        report.ipRiskScores.putAll(ipRiskScores);
        report.finalBlacklist.addAll(blacklistManager.getActiveBlacklist());
        report.finalSuspiciousIps.addAll(report.suspiciousEventFrequency.keySet());
        
        // Populate suspiciousIPActivity for the final detailed report section
        for (String ipAddress : report.finalSuspiciousIps) {
            // Only include IP if its final calculated risk score meets or exceeds the threshold
            if (ipRiskScores.getOrDefault(ipAddress, 0.0) >= appConfig.getThreatScoreThreshold()) {
                report.suspiciousIPActivity.put(ipAddress, allLogsByIP.get(ipAddress));
            }
        }
        
        return report;
    }
    
    private void calculateSingleLogRiskScore(LogRecord log, ScanReport report) {
        double currentScore = ipRiskScores.getOrDefault(log.ipSourceAddress, 0.0);
        double points = 0.0;
        String detailsLower = log.messageDetails.toLowerCase();
        String pathLower = log.requestPath.toLowerCase();
        
        // 1. High Risk: Confidential Resource Access (0.30 points)
        if (isConfidentialResource(log.requestPath)) {
            points += 0.30;
        }
        
        // 2. Medium-High Risk: Explicit Attack Indicators (0.25 points)
        if (detailsLower.contains("sql injection") || detailsLower.contains("traversal attempt") ||
            pathLower.contains("passwd") || pathLower.contains("/etc/") || pathLower.contains(".env")) {
            points += 0.25;
        }
        
        // 3. Medium Risk: Failed Login Attempt (0.15 points)
        if (detailsLower.contains("failed login")) {
            points += 0.15;
        }
        
        // 4. Low Risk: HTTP Status Codes
        if (log.httpStatusCode >= 500) {
            points += 0.10;
        } else if (log.httpStatusCode >= 400 && log.httpStatusCode < 500) {
            points += 0.05;
        }
        
        double newScore = Math.min(currentScore + points, 1.0);
        ipRiskScores.put(log.ipSourceAddress, newScore);
        
        if (newScore > appConfig.getThreatScoreThreshold() && 
            currentScore <= appConfig.getThreatScoreThreshold()) {
            blacklistManager.add(log.ipSourceAddress);
            report.newlyBlockedIPs.putIfAbsent(log.ipSourceAddress, log.timestamp);
            if (liveAlertCallback != null) {
                liveAlertCallback.onThreatThresholdCrossed(log.ipSourceAddress, newScore, log.timestamp);
            }
        }
    }
    
    private void updateScanSummaries(ScanReport report, LogRecord log) {
        // Count events for any abnormal status code (>=400) or explicit attack indicators
        if (log.httpStatusCode >= 400 || 
            log.messageDetails.toLowerCase().contains("injection") || 
            log.messageDetails.toLowerCase().contains("traversal")) {
            report.suspiciousEventFrequency.merge(log.ipSourceAddress, 1L, Long::sum);
            
            Geolocation geo = geoData.get(BlacklistManager.getIpPrefix(log.ipSourceAddress));
            if (geo != null) {
                report.suspiciousEventsByCountry.merge(geo.country, 1L, Long::sum);
            }
        }
    }
    
    private boolean isConfidentialResource(String requestPath) {
        for (Pattern pattern : confidentialPatterns) {
            if (pattern.matcher(requestPath).find()) {
                return true;
            }
        }
        return false;
    }
    
    public Map<String, Double> getIpRiskScores() {
        return ipRiskScores;
    }
}
