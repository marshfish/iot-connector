package com.hc.equipment;

import com.hc.equipment.tcp.mvc.DispatcherProxy;
import com.hc.equipment.bootstrap.ServerVerticle;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EquipmentTcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentTcpApplication.class, args);
        Vertx.vertx().deployVerticle(SpringContextUtil.getBean(ServerVerticle.class));
        SpringContextUtil.getBean(DispatcherProxy.class).loadMVC();
    }

}
