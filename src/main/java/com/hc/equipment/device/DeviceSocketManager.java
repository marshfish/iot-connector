package com.hc.equipment.device;

import io.vertx.core.net.NetSocket;

import java.util.Optional;

/**
 * 公共设备连接管理器
 */
public interface DeviceSocketManager {
    /**
     * 根据指令获取请求上行协议号
     */
    String getProtocolNumber(String data);

    /**
     * 根据指令获取设备唯一编号
     */
    String getEquipmentId(String data);

    /**
     * 设备登陆
     *
     * @param netSocket socket
     * @param data      数据
     * @return 设备唯一编号
     */
    String deviceLogin(NetSocket netSocket, String data);

    /**
     * 设备登出
     */
    void deviceLogout(NetSocket netSocket);

    /**
     * 获取设备socket连接
     */
    Optional<NetSocket> getDeviceNetSocket(String equipmentId);

    /**
     * 写入字符串
     */
    void writeString(NetSocket netSocket, String data);

}
