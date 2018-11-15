package com.hc.equipment.rpc;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.configuration.MqConfig;
import com.hc.equipment.type.QosType;
import com.hc.equipment.util.SpringContextUtil;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Data
public class PublishEvent {
    private Integer defaultTimeout = SpringContextUtil.getBean(CommonConfig.class).getDefaultTimeout();
    /**
     * 队列名
     */
    private String queue = SpringContextUtil.getBean(MqConfig.class).getUpQueueName();
    /**
     * 消息体
     */
    private byte[] message;
    /**
     * 消息流水号
     */
    private String serialNumber;
    /**
     * mq消息头
     */
    private Map<String, Object> headers = new HashMap<>(3);
    /**
     * 消息重发窗口时间
     */
    @Setter
    private Integer timeout;
    /**
     * 服务质量
     */
    @Setter
    private Integer qos;

    /**
     * 时间戳
     */
    private long timeStamp;

    public PublishEvent(byte[] message, String serialNumber) {
        this.message = message;
        this.serialNumber = serialNumber;
        this.timeout = defaultTimeout;
        this.qos = QosType.AT_MOST_ONCE.getType();
        this.timeStamp = System.currentTimeMillis();
    }

    /**
     * 添加rabbitMq消息头
     */
    public void addHeaders(String key, Object value) {
        headers.put(key, value);
    }

}
