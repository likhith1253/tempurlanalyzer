package com.sentinelai.guard.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "Standard error response format")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type", example = "VALIDATION_ERROR")
    private String error;
    
    @Schema(description = "Human-readable error message", 
            example = "Validation failed for request parameters")
    private String message;
    
    @Schema(description = "Detailed error messages")
    private List<String> details;
    
    @Schema(description = "Timestamp when the error occurred", 
            example = "2025-11-07T21:18:30.123+05:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;
    
    @Schema(description = "API endpoint where the error occurred", 
            example = "/api/analyze/url")
    private String path;
}
