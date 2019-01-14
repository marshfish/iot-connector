package com.hc.equipment.tcp;

import com.hc.equipment.custom.WriststrapProtocol;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

/**
 * 自定义粘包半包处理器
 */
@Slf4j
public class SplitPacketHandler implements PacketHandler {
    private String PREFIX = WriststrapProtocol.PREFIX;
    private String SUFFIX = WriststrapProtocol.SUFFIX;
    private Queue<String> halfPacket = new ArrayBlockingQueue<>(1);
    private List<String> command = new ArrayList<>(5);
    private Consumer< String> consumer;

    public SplitPacketHandler(Consumer< String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handler(Buffer buffer) {
        String data = buffer.getString(0, buffer.length());
        if (data == null) {
            return;
        }
        boolean start = data.startsWith(PREFIX);
        boolean end = data.endsWith(SUFFIX);
        try {
            if (start && end) {
                //包头尾均无问题，但可能粘包，如IWAP00353456789012345#IWAP03,06000908000102,5555,30#
                String[] splitCommand = data.split(PREFIX);
                for (int i = 1; i < splitCommand.length; i++) {
                    command.add(PREFIX + splitCommand[i]);
                }
            } else if (!start && end) {
                //包头半包包尾没问题，如53456789012345# 或 53456789012345#IWAP03,06000908000102,5555,30#
                int i = data.indexOf(PREFIX);
                if (i == -1) {
                    String poll = halfPacket.remove();
                    command.add(poll + data);
                } else {
                    //粘包
                    String[] splitCommand = data.split(PREFIX);
                    int length = splitCommand.length;
                    String poll = halfPacket.remove();
                    command.add(poll + data.substring(0, i));
                    for (int j = 1; j < length; j++) {
                        command.add(PREFIX + splitCommand[j]);
                    }
                }
            } else if (start) {
                //包头没问题，包尾半包，如IWAP0035345678901 或 IWAP00353456789012345#IWAP03,06000908000102,5555,3
                int i = data.indexOf(SUFFIX);
                if (i == -1) {
                    halfPacket.add(data);
                } else {
                    //粘包
                    String[] splitCommand = data.split(PREFIX);
                    int length = splitCommand.length;
                    for (int j = 1; j < length - 1; j++) {
                        command.add(PREFIX + splitCommand[j]);
                    }
                    halfPacket.add(PREFIX + splitCommand[length - 1]);
                }
            } else {
                //包头包尾均半包，如03534567 或 345IWAP00353456789012345#IWAP03,06000908000102,5555,30#IW598
                int i = data.indexOf(PREFIX);
                if (i == -1) {
                    String firstPacket = halfPacket.remove();
                    halfPacket.add(firstPacket + data);
                } else {
                    //粘包
                    String[] splitCommand = data.split(PREFIX);
                    int length = splitCommand.length;
                    String firstPacket = halfPacket.remove();
                    String lastPacket = splitCommand[0];
                    command.add(firstPacket + lastPacket);
                    for (int j = 1; j < length - 1; j++) {
                        command.add(PREFIX + splitCommand[j]);
                    }
                    halfPacket.add(PREFIX + splitCommand[length - 1]);
                }
            }
        } catch (IllegalStateException | NoSuchElementException e) {
            halfPacket.clear();
            log.error("包处理队列异常,清空队列,{}", e);
            throw e;
        }
        command.forEach(consumer);
        command.clear();
    }

}
