# Python数据分析 面试宝典
> 基于课程大纲全面覆盖面试高频考点，涵盖 NumPy/Pandas/Matplotlib/Seaborn/EDA 全栈

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

> 本部分覆盖面试前 5 分钟高频快问快答，每题控制在 30 秒内完成。

### 1.1 NumPy 基础

**Q1：ndarray 与 Python list 的区别是什么？**

| 维度 | ndarray | Python list |
|------|---------|-------------|
| 存储 | 连续内存块，同质（homogeneous） | 指针数组，异质（heterogeneous） |
| 性能 | 向量化操作，C 级别循环 | Python 级别循环，速度慢 10-50x |
| 方法 | 支持 broadcasting、ufunc、fancy indexing | 仅支持基本索引切片 |
| 内存 | 更紧凑，无对象头开销 | 每个元素是 PyObject，开销大 |

> 💡 **面试关键词**："同质连续内存"、"向量化"、"broadcasting"、"避免 Python 循环"

**Q2：ndarray 的 shape 和 dtype 是什么？**

```python
import numpy as np
arr = np.array([[1, 2, 3], [4, 5, 6]])
print(arr.shape)   # (2, 3) — 2行3列
print(arr.dtype)   # int64 — 自动推断为 64 位整数
print(arr.ndim)    # 2 — 维度数
print(arr.size)    # 6 — 元素总数
```

**Q3：什么是 broadcasting？请举例说明。**

Broadcasting 是 NumPy 对不同 shape 数组进行算术运算的机制。规则：从尾维度开始比较，维度为 1 或缺失则广播。

```python
a = np.array([[1, 2, 3], [4, 5, 6]])  # shape (2, 3)
b = np.array([10, 20, 30])            # shape (3,) → 广播为 (2, 3)
print(a + b)
# [[11 22 33]
#  [14 25 36]]
```

> ⚠️ **常见错误**：`(3,)` 与 `(3, 1)` 不同，`(3,)` 只能与 `(1, 3)` 或 `(n, 3)` 广播，不能与 `(3, n)` 直接广播。

**Q4：什么是 ufunc（universal function）？**

ufunc 是对 ndarray 每个元素进行操作的向量化函数，执行速度远超 Python 循环。

```python
arr = np.array([1, 4, 9, 16])
print(np.sqrt(arr))   # [1. 2. 3. 4.] — ufunc
print(np.exp(arr))    # 指数函数
print(np.add.reduce([1, 2, 3]))  # 6 — reduce 操作
```

### 1.2 Pandas 基础

**Q5：Series 和 DataFrame 的区别？**

| 特性 | Series | DataFrame |
|------|--------|-----------|
| 结构 | 一维带标签数组 | 二维带标签表格 |
| 类比 | 带索引的 list / 单列 | 多个 Series 共享 row index |
| 创建 | `pd.Series([1,2,3], index=['a','b','c'])` | `pd.DataFrame({'col': [1,2]})` |
| 单列访问 | — | `df['col']` 返回 Series |

**Q6：iloc 与 loc 的区别？**

```python
df = pd.DataFrame({'A': [1, 2, 3], 'B': [4, 5, 6]}, index=['x', 'y', 'z'])
print(df.loc['y'])      # 按标签索引 → B  5
print(df.iloc[1])       # 按位置索引 → B  5
print(df.loc['x':'y'])  # 标签切片，包含终点
print(df.iloc[0:2])     # 位置切片，不包含终点
```

> 💡 **记忆口诀**：`loc` = label-based，`iloc` = integer position-based。

**Q7：groupby 的常见用法？**

```python
df = pd.DataFrame({'dept': ['A', 'A', 'B', 'B'], 'val': [1, 2, 3, 4]})
# 单列聚合
print(df.groupby('dept')['val'].agg(['sum', 'mean', 'count']))
# 多列聚合
print(df.groupby('dept').agg(sum_val=('val', 'sum'), mean_val=('val', 'mean')))
# transform — 保留原行数
df['dept_mean'] = df.groupby('dept')['val'].transform('mean')
```

**Q8：merge、join、concat 的区别？**

```python
# merge — SQL 风格合并
pd.merge(left, right, on='key', how='inner')  # inner/left/right/outer

# join — 基于索引合并
left.join(right, how='left')  # 等价于 merge on index

# concat — 行或列拼接
pd.concat([df1, df2], axis=0)  # 行拼接
pd.concat([df1, df2], axis=1)  # 列拼接
```

> 🎯 **总结**：合并用 `merge`（类似 SQL JOIN），索引对齐用 `join`，简单拼接用 `concat`。

