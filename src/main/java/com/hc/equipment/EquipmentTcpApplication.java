package com.hc.equipment;

import com.google.gson.Gson;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hc.equipment.connector.MqConnector;
import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.dispatch.EventBusListener;
import com.hc.equipment.dispatch.TCPDownStream;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class EquipmentTcpApplication {
    private static EventBus eventBus;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(EquipmentTcpApplication.class);
        springApplication.setWebEnvironment(false);
        springApplication.run(args);

        com.hc.equipment.util.Config beanConfig = SpringContextUtil.getBean(com.hc.equipment.util.Config.class);
        Config hazelcastConfig = new Config().
                setGroupConfig(new GroupConfig().
                        setName(beanConfig.getArtifactId()).
                        setPassword(beanConfig.getArtifactId()));

        HazelcastClusterManager manager = new HazelcastClusterManager(hazelcastConfig);
        VertxOptions vertxOptions = new VertxOptions().
                setPreferNativeTransport(true).
                setClusterManager(manager);
        Vertx.clusteredVertx(vertxOptions, EquipmentTcpApplication::bootstrapHandler);
    }

    public static void bootstrapHandler(AsyncResult<Vertx> event) {
        if (event.succeeded()) {
            Vertx vertx = event.result();
            vertx.deployVerticle(TCPDownStream.class, new DeploymentOptions().
                    setInstances(Runtime.getRuntime().availableProcessors()));
            eventBus = vertx.eventBus();
            SpringContextUtil.getBean(DispatcherProxy.class).init();
            SpringContextUtil.getBean(EventBusListener.class).init();
            registerInstance();
        } else {
            log.error("集群启动失败，{}", event.cause());
        }
    }

    public static EventBus getEventBus() {
        return eventBus;
    }

    /**
     * 向dispatcher注册该节点
     */
    public static void registerInstance() {
        com.hc.equipment.util.Config config = SpringContextUtil.getBean(com.hc.equipment.util.Config.class);
        MqConnector mqConnector = SpringContextUtil.getBean(MqConnector.class);
        TransportEventEntry eventEntry = new TransportEventEntry();
        String serialNumber = String.valueOf(IdGenerator.buildDistributedId());
        eventEntry.setInstanceId(config.getInstanceId());
        eventEntry.setType(EventTypeEnum.INSTANCE_REGISTER.getType());
        eventEntry.setSerialNumber(serialNumber);
        TransportEventEntry event = mqConnector.publishSync(new Gson().toJson(eventEntry), serialNumber);
        if (event.getType() == EventTypeEnum.REGISTER_SUCCESS.getType()) {
            log.info("{} 节点注册成功", config.getInstanceId());
        } else {
            throw new RuntimeException("节点注册失败" + event);
        }
    }

}
