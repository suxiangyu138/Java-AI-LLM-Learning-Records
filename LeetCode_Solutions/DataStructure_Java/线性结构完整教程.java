package DataStructure_Java;

import java.util.*;

public class 线性结构完整教程 {

    // ==================== 1. 数组（Array）====================
    
    static class ArrayDemo {
        public static void run() {
            System.out.println("\n========== 数组（Array）==========");
            
            int[] arr = new int[5];
            arr[0] = 10;
            arr[1] = 20;
            arr[2] = 30;
            arr[3] = 40;
            arr[4] = 50;
            
            System.out.println("访问arr[2]: " + arr[2]);
            System.out.println("数组长度: " + arr.length);
            
            System.out.print("遍历数组: ");
            for (int num : arr) {
                System.out.print(num + " ");
            }
            System.out.println();
            
            System.out.println("✅ 特点:");
            System.out.println("   - 连续内存空间");
            System.out.println("   - 随机访问 O(1)");
            System.out.println("   - 插入/删除 O(n)");
            System.out.println("   - 大小固定");
        }
    }

    // ==================== 2. 链表（Linked List）====================
    
    static class LinkedListDemo {
        
        static class Node {
            int data;
            Node next;
            
            Node(int data) {
                this.data = data;
                this.next = null;
            }
        }
        
        private Node head;
        private int size;
        
        public LinkedListDemo() {
            this.head = null;
            this.size = 0;
        }
        
        public void addFirst(int item) {
            Node newNode = new Node(item);
            newNode.next = head;
            head = newNode;
            size++;
        }
        
        public void addLast(int item) {
            Node newNode = new Node(item);
            if (head == null) {
                head = newNode;
            } else {
                Node current = head;
                while (current.next != null) {
                    current = current.next;
                }
                current.next = newNode;
            }
            size++;
        }
        
        public int removeFirst() {
            if (head == null) {
                throw new NoSuchElementException("链表为空");
            }
            int item = head.data;
            head = head.next;
            size--;
            return item;
        }
        
        public boolean search(int target) {
            Node current = head;
            while (current != null) {
                if (current.data == target) {
                    return true;
                }
                current = current.next;
            }
            return false;
        }
        
        public void display() {
            Node current = head;
            System.out.print("链表: ");
            while (current != null) {
                System.out.print(current.data + " -> ");
                current = current.next;
            }
            System.out.println("null");
        }
        
        public static void run() {
            System.out.println("\n========== 链表（Linked List）==========");
            
            LinkedListDemo list = new LinkedListDemo();
            list.addLast(10);
            list.addLast(20);
            list.addLast(30);
            list.addFirst(5);
            list.display();
            
            System.out.println("查找20: " + list.search(20));
            System.out.println("删除头节点: " + list.removeFirst());
            list.display();
            
            System.out.println("✅ 特点:");
            System.out.println("   - 非连续内存，动态分配");
            System.out.println("   - 插入/删除 O(1)（已知位置）");
            System.out.println("   - 访问 O(n)");
            System.out.println("   - 大小可动态扩展");
        }
    }

    // ==================== 3. 栈（Stack）====================
    
    static class StackDemo {
        
        static class ArrayStack {
            private int[] array;
            private int top;
            private int capacity;
            
            public ArrayStack(int capacity) {
                this.capacity = capacity;
                this.array = new int[capacity];
                this.top = -1;
            }
            
            public void push(int item) {
                if (isFull()) {
                    throw new StackOverflowError("栈已满");
                }
                array[++top] = item;
            }
            
            public int pop() {
                if (isEmpty()) {
                    throw new NoSuchElementException("栈为空");
                }
                return array[top--];
            }
            
            public int peek() {
                if (isEmpty()) {
                    throw new NoSuchElementException("栈为空");
                }
                return array[top];
            }
            
            public boolean isEmpty() {
                return top == -1;
            }
            
            public boolean isFull() {
                return top == capacity - 1;
            }
            
            public int size() {
                return top + 1;
            }
        }
        
        public static void run() {
            System.out.println("\n========== 栈（Stack）==========");
            
            ArrayStack stack = new ArrayStack(5);
            stack.push(10);
            stack.push(20);
            stack.push(30);
            
            System.out.println("栈顶元素: " + stack.peek());
            System.out.println("弹出: " + stack.pop());
            System.out.println("弹出: " + stack.pop());
            System.out.println("当前大小: " + stack.size());
            
            System.out.println("\n📌 应用场景:");
            System.out.println("   - 函数调用栈");
            System.out.println("   - 表达式求值");
            System.out.println("   - 括号匹配");
            System.out.println("   - 撤销操作");
            
            System.out.println("\n✅ 特点: LIFO（后进先出）");
            System.out.println("   - push/pop/peek 都是 O(1)");
        }
    }

    // ==================== 4. 队列（Queue）====================
    
