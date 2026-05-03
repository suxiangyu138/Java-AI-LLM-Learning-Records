package DataStructure_Java;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 队列数据结构完整教程
 *
 * 队列（Queue）是一种先进先出（FIFO - First In First Out）的线性数据结构
 *
 * 核心特点：
 * 1. 元素从队尾添加（入队 - enqueue）
 * 2. 元素从队头删除（出队 - dequeue）
 * 3. 最先加入的元素最先被移除
 *
 * 生活例子：排队买票、打印任务队列、消息队列
 */
public class 队列 {

    // ==================== 1. 基于数组实现的简单队列 ====================

    static class ArrayQueue {
        private int[] array;
        private int front;      // 队头指针
        private int rear;       // 队尾指针
        private int size;       // 当前元素个数
        private int capacity;   // 队列容量

        public ArrayQueue(int capacity) {
            this.capacity = capacity;
            this.array = new int[capacity];
            this.front = 0;
            this.rear = -1;
            this.size = 0;
        }

        // 入队 - 在队尾添加元素
        public boolean enqueue(int item) {
            if (isFull()) {
                System.out.println("队列已满，无法添加: " + item);
                return false;
            }
            rear = (rear + 1) % capacity;  // 循环队列
            array[rear] = item;
            size++;
            System.out.println("入队: " + item);
            return true;
        }

        // 出队 - 从队头移除元素
        public int dequeue() {
            if (isEmpty()) {
                System.out.println("队列为空，无法出队");
                return -1;
            }
            int item = array[front];
            front = (front + 1) % capacity;  // 循环队列
            size--;
            System.out.println("出队: " + item);
            return item;
        }

        // 查看队头元素
        public int peek() {
            if (isEmpty()) {
                System.out.println("队列为空");
                return -1;
            }
            return array[front];
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public boolean isFull() {
            return size == capacity;
        }

        public int size() {
            return size;
        }

        public void display() {
            if (isEmpty()) {
                System.out.println("队列为空");
                return;
            }
            System.out.print("队列内容: ");
            for (int i = 0; i < size; i++) {
                int index = (front + i) % capacity;
                System.out.print(array[index] + " ");
            }
            System.out.println();
        }
    }

    // ==================== 2. 基于链表实现的队列 ====================

    static class LinkedQueue {

        static class Node {
            int data;
            Node next;

            Node(int data) {
                this.data = data;
                this.next = null;
            }
        }

        private Node front;  // 队头
        private Node rear;   // 队尾
        private int size;

        public LinkedQueue() {
            this.front = null;
            this.rear = null;
            this.size = 0;
        }

        // 入队 - O(1)
        public void enqueue(int item) {
            Node newNode = new Node(item);
            if (isEmpty()) {
                front = rear = newNode;
            } else {
                rear.next = newNode;
                rear = newNode;
            }
            size++;
            System.out.println("入队: " + item);
        }

        // 出队 - O(1)
        public int dequeue() {
            if (isEmpty()) {
                System.out.println("队列为空，无法出队");
                return -1;
            }
            int item = front.data;
            front = front.next;
            if (front == null) {
                rear = null;
            }
            size--;
            System.out.println("出队: " + item);
            return item;
        }

        // 查看队头 - O(1)
        public int peek() {
            if (isEmpty()) {
                System.out.println("队列为空");
                return -1;
            }
            return front.data;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public int size() {
            return size;
        }

        public void display() {
            if (isEmpty()) {
                System.out.println("队列为空");
                return;
            }
            System.out.print("队列内容: ");
            Node current = front;
            while (current != null) {
                System.out.print(current.data + " ");
                current = current.next;
            }
            System.out.println();
        }
    }

    // ==================== 3. Java内置队列的使用示例 ====================

