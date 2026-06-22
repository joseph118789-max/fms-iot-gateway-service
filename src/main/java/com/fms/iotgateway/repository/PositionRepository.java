package com.fms.iotgateway.repository;

import com.fms.iotgateway.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository contract for Position persistence in telemetry schema.
 *
 * <p>Positions are immutable records — no soft-delete, no updates.
 * Primary key is composite: (deviceId, fixTime).
 *
 * <p>NOTE: No findById — schema has no id column.
 * Use findByDeviceId + findLatestByDeviceId for reads.
 */
public interface PositionRepository {

    /**
     * Find positions for a specific device, paginated.
     * Ordered by receivedAt descending (most recent first).
     */
    Page<Position> findByDeviceId(Long deviceId, Pageable pageable);

    /**
     * Find positions within a time range, paginated.
     * Ordered by receivedAt descending.
     */
    Page<Position> findByReceivedAtBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    /**
     * Find the most recent position for a device.
     */
    Optional<Position> findLatestByDeviceId(Long deviceId);

    /**
     * Save a new position record.
     * Idempotent per D.18.w Option B: duplicate (deviceId, fixTime) raises SQLSTATE 23505.
     * Caller should catch DataAccessException and treat as success.
     */
    void save(Position position);

    /**
     * Count positions for a device.
     */
    long countByDeviceId(Long deviceId);
}
