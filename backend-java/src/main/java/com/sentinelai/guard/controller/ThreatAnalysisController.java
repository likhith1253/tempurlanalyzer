package com.sentinelai.guard.controller;

import com.sentinelai.guard.model.dto.AnalysisRequest;
import com.sentinelai.guard.model.dto.AnalysisResponse;
import com.sentinelai.guard.service.ThreatAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Threat Analysis", description = "Endpoints for analyzing potential security threats")
public class ThreatAnalysisController {

    private final ThreatAnalysisService threatAnalysisService;

    @Operation(
        summary = "Analyze a URL or domain",
        description = "Performs security analysis on the provided URL or domain"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analysis completed successfully", 
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping(value = "/analyze/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponse> analyzeUrl(
            @Valid @RequestBody AnalysisRequest request) {
        log.info("Received URL analysis request for: {}", request.getContent());
        AnalysisResponse response = threatAnalysisService.analyzeUrl(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Analyze batch logs",
        description = "Processes and analyzes multiple log entries for security threats"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logs analyzed successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnalysisResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping(value = "/analyze/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalysisResponse>> analyzeLogs(
            @Valid @RequestBody List<AnalysisRequest> requests) {
        log.info("Received batch log analysis request with {} entries", requests.size());
        List<AnalysisResponse> responses = threatAnalysisService.analyzeLogs(requests);
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get current security alerts",
        description = "Retrieves a list of active security alerts"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnalysisResponse.class))))
    })
    @GetMapping(value = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalysisResponse>> getAlerts(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching {} most recent alerts", limit);
        List<AnalysisResponse> alerts = threatAnalysisService.getRecentAlerts(limit);
        return ResponseEntity.ok(alerts);
    }

    @Operation(
        summary = "List cached domain analyses",
        description = "Retrieves a list of previously analyzed domains with their results"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cached domains retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnalysisResponse.class))))
    })
    @GetMapping(value = "/domains", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalysisResponse>> getCachedDomains(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Fetching {} most recent domain analyses", limit);
        List<AnalysisResponse> domains = threatAnalysisService.getCachedDomainAnalyses(limit);
        return ResponseEntity.ok(domains);
    }
}
