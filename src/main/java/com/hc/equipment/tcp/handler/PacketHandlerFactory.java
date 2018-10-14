package com.hc.equipment.tcp.handler;

import io.vertx.core.net.NetSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PacketHandlerFactory {
    private static Map<String, PacketHandler> packetHandlerMap = new ConcurrentHashMap<>();
    private static Lock handlerLock = new ReentrantLock();

    public static PacketHandler buildPacketHandler(NetSocket netSocket) {
        String socketId = String.valueOf(netSocket.hashCode());
        PacketHandler packetHandler;
        handlerLock.lock();
        try {
            if ((packetHandler = packetHandlerMap.get(socketId)) == null) {
                packetHandler = new WriststrapPacketHandler();
                packetHandlerMap.put(socketId, packetHandler);
            }
        } finally {
            handlerLock.unlock();
        }
        return packetHandler;
    }

    public static void removePackageHandler(NetSocket netSocket){
        packetHandlerMap.remove(String.valueOf(netSocket.hashCode()));
    }
}
