package com.hc.equipment.tcp;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * vertx原生粘包半包处理器
 */
public class VertxPacketHandler implements PacketHandler {
    private static String suffix = "@";
    private Queue<String> cache = new ArrayBlockingQueue<>(1);
    private List<String> instructions = new ArrayList<>(1);
    private RecordParser parser = RecordParser.newDelimited(suffix, h -> cache.offer(h.toString()));


    @Override
    public Collection<String> packageHandler(Buffer buffer) {
        parser.handle(buffer);
        instructions.clear();
        Optional.ofNullable(cache.poll()).ifPresent(instructions::add);
        cache.clear();
        return instructions;
    }
}
