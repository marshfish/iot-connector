package com.hc.equipment.dispatch;

import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import com.hc.equipment.util.SpringContextUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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
                build().toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, serialNumber);
        Trans.event_data event = mqConnector.publishSync(publishEvent);
        if (event != null) {
            int type = event.getType();
            if (type != EventTypeEnum.REGISTER_SUCCESS.getType()) {
                throw new RuntimeException("节点注册失败，" + event.getMsg());
            }
            log.info("【{}】 节点在【{}】注册成功", commonConfig.getNodeArtifactId(),event.getDispatcherId());
        } else {
            throw new RuntimeException("节点注册失败,检查与dispatcher的通信状态,查看dispatcher端日志");
        }
    }

}
