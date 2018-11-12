package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.type.EquipmentTypeEnum;
import com.hc.equipment.type.EventTypeEnum;
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
                    Trans.event_data.Builder event = Trans.event_data.newBuilder();
                    byte[] bytes = event.setEqId(eqId).
                            setEqType(EquipmentTypeEnum.WRISTSTRAP.getType()).
                            setNodeArtifactId(commonConfig.getNodeArtifactId()).
                            setSerialNumber(serialNumber).
                            setType(EventTypeEnum.DEVICE_LOGIN.getType())
                            .build().toByteArray();

                    //异步转同步,交给dispatcher端做登陆/注册校验
                    TransportEventEntry eventResult = mqConnector.publishSync(
                            serialNumber,
                            bytes);
                    if (eventResult.getType() == EventTypeEnum.LOGIN_SUCCESS.getType()) {
                        log.info("登陆成功，{}", eventResult);
                        netSocketRegistry.put(eqId, netSocket);
                        netSocketIdMapping.put(netSocket.hashCode(), eqId);
                    } else {
                        throw new RuntimeException("登陆失败！: " + eventResult);
                    }
                }
            } else {
                eqId = Optional.ofNullable(netSocketIdMapping.get(netSocket.hashCode())).
                        orElseThrow(() -> new RuntimeException("设备未经登陆！无权上传数据"));

            }
        }
        return eqId == null ? StringUtils.EMPTY : eqId;
    }

}
