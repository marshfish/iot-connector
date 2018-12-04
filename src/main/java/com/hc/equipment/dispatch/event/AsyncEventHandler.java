package com.hc.equipment.dispatch.event;

import com.hc.equipment.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadFactory;

/**
 * 扩展抽象类，标注为异步事件处理器
 */
@Slf4j
public abstract class AsyncEventHandler extends CommonUtil implements EventHandler {

}
