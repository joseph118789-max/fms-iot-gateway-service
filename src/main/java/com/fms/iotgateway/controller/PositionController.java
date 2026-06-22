package com.fms.iotgateway.controller;

import com.fms.iotgateway.dto.PositionListResponse;
import com.fms.iotgateway.dto.PositionMapper;
import com.fms.iotgateway.dto.PositionResponse;
import com.fms.iotgateway.service.PositionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for position/telemetry queries.
 * Mirrors phase2a DeviceController pattern.
 */
@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    /**
     * Get positions for a specific device.
     */
    @GetMapping
    public PositionListResponse list(
        @RequestParam Long deviceId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<com.fms.iotgateway.domain.Position> positions =
            positionService.getPositions(deviceId, page, size);

        List<PositionResponse> list = positions.getContent().stream()
            .map(PositionMapper::toResponse)
            .toList();

        return new PositionListResponse(
            list,
            positions.getNumber(),
            positions.getSize(),
            positions.getTotalElements(),
            positions.getTotalPages()
        );
    }

    /**
     * Get the latest position for a specific device.
     */
    @GetMapping("/latest/{deviceId}")
    public ResponseEntity<PositionResponse> getLatest(@PathVariable Long deviceId) {
        return positionService.getLatestPosition(deviceId)
            .map(p -> ResponseEntity.ok(PositionMapper.toResponse(p)))
            .orElse(ResponseEntity.notFound().build());
    }
}
