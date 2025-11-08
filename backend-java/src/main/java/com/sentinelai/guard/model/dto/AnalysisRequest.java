package com.sentinelai.guard.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AnalysisRequest {
    @NotBlank(message = "Content cannot be empty")
    @Schema(description = "The content to be analyzed (URL or log entry)", example = "https://example.com")
    private String content;
    
    @Schema(description = "Additional context or metadata about the request", example = "User login attempt")
    private String context;
    
    @Schema(description = "List of tags for categorization", example = "[\"login\", \"high_priority\"]")
    private List<String> tags;
    
    @Schema(description = "Request timeout in seconds", example = "30")
    private Integer timeoutSeconds = 30;
}
