package com.hc.equipment.exception;

public class ConnectRefuseException extends GatewayException {
    public ConnectRefuseException() {
    }

    public ConnectRefuseException(String message) {
        super(message);
    }
}
