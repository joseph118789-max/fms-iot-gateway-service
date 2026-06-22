package com.fms.iotgateway.dto;

/**
 * REST response DTO for position data.
 */
public record PositionResponse(
    String id,
    Long deviceId,
    String serverTime,
    String deviceTime,
    Double latitude,
    Double longitude,
    Double altitude,
    Double speed,
    Double course,
    Double accuracy
) {}
