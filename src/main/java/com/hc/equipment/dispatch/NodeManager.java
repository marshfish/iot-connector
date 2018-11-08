package com.hc.equipment.dispatch;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.hazelcast.util.MapUtil;
import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.connector.MqConnector;
import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.type.EquipmentTypeEnum;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.util.IdGenerator;
import com.hc.equipment.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;

@Slf4j
@Component()
@LoadOrder(value = 4)
public class NodeManager implements Bootstrap {
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private Gson gson;

    /**
     * 节点注册
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        MqConnector mqConnector = SpringContextUtil.getBean(MqConnector.class);
        TransportEventEntry eventEntry = new TransportEventEntry();
        String serialNumber = String.valueOf(IdGenerator.buildDistributedId());
        eventEntry.setNodeArtifactId(commonConfig.getArtifactId());
        eventEntry.setType(EventTypeEnum.INSTANCE_REGISTER.getType());
        eventEntry.setSerialNumber(serialNumber);
        eventEntry.setEqType(EquipmentTypeEnum.WRISTSTRAP.getType());
        eventEntry.setProtocol(commonConfig.getProtocol());
        eventEntry.setDispatcherId("1");
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("dispatcherId", "1");
        TransportEventEntry event = mqConnector.producerSync(serialNumber,
                gson.toJson(eventEntry), headers);
        Integer eventType;
        if ((eventType = event.getType()) == null) {
            throw new RuntimeException("节点注册失败,检查与dispatcher的通信状态");
        }
        if (eventType != EventTypeEnum.REGISTER_SUCCESS.getType()) {
            throw new RuntimeException("节点注册失败，无权限注册本节点");
        }
        LinkedTreeMap<Integer, String> distributedConfig = (LinkedTreeMap<Integer, String>) event.getMsg();
        if (MapUtil.isNullOrEmpty(distributedConfig)) {
            throw new RuntimeException("获取回调配置信息失败");
        }
        //从dispatcher端拉取配置列表并注入
        distributedConfig.forEach((type, domain) -> commonConfig.addDomain(type, domain));
        //从dispatcher端拉取本节点ID并注入，ID由dispatcher端生成
        commonConfig.setConnectorId(event.getConnectorId());
        log.info("{} 节点注册成功", event.getConnectorId());
    }


}
