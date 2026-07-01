package com.fms.iotgateway.repository;

import com.fms.iotgateway.domain.Position;
import com.fms.iotgateway.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * jOOQ-backed implementation of {@link PositionRepository}.
 *
 * <p>Table: telemetry.position (jOOQ ref: Tables.POSITION)
 * Composite PK: (DEVICE_ID, FIX_TIME) — no id column.
 *
 * <p>D.18.w Option B idempotency:
 * Duplicate INSERT (same deviceId + fixTime) raises SQLSTATE 23505.
 * Caller (PositionService.processPosition) wraps in try/catch — duplicates are OK.
 */
@Repository
public class PositionRepositoryImpl implements PositionRepository {

    private static final Logger log = LoggerFactory.getLogger(PositionRepositoryImpl.class);

    private final DSLContext dsl;

    public PositionRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Page<Position> findByDeviceId(Long deviceId, Pageable pageable) {
        var records = dsl.selectFrom(Tables.POSITION)
            .where(Tables.POSITION.DEVICE_ID.eq(deviceId))
            .orderBy(Tables.POSITION.RECEIVED_AT.desc())
            .limit(pageable.getPageSize())
            .offset((int) pageable.getOffset())
            .fetch();

        long total = dsl.fetchCount(Tables.POSITION, Tables.POSITION.DEVICE_ID.eq(deviceId));

        var positions = records.map(this::mapToPosition);
        return new PageImpl<>(positions, pageable, total);
    }

    @Override
    public Page<Position> findByReceivedAtBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        var records = dsl.selectFrom(Tables.POSITION)
            .where(Tables.POSITION.RECEIVED_AT.ge(from))
            .and(Tables.POSITION.RECEIVED_AT.le(to))
            .orderBy(Tables.POSITION.RECEIVED_AT.desc())
            .limit(pageable.getPageSize())
            .offset((int) pageable.getOffset())
            .fetch();

        long total = dsl.fetchCount(Tables.POSITION,
            Tables.POSITION.RECEIVED_AT.ge(from).and(Tables.POSITION.RECEIVED_AT.le(to)));

