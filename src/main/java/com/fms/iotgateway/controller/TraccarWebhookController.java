package com.fms.iotgateway.controller;

import com.fms.iotgateway.dto.TraccarWebhookRequest;
import com.fms.iotgateway.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoint for receiving position updates from Traccar.
 *
 * <p>Traccar sends POST requests to this endpoint when devices report positions.
 * This endpoint is public (no JWT required) as Traccar cannot authenticate with JWT.
 * The Traccar API key is used to validate requests on the client side.
 *
 * <p>URL configured in Traccar: http://iot-gateway-service:8080/api/v1/traccar/position
 */
@RestController
@RequestMapping("/api/v1/traccar")
public class TraccarWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TraccarWebhookController.class);

    private final PositionService positionService;

    public TraccarWebhookController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * Receive position update from Traccar.
     * This endpoint is public — Traccar cannot do JWT auth.
     * The API key validation happens at Traccar configuration level.
     */
    @PostMapping("/position")
    public ResponseEntity<Void> receivePosition(@RequestBody TraccarWebhookRequest request) {
        log.debug("Received position webhook: deviceId={}, lat={}, lon={}",
            request.deviceId(), request.latitude(), request.longitude());

        try {
            positionService.processPosition(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process position webhook for device {}: {}",
                request.deviceId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
