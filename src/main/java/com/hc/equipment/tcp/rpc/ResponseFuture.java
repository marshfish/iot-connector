package com.hc.equipment.tcp.rpc;

public interface ResponseFuture {
    void onSuccess();
    void onFail();
    void onCancel();
}
