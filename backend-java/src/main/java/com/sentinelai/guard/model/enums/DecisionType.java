package com.sentinelai.guard.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Security decision types")
public enum DecisionType {
    @Schema(description = "Content is safe and allowed")
    ALLOW,
    
    @Schema(description = "Content is potentially malicious, warning recommended")
    WARN,
    
    @Schema(description = "Content is malicious and should be blocked")
    BLOCK,
    
    @Schema(description = "Analysis could not be completed")
    UNKNOWN
}
