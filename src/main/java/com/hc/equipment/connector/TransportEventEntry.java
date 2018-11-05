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
     * 节点ID
     */
    private String instanceId;
    /**
     * 指令流水号
     */
    private String serialNumber;
    /**
     * 事件消息
     */
    private String msg;
}
