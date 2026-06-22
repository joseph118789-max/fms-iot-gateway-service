package com.fms.iotgateway.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain object representing a normalized position record from Traccar.
 * This is the internal domain model — jOOQ types stay in the repository layer.
 */
public record Position(
    UUID id,
    Long deviceId,
    Instant serverTime,
    Instant deviceTime,
    Instant processedTime,
    Double latitude,
    Double longitude,
    Double altitude,
    Double speed,
    Double course,
    Double accuracy,
    Instant createdAt
) {
    public Position {
        if (serverTime == null) serverTime = Instant.now();
        if (processedTime == null) processedTime = Instant.now();
        if (createdAt == null) createdAt = Instant.now();
    }
}
