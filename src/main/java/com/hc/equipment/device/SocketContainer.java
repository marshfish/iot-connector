package com.hc.equipment.device;

import com.hc.equipment.configuration.CommonConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Component
public class SocketContainer {
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private DeviceSocketManager deviceSocketManager;
    //equipmentId -> netSocket
    private static ConcurrentHashMap<String, SocketContainer.Node<NetSocket>> socketRegistry = new ConcurrentHashMap<>();
    //netSocket.hashcode() -> equipmentId
    private ConcurrentHashMap<Integer, String> socketIdMapping = new ConcurrentHashMap<>();

    private final Object mutex = new Object();
    private static Node<NetSocket> tail;
    private static Node<NetSocket> head;

    @SuppressWarnings("UnusedAssignment")
    public void unRegisterSocket(Integer socketId, String eqId) {
        socketIdMapping.remove(socketId);
        Node<NetSocket> node;
        if ((node = socketRegistry.get(eqId)) != null) {
            synchronized (mutex) {
                Node<NetSocket> prev = node.prev;
                Node<NetSocket> next = node.next;
                if (node == tail) {
                    if (prev == null) {
                        //head=tail
                        if (socketRegistry.size() != 1) {
                            throw new RuntimeException("链表状态异常");
                        }
                    } else {
                        prev.next = null;
                        tail = prev;
                    }
                } else if (node == head) {
                    head = next;
                    if (head != null) {
                        head.prev = null;
                    }
                } else {
                    prev.next = next;
                    next.prev = prev;
                }
            }
            node = null;
            socketRegistry.remove(eqId);
        }
    }

    public NetSocket getSocket(String eqId) {
        return Optional.ofNullable(socketRegistry.get(eqId)).
                map(netSocketNode -> netSocketNode.element).
                orElse(null);
    }

    public String getEquipmentId(Integer socketHash) {
        return socketIdMapping.get(socketHash);
    }

    public void writeString(String eqId, String data) {
        Optional.ofNullable(socketRegistry.get(eqId)).
                ifPresent(socket -> socket.element.write(Buffer.buffer(data, "UTF-8")));
    }

    public void registerSocket(String eqId, NetSocket element) {
        socketIdMapping.put(element.hashCode(), eqId);
        synchronized (mutex) {
            Node<NetSocket> newNode = new Node<>(tail, element, null);
            socketRegistry.put(eqId, newNode);
            if (tail == null) {
                head = newNode;
            } else {
                tail.next = newNode;
            }
            tail = newNode;
        }
    }

    boolean flag = false;

    public void doTimeout() {
        long now = System.currentTimeMillis();
        synchronized (mutex) {
            Node<NetSocket> temp = head;
            if (temp != null) {
                while (temp != null && temp.lastTime + commonConfig.getTcpTimeout() < now) {
                    String hashCode;
                    int hashCodeStr = temp.element.hashCode();
                    if ((hashCode = socketIdMapping.get(hashCodeStr)) != null) {
                        Node<NetSocket> netSocketNode;
                        if ((netSocketNode = socketRegistry.get(hashCode)) != null) {
                            deviceSocketManager.deviceLogout(hashCodeStr, true);
                            NetSocket element = netSocketNode.element;
                            log.info("心跳超时，断开TCP连接：{}", element.remoteAddress().host());
                            element.close();
                        }
                    }
                    temp = temp.next;
                }
            }
        }
    }

    public void heartBeat(String eqId) {
        Optional.ofNullable(socketRegistry.get(eqId)).ifPresent(this::moveLinkedList);
    }

    public void moveLinkedList(Node<NetSocket> node) {
        synchronized (mutex) {
            if (node != tail) {
                if (node == head) {
                    node.next.prev = null;
                    head = node.next;
                } else {
                    node.prev.next = node.next;
                    node.next.prev = node.prev;
                }
                tail.next = node;
                node.prev = tail;
                node.next = null;
                tail = node;
            }
        }
        node.update();
    }

    /**
     * 在大量socket连接时，有三种方法实现心跳超时扫描
     * 1.每个socket连接new一个定时器，吃cpu
     * 2.lazy check，当需要使用socket时通过超时检查链路，吃内存
     * 3.兼顾时间/空间，如netty的HashedWheelTimer，通过一条线程管理大量定时任务，比较均衡
     * 1不现实，socket连接数过多时可能导致线程栈溢出，2不吃cpu，但由于socket长时间未使用导致内存释放不及时，tcp默认timeout120分钟
     * 3比较靠谱，但并不适用当前场景，设备心跳频繁可能需要重复新建取消大量定时器
     * 因此采用线程安全的双向链表，由于是长连接，设备登录登出不频繁，将设备心跳时的资源消耗挪一部分到设备登陆登出时
     * 当设备socket心跳时会被挪到链表尾部，从而链表头部总会是心跳超时的连接，既可以保证内存释放及时，又不吃cpu
     * timer-ping-1线程定时从链表头部扫描并清除死链，由于并非线程安全，全部加锁，可以继续优化为无锁的双向链表
     */
    public static class Node<E> {
        private Node<E> prev;
        private Node<E> next;
        private E element;
        private long lastTime = System.currentTimeMillis();

        public Node(Node<E> prev, E element, Node<E> next) {
            this.prev = prev;
            this.element = element;
            this.next = next;
        }

        public void update() {
            this.lastTime = System.currentTimeMillis();
        }
    }

}
