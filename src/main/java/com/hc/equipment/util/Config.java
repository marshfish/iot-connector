package com.hc.equipment.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "hc.commons")
@Data
public class Config {
    /**
     * 项目ID
     */
    private String artifactId;
    /**
     * 实例ID
     */
    private String instanceId;
    /**
     * 实例编号（由dispatcher端生成）
     * 仅用于生成分布式ID
     */
    private Integer instanceNumber;
    /**
     * 主机
     */
    private String host;
    /**
     * tcp端口号
     */
    private int tcpPort;
    /**
     * 回调域名
     */
    private String callbackDomain;
    /**
     * 同步调用主线程最大阻塞时间
     */
    private int maxBusBlockingTime;
    /**
     * 集群通信事件处理线程数
     */
    private int eventBusThreadNumber;
    /**
     * 集群通信事件队列容量
     */
    private int eventBusQueueSize;
    /**
     * rabbitMq相关配置
     */
    private String mqHost;
    private int mqPort;
    private String mqUserName;
    private String mqPwd;
    private String virtualHost;
    private String upQueueName;
    private String exchangeName;
    /**
     * 心跳超时，网络延时
     */
    private long timeout;
    /**
     * 心跳超时，断开连接
     */
    private long timeDisconnect;
}
