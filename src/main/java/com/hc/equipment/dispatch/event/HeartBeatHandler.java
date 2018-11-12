package com.hc.equipment.dispatch.event;

import com.google.gson.Gson;
import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.dispatch.event.handler.Pong;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 应用层心跳
 */
@Slf4j
@Component
@LoadOrder(value = 11)
public class HeartBeatHandler implements Bootstrap {
    @Resource
    private MqConnector mqConnector;
    @Resource
    private Gson gson;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private Pong pong;
    private long pingTime;
    private ScheduledExecutorService pingService = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setName("timer-ping-1");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void init() {
        log.info("load heart beat timer");
        Trans.event_data.Builder eventEntry = Trans.event_data.newBuilder();
        byte[] bytes = eventEntry.setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setType(EventTypeEnum.PING.getType()).
                setEqType(commonConfig.getEquipmentType()).
                setEqQueueName(mqConnector.getRoutingKey()).build().toByteArray();
        pingService.scheduleAtFixedRate(() -> {
            try {
                log.info("节点心跳");
                pingTime = System.currentTimeMillis();
                long timeout = pingTime - pong.getPongTime();
                if (timeout > commonConfig.getTimeout()) {
                    log.warn("心跳超时，存在网络延时大于{}ms", commonConfig.getTimeout());
                } else if (timeout > commonConfig.getTimeDisconnect()) {
                    log.warn("心跳超时，已断开连接");
                }
                eventEntry.setSerialNumber(String.valueOf(IdGenerator.buildDistributedId()));
                mqConnector.publish(bytes);
            } catch (Exception e) {
                log.error("心跳发生异常！，{}", e);
            }
        }, 30000, 15000, TimeUnit.MILLISECONDS);
    }

}
