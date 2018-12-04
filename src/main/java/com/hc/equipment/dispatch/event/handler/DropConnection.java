package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.ClusterManager;
import com.hc.equipment.dispatch.NodeManager;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 节点断线重连
 */
@Slf4j
@Component
public class DropConnection extends AsyncEventHandler {
    @Resource
    private NodeManager nodeManager;

    @Override
    public void accept(Trans.event_data event) {
        //节点超时断开，尝试重连。注意要新开线程，否则可能死锁
        //如果能收到dropConnection的消息，说明之前有一段时间由于一些原因（网络延时、dispatcher/mq/redis宕机等）导致心跳超时
        //但现在的通信链路正常，仅需重新注册该节点即可
        log.warn("节点断开，尝试与dispatcher重新连接");
        ClusterManager.getVertx().executeBlocking(event1 -> {
            nodeManager.init();
            event1.complete();
        }, event12 -> {
            //do nothing
        });
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.DROPPED.getType();
    }
}
