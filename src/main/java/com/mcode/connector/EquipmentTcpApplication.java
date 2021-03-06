package com.mcode.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@SpringBootApplication
public class EquipmentTcpApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(EquipmentTcpApplication.class);
        springApplication.setWebEnvironment(false);
        Map<String, Bootstrap> beansOfType = springApplication.run(args).getBeansOfType(Bootstrap.class);
        bootstrap(beansOfType);
    }

    /**
     * 按顺序初始化启动类
     *
     * @param beansOfType 继承启动接口的beanMap
     */
    public static void bootstrap(Map<String, Bootstrap> beansOfType) {
        List<Bootstrap> sortList = new ArrayList<>(beansOfType.values());
        sortList.stream().sorted((o1, o2) -> {
            Integer finalOrder = Integer.MAX_VALUE;
            Integer o1Sort = Optional.ofNullable(o1.getClass().getAnnotation(LoadOrder.class)).
                    map(LoadOrder::value).orElse(finalOrder);
            Integer o2Sort = Optional.ofNullable(o2.getClass().getAnnotation(LoadOrder.class)).
                    map(LoadOrder::value).orElse(finalOrder);
            return o1Sort - o2Sort;
        }).forEach(Bootstrap::init);
    }

}
