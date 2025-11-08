package com.sentinelai.guard.model.report;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReportData {
    private String reportId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReportType reportType;
    private ReportSummary summary;
    private List<ThreatAnalysis> topThreats;
    private Map<String, Long> domainStats;
    private Map<String, Long> decisionStats;
    private String generatedBy;
    
    public enum ReportType {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }
    
    @Data
    @Builder
    public static class ReportSummary {
        private long totalUrlsAnalyzed;
        private long allowedCount;
        private long warnedCount;
        private long blockedCount;
        private long uniqueDomains;
    }
    
    @Data
    @Builder
    public static class ThreatAnalysis {
        private String url;
        private String domain;
        private String threatType;
        private String description;
        private String riskLevel;
        private String aiExplanation;
        private String timestamp;
    }
}
