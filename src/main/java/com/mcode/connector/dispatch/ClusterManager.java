package com.mcode.connector.dispatch;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.mcode.connector.Bootstrap;
import com.mcode.connector.LoadOrder;
import com.mcode.connector.configuration.CommonConfig;
import com.mcode.connector.rpc.serialization.Trans;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@LoadOrder(value = 2)
public class ClusterManager implements Bootstrap {
    @Resource
    private CommonConfig beanCommonConfig;
    @Resource
    private MqEventDownStream eventDownStream;
    @Resource
    private CommonConfig commonConfig;
    private CountDownLatch latch = new CountDownLatch(1);
    private static EventBus eventBus;
    private static Vertx vertx;

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
            vertx = event.result();
            vertx.deployVerticle(TCPDownStream.class, new DeploymentOptions().
                    setInstances(Runtime.getRuntime().availableProcessors()));
            eventBus = vertx.eventBus();
            listen();
            latch.countDown();
        } else {
            log.error("集群启动失败，{}", Arrays.toString(event.cause().getStackTrace()));
            System.exit(1);
        }
    }

    /**
     * 监听本vertx节点的消息
     */
    private void listen() {
        log.info("load and listen eventBus ");
        eventBus.consumer(commonConfig.getNodeArtifactId(),
                (Handler<Message<byte[]>>) event -> {
                    try {
                        byte[] bytes = event.body();
                        eventDownStream.handlerMessage(Trans.event_data.parseFrom(bytes));
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

    /**
     * 获取vertx
     */
    public static Vertx getVertx() {
        return vertx;
    }
}
