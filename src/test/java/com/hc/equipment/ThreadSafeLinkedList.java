package com.hc.equipment;

import com.hc.equipment.configuration.CommonConfig;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("Duplicates")
public class ThreadSafeLinkedList {
    //equipmentId -> netSocket
    private static ConcurrentHashMap<String, Node<Integer>> socketRegistry = new ConcurrentHashMap<>();
    @Resource
    private CommonConfig commonConfig;
    private final Object mutex = new Object();
    private static Node<Integer> tail;
    private static Node<Integer> head;


    @SuppressWarnings("UnusedAssignment")
    public boolean unRegisterSocket(String eqId) {
        Node<Integer> node;
        if ((node = socketRegistry.get(eqId)) != null) {
            synchronized (mutex) {
                Node<Integer> prev = node.prev;
                Node<Integer> next = node.next;
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
            return true;
        }
        return false;
    }


    public void registerSocket(String eqId, Integer element) {
        synchronized (mutex) {
            Node<Integer> newNode = new Node<>(tail, element, null);
            socketRegistry.put(eqId, newNode);
            if (tail == null) {
                head = newNode;
            } else {
                tail.next = newNode;
            }
            tail = newNode;
        }
    }

    public void doTimeout(String i) {
        long now = System.currentTimeMillis();
        synchronized (mutex) {
            if(head!=null){
                Node<Integer> temp = head;
//            while (temp.lastTime + < now) {
                socketRegistry.remove(i);
                temp = head.next;
//            }
            }

        }
    }

    public void heartBeat(String eqId) {
        Optional.ofNullable(socketRegistry.get(eqId)).ifPresent(this::moveLinkedList);
    }

    public void moveLinkedList(Node<Integer> node) {
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

    static CountDownLatch countDownLatch = new CountDownLatch(3);
    private static AtomicInteger ii = new AtomicInteger(1);

    public static void main(String[] args) {
        ThreadSafeLinkedList loooop = new ThreadSafeLinkedList();
        List<String> list = new ArrayList<>();
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 15000; i++) {
//                    String key = String.valueOf(ints[i]);
                    String value = String.valueOf(i);
                    loooop.registerSocket(value, i);
                    list.add(value);
                    System.out.println("注册：" + value);
                }
                countDownLatch.countDown();
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                for (int i = 0; i < 10000; i++) {
                    int i1 = random.nextInt(7000);
                    String s = String.valueOf(i1);
                    boolean flag = loooop.unRegisterSocket(s);
                    System.out.println("删除：" + s);
                    if (flag) {
                        ii.getAndIncrement();
                    }
                }
                countDownLatch.countDown();
            }
        });


        Thread thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10000; i++) {
                    loooop.doTimeout(String.valueOf(i));
                    System.out.println("心跳：！");
                }
                countDownLatch.countDown();
            }
        });
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int i = 0;
        if (ThreadSafeLinkedList.head != null) {
            Node<Integer> temp = ThreadSafeLinkedList.head;
            System.out.println("链表长度：");
            while (temp.next != null) {
                System.out.println(temp.next.element);
                i++;
                temp = temp.next;
            }
            System.out.println("删除成功++++++++++++++++++++++++ ：" + ii.get());
            System.out.println("最终长度：" + i);
            System.out.println("总共：" + (i + ii.get()));
        }

    }
}
