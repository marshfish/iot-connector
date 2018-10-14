package com.hc.equipment;

import com.hc.equipment.bootstrap.HTTPVerticle;
import com.hc.equipment.bootstrap.TCPVerticle;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EquipmentTcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EquipmentTcpApplication.class, args);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors());
        Vertx.vertx().deployVerticle(TCPVerticle.class, deploymentOptions);
        Vertx.vertx().deployVerticle(new HTTPVerticle());
        SpringContextUtil.getBean(DispatcherProxy.class).loadMVC();
    }

}
