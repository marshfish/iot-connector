package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.device.SocketContainer;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.dispatch.event.DataUploadHandler;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.type.QosType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class PushInstruction extends AsyncEventHandler {
    @Resource
    private DataUploadHandler dataUploadHandler;
    @Resource
    private SocketContainer socketContainer;
    private static final String FAIL = "sendFail_noLogin";

    @Override
    public void accept(Trans.event_data event) {
        String eqId = event.getEqId();
        String msg = event.getMsg();
        String dispatcherId = event.getDispatcherId();
        String serialNumber = event.getSerialNumber();
        Integer qos = event.getQos();
        int reTryTimeout = event.getReTryTimeout();
        validEmpty("dispatcher端ID", dispatcherId);
        validEmpty("设备ID", eqId);
        validEmpty("指令", msg);
        validEmpty("指令流水号", serialNumber);
        validEmpty("qos", qos);
        validEmpty("消息重发窗口时间", reTryTimeout);
        if (qos == QosType.AT_MOST_ONCE.getType()) {
            socketContainer.writeString(eqId, msg);
        } else {
            if (socketContainer.writeString(eqId, msg)) {
                //注册指令流水号与dispatcherId
                dataUploadHandler.attachSeriaId2DispatcherId(serialNumber, dispatcherId);
            } else {
                //TODO qos1 返回给dispatcher，发送失败,设备未登陆
                dataUploadHandler.uploadCallback(serialNumber, eqId, FAIL);
            }
        }
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.SERVER_PUBLISH.getType();
    }
}
