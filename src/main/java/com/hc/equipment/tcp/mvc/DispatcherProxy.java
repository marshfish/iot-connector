package com.hc.equipment.tcp.mvc;

import com.hc.equipment.device.CommonDevice;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.ReflectionUtil;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DispatcherProxy {
    @Resource
    private CommonDevice commonDevice;
    @Resource
    private Config config;

    private static final ConcurrentHashMap<Class<?>, Object> BEAN_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, InvokeEntry> INSTRUCTION_MAPPING = new ConcurrentHashMap<>();

    public void loadMVC() {
        List<String> controllerPath = config.getControllerPath();
        Set<Class<?>> classSet = new HashSet<>();
        try {
            for (String path : controllerPath) {
                Class<?> aClass = Class.forName(path);
                classSet.add(aClass);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        for (Class<?> cls : classSet) {
            if (cls.isAnnotationPresent(InstructionManager.class)) {
                BEAN_MAP.put(cls, ReflectionUtil.newInstance(cls));
            }
        }
        for (Class<?> cls : BEAN_MAP.keySet()) {
            Method[] methods = cls.getMethods();
            for (Method method : methods) {
                Instruction instruction;
                if ((instruction = method.getAnnotation(Instruction.class)) != null) {
                    String value = instruction.value();
                    INSTRUCTION_MAPPING.put(value, new InvokeEntry(Optional.ofNullable(BEAN_MAP.get(cls))
                            .orElseThrow(RuntimeException::new), method));
                }
            }
        }
    }

    public String routing(String data, String deviceUniqueId) {
        String protocolNumber = commonDevice.getProtocolNumber(data);
        InvokeEntry invokeEntry;
        if ((invokeEntry = INSTRUCTION_MAPPING.get(protocolNumber)) != null) {
            return (String) ReflectionUtil.invokeMethod(invokeEntry.getObject(),
                    invokeEntry.getMethod(), deviceUniqueId, data);
        } else {
            log.warn("无法识别的协议号：{}", data);
            return null;
        }
    }

}
