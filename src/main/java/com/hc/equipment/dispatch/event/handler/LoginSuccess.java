package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginSuccess extends AsyncEventHandler {

    @Override
    public void accept(TransportEventEntry event) {
        log.info("登陆成功，{}", event);
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.LOGIN_SUCCESS.getType();
    }
}
