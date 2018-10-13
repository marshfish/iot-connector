package com.hc.equipment.tcp.mvc;

import lombok.Getter;
import lombok.ToString;

import java.lang.reflect.Method;
@ToString
@Getter
public class MappingEntry {
    private Object object;
    private Method method;

    public MappingEntry(Object object, Method method) {
        this.object = object;
        this.method = method;
    }
}
