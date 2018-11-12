package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbsSocketManager implements DeviceSocketManager {
    //equipmentId -> netSocket
    protected ConcurrentHashMap<String, NetSocket> netSocketRegistry = new ConcurrentHashMap<>();
    //netSocket.hashcode() -> equipmentId
    protected ConcurrentHashMap<Integer, String> netSocketIdMapping = new ConcurrentHashMap<>();

    @Override
    public void deviceLogout(NetSocket netSocket) {
        if (netSocket != null) {
            //解除本地设备注册
            int hashCode = netSocket.hashCode();
            String eqId = netSocketIdMapping.get(hashCode);
            netSocketRegistry.remove(eqId);
            netSocketIdMapping.remove(hashCode);
            //推送dispatcher端设备登出
            MqConnector mqConnector = SpringContextUtil.getBean(MqConnector.class);
            CommonConfig config = SpringContextUtil.getBean(CommonConfig.class);
            Trans.event_data.Builder builder = Trans.event_data.newBuilder();
            byte[] bytes = builder.setType(EventTypeEnum.DEVICE_LOGOUT.getType()).
                    setEqId(eqId).
                    setEqType(config.getEquipmentType()).
                    setTimeStamp(System.currentTimeMillis()).
                    setSerialNumber(String.valueOf(IdGenerator.buildDistributedId())).
                    build().toByteArray();
            mqConnector.publish(bytes);
        }

    }

    @Override
    public Optional<NetSocket> getDeviceNetSocket(String equipmentId) {
        return Optional.ofNullable(equipmentId).map(id -> netSocketRegistry.get(id));
    }

    @Override
    public void writeString(NetSocket netSocket, String data) {
        netSocket.write(Buffer.buffer(data, "UTF-8"));
    }
}