    public static void demonstrateJavaQueue() {
        System.out.println("\n========== Java内置队列演示 ==========");

        // 使用LinkedList作为队列
        Queue<Integer> queue = new LinkedList<>();

        // 入队
        queue.offer(10);  // 推荐使用方法
        queue.offer(20);
        queue.offer(30);
        System.out.println("队列: " + queue);

        // 查看队头
        System.out.println("队头元素: " + queue.peek());

        // 出队
        System.out.println("出队: " + queue.poll());
        System.out.println("队列: " + queue);

        // 检查是否为空
        System.out.println("是否为空: " + queue.isEmpty());
        System.out.println("队列大小: " + queue.size());
    }

    // ==================== 4. 双端队列（Deque）====================

    public static void demonstrateDeque() {
        System.out.println("\n========== 双端队列演示 ==========");

        ArrayDeque<Integer> deque = new ArrayDeque<>();

        // 两端都可以添加和移除
        deque.offerFirst(10);   // 前端添加
        deque.offerLast(20);    // 后端添加
        deque.offerLast(30);

        System.out.println("双端队列: " + deque);
        System.out.println("前端移除: " + deque.pollFirst());
        System.out.println("后端移除: " + deque.pollLast());
        System.out.println("剩余: " + deque);
    }

    // ==================== 5. 队列的经典应用场景 ====================

    // 应用1: 广度优先搜索（BFS）
    public static void bfsExample() {
        System.out.println("\n========== BFS应用示例 ==========");

        // 模拟图的BFS遍历
        boolean[] visited = new boolean[6];
        Queue<Integer> bfsQueue = new LinkedList<>();

        bfsQueue.offer(0);
        visited[0] = true;

        System.out.print("BFS遍历顺序: ");
        while (!bfsQueue.isEmpty()) {
            int node = bfsQueue.poll();
            System.out.print(node + " ");

            // 模拟添加相邻节点
            for (int neighbor = node + 1; neighbor < 6 && neighbor <= node + 2; neighbor++) {
                if (!visited[neighbor]) {
                    bfsQueue.offer(neighbor);
                    visited[neighbor] = true;
                }
            }
        }
        System.out.println();
    }

    // 应用2: 生产者-消费者模型
    public static void producerConsumerExample() {
        System.out.println("\n========== 生产者-消费者模型 ==========");

        LinkedBlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>(5);

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    blockingQueue.put(i);
                    System.out.println("生产: " + i + ", 队列大小: " + blockingQueue.size());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    int item = blockingQueue.take();
                    System.out.println("消费: " + item + ", 队列大小: " + blockingQueue.size());
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ==================== 6. 测试所有功能 ====================

    public static void main(String[] args) {
        System.out.println("===== 队列数据结构完整教程 =====\n");

        // 测试数组队列
        System.out.println("========== 数组实现循环队列 ==========");
        ArrayQueue arrayQueue = new ArrayQueue(5);
        arrayQueue.enqueue(10);
        arrayQueue.enqueue(20);
        arrayQueue.enqueue(30);
        arrayQueue.display();
        arrayQueue.dequeue();
        arrayQueue.display();
        arrayQueue.enqueue(40);
        arrayQueue.enqueue(50);
        arrayQueue.display();

        // 测试链表队列
        System.out.println("\n========== 链表实现队列 ==========");
        LinkedQueue linkedQueue = new LinkedQueue();
        linkedQueue.enqueue(100);
        linkedQueue.enqueue(200);
        linkedQueue.enqueue(300);
        linkedQueue.display();
        linkedQueue.dequeue();
        linkedQueue.display();

        // 测试Java内置队列
        demonstrateJavaQueue();

        // 测试双端队列
        demonstrateDeque();

        // 应用示例
        bfsExample();
        producerConsumerExample();

        System.out.println("\n===== 学习要点总结 =====");
        System.out.println("1. 队列是FIFO（先进先出）结构");
        System.out.println("2. 基本操作：入队(enqueue)、出队(dequeue)、查看队头(peek)");
        System.out.println("3. 可以用数组或链表实现");
        System.out.println("4. 循环队列可以有效利用数组空间");
        System.out.println("5. 常见应用：BFS、任务调度、缓冲队列");
    }
}
