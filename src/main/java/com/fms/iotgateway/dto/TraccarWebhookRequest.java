package com.fms.iotgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Traccar position webhook (POST /api/v1/traccar/position).
 * Traccar sends position updates as JSON to this endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraccarWebhookRequest(
    @JsonProperty("id") Long id,
    @JsonProperty("deviceId") Long deviceId,
    @JsonProperty("type") String type,
    @JsonProperty("latitude") Double latitude,
    @JsonProperty("longitude") Double longitude,
    @JsonProperty("altitude") Double altitude,
    @JsonProperty("speed") Double speed,
    @JsonProperty("course") Double course,
    @JsonProperty("accuracy") Double accuracy,
    @JsonProperty("海拔") Double altitudeZh,  // Traccar localized field
    @JsonProperty("distance") Double distance,
    @JsonProperty("totalDistance") Double totalDistance,
    @JsonProperty("motion") Boolean motion,
    @JsonProperty("address") String address,
    @JsonProperty("protocol") String protocol,
    @JsonProperty("serverTime") Long serverTimeMs,
    @JsonProperty("deviceTime") Long deviceTimeMs,
    @JsonProperty("processedTime") Long processedTimeMs
) {
    public TraccarWebhookRequest {
        // Normalize null speed to 0.0
        if (speed == null) speed = 0.0;
        // Normalize null accuracy to 0.0
        if (accuracy == null) accuracy = 0.0;
    }
}
