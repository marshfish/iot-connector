package com.hc.equipment.dispatch.event;


import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * eventHandler流水线
 * 动态添加/停用eventHandler
 */
@Slf4j
@Component
public class EventHandlerPipeline implements Cloneable {
    //pipeline中的消费者事件字典
    private Map<Integer, Consumer<TransportEventEntry>> eventHandler = new HashMap<>();

    /**
     * 添加事件处理器
     *
     * @param eventHandler 时间处理器
     * @return pipeline
     */
    public EventHandlerPipeline addEventHandler(EventHandler eventHandler) {
        Integer eventType = eventHandler.setReceivedEventType();
        addEventHandler(eventType, eventHandler);
        log.info("注册事件：{}", EventTypeEnum.getEnumByCode(eventType));
        return this;
    }

    /**
     * 添加事件处理器
     *
     * @param eventType 事件类型
     * @param consumer  事件
     */
    public void addEventHandler(Integer eventType, Consumer<TransportEventEntry> consumer) {
        eventHandler.put(eventType, consumer);
    }

    public void removeEventHandler(Integer eventType) {
        eventHandler.remove(eventType);
    }

    public Consumer<TransportEventEntry> adaptEventHandler(Integer eventType) {
        return eventHandler.get(eventType);
    }

    @Override
    protected Object clone() {
        EventHandlerPipeline pipeline = null;
        try {
            pipeline = (EventHandlerPipeline) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return pipeline;
    }

}
