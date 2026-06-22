package com.fms.iotgateway.dto;

/**
 * Standard error response DTO.
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String timestamp
) {}
