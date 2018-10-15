package com.hc.equipment.http.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class WriststrapSetSOSDTO extends EquipmentDTO {
    @NotNull
    private List<String> sosContact;
}
