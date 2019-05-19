package com.mcode.connector.exception;

public class ConnectRefuseException extends GatewayException {
    public ConnectRefuseException() {
    }

    public ConnectRefuseException(String message) {
        super(message);
    }
}
