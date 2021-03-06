package com.mcode.connector.business;

import com.google.gson.Gson;
import com.mcode.connector.dispatch.event.DataUploadHandler;
import com.mcode.connector.mvc.ParamEntry;
import com.mcode.connector.mvc.TcpRouter;
import com.mcode.connector.mvc.TcpRouterManager;
import com.mcode.connector.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * tcp数据上传controller
 * 接受设备日常数据上传、设备执行指令的响应
 */
@Slf4j
@TcpRouterManager
@Component
public class WriststrapController extends CommonUtil {
    @Resource
    private DataUploadHandler dataUploadHandler;
    @Resource
    private Gson gson;

    /**
     * 登陆包
     */
    @TcpRouter("AP00")
    public String login(ParamEntry paramEntry) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String format = simpleDateFormat.format(System.currentTimeMillis());
        return buildParam(WriststrapProtocol.PREFIX,
                WriststrapProtocol.BPLS,
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
        dataUploadHandler.uploadData(paramEntry.getEquipmentId(),
                "/beatHeart",
                gson.toJson(map));
        return WriststrapProtocol.HEART_BEAT;
    }

    /**
     * 心率包
     */
    @TcpRouter("AP49")
    public String heartRate(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        String heartRate = instruction.substring(7, instruction.length() - 1);
        log.info("心率测量结果:{}", heartRate);
        dataUploadHandler.uploadData(paramEntry.getEquipmentId(),
                "/beatHeart",
                heartRate);
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
        dataUploadHandler.uploadData(paramEntry.getEquipmentId(),
                "/health",
                gson.toJson(param));
        return WriststrapProtocol.BLOOD;
    }

    /**
     * 下行心率测量响应
     */
    @TcpRouter("APXL")
    public void heartRateResponse(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        log.info("测量心率响应：{}", instruction);
        String seriaNumber = instruction.substring(7, instruction.length() - 1);
        dataUploadHandler.uploadCallback(seriaNumber, paramEntry.getEquipmentId(), instruction);
    }

    /**
     * 机器人响应
     */
    @TcpRouter("APRS")
    public void robotResponse(ParamEntry paramEntry) {
        String instruction = paramEntry.getInstruction();
        log.info("机器人响应：{}", instruction);
        String seriaNumber = instruction.substring(6, instruction.length() - 1);
        dataUploadHandler.uploadCallback(seriaNumber, paramEntry.getEquipmentId(), instruction);
    }

}
