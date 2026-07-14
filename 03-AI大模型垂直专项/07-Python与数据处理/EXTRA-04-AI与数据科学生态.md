# 阶段 4：AI 与数据科学生态

> **目标**：掌握 Python AI/数据方向核心库，能独立完成数据分析和模型推理
> **核心**：NumPy → Pandas → 可视化 → PyTorch → FastAPI → 爬虫（按需）
> **策略**：聚焦 AI/数据方向，Web 和爬虫仅做了解

---

## 📌 章节定位

对你而言，这是 Python 学习路线中**最有价值的部分**。Java 在 AI 生态上相比 Python 差距明显——NumPy、Pandas、PyTorch、LangChain 等库是 Python 的护城河。

本章目标是让你能独立在 Python 侧完成：数据处理 → 模型推理 → 服务暴露 → 通过 HTTP 被 Java 后端调用。

---

## 🎯 核心章节

### 第一部分：数据科学三件套

### 1. NumPy —— 数值计算基石

```python
import numpy as np

# --- 数组创建 ---
arr = np.array([1, 2, 3, 4, 5])
zeros = np.zeros((3, 4))              # 3×4 全零矩阵
ones = np.ones((2, 3))                # 2×3 全一矩阵
identity = np.eye(3)                  # 3×3 单位矩阵
random_arr = np.random.randn(100)     # 100 个标准正态分布随机数
range_arr = np.arange(0, 10, 0.5)     # [0, 0.5, 1.0, ..., 9.5]
linspace = np.linspace(0, 1, 11)      # [0, 0.1, 0.2, ..., 1.0]

# --- 数组属性 ---
arr.shape                             # (5,)  维度
arr.dtype                             # 数据类型
arr.ndim                              # 维度数
arr.size                              # 元素总数

# --- 索引与切片（⭐ 功能远超 Java 数组）---
matrix = np.array([[1, 2, 3], [4, 5, 6], [7, 8, 9]])

# 基本索引
print(matrix[1, 2])                   # 6
print(matrix[0, :])                   # [1, 2, 3]     （第 0 行）
print(matrix[:, 1])                   # [2, 5, 8]     （第 1 列）

# 布尔索引（⭐ 核心技巧）
arr = np.array([1, 3, 5, 7, 9])
mask = arr > 4
print(arr[mask])                      # [5, 7, 9]
print(arr[arr % 3 == 0])              # [3, 9]

# 花式索引
indices = [0, 2, 4]
print(arr[indices])                   # [1, 5, 9]

# --- 向量化运算（⭐ 不用写 for 循环）---
a = np.array([1, 2, 3])
b = np.array([4, 5, 6])

print(a + b)                          # [5, 7, 9]     （逐元素）
print(a * b)                          # [4, 10, 18]   （逐元素，不是点积！）
print(a @ b)                          # 32            （矩阵乘法/点积）
print(np.dot(a, b))                   # 32            （等价写法）

# 广播（Broadcasting）—— 不同形状数组运算
matrix = np.array([[1, 2, 3], [4, 5, 6]])  # (2, 3)
row = np.array([10, 20, 30])                # (3,)
print(matrix + row)                         # [[11,22,33], [14,25,36]]  ← 自动广播

# --- 常用函数 ---
arr = np.array([1, -2, 3, -4])
print(np.abs(arr))                       # [1, 2, 3, 4]
print(np.sum(arr))                       # -2
print(np.mean(arr))                      # -0.5
print(np.std(arr))                       # 标准差
print(np.max(arr), np.argmax(arr))       # 3, 2  （最大值及位置）
print(np.sort(arr))                      # [-4, -2, 1, 3]

# 沿指定轴操作
matrix = np.array([[1, 2], [3, 4], [5, 6]])
print(np.sum(matrix, axis=0))            # [9, 12]    （按列求和）
print(np.sum(matrix, axis=1))            # [3, 7, 11] （按行求和）

# --- 重塑与拼接 ---
arr = np.arange(6)
print(arr.reshape(2, 3))                 # [[0,1,2],[3,4,5]]

a, b = np.array([1, 2]), np.array([3, 4])
print(np.concatenate([a, b]))            # [1, 2, 3, 4]
print(np.vstack([a, b]))                 # [[1,2],[3,4]] 垂直堆叠
print(np.hstack([a, b]))                 # [1, 2, 3, 4]  水平堆叠
```

