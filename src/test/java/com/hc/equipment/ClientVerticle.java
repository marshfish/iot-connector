package com.hc.equipment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
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

        netClient.connect(8765, "127.0.0.1",
                result -> {
                    if (result.succeeded()) {
                        System.out.println("启动成功");
                    } else {
                        System.out.println("启动失败");
                        return;
                    }
                    NetSocket socket = result.result();
                    socket.write("IWAP003534");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    socket.write("56789012345#");
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    socket.write("IWAP03,0600");
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    socket.write("0908000102,5555,30#");
                    socket.write("IWAPXL,080835#");
                    socket.handler(buffer -> {
                                String data = buffer.getString(0, buffer.length());
                                log.info("接收数据:{} ", data);
                            });
                    socket.closeHandler(aVoid -> log.info("服务器拒绝连接"));
                });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ClientVerticle());
    }

}
