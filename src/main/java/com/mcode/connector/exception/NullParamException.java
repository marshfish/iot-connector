package com.mcode.connector.exception;

public class NullParamException extends GatewayException {
    private static final String NULL_TIP = "参数%s为空";

    public NullParamException() {
    }

    public NullParamException(String paramName) {
        super(String.format(NULL_TIP, paramName));
    }
}
