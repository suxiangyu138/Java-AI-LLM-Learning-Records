# 07-时间序列 Pandas 版

> 表的"时间"：to_datetime、dt 访问器、resample 重采样、shift/diff、rolling 滚动——"时间序列 = 数据 + 日历——重采样是时间分析的第一个动作"

---

## 📚 目录

1. [时间列的准备](#1-时间列的准备)
2. [dt 访问器](#2-dt-访问器)
3. [resample 重采样](#3-resample-重采样)
4. [shift 与 diff](#4-shift-与-diff)
5. [rolling 滚动窗口](#5-rolling-滚动窗口)
6. [练习 5 题](#6-练习-5-题)
7. [时间操作速查与面试题](#7-时间操作速查与面试题)
8. [本节验收](#8-本节验收)

---

## 1. 时间列的准备

**时间分析的第一步：把时间列变成"真正的时间"**（`../NumPy — 数值计算底层库/` 的 `to_datetime` 在这里成为表操作）：

```python
df["date"] = pd.to_datetime(df["date"], errors="coerce")   # 文本 → 时间（转不了变 NaT）
df = df.sort_values("date")                                # 排序（时间序列第一动作！）
df = df.set_index("date")                                  # 设时间索引（重采样的前提）

# 体检
df.index.is_monotonic_increasing    # 索引是否递增（排序验证）
df.isna().sum()                     # NaT 缺失检查
```

**准备心智**：**`to_datetime` 认识常见格式**（`2026-08-10`/`2026/08/10` 自动判断——`errors="coerce"` 转不了变 NaT——`../../阶段 1：前置基础/05-数据读写与文件格式.md` 的 parse_dates 参数可读时直接解析）；**排序是铁律**（不排序 = 折线乱序——`../Matplotlib  Seaborn — 数据可视化/07-时间序列与子图布局.md` 的乱序坑）；**设时间索引是重采样的前提**（`set_index("date")`——resample 只能对索引做）。

## 2. dt 访问器

**`dt` = 时间列的"属性访问器"**（像 `str` 之于字符串——`df["date"].dt.xxx`）：

```python
df["year"] = df["date"].dt.year        # 年
df["month"] = df["date"].dt.month      # 月（1-12）
df["weekday"] = df["date"].dt.weekday  # 星期几（0=周一）
df["hour"] = df["date"].dt.hour        # 小时
df["is_weekend"] = df["date"].dt.weekday >= 5   # 是否周末
df["date_str"] = df["date"].dt.strftime("%Y-%m")   # 格式化成字符串（"2026-08"）
```

**dt 心智**：**`dt` 是"时间字段提取器"**（年/月/日/星期/小时——一步提取新列）；**`weekday >= 5` = 周末**（0=周一 到 6=周日——**周末判断是业务高频**）；**`strftime` 格式化**（`%Y-%m` = "2026-08"——报表分组用）；**dt 的字段是"分析的分组维度"**（`groupby(df["date"].dt.month)`——按月分组的正确姿势）。

## 3. resample 重采样

**resample = 按时间窗口聚合**（时间分析的第一个正式动作——`../Matplotlib  Seaborn — 数据可视化/07-时间序列与子图布局.md` 的聚合口径落地）：

```python
# 按日数据 → 按月聚合（时间索引前提——第 1 节）
df_month = df.resample("M").sum()          # 月度求和（销量/收入）
df_month = df.resample("ME").sum()         # 新写法（3.0 起 "M" → "ME"——月末）
df_week = df.resample("W").mean()          # 按周均值
df_hour = df.resample("h").count()         # 按小时计数

# 频率符号：D 日 / W 周 / M 月 / Q 季 / Y 年 / h 时
df.resample("Q").agg(["sum", "mean"])      # 季度多函数（agg 配合——04 篇）
df.resample("M").max()                     # 月度最大（找峰值月）
```

**resample 心智**：**"resample = groupby 的时间版"**（`resample("M")` 按月份组、`.sum()` 每组聚合——**和 groupby 同一套心智**）；**频率符号速记**（D 日/W 周/M 月/Q 季/Y 年/h 时——**Pandas 3.0 起月份用 "ME"（月末）**——老教程 "M" 会有 FutureWarning）；**聚合函数按业务语义选**（销量 sum、单价 mean、库存 last——`../NumPy — 数值计算底层库/` 的"不是默认 mean"纪律延续）。

## 4. shift 与 diff

**环比/变化率 = shift/diff 的日常**（时间分析的"对比"工具）：

```python
df["prev_sales"] = df["sales"].shift(1)        # 上一期值（环比基准）
df["mom_change"] = df["sales"] - df["sales"].shift(1)     # 环比变化量（diff 手写版）
df["mom_pct"] = df["sales"].pct_change()       # 环比变化率（-0.05 = 降 5%）
df["diff_7"] = df["sales"].diff(7)             # 与 7 天前的差（周同比）

# shift 的其他用法
df["next_sales"] = df["sales"].shift(-1)       # 下一期（负 shift）
df["label"] = df["sales"].shift(-1) > df["sales"]   # 涨跌标签（预测预演）
```

**shift/diff 心智**：**`shift(n)` = 整体平移（n 期前/后）**——环比对比的基准；**`pct_change()` = 变化率一步到位**（`(本期-上期)/上期`——"降 5%"的正式写法）；**`diff(n)` = 差分的正式函数**（`diff(7)` 周同比）；**shift 是"标签构造器"**（`shift(-1)` 取未来值做预测标签——阶段 5 机器学习预演）。

## 5. rolling 滚动窗口

**rolling = 移动窗口统计**（趋势平滑/波动分析）：

```python
df["sales_ma7"] = df["sales"].rolling(7).mean()      # 7 日移动平均（趋势线！）
df["sales_ma30"] = df["sales"].rolling(30).mean()    # 30 日均线
df["volatility"] = df["sales"].rolling(7).std()      # 7 日波动（std）
df["sales_max7"] = df["sales"].rolling(7).max()      # 7 日最高

# 窗口参数
df["sales_ma7"] = df["sales"].rolling(7, min_periods=3).mean()  # 至少 3 个值才算
```

**rolling 心智**：**`rolling(7).mean()` = "每 7 天算一次均值"**（窗口滑动——**移动平均是趋势线的主力**——`../Matplotlib  Seaborn — 数据可视化/07-时间序列与子图布局.md` 的平滑图）；**窗口函数任意聚合**（mean/std/max/sum——和 groupby 同款）；**`min_periods` 处理前几天的 NaN**（窗口不满 7 天时——默认 NaN——"数据不够就不算"）；**移动平均 vs 原始线的对比**（ma 线平滑趋势、原始线看波动——**画图时两条线都画**——07 篇可视化的规范）。

## 6. 练习 5 题

1. 时间分析的第一步是什么？（to_datetime + 排序）
2. resample 的前提是什么？（时间索引）
3. 月度聚合的频率符号？（Pandas 3.0 新写法）
4. `pct_change()` 算什么？（环比变化率）
5. `rolling(7).mean()` 是什么？（7 日移动平均）

## 7. 时间操作速查与面试题

**时间操作速查表**（本课收口——"时间八式"）：

| 需求 | 写法 |
|------|------|
| 文本转时间 | `pd.to_datetime(col, errors="coerce")` |
| 提取字段 | `df["date"].dt.year/month/weekday/hour` |
| 周末判断 | `df["date"].dt.weekday >= 5` |
| 排序 | `df.sort_values("date")` |
| 月度聚合 | `df.resample("ME").sum()`（3.0 写法） |
| 环比变化率 | `df["sales"].pct_change()` |
| 周同比 | `df["sales"].diff(7)` |
| 移动平均 | `df["sales"].rolling(7).mean()` |

**面试题**："resample 和 groupby 什么关系？"——答：**"resample 是 groupby 的时间版——把时间索引按频率分桶（日/周/月/季/年）再聚合——groupby 按列值分组、resample 按时间窗口分组——频率符号 D/W/ME/Q/Y/h——3.0 起月份用 ME（月末）"**——**答题结构：类比（时间版 groupby）→ 频率符号 → 版本注意**——"补一句'聚合函数按业务语义选（销量 sum 单价 mean）'显示业务意识"。

**练习延伸**：`shift(1)` 和 `diff(1)` 的关系？（shift 平移取上一期值、diff 是"本期减上一期"——diff = 本期 - shift——**"shift 是 diff 的原料"**——07 篇第 4 节）。

**实战场景**（本课知识在真实分析里的位置）：**"时间分析是业务汇报的主角"**——三个最经典的业务场景：**月度经营汇报**（按日流水 → `resample("ME").sum()` 月度收入 → 环比 `pct_change()` → 画折线——`../Matplotlib  Seaborn — 数据可视化/07-时间序列与子图布局.md` 的趋势图）；**活动效果评估**（活动前后对比——`diff` 看增量、`rolling(7)` 平滑看趋势拐点——**"活动效果 = 增量 + 趋势拐点"**）；**异常检测**（`rolling(7).std()` 波动率飙升 = 异常信号——"波动是异常的先行者"）——**"时间分析的产出 = 趋势 + 变化率 + 拐点"**——面试讲项目时"我按月聚合看趋势、pct_change 看环比"是标准动作。

**时间序列的坑**（07 篇第 1 节的补充）：**"时间列没解析就分析 = 字符串当时间"**（`"2026-08"` 排序是字典序——`"2026-10" < "2026-9"` 出错——**先 `to_datetime` 再排序是铁律**）；**"NaT 混进聚合"**（`to_datetime(errors="coerce")` 的 NaT 会污染 groupby——先 `isna().sum()` 体检）；**"时区/格式不一致"**（`"2026/08/10"` 和 `"2026-08-10"` 混在一列——`to_datetime` 能识别但先统一更稳——**"时间列的准备永远先于分析"**）。

**一句话记住**：时间准备（to_datetime + 排序 + 设索引）→ dt 提取字段 → resample 聚合（3.0 用 "ME"）→ shift/diff/pct_change 对比 → rolling 平滑——**"时间序列 = 数据 + 日历——重采样是时间分析的第一个动作"**。

## 8. 本节验收

**验收动作**：造 90 天销售数据——① 时间准备三步（to_datetime/排序/设索引）；② dt 提取年月周末三列；③ resample 月度聚合（sum/mean 对比）；④ shift + pct_change 算环比变化率；⑤ rolling(7) 移动平均并画图对比原始线（`../Matplotlib  Seaborn — 数据可视化/` 的折线 + 平滑线）——**"五步全做 = 时间层通过"**——**练习纪律**：时间列先体检（NaT 计数）再分析——"时间脏了全盘皆错"。

> 🎯 **核心要点**：时间准备（to_datetime coerce/排序铁律/设时间索引）；dt 访问器（年/月/星期/周末——groupby 的时间维度）；resample = groupby 时间版（频率符号速记——3.0 用 "ME"——聚合函数按业务选）；shift/diff/pct_change（环比基准/变化率/周同比——shift(-1) 构造预测标签）；rolling 移动窗口（均线平滑趋势——min_periods 处理窗口不满）——**"时间序列 = 数据 + 日历——重采样是时间分析的第一个动作"**。

---

**上一模块**：[06-apply与函数应用.md](./06-apply与函数应用.md) / **下一模块**：[08-性能与工程实践.md](./08-性能与工程实践.md)
