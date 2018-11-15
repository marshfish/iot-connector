package com.hc.equipment.dispatch.event;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据上传
 */
@Slf4j
@Component
public class DataUploadHandler {
    @Resource
    private MqConnector mqConnector;
    @Resource
    private CommonConfig commonConfig;
    //流水号注册表 seriaId -> dispatcherId
    private static Map<String, String> seriaIdRegistry = new ConcurrentHashMap<>();

    /**
     * 上传数据
     * 设备-> connector
     * 适用于上传需要回调给业务系统的数据
     *
     * @param eqId    设备ID
     * @param uri     上传uri
     * @param message 消息(json)
     */
    public void uploadData(String eqId, String uri, String message) {
        String id = String.valueOf(IdGenerator.buildDistributedId());
        Trans.event_data.Builder builder = Trans.event_data.newBuilder();
        byte[] bytes = builder.setType(EventTypeEnum.DEVICE_UPLOAD.getType()).
                setUri(uri).
                setEqId(eqId).
                setEqType(commonConfig.getEquipmentType()).
                setMsg(message).
                setSerialNumber(id).
                setTimeStamp(System.currentTimeMillis()).build().toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, id);
        mqConnector.publishAsync(publishEvent);
    }

    /**
     * 上传响应
     * connector->设备，设备->connector
     * 适用于业务系统发送的指令，回调设备的响应结果
     *
     * @param serialNumber 指令流水号（需根据协议规定手动解析后传入）
     * @param eqId         设备ID
     * @param message      消息
     */
    public void uploadCallback(String serialNumber, String eqId, String message) {
        String dispatcherId = seriaIdRegistry.get(serialNumber);
        if (!StringUtils.isEmpty(dispatcherId)) {
            seriaIdRegistry.remove(serialNumber);
        } else {
            log.warn("根据流水号获取的dispatcher端ID不存在，拒绝上传");
            return;
        }
        Trans.event_data.Builder builder = Trans.event_data.newBuilder();
        byte[] bytes = builder.setType(EventTypeEnum.CLIENT_RESPONSE.getType()).
                setSerialNumber(serialNumber).
                setTimeStamp(System.currentTimeMillis()).
                setDispatcherId(dispatcherId).
                setMsg(message).
                setEqId(eqId).
                build().toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, serialNumber);
        mqConnector.publishAsync(publishEvent);
    }

    /**
     * 关联指令流水号与dispatcherId
     *
     * @param seriaId      流水号
     * @param dispatcherId dispatcherId
     */
    public void attachSeriaId2DispatcherId(String seriaId, String dispatcherId) {
        seriaIdRegistry.put(seriaId, dispatcherId);
    }
}
