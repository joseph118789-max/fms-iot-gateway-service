package com.fms.iotgateway.repository;

import com.fms.iotgateway.domain.Position;
import org.jooq.DSLContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * jOOQ-backed implementation of {@link PositionRepository}.
 *
 * <p>Uses {@link DSLContext} internally; jOOQ types stay inside this class.
 * Service layer receives {@link Position} domain objects.
 *
 * <p>NOTE: The actual jOOQ codegen classes (TC_POSITION table, records) are
 * generated at build time from jooq-codegen.xml. This class shows the shape.
 */
@Repository
public class PositionRepositoryImpl implements PositionRepository {

    private final DSLContext dsl;

    public PositionRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<Position> findById(UUID id) {
        // Shape (codegen classes injected at build time):
        //   return dsl.selectFrom(TC_POSITION)
        //       .where(TC_POSITION.ID.eq(id))
        //       .fetchOptionalInto(Position.class);
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }

    @Override
    public Page<Position> findByDeviceId(Long deviceId, Pageable pageable) {
        // Shape:
        //   var records = dsl.selectFrom(TC_POSITION)
        //       .where(TC_POSITION.DEVICE_ID.eq(deviceId))
        //       .orderBy(TC_POSITION.SERVER_TIME.desc())
        //       .limit(pageable.getPageSize())
        //       .offset(pageable.getOffset())
        //       .fetchInto(Position.class);
        //   long total = dsl.fetchCount(TC_POSITION, TC_POSITION.DEVICE_ID.eq(deviceId));
        //   return new PageImpl<>(records, pageable, total);
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }

    @Override
    public Page<Position> findByServerTimeBetween(Instant from, Instant to, Pageable pageable) {
        // Shape:
        //   var records = dsl.selectFrom(TC_POSITION)
        //       .where(TC_POSITION.SERVER_TIME.ge(from))
        //       .and(TC_POSITION.SERVER_TIME.le(to))
        //       .orderBy(TC_POSITION.SERVER_TIME.desc())
        //       .limit(pageable.getPageSize())
        //       .offset(pageable.getOffset())
        //       .fetchInto(Position.class);
        //   long total = dsl.fetchCount(TC_POSITION,
        //       TC_POSITION.SERVER_TIME.ge(from).and(TC_POSITION.SERVER_TIME.le(to)));
        //   return new PageImpl<>(records, pageable, total);
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }

    @Override
    public Optional<Position> findLatestByDeviceId(Long deviceId) {
        // Shape:
        //   return dsl.selectFrom(TC_POSITION)
        //       .where(TC_POSITION.DEVICE_ID.eq(deviceId))
        //       .orderBy(TC_POSITION.SERVER_TIME.desc())
        //       .limit(1)
        //       .fetchOptionalInto(Position.class);
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }

    @Override
    public void save(Position position) {
        // INSERT shape:
        //   dsl.insertInto(TC_POSITION)
        //       .set(TC_POSITION.ID, position.id())
        //       .set(TC_POSITION.DEVICE_ID, position.deviceId())
        //       .set(TC_POSITION.SERVER_TIME, position.serverTime())
        //       .set(TC_POSITION.DEVICE_TIME, position.deviceTime())
        //       .set(TC_POSITION.PROCESSED_TIME, position.processedTime())
        //       .set(TC_POSITION.LATITUDE, position.latitude())
        //       .set(TC_POSITION.LONGITUDE, position.longitude())
        //       .set(TC_POSITION.ALTITUDE, position.altitude())
        //       .set(TC_POSITION.SPEED, position.speed())
        //       .set(TC_POSITION.COURSE, position.course())
        //       .set(TC_POSITION.ACCURACY, position.accuracy())
        //       .set(TC_POSITION.CREATED_AT, position.createdAt())
        //       .execute();
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }

    @Override
    public long countByDeviceId(Long deviceId) {
        // Shape:
        //   return dsl.fetchCount(TC_POSITION, TC_POSITION.DEVICE_ID.eq(deviceId));
        throw new UnsupportedOperationException("Wire to jOOQ-generated TC_POSITION table");
    }
}
