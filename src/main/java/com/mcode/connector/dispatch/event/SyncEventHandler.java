package com.mcode.connector.dispatch.event;

import com.mcode.connector.dispatch.CallbackManager;
import com.mcode.connector.rpc.serialization.Trans;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * 同步事件处理器
 */
@Slf4j
public abstract class SyncEventHandler implements EventHandler, BeanFactoryAware {
    private CallbackManager callbackManager;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        callbackManager = beanFactory.getBean(CallbackManager.class);
    }

    @Override
    public void accept(Trans.event_data event) {
        callbackManager.execCallback(event);
    }
}
