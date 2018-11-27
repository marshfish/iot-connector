package com.hc.equipment.dispatch.event;

import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@LoadOrder(value = 6)
public class PipelineContainer implements Bootstrap {
    //一个请求对应一个pipeline
    private static Map<String, EventHandlerPipeline> pipelineMap = new ConcurrentHashMap<>(200);
    //默认公共的pipeline，所有实现了EventHandler的子类都会被注册到默认的pipeline中
    private static final EventHandlerPipeline defaultPipeline = new EventHandlerPipeline();

    //获取默认的pipeline
    public EventHandlerPipeline getDefaultPipeline() {
        return defaultPipeline;
    }

    //根据流水号获取pipeline
    public EventHandlerPipeline getPipelineBySerialId(String seriaId) {
        return pipelineMap.get(seriaId);
    }

    //添加pipeline
    public void addPipeline(String seriaId, EventHandlerPipeline pipeline) {
        pipelineMap.put(seriaId, pipeline);
    }

    //卸载pipeline
    public void removePipeline(String seriaId) {
        pipelineMap.remove(seriaId);
    }

    /**
     * 初始化defaultPipeline
     */
    @Override
    public void init() {
        log.info("load pipeline container");
        SpringContextUtil.getContext().getBeansOfType(EventHandler.class).
                forEach((name, handler) -> defaultPipeline.addEventHandler(handler));
    }

}
