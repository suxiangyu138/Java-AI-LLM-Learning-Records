# 机器学习练习 (Machine Learning Practice)

> Python 机器学习入门到进阶实践项目，覆盖经典算法与深度学习

## 项目概述

机器学习综合练习项目，从经典监督学习/无监督学习算法开始，逐步进阶到深度学习。使用 scikit-learn、TensorFlow/PyTorch 等主流框架，通过实际数据集训练模型，掌握数据预处理、特征工程、模型训练、评估调优的完整流程。

## 技术栈

| 技术 | 说明 |
|------|------|
| Python | 3.10+ |
| NumPy | 数值计算 |
| Pandas | 数据处理与分析 |
| Matplotlib / Seaborn | 数据可视化 |
| Scikit-learn | 经典机器学习算法 |
| TensorFlow / PyTorch | 深度学习框架 |

## 学习路线

### 第一阶段：基础与数据预处理
- NumPy 矩阵运算
- Pandas 数据清洗（缺失值、异常值、编码）
- 数据可视化（折线图、散点图、热力图）
- 特征缩放（标准化、归一化）

### 第二阶段：经典算法
- 线性回归 / 逻辑回归
- 决策树 / 随机森林
- SVM 支持向量机
- KNN / K-Means 聚类
- 朴素贝叶斯

### 第三阶段：模型评估与优化
- 交叉验证
- 混淆矩阵、ROC 曲线
- 网格搜索与随机搜索
- 过拟合与正则化

### 第四阶段：深度学习
- 神经网络基础（感知机 → 多层感知机）
- CNN 卷积神经网络（图像分类）
- RNN/LSTM（序列数据）
- Transformer 基础

## 项目结构

```
机器学习练习/
├── data/                       # 数据集目录
├── notebooks/                  # Jupyter Notebook 练习
│   ├── 01_numpy_basics.ipynb
│   ├── 02_pandas_cleaning.ipynb
│   ├── 03_linear_regression.ipynb
│   ├── 04_decision_tree.ipynb
│   └── 05_neural_network.ipynb
├── src/                        # Python 脚本
│   ├── preprocessing.py        # 预处理工具
│   ├── train_model.py          # 模型训练脚本
│   └── evaluate.py             # 评估脚本
├── models/                     # 保存的模型文件
├── requirements.txt
├── main.py
└── README.md
```

## 快速开始

```bash
# 创建虚拟环境
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # Mac/Linux

# 安装依赖
pip install -r requirements.txt

# 启动 Jupyter
jupyter notebook
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| 数据预处理 | 清洗、编码、标准化、特征工程 |
| 监督学习 | 分类与回归问题 |
| 无监督学习 | 聚类与降维 |
| 损失函数 | MSE、Cross-Entropy、Hinge Loss |
| 梯度下降 | SGD、Adam、学习率调度 |
| 过拟合/欠拟合 | 正则化、Dropout、Early Stopping |
| 模型评估 | 准确率、精确率、召回率、F1-Score |

## 推荐数据集

- Iris（鸢尾花分类 - 入门）
- Boston Housing（房价预测）
- MNIST（手写数字识别）
- CIFAR-10（图像分类）
- Titanic（Kaggle 入门赛）

## 注意事项

- 建议使用 Jupyter Notebook 进行探索性分析
- 数据集建议从 Kaggle 或 UCI ML Repository 下载
- 深度学习建议有 GPU 环境，或使用 Google Colab 免费 GPU
- 养成记录实验参数和结果的习惯（MLflow 或 TensorBoard）
