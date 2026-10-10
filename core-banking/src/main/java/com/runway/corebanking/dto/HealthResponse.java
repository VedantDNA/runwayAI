package com.runway.corebanking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health status response payload")
public class HealthResponse {

    @Schema(description = "Overall status indicator", example = "UP")
    private String status;

    @Schema(description = "Service name", example = "runway-core-banking")
    private String service;

    @Schema(description = "Current server timestamp in ISO-8601", example = "2026-10-10T20:00:00Z")
    private OffsetDateTime timestamp;
}
