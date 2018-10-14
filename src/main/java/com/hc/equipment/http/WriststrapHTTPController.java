package com.hc.equipment.http;

import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.http.dto.WriststrapDTO;
import com.hc.equipment.http.dto.WriststrapSetSOSDTO;
import com.hc.equipment.http.vo.BaseResult;
import com.hc.equipment.mvc.HttpInstruction;
import com.hc.equipment.mvc.HttpInstructionManager;
import com.hc.equipment.util.Util;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.util.List;
import java.util.UUID;

import static com.hc.equipment.tcp.promise.WriststrapProtocol.*;

@Slf4j
@HttpInstructionManager
public class WriststrapHTTPController {
    @Resource
    private DeviceSocketManager deviceSocketManager;

    @HttpInstruction(value = "/test",method="post")
    public BaseResult measureHea333rtRate(WriststrapDTO dto) {
        log.info(dto.toString());
        log.info("调用测试");
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/heart_rate",method = "post")
    public BaseResult measureHeartRate(WriststrapDTO dto) {
        Util.validDTOEmpty(dto);
        String uuid = UUID.randomUUID().toString();
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_RATE,
                                COMMA, dto.getUniqueId(), COMMA, uuid, SUFFIX)));
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/heart_pressure",method = "post")
    public BaseResult measureHeartPressure(WriststrapDTO dto) {
        Util.validDTOEmpty(dto);
        String uuid = UUID.randomUUID().toString();
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_PRESSURE,
                                COMMA, dto.getUniqueId(), COMMA, uuid, SUFFIX)));
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/sos_contact",method = "post")
    public BaseResult setSOSContact(WriststrapSetSOSDTO dto) {
        Util.validDTOEmpty(dto);
        List<String> sosContact = dto.getSosContact();
        StringBuilder sosFormat = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            String phone = sosContact.get(i);
            if (phone != null) {
                sosFormat.append(phone).append(COMMA);
            } else {
                sosFormat.append("           ").append(COMMA);
            }
        }
        String uuid = UUID.randomUUID().toString();
        String format = sosFormat.replace(sosFormat.length() - 1, sosFormat.length(), "").toString();
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_PRESSURE, COMMA, dto.getUniqueId(),
                                COMMA, uuid, COMMA, format, SUFFIX)));
        return BaseResult.getInstance();
    }
}
