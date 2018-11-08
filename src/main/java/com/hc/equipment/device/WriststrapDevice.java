package com.hc.equipment.device;

import com.google.gson.Gson;
import com.hc.equipment.connector.MqConnector;
import com.hc.equipment.connector.TransportEventEntry;
import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.type.EquipmentTypeEnum;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.util.IdGenerator;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 手环设备注册表
 */
@Component
@Slf4j
public class WriststrapDevice extends AbsSocketManager {
    @Resource
    private Gson gson;
    @Resource
    private MqConnector mqConnector;
    @Resource
    private CommonConfig commonConfig;

    @Override
    public String getEquipmentId(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(6, data.length() - 1)).orElse(null);
    }

    @Override
    public String getProtocolNumber(String data) {
        return Optional.ofNullable(data).map(v -> data.substring(2, 6)).orElse(null);
    }

    //允许同一设备重复登陆
    @Override
    public String deviceLogin(NetSocket netSocket, String data) {
        String protocolNumber;
        String eqId = null;
        if ((protocolNumber = getProtocolNumber(data)) != null) {
            if (WriststrapProtocol.LOGIN.equals(protocolNumber)) {
                if ((eqId = getEquipmentId(data)) != null) {
                    //TODO 配置中心获取上行对列名
                    //TODO 一定要保证与缓存的注册表数据最终一致
                    String serialNumber = String.valueOf(IdGenerator.buildDistributedId());
                    TransportEventEntry event = new TransportEventEntry();
                    event.setEqId(eqId);
                    event.setEqType(EquipmentTypeEnum.WRISTSTRAP.getType());
                    event.setConnectorId(commonConfig.getArtifactId());
                    event.setSerialNumber(serialNumber);
                    event.setType(EventTypeEnum.DEVICE_LOGIN.getType());
                    event.setDispatcherId("1");

                    //异步转同步,交给dispatcher端做登陆/注册校验
                    HashMap<String, Object> headers = new HashMap<>();
                    headers.put("dispatcherId", "1");
                    TransportEventEntry eventResult = mqConnector.producerSync(
                            serialNumber,
                            gson.toJson(event),
                            headers);
                    if (eventResult.getType() == EventTypeEnum.LOGIN_SUCCESS.getType()) {
                        log.info("登陆成功，{}", eventResult);
                        registry.put(eqId, new SocketWarpper(String.valueOf(netSocket.hashCode()), netSocket));
                    } else if (eventResult.getType() == EventTypeEnum.LOGIN_FAIL.getType()) {
                        throw new RuntimeException("登陆失败！: " + eventResult);
                    } else {
                        log.warn("消息类型有误！，{}", eventResult);
                    }
                }
            } else {
                eqId = registry.entrySet().
                        stream().
                        filter(entry ->
                                entry.getValue().getNetSocketId().equals(String.valueOf(netSocket.hashCode()))).
                        map(Map.Entry::getKey).
                        findFirst().
                        orElseThrow(() -> {
                            log.info("设备未注册，拒绝手环：{} 登陆请求", netSocket.remoteAddress().host());
                            return new ConnectRefuseException();
                        });
            }
        }
        return eqId == null ? StringUtils.EMPTY : eqId;
    }


    @Override
    public void deviceLogout(NetSocket netSocket) {
        String netSocketID = String.valueOf(netSocket.hashCode());
        registry.entrySet().removeIf(entry -> entry.getValue().getNetSocketId().equals(netSocketID));
    }

    @Override
    public Optional<NetSocket> getDeviceNetSocket(String equipmentId) {
        return Optional.ofNullable(equipmentId).map(id -> registry.get(id)).
                map(SocketWarpper::getNetSocket);
    }


}
