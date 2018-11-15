package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.dispatch.NodeManager;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * 节点断线重连
 */
@Slf4j
@Component
public class DropConnection extends AsyncEventHandler {
    @Resource
    private NodeManager nodeManager;

    @Override
    public void accept(TransportEventEntry event) {
        //节点超时断开，尝试重连。注意要新开线程，否则可能死锁
        log.warn("节点断开，尝试与dispatcher重新连接");
        blockingOperation(() -> nodeManager.init());
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.DROPPED.getType();
    }
}
