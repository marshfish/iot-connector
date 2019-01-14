package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginSuccess extends AsyncEventHandler {

    @Override
    public void accept(Trans.event_data event) {
        log.info("设备登陆成功，{}", event.asString());
    }

    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.LOGIN_SUCCESS;
    }
}
