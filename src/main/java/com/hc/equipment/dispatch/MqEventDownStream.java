package com.hc.equipment.dispatch;

import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.dispatch.event.EventHandlerPipeline;
import com.hc.equipment.dispatch.event.PipelineContainer;
import com.hc.equipment.rpc.serialization.Trans;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Data
@Component
@LoadOrder(value = 2)
public class MqEventDownStream implements Bootstrap {
    @Resource
    private CallbackManager callbackManager;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private PipelineContainer pipelineContainer;
    private final Object lock = new Object();
    private ArrayBlockingQueue<Trans.event_data> eventQueue;
    private ExecutorService eventExecutor;

    private void initQueue() {
        eventQueue = new ArrayBlockingQueue<>(commonConfig.getEventBusQueueSize());
        eventExecutor = new ThreadPoolExecutor(1,
                1,
                0,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(200), r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("event-loop-1");
            return thread;
        });
    }

    public void handlerMessage(Trans.event_data event) {
        synchronized (lock) {
            log.info("放到队列：{}, {}", event.getType(), event.getSerialNumber());
            if (!eventQueue.offer(event)) {
                log.warn("HttpUpStream事件处理队列已满");
            }
            lock.notify();
        }
    }

    /**
     * eventLoop单线程，不要修改其线程数
     * 否则一定会出现线程安全问题
     */
    @SuppressWarnings({"Duplicates", "InfiniteLoopStatement"})
    private void exeEventLoop() {
        eventExecutor.execute(() -> {
            while (true) {
                Trans.event_data event;
                synchronized (lock) {
                    while ((event = eventQueue.poll()) == null) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Integer eventType = event.getType();
                    String serialNumber = event.getSerialNumber();
                    log.info("处理队列----：{} , {}", eventType, serialNumber);
                    Consumer<Trans.event_data> consumer;
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
                    log.warn("事件处理异常，event:{},e:{}", event.asString(), e);
                }
            }
        });
    }

    @Override
    public void init() {
        log.info("load event poller");
        initQueue();
        exeEventLoop();
    }
}