**Q9：pivot_table 的用途？**

```python
df = pd.DataFrame({
    'date': ['Jan','Jan','Feb','Feb'],
    'city': ['BJ','SH','BJ','SH'],
    'sales': [100, 200, 150, 250]
})
pt = df.pivot_table(values='sales', index='date', columns='city',
                    aggfunc='mean', fill_value=0)
```

### 1.3 数据可视化基础

**Q10：Matplotlib 与 Seaborn 的关系？**

- **Matplotlib**：底层绘图库，灵活但代码量大，需手动控制几乎所有元素
- **Seaborn**：基于 Matplotlib 的高级封装，内置统计功能，默认样式美观

```python
import matplotlib.pyplot as plt
import seaborn as sns

# Matplotlib 方式
plt.figure(figsize=(10, 6))
plt.scatter(df['x'], df['y'], alpha=0.5)
plt.xlabel('X Label')
plt.ylabel('Y Label')
plt.title('Title')
plt.show()

# Seaborn 方式 — 一行搞定
sns.scatterplot(data=df, x='x', y='y', hue='category')
```

> 💡 **面试要点**：Seaborn 的 `hue`（颜色分组）、`col`/`row`（分面）是快速探索数据的利器。

### 1.4 数据清洗基础

**Q11：常见缺失值处理策略有哪些？**

| 策略 | 适用场景 | 代码 |
|------|----------|------|
| 删除行 | 缺失率低（<5%），随机缺失 | `df.dropna()` |
| 均值填充 | 数值型，近似正态分布 | `df.fillna(df.mean())` |
| 中位数填充 | 数值型，有异常值 | `df.fillna(df.median())` |
| 众数填充 | 类别型 | `df.fillna(df.mode()[0])` |
| 前向/后向填充 | 时间序列 | `df.fillna(method='ffill')` |
| 插值 | 时间序列，有趋势 | `df.interpolate()` |

**Q12：重复数据处理方法？**

```python
# 完全重复行删除
df.drop_duplicates(inplace=True)

# 指定列去重（保留最后一条）
df.drop_duplicates(subset=['user_id', 'timestamp'], keep='last')

# 先排序再去重（保留最新）
df.sort_values('timestamp', ascending=False).drop_duplicates('user_id')
```

### 1.5 时间序列基础

**Q13：resample 和 rolling 的区别？**

| 方法 | 用途 | 输出形状 | 示例 |
|------|------|----------|------|
| `resample` | 改变时间频率（降采样/升采样） | 新频率的索引 | `df.resample('M').mean()` |
| `rolling` | 滑动窗口计算，频率不变 | 同原形状 | `df.rolling(7).mean()` |

```python
ts = pd.date_range('2026-01-01', periods=365, freq='D')
df = pd.DataFrame({'value': np.random.randn(365)}, index=ts)

# 降采样：日 → 月
monthly = df.resample('M').mean()

# 滚动窗口：7 天移动平均
df['ma_7'] = df['value'].rolling(window=7).mean()
```

---

## 二、深度原理剖析

> 本部分考察对核心机制的深层理解，通常以"讲一下 X 的原理"形式出现。

### 2.1 NumPy 内存布局与性能

**Q14：ndarray 的内存布局是怎样的？**

ndarray 由两部分组成：
1. **元数据（metadata）**：shape、dtype、strides、ndim 等
2. **数据缓冲区（data buffer）**：C 风格（row-major / C-contiguous）连续内存块

```python
arr = np.array([[1, 2, 3], [4, 5, 6]])
print(arr.strides)  # (24, 8) — 行步长 24 字节，列步长 8 字节
# 内存排列：1 → 2 → 3 → 4 → 5 → 6 (C-contiguous)
```

- **C-contiguous（C 顺序）**：行优先，`arr.flags.c_contiguous`
- **Fortran-contiguous（F 顺序）**：列优先，`arr.flags.f_contiguous`

**Q15：什么是 fancy indexing？为何比切片慢？**

```python
arr = np.arange(10)
indices = [1, 3, 5, 7]
print(arr[indices])  # [1 3 5 7] — fancy indexing
```

> ⚠️ **性能提示**：Fancy indexing 返回**新数组的拷贝**（而非 view），因为索引不连续导致无法用 strides 描述。大数据量时优先使用切片或 `take()`。

**字节面试真题**：解释 `arr[1:5]` 与 `arr[[1,2,3,4]]` 的区别。
- 前者返回 **view**（共享内存），修改会影响原数组
- 后者返回 **copy**（独立内存），修改不影响原数组

### 2.2 Pandas 核心机制

