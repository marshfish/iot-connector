package com.hc.equipment.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "connector.commons")
@Data
public class CommonConfig {
    /**
     * 项目ID
     */
    private String nodeArtifactId;
    /**
     * 主机
     */
    private String host;
    /**
     * tcp端口号
     */
    private int tcpPort;
    /**
     * 协议类型
     */
    private Integer protocol;
    /**
     * 同步调用主线程最大阻塞时间
     */
    private int maxBusBlockingTime;
    /**
     * 集群通信事件队列容量
     */
    private int eventBusQueueSize;
    /**
     * rpc心跳超时，网络延时事件
     */
    private long timeout;
    /**
     * rpc心跳超时，断开连接事件
     */
    private long timeDisconnect;
    /**
     * 设备心跳超时
     */
    private long tcpTimeout;
    /**
     * 设备类型
     */
    private Integer equipmentType;
    /**
     * 默认消息重发窗口时间
     */
    private Integer defaultTimeout;
}
