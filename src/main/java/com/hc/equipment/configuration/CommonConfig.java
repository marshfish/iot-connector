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
    private String artifactId;
    /**
     * 实例ID
     */
    private String connectorId;
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
    private String devCallbackDomain;
    /**
     * 协议类型
     */
    private Integer protocol;
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
     * 心跳超时，网络延时事件
     */
    private long timeout;
    /**
     * 心跳超时，断开连接事件
     */
    private long timeDisconnect;
    /**
     * 设备类型
     */
    private Integer equipmentType;
    /**
     * 回调域名缓存
     */
    private Map<Integer, String> domainMap = new HashMap<>();

    public String getDomain(Integer domainType) {
        return domainMap.get(domainType);
    }

    public void addDomain(Integer domainType, String domain) {
        domainMap.put(domainType, domain);
    }
}