**Q16：Pandas 的 index 为何重要？**

Index 是 Pandas 的"灵魂"。它提供了：
- **自动对齐**（alignment）：运算时按 index 匹配，无需手动排序
- **快速查找**：基于 hash table 的 O(1) 索引
- **轴标签**：支持非整数标签，如时间戳、字符串

```python
s1 = pd.Series([1, 2, 3], index=['a', 'b', 'c'])
s2 = pd.Series([10, 20], index=['b', 'a'])
print(s1 + s2)
# a    21.0
# b    12.0
# c     NaN   ← 不存在的标签自动 NaN
```

> 💡 **面试点**：Pandas 的自动对齐既是优点（免去手动排序）也是坑点（可能意外产生 NaN）。

**Q17：groupby 的 split-apply-combine 机制？**

GroupBy 执行三阶段：
1. **Split**：根据分组键将 DataFrame 拆分为若干组
2. **Apply**：对每组应用函数（内置聚合 / transform / filter / 自定义函数）
3. **Combine**：将结果合并为最终输出

```python
df.groupby('dept')['salary'].agg('mean')  # split → apply mean → combine

# 更复杂：自定义 apply
df.groupby('dept').apply(lambda g: g.sort_values('salary').head(2))
```

**阿里面试真题**：`agg`、`transform`、`apply` 的区别？

| 方法 | 返回形状 | 典型用途 | 性能 |
|------|----------|----------|------|
| `agg` | 每组一行 | 组内均值、求和、计数 | 快（C 优化） |
| `transform` | 与原 DataFrame 相同行数 | 组内标准化、填充缺失值 | 快（C 优化） |
| `apply` | 取决于函数 | 复杂分组逻辑 | 慢（Python 循环） |

```python
# transform 示例：组内标准化
df['z_score'] = df.groupby('dept')['salary'].transform(
    lambda x: (x - x.mean()) / x.std()
)
```

**Q18：merge 的底层实现原理？**

Pandas merge 底层分为三步：
1. **hash 构建**：对 join key 建立 hash table（类似 Python dict）
2. **匹配**：遍历右表，在左表的 hash table 中查找匹配行
3. **拼接**：按匹配结果拼接行，决定内/外/左/右连接

复杂度为 O(n + m)，远优于嵌套循环 O(n*m)。

### 2.3 数据透视与 reshape

**Q19：pivot_table 与 groupby + unstack 的关系？**

```python
# pivot_table 是 groupby + unstack 的语法糖
pt = df.pivot_table(values='sales', index='region',
                    columns='product', aggfunc='sum')

# 等效实现
pt_equiv = df.groupby(['region', 'product'])['sales'].sum().unstack()
```

> 🎯 **核心区别**：`pivot_table` 自动处理缺失值（fill_value）、支持 margins（总计行/列）、聚合函数更灵活。

### 2.4 可视化原理

**Q20：Matplotlib 的 Figure 和 Axes 架构？**

Matplotlib 采用层次结构：
- **Figure**：最外层容器，类似画布
- **Axes**：实际绘图区域，一个 Figure 可包含多个 Axes
- **Axis**：坐标轴（x 轴 / y 轴），包含刻度（ticks）、标签等

```python
fig, axes = plt.subplots(2, 2, figsize=(12, 8))  # 2x2 子图
axes[0, 0].plot(x, y)       # 访问特定子图
axes[1, 0].scatter(x, y)    # 另一个子图
plt.tight_layout()
plt.show()
```

---

## 三、实战场景题

> 本部分模拟真实业务场景，考察将数据分析工具应用到具体问题的能力。

### 3.1 数据清洗实战

**Q21：如何处理含有大量缺失值的脏数据？**

```python
# 场景：用户画像表，缺失率 60%
df = pd.DataFrame({
    'age': [25, None, None, 35, None],
    'gender': ['M', None, 'F', None, None],
    'income': [10000, 15000, None, None, None],
    'label': [1, 0, 1, 0, 1]
})

# Step 1: 分析缺失模式
missing_pct = df.isnull().mean().sort_values(ascending=False)
# Step 2: 缺失 > 50% 的列直接删除
df.drop(columns=missing_pct[missing_pct > 0.5].index, inplace=True)
# Step 3: 数值列用中位数填充
df['age'].fillna(df['age'].median(), inplace=True)
# Step 4: 类别列用众数填充
df['gender'].fillna(df['gender'].mode()[0], inplace=True)
# Step 5: 剩余行若还有缺失则删除
df.dropna(inplace=True)
```

**Q22：异常值检测的完整流程？**

