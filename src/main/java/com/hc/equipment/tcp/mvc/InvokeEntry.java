package com.hc.equipment.tcp.mvc;

import lombok.Getter;
import lombok.ToString;

import java.lang.reflect.Method;
@ToString
@Getter
public class InvokeEntry {
    private Object object;
    private Method method;

    public InvokeEntry(Object object, Method method) {
        this.object = object;
        this.method = method;
    }
}
