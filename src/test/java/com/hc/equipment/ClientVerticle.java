package com.hc.equipment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientVerticle extends AbstractVerticle {

    @Override
    public void start() {
        log.info("客户端启动");
        NetClient netClient = vertx.createNetClient();

        netClient.connect(8765, "localhost",
                result -> {
                    NetSocket socket = result.result();
                    socket.write("IWAP03,06000908000102,5555,30#").
                            handler(buffer -> {
                                String data = buffer.getString(0, buffer.length());
                                log.info("接收数据:{} ", data);
                            });
                });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ClientVerticle());
    }

}