```python
# Z-score 方法（正态分布假设）
from scipy import stats
z_scores = np.abs(stats.zscore(df['value']))
df_no_outlier = df[z_scores < 3]  # 3σ 法则

# IQR 方法（稳健，不要求正态分布）
Q1, Q3 = df['value'].quantile([0.25, 0.75])
IQR = Q3 - Q1
lower, upper = Q1 - 1.5 * IQR, Q3 + 1.5 * IQR
df_no_outlier = df[(df['value'] >= lower) & (df['value'] <= upper)]

# 异常值标记
df['is_outlier'] = (df['value'] < lower) | (df['value'] > upper)
df['capped_value'] = df['value'].clip(lower, upper)  # 截尾处理
```

### 3.2 分组聚合实战

**Q23：计算每个部门薪资最高的 3 名员工？**

```python
df = pd.DataFrame({
    'dept': ['A','A','A','B','B','B','B'],
    'name': ['a','b','c','d','e','f','g'],
    'salary': [5000, 8000, 6000, 7000, 9000, 8000, 10000]
})

# 方法 1：sort + groupby head
top3 = df.sort_values('salary', ascending=False).groupby('dept').head(3)

# 方法 2：rank 过滤
df['rank'] = df.groupby('dept')['salary'].rank(method='dense', ascending=False)
top3 = df[df['rank'] <= 3].drop('rank', axis=1)

# 方法 3：nlargest
top3 = df.groupby('dept')['salary'].nlargest(3).reset_index(level=1, drop=True)
```

### 3.3 时间序列实战

**Q24：用户登录数据按周统计活跃度？**

```python
logs = pd.DataFrame({
    'user_id': [1, 1, 2, 3, 2, 4],
    'login_date': ['2026-01-01', '2026-01-02', '2026-01-01',
                   '2026-01-08', '2026-01-09', '2026-01-15']
})
logs['login_date'] = pd.to_datetime(logs['login_date'])

# 按周统计唯一用户数
weekly = logs.resample('W-Mon', on='login_date')['user_id'].nunique()

# 同比上周变化率
weekly_pct = weekly.pct_change() * 100

# 滚动 4 周平均活跃度
weekly_ma4 = weekly.rolling(4).mean()
```

**腾讯面试真题**：计算用户留存率（次日/7日/30日留存）。

```python
def retention_analysis(login_records):
    # login_records: DataFrame with user_id, login_date
    first_login = login_records.groupby('user_id')['login_date'].min().reset_index()
    first_login.columns = ['user_id', 'first_date']

    merged = login_records.merge(first_login, on='user_id')
    merged['day_diff'] = (merged['login_date'] - merged['first_date']).dt.days

    retention = {}
    for day in [1, 7, 30]:
        retained = merged[merged['day_diff'] == day]['user_id'].nunique()
        total = first_login.shape[0]
        retention[f'day_{day}'] = retained / total

    return retention
```

### 3.4 统计分析实战

**Q25：A/B 测试结果分析？**

```python
from scipy import stats

def ab_test_analysis(control_group, treatment_group, metric):
    """
    控制组与实验组的指标差异检验
    """
    control = control_group[metric].dropna()
    treatment = treatment_group[metric].dropna()

    # 描述性统计
    print(f"Control: mean={control.mean():.2f}, std={control.std():.2f}, n={len(control)}")
    print(f"Treatment: mean={treatment.mean():.2f}, std={treatment.std():.2f}, n={len(treatment)}")

    # 独立样本 t 检验
    t_stat, p_value = stats.ttest_ind(control, treatment)
    print(f"t-statistic={t_stat:.4f}, p-value={p_value:.4f}")

    # 效应量 Cohen's d
    pooled_std = np.sqrt((control.std()**2 + treatment.std()**2) / 2)
    cohens_d = (treatment.mean() - control.mean()) / pooled_std
    print(f"Cohen's d={cohens_d:.4f}")

    result = {
        'significant': p_value < 0.05,
        'p_value': p_value,
        'effect_size': cohens_d,
        'direction': '提升' if treatment.mean() > control.mean() else '下降'
    }
    return result
```

---

## 四、手写代码题

> 本部分模拟面试现场手写代码，注意代码规范和注释。

### 4.1 数据加载与概览

