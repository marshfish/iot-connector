package com.hc.equipment.dispatch;

import com.hc.equipment.EquipmentTcpApplication;
import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.util.Config;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 监听集群eventbus消息
 */
@Slf4j
@Component
public class EventBusListener {
    @Resource
    private Config config;
    @Resource
    private MqEventDownStream downStream;

    /**
     * 获取并分发该节点的消息
     */
    public void init() {
        EquipmentTcpApplication.getEventBus().consumer(config.getInstanceId(),
                (Handler<Message<TransportEventEntry>>) event -> {
            TransportEventEntry eventEntry = event.body();
            downStream.handlerMessage(eventEntry);
        });
    }

    /**
     * 推送给Connector
     *
     * @param serialNumber 指令流水号
     * @param message      消息
     */
    public void publish(String serialNumber, Object message) {
        publish(serialNumber, message, null);
    }

    public void publish(String serialNumber, Object message, CaseInsensitiveHeaders headers) {
        publish(serialNumber, message, headers, 30 * 1000);
    }

    public void publish(String serialNumber, Object message, CaseInsensitiveHeaders headers, long sendTimeout) {
        EquipmentTcpApplication.getEventBus().publish(serialNumber, message, new DeliveryOptions()
                .setHeaders(headers).setSendTimeout(sendTimeout));
    }

}