### 2. Pandas —— 数据处理主力

```python
import pandas as pd
import numpy as np

# --- 数据结构 ---
# Series：一维带标签数组
s = pd.Series([10, 20, 30], index=["a", "b", "c"])
print(s["b"])                           # 20

# DataFrame：二维表格（⭐ 最核心的数据结构）
df = pd.DataFrame({
    "name": ["Alice", "Bob", "Charlie"],
    "age": [25, 30, 35],
    "city": ["Beijing", "Shanghai", "Shenzhen"],
    "score": [88.5, 92.0, 78.5],
})

# --- 数据读取 ---
df = pd.read_csv("data.csv", encoding="utf-8")
df = pd.read_excel("data.xlsx", sheet_name="Sheet1")
df = pd.read_json("data.json")
df = pd.read_sql("SELECT * FROM users", conn)  # 需要 SQLAlchemy

# --- 数据查看 ---
print(df.head(5))                       # 前 5 行
print(df.tail(3))                       # 后 3 行
print(df.shape)                         # (rows, cols)
print(df.info())                        # 列类型、非空计数
print(df.describe())                    # 数值列的统计摘要
print(df.columns)                       # 列名列表
print(df["score"].value_counts())       # 值计数

# --- 数据选择与过滤 ---
# 选择列
print(df["name"])                       # 返回 Series
print(df[["name", "score"]])            # 返回 DataFrame

# 过滤行（⭐ 布尔索引）
high_scorers = df[df["score"] > 85]
beijing_users = df[df["city"] == "Beijing"]

# 多条件
complex_filter = df[(df["age"] > 25) & (df["score"] > 80)]
# ⚠️ 注意：必须用 & | ~，不能用 and or not

# loc 和 iloc（⭐ 核心选择器）
# loc：按标签索引
print(df.loc[0, "name"])                # Alice
print(df.loc[1:3, ["name", "score"]])   # 标签切片（右闭！）

# iloc：按位置索引
print(df.iloc[0, 0])                    # Alice
print(df.iloc[:2, [0, 3]])              # 位置切片（右开）

# --- 数据清洗 ---
# 缺失值处理
df.dropna()                             # 删除含 NaN 的行
df.dropna(subset=["name"])              # 仅当 name 为 NaN 时删除
df.fillna(0)                            # NaN 填充为 0
df["age"].fillna(df["age"].median(), inplace=True)  # 用中位数填充

# 重复值
df.drop_duplicates()                    # 删除重复行
df.drop_duplicates(subset=["name"])     # 按 name 列去重

# 类型转换
df["age"] = df["age"].astype(int)
df["date"] = pd.to_datetime(df["date"])

# --- 分组聚合（⭐ 日常高频操作）---
# 相当于 SQL: SELECT city, AVG(score), COUNT(*) FROM df GROUP BY city
stats = df.groupby("city").agg(
    avg_score=("score", "mean"),
    count=("name", "count"),
    max_age=("age", "max"),
)
print(stats)

# --- 变换与排序 ---
df_sorted = df.sort_values("score", ascending=False)  # 排序
df["score_rank"] = df["score"].rank(ascending=False)  # 排名
df["age_bucket"] = pd.cut(df["age"], bins=[0, 20, 30, 40, 100])  # 分箱

# --- 合并 ---
# 类似 SQL JOIN
merged = pd.merge(df1, df2, on="user_id", how="inner")  # how: left/right/outer
concatenated = pd.concat([df1, df2], axis=0)            # 纵向拼接

# --- 应用自定义函数 ---
df["name_upper"] = df["name"].apply(str.upper)          # 逐元素应用
df["level"] = df["score"].apply(lambda x: "A" if x >= 90 else "B" if x >= 80 else "C")

# --- 写出 ---
df.to_csv("output.csv", index=False, encoding="utf-8-sig")  # utf-8-sig 兼容 Excel
df.to_excel("output.xlsx", sheet_name="Results")
df.to_json("output.json", orient="records", force_ascii=False)
```

