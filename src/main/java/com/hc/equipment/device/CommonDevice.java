package com.hc.equipment.device;

import io.vertx.core.net.NetSocket;

public interface CommonDevice {
    String getProtocolNumber(String data);
    void deviceRegister(NetSocket netSocket, String data);
}
