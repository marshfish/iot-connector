package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;

@Slf4j
@Component
public class Dump extends AsyncEventHandler {
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private MqConnector mqConnector;

    @Override
    public void accept(Trans.event_data event) {
        log.info("dump本地线程");
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        String info = Arrays.toString(threadInfos);
        String id = String.valueOf(IdGenerator.buildDistributedId());
        byte[] bytes = Trans.event_data.newBuilder().
                setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setType(EventTypeEnum.DUMP.getType()).
                setSerialNumber(id).
                setTimeStamp(System.currentTimeMillis()).
                setDispatcherId(event.getDispatcherId()).
                setMsg(info).
                build().
                toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, id);
        mqConnector.publishAsync(publishEvent);
    }

    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.DUMP;
    }
}
