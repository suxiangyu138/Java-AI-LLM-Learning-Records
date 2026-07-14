# Python 全栈技术栈学习路线（2026 最新）

> **核心摘要**：结合 Java 背景 + AI 兴趣（人脸识别、大模型），Python 技术栈是跨语言、落地 AI、做全栈的核心选择。按基础 → 核心 → 方向 → 实战梳理，覆盖后端开发、数据处理、AI/大模型、自动化全场景。

---

## 一、Python 基础筑基（1-2 周）

**核心目标**：掌握 Python 语法、数据结构、编程思维，适配后续框架与 AI 学习。

**必学内容**：
- 基础语法：变量、数据类型、流程控制、函数、模块与包、文件操作
- 数据结构：列表、字典、元组、集合、推导式、迭代器、生成器
- 面向对象：类、继承、多态、封装、魔法方法
- 高级特性：装饰器、上下文管理器、异常处理、lambda

## 二、Python 核心库与数据处理（2-3 周）

**核心目标**：成为 Python 数据处理/AI 的基础工具，是机器学习、人脸识别、大模型的前置技能。

| 库 | 用途 |
|----|------|
| NumPy | 数值计算、多维数组、矩阵运算 |
| Pandas | 数据清洗、分析、表格处理 |
| Matplotlib/Seaborn | 数据可视化 |
| OpenCV | 图像处理核心，人脸识别 |

## 三、Web 后端开发（3-4 周）

**核心目标**：搭建 Python 后端服务，对接前端、数据库。

| 框架 | 特点 | 适用场景 |
|------|------|---------|
| Flask | 轻量级 | 小型项目、快速开发 |
| FastAPI | 高性能、异步支持 | AI 接口服务（推荐） |
| Django | 全栈框架，内置 ORM | 大型项目 |

**配套技术**：MySQL（pymysql/SQLAlchemy）、Redis（redis-py）、异步编程（asyncio、aiohttp）。

## 四、AI/机器学习/大模型（核心方向，4-8 周）

### 4.1 深度学习框架

| 框架 | 用途 |
|------|------|
| PyTorch | 当前 AI/大模型主流框架 |
| Hugging Face Transformers | 大模型一站式库 |
| TensorFlow | 工业级部署 |

### 4.2 计算机视觉（人脸识别）

- OpenCV-Python：人脸检测、对齐、图像预处理
- InsightFace：工业级人脸识别框架
- MTCNN：人脸关键点检测

### 4.3 大模型与生成式 AI

- RAG：LangChain + 向量数据库（Milvus/FAISS）
- 模型微调：LoRA、QLoRA，PEFT + Transformers
- 向量数据库：Milvus、Chroma

## 五、自动化与运维（拓展方向，2 周）

- 爬虫：Requests、BeautifulSoup、Scrapy、Selenium
- 自动化：PyAutoGUI、Pytest
- 运维：Paramiko、Docker Python SDK

## 六、项目实战（推荐 3 个）

| 项目 | 技术栈 | 价值 |
|------|--------|------|
| 人脸认证系统 | OpenCV + InsightFace + FastAPI + MySQL + Docker | 综合所有技术栈 |
| 大模型私有知识库 | LangChain + Milvus + Qwen + FastAPI | 企业级 AI 落地 |
| Java+Python 分布式系统 | Dubbo(Java) + FastAPI(Python) + Redis + MySQL | 混合架构 |

## 核心要点回顾

1. 基础阶段：Python 语法 → NumPy/Pandas/OpenCV
2. 后端阶段：FastAPI/Flask → MySQL/Redis
3. AI 核心阶段：PyTorch → 人脸识别 → 大模型/RAG
4. 工程化阶段：Docker 部署 → 项目整合 → 上线运行

## 参考资料

1. FastAPI 官方文档 - fastapi.tiangolo.com
2. Hugging Face 文档 - Transformers 库
3. PyTorch 官方教程 - pytorch.org/tutorials
