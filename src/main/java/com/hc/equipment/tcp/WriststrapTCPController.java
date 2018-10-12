package com.hc.equipment.tcp;

import com.hc.equipment.tcp.mvc.Instruction;
import com.hc.equipment.tcp.mvc.InstructionManager;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.tcp.rpc.AsyncHttpClient;
import com.hc.equipment.tcp.rpc.ResponseFuture;
import com.hc.equipment.tcp.rpc.WriststrapRestUri;
import com.hc.equipment.util.SpringContextUtil;
import com.hc.equipment.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@InstructionManager
public class WriststrapTCPController {
    private AsyncHttpClient asyncHttpClient = SpringContextUtil.getBean(AsyncHttpClient.class);

    /**
     * 登陆包
     */
    @Instruction("AP00")
    public String login(String deviceUniqueId, String data) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return Util.buildParam(WriststrapProtocol.PREFIX, WriststrapProtocol.BP00, WriststrapProtocol.COMMA, format, WriststrapProtocol.COMMA, WriststrapProtocol.TIME_ZONE);
    }

    /**
     * 心跳包
     */
    @Instruction("AP03")
    public String heartBeat(String deviceUniqueId, String data) {
        String[] split = data.split(WriststrapProtocol.COMMA);
        String electricity = split[1].substring(6, 9);
        log.info("心跳包获取电量：{}", electricity);
        Map<String, String> map = new HashMap<>();
        map.put("battery", electricity);
        map.put("imei", deviceUniqueId);
        map.put("pedometer", "250666");
        asyncHttpClient.sendPost(WriststrapRestUri.BEAT_HEART.path, map, new ResponseFuture() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail() {

            }

            @Override
            public void onCancel() {

            }
        });
        return WriststrapProtocol.HEART_BEAT;
    }

    /**
     * 心率包
     */
    @Instruction("AP49")
    public String heartRate(String deviceUniqueId, String data) {
        log.info("心率测量结果:{}", data.substring(7, data.length() - 1));
        return WriststrapProtocol.HEART_RATE;
    }

    /**
     * 血压包
     */
    @Instruction("APHT")
    public String blood(String deviceUniqueId, String data) {
        String[] split = data.split(WriststrapProtocol.COMMA);
        log.info("血压测量结果--心率：{}，高压：{}，低压：{}", split[1], split[2], split[3].substring(0, split[3].length() - 1));
        return WriststrapProtocol.BLOOD;
    }
}
