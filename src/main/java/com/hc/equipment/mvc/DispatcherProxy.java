package com.hc.equipment.mvc;

import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.util.ReflectionUtil;
import com.hc.equipment.util.SpringContextUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@LoadOrder(value = 3)
public class DispatcherProxy implements Bootstrap {
    @Resource
    private DeviceSocketManager deviceSocketManager;
    private static final Map<String, MappingEntry> TCP_INSTRUCTION_MAPPING = new HashMap<>();

    @SneakyThrows
    @Override
    public void init() {
        log.info("load tcp mvc dispatcher");
        SpringContextUtil.getContext().getBeansWithAnnotation(TcpRouterManager.class).forEach((beanName, object) -> {
            Class<?> cls = object.getClass();
            Method[] methods = cls.getMethods();
            for (Method method : methods) {
                TcpRouter route;
                if ((route = method.getAnnotation(TcpRouter.class)) != null) {
                    String uriKey = route.value();
                    TCP_INSTRUCTION_MAPPING.put(uriKey, new MappingEntry(object, method));
                }
            }
        });
    }

    public String routingTCP(String instruction, String equipmentId) {
        return Optional.ofNullable(equipmentId).
                map(mappingEntry -> instruction).
                map(data -> deviceSocketManager.getProtocolNumber(instruction)).
                map(TCP_INSTRUCTION_MAPPING::get).
                map(mappingEntry -> (String) ReflectionUtil.invokeMethod(
                        mappingEntry.getObject(),
                        mappingEntry.getMethod(),
                        new ParamEntry(equipmentId, instruction))).orElse(null);
    }
}
