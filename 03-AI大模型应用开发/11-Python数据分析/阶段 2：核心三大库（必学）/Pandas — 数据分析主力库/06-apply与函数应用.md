# 06-apply 与函数应用

> 表的"变形"：map/apply/applymap 三兄弟、新建列的条件逻辑、向量化 vs apply——"apply 是最后选项不是默认选项——先想向量化"

---

## 📚 目录

1. [函数应用三兄弟](#1-函数应用三兄弟)
2. [map 映射](#2-map-映射)
3. [apply 行/列级](#3-apply-行列级)
4. [新建列的条件逻辑](#4-新建列的条件逻辑)
5. [向量化 vs apply](#5-向量化-vs-apply)
6. [练习 5 题](#6-练习-5-题)
7. [函数应用速查与面试题](#7-函数应用速查与面试题)
8. [本节验收](#8-本节验收)

---

## 1. 函数应用三兄弟

**给"表"应用函数的三个工具**——按作用范围选：

| 函数 | 作用对象 | 用途 | 例子 |
|------|---------|------|------|
| `Series.map(字典或函数)` | 一列（逐个值） | 值替换/映射 | `df["sex"].map({"男": "M", "女": "F"})` |
| `Series.apply(函数)` | 一列（逐个值） | 复杂逻辑逐值 | `df["age"].apply(is_adult)` |
| `DataFrame.apply(函数)` | 整行或整列 | 行/列级计算 | `df.apply(每行算总分, axis=1)` |
| `applymap(函数)` | 每个单元格 | 全表逐格 | 格式化（3.0 起推荐 `map` 替代） |

**心智**：**"map 是查表替换、apply 是函数应用"**——**map 适合"值 → 值"的固定映射**（字典——sex 转代码）；**apply 适合"值 → 派生值"的逻辑**（函数——年龄 → 是否成年）；**applymap 全表逐格少用**（慢——格式化场景才碰）。

## 2. map 映射

```python
# 字典映射（值替换——最高频）
df["sex_code"] = df["sex"].map({"男": 1, "女": 0})

# 函数映射（等价于 apply 的轻量版）
df["sex_code"] = df["sex"].map(lambda x: 1 if x == "男" else 0)

# 缺失值兜底（map 的 NaN 处理）
df["sex_code"] = df["sex"].map({"男": 1, "女": 0}).fillna(-1)   # 未映射到的 → NaN → 填 -1

# 反向映射
code_to_name = {v: k for k, v in {"男": 1, "女": 0}.items()}
```

**map 心智**：**map = 查表**（"字典里找到就换、找不到变 NaN"——**注意：未映射的值变 NaN**——用 `fillna` 兜底）；**map 也可以吃函数**（但函数逻辑用 apply 更清晰——**"映射用 map、逻辑用 apply"**——分工原则）；**反向映射**（字典推导式 `{v: k for k, v in ...}`——编码 ↔ 解码）。

## 3. apply 行/列级

```python
# Series.apply：逐值应用（最常用）
def age_group(a):
    if pd.isna(a): return "未知"
    if a < 18: return "少年"
    if a < 40: return "中年"
    return "老年"

df["age_group"] = df["age"].apply(age_group)

# DataFrame.apply：整行计算（axis=1——行级）
df["total"] = df[["math", "english"]].apply(lambda row: row["math"] + row["english"], axis=1)

# DataFrame.apply：整列计算（axis=0 默认）
df[["math", "english"]].apply(lambda col: col.max() - col.min())
```

**apply 心智**：**Series.apply 逐值**（`df["列"].apply(函数)`——最常用——"每格调一次函数"）；**DataFrame.apply 行级要写 `axis=1`**（`lambda row: ...`——"每行调一次"——**不写 axis=1 是对列算**——第一易错点）；**apply 的函数要健壮**（`pd.isna` 判断缺失——NaN 进函数会出错——**"写 apply 函数先处理 NaN"**）。

## 4. 新建列的条件逻辑

**新建列的三个层级**（由快到慢——**优先上层**）：

```python
# 1. 向量化（最快——优先！）
df["is_adult"] = df["age"] >= 18                    # 布尔列
df["age_group"] = np.where(df["age"] < 18, "少年",
                  np.where(df["age"] < 40, "中年", "老年"))   # 三态嵌套
df["fare_x2"] = df["fare"] * 2                      # 算术

# 2. loc 赋值（条件替换——有条件的写值）
df.loc[df["age"] > 60, "vip"] = "是"                # 只改满足条件的行
df["vip"] = "否"
df.loc[df["age"] > 60, "vip"] = "是"                # 先默认后定向

# 3. apply（最后选项——复杂逐行逻辑）
df["desc"] = df.apply(lambda r: f"{r['age']}岁/{r['sex']}", axis=1)
```

**条件新建列心智**：**"先想向量化（np.where/算术/布尔）、再想 loc 赋值、最后 apply"**——**np.where 嵌套 = 多分支打标签的向量化姿势**（`../NumPy — 数值计算底层库/05-布尔掩码与条件选择.md` 的 where 表版）；**loc 赋值是"先默认后定向"模式**（全表给默认值 → loc 定向改——比逐分支简单）；**apply 只用于"向量化表达不了"的**（字符串拼接/多列复杂规则）。

## 5. 向量化 vs apply

**性能对比**（`../NumPy — 数值计算底层库/08-与Pandas协作与性能.md` 的实测在表的场景）：

```python
df = pd.DataFrame({"age": np.random.randint(10, 60, 100_000)})

# apply（慢——Python 逐行）
df["g1"] = df["age"].apply(lambda a: "adult" if a >= 30 else "young")

# np.where 向量化（快 100 倍级）
df["g2"] = np.where(df["age"] >= 30, "adult", "young")

# 验证结果一致
(df["g1"] == df["g2"]).all()     # True
```

**选择心智**：**"能向量化就不 apply"**（Pandas 的第一性能纪律——NumPy 08 篇的表版）；**apply 的合法场景**：字符串处理（`str` 访问器没有的）、多列联合复杂规则、调用外部函数（不能向量化的库）——**"apply 是最后选项不是默认选项"**；**写 apply 前先自问三句**：有没有现成的向量化函数？能不能用 np.where？能不能用 str 访问器？——**"三问没有答案才 apply"**。

## 6. 练习 5 题

1. map 和 apply 的分工？（映射 vs 逻辑）
2. map 未映射到的值会变成什么？
3. DataFrame.apply 行级计算要写什么参数？
4. 三态打标签的向量化姿势是什么？（np.where 嵌套）
5. 写 apply 前自问哪三句？

## 7. 函数应用速查与面试题

**函数应用速查表**（本课收口——"新建列五式"）：

| 需求 | 写法 | 层级 |
|------|------|:---:|
| 算术新建列 | `df["x2"] = df["a"] * 2` | 向量化（最快） |
| 布尔新建列 | `df["flag"] = df["a"] >= 30` | 向量化 |
| 多分支打标 | `np.where(嵌套)` | 向量化 |
| 条件改值 | `df.loc[条件, "列"] = 值` | loc 定向 |
| 值映射 | `df["code"] = df["sex"].map({"男": 1})` | map 查表 |
| 复杂逻辑 | `df["col"].apply(函数)` | apply（最后） |

**面试题**："apply 和向量化的区别？什么时候用 apply？"——答：**"向量化是 C 级整列运算（快 100 倍级）、apply 是 Python 逐行调函数（慢但灵活）——能用向量化（np.where/算术/布尔）就不用 apply——apply 只用于向量化表达不了的需求：多列联合复杂规则、外部函数调用、字符串拼接——写 apply 前先自问'有没有向量化写法'"**——**答题结构：性能差异 → 选择标准 → 合法场景**——"答出'先问三句'显示工程思维"。

**练习延伸**：`map` 和 `apply` 谁快？（map 略快——查表优化——但语义不同：map 映射/apply 逻辑——**"性能差异小、语义差异大——按语义选"**）。

**实战场景**（本课知识在真实分析里的位置）：**"新建列是特征工程的起点"**（阶段 5 机器学习的预演）：**业务打标**（`np.where` 年龄分档/价格区间——"高/中/低客单价"——后续分组/画像的分组维度）；**编码转换**（`map` 性别/地区转代码——模型输入准备）；**衍生指标**（`df["客单价"] = df["销售额"] / df["订单数"]`——**"派生列是分析师造数据的方式"**——比原始列更贴近业务语义）；**描述拼接**（apply 生成"XX岁/XX地区"描述列——报表/报告用）——**"新建列 = 把业务规则写进表里"**——面试讲项目时"我构造了客单价、年龄分层等特征"是加分项。

**函数应用的错误观**（本课最后的心态校准）：**"不是所有逻辑都要写函数"**——能用 `str` 访问器（`df["col"].str.replace`）就不写 apply、能用算术就不写函数、能用 `np.where` 就不写多分支——**"函数是最后的手段，不是第一反应"**——**"写得越少的代码越快越不易错——向量化优先是 Pandas 的第一审美"**。

**一句话记住**：map 查表映射/apply 逐值逻辑/applymap 全表——**"新建列三层级：向量化最快 → loc 定向 → apply 最后"**——apply 写前自问三句（现成函数？np.where？str 访问器？）——**"能向量化就不 apply——apply 是最后选项不是默认选项"**。

## 8. 本节验收

**验收动作**：① map 做 sex 编码（含未映射 NaN 兜底）；② apply 函数做年龄四档（含 NaN 处理）；③ 新建列三层级各一次（np.where 打标/loc 先默认后定向/apply 拼接描述）；④ 10 万元素 apply vs np.where 计时对比——**"三工具 + 层级 + 实测 = 函数层通过"**——**练习纪律**：apply 函数先处理 NaN——"NaN 进函数 = 报错或错值"。

> 🎯 **核心要点**：三兄弟（map 查表替换/apply 逐值逻辑/applymap 全表）；map 未映射变 NaN（fillna 兜底）；DataFrame.apply 行级要 axis=1（不写是对列算）；新建列三层级（向量化最快 → loc 定向 → apply 最后——np.where 嵌套打标）；**"能向量化就不 apply——apply 是最后选项不是默认选项"**（写前自问三句）。

---

**上一模块**：[05-合并与连接.md](./05-合并与连接.md) / **下一模块**：[07-时间序列Pandas版.md](./07-时间序列Pandas版.md)
