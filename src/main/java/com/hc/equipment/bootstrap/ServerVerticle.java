package com.hc.equipment.bootstrap;

import com.hc.equipment.device.CommonDevice;
import com.hc.equipment.tcp.mvc.DispatcherProxy;
import com.hc.equipment.util.BufferUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Component
public class ServerVerticle extends AbstractVerticle {
    @Resource
    private DispatcherProxy dispatcherProxy;
    @Resource
    private CommonDevice commonDevice;

    @Override
    public void start() {
        NetServer netServer = vertx.createNetServer();
        readHandler(netServer);
        netServer.listen(8765, "localhost", netServerAsyncResult -> {
            if (netServerAsyncResult.succeeded()) {
                log.info("vert.x启动成功");
            } else {
                log.info("vert.x失败");
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeHandler(netServer)));
    }

    private void closeHandler(NetServer netServer) {
        netServer.connectHandler(netSocket -> netSocket.closeHandler(v -> {
            log.info("关闭socket连接");
        }));
    }

    /**
     * 接收数据
     * 响应请求
     */
    private void readHandler(NetServer netServer) {
        netServer.connectHandler(netSocket ->
                netSocket.handler(buffer -> {
                    String data = buffer.getString(0, buffer.length());
                    log.info("{}接收数据:{} ", netSocket.remoteAddress().host(), data);
                    commonDevice.deviceRegister(netSocket, data);
                    Optional.ofNullable(dispatcherProxy.routing(data)).
                            ifPresent(result -> sendBuffer(netSocket, BufferUtil.allocString(result)));
                }));
    }

    /**
     * 发送数据
     */
    private void writeHandler(String data, NetSocket netSocket) {
        sendBuffer(netSocket, BufferUtil.allocString(data));
    }

    private void sendBuffer(NetSocket netSocket, Buffer outBuffer) {
        netSocket.write(outBuffer);
    }

}
