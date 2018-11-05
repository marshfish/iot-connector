package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.connector.TransportEventEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfigDiscover extends AsyncEventHandler {
    @Override
    public void accept(TransportEventEntry event) {

    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.CONFIG_DISCOVER.getType();
    }
}
