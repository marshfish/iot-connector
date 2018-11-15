package com.hc.equipment.dispatch;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.rpc.serialization.Trans;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private NodeManager nodeManager;
    private CountDownLatch latch = new CountDownLatch(1);
    private static EventBus eventBus;

    @Override
    @SneakyThrows
    public void init() {
        log.info("load cluster manager");
        Config hazelcastConfig = new Config().
                setGroupConfig(new GroupConfig().
                        setName(beanCommonConfig.getNodeArtifactId()).
                        setPassword(beanCommonConfig.getNodeArtifactId()));

        HazelcastClusterManager manager = new HazelcastClusterManager(hazelcastConfig);
        VertxOptions vertxOptions = new VertxOptions().
                setPreferNativeTransport(true).
                setClusterManager(manager);
        Vertx.clusteredVertx(vertxOptions, this::bootstrapHandler);
        latch.await(10000, TimeUnit.MILLISECONDS);
    }

    private void bootstrapHandler(AsyncResult<Vertx> event) {
        if (event.succeeded()) {
            Vertx vertx = event.result();
            vertx.deployVerticle(TCPDownStream.class, new DeploymentOptions().
                    setInstances(Runtime.getRuntime().availableProcessors()));
            eventBus = vertx.eventBus();
            listen();
            latch.countDown();
        } else {
            log.error("集群启动失败，{}", event.cause());
        }
    }

    /**
     * 监听本vertx节点的消息
     */
    private void listen() {
        log.info("load and listen eventBus ");
        ClusterManager.getEventBus().consumer(commonConfig.getNodeArtifactId(),
                (Handler<Message<byte[]>>) event -> {
                    try {
                        byte[] bytes = event.body();
                        Trans.event_data eventData = Trans.event_data.parseFrom(bytes);
                        TransportEventEntry eventEntry = TransportEventEntry.parseTrans2This(eventData);
                        //TODO 消息过期  消息重发
                        log.info("***: {}, {}"+eventEntry.getType(),eventEntry.getSerialNumber());
                        eventDownStream.handlerMessage(eventEntry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
