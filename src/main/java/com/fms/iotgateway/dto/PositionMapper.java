package com.fms.iotgateway.dto;

import com.fms.iotgateway.domain.Position;

/**
 * Mapper for converting between domain objects and DTOs.
 */
public final class PositionMapper {

    private PositionMapper() {}

    public static PositionResponse toResponse(Position position) {
        return new PositionResponse(
            position.uuid().toString(),
            position.deviceId(),
            position.fixTime(),
            position.receivedAt(),
            position.latitude(),
            position.longitude(),
            position.altitude(),
            position.speed() != null ? position.speed().doubleValue() : null,
            position.course() != null ? position.course().doubleValue() : null,
            position.accuracy(),
            position.batteryLevel()
        );
    }
}