### 3. 数据可视化

```python
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np

# Matplotlib 风格设置
plt.rcParams["font.sans-serif"] = ["SimHei"]      # 中文支持
plt.rcParams["axes.unicode_minus"] = False         # 负号显示

# --- Matplotlib 基础 ---
fig, axes = plt.subplots(2, 2, figsize=(12, 10))   # 2×2 子图

# 折线图
axes[0, 0].plot(x, y, color="blue", marker="o", label="趋势")
axes[0, 0].set_title("折线图")
axes[0, 0].legend()

# 柱状图
axes[0, 1].bar(categories, values, color="steelblue")
axes[0, 1].set_xlabel("类别")

# 散点图
axes[1, 0].scatter(x, y, c=colors, alpha=0.6)

# 直方图
axes[1, 1].hist(data, bins=20, edgecolor="black")

plt.tight_layout()
plt.savefig("chart.png", dpi=150, bbox_inches="tight")
plt.show()

# --- Seaborn（统计可视化，⭐ 推荐） ---
sns.set_theme(style="whitegrid")

# 单变量分布
sns.histplot(df["score"], bins=20, kde=True)

# 多变量关系
sns.scatterplot(data=df, x="age", y="score", hue="city", size="count")

# 分类对比
sns.boxplot(data=df, x="city", y="score")
sns.barplot(data=df, x="city", y="score", estimator=np.mean, ci="sd")

# 热力图（相关性矩阵）
corr = df.corr(numeric_only=True)
sns.heatmap(corr, annot=True, cmap="coolwarm", center=0)

# 子图网格
g = sns.FacetGrid(df, col="city", height=4)
g.map(sns.histplot, "score")
```

---

### 第二部分：AI / 机器学习方向

### 4. PyTorch —— 深度学习框架

```python
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset

# --- 张量（Tensor）基础 ---
# 创建
x = torch.tensor([1.0, 2.0, 3.0])
zeros = torch.zeros(2, 3)
ones = torch.ones(2, 3)
randn = torch.randn(100, 10)          # 标准正态分布
arange = torch.arange(0, 10, 2)

# 张量属性
print(x.shape)                        # torch.Size([3])
print(x.dtype)                        # torch.float32
print(x.device)                       # cpu / cuda:0

# GPU 支持（⭐ 如果有 NVIDIA 显卡 + CUDA）
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
x = x.to(device)                      # 移入 GPU

# NumPy ↔ Tensor 互转
arr = x.numpy()                       # Tensor → NumPy（都在 CPU）
tensor = torch.from_numpy(arr)        # NumPy → Tensor

# 自动微分（⭐ PyTorch 核心）
x = torch.tensor([2.0], requires_grad=True)
y = x ** 3 + 2 * x                    # y = x³ + 2x
y.backward()                          # 自动计算 dy/dx
print(x.grad)                         # tensor([14.])  3x² + 2 = 14

# --- 神经网络基本组件 ---
# 定义一个简单的 MLP
class SimpleMLP(nn.Module):
    def __init__(self, input_dim: int, hidden_dim: int, output_dim: int):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(hidden_dim, output_dim)
        self.dropout = nn.Dropout(0.2)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = self.relu(self.fc1(x))
        x = self.dropout(x)
        x = self.fc2(x)
        return x

# 训练循环骨架
def train_epoch(
    model: nn.Module,
    dataloader: DataLoader,
    optimizer: optim.Optimizer,
    loss_fn: nn.Module,
    device: torch.device,
):
    model.train()
    total_loss = 0.0
    for batch_x, batch_y in dataloader:
        batch_x, batch_y = batch_x.to(device), batch_y.to(device)

        optimizer.zero_grad()              # 清空梯度
        pred = model(batch_x)              # 前向传播
        loss = loss_fn(pred, batch_y)      # 计算损失
        loss.backward()                    # 反向传播
        optimizer.step()                   # 更新参数

        total_loss += loss.item()
    return total_loss / len(dataloader)

# 模型保存与加载
torch.save(model.state_dict(), "model.pth")
model.load_state_dict(torch.load("model.pth", map_location=device))
model.eval()                               # 切换到评估模式

# --- Hugging Face Transformers（预训练模型）---
# pip install transformers
from transformers import AutoTokenizer, AutoModel

# 加载预训练模型（自动下载）
model_name = "BAAI/bge-small-zh-v1.5"      # 中文 Embedding 模型
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModel.from_pretrained(model_name).to(device)

# 生成文本 Embedding
def get_embedding(text: str) -> torch.Tensor:
    """将文本转为向量。"""
    inputs = tokenizer(
        text, padding=True, truncation=True,
        max_length=512, return_tensors="pt"
    ).to(device)
    with torch.no_grad():
        outputs = model(**inputs)
    # 取 [CLS] token 或 mean pooling
    return outputs.last_hidden_state[:, 0, :].cpu()

# 向量嵌入
vec = get_embedding("这是一段测试文本")
print(vec.shape)  # torch.Size([1, 768])
```

