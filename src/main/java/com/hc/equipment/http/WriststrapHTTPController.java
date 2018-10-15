package com.hc.equipment.http;

import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.device.WriststrapDevice;
import com.hc.equipment.http.dto.EquipmentDTO;
import com.hc.equipment.http.dto.WriststrapSetSOSDTO;
import com.hc.equipment.http.vo.BaseResult;
import com.hc.equipment.mvc.HttpInstruction;
import com.hc.equipment.mvc.HttpInstructionManager;
import com.hc.equipment.util.SpringContextUtil;
import com.hc.equipment.util.Util;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.util.List;

import static com.hc.equipment.tcp.promise.WriststrapProtocol.*;

/**
 * http -> tcp
 */
@Slf4j
@HttpInstructionManager
public class WriststrapHTTPController {

    private DeviceSocketManager deviceSocketManager = SpringContextUtil.getBean(WriststrapDevice.class);

    @HttpInstruction(value = "/test", method = "post")
    public BaseResult measureHea333rtRate(EquipmentDTO dto) {
        log.info(dto.toString());
        log.info("调用测试");
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/heart_rate", method = "post")
    public BaseResult measureHeartRate(EquipmentDTO dto) {
        Util.validDTOEmpty(dto);
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_RATE,
                                COMMA, dto.getUniqueId(), COMMA, dto.getSerialNumber(), SUFFIX)));
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/heart_pressure", method = "post")
    public BaseResult measureHeartPressure(EquipmentDTO dto) {
        Util.validDTOEmpty(dto);
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_PRESSURE,
                                COMMA, dto.getUniqueId(), COMMA, dto.getSerialNumber(), SUFFIX)));
        return BaseResult.getInstance();
    }

    @HttpInstruction(value = "/measure/sos_contact", method = "post")
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
        String format = sosFormat.replace(sosFormat.length() - 1, sosFormat.length(), "").toString();
        deviceSocketManager.getDeviceNetSocket(dto.getUniqueId()).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket,
                        Util.buildParam(PREFIX, MEASURE_HEART_PRESSURE, COMMA, dto.getUniqueId(),
                                COMMA, dto.getSerialNumber(), COMMA, format, SUFFIX)));
        return BaseResult.getInstance();
    }
}
