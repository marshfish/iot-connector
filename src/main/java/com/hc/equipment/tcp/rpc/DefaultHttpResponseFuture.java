package com.hc.equipment.tcp.rpc;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

@Slf4j
public class DefaultHttpResponseFuture implements FutureCallback<HttpResponse> {
    private String url;
    private String param;

    public DefaultHttpResponseFuture(String url, String param) {
        this.url = url;
        this.param = param;
    }

    @Override
    public void completed(HttpResponse httpResponse) {
        log.info("调用 {} 接口成功,参数：{}", url, param);
    }

    @Override
    public void failed(Exception e) {
        log.warn("访问接口失败，uri：{},param:{},Exception:{}", url, param, e);
    }

    @Override
    public void cancelled() {
        log.info("取消调用 {} 接口,参数：{}", url, param);
    }
}
