package com.hc.equipment.tcp;

import io.vertx.core.buffer.Buffer;

import java.util.Collection;

/**
 * 粘包半包相关处理
 */
public interface PacketHandler {
    Collection<String> packageHandler(Buffer buffer);
}
