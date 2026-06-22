package com.fms.iotgateway.exception;

/**
 * Exception thrown when a position record is not found.
 */
public class PositionNotFoundException extends RuntimeException {

    public PositionNotFoundException(String message) {
        super(message);
    }

    public PositionNotFoundException(Long deviceId) {
        super("No position found for device: " + deviceId);
    }
}
