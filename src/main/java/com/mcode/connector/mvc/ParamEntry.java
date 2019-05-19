package com.mcode.connector.mvc;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备公共参数
 */
@Slf4j
@Getter
public class ParamEntry {
    private String equipmentId;
    private String instruction;

    public ParamEntry(String equipmentId, String instruction) {
        this.equipmentId = equipmentId;
        this.instruction = instruction;
    }
}
