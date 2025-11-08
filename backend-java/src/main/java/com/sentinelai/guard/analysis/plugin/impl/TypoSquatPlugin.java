package com.sentinelai.guard.analysis.plugin.impl;

import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TypoSquatPlugin implements AnalysisPlugin {

    @Value("${sentinel.plugins.typosquat.enabled:true}")
    private boolean enabled;
    
    @Value("${sentinel.plugins.typosquat.whitelist:top-1000-domains.txt}")
    private String whitelistFile;
    
    @Value("${sentinel.plugins.typosquat.max-edit-distance:2}")
    private int maxEditDistance;
    
    @Value("${sentinel.plugins.typosquat.similarity-threshold:0.8}")
    private double similarityThreshold;
    
    private final Set<String> popularDomains = new HashSet<>();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();
    
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("TypoSquat plugin is disabled");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(whitelistFile).getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    popularDomains.add(extractDomainName(line));
                }
            }
            log.info("Loaded {} popular domains for typosquat detection", popularDomains.size());
            
        } catch (IOException e) {
            log.error("Failed to load whitelist file: {}", whitelistFile, e);
            enabled = false;
        }
    }
    
    @Override
    public String getId() {
        return "typosquat-detector";
    }

    @Override
    public String getName() {
        return "Typosquatting Detector";
    }

    @Override
    public String getDescription() {
        return "Detects potential typosquatting attempts by comparing domain similarity";
    }

    @Override
    public double getWeight() {
        return 0.1; // 10% weight in final score
    }

    @Override
    public boolean isEnabled() {
        return enabled && !popularDomains.isEmpty();
    }

    @Override
    public PluginResult analyze(String url) {
        if (!isEnabled()) {
            return new PluginResult(0.0, "Typosquat detection skipped (plugin disabled or no whitelist)");
        }
        
        try {
            String domain = extractDomain(url);
            if (domain == null) {
                return new PluginResult(0.0, "Invalid domain format");
            }
            
            String domainName = extractDomainName(domain);
            
            // Check for exact match in whitelist
            if (popularDomains.contains(domainName)) {
                return new PluginResult(0.0, "Domain is in the whitelist");
            }
            
            // Find closest match in popular domains
            Map<String, Integer> matches = new HashMap<>();
            for (String popular : popularDomains) {
                int distance = levenshtein.apply(domainName, popular);
                if (distance >= 0 && distance <= maxEditDistance) {
                    matches.put(popular, distance);
                }
            }
            
            if (!matches.isEmpty()) {
                // Sort by edit distance (ascending)
                List<Map.Entry<String, Integer>> sortedMatches = matches.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toList());
                
                String closestMatch = sortedMatches.get(0).getKey();
                int distance = sortedMatches.get(0).getValue();
                
                // Calculate similarity score (0.0 to 1.0, where 1.0 is identical)
                double maxLength = Math.max(domainName.length(), closestMatch.length());
                double similarity = 1.0 - (distance / maxLength);
                
                if (similarity >= similarityThreshold) {
                    double riskScore = 0.3 + (0.7 * similarity); // Base 0.3 + up to 0.7 for similarity
                    return new PluginResult(
                        Math.min(1.0, riskScore),
                        String.format("Possible typosquatting: '%s' is similar to '%s' (distance: %d, similarity: %.1f%%)",
                            domainName, closestMatch, distance, similarity * 100)
                    );
                }
            }
            
            return new PluginResult(0.0, "No typosquatting detected");
            
        } catch (Exception e) {
            log.warn("Error in typosquat detection for {}: {}", url, e.getMessage());
            return new PluginResult(0.2, "Typosquat check failed: " + e.getMessage());
        }
    }
    
    private String extractDomain(String url) {
        try {
            // Remove protocol and path
            String domain = url.replaceFirst("^(https?://)?(www\\.)?", "");
            // Remove port and path
            domain = domain.split("[/:?]+")[0];
            return domain.toLowerCase().trim();
        } catch (Exception e) {
            log.warn("Failed to extract domain from URL: {}", url, e);
            return null;
        }
    }
    
    private String extractDomainName(String domain) {
        // Remove subdomains (e.g., sub.domain.com -> domain.com)
        String[] parts = domain.split("\\.");
        if (parts.length <= 2) {
            return domain;
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
