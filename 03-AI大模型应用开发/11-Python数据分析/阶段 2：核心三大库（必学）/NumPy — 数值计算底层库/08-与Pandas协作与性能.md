# 08-与 Pandas 协作与性能

> 数组与表的协作：双向转换、apply vs 向量化（性能实测）、内存瘦身、随机数——"NumPy 是引擎、Pandas 是车身——引擎强车身才快"

---

## 📚 目录

1. [双向转换](#1-双向转换)
2. [apply vs 向量化](#2-apply-vs-向量化)
3. [性能实测与纪律](#3-性能实测与纪律)
4. [内存瘦身](#4-内存瘦身)
5. [随机数](#5-随机数)
6. [练习 5 题](#6-练习-5-题)
7. [性能优化检查单与随机数场景](#7-性能优化检查单与随机数场景)
8. [本节验收](#8-本节验收)

---

## 1. 双向转换

**NumPy 与 Pandas 是"引擎与车身"**——Pandas 的每一列底层都是 numpy 数组——转换是日常操作：

```python
import pandas as pd

# numpy → pandas：数组变表（列优先心智的落地——阶段 1 04 篇）
arr = np.array([[22, "男"], [35, "女"], [41, "女"]])
df = pd.DataFrame(arr, columns=["age", "sex"])

# pandas → numpy：表变数组
df["age"].to_numpy()          # array([22, 35, 41]) —— 一列转数组（推荐）
df["age"].values              # 旧写法（功能相同——to_numpy 是新标准）
df.to_numpy()                 # 整个表转二维数组（(3, 2)）
```

**转换心智**：**"Pandas 管表、NumPy 管数组——表格分析在 Pandas、数值运算转 NumPy"**——典型流水线：**读表（read_csv）→ 清洗（Pandas）→ 数值运算（转 numpy 向量化）→ 结果回表（DataFrame）**——`to_numpy` 是新标准（`.values` 是旧写法——性能/一致性 to_numpy 更稳）。

## 2. apply vs 向量化

**Pandas 的 apply 很方便，但向量化更快**——对比三种写法：

```python
df = pd.DataFrame({"age": np.random.randint(10, 60, 100_000)})

# 写法一：apply + 函数（最慢——Python 逐行）
df["label"] = df["age"].apply(lambda a: "成年" if a >= 30 else "未成年")

# 写法二：Pandas 向量化（快——但标量判断）
df["label"] = np.where(df["age"] >= 30, "成年", "未成年")

# 写法三：NumPy 向量化（最快——纯数组）
ages = df["age"].to_numpy()
df["label"] = np.where(ages >= 30, "成年", "未成年")
```

**三写法的差距**（10 万元素实测量级）：apply 约 **百毫秒级**、Pandas 向量化约 **毫秒级**、NumPy 向量化约 **亚毫秒级**——**"能向量化就不 apply"**（01 篇纪律的 Pandas 版）。

**什么时候用 apply**（向量化做不到时）：**逐行复杂逻辑**（需要多列联合判断+业务规则）、**调用不向量化的函数**（字符串处理/text 处理）——**"apply 是最后的选项，不是默认选项"**——**先想向量化（np.where/掩码/数学函数）、再想 apply**。

## 3. 性能实测与纪律

**性能纪律三条**（数组/表的性能第一原则）：

1. **能向量化就不循环**（循环 vs 向量化 = 100 倍级——01 篇）
2. **能 NumPy 就不 Pandas 行级**（列内运算转 numpy——08 篇）
3. **先建后算不 append**（循环 append = O(n²)——zeros 占位或列表转数组——07 篇）

**定位慢代码的方法**（`%time` 测量——`../../阶段 1：前置基础/07-Jupyter Notebook 实战.md` 的魔法命令）：

```python
%time df["age"].apply(lambda a: a * 2)     # 慢在哪——先测
%time df["age"].to_numpy() * 2             # 快多少——对比
```

**性能心智**：**"优化前先测量"**（不测就优化 = 猜——`%time` 一行的事）；**"慢的通常是循环和 apply——换成向量化再看"**（90% 的 Pandas 慢代码是这个原因）；**大数组检查内存**（`nbytes`/`df.memory_usage(deep=True)`——第 4 节）。

## 4. 内存瘦身

**内存 = 类型 × 数量的账本**（04 篇的实战）：

```python
df = pd.DataFrame({"price": [29.9, 45.0, 12.5]})

df["price"].dtype                  # float64（默认——8 字节/个）
df["price"].astype("float32")      # 省一半（4 字节/个）
df["is_vip"].astype("bool")        # 布尔 1 字节
df["category"].astype("category")  # 分类类型——重复字符串省巨量（阶段 2 Pandas 篇）
df.memory_usage(deep=True)         # 每列内存体检（deep=True 含字符串实际大小）
```

**瘦身心智**：**先体检后瘦身**（`df.memory_usage(deep=True)` / `nbytes`）；**降档顺序**：float64→float32、int64→int32、字符串列→category（**重复多的字符串列 category 省 10 倍+**）；**精度优先**（价格/比例保持 float64——省内存不能牺牲正确性——04 篇精度陷阱）；**大文件场景**（read_csv 的 dtype 参数指定——`../../阶段 1：前置基础/05-数据读写与文件格式.md` 参数速查）。

## 5. 随机数

**随机数 = 数据实验的原料**（阶段 3 统计基础/抽样的预演——先掌握用法）：

```python
np.random.seed(42)                 # 固定种子——结果可复现（实验纪律！）
np.random.rand(5)                  # 5 个 [0,1) 均匀随机
np.random.randint(1, 7, 10)        # 10 个 1-6 整数（骰子——含左不含右）
np.random.normal(50, 10, 100)      # 100 个正态（均值 50、标准差 10）
np.random.choice([1, 2, 3], 5)     # 从列表随机抽 5 个（抽样）
np.random.choice([1, 2, 3], 5, p=[0.1, 0.3, 0.6])  # 按概率抽
```

**随机数心智**：**`seed` 是实验纪律**（同一 seed 每次结果相同——可复现——**"没有 seed 的实验无法复现 = 不算实验"**——阶段 3 假设检验会反复强调）；**分布函数**（normal 正态/rand 均匀——统计预演）；**choice 带概率 = 加权抽样**（p 参数——业务场景：按权重抽用户）。

## 6. 练习 5 题

1. 一列 pandas 数据转 numpy 数组的推荐写法？
2. apply 和向量化谁快？什么时候必须用 apply？
3. 性能优化三纪律是哪三条？
4. 字符串重复多的列怎么瘦身？（astype 什么）
5. `np.random.seed(42)` 为什么是实验纪律？

## 7. 性能优化检查单与随机数场景

**性能优化检查单**（数组/表变慢时逐条过）：

1. ☐ **循环还是向量化？**——`for` 逐元素 → 换整组运算（01 篇——最大头）
2. ☐ **apply 还是向量化？**——行级 apply → 换 `np.where`/掩码（08 篇第 2 节）
3. ☐ **append 循环？**——循环拼数组 → 先建后算/列表转数组（07 篇）
4. ☐ **类型合理吗？**——float64 的整数列 → int32/float32（04 篇账本）
5. ☐ **内存爆了吗？**——`nbytes`/`memory_usage` 体检 → 降档/分块（04/08 篇）
6. ☐ **测量了吗？**——`%time` 对比优化前后（**不测量不优化**——03 节纪律）

**随机数场景表**（什么时候用哪个）：

| 场景 | 函数 | 例子 |
|------|------|------|
| 均匀随机 [0,1) | `np.random.rand(n)` | 生成测试数据 |
| 整数随机 | `np.random.randint(lo, hi, n)` | 骰子/随机年龄 |
| 正态分布 | `np.random.normal(mean, std, n)` | 身高/误差模拟 |
| 从数组抽样 | `np.random.choice(arr, k)` | 随机抽用户（不重复用 replace=False） |
| 加权抽样 | `np.random.choice(arr, k, p=[权重])` | 按业务概率抽样 |

**检查单心智**：**"性能问题 90% 是'循环/apply/append'三兄弟"**——先查这三、再查类型内存——**"测量先行：不测就优化是猜"**（`%time` 一行——`../../阶段 1：前置基础/07-Jupyter Notebook 实战.md` 的魔法命令）。

**协作流程全景**（本课收口——一张流程图讲清三大库的分工）：

```text
读表（Pandas read_csv）→ 清洗（Pandas——表视角）
    → 数值运算（转 NumPy 向量化——快）
    → 统计（np.nanmean/percentile）
    → 结果回表（DataFrame 新列）
    → 可视化（Matplotlib/Seaborn——图即证据）
```

**流程心智**：**"每步用最顺手的工具"**——表操作（分组/标签）用 Pandas、数值运算用 NumPy、画图用 Matplotlib——**"工具分工不是教条，是'谁擅长什么谁干'"**——09 篇实战走一遍完整流程——**"Pandas 管表、NumPy 算数、Matplotlib 画图——三大库的第一次完整协作"**——这也是整个阶段 2 的学习目标（三大库齐活 = 数据分析主武器库建成）。

**一句话总结**：协作 = 双向转换（to_numpy 新标准）+ 性能纪律（**能向量化就不 apply、能 NumPy 就不行级、能先建就不追加——性能三兄弟 + 测量先行**）+ 内存瘦身（体检 → 降档 → category）+ 随机数（seed 可复现）——**"引擎强车身才快——三大库各管一段"**。

## 8. 本节验收

**验收动作**：① 造 10 万元素 df——三种写法打标签并 `%time` 对比记录差距；② `to_numpy` 双向转换流水线（数组算均值 → 回填 df 新列）；③ `memory_usage(deep=True)` 体检 + 一列降档对比；④ seed 实验（同 seed 两次随机数是否相同）——**"对比实测 + 双向转换 = 协作层通过"**。

> 🎯 **核心要点**：双向转换（to_numpy 新标准——流水线：Pandas 清洗 → NumPy 运算 → 回表）；apply vs 向量化（百毫秒 vs 亚毫秒——**能向量化就不 apply**——复杂逻辑/字符串才 apply）；性能三纪律（向量化/numpy 列级/先建后算——**优化前先 %time 测量**）；内存瘦身（体检 → 降档 → category——精度优先）；随机数（seed 可复现——normal/choice 带概率）——**"NumPy 是引擎、Pandas 是车身——引擎强车身才快"**。

---

**上一模块**：[07-数组操作与变形.md](./07-数组操作与变形.md) / **下一模块**：[09-阶段实战与自测.md](./09-阶段实战与自测.md)
