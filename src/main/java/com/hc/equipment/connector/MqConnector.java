package com.hc.equipment.connector;

import com.google.gson.Gson;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.configuration.MqConfig;
import com.hc.equipment.dispatch.ClusterManager;
import com.hc.equipment.dispatch.MqEventDownStream;
import com.hc.equipment.dispatch.CallbackManager;
import com.hc.equipment.dispatch.event.EventHandler;
import com.hc.equipment.dispatch.event.EventHandlerPipeline;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Slf4j
public class MqConnector implements InitializingBean {
    @Resource
    private MqConfig mqConfig;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private Gson gson;
    @Resource
    private CallbackManager callbackManager;
    @Resource
    private MqEventDownStream downStream;
    @Resource
    private ClusterManager clusterManager;
    private static Connection connection;
    private static String QUEUE_MODEL = "direct";

    /**
     * 连接mq
     */
    @SneakyThrows
    private void connect() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqConfig.getMqHost());
        factory.setPort(mqConfig.getMqPort());
        factory.setUsername(mqConfig.getMqUserName());
        factory.setPassword(mqConfig.getMqPwd());
        factory.setVirtualHost(StringUtils.isBlank(mqConfig.getVirtualHost()) ? "/" : mqConfig.getVirtualHost());
        connection = factory.newConnection();
    }

    /**
     * MQ消费消息
     */
    private void consumer() {
        Channel channel;
        String connectorQueueName = mqConfig.getDownQueueName();
        String exchangeName = mqConfig.getExchangeName();
        try {
            //TODO 连接池？
            channel = connection.createChannel();
            channel.exchangeDeclare(exchangeName, QUEUE_MODEL, false);
            channel.queueDeclare(connectorQueueName, true, false, false, null);
            channel.queueBind(connectorQueueName, exchangeName, "");
            channel.basicConsume(connectorQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    //TODO try catch？
                    log.info("收到消息：{}", message);
                    Object connectorId;
                    if ((connectorId = properties.getHeaders().get("connectorId")) != null) {
                        clusterManager.publish((String) connectorId, message);
                    }
                }
            });
        } catch (IOException e) {
            log.error("rabbitmq连接失败，{}", e);
        }
    }

    /**
     * 与dispatcher同步通信
     * 同步发送消息需要注册一个eventHandler事件处理器，继承SyncEventHandler，并通过setEventType添加事件类型（枚举中定义）
     * 这个eventHandler将作为异步——同步的桥梁传递事件，dispatcher端响应对的结果将通过eventHandler同步返回
     * 若不注册eventHandler，则会导致无法接收到事件，同步调用超时，添加新的eventHandler后注意要通过
     * {@link EventHandlerPipeline#addEventHandler(EventHandler)}方法将事件处理器添加到pipeline才能生效
     * 回调流程详见 {@link com.hc.equipment.dispatch.event.EventHandler}
     * @param serialNumber 流水号
     * @param message 消息
     * @return TransportEventEntry
     */
    public TransportEventEntry producerSync(String serialNumber, String message) {
        return this.producerSync(serialNumber, message, null);
    }

    /**
     * 与dispatcher同步通信
     * @param message      消息
     * @param serialNumber 消息流水号
     * @return dispatcher端返回事件
     */
    public TransportEventEntry producerSync(String serialNumber, String message, Map<String, Object> headers) {
        SyncWarpper warpper = new SyncWarpper();
        Consumer<TransportEventEntry> consumerProxy = warpper.mockCallback();
        callbackManager.registerCallbackEvent(serialNumber, consumerProxy);
        producer(message, headers);
        return warpper.blockingResult();
    }

    /**
     * 异步通信
     * @param message 消息
     */
    public void producer(String message) {
        this.producer(message, null);
    }

    /**
     * 向dispather异步推送消息
     *
     * @param message 消息体
     */
    public void producer(String message, Map<String, Object> headers) {
        String exchangeName = mqConfig.getExchangeName();
        try {
            //建立通道
            Channel channel = connection.createChannel();
            //交换机持久化
            channel.exchangeDeclare(exchangeName, QUEUE_MODEL, false);
            channel.queueBind(mqConfig.getUpQueueName(), exchangeName, "");
            //设置数据持久化
            AMQP.BasicProperties props = new AMQP.BasicProperties().builder().
                    deliveryMode(2).
                    headers(headers).
                    build();
            channel.basicPublish(exchangeName, "", props, message.getBytes("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 消息同步器
     */
    private class SyncWarpper {
        private volatile TransportEventEntry eventEntry;
        private CountDownLatch latch = new CountDownLatch(1);
        private long current = System.currentTimeMillis();

        public TransportEventEntry blockingResult() {
            try {
                boolean await = latch.await(commonConfig.getMaxBusBlockingTime(), TimeUnit.MILLISECONDS);
                if (!await) {
                    log.warn("同步调用超时，检查mq连接状态");
                    return new TransportEventEntry();
                }
            } catch (InterruptedException e) {
                log.warn("同步调用线程被中断,{}", e);
                return new TransportEventEntry();
            }
            log.info("同步调用返回结果:{},耗时：{}", eventEntry, System.currentTimeMillis() - current);
            return eventEntry;
        }

        public Consumer<TransportEventEntry> mockCallback() {
            return eventEntry -> {
                this.eventEntry = eventEntry;
                latch.countDown();
            };
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        consumer();
    }
}
