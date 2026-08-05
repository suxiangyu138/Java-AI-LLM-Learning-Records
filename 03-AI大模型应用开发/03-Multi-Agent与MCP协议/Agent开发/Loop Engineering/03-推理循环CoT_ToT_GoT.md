# 03 - 推理循环：CoT / ToT / GoT

> 🎯 推理循环是 LLM "深度思考"的结构化实现——CoT 线性推理 → ToT 树形探索 → GoT 图状推理。理解这三种循环结构，就理解了"让 LLM 变聪明"的核心技法

---

## 目录

1. [CoT 思维链：线性推理循环](#1-cot-思维链线性推理循环)
2. [ToT 思维树：分支探索循环](#2-tot-思维树分支探索循环)
3. [GoT 思维图：网状推理循环](#3-got-思维图网状推理循环)
4. [循环工程对比](#4-循环工程对比)

---

## 1. CoT 思维链：线性推理循环

### 1.1 核心循环

```text
CoT (Chain-of-Thought) = 线性链式推理

循环结构：
  Step 1 → Step 2 → Step 3 → ... → Final Answer

每一步的输出 = 下一步的输入
无回溯，无分支，一条路走到答案
```

### 1.2 循环实现

```python
class CoTReasoner:
    def reason(self, problem: str) -> str:
        prompt = f"""请一步步推理解决以下问题。每步标注 Step N。

问题：{problem}

Step 1: 理解问题
Step 2: 列出已知条件
Step 3: 选择解题方法
Step 4: 逐步计算
Step 5: 验证答案
Step 6: 写出最终答案
"""
        context = [{"role": "user", "content": prompt}]

        # 循环生成每步推理
        for step in range(1, 7):
            response = self.llm.chat(context)
            context.append({"role": "assistant", "content": response})

            # 如果已得到答案，提前终止
            if "最终答案" in response or "Answer:" in response:
                return self._extract_answer(response)

            # 提示继续
            context.append({
                "role": "user",
                "content": f"继续 Step {step + 1}"
            })

        return response
```

### 1.3 CoT 变体

| 变体 | 差异 | 适用 |
|------|------|------|
| **Zero-shot CoT** | "Let's think step by step" | 通用，省 Token |
| **Few-shot CoT** | 给 2-3 个带推理的示例 | 特定领域 |
| **Auto-CoT** | 让 LLM 自动生成示例 | 动态适应 |
| **Self-Consistency** | 跑 N 次 CoT → 投票选最佳 | 提高准确率 |
| **Least-to-Most** | 先解子问题 → 再解主问题 | 复杂任务 |

## 2. ToT 思维树：分支探索循环

### 2.1 核心循环

```text
ToT (Tree of Thoughts) = 树形分支探索

每一层有多条路径，每条路径独立推理
→ 每层评估所有路径 → 保留最优的 K 条 → 继续扩展

循环结构：
  根问题
  ├── 思路A₁ ── 思路A₂ ── 思路A₃ → 答案A
  ├── 思路B₁ ── 思路B₂ → 放弃（评估太差）
  └── 思路C₁ ── 思路C₂ ── 思路C₃ → 答案C（最优！）
```

### 2.2 BFS/DFS 搜索策略

```python
class ToTReasoner:
    def reason(self, problem: str, breadth=3, depth=4):
        root = ThoughtNode(content=problem, depth=0)
        queue = [root]

        for d in range(depth):
            next_level = []

            # 1. 对当前层每个节点 → 生成 breadth 个子思路
            for node in queue:
                children = self._generate_thoughts(node, breadth)
                node.children = children
                next_level.extend(children)

            # 2. 评估所有子思路 → 保留最好的 breadth 个
            next_level = self._evaluate_and_prune(next_level, k=breadth)

            queue = next_level

        # 3. 从保留的最优路径回朔答案
        best_path = self._extract_best_path(root)
        return self._synthesize_answer(best_path)

    def _generate_thoughts(self, node, n):
        prompt = f"""当前推理状态：{node.content}

请提出 {n} 种不同的下一步推理方向。
每种标注 [Direction N]: ..."""
        response = self.llm.chat(prompt)
        return self._parse_directions(response, n)

    def _evaluate_and_prune(self, nodes, k):
        for node in nodes:
            score = self.llm.chat(
                f"评估这条推理路径的可行性(1-10分)：{node.content}"
            )
            node.score = int(score)
        return sorted(nodes, key=lambda n: n.score, reverse=True)[:k]
```

### 2.3 ToT 的关键参数

| 参数 | 含义 | 小值 | 大值 |
|------|------|:---:|:---:|
| **breadth (b)** | 每层保留几条路径 | 省 Token，可能漏解 | 全面，Token 消耗大 |
| **depth (d)** | 最多推理几层 | 适合简单问题 | 适合需要深度的推理 |
| **k** | 每层保留几个节点 | 快速收敛 | 更多探索 |

## 3. GoT 思维图：网状推理循环

### 3.1 核心突破

```text
GoT (Graph of Thoughts) = 把推理变成有向图

CoT: A → B → C → D          (线性)
ToT: A → (B₁,B₂) → (C₁,C₂)  (树形)
GoT: A → B₁ → C₁             (图！可以合并、聚合)
         B₂ → C₂ → D₁
              ↓       ↓
              E₁ ←────┘      (任意方向连接)

核心操作：
  → Aggregate (聚合)：多个思路合成一个
  → Refine   (精炼)：一个思路迭代优化
  → Generate (生成)：从一个思路产生多个
```

### 3.2 GoT 的图操作

```python
class GoTReasoner:
    def aggregate(self, thoughts: list[Thought]) -> Thought:
        """聚合：N 个独立推理 → 1 个综合结论"""
        prompt = f"将以下 {len(thoughts)} 个推理结果综合为一个结论：\n"
        for t in thoughts:
            prompt += f"- {t.content}\n"
        result = self.llm.chat(prompt)
        return Thought(content=result, sources=thoughts)

    def refine(self, thought: Thought, iterations=3) -> Thought:
        """精炼：一个思路迭代改进"""
        current = thought
        for i in range(iterations):
            result = self.llm.chat(
                f"改进以下推理（第{i+1}轮）：\n{current.content}\n"
                "指出弱点并给出改进版本："
            )
            current = Thought(content=result)
        return current

    def reason_with_graph(self, problem: str) -> str:
        # 1. 生成初始思路
        roots = self._generate_initial_thoughts(problem, n=3)

        # 2. 独立推理
        paths = [self._chain_reason(r, depth=3) for r in roots]

        # 3. 聚合
        merged = self.aggregate(paths)

        # 4. 精炼
        final = self.refine(merged, iterations=2)

        return final.content
```

## 4. 循环工程对比

| 维度 | CoT | ToT | GoT |
|------|:---:|:---:|:---:|
| 结构 | 线性链 | 树 | 有向图 |
| 分支 | 无 | ✅ | ✅ |
| 回溯 | 无 | 有限 | ✅ |
| 聚合 | 无 | 无 | ✅ N→1 |
| LLM 调用次数 | O(d) | O(b^d) | O(n·d) |
| 适用 | 简单推理 | 需要探索的 | 复杂综合问题 |
| Token 成本 | ★ | ★★★ | ★★★★ |

```text
选择指南：
├── 简单推理题 → CoT（够用且便宜）
├── 需要多方案探索 → ToT（创意写作、策略规划）
├── 需要综合多源头信息 → GoT（研究报告、论文综述）
└── 数学证明 → CoT + Self-Consistency（多次采样投票）
```

## 核心要点回顾

- CoT = 线性推理，便宜高效，90% 场景够用
- ToT = BFS/DFS 树搜索，广度 b 和深度 d 是核心调参
- GoT = 图推理，支持聚合(N→1)和精炼(1→1')，最灵活也最贵
- Token 成本：CoT << ToT < GoT，按需选复杂度
- 实用建议：默认 CoT → 不够加 Self-Consistency → 还不够上 ToT

## 参考资料

1. CoT 论文 (Wei et al., 2022)
2. ToT 论文 (Yao et al., 2023)
3. GoT 论文 (Besta et al., 2023)
