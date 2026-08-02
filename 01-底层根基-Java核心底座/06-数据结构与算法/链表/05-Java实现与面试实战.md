# Java实现与面试实战
> Hot 100 链表全覆盖 + 手写话术 + LRU 设计题——链表是最能体现"画图功力"的题型

## 📚 目录
1. [LeetCode 链表高频题映射](#1-leetcode-链表高频题映射)
2. [面试手写链表话术](#2-面试手写链表话术)
3. [高频追问与标准回答](#3-高频追问与标准回答)
4. [设计题：146. LRU 缓存](#4-设计题146-lru-缓存)
5. [进阶题复盘：143 重排链表 / 138 随机复制](#5-进阶题复盘143-重排链表--138-随机复制)
6. [调试技巧与自检清单](#6-调试技巧与自检清单)

---

## 1. LeetCode 链表高频题映射

### 1.1 全题单

| 题号 | 题目 | 类型 | 难度 | 一句话要点 | 对应模块 |
|:---:|------|------|:---:|------|:---:|
| 206 | 反转链表 | 反转 | 🟢 | 三指针/递归，母题 | [02-反转系列](02-反转链表系列.md) |
| 92 | 反转链表 II | 反转 | 🟡 | 头插法反转区间 | [02-反转系列](02-反转链表系列.md) |
| 24 | 两两交换 | 反转 | 🟡 | dummy + 三指针 | [02-反转系列](02-反转链表系列.md) |
| 25 | K 个一组翻转 | 反转 | 🔴 | 区间反转 + 拼接 | [02-反转系列](02-反转链表系列.md) |
| 141 | 环形链表 | 快慢指针 | 🟢 | Floyd 判圈 | [03-环形快慢](03-环形与快慢指针.md) |
| 142 | 环形链表 II | 快慢指针 | 🟡 | 重置 slow 找入口 | [03-环形快慢](03-环形与快慢指针.md) |
| 876 | 链表中点 | 快慢指针 | 🟢 | 快 2 慢 1 | [03-环形快慢](03-环形与快慢指针.md) |
| 19 | 删除倒数第 N | 快慢指针 | 🟡 | fast 先走 N+1 | [03-环形快慢](03-环形与快慢指针.md) |
| 160 | 相交链表 | 快慢指针 | 🟢 | 走完跳对方 | [03-环形快慢](03-环形与快慢指针.md) |
| 234 | 回文链表 | 综合 | 🟢 | 中点+反转+比较 | [03-环形快慢](03-环形与快慢指针.md) |
| 21 | 合并两有序链 | 合并 | 🟢 | 双指针 + dummy | [04-合并排序](04-合并与排序链表.md) |
| 23 | 合并 K 链 | 合并 | 🔴 | 优先队列 | [04-合并排序](04-合并与排序链表.md) |
| 148 | 排序链表 | 排序 | 🟡 | 归并 + 快慢指针 | [04-合并排序](04-合并与排序链表.md) |
| 86 | 分隔链表 | 操作 | 🟡 | 双 dummy + 断环 | [04-合并排序](04-合并与排序链表.md) |
| 82 | 删除重复 II | 操作 | 🟡 | 跳过重复区间 | [04-合并排序](04-合并与排序链表.md) |
| 2 | 两数相加 | 操作 | 🟡 | 进位 + 双指针 | — |
| 61 | 旋转链表 | 操作 | 🟡 | 首尾相连 + 断链 | — |
| 143 | 重排链表 | 进阶 | 🟡 | 中点+反转+交替 | 05 模块 |
| 138 | 随机链表复制 | 进阶 | 🟡 | 哈希表 / 原地复制 | 05 模块 |
| 146 | LRU 缓存 | 设计 | 🟡 | 哈希表+双向链表 | 05 模块 |

### 1.2 面试频率 TOP 8

```
排名  题号  题目                面经出现频率
 1    206   反转链表            ████████████████████
 2    141   环形链表            ██████████████████
 3    21    合并两有序链        ████████████████
 4    19    删除倒数第 N        ██████████████
 5    142   环形链表 II         █████████████
 6    146   LRU 缓存            ████████████
 7    160   相交链表            ██████████
 8    148   排序链表            █████████
```

---

## 2. 面试手写链表话术

```
📣 第 1 步：识别技巧 (10秒)
"这道题的核心技巧是（反转三指针/快慢指针/dummy 虚拟头）。"

📣 第 2 步：说明边界 (15秒)
"我先考虑边界：空链表、单节点、删除头节点等特殊场景。
 为了统一处理头节点操作，我先建一个 dummy 虚拟头。"

📣 第 3 步：画图推演 (60秒) ⭐ 链表题的灵魂
"我画一下指针变化过程：
 [1] → [2] → [3]
 prev=null, curr=1
 第一步：保存 next=2，反转 curr.next=null，
         更新 prev=1, curr=2 ..."

📣 第 4 步：写代码 + 复杂度 (2分钟)
"迭代版空间 O(1)；如果要求递归版，空间 O(n) 有栈溢出风险。
 链表很长时我会优先迭代。"
```

### 面试自检话术

```
"写链表题我有个习惯：
 ① 先画图把指针变化走一遍，不画图不动手；
 ② 修改链表必用 dummy，返回 dummy.next；
 ③ 边界优先：null、单节点、头尾节点；
 ④ 迭代为主，递归为辅。"
```

---

## 3. 高频追问与标准回答

| # | 追问 | 标准回答 |
|:---:|------|------|
| 1 | 反转链表迭代和递归空间差异？ | 迭代 O(1)，递归 O(n) 栈空间。链表长时递归可能栈溢出，面试主推迭代 |
| 2 | dummy 节点的作用？ | 统一头节点操作：删除头节点、确定新头等场景无需特判，代码更简洁不易错 |
| 3 | 为什么快指针走 2 步？ | 相对速度 1 保证任何环长必相遇；走 3 步以上可能跳过 slow 永远追不上 |
| 4 | 142 找环入口的数学原理？ | 设 a=头到入口，b=入口到相遇点，c=相遇点到入口，由 fast=2×slow 推得 a=(k-1)L+c，重置 slow 后同速走，再次相遇即入口 |
| 5 | 链表排序为什么用归并？ | 链表无随机访问，快排 partition 低效易退化；归并只需改 next 指针，稳定 O(n log n) |
| 6 | 合并 K 链的复杂度？ | 堆大小 K，每次取最小 O(log K)，总节点 N → O(N log K) |
| 7 | 分隔链表为什么要断开尾部？ | 原链表尾节点可能还指向前面节点，不断会成环 |
| 8 | 链表中点的奇偶差异？ | 偶数长度返回第二个中点（slow 停在右中点），回文题要配合反转后半处理 |

---

## 4. 设计题：146. LRU 缓存

### 4.1 为什么用"哈希表 + 双向链表"？

```
需求：get 和 put 都 O(1)
  get O(1) → 哈希表（key → 节点）
  有序性（最近使用排前）→ 链表
  删除任意节点 O(1) → 双向链表（单向链表删任意节点需 O(n) 找前驱）

设计：
  哈希表：key → Node
  双向链表：头部 = 最近使用，尾部 = 最久未使用
  get：命中 → 移到头部
  put：存在 → 更新值 + 移到头部；不存在 → 插入头部，超容量删尾部
```

### 4.2 完整实现

```java
import java.util.*;

/**
 * LeetCode 146. LRU 缓存（面试最高频设计题）
 * 哈希表 + 双向链表，get/put 均为 O(1)
 */
class LRUCache {
    // 双向链表节点
    class Node {
        int key, value;
        Node prev, next;
        Node(int key, int value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(-1, -1);   // 虚拟头
    private final Node tail = new Node(-1, -1);   // 虚拟尾

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;          // 初始化空链表
        tail.prev = head;
    }

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) return -1;
        moveToHead(node);          // 使用过 → 移到头部
        return node.value;
    }

    public void put(int key, int value) {
        Node node = map.get(key);
        if (node != null) {
            node.value = value;    // 更新值
            moveToHead(node);      // 移到头部
        } else {
            node = new Node(key, value);
            map.put(key, node);
            addToHead(node);       // 新节点放头部
            if (map.size() > capacity) {
                Node removed = removeTail();   // 超出容量删尾部
                map.remove(removed.key);
            }
        }
    }

    // ── 双向链表四件套 ──────────────────────────────
    private void addToHead(Node node) {          // 插入头部
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {         // 摘除节点
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node node) {         // 移到头部 = 摘除 + 插入
        removeNode(node);
        addToHead(node);
    }

    private Node removeTail() {                  // 删除尾部节点
        Node node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

### 4.3 面试追问

| 追问 | 回答 |
|------|------|
| 为什么不用单链表？ | 删除任意节点需要找前驱 O(n)，双向链表 O(1) |
| 为什么不用 LinkedHashMap？ | 可以（removeEldestEntry），但面试要展示底层实现能力，手写更有说服力 |
| 虚拟头尾的作用？ | 空链表操作统一，无需判空 |
| put 已存在时？ | 更新 value 并移到头部，不新增节点 |
| 线程安全？ | 不加锁不安全；可加锁或使用 ConcurrentHashMap |

---

## 5. 进阶题复盘：143 重排链表 / 138 随机复制

### 5.1 143. 重排链表（美团/字节面经题）

```java
/**
 * LeetCode 143. 重排链表
 * L0→L1→...→Ln-1→Ln 重排为 L0→Ln→L1→Ln-1→L2→Ln-2→...
 *
 * 三步：① 找中点 ② 反转后半 ③ 交替合并
 * 1→2→3→4→5 → 1→5→2→4→3
 */
public void reorderList(ListNode head) {
    if (head == null || head.next == null) return;

    // ① 找中点
    ListNode slow = head, fast = head;
    while (fast.next != null && fast.next.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }

    // ② 反转后半段
    ListNode second = reverse(slow.next);
    slow.next = null;               // 断开

    // ③ 交替合并：第一段和反转后的第二段
    ListNode first = head;
    while (second != null) {
        ListNode nextFirst = first.next;
        ListNode nextSecond = second.next;
        first.next = second;        // 第一段节点 → 接第二段节点
        second.next = nextFirst;    // 第二段节点 → 接第一段下一个
        first = nextFirst;
        second = nextSecond;
    }
}

private ListNode reverse(ListNode head) {
    ListNode prev = null, curr = head;
    while (curr != null) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    return prev;
}
```

### 5.2 138. 随机链表的复制

```java
/**
 * LeetCode 138. 随机链表的复制
 * 每个节点除了 next 还有 random 指针（可指向任意节点或 null），
 * 深拷贝整个链表。
 *
 * 方法一（哈希表）：O(n) 时间 O(n) 空间
 *   Map<Node, Node>：原节点 → 新节点
 *   第一遍创建新节点建映射，第二遍连接 next 和 random
 *
 * 方法二（原地）：O(n) 时间 O(1) 空间（追问加分）
 *   ① 每个原节点后插入拷贝节点：A → A' → B → B'
 *   ② 拷贝的 random 指向原 random 的后继：A'.random = A.random.next
 *   ③ 拆链表：还原原链 + 取出拷贝链
 */
public Node copyRandomList(Node head) {
    if (head == null) return null;
    // 哈希表法（最稳妥）
    Map<Node, Node> map = new HashMap<>();
    Node curr = head;
    while (curr != null) {                 // ① 创建新节点
        map.put(curr, new Node(curr.val));
        curr = curr.next;
    }
    curr = head;
    while (curr != null) {                 // ② 连接指针
        map.get(curr).next = map.get(curr.next);
        map.get(curr).random = map.get(curr.random);
        curr = curr.next;
    }
    return map.get(head);
}
```

---

## 6. 调试技巧与自检清单

### 6.1 链表 Bug 自检三步法

```
第 1 步：检查断链
  □ 修改 next 之前，需要暂存的后继暂存了吗？
  □ 插入操作顺序对吗（先连新节点，再接前驱）？

第 2 步：检查边界
  □ 空链表/单节点/头节点操作处理了吗？
  □ 返回的是新 head 还是 dummy.next？

第 3 步：检查环
  □ 分隔/拼接操作后是否断开尾部（largeTail.next = null）？
  □ 快慢指针循环条件防空指针了吗？
```

### 6.2 高频 Bug 速查表

| Bug | 原因 | 修复 |
|------|------|------|
| 死循环/输出环 | 拼接后未断开 | 操作后断链（如 86/143 的 `next=null`） |
| 空指针异常 | 未判 fast.next 就访问 | 循环条件 `fast != null && fast.next != null` |
| 头节点丢失 | 修改后未更新 head | 用 dummy 或最后返回 dummy.next |
| 反转部分丢失 | 未暂存 next | `ListNode next = curr.next` 先行 |
| 结果顺序错 | 插入顺序反了 | 先连后继再接前驱 |
| 输出少节点 | 边界判断 `>` 与 `>=` 混淆 | 画图核对循环边界 |

### 6.3 面试手写 Checklist

```
□ 画图推演指针变化（至少脑内完成）
□ 边界：null / 单节点 / 头尾
□ 修改链表用 dummy
□ 三指针反转暂存 next
□ 快慢指针循环条件防空指针
□ 拼接后断环
□ 迭代为主（空间 O(1)）
□ 能答出复杂度（时间/空间）
□ 手动跑 1 个用例
```

> 🎯 **核心要点**：链表面试 = **画图 + 四大技巧 + 边界意识**。206/141/21/19 是四大必背题，146 LRU 是设计题之王。记住"修改链表先 dummy、暂存后继防断链、拼接之后要断环"三条铁律，再练熟 142 的数学推导和 23 的堆合并，链表就是你的送分题型。

---

**返回总览**：[00-链表知识体系总览](00-链表知识体系总览.md)
