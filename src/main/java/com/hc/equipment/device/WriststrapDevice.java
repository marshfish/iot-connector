package com.hc.equipment.device;

import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class WriststrapDevice implements CommonDevice {
    private ConcurrentHashMap<String, SocketWarpper> registry = new ConcurrentHashMap<>();

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
            String deviceUniqueId = getDeviceUniqueId(data);
            String netSocketID = String.valueOf(netSocket.hashCode());
            registry.put(deviceUniqueId, new SocketWarpper(netSocketID, netSocket));
            log.info("手环：{} 注册 && 登陆成功，uniqueId：{},socketId:{}",
                    netSocket.remoteAddress().host(), deviceUniqueId, netSocketID);
            return deviceUniqueId;
        } else {
            return registry.entrySet().
                    stream().
                    filter(entry ->
                            entry.getValue().getNetSocketId().equals(String.valueOf(netSocket.hashCode()))).
                    map(Map.Entry::getKey).
                    findFirst().
                    orElseThrow(() -> {
                        log.info("设备未注册，拒绝手环：{} 登陆请求", netSocket.remoteAddress().host());
                        return new ConnectRefuseException();
                    });
        }
    }

    @Override
    public void deviceUnRegister(NetSocket netSocket) {
        String netSocketID = String.valueOf(netSocket.hashCode());
        registry.entrySet().removeIf(entry -> entry.getValue().getNetSocketId().equals(netSocketID));
    }
}
