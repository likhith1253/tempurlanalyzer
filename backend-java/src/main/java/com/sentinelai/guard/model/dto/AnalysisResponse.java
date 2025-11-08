package com.sentinelai.guard.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sentinelai.guard.model.enums.DecisionType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Analysis response containing the security assessment")
public class AnalysisResponse {
    
    @Schema(description = "The analyzed content (URL or log entry)", 
            example = "https://example.com")
    private String content;
    
    @Schema(implementation = DecisionType.class, 
            description = "Security decision for the analyzed content", 
            example = "BLOCK")
    private DecisionType decision;
    
    @Schema(description = "Risk score between 0.0 (safe) to 1.0 (malicious)", 
            example = "0.85")
    private Double riskScore;
    
    @Schema(description = "Human-readable reason for the decision", 
            example = "Domain is known for phishing activities")
    private String reason;
    
    @ArraySchema(
        arraySchema = @Schema(description = "Supporting evidence for the decision"),
        schema = @Schema(description = "Evidence item", example = "IP 192.168.1.1 is in the blacklist")
    )
    private List<String> evidence;
    
    @Schema(description = "Timestamp when the analysis was performed", 
            example = "2025-11-07T21:18:30.123+05:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;
    
    @Schema(description = "Unique identifier for this analysis", 
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String analysisId;
    
    @Schema(description = "Additional metadata about the analysis")
    private Object metadata;
}
