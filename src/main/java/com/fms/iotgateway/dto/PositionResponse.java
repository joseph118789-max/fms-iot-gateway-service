package com.fms.iotgateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * REST response DTO for position data.
 * Maps from domain Position to a clean API response.
 */
public record PositionResponse(
    String uuid,
    Long deviceId,
    @JsonProperty("fixTime") OffsetDateTime fixTime,
    @JsonProperty("receivedAt") OffsetDateTime receivedAt,
    Double latitude,
    Double longitude,
    Double altitude,
    Double speed,
    Double course,
    Double accuracy,
    Float batteryLevel
) {}
