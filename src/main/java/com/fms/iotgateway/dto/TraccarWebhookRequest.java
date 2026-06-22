package com.fms.iotgateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for Traccar position webhook (POST /api/v1/traccar/position).
 * Traccar sends position updates as JSON to this endpoint.
 *
 * <p>Traccar uses both "latitude"/"longitude" and "lat"/"lon" field names
 * depending on version/config. Both are accepted via @JsonAlias.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraccarWebhookRequest(
    @JsonAlias({"latitude", "lat"}) Double latitude,
    @JsonAlias({"longitude", "lon"}) Double longitude,
    Long id,
    Long deviceId,
    String type,
    Double altitude,
    Double speed,
    Double course,
    Double accuracy,
    Double altitudeZh,
    Double distance,
    Double totalDistance,
    Boolean motion,
    String address,
    String protocol,
    Long serverTimeMs,
    Long deviceTimeMs,
    Long processedTimeMs
) {
    public TraccarWebhookRequest {
        // Normalize null speed to 0.0
        if (speed == null) speed = 0.0;
        // Normalize null accuracy to 0.0
        if (accuracy == null) accuracy = 0.0;
        // Defensive: if latitude came in via "lat" field, it's already set
        // No action needed — @JsonAlias handles it
    }

    /**
     * Convenience factory for testing — accepts all fields explicitly.
     */
    public static TraccarWebhookRequest of(
            Double latitude, Double longitude,
            Long id, Long deviceId, String type,
            Double altitude, Double speed, Double course, Double accuracy,
            Double altitudeZh, Double distance, Double totalDistance,
            Boolean motion, String address, String protocol,
            Long serverTimeMs, Long deviceTimeMs, Long processedTimeMs) {
        return new TraccarWebhookRequest(
            latitude, longitude,
            id, deviceId, type,
            altitude, speed, course, accuracy,
            altitudeZh, distance, totalDistance,
            motion, address, protocol,
            serverTimeMs, deviceTimeMs, processedTimeMs);
    }
}
