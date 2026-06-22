package com.fms.iotgateway.dto;

import java.util.List;

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
