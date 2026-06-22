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
     */
    @Transactional
    public Position processPosition(TraccarWebhookRequest request) {
        log.debug("Processing position for device {}: lat={}, lon={}, speed={}",
            request.deviceId(), request.latitude(), request.longitude(), request.speed());

        Position position = new Position(
            UUID.randomUUID(),
            request.deviceId(),
            millisToInstant(request.serverTimeMs()),
            millisToInstant(request.deviceTimeMs()),
            millisToInstant(request.processedTimeMs()),
            request.latitude(),
            request.longitude(),
            request.altitude(),
            request.speed(),
            request.course(),
            request.accuracy(),
            Instant.now()
        );

        positionRepository.save(position);
        log.info("Saved position {} for device {}", position.id(), position.deviceId());
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
        return positionRepository.findByServerTimeBetween(from, to, pageable);
    }

    /**
     * Get the latest position for a device.
     */
    @Transactional(readOnly = true)
    public Optional<Position> getLatestPosition(Long deviceId) {
        return positionRepository.findLatestByDeviceId(deviceId);
    }

    private Instant millisToInstant(Long millis) {
        return millis != null ? Instant.ofEpochMilli(millis) : Instant.now();
    }
}