```python
# 题目：加载 CSV 并输出完整的数据质量报告
def data_quality_report(filepath):
    df = pd.read_csv(filepath)
    report = {
        'shape': df.shape,
        'columns': list(df.columns),
        'dtypes': df.dtypes.astype(str).to_dict(),
        'missing_count': df.isnull().sum().to_dict(),
        'missing_pct': (df.isnull().sum() / len(df) * 100).round(2).to_dict(),
        'duplicate_rows': df.duplicated().sum(),
        'unique_counts': {col: df[col].nunique() for col in df.columns}
    }
    return report

# 题目：分块读取大文件
def process_large_csv(filepath, chunk_size=10000):
    chunks = []
    for chunk in pd.read_csv(filepath, chunksize=chunk_size):
        # 逐块处理
        chunk = chunk.dropna(subset=['key_column'])
        chunks.append(chunk)
    return pd.concat(chunks, ignore_index=True)
```

### 4.2 特征工程

```python
# 题目：对类别特征进行编码
# One-Hot Encoding
df_encoded = pd.get_dummies(df, columns=['city'], prefix='city')

# Label Encoding
from sklearn.preprocessing import LabelEncoder
le = LabelEncoder()
df['city_code'] = le.fit_transform(df['city'])

# 题目：数值分箱
df['age_group'] = pd.cut(df['age'], bins=[0, 18, 35, 60, 120],
                         labels=['少年', '青年', '中年', '老年'])
df['amount_bin'] = pd.qcut(df['amount'], q=4,
                           labels=['Q1', 'Q2', 'Q3', 'Q4'])

# 题目：标准化与归一化
def z_score_normalize(data):
    return (data - data.mean()) / data.std()

def min_max_normalize(data):
    return (data - data.min()) / (data.max() - data.min())
```

### 4.3 数据合并与转换

```python
# 题目：实现 Excel VLOOKUP 功能
def vlookup(left_df, right_df, on_key, value_col, how='left'):
    return left_df.merge(right_df[[on_key, value_col]], on=on_key, how=how)

# 题目：宽表转长表（melt）
wide = pd.DataFrame({
    'date': ['Jan', 'Feb'],
    'BJ': [100, 150],
    'SH': [200, 250]
})
long = wide.melt(id_vars=['date'], var_name='city', value_name='sales')
#   date city  sales
# 0  Jan   BJ    100
# 1  Feb   BJ    150
# 2  Jan   SH    200
# 3  Feb   SH    250

# 长表转宽表（pivot）
wide_again = long.pivot(index='date', columns='city', values='sales')
```

### 4.4 EDA 工具函数

```python
# 题目：一键 EDA 函数
import matplotlib.pyplot as plt
import seaborn as sns

def quick_eda(df):
    """快速探索性数据分析"""
    print("=" * 50)
    print("1. 数据概览")
    print("=" * 50)
    print(f"Shape: {df.shape}")
    print(df.info())
    print(f"\n缺失值:\n{df.isnull().sum()}")

    print("\n" + "=" * 50)
    print("2. 数值列统计")
    print("=" * 50)
    numeric_cols = df.select_dtypes(include=['int64', 'float64']).columns
    print(df[numeric_cols].describe())

    print("\n" + "=" * 50)
    print("3. 类别列分布")
    print("=" * 50)
    cat_cols = df.select_dtypes(include=['object', 'category']).columns
    for col in cat_cols[:5]:  # 最多显示 5 列
        print(f"\n{col}:\n{df[col].value_counts().head(10)}")

    # 4. 相关性热力图
    if len(numeric_cols) > 1:
        plt.figure(figsize=(10, 8))
        sns.heatmap(df[numeric_cols].corr(), annot=True, cmap='RdBu_r', center=0)
        plt.title('Correlation Heatmap')
        plt.tight_layout()
        plt.show()
```

---

## 五、系统设计题

> 本部分考察数据分析工程化能力，适合 3-5 年经验及以上。

### 5.1 自动化数据报表系统

**需求**：每日从多个数据源采集业务指标，生成报表邮件发送给各部门。

**设计方案**：

```
┌─────────┐   ┌──────────┐   ┌───────────┐   ┌──────────┐
│  MySQL   │   │  API 采集 │   │   日志    │   │  CSV导入 │
│ 业务库   │   │ 第三方   │   │ 文件(OSS) │   │ 手动上传 │
└────┬────┘   └────┬─────┘   └─────┬─────┘   └────┬─────┘
     └──────────────┴──────────────┴──────────────┘
                        │
               ┌────────▼────────┐
               │  数据清洗管道    │
               │  (missing fill, │
               │   remove dup,   │
               │   type convert) │
               └────────┬────────┘
                        │
               ┌────────▼────────┐
               │  指标计算引擎    │
               │  groupby agg    │
               │  pivot_table    │
               │  time series    │
               └────────┬────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐
   │ 日报PDF  │  │ DashBoard │  │ Slack通知│
   │邮件发送  │  │ Grafana   │  │ 异常告警 │
   └──────────┘  └──────────┘  └──────────┘
```

