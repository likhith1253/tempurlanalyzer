package com.sentinelai.guard.analysis.plugin.impl;

import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class DomainAgePlugin implements AnalysisPlugin {

    @Value("${sentinel.plugins.domain-age.enabled:true}")
    private boolean enabled;
    
    @Value("${sentinel.plugins.domain-age.threshold-days:90}")
    private int thresholdDays;
    
    @Override
    public String getId() {
        return "domain-age";
    }

    @Override
    public String getName() {
        return "Domain Age Analyzer";
    }

    @Override
    public String getDescription() {
        return "Analyzes domain registration age and flags recently registered domains";
    }

    @Override
    public double getWeight() {
        return 0.2; // 20% weight in final score
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public PluginResult analyze(String url) {
        if (!enabled) {
            return new PluginResult(0.0, "Domain age check skipped (plugin disabled)");
        }

        try {
            // In a real implementation, we would query WHOIS or a domain age service
            // For this example, we'll simulate the behavior
            String domain = extractDomain(url);
            if (domain == null) {
                return new PluginResult(0.5, "Invalid domain format");
            }

            // Simulate domain age lookup
            Date creationDate = simulateWhoisLookup(domain);
            if (creationDate == null) {
                return new PluginResult(0.3, "Could not determine domain age");
            }

            LocalDate now = LocalDate.now();
            LocalDate createdDate = creationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            long ageInDays = ChronoUnit.DAYS.between(createdDate, now);
            
            if (ageInDays < 0) {
                return new PluginResult(0.8, "Suspicious domain: Creation date in the future");
            } else if (ageInDays < thresholdDays) {
                double risk = 0.7 * (1 - (ageInDays / (double) thresholdDays));
                return new PluginResult(risk, String.format(
                    "Recently registered domain: %d days old (threshold: %d days)", 
                    ageInDays, thresholdDays));
            } else {
                return new PluginResult(0.0, 
                    String.format("Domain is %d days old (above threshold of %d days)", 
                    ageInDays, thresholdDays));
            }
            
        } catch (Exception e) {
            log.warn("Error analyzing domain age for {}: {}", url, e.getMessage());
            return new PluginResult(0.2, "Error checking domain age: " + e.getMessage());
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
    
    // Simulates a WHOIS lookup - replace with actual WHOIS API call in production
    private Date simulateWhoisLookup(String domain) {
        try {
            // In a real implementation, this would call a WHOIS service or database
            // For simulation, we'll return a date between 1 and 1000 days ago
            long randomDaysAgo = (long) (Math.random() * 1000);
            if (randomDaysAgo < 10) {
                // Occasionally return null to simulate failed lookup
                if (Math.random() < 0.1) {
                    return null;
                }
                // Occasionally return future date to test that case
                if (Math.random() < 0.05) {
                    return Date.from(java.time.LocalDate.now()
                        .plusDays((long)(Math.random() * 30) + 1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant());
                }
            }
            return Date.from(java.time.LocalDate.now()
                .minusDays(randomDaysAgo)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
        } catch (Exception e) {
            log.warn("Error in simulated WHOIS lookup for {}: {}", domain, e.getMessage());
            return null;
        }
    }
}
