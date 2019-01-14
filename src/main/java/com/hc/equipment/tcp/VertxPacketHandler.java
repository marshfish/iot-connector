package com.hc.equipment.tcp;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

public class VertxPacketHandler implements PacketHandler {
    private static String suffix = "@";
    private Queue<String> cache = new ArrayBlockingQueue<>(1);
    private List<String> instructions = new ArrayList<>(1);
    private RecordParser parser = RecordParser.newDelimited(suffix, h -> cache.offer(h.toString()));
    private Consumer< String> consumer;

    public VertxPacketHandler(Consumer< String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handler(Buffer buffer) {
        parser.handle(buffer);
        instructions.clear();
        Optional.ofNullable(cache.poll()).ifPresent(instructions::add);
        cache.clear();
        instructions.forEach(consumer);
    }

}
