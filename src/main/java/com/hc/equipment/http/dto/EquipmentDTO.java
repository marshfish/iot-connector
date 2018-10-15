package com.hc.equipment.http.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class EquipmentDTO {
    /**
     * 设备唯一ID
     */
    @NotNull
    private String uniqueId;
    /**
     * 指令流水号
     */
    @NotNull
    private String serialNumber;
}
