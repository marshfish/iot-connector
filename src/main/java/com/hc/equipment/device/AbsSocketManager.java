package com.hc.equipment.device;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbsSocketManager implements DeviceSocketManager {
    protected ConcurrentHashMap<String, SocketWarpper> registry = new ConcurrentHashMap<>();

    @Override
    public void writeString(NetSocket netSocket,String data) {
        netSocket.write(Buffer.buffer(data, "UTF-8"));
    }

}
