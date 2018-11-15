package com.hc.equipment.custom;

import com.hc.equipment.device.AbsSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 手环设备注册表
 */
@Component
@Slf4j
public class WriststrapDevice extends AbsSocketManager {

    @Override
    public String setEquipmentId(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(6, data.length() - 1)).orElse(null);
    }

    @Override
    public String setProtocolNumber(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(2, 6)).orElse(null);
    }

    @Override
    protected String setLoginProtocolNumber() {
        return WriststrapProtocol.LOGIN;
    }
}
