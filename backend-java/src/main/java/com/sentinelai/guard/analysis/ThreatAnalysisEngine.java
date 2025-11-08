package com.sentinelai.guard.analysis;

import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import com.sentinelai.guard.analysis.plugin.impl.GeminiPlugin;
import com.sentinelai.guard.model.dto.AnalysisResponse;
import com.sentinelai.guard.model.enums.DecisionType;
import com.sentinelai.guard.exception.ThreatAnalysisException; // Import the new exception
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatAnalysisEngine {

    private final List<AnalysisPlugin> plugins;
    private final GeminiPlugin geminiPlugin;
    
    /**
     * Analyze a URL using all available plugins
     */
    public AnalysisResponse analyzeUrl(String url) {
        log.info("Starting analysis for URL: {}", url);
        
        // List for our *internal* wrapper class
        List<CompletableFuture<PluginResult>> futures = plugins.stream()
            .filter(AnalysisPlugin::isEnabled)
            .map(plugin -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Running plugin: {} on URL: {}", plugin.getId(), url);
                    // Run the plugin
                    AnalysisPlugin.PluginResult pluginResult = plugin.analyze(url);
                    // Wrap the result in our internal class to store the ID
                    return new PluginResult(plugin.getId(), pluginResult.score(), pluginResult.reason());
                } catch (Exception e) {
                    log.error("Plugin {} failed for URL {}: {}", 
                        plugin.getId(), url, e.getMessage(), e);
                    // Wrap the error in our internal class
                    return new PluginResult(plugin.getId(), 0.0, "Plugin error: " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());
        
        // Wait for all plugins to complete
        List<PluginResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        
        // Calculate weighted score
        double weightedScore = calculateWeightedScore(results);
        
        if (shouldConsultGemini(weightedScore)) {
            log.debug("Score {} is in uncertain range, consulting Gemini AI", weightedScore);
            AnalysisPlugin.PluginResult geminiResult = geminiPlugin.analyze(url);
            
            if (geminiResult.score() > 0.0) {
                double geminiAdjustedScore = (weightedScore * 0.7) + (geminiResult.score() * 0.3);
                log.debug("Gemini adjusted score from {} to {}", weightedScore, geminiAdjustedScore);
                weightedScore = geminiAdjustedScore;
                // Add Gemini's result to the list for evidence
                results.add(new PluginResult(geminiPlugin.getId(), geminiResult.score(), geminiResult.reason()));
            }
        }
        
        DecisionType decision = determineDecision(weightedScore);
        
        return AnalysisResponse.builder()
            .content(url)
            .decision(decision)
            .riskScore(weightedScore)
            .reason(generateReason(decision, results))
            .evidence(generateEvidence(results))
            .timestamp(OffsetDateTime.now())
            .analysisId(UUID.randomUUID().toString())
            .build();
    }
    
    private double calculateWeightedScore(List<PluginResult> results) {
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        // Use a map for efficient plugin lookup
        Map<String, AnalysisPlugin> pluginMap = plugins.stream()
            .collect(Collectors.toMap(AnalysisPlugin::getId, plugin -> plugin));
        
        for (PluginResult result : results) {
            AnalysisPlugin plugin = pluginMap.get(result.pluginId());
            if (plugin != null && plugin.isEnabled()) {
                double weight = plugin.getWeight();
                weightedSum += result.score() * weight;
                totalWeight += weight;
            }
        }
        
        if (totalWeight == 0) return 0.0;
        return weightedSum / totalWeight;
    }
    
    private boolean shouldConsultGemini(double score) {
        return score > 0.3 && score < 0.7 && geminiPlugin.isEnabled();
    }
    
    private DecisionType determineDecision(double score) {
        if (score >= 0.7) {
            return DecisionType.BLOCK;
        } else if (score >= 0.4) {
            return DecisionType.WARN;
        } else {
            return DecisionType.ALLOW;
        }
    }
    
    private String generateReason(DecisionType decision, List<PluginResult> results) {
        if (results.isEmpty()) {
            return "No analysis results available";
        }
        
        Optional<PluginResult> highest = results.stream()
            .max(Comparator.comparingDouble(PluginResult::score));
            
        if (highest.isPresent() && highest.get().score() > 0.5) {
            return highest.get().reason();
        }
        
        switch (decision) {
            case BLOCK: return "High risk indicators detected";
            case WARN: return "Potential risk factors identified";
            default: return "No significant threats detected";
        }
    }
    
    private List<String> generateEvidence(List<PluginResult> results) {
        return results.stream()
            .filter(r -> r.score() > 0.3) 
            .map(r -> String.format("[%s] %s (score: %.2f)", 
                r.pluginId(), 
                r.reason(), 
                r.score()))
            .collect(Collectors.toList());
    }
    
    /**
     * Internal wrapper class to store pluginId with the result.
     * This class does NOT implement AnalysisPlugin.PluginResult.
     */
    private static class PluginResult {
        private final String pluginId;
        private final double score;
        private final String reason;
        
        public PluginResult(String pluginId, double score, String reason) {
            this.pluginId = pluginId;
            this.score = Math.max(0.0, Math.min(1.0, score));
            this.reason = reason != null ? reason : "";
        }
        
        public double score() { return score; }
        public String reason() { return reason; }
        public String pluginId() { return pluginId; }
    }
}