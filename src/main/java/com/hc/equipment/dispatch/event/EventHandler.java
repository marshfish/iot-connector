package com.hc.equipment.dispatch.event;

import com.hc.equipment.rpc.MqConnector;
import com.hc.equipment.rpc.PublishEvent;
import com.hc.equipment.rpc.TransportEventEntry;

import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * 事件处理器。connector与dispatcher的消息通信都被抽象成事件，根据业务不同分别提供了异步事件处理器（非阻塞）和同步事件处理器（阻塞），
 * 异步事件处理器：
 * 流程：
 * 发送消息：
 * {@link MqConnector#publishAsync(PublishEvent publishEvent)}方法推送给dispatcher端
 * 回调：
 * RabbitMq发送消息 -> eventBus转发消息-> 相应节点的MqEventDownStream事件循环获取到事件，分配给EventHandler ->
 * AsyncEventHandler调用handler方法
 * 同步事件处理器：继承同步事件处理器的子类不要重写handler方法，因为handler方法是异步的，重写后将无法实现同步调用
 * 流程：
 * 发送消息：
 * {@link MqConnector#publishSync(PublishEvent)}方法 -> Warpper.mockCallback设置同步回调-> rabbitMq推送 -> Warpper.blockingResult阻塞直到结果返回
 * 回调：
 * RabbitMq发送消息 -> eventBus转发消息-> 相应节点的MqEventDownStream事件循环获取到事件，分配给EventHandler ->
 * SyncEventHandler调用handler方法 -> CallbackManager获取同步回调的mockCallback -> 将响应结果set到Warpper里，并唤醒主线程 ->
 * 返回Warpper中的响应结果
 * rabbitmq推送相关详见{@link MqConnector#publishAsync(PublishEvent publishEvent)}
 */
public interface EventHandler extends Consumer<TransportEventEntry> {
    /**
     * 事件处理
     *
     * @param event 事件
     */
    void accept(TransportEventEntry event);

    /**
     * 设置接受的事件类型
     *
     * @return 事件类型
     */
    Integer setReceivedEventType();
}
