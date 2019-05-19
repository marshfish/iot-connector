package com.mcode.connector.tcp;

import io.vertx.core.buffer.Buffer;

import java.util.function.Consumer;

/**
 * 粘包半包相关处理
 * 注意RecordParser.newDelimited只能用前缀分隔符，可用{@link VertxPacketHandler }
 * 如果需要解码前后缀分隔符，直接用自定义的前后缀间隔符解码器{@link SplitPacketHandler}
 */
public interface PacketHandler {
    PacketHandler register(Consumer<String> consumer);

    void handler(Buffer buffer);
}
