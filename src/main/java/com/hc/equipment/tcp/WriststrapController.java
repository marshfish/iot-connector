package com.hc.equipment.tcp;

import com.hc.equipment.tcp.mvc.Instruction;
import com.hc.equipment.tcp.mvc.InstructionManager;
import com.hc.equipment.device.CommonDevice;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.util.Util;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.text.SimpleDateFormat;


@Slf4j
@InstructionManager
public class WriststrapController {
    @Resource
    private CommonDevice commonDevice;

    /**
     * 登陆包
     */
    @Instruction("AP00")
    public String login(String data) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return Util.buildParam(WriststrapProtocol.PREFIX, WriststrapProtocol.BP00, WriststrapProtocol.COMMA, format, WriststrapProtocol.COMMA, WriststrapProtocol.TIME_ZONE);
    }

    /**
     * 心跳包
     */
    @Instruction("AP03")
    public void heartBeat(String data) {
        String[] split = data.split(WriststrapProtocol.COMMA);
        String electricity = split[1].substring(6, 9);
        log.info("心跳包获取电量：{}",electricity );
//        return WriststrapProtocol.HEART_BEAT;
    }

    /**
     * 心率包
     */
    @Instruction("AP49")
    public String heartRate(String data) {
        log.info("心率测量结果:{}", data.substring(7, data.length() - 1));
        return WriststrapProtocol.HEART_RATE;
    }

    /**
     * 血压包
     */
    @Instruction("APHT")
    public String blood(String data) {
        String[] split = data.split(WriststrapProtocol.COMMA);
        log.info("血压测量结果--心率：{}，高压：{}，低压：{}", split[1], split[2], split[3].substring(0, split[3].length() - 1));
        return WriststrapProtocol.BLOOD;
    }
}