> 💡 **关键设计决策**：
> 1. 使用 `pd.read_sql()` + `chunksize` 避免 OOM
> 2. 数据清洗逻辑封装为 Pipeline（参考 sklearn Pipeline 思路）
> 3. 指标计算用 SQL 还是 Pandas？小数据量用 Pandas 更灵活，大数据量下沉到数据库

### 5.2 A/B 测试分析系统

**需求**：产品上线新功能，分实验组和对照组，分析核心指标是否显著提升。

**分析流程**：

```python
# 1. 样本量计算（实验前）
from statsmodels.stats.power import TTestIndPower
power = TTestIndPower()
n = power.solve_power(effect_size=0.2, power=0.8, alpha=0.05)
print(f"每组所需样本量: {int(n)}")

# 2. 数据采集：user_id, group (control/treatment), metric_1, metric_2

# 3. 异常值过滤 + 用户去重

# 4. 指标计算
agg = df.groupby('group')[['metric_1', 'metric_2']].agg(['mean', 'std', 'count'])

# 5. 显著性检验
control = df[df['group'] == 'control']['metric_1']
treatment = df[df['group'] == 'treatment']['metric_1']
t_stat, p_value = stats.ttest_ind(control, treatment)

# 6. 效应量
def cohens_d(a, b):
    return (a.mean() - b.mean()) / np.sqrt((a.var() + b.var()) / 2)

# 7. 可视化
sns.boxplot(data=df, x='group', y='metric_1')
```

> ⚠️ **常见面试追问**：
> - 多重比较如何修正？（Bonferroni / Benjamini-Hochberg）
> - 指标不服从正态分布怎么办？（Mann-Whitney U 检验）
> - 如何避免辛普森悖论？（分层分析 + 卡方检验）

### 5.3 指标监控与异常检测系统

```python
# 基于 3σ 的实时异常检测
def detect_anomaly_3sigma(new_value, history_series, n_sigma=3):
    mean = history_series.mean()
    std = history_series.std()
    return abs(new_value - mean) > n_sigma * std

# 基于移动平均的平滑检测
def moving_avg_detect(series, window=7, threshold=2.0):
    ma = series.rolling(window=window).mean()
    std = series.rolling(window=window).std()
    upper = ma + threshold * std
    lower = ma - threshold * std
    return (series > upper) | (series < lower)
```

**美团面试真题**：设计外卖订单量异常监控系统。

```python
class OrderMonitor:
    def __init__(self, history_days=30):
        self.history_days = history_days

    def update(self, daily_orders: pd.Series):
        # daily_orders: 每日订单量时间序列
        history = daily_orders.tail(self.history_days)

        # 同比检测（与上周同日对比）
        weekly_diff = daily_orders.diff(7).tail(7)
        if (weekly_diff.abs() > history.std() * 3).any():
            return "异常：较上周同期波动过大"

        # 环比检测（与前一日对比）
        daily_diff = daily_orders.diff(1).tail(1)
        if abs(daily_diff.iloc[0]) > history.std() * 2:
            return "异常：较昨日波动过大"

        return "正常"
```

---

## 六、常见坑点与最佳实践

> ⚠️ 本部分汇总面试中高频出现的踩坑知识点。

### 6.1 常见坑点速查表

| 坑点 | 错误示例 | 正确做法 | 说明 |
|------|----------|----------|------|
| 链式赋值警告 | `df[df.a>0]['b'] = 1` | `df.loc[df.a>0, 'b'] = 1` | 链式赋值可能操作的是 copy 而非 view |
| 修改原数据意外 | 用切片后修改 | 明确 `.copy()` | `df2 = df1[['a']].copy()` |
| NaN 比较 | `df[df.a == np.nan]` | `df[df.a.isna()]` | `np.nan != np.nan`（恒为 False） |
| 浮点数精度 | `0.1 + 0.2 == 0.3` | `np.isclose(0.1+0.2, 0.3)` | 浮点二进制表示误差 |
| 索引非唯一 | 用 `loc` 获取单行 | `df.index.is_unique` 先检查 | 非唯一索引返回 DataFrame 而非 Series |
| dtype 转换陷阱 | `df['col'].astype('int')` | `pd.to_numeric(df['col'], errors='coerce')` | 含 NaN 时 int 转换会失败 |
| 时间列未解析 | `pd.read_csv()` 默认读为 string | `parse_dates=['col']` 参数 | 时间操作需要 datetime 类型 |
| groupby 忘记 reset_index | 返回 MultiIndex | `.reset_index()` | 不利于后续 merge 操作 |
| axis 混淆 | `df.drop('col', axis=1)` 写错 | axis=0 删除行，axis=1 删除列 | 记忆：axis=1 是水平方向（列） |
| inplace 不生效 | 部分方法不支持 inplace | 检查文档 | 推荐赋值方式：`df = df.method()` |

