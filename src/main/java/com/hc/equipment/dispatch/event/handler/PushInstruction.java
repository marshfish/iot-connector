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
        log.info("推送指令事件：{}", event);
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
        /**
         * 消息重发qos由dispatcher端负责
         * qos1下则上传响应返回给前端逻辑
         * qos0下也需要上传响应给dispatcher用于记录指令req/res
         */
        dataUploadHandler.attachSeriaId2DispatcherId(serialNumber, dispatcherId);
        if (!socketContainer.writeString(eqId, msg)) {
            if (qos == QosType.AT_MOST_ONCE.getType()) {
                dataUploadHandler.uploadCallback(serialNumber, eqId, FAIL);
            }
        }
    }

    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.SERVER_PUBLISH;
    }
}
