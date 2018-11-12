package com.hc.equipment.device;

import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class SocketWarpper {
    private String netSocketId;
    private NetSocket netSocket;

    public SocketWarpper(String netSocketId, NetSocket netSocket) {
        this.netSocketId = netSocketId;
        this.netSocket = netSocket;
    }

}
