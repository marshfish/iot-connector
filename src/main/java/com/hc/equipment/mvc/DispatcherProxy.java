package com.hc.equipment.mvc;

import com.google.gson.Gson;
import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.http.vo.BaseResult;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.ReflectionUtil;
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
    private DeviceSocketManager deviceSocketManager;
    @Resource
    private Config config;
    private Gson gson = new Gson();
    private static final ConcurrentHashMap<Class<?>, Object> BEAN_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MappingEntry> TCP_INSTRUCTION_MAPPING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MappingEntry> HTTP_INSTRUCTION_MAPPING = new ConcurrentHashMap<>();

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
            if (cls.isAnnotationPresent(TcpInstructionManager.class)) {
                BEAN_MAP.put(cls, ReflectionUtil.newInstance(cls));
            } else if (cls.isAnnotationPresent(HttpInstructionManager.class)) {
                BEAN_MAP.put(cls, ReflectionUtil.newInstance(cls));
            }
        }
        for (Class<?> cls : BEAN_MAP.keySet()) {
            Method[] methods = cls.getMethods();
            HttpInstructionManager manager;
            if (cls.isAnnotationPresent(TcpInstructionManager.class)) {
                for (Method method : methods) {
                    TcpInstruction TcpInstruction;
                    if ((TcpInstruction = method.getAnnotation(TcpInstruction.class)) != null) {
                        String value = TcpInstruction.value();
                        TCP_INSTRUCTION_MAPPING.put(value, new MappingEntry(Optional.ofNullable(BEAN_MAP.get(cls))
                                .orElseThrow(RuntimeException::new), method));
                    }
                }
            } else if ((manager = cls.getAnnotation(HttpInstructionManager.class)) != null) {
                for (Method method : methods) {
                    HttpInstruction httpInstruction;
                    if ((httpInstruction = method.getAnnotation(HttpInstruction.class)) != null) {
                        String uriKey = manager.value() + httpInstruction.value() + httpInstruction.method().toUpperCase();
                        HTTP_INSTRUCTION_MAPPING.put(uriKey, new MappingEntry(Optional.ofNullable(BEAN_MAP.get(cls))
                                .orElseThrow(RuntimeException::new), method));
                    }
                }
            }

        }
    }

    public String routingTCP(String instruction, String deviceUniqueId) {
        return Optional.ofNullable(deviceUniqueId).
                map(mappingEntry -> instruction).
                map(data -> deviceSocketManager.getProtocolNumber(instruction)).
                map(TCP_INSTRUCTION_MAPPING::get).
                map(mappingEntry -> (String) ReflectionUtil.invokeMethod(
                        mappingEntry.getObject(),
                        mappingEntry.getMethod(),
                        new ParamEntry(deviceUniqueId, instruction))).
                orElseGet(() -> {
                    log.warn("无法识别的协议号：{}", instruction);
                    return null;
                });
    }

    public String routingHTTP(String uri, String method, String jsonBody) {
        return Optional.ofNullable(uri).
                map(path -> HTTP_INSTRUCTION_MAPPING.get(path + method)).
                map(mappingEntry -> {
                    Method invokeMethod = mappingEntry.getMethod();
                    Class<?>[] parameterTypes = invokeMethod.getParameterTypes();
                    Object param = gson.fromJson(jsonBody.
                            replace("\n", "").
                            replace("\t", ""), parameterTypes[0]);
                    return (BaseResult) ReflectionUtil.invokeMethod(
                            mappingEntry.getObject(),
                            invokeMethod,
                            param);
                }).
                map(baseResult -> gson.toJson(baseResult)).
                orElseGet(() -> {
                    log.warn("找不到该URI的处理器：{},参数：{}", uri, jsonBody);
                    return null;
                });
    }

}
