package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.device.SocketContainer;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class Monitor extends AsyncEventHandler {
    @Resource
    private SocketContainer container;
    @Resource
    private MqConnector mqConnector;
    @Resource
    private CommonConfig commonConfig;

    @Override
    public void accept(Trans.event_data event) {
        String all = container.monitorAll();
        String id = String.valueOf(IdGenerator.buildDistributedId());
        byte[] bytes = Trans.event_data.newBuilder().
                setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setType(EventTypeEnum.MONITOR_DATA.getType()).
                setSerialNumber(id).
                setTimeStamp(System.currentTimeMillis()).
                setDispatcherId(event.getDispatcherId()).
                setMsg(all).
                build().
                toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, id);
        mqConnector.publishAsync(publishEvent);
        log.info("监控api，上传数据");
    }

    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.MONITOR;
    }

}
