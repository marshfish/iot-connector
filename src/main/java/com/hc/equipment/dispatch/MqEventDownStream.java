package com.hc.equipment.dispatch;

import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.dispatch.event.EventHandlerPipeline;
import com.hc.equipment.dispatch.event.PipelineContainer;
import com.hc.equipment.configuration.CommonConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@Data
@Component
public class MqEventDownStream implements InitializingBean {
    @Resource
    private CallbackManager callbackManager;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private PipelineContainer pipelineContainer;

    private static Queue<TransportEventEntry> eventQueue;
    private static ExecutorService eventExecutor;

    private void initQueue() {
        eventQueue = new LinkedBlockingQueue<>(commonConfig.getEventBusQueueSize());
        eventExecutor = Executors.newFixedThreadPool(commonConfig.getEventBusThreadNumber(), new ThreadFactory() {
            private AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread();
                thread.setDaemon(true);
                thread.setName("event-poller-" + count.getAndIncrement());
                return thread;
            }
        });
    }

    public void handlerMessage(TransportEventEntry transportEventEntry) {
        if (!eventQueue.add(transportEventEntry)) {
            log.warn("HttpUpStream事件处理队列已满");
        }
    }


    private void exeEventLoop() {
        eventExecutor.submit((Runnable) () -> {
            while (true) {
                TransportEventEntry event = eventQueue.poll();
                if (event != null) {
                    try {
                        Integer eventType = event.getType();
                        String serialNumber = event.getSerialNumber();
                        Consumer<TransportEventEntry> consumer;
                        //暂时没有特殊需求，使用默认pipeline即可，如果需要动态添加/删除，须在TCPDownStream配置
                        EventHandlerPipeline pipeline = pipelineContainer.getPipelineBySerialId(serialNumber);
                        if (pipeline == null) {
                            pipeline = pipelineContainer.getDefaultPipeline();
                        }
                        if ((consumer = pipeline.adaptEventHandler(eventType)) != null) {
                            consumer.accept(event);
                            pipelineContainer.removePipeline(serialNumber);
                        } else {
                            log.warn("未经注册的事件，{}", event);
                        }
                    } catch (Exception e) {
                        log.warn("事件处理异常，event;{},e:{}", event, e);
                    }
                }
            }
        });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        initQueue();
        exeEventLoop();
    }
}
