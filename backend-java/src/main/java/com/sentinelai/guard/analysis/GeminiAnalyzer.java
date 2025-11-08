package com.sentinelai.guard.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Analyzes domains and HTML content using the Gemini API for security analysis.
 * Implements caching to reduce API calls and costs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAnalyzer {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    private static final long CACHE_EXPIRY_MINUTES = 60; // 1 hour cache

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Simple in-memory cache with domain as key and cache entry as value
    private final Map<String, CacheEntry> analysisCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Gemini analysis will be disabled.");
        }
    }

    /**
     * Analyzes a domain and its HTML content for security risks using Gemini API.
     * 
     * @param domain The domain to analyze
     * @param htmlSnippet HTML content of the domain (can be a snippet)
     * @return AnalysisResult containing risk score and analysis details
     * @throws GeminiAnalysisException if analysis fails
     */
    public AnalysisResult analyzeDomain(String domain, String htmlSnippet) throws GeminiAnalysisException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAnalysisException("Gemini API key is not configured");
        }

        // Check cache first
        CacheEntry cachedResult = analysisCache.get(domain);
        if (cachedResult != null && !cachedResult.isExpired()) {
            log.debug("Returning cached analysis result for domain: {}", domain);
            return cachedResult.getResult();
        }

        try {
            // Prepare the request to Gemini API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Construct the prompt
            String prompt = String.format("""
                Analyze the following domain and metadata and return a JSON with fields {risk_score, reason, indicators}.
                
                Domain: %s
                HTML Content: %s
                
                Return only the JSON response, no other text.
                """, domain, htmlSnippet);

            // Build the request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);

            // Add safety settings to ensure we get a response
            requestBody.putObject("safetySettings")
                .putArray("safetyRatings")
                .addObject()
                .put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                .put("threshold", "BLOCK_NONE");

            // Add generation config
            requestBody.putObject("generationConfig")
                .put("temperature", 0.7)
                .put("topP", 0.8)
                .put("topK", 40);

            // Build the full URL with API key
            String url = String.format("%s?key=%s", GEMINI_API_URL, apiKey);
            
            // Send the request
            log.debug("Sending analysis request to Gemini API for domain: {}", domain);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                String.class
            );

            // Process the response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse the Gemini response
                ObjectNode responseJson = (ObjectNode) objectMapper.readTree(response.getBody());
                String textResponse = responseJson.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

                // Parse the JSON response from the text
                ObjectNode analysisJson = (ObjectNode) objectMapper.readTree(textResponse);
                
                // Create and cache the result
                AnalysisResult result = new AnalysisResult(
                    analysisJson.path("risk_score").asDouble(),
                    analysisJson.path("reason").asText(),
                    analysisJson.path("indicators").asText()
                );
                
                // Update cache
                analysisCache.put(domain, new CacheEntry(result));
                
                return result;
            } else {
                throw new GeminiAnalysisException("Failed to analyze domain: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            throw new GeminiAnalysisException("Network timeout while connecting to Gemini API", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Gemini API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new GeminiAnalysisException("Gemini API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error analyzing domain with Gemini: {}", domain, e);
            throw new GeminiAnalysisException("Error analyzing domain: " + e.getMessage(), e);
        }
    }

    /**
     * Clears the analysis cache.
     */
    public void clearCache() {
        analysisCache.clear();
        log.info("Cleared Gemini analysis cache");
    }

    /**
     * Represents the result of a domain analysis.
     */
    public record AnalysisResult(
        double riskScore,  // Normalized to [0,1]
        String reason,     // Explanation of the risk
        String indicators  // Comma-separated list of risk indicators
    ) {
        public AnalysisResult {
            // Ensure risk score is within [0,1]
            riskScore = Math.max(0, Math.min(1, riskScore));
        }
    }

    /**
     * Custom exception for Gemini analysis errors.
     */
    public static class GeminiAnalysisException extends Exception {
        public GeminiAnalysisException(String message) {
            super(message);
        }

        public GeminiAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Cache entry with expiration support.
     */
    private static class CacheEntry {
        private final AnalysisResult result;
        private final long timestamp;

        public CacheEntry(AnalysisResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        public AnalysisResult getResult() {
            return result;
        }

        public boolean isExpired() {
            long ageInMinutes = TimeUnit.MILLISECONDS.toMinutes(
                System.currentTimeMillis() - timestamp);
            return ageInMinutes >= CACHE_EXPIRY_MINUTES;
        }
    }
}
