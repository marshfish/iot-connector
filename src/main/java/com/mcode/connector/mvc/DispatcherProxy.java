package com.mcode.connector.mvc;

import com.mcode.connector.Bootstrap;
import com.mcode.connector.LoadOrder;
import com.mcode.connector.device.DeviceSocketManager;
import com.mcode.connector.util.ReflectionUtil;
import com.mcode.connector.util.SpringContextUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@LoadOrder(value = 3)
public class DispatcherProxy implements Bootstrap {
    @Resource
    private DeviceSocketManager deviceSocketManager;
    private static final Map<String, MappingEntry> TCP_INSTRUCTION_MAPPING = new HashMap<>();
    private static final String HEARTBEAT_PACKAGE = "APHB";

    @SneakyThrows
    @Override
    public void init() {
        log.info("load tcp mvc dispatcher");
        SpringContextUtil.getContext().getBeansWithAnnotation(TcpRouterManager.class).
                forEach((beanName, object) -> Stream.of(object.getClass().getMethods()).
                        forEach(method -> initContainer(object, method)));
    }

    private void initContainer(Object object, Method method) {
        TcpRouter route;
        if ((route = method.getAnnotation(TcpRouter.class)) != null) {
            String uriKey = route.value();
            TCP_INSTRUCTION_MAPPING.put(uriKey, new MappingEntry(object, method));
            TCP_INSTRUCTION_MAPPING.put(HEARTBEAT_PACKAGE, null);
        }
    }

    public String routingTCP(String instruction, String equipmentId) {
        return Optional.ofNullable(equipmentId).
                map(mappingEntry -> instruction).
                map(data -> deviceSocketManager.setProtocolNumber(instruction)).
                map(TCP_INSTRUCTION_MAPPING::get).
                map(mappingEntry -> (String) ReflectionUtil.invokeMethod(
                        mappingEntry.getObject(),
                        mappingEntry.getMethod(),
                        new ParamEntry(equipmentId, instruction))).orElse(null);
    }
}
