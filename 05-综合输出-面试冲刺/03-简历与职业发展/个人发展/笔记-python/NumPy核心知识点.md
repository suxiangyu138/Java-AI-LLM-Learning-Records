# NumPy 核心知识点

## 一、基础定义
NumPy（Numerical Python） 是 Python 科学计算的底层核心库，提供高性能的多维数组对象和数学运算工具，是 Pandas、Matplotlib、Scikit-learn、深度学习框架的基础依赖。
- 核心定位：高效数值计算、矩阵运算、向量化操作
- 核心优势：速度快（C语言实现）、内存占用小、支持广播机制

## 二、核心对象：ndarray
 ndarray  是 NumPy 的核心，即N维数组，具有以下特性：
1. 数组内元素类型必须统一
2. 存储在连续内存中，计算效率极高
3. 支持向量化运算，无需循环
    关键属性
```python
import numpy as np
arr = np.array([1,2,3])
arr.ndim      # 维度
arr.shape     # 形状 (行,列)
arr.dtype     # 元素类型
arr.size      # 元素总个数
```
 
## 三、数组创建
1. 基础创建
```python
np.array([1,2,3])
np.zeros((2,3))    # 全0数组
np.ones((2,3))     # 全1数组
np.eye(3)          # 单位矩阵
np.empty((2,2))    # 空数组
```
 
2. 序列创建
```python
np.arange(0,10,2)       # 等差数组 [0,2,4,6,8]
np.linspace(0,1,5)      # 等分数组
```
 
3. 随机数组
```python
np.random.rand(2,3)     # [0,1) 均匀分布
np.random.randn(2,3)    # 标准正态分布
np.random.randint(1,10) # 随机整数
```
 
## 四、数组索引与切片
1. 一维数组
```python
arr[0]
arr[1:3]
arr[::-1]  # 反转
```
 
2. 二维数组
```python
arr[0,1]        # 第0行第1列
arr[0,:]        # 第0行
arr[:,1]        # 第1列
arr[1:3, 0:2]   # 切片
```
 
3. 布尔索引
```python
arr[arr>5]
```
 
## 五、维度操作
```python
arr.reshape(2,3)    # 重塑形状
arr.flatten()       # 展平一维
arr.T               # 转置
np.vstack([a,b])    # 垂直拼接
np.hstack([a,b])    # 水平拼接
```
 
## 六、向量化运算
核心：数组运算直接作用于每个元素，无需for循环
```python
a = np.array([1,2,3])
b = np.array([4,5,6])
a + b
a * b
a ** 2
np.sin(a)
np.sum(a)
np.mean(a)
np.max(a)
```
 
## 七、广播机制
广播：不同形状数组自动扩展为相同形状再运算
规则：
1. 维度数不同，小维度数组补1
2. 对应维度大小相等或其中一个为1
```python
a = np.array([[1,2],[3,4]])
b = np.array([10,20])
a + b
```
 
## 八、矩阵运算
```python
A @ B           # 矩阵乘法
np.dot(A,B)
A.T             # 转置
np.linalg.inv(A)# 逆矩阵
np.linalg.det(A)# 行列式
```
 
## 九、常用数学函数
```python
np.sum() / np.mean() / np.max() / np.min()
np.argmax() / np.argmin()
np.sort()
np.unique()
np.concatenate()
```
 
## 十、核心高频考点
1. ndarray 与 list 区别
    - ndarray：同类型、连续内存、向量化运算、速度快
    - list：类型任意、分散内存、循环运算、速度慢
2. 广播机制原理与使用
3. 向量化运算优势
4. 数组形状变换与拼接
5. 矩阵运算与线性代数支持
    十一、一句话总结
    NumPy 是 Python 数值计算的基石，核心就是 ndarray 多维数组 + 向量化运算 + 广播机制，用底层C语言实现换来了极致性能，是数据分析、机器学习、AI 开发的必备基础。
