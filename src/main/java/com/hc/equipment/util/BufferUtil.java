package com.hc.equipment.util;

import io.vertx.core.buffer.Buffer;

public class BufferUtil {
    public static Buffer allocString(String data) {
        return Buffer.buffer(data, "UTF-8");
    }
}
