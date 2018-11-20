package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.TransportEventEntry;
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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbsSocketManager implements DeviceSocketManager, BeanFactoryAware {
    private MqConnector mqConnector;
    private CommonConfig commonConfig;
    //equipmentId -> netSocket
    protected ConcurrentHashMap<String, SocketWarpper> netSocketRegistry = new ConcurrentHashMap<>();
    //netSocket.hashcode() -> equipmentId
    protected ConcurrentHashMap<Integer, String> netSocketIdMapping = new ConcurrentHashMap<>();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        mqConnector = beanFactory.getBean(MqConnector.class);
        commonConfig = beanFactory.getBean(CommonConfig.class);
    }

    @Override
    public void deviceLogout(NetSocket netSocket) {
        if (netSocket != null) {
            //解除本地设备注册
            int hashCode = netSocket.hashCode();
            String eqId = netSocketIdMapping.get(hashCode);
            //若未登录成功退出，则eqId为空，且不会向dispatcher端推送退出消息
            if (eqId != null) {
                netSocketRegistry.remove(eqId);
                netSocketIdMapping.remove(hashCode);
                //推送dispatcher端设备登出
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
                //mq宕机无需处理，缓存到了MqFailProcessor，会自动重发
                //dispatcher宕机可以通过mq的autoAck和produceAck避免
                //redis挂了不考虑，缓存全丢掉，重新初始化即可
                //网络抖动就重发，dispatcher端做幂等
                log.info("设备登出成功");
            }
        }
    }

    @Override
    public Optional<NetSocket> getDeviceNetSocket(String equipmentId) {
        return Optional.ofNullable(equipmentId).
                map(id -> netSocketRegistry.get(id)).
                map(SocketWarpper::getNetSocket);
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
                    setType(EventTypeEnum.DEVICE_LOGIN.getType())
                    .build().toByteArray();

            //异步转同步,交给dispatcher端做登陆/注册校验
            PublishEvent publishEvent = new PublishEvent(bytes, serialNumber);
            TransportEventEntry eventResult = mqConnector.publishSync(publishEvent);
            final String finalEqId = eqId;
            Optional.ofNullable(eventResult).map(TransportEventEntry::getType).ifPresent(e -> {
                if (eventResult.getType() == EventTypeEnum.LOGIN_SUCCESS.getType()) {
                    log.info("登陆成功，{}", eventResult);
                    SocketWarpper socketWarpper;
                    if ((socketWarpper = netSocketRegistry.get(finalEqId)) != null) {
                        //缓存不一致
                        log.error("设备【{}】在dispatcher端登陆成功，但上一次该设备退出时在connector端退出失败，" +
                                "在dispatcher端登陆成功，导致本地缓存不一致，删除旧缓存", finalEqId);
                        netSocketIdMapping.remove(socketWarpper.hashCode());
                        netSocketRegistry.remove(finalEqId);
                    }
                    netSocketRegistry.put(finalEqId, new SocketWarpper(netSocket));
                    netSocketIdMapping.put(socketId, finalEqId);
                } else {
                    String failMsg = "登陆失败！: cause:%s,time:%s,eqId:%s,";
                    throw new RuntimeException(String.format(failMsg, eventResult.getMsg(),
                            eventResult.getTimeStamp(),
                            eventResult.getEqId()));
                }
            });
        } else {
            if ((eqId = netSocketIdMapping.get(socketId)) != null) {
                netSocketRegistry.get(eqId).updateTimer();
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
