package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.util.SpringContextUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class SocketWarpper {
    private static HashedWheelTimer timer = new HashedWheelTimer();
    private static long tcpTimeout = SpringContextUtil.getBean(CommonConfig.class).getTcpTimeout();
    private NetSocket netSocket;
    private Timeout timeout;

    public SocketWarpper(NetSocket netSocket) {
        this.netSocket = netSocket;
        if (timeout == null) {
            timeout = timer.newTimeout(timeout1 -> netSocket.close(),
                    tcpTimeout, TimeUnit.MILLISECONDS);
        }
    }

    public void updateTimer() {
        boolean cancel = timeout.cancel();
        if (cancel) {
            timeout = timer.newTimeout(timeout1 -> netSocket.close(),
                    tcpTimeout, TimeUnit.MILLISECONDS);
        }
    }

}
