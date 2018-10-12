package com.hc.equipment.tcp.rpc;

import com.google.gson.Gson;
import com.hc.equipment.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@Slf4j
@Component
public class AsyncHttpClientPool implements InitializingBean {
    @Resource
    private Config config;
    private CloseableHttpAsyncClient client;

    public void constructorSyncHttpClient() {
        //配置Reactor io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setSoKeepAlive(true)
                .build();
        //设置连接池大小
        ConnectingIOReactor ioReactor = null;
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            e.printStackTrace();
        }
        //连接池配置
        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(100);
        //请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(1000)
                .build();
        //构造请求client
        client = HttpAsyncClients.custom().
                setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
        client.start();
    }

    public void sendPost(String uri, Map<String,String> body, ResponseFuture future) {
        HttpPost httpPost = new HttpPost(config + uri);
        StringEntity entity = null;
        try {
            entity = new StringEntity(new Gson().toJson(body));
            entity.setContentEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("httpclient编码错误");
        }
        httpPost.setEntity(entity);
        client.execute(httpPost, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse httpResponse) {
                future.onSuccess();
                log.info("调用回调接口成功");
            }

            @Override
            public void failed(Exception e) {
                future.onFail();
                log.warn("访问接口失败，uri：{},Exception:{}", httpPost.getURI(), e);
            }

            @Override
            public void cancelled() {
                future.onCancel();
                log.warn("调用取消");
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        constructorSyncHttpClient();
    }
}
