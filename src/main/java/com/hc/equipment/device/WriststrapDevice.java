package com.hc.equipment.device;

import com.hc.equipment.tcp.promise.WriststrapProtocol;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class WriststrapDevice implements CommonDevice {
    //netSocket.hashCode->NetSocket
    private ConcurrentHashMap<String, NetSocket> registry = new ConcurrentHashMap<>();
    //deviceUniqueID->netSocket.hashCode
    private ConcurrentHashMap<String, String> association = new ConcurrentHashMap<>();
    private Lock lock = new ReentrantLock();

    @Override
    public String getDeviceUniqueId(String data) {
        return data.substring(6, data.length() - 1);
    }

    @Override
    public String getProtocolNumber(String data) {
        return data.substring(2, 6);
    }

    @Override
    public String deviceRegister(NetSocket netSocket, String data) {
        String protocolNumber = getProtocolNumber(data);
        if (WriststrapProtocol.LOGIN.equals(protocolNumber)) {
            lock.lock();
            String deviceUniqueId = getDeviceUniqueId(data);
            try {
                if (!association.containsKey(deviceUniqueId)) {
                    String netSocketID = String.valueOf(netSocket.hashCode());
                    association.put(deviceUniqueId, netSocketID);
                    registry.put(netSocketID, netSocket);
                    log.info("手环：{} 登陆成功", netSocket.remoteAddress().host());
                } else {
                    throw new RuntimeException("无法登陆，设备注册表不存在该设备,ip:" +
                            netSocket.remoteAddress().host() + "data:{}" + data);
                }
            } finally {
                lock.unlock();
            }
            return deviceUniqueId;
        } else {
            //TODO 数据结构优化
            for (Map.Entry<String, String> entry : association.entrySet()) {
                if (entry.getValue().equals(String.valueOf(netSocket.hashCode()))) {
                    return entry.getKey();
                }
            }
            throw new RuntimeException("该设备尚未登陆，拒绝连接");
        }
    }
}
