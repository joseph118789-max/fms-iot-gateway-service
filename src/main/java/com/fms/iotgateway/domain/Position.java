package com.fms.iotgateway.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain object representing a normalized position record from Traccar.
 *
 * <p>Maps to telemetry.position table (jOOQ table: Tables.POSITION).
 * Composite primary key: (deviceId, fixTime) — no separate id column.
 *
 * <p>Schema columns:
 * DEVICE_ID (NOT NULL), FIX_TIME (NOT NULL), LATITUDE, LONGITUDE,
 * SPEED, ALTITUDE, COURSE, ACCURACY, BATTERY_LEVEL,
 * RECEIVED_AT (NOT NULL, default now()), RAW_PAYLOAD (JSONB)
 */
public record Position(
    UUID uuid,           // internal UUID, not in schema (used for idempotency tracking)
    Long deviceId,
    OffsetDateTime fixTime,       // GPS fix time (from device, NOT NULL)
    Double latitude,
    Double longitude,
    Double altitude,
    Float speed,
    Float course,
    Double accuracy,
    Float batteryLevel,
    OffsetDateTime receivedAt,    // server-side receipt time (NOT NULL, default now())
    String rawPayload             // JSONB, nullable
) {
    public Position {
        if (receivedAt == null) receivedAt = OffsetDateTime.now();
        if (uuid == null) uuid = UUID.randomUUID();
    }

    /**
     * Factory for Traccar webhook — maps Traccar fields to domain.
     * Traccar sends serverTimeMs (server) and deviceTimeMs (device).
     * We treat deviceTimeMs as fixTime, serverTimeMs as receivedAt.
     */
    public static Position fromTraccar(
            UUID uuid,
            Long deviceId,
            Long deviceTimeMs,
            Long serverTimeMs,
            Long processedTimeMs,
            Double latitude,
            Double longitude,
            Double altitude,
            Double speed,
            Double course,
            Double accuracy,
            String rawPayload) {

        OffsetDateTime fixTime = deviceTimeMs != null
            ? OffsetDateTime.now().withNano(0) // TODO: parse actual offset from Traccar payload
            : OffsetDateTime.now();

        OffsetDateTime receivedAt = serverTimeMs != null
            ? OffsetDateTime.now().withNano(0) // TODO: parse actual offset from Traccar payload
            : OffsetDateTime.now();

        return new Position(
            uuid != null ? uuid : UUID.randomUUID(),
            deviceId,
            fixTime,
            latitude,
            longitude,
            altitude,
            speed != null ? speed.floatValue() : null,
            course != null ? course.floatValue() : null,
            accuracy,
            null, // batteryLevel not in Traccar webhook payload
            receivedAt,
            rawPayload
        );
    }
}
