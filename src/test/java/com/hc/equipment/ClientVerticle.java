package com.hc.equipment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ClientVerticle extends AbstractVerticle {
    private static AtomicInteger count = new AtomicInteger(1);

    @Override
    public void start() {
        log.info("客户端启动");
        NetClient netClient = vertx.createNetClient();
        netClient.connect(8765, /*"39.105.146.15"*/"localhost",
                result -> {
                    try {
                        if (result.succeeded()) {
                            System.out.println("启动成功: " + count.getAndIncrement());
                        } else {
                            System.out.println("启动失败");
                            return;
                        }
                        NetSocket socket = result.result();
                        socket.write("IWAP003" + (Math.random() * 9 + 1) * 100000 + "2801330814#");
//                        for (int i1 = 0; i1 < 50; i1++) {
//                            Thread.sleep(new Random().nextInt(12000));
//                            socket.write("IWAPHT,60,130,85#");
//                        }

                        socket.handler(buffer -> {
                            String data = buffer.getString(0, buffer.length());
                            log.info("接收数据:{} ", data);
                        });
                        socket.closeHandler(aVoid -> log.info("服务器拒绝连接"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void main(String[] args) {
        for (int i = 0; i < 500; i++) {
            Vertx vertx = Vertx.vertx();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            vertx.deployVerticle(new ClientVerticle());
        }

    }

}
