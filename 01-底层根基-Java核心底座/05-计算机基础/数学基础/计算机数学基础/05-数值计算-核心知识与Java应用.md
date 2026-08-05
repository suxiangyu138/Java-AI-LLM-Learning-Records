# 05 — 数值计算：核心知识与 Java 应用
> **一句话总结：** 数值计算教你如何让计算机安全、精确、高效地做数学运算——从浮点数精度的陷阱到线性方程组的求解，从数值微分到误差控制，是科学计算和金融系统的必修课。

<!-- v1.0, updated 2026-06-13, priority ★★★☆☆ -->

---

## 目录

1. [课程定位与资源](#1-课程定位与资源)
2. [浮点数与精度问题](#2-浮点数与精度问题)
3. [误差分析基础](#3-误差分析基础)
4. [线性方程组数值求解](#4-线性方程组数值求解)
5. [非线性方程求根](#5-非线性方程求根)
6. [数值微分与数值积分](#6-数值微分与数值积分)
7. [Java 中的数值计算实践](#7-java-中的数值计算实践)

---

## 1. 课程定位与资源

### 1.1 为什么 Java 后端开发者需要数值计算

| 工程场景 | 数值计算对应 | 不重视的后果 |
|----------|------------|-------------|
| **金融计算** | 浮点精度控制 | `0.1 + 0.2 ≠ 0.3` 导致资金误差 |
| **科学计算** | 迭代收敛判定 | 死循环或结果错误 |
| **推荐系统** | 矩阵分解求解 | 训练不收敛 |
| **游戏物理引擎** | 数值积分 | 物理模拟发散 |
| **机器学习推理** | 矩阵运算精度 | 模型输出误差累积 |

### 1.2 推荐资源

| 类型 | 资源 | 说明 |
|------|------|------|
| **系统** | MIT 18.330 《Introduction to Numerical Analysis》 | 计算数学入门 |
| **教材** | 《数值分析》(Kincaid & Cheney) | 理论与实践并重 |
| **Java 库** | Apache Commons Math 官方文档 | Java 数值计算工具箱 |
| **参考** | IEEE 754 浮点数标准 | 理解浮点数本质 |

---

## 2. 浮点数与精度问题

### 2.1 IEEE 754 浮点数结构

```text
float (32-bit)： 1 位符号 + 8 位指数 + 23 位尾数  → 约 7 位有效数字
double (64-bit)：1 位符号 + 11 位指数 + 52 位尾数 → 约 16 位有效数字

数值 = (-1)^sign × (1 + mantissa) × 2^(exponent - bias)

关键限制：
  - 不是所有实数都能精确表示（如 0.1）
  - 存在 ±0、±∞、NaN
  - 数值越接近 0，精度越高；越远离 0，精度越低
```

### 2.2 经典浮点陷阱

```java
// 陷阱 1：0.1 + 0.2 ≠ 0.3
System.out.println(0.1 + 0.2);        // 0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3); // false

// 原因：0.1 和 0.2 在二进制中是无限循环小数，存储时被截断

// 陷阱 2：大数吃小数
double big = 1e16;
double small = 1.0;
System.out.println(big + small == big); // true！small 被"吃掉"了

// 陷阱 3：减法的灾难性抵消
double a = 1.000000000000001;
double b = 1.000000000000000;
// a - b = 9.992...e-16  （理论上 = 1e-15，丢失了精度）
```

### 2.3 Java 中的正确做法

```java
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// 金融计算：使用 BigDecimal
BigDecimal price = new BigDecimal("0.1");
BigDecimal qty   = new BigDecimal("0.2");
BigDecimal total = price.add(qty);  // 0.3（精确！）
System.out.println(total.equals(new BigDecimal("0.3"))); // true

// 比较浮点数：使用容差（epsilon）
double EPSILON = 1e-9;
boolean approxEqual(double a, double b) {
    return Math.abs(a - b) < EPSILON;
}
// 或使用相对误差
boolean relativeEqual(double a, double b) {
    return Math.abs(a - b) <= EPSILON * Math.max(Math.abs(a), Math.abs(b));
}

// 避免大数吃小数：从小到大累加
double[] values = {1.0, 1e16, 1.0, 1e16, -2e16};
Arrays.sort(values); // 先排序，从小到大加
double sum = 0;
for (double v : values) sum += v;  // 更精确
```

---

## 3. 误差分析基础

### 3.1 误差的分类

| 类型 | 定义 | 示例 |
|------|------|------|
| **绝对误差** | `|近似值 - 真值|` | 测量值 3.14，真值 π → 绝对误差 ≈ 0.0016 |
| **相对误差** | `|近似值 - 真值| / |真值|` | 同上 → 相对误差 ≈ 0.05% |
| **截断误差** | 用有限项近似无穷级数 | `eˣ ≈ 1 + x + x²/2!`（截断 Taylor 级数） |
| **舍入误差** | 有限位表示实数 | `1/3 = 0.33333333...`（只能存有限位） |
| **传播误差** | 前一步的误差在后继计算中放大 | 病态方程组 |

### 3.2 条件数（Condition Number）

```text
定义：κ(A) = ‖A‖ · ‖A⁻¹‖

含义：
  κ ≈ 1       → 良态（well-conditioned），输入小误差 → 输出小误差
  κ >> 1      → 病态（ill-conditioned），输入小误差 → 输出可能巨大误差

AI 关联：
  神经网络中，权重矩阵的条件数过大 → 梯度不稳定
  → 用谱归一化（Spectral Normalization）控制条件数
```

### 3.3 收敛阶

| 阶数 | 名称 | 每次迭代精确位数翻 | 示例 |
|------|------|-------------------|------|
| 线性收敛 | r=1 | 固定位数增加 | 二分法 |
| 二次收敛 | r=2 | 位数翻倍 | Newton 法（靠近根时） |
| 三次收敛 | r=3 | 位数三倍 | Halley 法 |

---

## 4. 线性方程组数值求解

### 4.1 问题形式

```
求解 Ax = b

直接法（精确求解，通过有限步运算）：
  - 高斯消元法 + 回代
  - LU 分解 (A = LU)
  - Cholesky 分解（对称正定矩阵）

迭代法（近似求解，逐步逼近）：
  - Jacobi 迭代
  - Gauss-Seidel 迭代
  - 共轭梯度法（对称正定矩阵）
```

### 4.2 高斯消元法的数值问题

```text
朴素高斯消元的问题：
  [ 0.0001   1  ] [x₁]   [1]
  [   1      1  ] [x₂] = [2]
  
  若 pivot = 0.0001（很小）→ 用其消去下方 → 放大舍入误差

解决方案：选主元（Partial Pivoting）
  选当前列中绝对值最大的行作为 pivot → 减小误差放大
```

### 4.3 Java LU 分解示例

```java
import org.apache.commons.math3.linear.*;

// 构造矩阵和向量
RealMatrix A = MatrixUtils.createRealMatrix(new double[][]{
    {3, -1,  1},
    {1,  3, -1},
    {1,  1,  3}
});
RealVector b = new ArrayRealVector(new double[]{4, 2, 6});

// LU 分解求解
DecompositionSolver solver = new LUDecomposition(A).getSolver();
RealVector x = solver.solve(b);  // x ≈ [1, 1, 2]

// 检查残差
RealVector residual = A.operate(x).subtract(b);
double error = residual.getNorm();  // 应接近 0
```

### 4.4 迭代法适用场景

| 场景 | 推荐方法 |
|------|----------|
| 大型稀疏矩阵（n > 10⁴） | 共轭梯度法 (CG) |
| 非对称大型稀疏 | GMRES |
| 预处理加速 | 预处理共轭梯度 (PCG) |
| 小型 dense 矩阵（n < 1000） | 直接法（LU 分解） |

---

## 5. 非线性方程求根

### 5.1 核心方法对比

| 方法 | 公式 | 收敛阶 | 需要 | 特点 |
|------|------|--------|------|------|
| **二分法** | `c = (a+b)/2` | 线性 | 区间 [a,b] | 最慢但最稳 |
| **Newton 法** | `x_{n+1} = xₙ - f(xₙ)/f'(xₙ)` | 二次 | f 和 f' | 快但需要导数 |
| **割线法** | `x_{n+1} = xₙ - f(xₙ)(xₙ-x_{n-1})/(f(xₙ)-f(x_{n-1}))` | ≈1.618 | 两个初值 | 不用导数 |
| **不动点迭代** | `x_{n+1} = g(xₙ)` | 线性 | — | 收敛依赖 g 选择 |

### 5.2 Newton 法在机器学习中的应用

```text
Newton 法 = 梯度下降的高阶替代

参数更新：
  θ_{n+1} = θₙ - H⁻¹ · ∇L(θₙ)

其中 H 是 Hessian 矩阵（二阶导数矩阵）

优点：收敛极快（二次收敛）
缺点：H 为 n×n 矩阵（n = 参数量），求逆不可行
  → 拟 Newton 法（BFGS、L-BFGS）用低秩近似 H⁻¹
  → L-BFGS 常用于小规模 ML 问题
```

---

## 6. 数值微分与数值积分

### 6.1 数值微分

```text
前向差分：f'(x) ≈ (f(x+h) - f(x)) / h       误差 O(h)
中心差分：f'(x) ≈ (f(x+h) - f(x-h)) / (2h)   误差 O(h²)   ← 更精确

h 的选择：
  h 太大 → 截断误差大（Taylor 近似不准）
  h 太小 → 舍入误差大（f(x+h) ≈ f(x)，相减灾难性抵消）
  最优 h ≈ ε^{1/3}（中心差分）或 ε^{1/2}（前向差分），ε 是机器精度
```

### 6.2 数值积分方法

| 方法 | 公式（∫ₐᵇ f(x)dx ≈） | 误差 | 适用 |
|------|----------------------|------|------|
| **矩形法** | `h·Σ f(xᵢ)` | O(h) | 最粗糙 |
| **梯形法** | `h·(f(a)+f(b))/2 + h·Σ f(xᵢ)` | O(h²) | 通用 |
| **Simpson 法** | `h/3·[f(a)+f(b)+4Σf(奇)+2Σf(偶)]` | O(h⁴) | 光滑函数 |
| **Gauss 求积** | `Σ wᵢ·f(xᵢ)`（最优节点） | 极高 | 光滑函数 |

### 6.3 数值积分在概率中的应用

```text
连续分布的概率计算：
  P(a ≤ X ≤ b) = ∫ₐᵇ f(x)dx（通常没有解析解）

  → 用数值积分（Simpson / Gauss 求积）近似
  → 或用 Monte Carlo 方法采样近似

例如：标准正态分布的 CDF：
  Φ(z) = ∫₋∞ᶻ (1/√(2π))·e^{-t²/2} dt
  → 没有解析解，通过数值积分或有理近似计算
```

---

## 7. Java 中的数值计算实践

### 7.1 非线性方程求根（Newton 法）

```java
import java.util.function.UnaryOperator;

public class NewtonSolver {
    public static double solve(UnaryOperator<Double> f,
                               UnaryOperator<Double> df,
                               double x0, double tol, int maxIter) {
        double x = x0;
        for (int i = 0; i < maxIter; i++) {
            double fx = f.apply(x);
            double dfx = df.apply(x);
            if (Math.abs(dfx) < 1e-15) throw new RuntimeException("导数接近0");
            double dx = fx / dfx;
            x = x - dx;
            if (Math.abs(dx) < tol) return x;
        }
        throw new RuntimeException("未收敛");
    }

    public static void main(String[] args) {
        // 求 √2：解 f(x) = x² - 2 = 0
        double sqrt2 = solve(
            x -> x * x - 2,
            x -> 2 * x,
            1.0, 1e-10, 100
        );
        System.out.println(sqrt2);  // 1.4142135623730951
    }
}
```

### 7.2 Kahan 求和算法（减少累加误差）

```java
// 普通累加：误差累积 O(n)
double sum = 0;
for (double v : values) sum += v;

// Kahan 补偿求和：误差控制在 O(1)
double sum = 0;
double compensation = 0;  // 记录丢失的小量
for (double v : values) {
    double y = v - compensation;
    double t = sum + y;
    compensation = (t - sum) - y;  // 计算被丢弃的低位
    sum = t;
}
// 适用于大量浮点数累加（如梯度累积、损失求和）
```

### 7.3 Apache Commons Math 的数值积分

```java
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.*;

// Simpson 积分
UnivariateFunction f = x -> Math.sin(x);  // ∫₀^π sin(x)dx = 2

SimpsonIntegrator integrator = new SimpsonIntegrator();
double result = integrator.integrate(10000, f, 0, Math.PI);
System.out.println(result);  // ≈ 2.0

// 自适应积分（Romberg）
RombergIntegrator romberg = new RombergIntegrator();
result = romberg.integrate(100, f, 0, Math.PI);
```

---

## 关键公式速记卡

| 公式 | 用途 |
|------|------|
| `|近似值 - 真值|` | 绝对误差 |
| `x_{n+1} = xₙ - f(xₙ)/f'(xₙ)` | Newton 法求根 |
| `κ(A) = ‖A‖·‖A⁻¹‖` | 条件数（问题敏感度） |
| `f'(x) ≈ (f(x+h)-f(x-h))/(2h)` | 中心差分（O(h²)） |
| `∫ₐᵇ f(x)dx ≈ h/3·[f(a)+f(b)+4Σf(奇)+2Σf(偶)]` | Simpson 积分 |

---

## 关联文档

- [01-线性代数-核心知识与AI应用](./01-线性代数-核心知识与AI应用.md) —— 线性方程组的矩阵解法
- [02-微积分-核心知识与AI应用](./02-微积分-核心知识与AI应用.md) —— 数值微分的理论基础
- [03-概率论与数理统计-核心知识与AI应用](./03-概率论与数理统计-核心知识与AI应用.md) —— 数值积分在概率中的应用
- [00-数学基础总览](./00-数学基础总览.md) —— 返回总览
