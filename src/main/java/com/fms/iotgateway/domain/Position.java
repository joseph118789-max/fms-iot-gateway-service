package com.fms.iotgateway.domain;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    /**
     * Fix D.18.bbb: do NOT generate uuid here. Compact constructor side-effects
     * that assign default values produce fresh UUIDs on every read of the same
     * record (because jOOQ hydrates from DB by calling the canonical
     * constructor). Pass uuid explicitly via fromTraccar(...) or a service-layer
     * helper. This preserves the API contract (every Position has a uuid)
     * while making the value stable across reads of the same persisted row.
     */
    public Position {
        if (receivedAt == null) receivedAt = OffsetDateTime.now();
    }

    /**
     * Factory for Traccar webhook — maps Traccar fields to domain.
     * Traccar sends serverTimeMs (server) and deviceTimeMs (device).
     * We treat deviceTimeMs as fixTime, serverTimeMs as receivedAt.
     *
     * <p>Fix D.18.ccc: deviceTimeMs / serverTimeMs / processedTimeMs are
     * epoch-millisecond values from Traccar. They MUST be parsed via
     * Instant.ofEpochMilli(...).atOffset(ZoneOffset.UTC) — NOT replaced with
     * OffsetDateTime.now(). Replacing with now() collapses the unique key
     * (device_id, fix_time) into (device_id, NOW) and ON CONFLICT DO NOTHING
     * silently never fires.
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
            Float batteryLevel,
            String rawPayload) {

        OffsetDateTime fixTime = deviceTimeMs != null
            ? Instant.ofEpochMilli(deviceTimeMs).atOffset(ZoneOffset.UTC)
            : OffsetDateTime.now();

        OffsetDateTime receivedAt = serverTimeMs != null
            ? Instant.ofEpochMilli(serverTimeMs).atOffset(ZoneOffset.UTC)
            : OffsetDateTime.now();

        return new Position(
            uuid,                       // Fix D.18.bbb: pass through, never synthesize
            deviceId,
            fixTime,                    // Fix D.18.ccc: real deviceTime, not now()
            latitude,
            longitude,
            altitude,
            speed != null ? speed.floatValue() : null,
            course != null ? course.floatValue() : null,
            accuracy,
            batteryLevel,               // Fix D.18.ddd: threaded from DTO
            receivedAt,                 // Fix D.18.ccc: real serverTime, not now()
            rawPayload
        );
    }
}
