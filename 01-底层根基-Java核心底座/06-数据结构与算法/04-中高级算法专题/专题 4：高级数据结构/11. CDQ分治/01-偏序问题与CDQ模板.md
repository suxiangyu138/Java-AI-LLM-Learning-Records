# 01-偏序问题与 CDQ 模板

> 三维偏序的标准解法是"排序一维、分治一维、树状数组一维"。CDQ 分治的关键不是分治本身，而是保证每一对点只在它们的 LCA 分治节点处被统计一次，从而不重不漏。

---

## 目录

1. [问题模型](#1-问题模型)
2. [从二维到三维](#2-从二维到三维)
3. [算法流程](#3-算法流程)
4. [正确性说明](#4-正确性说明)
5. [完整模板](#5-完整模板)
6. [扩展到动态问题](#6-扩展到动态问题)
7. [复杂度分析](#7-复杂度分析)
8. [易错点](#8-易错点)
9. [小结](#9-小结)

---

## 1. 问题模型

**三维偏序**（洛谷 P3810 陌上花开）：

```text
给定 n 个三元组 (a_i, b_i, c_i)
对每个 i，求满足以下条件的 j 的数量：
  a_j <= a_i
  b_j <= b_i
  c_j <= c_i
  j != i
```

输出：对于每个 d，有多少个点满足"比它小的点数恰好为 d"。

数据范围：$n\le 10^5$，$a,b,c\le 2\times 10^5$。

---

## 2. 从二维到三维

### 2.1 一维偏序

排序即可，$O(n\log n)$。

### 2.2 二维偏序

按第一维排序，第二维用树状数组统计：

```text
排序 a 升序
遍历每个点：
  查询树状数组中 b' <= b 的数量
  把当前 b 加入树状数组
```

复杂度 $O(n\log n)$。

### 2.3 三维偏序

多了一维，不能简单排序：

```text
如果按 a 排序后，对每个点查 b'<=b 且 c'<=c
→ 这是二维查询，需要二维数据结构（树套树）
→ 复杂度 O(n log² n)，但空间 O(n log n) 或更高
```

CDQ 的思路：

```text
第一维 a：排序，转成"只统计左侧对右侧的贡献"
第二维 b：分治过程中归并，保证左半 b <= 右半 b
第三维 c：树状数组统计
```

---

## 3. 算法流程

```text
第 1 步：按 (a, b, c) 升序排序
第 2 步：去重，相同三元组合并为一个点，记录 cnt
第 3 步：CDQ 分治

cdq(l, r):
  if l == r: return
  mid = (l + r) / 2
  cdq(l, mid)
  cdq(mid + 1, r)

  # 此时左右两半内部已按 b 有序
  i = l, j = mid + 1
  while j <= r:
      while i <= mid && p[i].b <= p[j].b:
          树状数组.add(p[i].c, p[i].cnt)
          temp[t++] = p[i++]
      p[j].ans += 树状数组.query(p[j].c)
      temp[t++] = p[j++]

  把剩余的 p[i] 与 p[j] 放入 temp
  撤销树状数组的修改
  用 temp 覆盖 p[l..r]
```

三个关键动作：

| 动作 | 作用 |
|------|------|
| 递归左右 | 处理子问题内部的偏序 |
| 归并按 b | 让左右两半按 b 有序，便于双指针 |
| 树状数组查 c | 统计第三维 |

---

## 4. 正确性说明

### 4.1 每对只统计一次

对任意一对点 $(u,v)$，设它们在分治树中的最近公共祖先对应区间 $[l,r]$，分界点 $mid$：

```text
① u 与 v 分居 mid 两侧：
   在这一层被统计一次
   之后递归到子区间时，它们不再同属一个区间

② u 与 v 在同一侧：
   在这一层不被统计
   继续递归，直到某层分开

→ 每对恰好被统计一次
```

### 4.2 统计条件完整

在某个分治节点：

```text
左半部分的 a 一定 <= 右半部分的 a（第 1 步排序保证）
归并后左半部分的 b <= 右半部分被处理的 b（双指针保证）
树状数组查询 c' <= c（第三维保证）

→ 三个条件同时满足
```

### 4.3 为什么去重

```text
相同的三元组 (a,b,c) 会互相满足偏序
如果不去重，分治时可能把"同一个点"重复统计
去重后：
  每个 unique 点带 cnt
  统计时把 cnt 一起加入树状数组
  最终答案加上 cnt - 1（排除自身）
```

---

## 5. 完整模板

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;

public class CDQDivideAndConquer {

    private static class Node {
        int a;
        int b;
        int c;
        int count;
        int answer;

        Node(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.count = 1;
        }
    }

    private Node[] nodes;
    private Node[] temp;
    private int[] bit;
    private int maxC;

    private void add(int index, int delta) {
        for (int i = index; i <= maxC; i += i & -i) {
            bit[i] += delta;
        }
    }

    private int query(int index) {
        int result = 0;
        for (int i = index; i > 0; i -= i & -i) {
            result += bit[i];
        }
        return result;
    }

    private void cdq(int left, int right) {
        if (left >= right) {
            return;
        }
        int mid = (left + right) >>> 1;
        cdq(left, mid);
        cdq(mid + 1, right);

        int i = left;
        int j = mid + 1;
        int t = left;

        while (j <= right) {
            while (i <= mid && nodes[i].b <= nodes[j].b) {
                add(nodes[i].c, nodes[i].count);
                temp[t++] = nodes[i++];
            }
            nodes[j].answer += query(nodes[j].c);
            temp[t++] = nodes[j++];
        }

        while (i <= mid) {
            temp[t++] = nodes[i++];
        }
        while (j <= right) {
            temp[t++] = nodes[j++];
        }

        for (int x = left; x < i; x++) {
            add(nodes[x].c, -nodes[x].count);
        }
        for (int x = left; x <= right; x++) {
            nodes[x] = temp[x];
        }
    }

    public int[] solve(int[][] input, int maxC) {
        int n = input.length;
        this.maxC = maxC;
        nodes = new Node[n];
        for (int i = 0; i < n; i++) {
            nodes[i] = new Node(input[i][0], input[i][1], input[i][2]);
        }

        Arrays.sort(nodes, (x, y) -> {
            if (x.a != y.a) {
                return Integer.compare(x.a, y.a);
            }
            if (x.b != y.b) {
                return Integer.compare(x.b, y.b);
            }
            return Integer.compare(x.c, y.c);
        });

        int uniqueCount = 0;
        for (int i = 0; i < n; i++) {
            if (i > 0 && nodes[i].a == nodes[i - 1].a
                    && nodes[i].b == nodes[i - 1].b
                    && nodes[i].c == nodes[i - 1].c) {
                nodes[uniqueCount - 1].count++;
            } else {
                nodes[uniqueCount++] = nodes[i];
            }
        }

        temp = new Node[uniqueCount];
        bit = new int[maxC + 1];
        cdq(0, uniqueCount - 1);

        int[] result = new int[n];
        for (int i = 0; i < uniqueCount; i++) {
            int rank = nodes[i].answer + nodes[i].count - 1;
            result[rank] += nodes[i].count;
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer tokenizer = new StringTokenizer(reader.readLine());
        int n = Integer.parseInt(tokenizer.nextToken());
        int maxC = Integer.parseInt(tokenizer.nextToken());

        int[][] input = new int[n][3];
        for (int i = 0; i < n; i++) {
            tokenizer = new StringTokenizer(reader.readLine());
            input[i][0] = Integer.parseInt(tokenizer.nextToken());
            input[i][1] = Integer.parseInt(tokenizer.nextToken());
            input[i][2] = Integer.parseInt(tokenizer.nextToken());
        }

        int[] result = new CDQDivideAndConquer().solve(input, maxC);
        StringBuilder output = new StringBuilder();
        for (int value : result) {
            output.append(value).append('\n');
        }
        System.out.print(output);
    }
}
```

---

## 6. 扩展到动态问题

CDQ 分治的第二个用途：把"带时间维的动态问题"转成三维偏序。

### 6.1 动态逆序对

```text
问题：
  初始数组，每次删除一个位置
  求每次删除前的逆序对数量

转化：
  每个元素有三个属性：
    时间 t（何时被删除，越早越大）
    位置 pos
    值 val
  一个元素对逆序对的贡献 = 在它之前出现、位置在它前面、值比它大的元素数
  → 三维偏序
```

### 6.2 动态问题的一般模式

```text
把每个"修改/查询"看成带时间戳的点
时间就是第一维
CDQ 按时间分治
→ 每次修改只影响它之后的查询
```

这被称为 **CDQ 分治离线化**，是处理"单点修改 + 区间查询"类问题的通用手段。

---

## 7. 复杂度分析

```text
排序：O(n log n)
去重：O(n)
分治层数：O(log n)
每层归并与树状数组：O(n log C)
总复杂度：O(n log n log C)

当 C 与 n 同阶时：O(n log² n)
空间：O(n)（树状数组 + 临时数组）
```

与树套树的对比：

| 方法 | 时间 | 空间 | 实现难度 |
|------|------|------|---------|
| CDQ 分治 | O(n log² n) | O(n) | 中 |
| 树套树 | O(n log² n) | O(n log n) | 高 |
| 整体二分 | O(n log² n) | O(n) | 中高 |

> **提示**：CDQ 是离线算法，必须能一次读完所有数据。在线场景只能用树套树或 KD-Tree。

---

## 8. 易错点

```text
① 忘记去重
   相同三元组互相统计，答案偏大

② 去重后没有更新 count
   树状数组只加了 1，应该加 count

③ 归并时没有临时数组
   直接在原数组上归并会覆盖未处理元素

④ 树状数组没有撤销
   上一层的贡献残留到下一层，答案偏大

⑤ 撤销范围写成 [left, mid]
   应该撤销 [left, i)，i 是实际加入的左半元素个数

⑥ 排序顺序错误
   必须按 (a, b, c) 全序排序，否则去重不连续

⑦ maxC 开小
   树状数组越界或漏统计

⑧ 用 int 存答案
   答案可达 n，n=10^5 时 int 够用；但多组累加要用 long
```

---

## 9. 小结

| 问题 | 答案 |
|------|------|
| 三维偏序三件套 | 排序、分治、树状数组 |
| 每对统计次数 | 恰好一次（分治树 LCA 处） |
| 去重的作用 | 处理相同三元组，避免重复 |
| 复杂度 | O(n log² n) |
| 动态问题 | 时间维当作第一维，转离线偏序 |
| 与树套树 | 时间同阶，空间更小，但只能离线 |

**下一模块**：无　**返回总览**：[00-CDQ分治知识体系总览](00-CDQ分治知识体系总览.md)
