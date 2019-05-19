package com.mcode.connector.dispatch.event;


import com.mcode.connector.Bootstrap;
import com.mcode.connector.LoadOrder;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
import com.mcode.connector.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * eventHandler流水线
 * 动态添加/停用eventHandler
 */
@Slf4j
@Component
@LoadOrder(6)
public class EventHandlerPipeline implements Bootstrap {
    //pipeline中的消费者事件字典
    private Map<EventTypeEnum, Consumer<Trans.event_data>> eventHandler = new EnumMap<>(EventTypeEnum.class);

    /**
     * 添加事件处理器
     *
     * @param eventHandler 时间处理器
     * @return pipeline
     */
    public EventHandlerPipeline addEventHandler(EventHandler eventHandler) {
        EventTypeEnum eventType = eventHandler.setReceivedEventType();
        addEventHandler(eventType, eventHandler);
        log.info("注册事件：{}", eventType);
        return this;
    }

    /**
     * 添加事件处理器
     *
     * @param eventType 事件类型
     * @param consumer  事件
     */
    public void addEventHandler(EventTypeEnum eventType, Consumer<Trans.event_data> consumer) {
        eventHandler.put(eventType, consumer);
    }

    public void removeEventHandler(Integer eventType) {
        eventHandler.remove(eventType);
    }

    public Consumer<Trans.event_data> choose(EventTypeEnum eventType) {
        return eventHandler.get(eventType);
    }

    /**
     * 初始化defaultPipeline
     */
    @Override
    public void init() {
        log.info("load pipeline container");
        SpringContextUtil.getContext().getBeansOfType(EventHandler.class).values().
                forEach(this::addEventHandler);
    }
}
