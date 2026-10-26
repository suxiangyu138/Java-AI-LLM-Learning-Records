# 01-LCT 原理与动态树模板

> LCT 的全部魔法在 access：它把"根到某个节点的路径"变成一条由 splay 维护的偏好路径。换根是把路径翻转，路径查询是把两端点之间的路径抽出来，连边断边只是修改父指针。理解 access 的循环，就理解了 LCT。

---

## 目录

1. [动态树问题](#1-动态树问题)
2. [实链剖分与偏好路径](#2-实链剖分与偏好路径)
3. [辅助树与双父指针](#3-辅助树与双父指针)
4. [access：核心操作](#4-access核心操作)
5. [makeroot 与 findroot](#5-makeroot-与-findroot)
6. [split / link / cut](#6-split--link--cut)
7. [完整模板](#7-完整模板)
8. [例题：动态树路径异或和](#8-例题动态树路径异或和)
9. [复杂度与易错点](#9-复杂度与易错点)
10. [小结](#10-小结)

---

## 1. 动态树问题

树链剖分能处理静态树的路径问题，但遇到以下操作就失效：

```text
① 断开一条边
② 连接两个原本不连通的点
③ 把某个点换成根
④ 在断边/连边过程中维护路径信息
```

这些操作会改变树的结构，之前算好的 dfn、size、heavy 全部失效。重新剖分的代价是 O(n)，无法承受多次操作。

LCT 的目标：

```text
支持：
  link(x, y)：连边
  cut(x, y)：断边
  makeroot(x)：换根
  split(x, y)：把路径抽出来
  路径查询/修改：在抽出的 splay 上做

复杂度：所有操作均摊 O(log n)
```

---

## 2. 实链剖分与偏好路径

LCT 用"实链剖分"代替树链剖分的"重链剖分"：

```text
树链剖分：
  按子树大小选重儿子，静态确定

LCT：
  每个节点最多选一个"实儿子"
  实儿子可以动态改变
  实边连成的路径叫"偏好路径"（preferred path）
```

偏好路径的性质：

```text
① 每条偏好路径用一棵 splay 维护
② splay 的中序遍历 = 路径上从浅到深的节点顺序
③ 一个节点最多属于一条偏好路径（在 splay 中有一个位置）
④ 不同偏好路径之间用"虚边"连接
```

```text
原树：
        1
      /   \
     2     3
    / \
   4   5

一种实链划分（实边用粗线表示）：
  路径 A：4 - 2 - 1
  路径 B：5
  路径 C：3

辅助树：
  splay A 维护 [4, 2, 1]（中序按深度）
  splay B 维护 [5]
  splay C 维护 [3]
  三条路径之间通过虚边（path-parent）连接
```

---

## 3. 辅助树与双父指针

LCT 的 `fa[]` 有两种语义，这是最容易混淆的地方：

```text
① splay 内的父指针
   x 是 splay 中的孩子，fa[x] 指向 splay 内的父亲

② path-parent（路径父指针）
   x 是某棵 splay 的根，fa[x] 指向原树上 x 的父亲
   （该父亲在另一棵 splay 中）

判断 x 是否是 splay 的根：
  fa[x] == 0
  或 ch0[fa[x]] != x 且 ch1[fa[x]] != x
```

```java
private boolean isRoot(int x) {
    int f = fa[x];
    return f == 0 || (ch0[f] != x && ch1[f] != x);
}
```

为什么需要双语义：

```text
如果只有 splay 父指针：
  断开 splay 后就找不到原树的父亲

如果只有原树父指针：
  无法在 splay 中旋转

双语义让"辅助树结构"与"原树结构"共存
```

---

## 4. access：核心操作

`access(x)` 的作用：把原树根到 $x$ 的路径变成一条偏好路径，且 $x$ 成为该 splay 的根。

```java
public void access(int x) {
    for (int y = 0; x != 0; y = x, x = fa[x]) {
        splay(x);
        ch1[x] = y;
        pushUp(x);
    }
}
```

逐行解释：

```text
① y 记录上一轮处理的节点（初始为 0）
② splay(x)：把 x 转到当前辅助树的根
③ ch1[x] = y：把 x 的右子树替换为 y
   → 右子树代表"比 x 深的路径"
   → 断开原来的右子树（变成虚边）
   → 接上 y（上一轮的路径）
④ pushUp(x)：更新 x 的聚合信息
⑤ x = fa[x]：跳到 path-parent，处理更浅的路径
```

执行过程图示：

```text
初始：
  根 1 到 x 的路径分散在多条偏好路径中

第 1 次循环（x 最深）：
  splay(x)，x 的右子树清空（x 是最深的，本来就没有右子树）

第 2 次循环（x 的 path-parent）：
  splay(父)，把父的右子树接到上一轮的 x

...

最后一次（根）：
  根成为 splay 根，整条路径在一棵 splay 中
```

> **要点**：access 是"自底向上把路径串起来"。每次把当前节点的右子树换成上一轮的节点，等价于"把这条边变成实边"。

---

## 5. makeroot 与 findroot

### 5.1 makeroot：换根

```java
public void makeroot(int x) {
    access(x);
    splay(x);
    pushRev(x);
}
```

原理：

```text
access(x) 后，根到 x 的路径在一棵 splay 中，x 是 splay 根
此时 splay 的中序是 [原根, ..., x]（浅到深）
把整棵 splay 翻转 → 中序变成 [x, ..., 原根]
→ x 成为原树的根
```

翻转用懒标记：

```java
private void pushRev(int x) {
    int temp = ch0[x];
    ch0[x] = ch1[x];
    ch1[x] = temp;
    rev[x] ^= 1;
}
```

### 5.2 findroot：找原树根

```java
public int findroot(int x) {
    access(x);
    splay(x);
    pushDown(x);
    while (ch0[x] != 0) {
        x = ch0[x];
        pushDown(x);
    }
    splay(x);
    return x;
}
```

原理：

```text
access(x) + splay(x) 后，x 所在 splay 的中序是 [根, ..., x]
根是最浅的节点 → splay 中最左边的节点
一直往左走到底即为根
```

注意每走一步都要 `pushDown`，否则翻转标记未下推，左右子树是反的。

---

## 6. split / link / cut

### 6.1 split：抽出路径

```java
public void split(int x, int y) {
    makeroot(x);
    access(y);
    splay(y);
}
```

执行后：

```text
x 是原树根
access(y) 把 x 到 y 的路径变成偏好路径
splay(y) 后，y 是这棵 splay 的根
→ y 的整棵 splay 就是 x 到 y 的路径
→ 直接读 y 的聚合值即可
```

### 6.2 link：连边

```java
public void link(int x, int y) {
    makeroot(x);
    if (findroot(y) != x) {
        fa[x] = y;
    }
}
```

```text
makeroot(x) 让 x 成为它所在树的根
如果 x 与 y 已经连通，连边会形成环 → 先判断
否则把 x 的 path-parent 设为 y
```

### 6.3 cut：断边

```java
public void cut(int x, int y) {
    makeroot(x);
    access(y);
    splay(y);
    if (ch0[y] == x && ch1[x] == 0) {
        ch0[y] = 0;
        fa[x] = 0;
        pushUp(y);
    }
}
```

判断条件：

```text
makeroot(x) + access(y) + splay(y) 后：
  x 到 y 的路径在 y 的 splay 中
  如果 x 与 y 直接相连：
    x 是 y 的左孩子（中序相邻）
    x 没有右孩子（x 是最深的左子树节点）
  → ch0[y] == x && ch1[x] == 0
```

---

## 7. 完整模板

```java
public class LinkCutTree {

    private final int[] ch0;
    private final int[] ch1;
    private final int[] fa;
    private final int[] rev;
    private final int[] value;
    private final int[] sum;
    private final int[] stack;

    public LinkCutTree(int n) {
        ch0 = new int[n + 1];
        ch1 = new int[n + 1];
        fa = new int[n + 1];
        rev = new int[n + 1];
        value = new int[n + 1];
        sum = new int[n + 1];
        stack = new int[n + 1];
    }

    public void setValue(int x, int v) {
        access(x);
        splay(x);
        value[x] = v;
        pushUp(x);
    }

    public int query(int x, int y) {
        split(x, y);
        return sum[y];
    }

    public void link(int x, int y) {
        makeroot(x);
        if (findroot(y) != x) {
            fa[x] = y;
        }
    }

    public void cut(int x, int y) {
        makeroot(x);
        access(y);
        splay(y);
        if (ch0[y] == x && ch1[x] == 0) {
            ch0[y] = 0;
            fa[x] = 0;
            pushUp(y);
        }
    }

    public void split(int x, int y) {
        makeroot(x);
        access(y);
        splay(y);
    }

    public void makeroot(int x) {
        access(x);
        splay(x);
        pushRev(x);
    }

    public int findroot(int x) {
        access(x);
        splay(x);
        pushDown(x);
        while (ch0[x] != 0) {
            x = ch0[x];
            pushDown(x);
        }
        splay(x);
        return x;
    }

    public void access(int x) {
        for (int y = 0; x != 0; y = x, x = fa[x]) {
            splay(x);
            ch1[x] = y;
            pushUp(x);
        }
    }

    private boolean isRoot(int x) {
        int f = fa[x];
        return f == 0 || (ch0[f] != x && ch1[f] != x);
    }

    private void pushUp(int x) {
        sum[x] = sum[ch0[x]] ^ sum[ch1[x]] ^ value[x];
    }

    private void pushRev(int x) {
        int temp = ch0[x];
        ch0[x] = ch1[x];
        ch1[x] = temp;
        rev[x] ^= 1;
    }

    private void pushDown(int x) {
        if (rev[x] == 0) {
            return;
        }
        if (ch0[x] != 0) {
            pushRev(ch0[x]);
        }
        if (ch1[x] != 0) {
            pushRev(ch1[x]);
        }
        rev[x] = 0;
    }

    private void rotate(int x) {
        int y = fa[x];
        int z = fa[y];
        int k = (ch1[y] == x) ? 1 : 0;
        int b = (k == 1) ? ch0[x] : ch1[x];

        if (!isRoot(y)) {
            if (ch0[z] == y) {
                ch0[z] = x;
            } else {
                ch1[z] = x;
            }
        }
        fa[x] = z;
        if (k == 1) {
            ch0[x] = y;
        } else {
            ch1[x] = y;
        }
        fa[y] = x;
        if (k == 1) {
            ch1[y] = b;
        } else {
            ch0[y] = b;
        }
        if (b != 0) {
            fa[b] = y;
        }
        pushUp(y);
        pushUp(x);
    }

    private void splay(int x) {
        int top = 0;
        int y = x;
        stack[top++] = y;
        while (!isRoot(y)) {
            y = fa[y];
            stack[top++] = y;
        }
        while (top > 0) {
            pushDown(stack[--top]);
        }

        while (!isRoot(x)) {
            int y = fa[x];
            int z = fa[y];
            if (!isRoot(y)) {
                boolean zigZig = (ch0[y] == x) == (ch0[z] == y);
                if (zigZig) {
                    rotate(y);
                } else {
                    rotate(x);
                }
            }
            rotate(x);
        }
    }
}
```

---

## 8. 例题：动态树路径异或和

维护 $n$ 个点的森林，支持：

```text
0 x y：查询 x 到 y 路径的异或和
1 x y：连边 x-y（保证不形成环）
2 x y：断边 x-y
3 x v：把 x 的点权改为 v
```

代码：

```java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Main {

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer tokenizer = new StringTokenizer(reader.readLine());
        int n = Integer.parseInt(tokenizer.nextToken());
        int m = Integer.parseInt(tokenizer.nextToken());

        LinkCutTree lct = new LinkCutTree(n);

        tokenizer = new StringTokenizer(reader.readLine());
        for (int i = 1; i <= n; i++) {
            lct.setValue(i, Integer.parseInt(tokenizer.nextToken()));
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < m; i++) {
            tokenizer = new StringTokenizer(reader.readLine());
            int op = Integer.parseInt(tokenizer.nextToken());
            int x = Integer.parseInt(tokenizer.nextToken());
            int y = Integer.parseInt(tokenizer.nextToken());

            switch (op) {
                case 0:
                    output.append(lct.query(x, y)).append('\n');
                    break;
                case 1:
                    lct.link(x, y);
                    break;
                case 2:
                    lct.cut(x, y);
                    break;
                case 3:
                    lct.setValue(x, y);
                    break;
                default:
                    break;
            }
        }
        System.out.print(output);
    }
}
```

---

## 9. 复杂度与易错点

### 9.1 复杂度

```text
access：均摊 O(log n)
splay：均摊 O(log n)
makeroot / findroot / split / link / cut：均摊 O(log n)

总复杂度：O((n + m) log n)
空间：O(n)
```

### 9.2 易错点

```text
① isRoot 判断错误
   把 splay 根误判为普通节点 → 旋转越界、死循环

② splay 前没有 pushDown 整条祖先链
   翻转标记未下推 → 左右子树方向错误

③ rotate 中忘记更新 path-parent
   z 不是 splay 根时要更新 z 的孩子指针

④ findroot 没有在最后 splay
   均摊复杂度退化

⑤ cut 判断条件写成 ch1[x] != 0
   应判断 ch1[x] == 0（x 是路径最深节点）

⑥ link 前没有 findroot 判连通
   → 形成环，后续操作错误

⑦ 下标从 0 开始
   LCT 用 0 表示空指针，节点编号必须从 1 开始

⑧ 多组数据没有重置数组
   → 残留指针导致错误
```

> **警告**：LCT 的常数很大，Java 实现尤其明显。$n,m\le 10^5$ 时建议用快速输入（`BufferedReader` + `StringTokenizer`），并避免在循环中创建对象。$10^6$ 级别优先考虑 C++ 或改用其他方法。

---

## 10. 小结

| 问题 | 答案 |
|------|------|
| LCT 解决什么 | 动态树的连边、断边、换根、路径查询 |
| 核心操作 | access |
| access 做了什么 | 把根到 x 的路径变成一条 splay |
| makeroot | access + splay + 翻转 |
| findroot | access + 一直往左 |
| split | makeroot(x) + access(y) + splay(y) |
| link | makeroot(x) 后挂到 y |
| cut | 判断直接相邻后断开 |
| 复杂度 | 均摊 O(log n) |

**下一模块**：[00-莫队算法知识体系总览](../10.%20莫队算法/00-莫队算法知识体系总览.md)　**返回总览**：[00-Link-Cut Tree知识体系总览](00-Link-Cut Tree知识体系总览.md)
