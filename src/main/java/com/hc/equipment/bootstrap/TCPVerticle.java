package com.hc.equipment.bootstrap;

import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.exception.ConnectRefuseException;
import com.hc.equipment.tcp.handler.PacketHandlerFactory;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.RemotingUtil;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.streams.Pump;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TCPVerticle extends AbstractVerticle {
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private DeviceSocketManager deviceSocketManager = SpringContextUtil.getBean(DeviceSocketManager.class);
    private Config config = SpringContextUtil.getBean(Config.class);
    private static AtomicInteger instance = new AtomicInteger(1);
    private NetServer netServer;


    @Override
    public void start() {
        if (RemotingUtil.isLinuxPlatform()) {
            //TODO tcp参数设置
            netServer = vertx.createNetServer(new NetServerOptions());
        } else {
            netServer = vertx.createNetServer();
        }
        loadConnectionProcessor();
        loadBootstrapListener();
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeHandler));
    }

    private void loadBootstrapListener() {
        netServer.listen(config.getTcpPort(), config.getHost(), netServerAsyncResult -> {
            if (netServerAsyncResult.succeeded()) {
                log.info("vert.x TCP实例{}启动成功,端口：{}", instance.getAndIncrement(), config.getTcpPort());
            } else {
                log.info("vert.x TCP实例{}启动后失败,端口：{}", instance.getAndIncrement(), config.getTcpPort());
            }
        });
    }

    private void closeHandler() {
        //TODO 资源清理
    }

    /**
     * 接收数据
     * 响应请求
     */
    private void loadConnectionProcessor() {
        netServer.connectHandler(netSocket -> {
            //由vertx进行流量控制，及时阻塞该netSocket，防止client写入流量过大或由于其他阻塞操作引起的netSocket无界等待队列过大导致OOM
            Pump.pump(netSocket, netSocket).start();
            netSocket.handler(buffer -> {
                log.info("{}接收数据:{} ", netSocket.remoteAddress().host(), buffer.getString(0, buffer.length()));
                PacketHandlerFactory.buildPacketHandler(netSocket).packageHandler(buffer).forEach(command -> {
                    String deviceUniqueId;
                    deviceUniqueId = deviceSocketManager.deviceRegister(netSocket, command);
                    Optional.ofNullable(dispatcherProxy.routingTCP(command, deviceUniqueId)).
                            ifPresent(result -> deviceSocketManager.writeString(netSocket, result));
                });
            }).closeHandler(aVoid -> {
                log.info("断开TCP连接：{}", netSocket.remoteAddress().host());
                deviceSocketManager.deviceUnRegister(netSocket);
                PacketHandlerFactory.removePackageHandler(netSocket);
            }).exceptionHandler(throwable -> {
                log.info("TCP连接异常，关闭连接：{}，异常：{}", netSocket.remoteAddress().host(), throwable);
                netSocket.close();
                deviceSocketManager.deviceUnRegister(netSocket);
                PacketHandlerFactory.removePackageHandler(netSocket);
            });
        }).exceptionHandler(throwable -> log.error("服务器异常:{}", throwable.getMessage()));
    }

}
