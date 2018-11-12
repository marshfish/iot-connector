package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.dispatch.NodeManager;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
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
    public void accept(TransportEventEntry event) {
        nodeManager.init();
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.DROPPED.getType();
    }
}
