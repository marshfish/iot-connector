package com.hc.equipment.tcp;

import com.hc.equipment.mvc.TcpInstruction;
import com.hc.equipment.mvc.TcpInstructionManager;
import com.hc.equipment.mvc.ParamEntry;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.tcp.rpc.AsyncHttpClient;
import com.hc.equipment.tcp.rpc.WriststrapRestUri;
import com.hc.equipment.util.Util;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@TcpInstructionManager
public class WriststrapTCPController {
    /**
     * 登陆包
     */
    @TcpInstruction("AP00")
    public String login(ParamEntry paramEntry) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return Util.buildParam(WriststrapProtocol.PREFIX, WriststrapProtocol.BP00, WriststrapProtocol.COMMA, format, WriststrapProtocol.COMMA, WriststrapProtocol.TIME_ZONE);
    }

    /**
     * 心跳包
     */
    @TcpInstruction("AP03")
    public String heartBeat(ParamEntry paramEntry) {
        String[] split = paramEntry.getInstruction().split(WriststrapProtocol.COMMA);
        String electricity = split[1].substring(6, 9);
        log.info("心跳包获取电量：{}", electricity);
        Map<String, String> map = new HashMap<>();
        map.put("battery", electricity);
        map.put("imei", paramEntry.getUniqueId());
        map.put("pedometer", "250666");
        AsyncHttpClient.sendPost(WriststrapRestUri.BEAT_HEART.path, map);
        return WriststrapProtocol.HEART_BEAT;
    }

    /**
     * 心率包
     */
    @TcpInstruction("AP49")
    public String heartRate(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        log.info("心率测量结果:{}", instruction.substring(7, instruction.length() - 1));
        return WriststrapProtocol.HEART_RATE;
    }

    /**
     * 血压包
     */
    @TcpInstruction("APHT")
    public String blood(ParamEntry paramEntry) {
        String[] split = paramEntry.getInstruction().split(WriststrapProtocol.COMMA);
        log.info("血压测量结果--心率：{}，高压：{}，低压：{}", split[1], split[2], split[3].substring(0, split[3].length() - 1));
        return WriststrapProtocol.BLOOD;
    }

    /**
     * 下行心率测量响应
     */
    @TcpInstruction("APXL")
    public String heartRateResponse(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        return "";
    }
}
