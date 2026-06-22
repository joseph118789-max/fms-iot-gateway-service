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
                position.rawPayload() != null
                    ? JSONB.jsonb(position.rawPayload())
                    : null)
            .onConflictDoNothing()  // D.18.w Option B: silent duplicate ignore
            .execute();

        log.debug("Inserted position for deviceId={}, fixTime={}",
            position.deviceId(), position.fixTime());
    }

    @Override
    public long countByDeviceId(Long deviceId) {
        return dsl.fetchCount(Tables.POSITION, Tables.POSITION.DEVICE_ID.eq(deviceId));
    }

    /**
     * Map a jOOQ position record to domain Position.
     * Schema has no uuid column — generate one for domain compatibility.
     * Schema uses Float for altitude/speed/course/batteryLevel, domain uses Double/Float.
     */
    private Position mapToPosition(Record record) {
        JSONB rawPayloadJsonb = record.get(Tables.POSITION.RAW_PAYLOAD);
        String rawPayload = rawPayloadJsonb != null ? rawPayloadJsonb.data() : null;
        return new Position(
            UUID.randomUUID(),  // schema has no uuid — generate for domain use
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

    private static Double doubleValue(Double v) { return v; }
    private static Double doubleValue(Float v) { return v != null ? v.doubleValue() : null; }
}
