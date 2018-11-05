package com.hc.equipment.dispatch.event;


import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.dispatch.event.handler.ConfigDiscover;
import com.hc.equipment.dispatch.event.handler.InstructionHandler;
import com.hc.equipment.dispatch.event.handler.LoginFail;
import com.hc.equipment.dispatch.event.handler.LoginSuccess;
import com.hc.equipment.dispatch.event.handler.Pong;
import com.hc.equipment.dispatch.event.handler.RegisterFail;
import com.hc.equipment.dispatch.event.handler.RegisterSuccess;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
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
public class EventHandlerPipeline implements InitializingBean, Cloneable {
    //一个请求对应一个pipeline
    private static Map<String, EventHandlerPipeline> pipelineMap = new LRUHashMap<>();
    //pipeline中的消费者事件字典
    private Map<Integer, Consumer<TransportEventEntry>> eventHandler = new HashMap<>();
    //单例默认pipeline
    private static final EventHandlerPipeline defaultPipeline = new EventHandlerPipeline();

    /**
     * 根据流水号获取pipeline
     *
     * @param seriaId 流水号
     * @return pipeline
     */
    public static EventHandlerPipeline getBySerialId(String seriaId) {
        return pipelineMap.get(seriaId);
    }

    /**
     * 为新的请求添加pipeline
     *
     * @param seriaId  流水号
     * @param pipeline pipeline
     */
    public static void addPipeline(String seriaId, EventHandlerPipeline pipeline) {
        pipelineMap.put(seriaId, pipeline);
    }

    /**
     * 获取默认的pipeline
     *
     * @return pipeline
     */
    public static EventHandlerPipeline getDefaultPipeline() {
        return defaultPipeline;
    }

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

    @Override
    public void afterPropertiesSet() throws Exception {
        defaultPipeline.addEventHandler(SpringContextUtil.getBean(ConfigDiscover.class))
                .addEventHandler(SpringContextUtil.getBean(InstructionHandler.class))
                .addEventHandler(SpringContextUtil.getBean(LoginFail.class))
                .addEventHandler(SpringContextUtil.getBean(LoginSuccess.class))
                .addEventHandler(SpringContextUtil.getBean(RegisterFail.class))
                .addEventHandler(SpringContextUtil.getBean(RegisterSuccess.class))
                .addEventHandler(SpringContextUtil.getBean(Pong.class));
    }
}
