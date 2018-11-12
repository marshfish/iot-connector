package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.DataUploadHandler;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PushInstruction extends AsyncEventHandler {
    @Resource
    private DeviceSocketManager deviceSocketManager;
    @Resource
    private DataUploadHandler dataUploadHandler;

    //TODO 配置中心，统一TransportEventEntry 的属性
    @Override
    public void accept(TransportEventEntry event) {
        String eqId = event.getEqId();
        String msg = event.getMsg();
        String dispatcherId = event.getDispatcherId();
        String serialNumber = event.getSerialNumber();
        validEmpty("dispatcher端ID", dispatcherId);
        validEmpty("设备ID", eqId);
        validEmpty("指令", msg);
        validEmpty("指令流水号", serialNumber);
        //注册指令流水号与dispatcherId
        dataUploadHandler.attachSeriaId2DispatcherId(serialNumber, dispatcherId);
        deviceSocketManager.getDeviceNetSocket(eqId).
                ifPresent(netSocket -> deviceSocketManager.writeString(netSocket, msg));
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.SERVER_PUBLISH.getType();
    }
}