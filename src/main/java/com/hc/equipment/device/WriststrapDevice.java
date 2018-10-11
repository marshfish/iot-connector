package com.hc.equipment.device;

import com.hc.equipment.tcp.promise.WriststrapProtocol;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 包头： IW
 * 协议号：上行(设备服务器) AP[两位数字]，下行(服务器设备) BP[两位数字]，数字一样表示数据包及数据包响应
 * 参数：数据包内容
 * 结束符：#
 * <p>
 * 所有与中文有关，例如地址，都使用UNICODE编码
 * 数据包中所有标点符号(除了下发地址中的标点)均为英文半角。
 */

@Component
@Slf4j
public class WriststrapDevice implements CommonDevice {
    private ConcurrentHashMap<String, NetSocket> registry = new ConcurrentHashMap<>();
    private Lock lock = new ReentrantLock();

    /**
     * 获取手环imei
     */
    private String getUniqueNumber(String data) {
        return data.substring(6, data.length() - 1);
    }

    /**
     * 获取该指令协议号
     */
    @Override
    public String getProtocolNumber(String data) {
        return data.substring(2, 6);
    }

    @Override
    public void deviceRegister(NetSocket netSocket, String data) {
        String protocolNumber = getProtocolNumber(data);
        if (WriststrapProtocol.LOGIN.equals(protocolNumber)) {
            lock.lock();
            try {
                if (!registry.containsKey(data)) {
                    registry.put(getUniqueNumber(data), netSocket);
                    log.info("手环：{} 登陆成功", netSocket.remoteAddress().host());
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
