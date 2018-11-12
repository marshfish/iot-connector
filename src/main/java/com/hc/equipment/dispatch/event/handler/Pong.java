package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Pong extends AsyncEventHandler {
    private long pongTime = System.currentTimeMillis();

    @Override
    public void accept(TransportEventEntry event) {
        pongTime = System.currentTimeMillis();
        log.info("收到来自服务器的pong");
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.PONG.getType();
    }

    public long getPongTime() {
        return pongTime;
    }
}