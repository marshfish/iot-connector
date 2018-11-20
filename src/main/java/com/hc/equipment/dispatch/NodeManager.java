package com.hc.equipment.dispatch;

import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import com.hc.equipment.util.SpringContextUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Component()
@LoadOrder(value = 10)
public class NodeManager implements Bootstrap {
    @Resource
    private CommonConfig commonConfig;

    /**
     * 节点注册
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void init() {
        log.info("load node register");
        MqConnector mqConnector = SpringContextUtil.getBean(MqConnector.class);
        String serialNumber = String.valueOf(IdGenerator.buildDistributedId());

        Trans.event_data.Builder eventEntry = Trans.event_data.newBuilder();
        byte[] bytes = eventEntry.setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setType(EventTypeEnum.INSTANCE_REGISTER.getType()).
                setSerialNumber(serialNumber).
                setEqType(commonConfig.getEquipmentType()).
                setProtocol(commonConfig.getProtocol()).
                setEqQueueName(mqConnector.getQueue()).
                build().toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, serialNumber);
        TransportEventEntry event = mqConnector.publishSync(publishEvent);
        Optional.ofNullable(event).map(TransportEventEntry::getType).ifPresent(e -> {
            Integer eventType;
            if ((eventType = event.getType()) == null) {
                throw new RuntimeException("节点注册失败,检查与dispatcher的通信状态,查看dispatcher端日志");
            }
            if (eventType != EventTypeEnum.REGISTER_SUCCESS.getType()) {
                throw new RuntimeException("节点注册失败，" + event.getMsg());
            }
            log.info("{} 节点注册成功", commonConfig.getNodeArtifactId());
        });
    }

}
