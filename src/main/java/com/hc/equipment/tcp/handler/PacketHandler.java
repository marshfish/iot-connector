package com.hc.equipment.tcp.handler;

        import java.util.List;

/**
 * 粘包半包相关处理
 */
public interface PacketHandler {
    List<String> packageHandler(String data);
}
