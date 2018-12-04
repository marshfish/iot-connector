package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Pong extends AsyncEventHandler {
    private long pongTime = System.currentTimeMillis();

    @Override
    public void accept(Trans.event_data event) {
        pongTime = System.currentTimeMillis();
        log.info("pong");
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.PONG.getType();
    }

    public long getPongTime() {
        return pongTime;
    }
}
