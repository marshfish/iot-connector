package com.hc.equipment.bootstrap;

import com.google.gson.Gson;
import com.hc.equipment.http.vo.BaseResult;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HTTPVerticle extends AbstractVerticle {
    private HttpServer httpServer;
    private Config config = SpringContextUtil.getBean(Config.class);
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private static AtomicInteger instance = new AtomicInteger(1);
    public static EventBus eventBus;

    @Override
    public void start() throws Exception {
        httpServer = vertx.createHttpServer(new HttpServerOptions().
                setIdleTimeout(config.getMaxHTTPIdleTime()));
        eventBus = vertx.eventBus();
        loadConnectionProcessor();
        loadBootstrapListener();
    }

    private void loadBootstrapListener() {
        httpServer.listen(config.getHttpPort(), config.getHost(), httpServerAsyncResult -> {
            if (httpServerAsyncResult.succeeded()) {
                log.info("vert.x HTTP实例{}启动成功,端口：{}", instance.getAndIncrement(), config.getHttpPort());
            } else {
                log.info("vert.x HTTP实例{}启动成功,端口：{}", instance.getAndIncrement(), config.getHttpPort());
            }
        });
    }

    private void loadConnectionProcessor() {
        httpServer.requestHandler(httpServerRequest ->
                httpServerRequest.bodyHandler(buffer -> {
                    try {
                        String result = dispatcherProxy.routingHTTP(httpServerRequest, buffer.getString(0, buffer.length()));
                        eventBus.consumer(String.valueOf(httpServerRequest.hashCode()), (Handler<Message<Boolean>>) event -> {
                            if (event.body()) {
                                writeSuccessResponse(httpServerRequest, result);
                            } else {
                                log.warn("指令发送失败,{}", httpServerRequest.uri());
                                writeFailResponse(httpServerRequest, "指令发送失败");
                            }
                        });
                    } catch (Exception e) {
                        log.error("HTTP数据处理异常:", e);
                        writeFailResponse(httpServerRequest, e.getMessage());
                    }
                }).exceptionHandler(throwable -> {
                    log.error("HTTP request异常，{}", throwable);
                    writeFailFast(httpServerRequest);
                })).exceptionHandler(throwable -> log.error("HTTP服务器异常:{}", throwable));
    }

    private void writeSuccessResponse(HttpServerRequest httpServerRequest, String result) {
        httpServerRequest.response().
                putHeader("content-type", "application/json").
                putHeader("Content-Length", String.valueOf(result.getBytes().length)).
                setStatusCode(200).
                write(result, "UTF-8").end();
    }

    private void writeFailFast(HttpServerRequest httpServerRequest) {
        writeFailResponse(httpServerRequest,"服务器异常");
    }

    private void writeFailResponse(HttpServerRequest httpServerRequest, String message) {
        String failMessage = new Gson().toJson(new BaseResult(500, message));
        httpServerRequest.response().
                setStatusCode(500).
                putHeader("content-type", "application/json").
                putHeader("Content-Length", String.valueOf(failMessage.getBytes().length))
                .write(failMessage, "UTF-8").end();
    }
}
