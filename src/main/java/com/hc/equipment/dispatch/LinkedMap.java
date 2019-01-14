package com.hc.equipment.dispatch;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 自定义链表
 * 便于处理各种超时情况，如socket心跳超时清除死链、由于网络原因回调未执行导致回调列表内存泄漏等
 * <p>
 * 心跳超时一般用定时器/扫描哈希表/时间轮定时器HashedWheelTimer/lazy check，运行时检查链路有效性
 * 但连接数过多时（10k以上）以上均不适用，原因分别是线程数过多，拖垮系统/太吃cpu/重复新建取消定时器复杂度过高/内存不能及时释放
 * 由于jdk linkedList实现不了自定义逻辑，重写linkedList，在socket心跳时会被移动至链表尾部，
 * 从而实现链表头部只会有已超时的连接，只扫描链表头部即可清除死链，把一部分复杂度从心跳时挪到socket连接/关闭时，减少cpu与内存消耗
 */
@Slf4j
public class LinkedMap<S, E> {
    private Map<S, Node<S, E>> map = new HashMap<>();
    private final Object mutex;
    private Node<S, E> tail;
    private Node<S, E> head;

    public LinkedMap() {
        this.mutex = this;
    }

    public LinkedMap(Object lock) {
        this.mutex = lock;
    }

    public E get(S key) {
        return Optional.ofNullable(map.get(key)).map(Node::getElement).orElse(null);
    }

    public Node<S, E> getNode(S key) {
        return map.get(key);
    }

    public Set<Map.Entry<S, Node<S, E>>> getEntries() {
        synchronized (mutex) {
            return map.entrySet();
        }
    }

    @SuppressWarnings("UnusedAssignment")
    public boolean remove(S key) {
        log.info("remove linkedMap,{}", key);
        Node<S, E> node = map.get(key);
        if (node == null) {
            return false;
        }
        Node<S, E> prev = node.prev;
        Node<S, E> next = node.next;
        synchronized (mutex) {
            if (node == tail && node == head) {
                head = tail = null;
            } else if (node == tail) {
                prev.next = null;
                tail = prev;
            } else if (node == head) {
                head.prev = null;
                head = next;
            } else {
                prev.next = next;
                next.prev = prev;
            }
            node = null;
            map.remove(key);
        }
        return true;
    }


    public void addTail(S key, E element) {
        log.info("add LinkedMap:{},{}", key, element);
        synchronized (mutex) {
            Node<S, E> newNode = new Node<>(key, tail, element, null);
            if (tail == null) {
                head = newNode;
            } else {
                tail.next = newNode;
            }
            tail = newNode;
            map.put(key, newNode);
        }
    }

    public void moveToTail(Node<S, E> node) {
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

    public void onTimeout(Predicate<Node<S, E>> condition) {
        synchronized (mutex) {
            Node<S, E> temp = head;
            while (temp != null && condition.test(temp)) {
                log.info("节点666;{},{}", temp.getElement().hashCode(), temp.getLastTime());
                temp = temp.next;
            }
        }
    }

    public static class Node<S, E> {
        private Node<S, E> prev;
        private Node<S, E> next;
        private S key;
        private E element;
        private long lastTime = System.currentTimeMillis();

        private Node(S key, Node<S, E> prev, E element, Node<S, E> next) {
            this.key = key;
            this.prev = prev;
            this.element = element;
            this.next = next;
        }

        public E getElement() {
            return element;
        }

        public S getKey() {
            return key;
        }

        public long getLastTime() {
            return lastTime;
        }

        private void update() {
            log.info("更新时间*********************");
            this.lastTime = System.currentTimeMillis();
        }
    }
}