        var positions = records.map(this::mapToPosition);
        return new PageImpl<>(positions, pageable, total);
    }

    @Override
    public Optional<Position> findLatestByDeviceId(Long deviceId) {
        var record = dsl.selectFrom(Tables.POSITION)
            .where(Tables.POSITION.DEVICE_ID.eq(deviceId))
            .orderBy(Tables.POSITION.FIX_TIME.desc())
            .limit(1)
            .fetchOptional();

        return record.map(this::mapToPosition);
    }

    @Override
    public void save(Position position) {
        // Fix D.18.bbb: persist uuid into raw_payload JSONB so reads return
        // the same uuid value that was written. Without this, every read of
        // the same row would synthesize a fresh UUID (D.18.bbb defect).
        String rawPayloadWithUuid = injectUuidIntoRawPayload(
            position.rawPayload(), position.uuid());

        dsl.insertInto(Tables.POSITION)
            .set(Tables.POSITION.DEVICE_ID, position.deviceId())
            .set(Tables.POSITION.FIX_TIME, position.fixTime())
            .set(Tables.POSITION.LATITUDE, position.latitude())
            .set(Tables.POSITION.LONGITUDE, position.longitude())
            // Schema: Float, Domain: Double — explicit conversion
            .set(Tables.POSITION.ALTITUDE, position.altitude() != null ? position.altitude().floatValue() : null)
            .set(Tables.POSITION.SPEED, position.speed())
            .set(Tables.POSITION.COURSE, position.course())
            // Schema: Float, Domain: Double — explicit conversion
            .set(Tables.POSITION.ACCURACY, position.accuracy() != null ? position.accuracy().floatValue() : null)
            .set(Tables.POSITION.BATTERY_LEVEL, position.batteryLevel())
            .set(Tables.POSITION.RECEIVED_AT, position.receivedAt())
            .set(Tables.POSITION.RAW_PAYLOAD,
                rawPayloadWithUuid != null
                    ? JSONB.jsonb(rawPayloadWithUuid)
                    : null)
            .onConflictDoNothing()  // D.18.w Option B: silent duplicate ignore
            .execute();

        log.debug("Inserted position for deviceId={}, fixTime={}, uuid={}",
            position.deviceId(), position.fixTime(), position.uuid());
    }

    /**
     * Inject the uuid into raw_payload JSONB so the same uuid is returned on
     * subsequent reads. If rawPayload is null, synthesize a minimal
     * {@code {"uuid":"..."}} wrapper. If rawPayload already has a uuid key,
     * leave it untouched (preserve the original uuid across re-saves).
     */
    private static String injectUuidIntoRawPayload(String rawPayload, UUID uuid) {
        if (uuid == null) return rawPayload;
        if (rawPayload == null || rawPayload.isBlank()) {
            return "{\"uuid\":\"" + uuid + "\"}";
        }
        if (rawPayload.contains("\"uuid\"")) {
            return rawPayload;  // already has uuid, preserve
        }
        // Splice the uuid key in right after the opening brace
        int braceIdx = rawPayload.indexOf('{');
        if (braceIdx < 0) {
            return "{\"uuid\":\"" + uuid + "\"," + rawPayload;
        }
        int insertAt = braceIdx + 1;
        // Skip whitespace
        while (insertAt < rawPayload.length() && Character.isWhitespace(rawPayload.charAt(insertAt))) {
            insertAt++;
        }
        // Check if there's content after the brace
        boolean hasContent = insertAt < rawPayload.length() && rawPayload.charAt(insertAt) != '}';
        String injection = "\"uuid\":\"" + uuid + "\"";
        if (hasContent) {
            injection = injection + ",";
        }
        return rawPayload.substring(0, insertAt) + injection + rawPayload.substring(insertAt);
    }

    @Override
    public long countByDeviceId(Long deviceId) {
        return dsl.fetchCount(Tables.POSITION, Tables.POSITION.DEVICE_ID.eq(deviceId));
    }

    /**
     * Map a jOOQ position record to domain Position.
     * Schema has no uuid column — recover from rawPayload JSONB if present,
     * else fall back to a deterministic derivation from deviceId + fixTime.
     * Schema uses Float for altitude/speed/course/batteryLevel, domain uses Double/Float.
     *
     * <p>Fix D.18.bbb: do NOT synthesize a fresh UUID on every read.
     * The uuid is generated once at the service layer and persisted inside
     * the raw_payload JSONB column (under key "uuid"). Reading the same row
     * twice MUST return the same uuid. If raw_payload lacks the uuid key
     * (rows written before D.18.bbb was fixed), derive a deterministic uuid
     * from (deviceId, fixTime) so reads remain stable.
     */
    private Position mapToPosition(Record record) {
        JSONB rawPayloadJsonb = record.get(Tables.POSITION.RAW_PAYLOAD);
        String rawPayload = rawPayloadJsonb != null ? rawPayloadJsonb.data() : null;

        UUID stableUuid = extractUuidFromRawPayload(rawPayload);
        if (stableUuid == null) {
            // Legacy rows: derive deterministically from (deviceId, fixTime)
            Long deviceId = record.get(Tables.POSITION.DEVICE_ID);
            OffsetDateTime fixTime = record.get(Tables.POSITION.FIX_TIME);
            String seed = deviceId + "|" + (fixTime != null ? fixTime.toString() : "null");
            stableUuid = UUID.nameUUIDFromBytes(seed.getBytes());
        }

        return new Position(
            stableUuid,
            record.get(Tables.POSITION.DEVICE_ID),
            record.get(Tables.POSITION.FIX_TIME),
            record.get(Tables.POSITION.LATITUDE),
            record.get(Tables.POSITION.LONGITUDE),
            doubleValue(record.get(Tables.POSITION.ALTITUDE)),  // Float->Double
            record.get(Tables.POSITION.SPEED),
            record.get(Tables.POSITION.COURSE),
            doubleValue(record.get(Tables.POSITION.ACCURACY)),  // Double stays Double
            record.get(Tables.POSITION.BATTERY_LEVEL),
            record.get(Tables.POSITION.RECEIVED_AT),
            rawPayload
        );
    }

    /**
     * Extract the persisted uuid from raw_payload JSONB if present.
     * Returns null if rawPayload is null, malformed, or lacks the uuid key.
     * Cheap string scan — avoids pulling in a JSON parser dependency.
     */
    private static UUID extractUuidFromRawPayload(String rawPayload) {
        if (rawPayload == null) return null;
        int idx = rawPayload.indexOf("\"uuid\"");
        if (idx < 0) return null;
        int colon = rawPayload.indexOf(':', idx);
        if (colon < 0) return null;
        int q1 = rawPayload.indexOf('"', colon);
        if (q1 < 0) return null;
        int q2 = rawPayload.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        try {
            return UUID.fromString(rawPayload.substring(q1 + 1, q2));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Double doubleValue(Double v) { return v; }
    private static Double doubleValue(Float v) { return v != null ? v.doubleValue() : null; }
}
