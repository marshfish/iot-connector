package com.hc.equipment.dispatch;

import com.google.gson.Gson;
import com.hazelcast.config.GroupConfig;
import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.connector.TransportEventEntry;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Component
@LoadOrder(value = 1)
public class ClusterManager implements Bootstrap {
    @Resource
    private CommonConfig beanCommonConfig;
    @Resource
    private MqEventDownStream eventDownStream;
    @Resource
    private Gson gson;
    @Resource
    private CommonConfig commonConfig;
    private static EventBus eventBus;

    @Override
    public void init() {
        com.hazelcast.config.Config hazelcastConfig = new com.hazelcast.config.Config().
                setGroupConfig(new GroupConfig().
                        setName(beanCommonConfig.getArtifactId()).
                        setPassword(beanCommonConfig.getArtifactId()));

        HazelcastClusterManager manager = new HazelcastClusterManager(hazelcastConfig);
        VertxOptions vertxOptions = new VertxOptions().
                setPreferNativeTransport(true).
                setClusterManager(manager);
        Vertx.clusteredVertx(vertxOptions, this::bootstrapHandler);
    }

    private void bootstrapHandler(AsyncResult<Vertx> event) {
        if (event.succeeded()) {
            Vertx vertx = event.result();
            vertx.deployVerticle(TCPDownStream.class, new DeploymentOptions().
                    setInstances(Runtime.getRuntime().availableProcessors()));
            eventBus = vertx.eventBus();
            listen();
        } else {
            log.error("集群启动失败，{}", event.cause());
        }
    }

    /**
     * 监听本vertx节点的消息
     */
    private void listen() {
        log.info("load and listen eventBus ");
        //TODO
        ClusterManager.getEventBus().consumer(commonConfig.getArtifactId(),
                (Handler<Message<String>>) event -> {
                    String eventJson = event.body();
                    TransportEventEntry eventEntry = gson.fromJson(eventJson, TransportEventEntry.class);
                    eventDownStream.handlerMessage(eventEntry);
                });
    }

    /**
     * 发布消息到EventBus
     *
     * @param serialNumber 指令流水号
     * @param message      消息
     */
    public void publish(String serialNumber, Object message) {
        publish(serialNumber, message, null);
    }

    public void publish(String serialNumber, Object message, CaseInsensitiveHeaders headers) {
        publish(serialNumber, message, headers, 30 * 1000);
    }

    public void publish(String serialNumber, Object message, CaseInsensitiveHeaders headers, long sendTimeout) {
        Optional.ofNullable(ClusterManager.getEventBus()).ifPresent(eventBus ->
                eventBus.publish(serialNumber,
                        message,
                        new DeliveryOptions().setHeaders(headers).setSendTimeout(sendTimeout)));
    }


    /**
     * 获取集群Eventbus
     */
    public static EventBus getEventBus() {
        return eventBus;
    }
}
