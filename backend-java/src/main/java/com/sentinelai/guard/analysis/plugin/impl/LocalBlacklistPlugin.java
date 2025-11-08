package com.sentinelai.guard.analysis.plugin.impl;

import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class LocalBlacklistPlugin implements AnalysisPlugin {
    
    @Value("${sentinel.plugins.blacklist.enabled:true}")
    private boolean enabled;
    
    @Value("${sentinel.plugins.blacklist.file:blacklist.txt}")
    private String blacklistFile;
    
    private final Set<String> blacklistedDomains = new HashSet<>();
    
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Blacklist plugin is disabled");
            return;
        }
        
        try (InputStream is = new ClassPathResource(blacklistFile).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    blacklistedDomains.add(line);
                }
            }
            log.info("Loaded {} blacklisted domains", blacklistedDomains.size());
            
        } catch (IOException e) {
            log.error("Failed to load blacklist file: {}", blacklistFile, e);
            enabled = false;
        }
    }
    
    @Override
    public String getId() {
        return "local-blacklist";
    }
    
    @Override
    public String getName() {
        return "Local Domain Blacklist";
    }
    
    @Override
    public String getDescription() {
        return "Checks if the domain is in the local blacklist";
    }
    
    @Override
    public double getWeight() {
        return 0.4; // 40% weight in final score
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public PluginResult analyze(String url) {
        if (!enabled) {
            return new PluginResult(0.0, "Blacklist check skipped (plugin disabled)");
        }
        
        String domain = extractDomain(url);
        if (domain == null) {
            return new PluginResult(0.5, "Invalid domain format");
        }
        
        if (isBlacklisted(domain)) {
            log.warn("Blacklisted domain detected: {}", domain);
            return new PluginResult(1.0, "Domain is blacklisted: " + domain);
        }
        
        return new PluginResult(0.0, "Domain not found in blacklist");
    }
    
    private boolean isBlacklisted(String domain) {
        // Check exact match
        if (blacklistedDomains.contains(domain)) {
            return true;
        }
        
        // Check subdomains (e.g., if evil.com is blacklisted, sub.evil.com should be too)
String[] parts = domain.split("\\.");
        for (int i = 1; i < parts.length; i++) {
            String parentDomain = String.join(".", java.util.Arrays.copyOfRange(parts, i, parts.length));
            if (blacklistedDomains.contains(parentDomain)) {
                return true;
            }
        }
        
        return false;
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
}