    static class QueueDemo {
        public static void run() {
            System.out.println("\n========== 队列（Queue）==========");
            
            Queue<Integer> queue = new LinkedList<>();
            queue.offer(10);
            queue.offer(20);
            queue.offer(30);
            
            System.out.println("队列: " + queue);
            System.out.println("队头: " + queue.peek());
            System.out.println("出队: " + queue.poll());
            System.out.println("队列: " + queue);
            
            System.out.println("\n📌 应用场景:");
            System.out.println("   - BFS广度优先搜索");
            System.out.println("   - 任务调度");
            System.out.println("   - 消息队列");
            System.out.println("   - 缓冲区");
            
            System.out.println("\n✅ 特点: FIFO（先进先出）");
            System.out.println("   - offer/poll/peek 都是 O(1)");
        }
    }

    // ==================== 5. 双端队列（Deque）====================
    
    static class DequeDemo {
        public static void run() {
            System.out.println("\n========== 双端队列（Deque）==========");
            
            Deque<Integer> deque = new ArrayDeque<>();
            deque.offerFirst(10);
            deque.offerLast(20);
            deque.offerFirst(5);
            deque.offerLast(30);
            
            System.out.println("双端队列: " + deque);
            System.out.println("从前端移除: " + deque.pollFirst());
            System.out.println("从后端移除: " + deque.pollLast());
            System.out.println("剩余: " + deque);
            
            System.out.println("\n✅ 特点:");
            System.out.println("   - 两端都可以插入和删除");
            System.out.println("   - 可实现栈和队列的功能");
            System.out.println("   - 所有操作 O(1)");
        }
    }

    // ==================== 6. 综合对比 ====================
    
    static class Comparison {
        public static void run() {
            System.out.println("\n========== 线性结构对比 ==========");
            System.out.println("┌──────────┬──────────┬──────────┬──────────┐");
            System.out.println("│  结构    │  访问    │  插入    │  删除    │");
            System.out.println("├──────────┼──────────┼──────────┼──────────┤");
            System.out.println("│  数组    │  O(1)    │  O(n)    │  O(n)    │");
            System.out.println("│  链表    │  O(n)    │  O(1)*   │  O(1)*   │");
            System.out.println("│  栈      │  O(n)    │  O(1)    │  O(1)    │");
            System.out.println("│  队列    │  O(n)    │  O(1)    │  O(1)    │");
            System.out.println("│  双端队列│  O(n)    │  O(1)    │  O(1)    │");
            System.out.println("└──────────┴──────────┴──────────┴──────────┘");
            System.out.println("*指已知位置的情况");
            
            System.out.println("\n💡 选择建议:");
            System.out.println("   - 需要频繁访问 → 数组");
            System.out.println("   - 需要频繁插入/删除 → 链表");
            System.out.println("   - 后入先出场景 → 栈");
            System.out.println("   - 先入先出场景 → 队列");
            System.out.println("   - 两端操作 → 双端队列");
        }
    }

    // ==================== 7. 实战练习 ====================
    
    static class Practice {
        
        public static boolean isValidParentheses(String s) {
            Deque<Character> stack = new ArrayDeque<>();
            for (char c : s.toCharArray()) {
                if (c == '(' || c == '[' || c == '{') {
                    stack.push(c);
                } else {
                    if (stack.isEmpty()) return false;
                    char top = stack.pop();
                    if ((c == ')' && top != '(') ||
                        (c == ']' && top != '[') ||
                        (c == '}' && top != '{')) {
                        return false;
                    }
                }
            }
            return stack.isEmpty();
        }
        
        public static int[] reverseArray(int[] arr) {
            Stack<Integer> stack = new Stack<>();
            for (int num : arr) {
                stack.push(num);
            }
            int[] reversed = new int[arr.length];
            for (int i = 0; i < arr.length; i++) {
                reversed[i] = stack.pop();
            }
            return reversed;
        }
        
        public static void run() {
            System.out.println("\n========== 实战练习 ==========");
            
            System.out.println("1. 括号匹配验证:");
            String test1 = "{[()]}";
            String test2 = "{[(])}";
            System.out.println("   \"" + test1 + "\" → " + isValidParentheses(test1));
            System.out.println("   \"" + test2 + "\" → " + isValidParentheses(test2));
            
            System.out.println("\n2. 数组反转:");
            int[] arr = {1, 2, 3, 4, 5};
            int[] reversed = reverseArray(arr);
            System.out.println("   原数组: " + Arrays.toString(arr));
            System.out.println("   反转后: " + Arrays.toString(reversed));
        }
    }

    // ==================== 主方法 ====================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║     线性结构快速入门教程           ║");
        System.out.println("╚════════════════════════════════════╝");
        
        ArrayDemo.run();
        LinkedListDemo.run();
        StackDemo.run();
        QueueDemo.run();
        DequeDemo.run();
        Comparison.run();
        Practice.run();
        
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println("║     🎉 恭喜完成线性结构学习！      ║");
        System.out.println("╚════════════════════════════════════╝");
    }
}
