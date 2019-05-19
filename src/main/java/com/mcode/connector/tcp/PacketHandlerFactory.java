package com.mcode.connector.tcp;

import io.vertx.core.net.NetSocket;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PacketHandlerFactory {
    private static Map<String, PacketHandler> packetHandlerMap = Collections.synchronizedMap(new LRUMap<>());

    public static PacketHandler build(String socketId) {
        return packetHandlerMap.computeIfAbsent(socketId, s -> new SplitPacketHandler());
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
