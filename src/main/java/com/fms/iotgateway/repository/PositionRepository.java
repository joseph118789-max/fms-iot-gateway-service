package com.fms.iotgateway.repository;

import com.fms.iotgateway.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository contract for Position persistence in telemetry schema.
 *
 * <p>Positions are immutable records — no soft-delete, no updates.
 * Only insert (new position) and read operations.
 */
public interface PositionRepository {

    /**
     * Find a position record by id.
     */
    Optional<Position> findById(UUID id);

    /**
     * Find positions for a specific device, paginated.
     * Ordered by serverTime descending (most recent first).
     */
    Page<Position> findByDeviceId(Long deviceId, Pageable pageable);

    /**
     * Find positions within a time range, paginated.
     * Ordered by serverTime descending.
     */
    Page<Position> findByServerTimeBetween(Instant from, Instant to, Pageable pageable);

    /**
     * Find the most recent position for a device.
     */
    Optional<Position> findLatestByDeviceId(Long deviceId);

    /**
     * Save a new position record.
     */
    void save(Position position);

    /**
     * Count positions for a device.
     */
    long countByDeviceId(Long deviceId);
}
