package com.hc.equipment.exception;

public class ConnectRefuseException extends RuntimeException {
    public ConnectRefuseException() {
    }

    public ConnectRefuseException(String message) {
        super(message);
    }
}
