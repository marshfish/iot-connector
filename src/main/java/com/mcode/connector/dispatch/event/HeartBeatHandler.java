package com.mcode.connector.dispatch.event;

import com.mcode.connector.Bootstrap;
import com.mcode.connector.LoadOrder;
import com.mcode.connector.configuration.CommonConfig;
import com.mcode.connector.device.SocketContainer;
import com.mcode.connector.dispatch.event.handler.Pong;
import com.mcode.connector.rpc.MqConnector;
import com.mcode.connector.rpc.PublishEvent;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
import com.mcode.connector.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    private CommonConfig commonConfig;
    @Resource
    private Pong pong;
    @Resource
    private SocketContainer socketContainer;
    @Resource
    private DataUploadHandler dataUploadHandler;

    private ScheduledExecutorService pingService = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setName("timer-ping-1");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void init() {
        log.info("load heart beat timer");
        initMqHeartbeat();
    }

    private void initMqHeartbeat() {
        String id = String.valueOf(IdGenerator.buildDistributedId());
        Trans.event_data.Builder eventEntry = Trans.event_data.newBuilder();
        byte[] bytes = eventEntry.setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setSerialNumber(id).
                setType(EventTypeEnum.PING.getType()).
                setEqType(commonConfig.getEquipmentType()).
                build().toByteArray();
        pingService.scheduleAtFixedRate(new Runnable() {
            private int flag = 0;

            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    heartBeat(now);
                    deadChainCheck(now);
                } catch (Exception e) {
                    log.error("心跳发生异常！，{}", e);
                }
            }

            private void deadChainCheck(long now) {
                //默认内存清理间隔是心跳时长的5倍
                if (flag++ == 5) {
                    socketContainer.doTimeout(now);
                    dataUploadHandler.doTimeout(now);
                    flag = 0;
                }
            }

            private void heartBeat(long now) {
                long timeout = now - pong.getPongTime();
                log.info("节点心跳，距上次心跳时间：{}",timeout);
                PublishEvent publishEvent = new PublishEvent(bytes, id);
                mqConnector.publishAsync(publishEvent);
            }
        }, 30000, 60000, TimeUnit.MILLISECONDS);
    }

}
