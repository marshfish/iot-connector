package com.hc.equipment.device;

import com.google.gson.Gson;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.dispatch.LinkedMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Component
public class SocketContainer {
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private DeviceSocketManager deviceSocketManager;
    @Resource
    private Gson gson;
    //netSocket.hashcode() -> equipmentId
    private ConcurrentHashMap<Integer, String> socketIdMapping = new ConcurrentHashMap<>();
    private LinkedMap<String, NetSocket> linkedMap = new LinkedMap<>();

    @SuppressWarnings("UnusedAssignment")
    public void unRegisterSocket(Integer socketId, String eqId) {
        log.info("移除socket:id{}", socketId);
        if (linkedMap.remove(eqId)) {
            socketIdMapping.remove(socketId);
        }
    }

    public NetSocket getSocket(String eqId) {
        return Optional.ofNullable(eqId).
                map(linkedMap::get).
                orElse(null);
    }

    public String getEquipmentId(Integer socketHash) {
        return socketIdMapping.get(socketHash);
    }

    public boolean writeString(String eqId, String data) {
        return Optional.ofNullable(eqId).
                map(linkedMap::get).
                map(socket -> {
                    socket.write(Buffer.buffer(data, "UTF-8"));
                    log.info("向【{}】发送指令【{}】", eqId, data);
                    return true;
                }).orElseGet(() -> {
            log.warn("设备【{}】未登陆，无法推送指令", eqId);
            return false;
        });
    }

    public void registerSocket(String eqId, NetSocket element) {
        linkedMap.addTail(eqId, element);
        socketIdMapping.put(element.hashCode(), eqId);
    }


    public void doTimeout(long now) {
        linkedMap.onTimeout(node -> {
            if (node.getLastTime() + commonConfig.getTcpTimeout() < now) {
                NetSocket socket = node.getElement();
                log.info("心跳超时，断开TCP连接：{},{}，{}", socket.remoteAddress().host(),
                        socket.hashCode(),
                        node.getLastTime());
                //无需使用linkedNode删除，deviceLogout会间接调用unRegisterSocket
                deviceSocketManager.deviceLogout(socket.hashCode(), true);
                socket.close();
                return true;
            }
            return false;
        });
    }

    public void heartBeat(String eqId) {
        Optional.ofNullable(eqId).map(linkedMap::getNode).ifPresent(linkedMap::moveToTail);
    }

    public String monitorAll() {
        Set<Map.Entry<String, LinkedMap.Node<String, NetSocket>>> entries = linkedMap.getEntries();
        List<Response> list = new ArrayList<>(entries.size());
        entries.forEach(e -> list.add(new Response(e.getKey(), e.getValue().getLastTime())));
        return gson.toJson(list);
    }

    @Data
    class Response {
        private String eqId;
        private long connectTime;

        public Response(String eqId, long connectTime) {
            this.eqId = eqId;
            this.connectTime = connectTime;
        }
    }
}
