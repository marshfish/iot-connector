package com.hc.equipment.bootstrap;

import com.hc.equipment.device.CommonDevice;
import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.tcp.handler.PacketHandler;
import com.hc.equipment.tcp.handler.WriststrapPacketHandler;
import com.hc.equipment.tcp.mvc.DispatcherProxy;
import com.hc.equipment.util.BufferUtil;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class ServerVerticle extends AbstractVerticle {
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private CommonDevice commonDevice = SpringContextUtil.getBean(CommonDevice.class);
    private Config config = SpringContextUtil.getBean(Config.class);

    private NetServer netServer;
    private Map<String, PacketHandler> packetHandlerMap = new ConcurrentHashMap<>();
    private Lock handlerLock = new ReentrantLock();

    @Override
    public void start() {
        netServer = vertx.createNetServer();
        loadConnectionProcessor();
        loadBootstrapListener();
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeHandler));
    }

    private void loadBootstrapListener() {
        netServer.listen(config.getTcpPort(), config.getTcpHost(), netServerAsyncResult -> {
            if (netServerAsyncResult.succeeded()) {
                log.info("vert.x启动成功,端口：{}", config.getTcpPort());
            } else {
                log.info("vert.x失败,端口：{}", config.getTcpPort());
            }
        });
    }

    private void closeHandler() {
        Optional.ofNullable(netServer).ifPresent(server ->
                server.connectHandler(netSocket ->
                        netSocket.closeHandler(v -> {
                            log.info("关闭vert.x服务器");
                        })));
    }

    /**
     * 接收数据
     * 响应请求
     */
    private void loadConnectionProcessor() {
        netServer.connectHandler(netSocket ->
                netSocket.handler(buffer -> {
                    String data = buffer.getString(0, buffer.length());
                    log.info("{}接收数据:{} ", netSocket.remoteAddress().host(), data);
                    PacketHandler packetHandler = buildPacketHandler(netSocket);
                    packetHandler.packageHandler(data).forEach(command -> {
                        String deviceUniqueId;
                        try {
                            deviceUniqueId = commonDevice.deviceRegister(netSocket, command);
                        } catch (ConnectRefuseException e) {
                            netSocket.close();
                            return;
                        }
                        Optional.ofNullable(dispatcherProxy.routing(command, deviceUniqueId)).
                                ifPresent(result -> sendBuffer(netSocket, BufferUtil.allocString(result)));
                    });
                }).closeHandler(aVoid -> {
                    log.info("关闭连接：{}", netSocket.remoteAddress().host());
                    commonDevice.deviceUnRegister(netSocket);
                }));
        netServer.exceptionHandler(throwable -> {
            log.error(throwable.getMessage());
            throwable.printStackTrace();
        });
    }

    private PacketHandler buildPacketHandler(NetSocket netSocket) {
        String socketId = String.valueOf(netSocket.hashCode());
        PacketHandler packetHandler;
        handlerLock.lock();
        try {
            if ((packetHandler = packetHandlerMap.get(socketId)) == null) {
                packetHandler = new WriststrapPacketHandler();
                packetHandlerMap.put(socketId, packetHandler);
            }
        } finally {
            handlerLock.unlock();
        }
        return packetHandler;
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
