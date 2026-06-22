package com.fms.iotgateway.dto;

import java.util.List;

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

/**
 * REST response DTO for list of positions.
 */
public record PositionListResponse(
    List<PositionResponse> positions,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
