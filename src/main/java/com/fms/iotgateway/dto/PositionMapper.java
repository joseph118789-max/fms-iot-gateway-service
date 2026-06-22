package com.fms.iotgateway.dto;

import com.fms.iotgateway.domain.Position;

/**
 * Mapper for converting between domain objects and DTOs.
 */
public final class PositionMapper {

    private PositionMapper() {}

    public static PositionResponse toResponse(Position position) {
        return new PositionResponse(
            position.id().toString(),
            position.deviceId(),
            position.serverTime().toString(),
            position.deviceTime() != null ? position.deviceTime().toString() : null,
            position.latitude(),
            position.longitude(),
            position.altitude(),
            position.speed(),
            position.course(),
            position.accuracy()
        );
    }
}
