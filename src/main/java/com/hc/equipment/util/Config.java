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
    private String basePath;
    private List<String> controllerPath = new ArrayList<>();
    private int tcpPort;
    private String tcpHost;
}
