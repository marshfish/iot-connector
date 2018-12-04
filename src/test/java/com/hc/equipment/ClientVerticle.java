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
        netClient.connect(8765, "localhost",
                result -> {
                    try {
                        if (result.succeeded()) {
                            log.info("启动成功: " + count.getAndIncrement());
                        } else {
                            log.info("启动失败");
                            return;
                        }
                        NetSocket socket = result.result();
                        socket.write("IWAP00351905183124515#");
//                        for (int i1 = 0; i1 < 50; i1++) {
//                            Thread.sleep(new Random().nextInt(12000));
//                            socket.write("IWAPHT,60,130,85#");
//                        }
                        socket.handler(buffer -> {
                            String data = buffer.getString(0, buffer.length());
                            log.info("接收数据:{} ", data);
                            if (data.equals("IWBPXY,353456789012345,080835#")) {
                                socket.write("IWAPXL,080835#");
                            }
                        });
                        socket.closeHandler(aVoid -> log.info("关闭连接"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }


    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
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
