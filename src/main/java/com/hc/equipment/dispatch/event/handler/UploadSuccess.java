package com.hc.equipment.dispatch.event.handler;

import com.hc.equipment.dispatch.event.AsyncEventHandler;
import com.hc.equipment.dispatch.event.DataUploadHandler;
import com.hc.equipment.rpc.TransportEventEntry;
import com.hc.equipment.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class UploadSuccess extends AsyncEventHandler {
    @Resource
    private DataUploadHandler dataUploadHandler;

    @Override
    public void accept(TransportEventEntry event) {
        String serialNumber = event.getSerialNumber();
        validEmpty("设备上传消息流水号", serialNumber);
        log.info("数据上传成功，{}",serialNumber);
        dataUploadHandler.ackDataUpload(serialNumber);
    }

    @Override
    public Integer setReceivedEventType() {
        return EventTypeEnum.UPLOAD_SUCCESS.getType();
    }
}
