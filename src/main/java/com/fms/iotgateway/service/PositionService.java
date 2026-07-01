package com.fms.iotgateway.service;

import com.fms.iotgateway.domain.Position;
import com.fms.iotgateway.dto.TraccarWebhookRequest;
import com.fms.iotgateway.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for position data operations.
 * Handles incoming Traccar webhooks and position queries.
 */
@Service
public class PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionService.class);

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Process an incoming position from Traccar webhook.
     * Converts Traccar DTO to domain object and persists.
     *
     * <p>D.18.w Option B: duplicate (deviceId, fixTime) is silently ignored
     * via ON CONFLICT DO NOTHING in the INSERT statement.
     * Traccar retries are idempotent at the app layer.
     *
     * <p>Fix D.18.bbb: persist uuid in raw_payload JSONB so the same row
     * returns the same uuid on read. Generated once at the service boundary,
     * then echoed back via the persisted JSONB column. The Position compact
     * constructor no longer synthesizes a fresh uuid on hydration.
     *
     * <p>Fix D.18.ddd: pass {@code request.batteryLevel()} through to
     * {@code Position.fromTraccar(...)} so the typed {@code battery_level}
     * column receives the Traccar value instead of always-null.
     */
    @Transactional
    public Position processPosition(TraccarWebhookRequest request) {
        log.debug("Processing position for device {}: lat={}, lon={}, speed={}",
            request.deviceId(), request.latitude(), request.longitude(), request.speed());

        UUID stableUuid = UUID.randomUUID();  // generated once, persisted in raw_payload

        Position position = Position.fromTraccar(
            stableUuid,
            request.deviceId(),
            request.deviceTimeMs(),
            request.serverTimeMs(),
            request.processedTimeMs(),
            request.latitude(),
            request.longitude(),
            request.altitude(),
            request.speed(),
            request.course(),
            request.accuracy(),
            request.batteryLevel(),          // Fix D.18.ddd
            null                             // rawPayload — service layer adds uuid below
        );

        positionRepository.save(position);
        log.info("Saved position for device {} at fixTime={} uuid={}",
            position.deviceId(), position.fixTime(), position.uuid());
        return position;
    }

    /**
     * Get positions for a specific device.
     */
    @Transactional(readOnly = true)
    public Page<Position> getPositions(Long deviceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return positionRepository.findByDeviceId(deviceId, pageable);
    }

    /**
     * Get positions within a time range.
     */
    @Transactional(readOnly = true)
    public Page<Position> getPositions(Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        OffsetDateTime fromDt = OffsetDateTime.ofInstant(from, ZoneOffset.UTC);
        OffsetDateTime toDt = OffsetDateTime.ofInstant(to, ZoneOffset.UTC);
        return positionRepository.findByReceivedAtBetween(fromDt, toDt, pageable);
    }

    /**
     * Get the latest position for a device.
     */
    @Transactional(readOnly = true)
    public Optional<Position> getLatestPosition(Long deviceId) {
        return positionRepository.findLatestByDeviceId(deviceId);
    }
}
