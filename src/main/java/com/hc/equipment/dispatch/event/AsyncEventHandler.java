package com.hc.equipment.dispatch.event;

import com.hc.equipment.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 扩展抽象类，标注为异步事件处理器
 */
@Slf4j
public abstract class AsyncEventHandler extends CommonUtil implements EventHandler {

    private ThreadFactory factory = r -> {
        Thread thread = new Thread(r);
        thread.setName("blocking-exec-1");
        thread.setDaemon(true);
        return thread;
    };

    /**
     * IO阻塞操作交给这里处理，不要阻塞eventLoop线程
     *
     * @param runnable 操作
     */
    protected void blockingOperation(Runnable runnable) {
        factory.newThread(runnable).start();
    }

}
