package com.mcode.connector.dispatch.event.handler;

import com.mcode.connector.device.SocketContainer;
import com.mcode.connector.dispatch.event.AsyncEventHandler;
import com.mcode.connector.dispatch.event.DataUploadHandler;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
import com.mcode.connector.type.QosType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
//业务系统 --  http -- dispatcher --- mq  -- tcp  -  eventbus  - tcp - 设备
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
