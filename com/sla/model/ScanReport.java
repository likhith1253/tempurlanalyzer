package com.sla.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScanReport {
    public List<LogRecord> initiallyBlockedLogs = new ArrayList<>();
    public Map<String, LocalDateTime> newlyBlockedIPs = new LinkedHashMap<>();
    public Map<String, Double> ipRiskScores = new HashMap<>();
    public Set<String> finalBlacklist = new HashSet<>();
    public Set<String> finalSuspiciousIps = new HashSet<>();
    public Map<String, String> performanceMetrics = new LinkedHashMap<>();
    public Map<String, Long> suspiciousEventFrequency = new HashMap<>();
    public Map<String, Geolocation> ipToGeolocationMap = new HashMap<>();
    public Map<String, Long> suspiciousEventsByCountry = new HashMap<>();
    public Map<String, List<LogRecord>> suspiciousIPActivity = new HashMap<>();
}
