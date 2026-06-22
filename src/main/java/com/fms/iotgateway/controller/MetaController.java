package com.fms.iotgateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Meta controller for service information.
 * Mirrors phase2a MetaController pattern.
 */
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "iot-gateway-service",
            "version", buildProperties != null ? buildProperties.getVersion() : "unknown",
            "description", "FMS IoT Gateway — Traccar poller, telemetry normalizer, REST API"
        ));
    }
}
