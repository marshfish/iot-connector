package com.hc.equipment.tcp;

import com.hc.equipment.connector.MqConnector;
import com.hc.equipment.mvc.TcpRouter;
import com.hc.equipment.mvc.TcpRouterManager;
import com.hc.equipment.mvc.ParamEntry;
import com.hc.equipment.tcp.promise.WriststrapProtocol;
import com.hc.equipment.tcp.rpc.AsyncHttpClient;
import com.hc.equipment.tcp.rpc.WriststrapRestUri;
import com.hc.equipment.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * tcp -> http
 */
@Slf4j
@TcpRouterManager
@Component
public class WriststrapTCPController extends CommonUtil {
    @Resource
    private MqConnector mqConnector;
    /**
     * 登陆包
     */
    @TcpRouter("AP00")
    public String login(ParamEntry paramEntry) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return buildParam(WriststrapProtocol.PREFIX,
                WriststrapProtocol.BP00,
                WriststrapProtocol.COMMA,
                format,
                WriststrapProtocol.COMMA,
                WriststrapProtocol.TIME_ZONE);
    }

    /**
     * 心跳包
     */
    @TcpRouter("AP03")
    public String heartBeat(ParamEntry paramEntry) {
        String[] split = paramEntry.getInstruction().split(WriststrapProtocol.COMMA);
        String electricity = split[1].substring(6, 9);
        log.info("心跳包获取电量：{}", electricity);
        Map<String, String> map = new HashMap<>();
        map.put("battery", electricity);
        map.put("imei", paramEntry.getEquipmentId());
        map.put("pedometer", "250666");
        AsyncHttpClient.sendPost(WriststrapRestUri.BEAT_HEART.path, map);
        return WriststrapProtocol.HEART_BEAT;
    }

    /**
     * 心率包
     */
    @TcpRouter("AP49")
    public String heartRate(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        log.info("心率测量结果:{}", instruction.substring(7, instruction.length() - 1));
        return WriststrapProtocol.HEART_RATE;
    }

    /**
     * 血压包
     */
    @TcpRouter("APHT")
    public String blood(ParamEntry paramEntry) {
        String[] split = paramEntry.getInstruction().split(WriststrapProtocol.COMMA);
        log.info("血压测量结果--心率：{}，高压：{}，低压：{}", split[1], split[2],
                split[3].substring(0, split[3].length() - 1));
        Map<String, String> param = new HashMap<>();
        param.put("imei", paramEntry.getEquipmentId());
        param.put("heartRate", split[1]);
        param.put("sdp", split[2]);
        param.put("dbp", split[3].substring(0, split[3].length() - 1));
        param.put("oxygen", "100");
        AsyncHttpClient.sendPost(WriststrapRestUri.HEALTH.path, param);
        return WriststrapProtocol.BLOOD;
    }

    /**
     * 下行心率测量响应
     */
    @TcpRouter("APXL")
    public void heartRateResponse(ParamEntry paramEntry) {
        log.info("测量心率响应：{}",paramEntry.getInstruction());

        Optional.ofNullable(paramEntry.getInstruction()).
                map(instruction -> instruction.substring(7, instruction.length() - 1)).
                ifPresent(uuid -> mqConnector.producer(""));
    }


}
