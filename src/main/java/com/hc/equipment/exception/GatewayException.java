package com.hc.equipment.exception;

public class GatewayException extends RuntimeException {
    public GatewayException(String message) {
        super(message);
    }

    public GatewayException() {
    }
}
