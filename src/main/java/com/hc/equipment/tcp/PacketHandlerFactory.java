package com.hc.equipment.tcp;

import io.vertx.core.net.NetSocket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class PacketHandlerFactory {
    private static Map<String, PacketHandler> packetHandlerMap = new LRUMap<>();
    private static Lock handlerLock = new ReentrantLock();

    public static PacketHandler build(NetSocket netSocket, Consumer<String> consumer) {
        String socketId = String.valueOf(netSocket.hashCode());
        PacketHandler packetHandler;
        handlerLock.lock();
        try {
            packetHandler = packetHandlerMap.computeIfAbsent(socketId, s -> new SplitPacketHandler(consumer));
        } finally {
            handlerLock.unlock();
        }
        return packetHandler;
    }

    public static void removePackageHandler(NetSocket netSocket) {
        packetHandlerMap.remove(String.valueOf(netSocket.hashCode()));
    }

    private static class LRUMap<K, V> extends LinkedHashMap<K, V> {
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return true;
        }

        private LRUMap() {
            super(200, 0.75f, true);
        }
    }
}
