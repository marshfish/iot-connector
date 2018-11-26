package com.hc.equipment.dispatch.event;

import com.google.gson.Gson;
import com.hc.equipment.Bootstrap;
import com.hc.equipment.LoadOrder;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.serialization.Trans;
import com.hc.equipment.type.EventTypeEnum;
import com.hc.equipment.util.IdGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据上传
 */
@Slf4j
@Component
@LoadOrder
public class DataUploadHandler implements Bootstrap {
    @Resource
    private MqConnector mqConnector;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private MapDatabase mapDatabase;
    //流水号注册表 seriaId -> dispatcherId
    @Getter
    private Map<String, String> seriaIdRegistry = new ConcurrentHashMap<>();
    //数据上传的备份
    @Getter
    private Map<String, PublishEvent> backupData = Collections.synchronizedMap(new HashMap<>(200));
    //失败消息队列
    @Getter
    private Queue<PublishEvent> failQueue = new LinkedBlockingQueue<>(200);
    public static final String RE_POST_MESSAGE = "backup_msg";
    private static ScheduledExecutorService reSendThread = Executors.newScheduledThreadPool(1, r -> {
        Thread thread = new Thread(r);
        thread.setName("rePost-exec-1");
        thread.setDaemon(true);
        return thread;
    });

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
                setNodeArtifactId(commonConfig.getNodeArtifactId()).
                setUri(uri).
                setEqId(eqId).
                setEqType(commonConfig.getEquipmentType()).
                setMsg(message).
                setSerialNumber(id).
                setTimeStamp(System.currentTimeMillis()).build().toByteArray();
        PublishEvent publishEvent = new PublishEvent(bytes, id);
        qos1Publish(publishEvent);
    }

    /**
     * qos1下的数据上传，若上传数据得不到dispatcher端的ack，则尝试重发，
     * 若依然失败，则持久化到MapDB准备重发
     */
    private void qos1Publish(PublishEvent publishEvent) {
        //超时扫描失败消息
        publishEvent.addTimer(o -> {
            log.warn("缓存失败消息");
            String serialNumber = o.getSerialNumber();
            if (backupData.containsKey(serialNumber)) {
                backupData.remove(serialNumber);
                failQueue.offer(o.addRePostCount());
            }
        });
        //添加到确认回调列表中
        backupData.put(publishEvent.getSerialNumber(), publishEvent);
        mqConnector.publishAsync(publishEvent);
    }

    /**
     * 确认消息
     *
     * @param serialNumber 消息流水号
     */
    public void ackDataUpload(String serialNumber) {
        PublishEvent publishEvent;
        if ((publishEvent = backupData.get(serialNumber)) != null) {
            if (publishEvent.isEndurance()) {
                mapDatabase.remove(serialNumber, RE_POST_MESSAGE);
            }
            backupData.remove(serialNumber);
        }

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

    @SuppressWarnings("UnusedAssignment")
    @Override
    public void init() {
        reSendThread.scheduleAtFixedRate(new Runnable() {
            private int count;

            @Override
            public void run() {
                try {
                    count++;
                    PublishEvent publishEvent;
                    while ((publishEvent = failQueue.poll()) != null) {
                        publishEvent.addRePostCount();
                        //当该消息投递次数小于2次时，暂不持久化到DB，继续尝试重发
                        String serialNumber = publishEvent.getSerialNumber();
                        if (publishEvent.getRePostCount() < 2) {
                            log.info("尝试立即重发失败消息");
                            qos1Publish(publishEvent);
                        } else {
                            log.info("尝试次数大于两次，持久化失败消息到DB，等待重新发送");
                            //写入到嵌入式数据库中
                            publishEvent.setEnduranceFlag(true);
                            mapDatabase.write(serialNumber,
                                    publishEvent,
                                    RE_POST_MESSAGE).
                                    close();
                        }
                    }
                    //每2小时检查一下数据库，尝试重发
                    if (count == 2) {
                        log.info("重新发送消息");
                        mapDatabase.read(PublishEvent.class, RE_POST_MESSAGE).
                                forEach(event -> qos1Publish(event));
                        mapDatabase.close();
                        count = 0;
                    }
                } catch (Exception e) {
                    log.error("消息重发线程异常：{}", Arrays.toString(e.getStackTrace()));
                }
            }
        }, 30 * 1000, 1 * 60 * 1000, TimeUnit.MILLISECONDS);
    }
}
