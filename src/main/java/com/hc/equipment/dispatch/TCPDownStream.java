package com.hc.equipment.dispatch;

import com.hc.equipment.device.DeviceSocketManager;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.tcp.handler.PacketHandlerFactory;
import com.hc.equipment.configuration.CommonConfig;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * tcp下行请求处理
 */
@Slf4j
public class TCPDownStream extends AbstractVerticle {
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private DeviceSocketManager deviceSocketManager = SpringContextUtil.getBean(DeviceSocketManager.class);
    private CommonConfig commonConfig = SpringContextUtil.getBean(CommonConfig.class);
    private static AtomicInteger instance = new AtomicInteger(1);
    private NetServer netServer;


    @Override
    public void start() {
        netServer = vertx.createNetServer(new NetServerOptions());
        loadConnectionProcessor();
        loadBootstrapListener();
    }

    private void loadBootstrapListener() {
        netServer.listen(commonConfig.getTcpPort(), commonConfig.getHost(), netServerAsyncResult -> {
            if (netServerAsyncResult.succeeded()) {
                log.info("vert.x TCP实例{}启动成功,端口：{}", instance.getAndIncrement(), commonConfig.getTcpPort());
            } else {
                log.info("vert.x TCP实例{}启动后失败,端口：{}", instance.getAndIncrement(), commonConfig.getTcpPort());
            }
        });
    }


    @Override
    public void stop() throws Exception {
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
                try {
                    PacketHandlerFactory.buildPacketHandler(netSocket).packageHandler(buffer).forEach(command -> {
                        String equipmentId;
                        equipmentId = deviceSocketManager.deviceLogin(netSocket, command);
                        Optional.ofNullable(dispatcherProxy.routingTCP(command, equipmentId)).
                                ifPresent(result -> deviceSocketManager.writeString(netSocket, result));
                    });
                } catch (Exception e) {
                    log.error("TCP数据处理异常{}", e);
                    clearOnException(netSocket, false);
                }
            }).exceptionHandler(throwable -> {
                log.info("TCP连接异常，关闭连接：{}，异常：{}", netSocket.remoteAddress().host(), throwable);
                clearOnException(netSocket, true);
            });
        }).exceptionHandler(throwable -> log.error("TCP服务器异常:{}", throwable));
    }

    private void clearOnException(NetSocket netSocket, boolean disConnect) {
        deviceSocketManager.deviceLogout(netSocket);
        PacketHandlerFactory.removePackageHandler(netSocket);
        if (disConnect) {
            log.info("断开TCP连接：{}", netSocket.remoteAddress().host());
            netSocket.close();
        }
    }
}
