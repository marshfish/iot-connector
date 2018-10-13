package com.hc.equipment.http;

import com.hc.equipment.device.CommonDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/vertx")
public class WriststrapHTTPController {
    @Resource
    private CommonDevice commonDevice;

    @GetMapping("/websocket/resources")
    public Mono getConcurrentConnection() {
        return new Mono() {
            @Override
            public void subscribe(CoreSubscriber coreSubscriber) {

            }
        };
    }

}