### 6.2 最佳实践清单

1. **优先向量化操作**：避免 `df.apply()` 和显式 Python 循环，用 NumPy ufunc 或 Pandas 内置函数
2. **善用 `inplace=False`**：大部分 Pandas 方法返回新对象，链式调用更安全
3. **大数据集优化**：
   - 只读需要的列：`pd.read_csv('f.csv', usecols=['a', 'b'])`
   - 指定列为 categorical：`pd.read_csv('f.csv', dtype={'city': 'category'})`
   - 逐块读取 + 增量处理：`chunksize=10000`
4. **代码复用**：将数据清洗逻辑封装为函数或类（Pipeline 模式）
5. **版本锁定**：生产环境固定 `pandas`、`numpy` 版本
6. **善用 `.pipe()` 方法构建可读性的数据处理链**：

```python
def remove_outliers(df, col, factor=1.5):
    Q1, Q3 = df[col].quantile([0.25, 0.75])
    iqr = Q3 - Q1
    return df[(df[col] >= Q1 - factor*iqr) & (df[col] <= Q3 + factor*iqr)]

def fill_missing(df, col, strategy='median'):
    if strategy == 'median':
        return df.fillna({col: df[col].median()})
    return df

# pipe 链式调用
clean_df = (df
    .pipe(fill_missing, 'age')
    .pipe(remove_outliers, 'salary')
    .drop_duplicates())
```

> 💡 **Pandas 2.0 新特性**：支持 PyArrow 后端（`pd.set_option('mode.dtype_backend', 'pyarrow')`），大幅提升性能和内存效率，支持缺失值友好的 `int64` 类型。

---

## 七、面试回答模板（Top 5）

> 🎯 以下模板帮助你在面试中组织语言，清晰有逻辑地表达。

### 模板 1：如何优化 Pandas 代码性能？

"Pandas 性能优化我从三个层面入手：
1. **语法层**：优先使用向量化操作代替 `apply` 和循环，比如用 `df['a'] + df['b']` 而非 `df.apply(lambda r: r.a + r.b, axis=1)`
2. **数据类型层**：将字符串列转为 `category` 类型可大幅节省内存；数值列尽量用合适精度的 `dtype`（如 `int32` 而非 `int64`）
3. **IO 层**：读文件时指定 `usecols` 和 `dtype`，大文件用 `chunksize` 分块读取，写文件用 parquet 格式代替 CSV"

### 模板 2：如何处理不平衡数据？

"针对不平衡数据，我会从数据、算法、评价指标三个角度处理：
1. **数据层面**：欠采样多数类、过采样少数类（SMOTE）
2. **算法层面**：调整 class_weight 参数、使用异常检测算法替代分类
3. **评价指标**：不用准确率，改用 AUC-ROC、Precision-Recall、F1-score。同时用分层采样保持验证集的类别分布"

### 模板 3：EDA（探索性数据分析）的流程？

"我的 EDA 流程分五步：
1. **数据概览**：shape、info、describe、head/tail、isnull
2. **单变量分析**：直方图（数值型）、柱状图（类别型）、箱线图（异常值）
3. **双变量分析**：散点图（数值 vs 数值）、分组箱线图（类别 vs 数值）、堆叠柱状图（类别 vs 类别）
4. **多变量分析**：相关性热力图、pairplot、分面散点图
5. **结论总结**：列出关键发现、潜在问题、待验证假设"

### 模板 4：缺失值处理的完整思路？

"处理缺失值前先回答三个问题：缺失机制是什么（完全随机 MCAR / 随机 MAR / 非随机 MNAR）？缺失率多少？业务含义是否允许删除？

- 缺失率 < 5%，可直接删除
- 缺失率 5%-30%，均值/中位数填充（数值型）、众数填充（类别型）、ffill/bfill（时间序列）
- 缺失率 > 30%，建立预测模型（用其他列预测缺失值），或保留缺失标记作为新特征

最后一定要验证处理后数据的分布是否与原分布一致。"

### 模板 5：Python 数据分析项目如何落地到生产？

