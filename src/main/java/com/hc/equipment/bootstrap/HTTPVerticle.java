package com.hc.equipment.bootstrap;

import com.google.gson.Gson;
import com.hc.equipment.http.vo.BaseResult;
import com.hc.equipment.mvc.DispatcherProxy;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.SpringContextUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HTTPVerticle extends AbstractVerticle {
    private HttpServer httpServer;
    private Config config = SpringContextUtil.getBean(Config.class);
    private DispatcherProxy dispatcherProxy = SpringContextUtil.getBean(DispatcherProxy.class);
    private static AtomicInteger instance = new AtomicInteger(1);

    @Override
    public void start() throws Exception {
        httpServer = vertx.createHttpServer();
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
                    String result = dispatcherProxy.routingHTTP(
                            httpServerRequest.uri(),
                            httpServerRequest.method().name(),
                            buffer.getString(0, buffer.length()));
                    httpServerRequest.response().
                            putHeader("content-type", "application/json").
                            putHeader("Content-Length", String.valueOf(result.getBytes().length)).
                            setStatusCode(200).
                            write(result, "UTF-8").end();
                }).exceptionHandler(throwable -> {
                    log.error("HTTP服务器异常，{}", throwable);
                    String failFast = new Gson().toJson(BaseResult.getFailFast());
                    httpServerRequest.response().
                            setStatusCode(500).
                            putHeader("content-type", "application/json").
                            putHeader("Content-Length", String.valueOf(failFast.getBytes().length))
                            .write(failFast, "UTF-8").end();
                })).exceptionHandler(throwable -> log.error("服务器异常:{}", throwable.getMessage()));
    }
}
