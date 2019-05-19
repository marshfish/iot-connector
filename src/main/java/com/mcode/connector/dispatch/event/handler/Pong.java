package com.mcode.connector.dispatch.event.handler;

import com.mcode.connector.dispatch.event.AsyncEventHandler;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
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
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.PONG;
    }

    public long getPongTime() {
        return pongTime;
    }
}