"我会保证代码的以下特性：
1. **可复现**：用 `seed` 固定随机种子，`requirements.txt` 锁定依赖版本
2. **模块化**：`data_loader.py`、`cleaner.py`、`feature_engineer.py`、`analyzer.py`、`visualizer.py`
3. **可测试**：每个模块写单元测试（pytest），特别是数据清洗逻辑
4. **可监控**：输出数据质量报告（行数、缺失率、分布变化），设置异常告警
5. **可追溯**：记录每次运行的时间、参数、数据版本"

---

## 八、快速查漏补缺 Checklist

> ✅ 面试前对照检查，确保无知识盲区。

### NumPy 必会

| 知识点 | 掌握程度 | 复习要点 |
|--------|----------|----------|
| ndarray 创建 | □ 已掌握 | `np.array/np.zeros/np.ones/np.arange/np.linspace/np.random` |
| shape / dtype / strides | □ 已掌握 | 三者关系和内存布局 |
| broadcasting 规则 | □ 已掌握 | 从尾维度开始，维度为 1 则广播 |
| fancy indexing vs slice | □ 已掌握 | copy vs view，性能差异 |
| ufunc | □ 已掌握 | `np.add.reduce/np.sqrt/np.exp` |
| 条件筛选 | □ 已掌握 | 布尔索引、`np.where` |
| 矩阵运算 | □ 已掌握 | `np.dot/@/np.matmul/np.linalg` |
| 随机数生成 | □ 已掌握 | `seed/normal/uniform/choice/shuffle` |

### Pandas 必会

| 知识点 | 掌握程度 | 复习要点 |
|--------|----------|----------|
| Series vs DataFrame | □ 已掌握 | 一维 vs 二维，index 对齐 |
| iloc vs loc | □ 已掌握 | 位置 vs 标签，切片含不含终点 |
| groupby + agg/transform/apply | □ 已掌握 | 三者的返回形状和性能差异 |
| merge/join/concat | □ 已掌握 | SQL join vs 索引对齐 vs 简单拼接 |
| pivot_table / melt | □ 已掌握 | 宽表 vs 长表互转 |
| 时间序列 resample / rolling | □ 已掌握 | 降采样/升采样/滚动窗口 |
| 数据清洗 | □ 已掌握 | dropna/fillna/drop_duplicates/astype |
| 链式赋值陷阱 | □ 已掌握 | 使用 `.loc` 代替链式 |
| 大数据处理 | □ 已掌握 | chunksize / categorical dtype / pyarrow |

### 可视化必会

| 知识点 | 掌握程度 | 复习要点 |
|--------|----------|----------|
| Matplotlib Figure + Axes | □ 已掌握 | 子图创建与布局 |
| Seaborn 常用图 | □ 已掌握 | `sns.histplot/sns.boxplot/sns.scatterplot/sns.heatmap/sns.pairplot` |
| 美学映射 | □ 已掌握 | hue/size/style/col 参数 |
| 图形定制 | □ 已掌握 | title/xlabel/ylabel/legend/tight_layout |

### 统计与分析必会

| 知识点 | 掌握程度 | 复习要点 |
|--------|----------|----------|
| 描述性统计 | □ 已掌握 | mean/median/std/IQR/skew/kurtosis |
| 假设检验 | □ 已掌握 | t-test / Mann-Whitney / chi-square |
| 相关性分析 | □ 已掌握 | Pearson / Spearman / 热力图 |
| 概率分布 | □ 已掌握 | 正态/均匀/二项/泊松 |
| A/B 测试要点 | □ 已掌握 | 样本量/显著性/效应量/多重比较/辛普森悖论 |
| EDA 五步法 | □ 已掌握 | 概览→单变量→双变量→多变量→结论 |

### 大厂高频真题速查

| 公司 | 真题 | 考察点 |
|------|------|--------|
| 字节跳动 | `arr[1:5]` vs `arr[[1,2,3,4]]` 区别 | view vs copy，strides |
| 阿里巴巴 | `agg` / `transform` / `apply` 区别 | groupby 机制，返回形状 |
| 腾讯 | 用户留存率计算 | 时间序列，groupby 应用 |
| 美团 | 外卖订单异常检测系统 | 时间序列，监控设计 |

---

> 🎯 **面试必胜策略**：
> 1. **基础题**（一、六）：快速准确回答，展示扎实基础
> 2. **原理题**（二）：展示深度理解，提到源码机制和设计思想
> 3. **实战题**（三、四）：手写代码要边写边解释，先说思路再动笔
> 4. **系统题**（五）：展示架构能力，从数据流到部署全链路思考
> 5. 面试结束时主动提出："我是否可以反问您几个关于团队技术栈的问题？"

---

*文档版本：v1.0 | 最后更新：2026-07-22 | 覆盖面试题数：70+ | 总行数：420+*
