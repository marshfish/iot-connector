package com.mcode.connector.dispatch;

import com.mcode.connector.configuration.CommonConfig;
import com.mcode.connector.device.DeviceSocketManager;
import com.mcode.connector.dispatch.event.MapDatabase;
import com.mcode.connector.mvc.DispatcherProxy;
import com.mcode.connector.tcp.PacketHandlerFactory;
import com.mcode.connector.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * tcp下行请求处理
 */
@Slf4j
public class TCPDownStream extends AbstractVerticle {
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private DeviceSocketManager deviceSocketManager = SpringContextUtil.getBean(DeviceSocketManager.class);
    private CommonConfig commonConfig = SpringContextUtil.getBean(CommonConfig.class);
    private AtomicInteger instance = new AtomicInteger(1);
    private NetServer netServer;
    private Function<NetSocket, Consumer<String>> action = netSocket -> command -> {
        String eqId;
        if ((eqId = deviceSocketManager.deviceLogin(netSocket, command)) != null) {
            String result = dispatcherProxy.routingTCP(command, eqId);
            if (result != null) {
                netSocket.write(Buffer.buffer(result, "UTF-8"));
            }
        } else {
            closeHook(netSocket, true);
        }
    };

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
        SpringContextUtil.getBean(MapDatabase.class).close();
    }

    /**
     * 接收数据
     * 响应请求
     */
    private void loadConnectionProcessor() {
        Handler<NetSocket> socketHandler = netSocket -> {
            Pump.pump(netSocket, netSocket).start();
            Handler<Buffer> handler = buffer -> {
                log.info("{}接收数据:{} ", netSocket.remoteAddress().host(), buffer.getString(0, buffer.length()));
                try {
                    PacketHandlerFactory.
                            build(String.valueOf(netSocket.hashCode())).
                            register(action.apply(netSocket)).
                            handler(buffer);
                } catch (Exception e) {
                    log.warn("TCP数据处理异常{}", Arrays.toString(e.getStackTrace()));
                    closeHook(netSocket, true);
                }
            };
            netSocket.handler(handler);
            netSocket.closeHandler(event -> closeHook(netSocket, false));
            netSocket.exceptionHandler(event -> {
                log.warn("TCP连接异常：{}", Arrays.toString(event.getStackTrace()));
                closeHook(netSocket, true);
            });
        };
        netServer.connectHandler(socketHandler);
        netServer.exceptionHandler(throwable -> log.error("TCP服务器异常:{}", Arrays.toString(throwable.getStackTrace())));
    }

    private void closeHook(NetSocket netSocket, boolean disConnect) {
        deviceSocketManager.deviceLogout(netSocket.hashCode(), true);
        PacketHandlerFactory.removePackageHandler(netSocket);
        if (disConnect) {
            log.info("断开TCP连接：{}", netSocket.remoteAddress().host());
            netSocket.close();
        }
    }

}
