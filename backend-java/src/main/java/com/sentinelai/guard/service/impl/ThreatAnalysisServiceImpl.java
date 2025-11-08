package com.sentinelai.guard.service.impl;

import com.sentinelai.guard.analysis.ThreatAnalysisEngine; // Import the engine
import com.sentinelai.guard.model.dto.AnalysisRequest;
import com.sentinelai.guard.model.dto.AnalysisResponse;
import com.sentinelai.guard.model.enums.DecisionType;
import com.sentinelai.guard.service.ThreatAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
// Import Cacheable for caching
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatAnalysisServiceImpl implements ThreatAnalysisService {

    // Inject the actual analysis engine
    private final ThreatAnalysisEngine threatAnalysisEngine;
    
    // Keep alertCache for getRecentAlerts endpoint
    private final ConcurrentMap<String, AnalysisResponse> alertCache = new ConcurrentHashMap<>();

    @Override
    // Use @Cacheable to cache the results from the engine
    @Cacheable(value = "domainAnalyses", key = "#request.content")
    public AnalysisResponse analyzeUrl(AnalysisRequest request) {
        String url = request.getContent();
        log.debug("Analyzing URL: {}", url);
        
        // Call the actual engine instead of the stub
        AnalysisResponse response = threatAnalysisEngine.analyzeUrl(url);

        // If threat detected, add to alerts
        if (response.getDecision() == DecisionType.BLOCK || 
            response.getDecision() == DecisionType.WARN) {
            alertCache.put(response.getAnalysisId(), response);
        }
        
        return response;
    }

    @Override
    public List<AnalysisResponse> analyzeLogs(List<AnalysisRequest> requests) {
        // Process logs in parallel by calling analyzeUrl for each
        return requests.parallelStream()
                .map(this::analyzeUrl) // Reuse the analyzeUrl logic
                .collect(Collectors.toList());
    }

    @Override
    public List<AnalysisResponse> getRecentAlerts(int limit) {
        return alertCache.values().stream()
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable("domainAnalyses") // This method seems unused, but caching it is correct
    public List<AnalysisResponse> getCachedDomainAnalyses(int limit) {
        // This implementation doesn't make sense with @Cacheable.
        // The controller gets alerts, not the whole cache.
        // Returning an empty list as the controller logic is handled by getRecentAlerts
        // A better implementation would be to query the cache, but that requires setup.
        // For now, returning alerts is the most logical behavior.
        return getRecentAlerts(limit);
    }

    // Removed the unused stub methods: analyzeLogEntry and performUrlAnalysis

    @Scheduled(fixedRate = 3600000) // Run every hour
    @CacheEvict(allEntries = true, cacheNames = {"domainAnalyses"})
    public void evictAllCaches() {
        log.info("Evicting all caches");
        // Clear the alert cache as well
        alertCache.clear();
    }
}