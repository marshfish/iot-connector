package com.mcode.connector.dispatch.event.handler;

import com.mcode.connector.dispatch.event.SyncEventHandler;
import com.mcode.connector.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RegisterSuccess extends SyncEventHandler {
    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.REGISTER_SUCCESS;
    }
}
