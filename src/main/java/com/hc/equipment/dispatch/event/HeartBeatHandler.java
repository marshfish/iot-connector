package com.hc.equipment.dispatch.event;

import com.google.gson.Gson;
import com.hc.equipment.connector.MqConnector;
import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.dispatch.event.handler.Pong;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用层心跳
 */
@Slf4j
@Component
public class HeartBeatHandler {
    @Resource
    private MqConnector mqConnector;
    @Resource
    private Gson gson;
    @Resource
    private Config config;
    @Resource
    private Pong pong;
    private ExecutorService pingService = Executors.newFixedThreadPool(1);
    private long pingTime;

    public void init() {
        TransportEventEntry eventEntry = new TransportEventEntry();
        eventEntry.setInstanceId(config.getInstanceId());
        eventEntry.setType(EventTypeEnum.PING.getType());
        pingService.submit(() -> {
            try {
                pingTime = System.currentTimeMillis();
                long timeout = pingTime - pong.getPongTime();
                if (timeout > config.getTimeout()) {
                    log.warn("心跳超时，存在网络延时大于{}ms", config.getTimeout());
                } else if (timeout > config.getTimeDisconnect()) {
                    log.warn("心跳超时，已断开连接");
                }
                eventEntry.setSerialNumber(String.valueOf(IdGenerator.buildDistributedId()));
                mqConnector.producer(gson.toJson(eventEntry));
            } catch (Exception e) {
                log.error("心跳发生异常！，{}", e);
            }
        });
    }

}
