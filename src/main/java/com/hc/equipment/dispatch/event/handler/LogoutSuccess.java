package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.SyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogoutSuccess extends SyncEventHandler {
    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.LOGOUT_SUCCESS;
    }
}
