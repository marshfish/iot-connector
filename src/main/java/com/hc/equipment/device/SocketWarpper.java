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
    private long lastUpdate;

    public SocketWarpper(NetSocket netSocket) {
        this.netSocket = netSocket;
        timeout = timer.newTimeout(timeout1 -> netSocket.close(),
                tcpTimeout, TimeUnit.MILLISECONDS);
        lastUpdate = System.currentTimeMillis();
    }

    public void updateTimer() {
        //防止短时间大量心跳导致重复取消新建timer
        //TODO
        long now = System.currentTimeMillis();
        if (now - lastUpdate < tcpTimeout * 0.5) {
            return;
        }
        boolean cancel = timeout.cancel();
        if (cancel) {
            timeout = timer.newTimeout(timeout1 -> netSocket.close(),
                    tcpTimeout, TimeUnit.MILLISECONDS);
            lastUpdate = now;
        }
    }

}
