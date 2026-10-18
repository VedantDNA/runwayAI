package com.runway.corebanking.controllers;

import com.runway.corebanking.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service health and readiness check API")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "Check service health",
            description = "Returns current operational status and server timestamp for runway-core-banking service.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Service is up and operational")
            }
    )
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .service("runway-core-banking")
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
