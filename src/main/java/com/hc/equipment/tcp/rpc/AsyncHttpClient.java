package com.hc.equipment.tcp.rpc;

import com.google.gson.Gson;
import com.hc.equipment.util.Config;
import com.hc.equipment.util.SpringContextUtil;
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

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Slf4j
public class AsyncHttpClient {
    private static Config config = SpringContextUtil.getBean(Config.class);
    private static CloseableHttpAsyncClient client;

    static {
        client = constructorSyncHttpClient();
        client.start();
    }

    private static CloseableHttpAsyncClient constructorSyncHttpClient() {
        //配置Reactor io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setSoKeepAlive(true)
                .build();
        //设置连接池大小
        ConnectingIOReactor ioReactor;
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            e.printStackTrace();
            throw new RuntimeException("创建httpClient reactor I/O 配置失败");
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
        return HttpAsyncClients.custom().
                setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public static void sendPost(String uri, Map<String, String> body) {
        String param = new Gson().toJson(body);
        sendPost(uri, param, new DefaultHttpResponseFuture(uri, param));
    }

    public static void sendPost(String uri, String body, FutureCallback<HttpResponse> future) {
        final String url = config.getCallbackDomain() + uri;
        HttpPost httpPost = new HttpPost(url);
        String param = new Gson().toJson(body);
        StringEntity entity;
        try {
            entity = new StringEntity(param);
            entity.setContentEncoding("utf-8");
            entity.setContentType("application/json");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("httpclient编码错误");
        }
        httpPost.setEntity(entity);
        client.execute(httpPost, future);
    }

}
