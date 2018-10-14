package com.hc.equipment.device;

import io.vertx.core.net.NetSocket;

import java.util.Optional;

/**
 * 公共设备连接管理器
 */
public interface DeviceSocketManager {
    /**
     * 获取请求上行协议号
     */
    String getProtocolNumber(String data);

    /**
     * 获取设备唯一编号
     */
    String getDeviceUniqueId(String data);

    /**
     * 设备注册
     *
     * @param netSocket socket
     * @param data      数据
     * @return 设备唯一编号
     */
    String deviceRegister(NetSocket netSocket, String data);

    /**
     * 设备解除注册
     */
    void deviceUnRegister(NetSocket netSocket);

    /**
     * 获取设备socket连接
     */
    Optional<NetSocket> getDeviceNetSocket(String uniqueId);

    /**
     * 写入字符串
     */
    void writeString(NetSocket netSocket, String data);

}
