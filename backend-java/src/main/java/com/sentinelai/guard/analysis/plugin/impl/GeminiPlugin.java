package com.sentinelai.guard.analysis.plugin.impl;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.sentinelai.guard.analysis.plugin.AnalysisPlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiPlugin implements AnalysisPlugin {
    
    @Value("${sentinel.plugins.gemini.enabled:false}")
    private boolean enabled;
    
    @Value("${sentinel.plugins.gemini.project-id:#{null}}")
    private String projectId;
    
    @Value("${sentinel.plugins.gemini.location:us-central1}")
    private String location;
    
    @Value("${sentinel.plugins.gemini.model-name:gemini-pro}")
    private String modelName;

    @Value("${sentinel.plugins.gemini.temperature:0.2}")
    private float temperature;
    
    @Value("${sentinel.plugins.gemini.max-output-tokens:1024}")
    private int maxOutputTokens;
    
    @Value("${sentinel.plugins.gemini.confidence-threshold:0.7}")
    private double confidenceThreshold;

    private VertexAI vertexAI;
    private GenerativeModel model;

    // --- THIS METHOD IS NOW CORRECTED ---
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Gemini plugin is disabled");
            return;
        }
        
        if (projectId == null || projectId.isBlank()) {
            log.warn("Gemini plugin is enabled but project-id is not configured");
            enabled = false;
            return;
        }
        
        
            // 1. Initialize the VertexAI client
            this.vertexAI = new VertexAI(projectId, location);
            
            // 2. Initialize the model
            this.model = new GenerativeModel(modelName, this.vertexAI);
            
            // Note: Temperature and maxOutputTokens will be set when making generateContent calls
            // These values will be used in the analyze() method when calling the model

            log.info("Initialized Gemini model: {} in project: {}", modelName, projectId);
            
        
    }

    @PreDestroy
    public void shutdown() {
        if (vertexAI != null) {
            log.info("Shutting down Gemini VertexAI client.");
            try {
                vertexAI.close();
            } catch (Exception e) {
                log.error("Error shutting down VertexAI client", e);
            }
        }
    }

    @Override
    public String getId() {
        return "gemini-ai";
    }

    @Override
    public String getName() {
        return "Gemini AI Analyzer";
    }

    @Override
    public String getDescription() {
        return "Uses Google's Gemini AI to analyze content for potential threats";
    }

    @Override
    public double getWeight() {
        return 0.2; // 20% weight in final score
    }

    @Override
    public boolean isEnabled() {
        return enabled && model != null;
    }

    @Override
    public PluginResult analyze(String url) {
        if (!isEnabled()) {
            return new PluginResult(0.0, "Gemini analysis skipped (plugin disabled or not configured)");
        }

        try {
            String prompt = String.format("""
                Analyze the following URL for potential security threats and provide a risk assessment.
                URL: %s
                
                Consider the following aspects:
                1. Domain reputation and history
                2. Potential phishing indicators
                3. Suspicious patterns in the URL
                4. Known malicious patterns
                
                Respond in JSON format with these fields:
                {
                    "risk_score": number (0.0 to 1.0, where 1.0 is most risky),
                    "reason": "Brief explanation of the risk assessment",
                    "confidence": number (0.0 to 1.0, your confidence in this assessment),
                    "indicators": ["list", "of", "suspicious", "indicators"]
                }
                """, url);
            
            GenerateContentResponse response = model.generateContent(prompt);
            String responseText = ResponseHandler.getText(response);
            
            double riskScore = extractDouble(responseText, "risk_score");
            double confidence = extractDouble(responseText, "confidence");
            String reason = extractString(responseText, "reason");
            
            if (confidence < confidenceThreshold) {
                return new PluginResult(0.0, "Low confidence in AI analysis");
            }
            
            double adjustedScore = riskScore * confidence;
            
            return new PluginResult(
                adjustedScore,
                "AI Analysis: " + (reason != null ? reason : "No specific threat indicators found")
            );
            
        } catch (Exception e) {
            log.error("Error in Gemini analysis for {}: {}", url, e.getMessage(), e);
            return new PluginResult(0.0, "AI analysis failed: " + e.getMessage());
        }
    }

    private double extractDouble(String text, String field) {
        try {
            String searchStr = "\"" + field + "\"\s*:\s*([0-9.]+)";
            Pattern pattern = Pattern.compile(searchStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract {} from response: {}", field, e.getMessage());
        }
        return field.equals("confidence") ? 1.0 : 0.0;
    }
    
    private String extractString(String text, String field) {
        try {
            String searchStr = "\"" + field + "\"\s*:\s*\"([^\"]+)\"";
            Pattern pattern = Pattern.compile(searchStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Failed to extract {} from response: {}", field, e.getMessage());
        }
        return null;
    }
}