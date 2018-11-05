package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class InstructionHandler extends AsyncEventHandler {
    @Resource
    private DeviceSocketManager deviceSocketManager;

    //TODO 配置中心，统一TransportEventEntry 的属性
    @Override
    public void accept(TransportEventEntry event) {
        //发送消息
        //TODO 判空
        validEmpty("设备ID",event.getEqId());
        validEmpty("指令",event.getMsg());
        String eqId = event.getEqId();
        String msg = event.getMsg();
        deviceSocketManager.getDeviceNetSocket(eqId).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket, msg));
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.SERVER_PUBLISH.getType();
    }
}