### 5. Scikit-learn —— 传统机器学习

```python
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

# 数据准备
X = df[["feature1", "feature2", "feature3"]].values
y = df["label"].values

# 划分训练集/测试集
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

# 特征标准化（⭐ 很多模型的前提步骤）
scaler = StandardScaler()
X_train = scaler.fit_transform(X_train)
X_test = scaler.transform(X_test)         # ⚠️ 用训练集的参数，不要重新 fit

# 训练模型
model = RandomForestClassifier(
    n_estimators=100, max_depth=10, random_state=42
)
model.fit(X_train, y_train)

# 评估
y_pred = model.predict(X_test)
print(f"准确率: {accuracy_score(y_test, y_pred):.3f}")
print(classification_report(y_test, y_pred))

# 交叉验证
scores = cross_val_score(model, X, y, cv=5)
print(f"5折交叉验证: {scores.mean():.3f} ± {scores.std():.3f}")

# 特征重要性
for name, importance in zip(df.columns, model.feature_importances_):
    print(f"{name}: {importance:.4f}")
```

---

### 第三部分：Web 服务（轻量）

### 6. FastAPI —— 把 AI 模型暴露为 HTTP 服务

FastAPI 是你的 **Java ↔ Python 桥梁**——用它把 Python AI 能力打包成 REST API，Java 后端通过 HTTP 调用。

```python
# pip install fastapi uvicorn[standard]
# 保存为 main.py，运行：uvicorn main:app --reload

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import numpy as np

app = FastAPI(
    title="AI 推理服务",
    description="为 Java 后端提供 AI 能力",
    version="1.0.0",
)

# --- 请求/响应模型 ---
class EmbeddingRequest(BaseModel):
    texts: List[str] = Field(..., min_length=1, max_length=100)
    model: str = Field(default="bge-small-zh", description="Embedding 模型名")

class EmbeddingResponse(BaseModel):
    embeddings: List[List[float]]
    dimensions: int
    model: str
    processing_time_ms: float

# --- API 端点 ---
@app.get("/health")
async def health_check():
    """健康检查 —— Java 侧调用此接口确认服务可用。"""
    return {"status": "ok", "service": "ai-inference"}

@app.post("/embed", response_model=EmbeddingResponse)
async def generate_embedding(req: EmbeddingRequest):
    """
    生成文本 Embedding 向量。
    Java 后端将文档文本发送至此接口，获取向量用于检索。
    """
    import time
    start = time.perf_counter()

    try:
        # 这里调用你的 embedding 模型
        # embeddings = your_model.encode(req.texts)
        embeddings = [[0.1] * 768 for _ in req.texts]  # placeholder
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    elapsed = (time.perf_counter() - start) * 1000
    return EmbeddingResponse(
        embeddings=embeddings,
        dimensions=len(embeddings[0]),
        model=req.model,
        processing_time_ms=round(elapsed, 2),
    )

# 启动服务: uvicorn main:app --host 0.0.0.0 --port 8000
```

#### FastAPI 核心特性速览

