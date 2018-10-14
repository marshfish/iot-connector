package com.hc.equipment.device;

import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 手环设备注册表
 */
@Component
@Slf4j
public class WriststrapDevice extends AbsSocketManager {

    @Override
    public String getDeviceUniqueId(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(6, data.length() - 1)).orElse(null);
    }

    @Override
    public String getProtocolNumber(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(2, 6)).orElse(null);
    }
    //允许同一设备重复登陆
    @Override
    public String deviceRegister(NetSocket netSocket, String data) {
        return Optional.ofNullable(getProtocolNumber(data)).map(protocolNumber -> {
            if (WriststrapProtocol.LOGIN.equals(protocolNumber)) {
                return Optional.ofNullable(getDeviceUniqueId(data)).map(deviceId -> {
                    String netSocketId = String.valueOf(netSocket.hashCode());
                    registry.put(deviceId, new SocketWarpper(netSocketId, netSocket));
                    log.info("手环：{} 注册 && 登陆成功，uniqueId：{},socketId:{}",
                            netSocket.remoteAddress().host(), deviceId, netSocketId);
                    return deviceId;
                }).orElse(null);
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
        }).orElse(StringUtils.EMPTY);

    }

    @Override
    public void deviceUnRegister(NetSocket netSocket) {
        String netSocketID = String.valueOf(netSocket.hashCode());
        registry.entrySet().removeIf(entry -> entry.getValue().getNetSocketId().equals(netSocketID));
    }

    @Override
    public Optional<NetSocket> getDeviceNetSocket(String uniqueId) {
        return Optional.ofNullable(uniqueId).map(id -> registry.get(id)).
                map(SocketWarpper::getNetSocket);
    }
}
