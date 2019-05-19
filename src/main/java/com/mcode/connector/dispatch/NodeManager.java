package com.mcode.connector.dispatch;

import com.mcode.connector.Bootstrap;
import com.mcode.connector.LoadOrder;
import com.mcode.connector.configuration.CommonConfig;
import com.mcode.connector.rpc.MqConnector;
import com.mcode.connector.rpc.PublishEvent;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
import com.mcode.connector.util.IdGenerator;
import com.mcode.connector.util.SpringContextUtil;
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
            if (type == EventTypeEnum.REGISTER_ERROR.getType()) {
                log.error("节点参数错误！检查该节点的协议与设备类型配置：{}", event.getMsg());
                System.exit(1);
            }
            if (type == EventTypeEnum.REGISTER_FAIL.getType()) {
                log.warn("节点重复登陆:{}", event.getMsg());
            }
            log.info("【{}】 节点在【{}】注册成功", commonConfig.getNodeArtifactId(), event.getDispatcherId());
        } else {
            log.warn("节点注册失败:1.同步调用超时 2.dispatcher节点挂掉，后续心跳将尝试注册到dispatcher端直到注册成功");
        }
    }

}
