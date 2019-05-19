package com.mcode.connector.dispatch.event.handler;

import com.mcode.connector.device.DeviceSocketManager;
import com.mcode.connector.dispatch.event.AsyncEventHandler;
import com.mcode.connector.rpc.serialization.Trans;
import com.mcode.connector.type.EventTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class LoginFail extends AsyncEventHandler {
    @Resource
    private DeviceSocketManager deviceSocketManager;

    @Override
    public void accept(Trans.event_data event) {
        String msg = event.getMsg();
        validEmpty("消息", msg);
        String[] split = msg.split(":");
        deviceSocketManager.deviceLogout(Integer.parseInt(split[0]), false);
        String failMsg = "登陆失败！: cause:%s,time:%s,eqId:%s,";
        throw new RuntimeException(String.format(failMsg, msg,
                event.getTimeStamp(),
                event.getEqId()));
    }

    @Override
    public EventTypeEnum setReceivedEventType() {
        return EventTypeEnum.LOGIN_FAIL;
    }

}
