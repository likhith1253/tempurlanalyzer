package com.sentinelai.guard.service;

import com.sentinelai.guard.model.dto.AnalysisRequest;
import com.sentinelai.guard.model.dto.AnalysisResponse;

import java.util.List;

/**
 * Service interface for threat analysis operations.
 */
public interface ThreatAnalysisService {
    
    /**
     * Analyzes a single URL or domain for potential security threats.
     *
     * @param request The analysis request containing the URL/domain to analyze
     * @return Analysis response with security assessment
     */
    AnalysisResponse analyzeUrl(AnalysisRequest request);
    
    /**
     * Analyzes multiple log entries in batch for security threats.
     *
     * @param requests List of analysis requests for log entries
     * @return List of analysis responses for each log entry
     */
    List<AnalysisResponse> analyzeLogs(List<AnalysisRequest> requests);
    
    /**
     * Retrieves recent security alerts.
     *
     * @param limit Maximum number of alerts to return
     * @return List of recent security alerts
     */
    List<AnalysisResponse> getRecentAlerts(int limit);
    
    /**
     * Retrieves cached domain analysis results.
     *
     * @param limit Maximum number of domain analyses to return
     * @return List of cached domain analyses
     */
    List<AnalysisResponse> getCachedDomainAnalyses(int limit);
}
