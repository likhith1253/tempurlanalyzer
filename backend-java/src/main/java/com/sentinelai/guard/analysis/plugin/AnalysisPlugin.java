package com.sentinelai.guard.analysis.plugin;

/**
 * Base interface for all analysis plugins.
 * Each plugin analyzes a specific aspect of a URL or domain and returns a score.
 */
public interface AnalysisPlugin {
    
    /**
     * Unique identifier for the plugin
     */
    String getId();
    
    /**
     * Human-readable name of the plugin
     */
    String getName();
    
    /**
     * Description of what this plugin checks
     */
    String getDescription();
    
    /**
     * Weight of this plugin's score in the final risk calculation (0.0 to 1.0)
     */
    double getWeight();
    
    /**
     * Analyze the given URL and return a plugin result
     * @param url The URL to analyze
     * @return PluginResult containing the score and reasoning
     */
    PluginResult analyze(String url);
    
    /**
     * Whether this plugin is enabled and should be used in analysis
     */
    boolean isEnabled();
    
    /**
     * Result of a plugin analysis
     */
    record PluginResult(double score, String reason) {
        public PluginResult {
            // Ensure score is between 0.0 and 1.0
            score = Math.max(0.0, Math.min(1.0, score));
        }
    }
}
