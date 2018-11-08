package com.hc.equipment.connector;

import lombok.Data;

/**
 * dispatcher—connector 通信消息格式
 */
@Data
public class TransportEventEntry {
    /**
     * 事件类型
     */
    private Integer type;
    /**
     * 设备唯一编号
     */
    private String eqId;
    /**
     * 设备类型
     */
    private Integer eqType;
    /**
     * connector节点项目名
     */
    private String nodeArtifactId;
    /**
     * disptcher节点ID
     */
    private String dispatcherId;
    /**
     * connector节点ID
     */
    private String connectorId;
    /**
     * 指令流水号
     */
    private String serialNumber;
    /**
     * 设备协议
     */
    private Integer protocol;
    /**
     * 事件消息
     */
    private Object msg;
}