```python
# --- 路径参数 ---
@app.get("/users/{user_id}")
async def get_user(user_id: int):
    return {"user_id": user_id}

# --- 查询参数 ---
@app.get("/search")
async def search(q: str, page: int = 1, size: int = 20):
    return {"query": q, "page": page, "results": [...]}

# --- 依赖注入 ---
from fastapi import Depends

def get_db():
    db = connect_to_database()
    try:
        yield db
    finally:
        db.close()

@app.get("/items")
async def list_items(db=Depends(get_db)):
    return db.query("SELECT * FROM items")

# --- 中间件（CORS 等） ---
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Java 服务地址
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 自动生成文档 ---
# 启动后访问:
#   http://localhost:8000/docs     ← Swagger UI（交互式）
#   http://localhost:8000/redoc    ← ReDoc
```

---

### 第四部分：爬虫（可选）

### 7. 网页数据采集

```python
# --- requests —— HTTP 客户端 ---
import requests

# GET 请求
resp = requests.get(
    "https://api.example.com/data",
    params={"page": 1, "size": 20},
    headers={"User-Agent": "MyApp/1.0"},
    timeout=10,                              # ⭐ 必须设超时
)
print(resp.status_code)                      # 200
print(resp.json())                           # 解析 JSON 响应
print(resp.text)                             # 纯文本

# POST 请求
resp = requests.post(
    "https://api.example.com/submit",
    json={"name": "test", "value": 42},      # 自动序列化为 JSON
    timeout=10,
)

# 文件下载
resp = requests.get("https://example.com/file.pdf", stream=True)
with open("file.pdf", "wb") as f:
    for chunk in resp.iter_content(chunk_size=8192):
        f.write(chunk)

# Session（保持 cookie，连接复用）
session = requests.Session()
session.headers.update({"Authorization": "Bearer token123"})
resp = session.get("https://api.example.com/protected")

# --- BeautifulSoup —— HTML 解析 ---
# pip install beautifulsoup4 lxml
from bs4 import BeautifulSoup

html = """
<html><body>
  <div class="article">
    <h2 class="title">标题一</h2>
    <p class="content">内容一</p>
  </div>
  <div class="article">
    <h2 class="title">标题二</h2>
    <p class="content">内容二</p>
  </div>
</body></html>
"""

soup = BeautifulSoup(html, "lxml")

# CSS 选择器（⭐ 推荐）
titles = [el.text for el in soup.select(".article .title")]
contents = [el.text for el in soup.select(".article .content")]

# Search 方法
first_div = soup.find("div", class_="article")
all_divs = soup.find_all("div", class_="article")

# 提取属性
link = soup.find("a")["href"]               # 获取链接
img_src = soup.find("img")["src"]           # 获取图片源
```

---

## 📊 方向选择决策矩阵

| 方向 | 核心库 | 应用场景 | 你的优先级 |
|------|--------|---------|:----------:|
| 数据分析 | NumPy, Pandas, Matplotlib | 数据清洗、报表、Excel 替代 | ⭐⭐⭐ |
| AI/ML 训练 | PyTorch, Scikit-learn | 模型训练、微调、实验 | ⭐⭐⭐ |
| AI 推理服务 | FastAPI, transformers | 将模型包装为 HTTP 服务 | ⭐⭐⭐ |
| NLP/Embedding | transformers, sentence-transformers | 文本向量化、语义搜索 | ⭐⭐⭐ |
| 爬虫采集 | requests, BeautifulSoup | 数据采集、知识库构建 | ⭐⭐ |
| Web 后端 | FastAPI, SQLAlchemy | 轻量 API（Java 做重的） | ⭐ |
| 数据可视化 | Matplotlib, Seaborn, Plotly | 分析报告、图表呈现 | ⭐⭐ |

---

## ✅ 阶段验收

1. 用 Pandas 读取一个 CSV 文件，完成数据清洗（去重、填充缺失、类型转换），输出统计摘要
2. 用 PyTorch 定义一个简单的神经网络，训练一个二分类器（可用 sklearn 生成数据）
3. 用 FastAPI 包装一个文本分类接口，通过 curl 或 Postman 测试
4. 用 requests 抓取一个公开 API 的数据，用 Pandas 分析后可视化

---

> **上一阶段** ← [03-标准库与日常脚本](03-标准库与日常脚本.md)
> **下一阶段** → [05-项目实战与 Java 联动](05-项目实战与Java联动.md)
