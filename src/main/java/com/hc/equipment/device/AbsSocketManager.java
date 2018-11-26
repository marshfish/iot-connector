package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.type.QosType;
import com.hc.equipment.util.IdGenerator;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import java.util.Optional;

@Slf4j
public abstract class AbsSocketManager implements DeviceSocketManager, BeanFactoryAware {
    private MqConnector mqConnector;
    private CommonConfig commonConfig;
    private SocketContainer socketContainer;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        mqConnector = beanFactory.getBean(MqConnector.class);
        commonConfig = beanFactory.getBean(CommonConfig.class);
        socketContainer = beanFactory.getBean(SocketContainer.class);
    }


    @Override
    public void deviceLogout(int socketId, boolean pushTODispatcher) {
        //解除本地设备注册
        String eqId = socketContainer.getEquipmentId(socketId);
        //若未登录成功退出，则eqId为空，且不会向dispatcher端推送退出消息
        if (eqId != null) {
            socketContainer.unRegisterSocket(socketId,eqId);
            //推送dispatcher端设备登出
            if (pushTODispatcher) {
                Trans.event_data.Builder builder = Trans.event_data.newBuilder();
                String id = String.valueOf(IdGenerator.buildDistributedId());
                byte[] bytes = builder.setType(EventTypeEnum.DEVICE_LOGOUT.getType()).
                        setNodeArtifactId(commonConfig.getNodeArtifactId()).
                        setEqId(eqId).
                        setEqType(commonConfig.getEquipmentType()).
                        setTimeStamp(System.currentTimeMillis()).
                        setSerialNumber(id).
                        build().toByteArray();
                PublishEvent publishEvent = new PublishEvent(bytes, id);
                publishEvent.setTimeout(3000);
                publishEvent.setQos(QosType.AT_LEAST_ONCE.getType());
                mqConnector.publishAsync(publishEvent);
            }
            log.info("设备登出成功");
        }
    }

    @Override
    public Optional<NetSocket> getDeviceNetSocket(String equipmentId) {
        return Optional.ofNullable(socketContainer.getSocket(equipmentId));
    }

    @Override
    public String deviceLogin(NetSocket netSocket, String data) {
        String protocolNumber;
        String eqId;
        if ((protocolNumber = setProtocolNumber(data)) == null) {
            throw new RuntimeException("不合法的请求，解析协议号失败:" + data);
        }
        int socketId = netSocket.hashCode();
        if (setLoginProtocolNumber().equals(protocolNumber)) {
            if ((eqId = setEquipmentId(data)) == null) {
                throw new RuntimeException("不合法的请求，解析设备ID失败:" + data);
            }
            String serialNumber = String.valueOf(IdGenerator.buildDistributedId());
            Trans.event_data.Builder event = Trans.event_data.newBuilder();
            byte[] bytes = event.setEqId(eqId).
                    setEqType(commonConfig.getEquipmentType()).
                    setNodeArtifactId(commonConfig.getNodeArtifactId()).
                    setSerialNumber(serialNumber).
                    setType(EventTypeEnum.DEVICE_LOGIN.getType()).
                    setMsg(String.valueOf(socketId)).
                    build().toByteArray();
            PublishEvent publishEvent = new PublishEvent(bytes, serialNumber);
            mqConnector.publishAsync(publishEvent);
            socketContainer.registerSocket(eqId,netSocket);

        } else {
            if ((eqId = socketContainer.getEquipmentId(socketId)) != null) {
               socketContainer.heartBeat(eqId);
            } else {
                throw new RuntimeException("设备未经登陆！无权上传数据");
            }
        }
        return eqId;
    }

    /**
     * 设置登陆包的协议号
     *
     * @return 登陆包的协议号
     */
    protected abstract String setLoginProtocolNumber();

    /**
     * 设置解析设备ID的方法
     *
     * @param data 原始登陆包
     * @return 设备ID
     */
    protected abstract String setEquipmentId(String data);
}
