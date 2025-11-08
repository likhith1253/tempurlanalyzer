package com.sentinelai.guard.controller;

import com.sentinelai.guard.model.report.ReportData;
import com.sentinelai.guard.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
@Tag(name = "Report Generation", description = "API for generating and managing security reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @Operation(summary = "Generate a new security report")
    public CompletableFuture<ResponseEntity<ReportGenerationResponse>> generateReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "WEEKLY") ReportData.ReportType reportType,
            Authentication authentication) {
        
        // Set default date range if not provided
        LocalDate reportEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate reportStartDate = startDate != null ? startDate : 
            reportType == ReportData.ReportType.DAILY ? reportEndDate.minusDays(1) :
            reportType == ReportData.ReportType.WEEKLY ? reportEndDate.minusWeeks(1) :
            reportType == ReportData.ReportType.MONTHLY ? reportEndDate.minusMonths(1) :
            reportEndDate.minusYears(1);

        String userId = authentication != null ? authentication.getName() : "system";
        
        // In a real implementation, you would fetch this data from your database
        ReportData reportData = prepareReportData(reportStartDate, reportEndDate, reportType, userId);
        
        return reportService.generateAndUploadReport(reportData)
                .thenApply(downloadUrl -> {
                    log.info("Report generated successfully. Download URL: {}", downloadUrl);
                    return ResponseEntity.ok(new ReportGenerationResponse(
                            reportData.getReportId(),
                            downloadUrl,
                            "Report generated successfully"
                    ));
                });
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report details by ID")
    public ResponseEntity<ReportGenerationResponse> getReport(@PathVariable String reportId) {
        // In a real implementation, you would fetch this from your database
        String downloadUrl = String.format("%s/reports/%s.pdf", 
            reportService.getStorageBaseUrl(), 
            reportId);
            
        return ResponseEntity.ok(new ReportGenerationResponse(
                reportId,
                downloadUrl,
                "Report retrieved successfully"
        ));
    }

    private ReportData prepareReportData(LocalDate startDate, LocalDate endDate, 
                                       ReportData.ReportType reportType, String userId) {
        // In a real implementation, you would fetch this data from your database
        // This is just a mock implementation
        
        ReportData.ReportSummary summary = ReportData.ReportSummary.builder()
                .totalUrlsAnalyzed(1245)
                .allowedCount(850)
                .warnedCount(250)
                .blockedCount(145)
                .uniqueDomains(320)
                .build();

        return ReportData.builder()
                .reportId(UUID.randomUUID().toString())
                .startDate(startDate)
                .endDate(endDate)
                .reportType(reportType)
                .summary(summary)
                .decisionStats(Map.of(
                        "ALLOW", summary.getAllowedCount(),
                        "WARN", summary.getWarnedCount(),
                        "BLOCK", summary.getBlockedCount()
                ))
                .domainStats(Map.of(
                        "example.com", 150L,
                        "test.org", 120L,
                        "malicious-site.com", 45L,
                        "phishing-site.net", 38L,
                        "legit-site.io", 32L
                ))
                .topThreats(List.of(
                        createMockThreat("https://phishing-site.net/login", "phishing-site.net", "Phishing", "High",
                                "This site is known for credential harvesting attacks.",
                                "AI analysis indicates a 98% probability of this being a phishing site based on known patterns and user reports."),
                        createMockThreat("http://malware-download.com/update.exe", "malware-download.com", "Malware", "Critical",
                                "Distributes trojanized software updates.",
                                "Behavioral analysis shows this domain is associated with drive-by downloads and malware distribution."),
                        createMockThreat("https://fake-bank.com/account/login", "fake-bank.com", "Spoofing", "High",
                                "Imitates legitimate banking sites to steal credentials.",
                                "Domain age and SSL certificate analysis reveal this is a recently registered site mimicking a legitimate bank.")
                ))
                .generatedBy(userId)
                .build();
    }

    private ReportData.ThreatAnalysis createMockThreat(String url, String domain, String type, 
                                                      String riskLevel, String description, String aiExplanation) {
        return ReportData.ThreatAnalysis.builder()
                .url(url)
                .domain(domain)
                .threatType(type)
                .riskLevel(riskLevel)
                .description(description)
                .aiExplanation(aiExplanation)
                .timestamp(LocalDate.now().minusDays(1).toString())
                .build();
    }

    public record ReportGenerationResponse(
            String reportId,
            String downloadUrl,
            String message
    ) {}
}
