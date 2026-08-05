# LLM大模型入门进阶 面试宝典
> 基于26集进阶课程大纲，聚焦RAG向量数据库工程化、LangChain Function Calling、企业级落地与AI岗位求职面试考点。涵盖PgVector/Mybatis/Java+Python混合实战、大模型微调部署、年薪60W+成长路径。

## 目录
1. [一、基础概念速答（20题）](#一基础概念速答20题)
2. [二、深度原理剖析（15题）](#二深度原理剖析15题)
3. [三、实战场景题（12题·含完整代码）](#三实战场景题12题含完整代码)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践（15个坑点）](#六常见坑点与最佳实践15个坑点)
7. [七、面试回答模板（Top 5高频题）](#七面试回答模板top-5高频题)
8. [八、快速查漏补缺Checklist（30项）](#八快速查漏补缺checklist30项)

---

## 一、基础概念速答（20题）

### 1. RAG完整工作流程是什么？

**RAG（Retrieval-Augmented Generation）** 检索增强生成，核心流程：

```text
用户查询 → 查询向量化 → 向量检索（Top-K） → 检索结果重排序 → 注入Prompt上下文 → LLM生成回答
```

详细链路：用户输入 → Embedding模型转为向量 → 向量数据库ANN检索 → 取出Top-K相关文档块 → 可选ReRanker精排 → 将文档块+原始问题拼入Prompt → 调用LLM生成答案 → 返回给用户。

### 2. 向量数据库与传统数据库的区别？

| 维度 | 传统数据库（MySQL/PgSQL） | 向量数据库（pgvector/Pinecone） |
|------|--------------------------|-------------------------------|
| 数据类型 | 结构化字段（数值、字符串、时间） | 高维浮点数向量（768/1536维） |
| 索引结构 | B+树、Hash索引 | IVF、HNSW图索引 |
| 查询方式 | 精确匹配、范围查询 | 近似最近邻（ANN）、余弦/欧氏距离 |
| 适用场景 | 事务处理、CRUD | 语义搜索、相似度匹配、RAG |
| 一致性 | ACID强一致 | 最终一致性优先 |
| 扩展方式 | 分库分表（水平扩展复杂） | 分布式分片（Sharding原生） |

> 💡 企业常用方案：PgSQL + pgvector插件同时支持结构化查询和向量检索，避免引入过多中间件。

### 3. PgVector是什么？如何实现向量检索？

**PgVector** 是 PostgreSQL 的开源向量检索插件，支持：

- **向量数据类型**：`vector(n)` 存储n维浮点数向量
- **索引类型**：IVFFlat（倒排文件）、HNSW（分层小世界图）
- **距离函数**：L2距离、内积（IP）、余弦距离
- **检索操作符**：`<->`（L2）、`<#>`（内积）、`<=>`（余弦）

基础使用：

```sql
-- 安装插件
CREATE EXTENSION vector;

-- 建表（768维向量：text-embedding-ada-002）
CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    content TEXT,
    embedding vector(768)
);

-- 创建IVFFlat索引（4个聚类中心）
CREATE INDEX ON documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 4);

-- 向量检索（查询Top-5相似）
SELECT id, content, 1 - (embedding <=> query_embedding) AS similarity
FROM documents
ORDER BY embedding <=> query_embedding
LIMIT 5;
```

### 4. LangChain的模型IO封装指什么？

LangChain核心组件之一，统称为 **Model I/O**，包含三层：

1. **Prompt Templates**：模板化输入构建，支持变量注入
2. **Models**：LLM（纯文本输入输出） vs ChatModel（消息列表输入输出）
3. **Output Parsers**：解析模型输出为结构化数据（JsonOutputParser、PydanticOutputParser）

```python
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser

chain = ChatPromptTemplate.from_template("翻译成英文：{text}") | ChatOpenAI() | StrOutputParser()
result = chain.invoke({"text": "你好世界"})  # "Hello World"
```

### 5. LLM vs ChatModel的区别？

| 维度 | LLM | ChatModel |
|------|-----|-----------|
| 输入格式 | 字符串（纯文本） | 消息列表（System/Human/AI/Tool） |
| 典型类 | `OpenAI()` | `ChatOpenAI()` |
| API调用 | 单轮文本补全 | 多轮对话 + 角色区分 |
| Function Calling | 不支持原生 | 原生支持（Tool绑定） |
| 适用场景 | 简单文本生成、分类 | 对话、Agent、工具调用 |
| 底层调用 | `/completions` 端点 | `/chat/completions` 端点 |

> ⚠️ 在新版LangChain中，建议始终使用ChatModel，LLM类已标记为legacy。

### 6. 什么是Function Calling（工具调用）？

Function Calling 是大模型能够根据用户需求**自动选择并调用外部工具函数**的机制。它不是模型真正"调用"函数，而是模型**输出一个结构化的函数调用指令**，由应用层执行。

```python
from openai import OpenAI

client = OpenAI()

# 定义工具
tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string"},
                "date": {"type": "string"}
            },
            "required": ["city"]
        }
    }
}]

response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "北京今天天气怎么样？"}],
    tools=tools
)
# 模型返回 tool_calls，应用层执行 get_weather("北京", "2026-07-22")
```

### 7. 结构化输出有哪些方式？

| 方案 | LangChain组件 | 说明 | 适用场景 |
|------|--------------|------|---------|
| JSON Schema | `JsonOutputParser` | 指定JSON结构模板 | 简单结构化提取 |
| Pydantic模型 | `PydanticOutputParser` | 定义类+类型校验 | 复杂结构化输出 |
| TypedDict | `with_structured_output()` | 类型注解驱动 | Pythonic方式 |
| `@dataclass` | 搭配OutputParser | Python原生数据类 | Python项目 |
| Function Calling | Tool定义 | 模型自动选择输出 | 工具调用场景 |

```python
from pydantic import BaseModel, Field
from langchain_core.output_parsers import PydanticOutputParser

class Person(BaseModel):
    name: str = Field(description="姓名")
    age: int = Field(description="年龄")
    skills: list[str] = Field(description="技能列表")

parser = PydanticOutputParser(pydantic_object=Person)

# 推荐方式（v0.3+）
llm = ChatOpenAI(model="gpt-4o")
structured_llm = llm.with_structured_output(Person)
result: Person = structured_llm.invoke("张三28岁，精通Python、Java、AI")
```

### 8. AI大模型岗位有哪些分类？

| 岗位方向 | 核心职责 | 技能要求 | 薪资范围（2025年·一线） |
|---------|---------|---------|---------------------|
| **AI应用开发** | RAG系统、Agent开发、API调用编排 | Python/Java + LangChain + LLM API | 40-80W |
| **模型训练/微调** | SFT、RLHF、LoRA微调、数据构建 | PyTorch + Deepspeed + 分布式训练 | 60-120W |
| **AI工程化/部署** | 模型推理加速、vLLM部署、监控 | vLLM/TGI + K8s + GPU运维 | 50-100W |
| **AI算法研究** | 预训练、多模态、对齐 | 顶会论文 + 扎实数学功底 | 80-150W+ |
| **AI产品经理** | 需求分析、数据标注管理、效果评估 | 技术理解力 + 数据分析 + 项目管理 | 40-80W |
| **AI Infra** | 集群管理、训练平台、数据管线 | K8s + GPU调度 + 分布式存储 | 60-120W |

### 9. 企业RAG工程化面临的挑战有哪些？

| 挑战维度 | 具体问题 | 解决方案 |
|---------|---------|---------|
| 数据更新 | 文档更新后向量不同步，检索到过期数据 | 增量索引 + 版本控制 + TTL过期 |
| 权限控制 | 不同用户可检索不同范围的文档 | 元数据过滤 + 行级权限 + SQL级过滤 |
| 延迟优化 | 向量检索+LLM生成总延迟>5s | 缓存 + batch推理 + LLM流式输出 |
| 质量评估 | 检索结果相关度低、幻觉率高 | RAGAS评估 + 人工标注 + A/B测试 |
| 成本控制 | 大量文档重复向量化，token消耗大 | 文档压缩 + 缓存复用 + 用小模型Rerank |
| 多语混合 | 中英混合文档，跨语言检索不准确 | 双语Embedding（如bge-m3）+ 翻译 |

### 10. 什么是模型私有化部署？

模型私有化部署是指将LLM部署在企业自有服务器或私有云上，数据不出域。

**部署方案对比：**

| 方案 | 适用模型 | 推理速度 | 显存需求 | 部署难度 | 典型场景 |
|------|---------|---------|---------|---------|---------|
| Ollama | 7B-72B | 中等 | 8-64GB | ⭐（最简单） | 本地开发测试 |
| vLLM | 全量开源模型 | ⭐最快 | 16-128GB | ⭐⭐⭐ | 生产高并发 |
| TGI (HuggingFace) | 主流开源模型 | 快 | 16-128GB | ⭐⭐⭐ | HF生态、企业级 |
| Triton + TensorRT-LLM | NVIDIA优化 | ⭐极快 | 32-256GB | ⭐⭐⭐⭐ | 高吞吐、NV卡优化 |
| llama.cpp | CPU/边缘设备 | 慢 | 4-16GB | ⭐⭐ | 边缘部署、Apple M系列 |

### 11. AI Agent在企业中的落地场景？

| 场景 | 架构模式 | 工具使用 | 典型案例 |
|------|---------|---------|---------|
| 智能客服 | RAG + Agent | 知识库检索 + 工单系统 | 京东JIMI、蚂蚁金服 |
| 数据分析 | Agent + Code Interpreter | SQL执行器 + Python沙箱 | 美团运营分析 |
| 代码审查 | Multi-Agent | Git + 静态分析 + 测试运行 | 字节跳动的Code Agent |
| 自动化运维 | (ReAct) Agent | Shell + K8s API + 监控 | 阿里云Sar |
| 招聘面试 | RAG + Agent | 简历解析 + 面试题库 + JD匹配 | 猎聘/智联AI助手 |

### 12. 2025年AI岗位招聘趋势？

- **大厂AI HC增长**：字节/阿里/腾讯/百度AI相关岗位占总招聘量25%+
- **Java+AI走俏**：纯Python岗逐步饱和，Java+AI（Spring AI + LangChain4j）需求上升
- **RAG工程师成标配**：几乎所有NLP岗位要求RAG系统搭建经验
- **Agent落地经验加分**：企业关注"能否把Agent做到生产级"，而非Demo级别
- **25届校招AI薪资**：SP 45-55W，SSP 55-70W（大模型方向）

### 13. Embedding模型与LLM的区别？

| 维度 | Embedding模型 | LLM |
|------|-------------|-----|
| 输出 | 高维向量（768/1024维） | 自然语言文本 |
| 目的 | 语义编码、相似度计算 | 文本生成、推理对话 |
| 架构 | BERT类Encoder-only | GPT类Decoder-only |
| 参数量 | 小（100M-1B） | 大（7B-100B+） |
| 训练方式 | 对比学习（NLI/SimCSE） | 自回归语言建模 |
| 推理成本 | 低（CPU也可运行） | 高（需要GPU） |

### 14. 什么是ReRanker（重排序器）？

ReRanker是在向量检索初步召回后，对Top-K结果进行精细排序的模型。

```python
from sentence_transformers import CrossEncoder

reranker = CrossEncoder("BAAI/bge-reranker-v2-m3")

query = "什么是RAG？"
documents = ["RAG是检索增强生成...", "大模型微调方法...", "向量数据库..."]

pairs = [[query, doc] for doc in documents]
scores = reranker.predict(pairs)

# 按分数降序排列
sorted_results = [doc for _, doc in sorted(zip(scores, documents), reverse=True)]
```

> 💡 推荐方案：Embedding模型（bge-m3）粗召回Top-50 → ReRanker精排Top-5，准确率提升15-30%。

### 15. 什么是Query Rewriting（查询改写）？

在RAG中，用户原始查询质量参差不齐，查询改写旨在优化：

```python
# 典型改写策略
rewrite_prompt = """基于对话历史，将用户问题改写为独立且清晰的搜索查询。

对话历史：{history}
用户问题：{question}

请输出改写后的查询："""
```

**改写策略：**
- **去指代**：将"它的原理是什么" → "RAG的原理是什么"
- **补全**：将"性能怎么样" → "PgVector在大数据量下的检索性能"
- **扩展**：将"向量数据库" → "向量数据库 选型 对比 应用场景"
- **多查询**（HyDE / Multi-Query）：生成多个不同角度的查询

### 16. MyBatis如何连接PgSQL + pgvector？

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
</dependency>

<!-- application.yml -->
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

### 17. MyBatis整合PgVector的Mapper编写要点？

```java
// Entity
public class DocumentEntity {
    private Long id;
    private String content;
    private float[] embedding;  // 768维
}

// Mapper XML（注意vector类型映射）
<resultMap id="documentMap" type="DocumentEntity">
    <id column="id" property="id"/>
    <result column="content" property="content"/>
    <result column="embedding" property="embedding"
            typeHandler="org.apache.ibatis.type.ArrayTypeHandler"/>
</resultMap>

<!-- 向量检索 -->
<select id="vectorSearch" resultMap="documentMap">
    SELECT id, content,
           1 - (embedding <=> #{queryVector, typeHandler=org.apache.ibatis.type.ArrayTypeHandler}::vector) AS similarity
    FROM documents
    ORDER BY embedding <=> #{queryVector}::vector
    LIMIT #{topK}
</select>
```

### 18. 什么是Hybrid Search（混合检索）？

混合检索 = **关键词检索（BM25）** + **向量语义检索**，结合两者优势：

```sql
-- 混合检索SQL示例（PgSQL + pgvector）
SELECT id, content,
       (0.3 * ts_rank(to_tsvector('chinese', content), plainto_tsquery('chinese', '搜索词')) +
        0.7 * (1 - (embedding <=> query_vec))) AS score
FROM documents
WHERE to_tsvector('chinese', content) @@ plainto_tsquery('chinese', '搜索词')
   OR 1 - (embedding <=> query_vec) > 0.7
ORDER BY score DESC
LIMIT 10;
```

### 19. 什么是Chunking（文本分块）策略？

| 策略 | 方法 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| 固定大小 | 按字符/token数切分 | 简单、确定性强 | 语义断裂 | 通用兜底 |
| 递归字符分块 | LangChain RecursiveCharacterTextSplitter | 保留段落语义 | 分块大小不均 | 通用文档 |
| 语义分块 | 按主题/段落边界切分 | 语义完整度高 | 实现复杂 | 高质量RAG |
| Agent分块 | LLM判断断点 | 最智能 | 成本高、慢 | 知识库构建 |
| 小分块 | 128-256 tokens | 检索精确 | 上下文不足 | FAQ问答 |

### 20. 企业级RAG系统的评价指标？

| 指标 | 含义 | 计算方法 |
|------|------|---------|
| **Hit Rate** | Top-K中包含正确答案的比例 | 命中数/总查询数 |
| **MRR** | 第一个正确答案的平均排序倒数 | 1/n 的均值 |
| **NDCG** | 排序质量归一化指标 | 累积增益/理想增益 |
| **Faithfulness** | 回答是否忠于检索文档 | LLM判定或人工标注 |
| **Answer Relevancy** | 回答与问题的相关度 | LLM打分 |
| **Latency P99** | 99%请求延迟 | 监控系统 |
| **Ragas Score** | RAGAS综合评分 | ragas框架计算 |

---

## 二、深度原理剖析（15题）

### 1. RAG全链路架构详解

```
┌─────────────────────────────────────────────────────┐
│                     数据准备                          │
│  源文档 → 格式解析(PDF/Word/HTML) → 清洗去重 → 分块   │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                    索引构建                           │
│  Embedding模型向量化 → 存储进向量数据库 → 元数据索引    │
└─────────────────────────────────────────────────────┘
                         ↓ (用户查询)
┌─────────────────────────────────────────────────────┐
│                    检索模块                           │
│  查询向量化 → ANN检索(Top-50) → ReRank精排(Top-5)      │
│          → Query Rewrite / HyDE(可选)                 │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                    生成模块                           │
│  Context + Prompt构建 → LLM生成 → 引用标注 → 输出     │
└─────────────────────────────────────────────────────┘
```

**各环节关键技术：**

| 环节 | 关键点 | 常用工具/库 |
|------|-------|------------|
| 文档解析 | PDF表格提取、OCR扫描件 | PyMuPDF、unstructured、Marker |
| 分块 | 块大小256-1024tokens、重叠10-20% | LangChain TextSplitter |
| 向量化 | 中文Embedding选型 | bge-m3、text-embedding-3-small |
| 向量检索 | IVFFlat/HNSW索引、Search参数 | pgvector、Milvus |
| 重排序 | CrossEncoder模型 | bge-reranker-v2-m3 |
| Prompt构建 | System Prompt + Context注入 | LangChain PromptTemplate |
| 生成 | 温度、Top-P、MaxTokens参数 | ChatOpenAI、本地模型 |

### 2. 向量数据库方案对比

| 特性 | pgvector | FAISS | Milvus | Pinecone | Chroma |
|------|----------|-------|--------|----------|--------|
| **类型** | PgSQL插件 | 库（C++/Python） | 独立服务 | SaaS | 嵌入式数据库 |
| **部署** | 随PgSQL | 集成到应用 | Docker/K8s | 云端托管 | pip install |
| **索引** | IVFFlat/HNSW | 多种索引策略 | IVF/HNSW/DiskANN | 托管自动 | HNSW |
| **分布式** | 需PgSQL扩展 | 不支持原生 | 原生支持 | 支持 | 不支持 |
| **一致性** | ACID | 无 | 最终一致性 | 最终一致性 | 无 |
| **适用场景** | 已用PgSQL的中型项目 | 自定义检索算法 | 海量向量生产级 | 快速原型验证 | 小项目/本地 |
| **成本** | 免费 | 免费 | 免费/付费版 | 付费（贵） | 免费 |
| **中文生态** | 无特殊 | 无特殊 | 好（中文社区） | 好 | 一般 |
| **推荐场景** | **Java+Spring生态** | 算法研究 | 大规模生产 | 快速起步 | 学习原型 |

> 🎯 **选型建议**：Java技术栈 + 已有PgSQL → pgvector；海量数据 + 分布式 → Milvus；快速验证 → Chroma/Pinecone。

### 3. PgVector原理：IVFFlat与HNSW索引算法

**IVFFlat（Inverted File with Flat）**

```
算法步骤：
1. 聚类：对数据集使用K-means聚类，分为lists个簇
2. 分配：每个向量分配到最近的簇中心
3. 查询：找到最近的probes个候选簇
4. 搜索：在候选簇中暴力计算距离

参数：
- lists：聚类数（影响召回/速度 trade-off）
  - 经验公式：lists = sqrt(rows)
  - 100万行 → lists = 1000
- probes：搜索簇数（越大召回越高）
  - SET ivfflat.probes = 10

特点：
- 构建快（一次聚类）
- 查询速度快但召回率低于HNSW
- 适合数据量不经常变化的场景
```

**HNSW（Hierarchical Navigable Small World）**

```
算法步骤：
1. 层级结构：构建多层图，顶层节点稀疏，底层节点密集
2. 导航：从顶层最粗粒度开始搜索，逐层下降到最底层
3. 局部搜索：每层使用贪心NN搜索，记录ef_search个候选

参数：
- m：每层最大连接数（影响图密度）
- ef_construction：建图时搜索宽度（越大索引质量越高）
- ef_search：查询时搜索宽度（越大召回越高）

特点：
- 召回率极高（95%+）
- 构建时间较长
- 内存占用较大
- 适合对召回率要求高的场景
```

**对比总结：**

| 维度 | IVFFlat | HNSW |
|------|---------|------|
| 构建速度 | ⭐⭐⭐ 快 | ⭐⭐ 中 |
| 查询速度 | ⭐⭐⭐ 中 | ⭐⭐⭐⭐ 快 |
| 召回率 | ⭐⭐⭐ 中 | ⭐⭐⭐⭐⭐ 高 |
| 内存占用 | ⭐⭐⭐⭐⭐ 低 | ⭐⭐ 高 |
| 动态插入 | 需重建索引 | 支持动态 |
| 适用范围 | 百万级 | 百万-千万级 |

### 4. 结构化输出实现原理

**核心思路**：通过定义输出schema，约束模型输出的格式和字段类型。

**Pydantic方法（LangChain推荐）：**

```python
from pydantic import BaseModel, Field
from typing import Literal

class Recipe(BaseModel):
    name: str = Field(description="菜名")
    difficulty: Literal["简单", "中等", "困难"] = Field(default="中等")
    ingredients: list[str] = Field(min_length=1, description="食材列表")
    steps: list[str] = Field(min_length=1, description="步骤列表")
    cook_time_minutes: int = Field(gt=0, le=1440)

llm = ChatOpenAI(model="gpt-4o", temperature=0)
structured_llm = llm.with_structured_output(Recipe, method="function_calling")
recipe = structured_llm.invoke("写一个番茄炒蛋的食谱")
# recipe.name, recipe.ingredients, recipe.steps 都是类型化字段
```

**JSON Schema方式（OpenAI原生）：**

```json
{
  "name": "extract_recipe",
  "strict": true,
  "schema": {
    "type": "object",
    "properties": {
      "name": {"type": "string"},
      "difficulty": {"type": "string", "enum": ["简单", "中等", "困难"]},
      "ingredients": {"type": "array", "items": {"type": "string"}, "minItems": 1}
    },
    "required": ["name", "difficulty", "ingredients"],
    "additionalProperties": false
  }
}
```

**实现方式对比：**

| 方法 | 底层机制 | 优点 | 缺点 |
|------|---------|------|------|
| Function Calling | 模型输出tool_calls | 最稳定，主流 | 需工具定义 |
| JSON mode | `response_format={type: "json_object"}` | 简单 | 无schema约束 |
| 约束解码 | `outlines`/`lm-format-enforcer` | 严格合规 | 需本地模型 |
| Prompt指导 | "请输出JSON格式" | 零依赖 | 不稳定，易失败 |

### 5. Function Calling执行流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  用户输入  │ ──> │  LLM判断  │ ──> │  解析工具  │ ──> │  执行函数  │
│ "北京天气" │     │ 需要调用  │     │  调用参数  │     │ get_weather│
└──────────┘     │ get_weather│     │ {city:    │     │("北京")   │
                 │  (tool_call)│     │ "北京"}   │     │           │
                 └──────────┘     └──────────┘     └─────┬─────┘
                                                          │
┌──────────┐     ┌──────────┐     ┌──────────┐           │
│  最终回答  │ <── │  LLM整合  │ <── │  返回结果  │ <────────┘
│ "北京晴天" │     │  tool结果 │     │ "晴，25℃" │
└──────────┘     └──────────┘     └──────────┘
```

**完整实现代码（LangChain Agent）：**

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain.tools import tool

@tool
def get_weather(city: str) -> str:
    """获取指定城市的天气"""
    # 这里模拟API调用
    weather_data = {"北京": "晴，25-32℃", "上海": "多云，28-35℃"}
    return weather_data.get(city, f"{city}天气数据暂不可用")

@tool
def query_database(sql: str) -> list:
    """执行SQL数据库查询"""
    # 实际项目中连接数据库
    return [{"result": "模拟数据"}]

llm = ChatOpenAI(model="gpt-4o", temperature=0)
tools = [get_weather, query_database]

agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

agent_executor.invoke({"input": "北京今天天气怎么样？"})
```

### 6. 企业级RAG工程化挑战与解决方案

| 挑战 | 详细说明 | 解决方案 |
|------|---------|---------|
| **数据更新** | 文档变更、新增、删除后向量库不一致 | - 增量更新：比对文档hash，只更新变化文档<br>- TTL过期：设置文档过期策略<br>- 版本控制：每个文档版本独立存储 |
| **权限控制** | 不同用户只能检索授权文档 | - 元数据过滤：存储doc_id/tenant_id字段<br>- PgSQL行级安全（RLS）<br>- 分租户索引 |
| **延迟优化** | 检索+生成整体延迟高 | - 检索：HNSW索引、减少probes<br>- 生成：流式输出、小模型首token快<br>- 缓存：热点查询缓存（Caffeine/Redis） |
| **质量评估** | 检索结果不相关、回答有幻觉 | - RAGAS框架定期评估<br>- 人工标注bad case<br>- 添加"我不确定"的兜底回答 |
| **成本控制** | API调用和GPU成本高 | - 本地小模型（7B/13B）处理简单查询<br>- 提示词压缩：去除冗余context<br>- Embedding缓存：相同查询复用向量 |
| **多模态文档** | PDF含表格、图片无法直接检索 | - OCR解析（PaddleOCR）<br>- 表格转Markdown<br>- 图生文（Captioning模型） |

### 7. 模型微调落地方案

| 方案 | 原理 | 训练参数量 | 显存需求（7B） | 效果 | 适用场景 |
|------|------|-----------|-------------|------|---------|
| **全量微调** | 更新全部参数 | 7B | 4×A100 80GB | ⭐⭐⭐⭐⭐ | 大规模算力充足 |
| **LoRA** | 低秩适配器，冻结原参数 | <1% | 1×A100 80GB | ⭐⭐⭐⭐ | 多数业务场景 |
| **QLoRA** | LoRA + 4bit量化 | <0.1% | 1×RTX 4090 24GB | ⭐⭐⭐ | 消费级显卡 |
| **P-Tuning** | 连续Prompt前缀 | <0.01% | 1×A100 80GB | ⭐⭐⭐ | 参数高效调优 |
| **Adapter** | Transformer层插入小网络 | <5% | 1×A100 80GB | ⭐⭐⭐ | 多任务适配 |

**LoRA微调代码示例：**

```python
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import LoraConfig, get_peft_model, TaskType

model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")

lora_config = LoraConfig(
    task_type=TaskType.CAUSAL_LM,
    r=8,                # 秩（rank）
    lora_alpha=32,      # 缩放因子
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
    lora_dropout=0.1,
    bias="none",
)

peft_model = get_peft_model(model, lora_config)
peft_model.print_trainable_parameters()
# 输出：trainable params: 4,194,304 || all params: 7,000,000,000 || trainable%: 0.0599

# 训练完成后保存
peft_model.save_pretrained("./qwen-lora-adapter")
```

### 8. 生产级模型部署方案

| 方案 | 并发能力 | 延迟 | 特性 | 推荐模型 | 部署复杂度 |
|------|---------|------|------|---------|---------|
| **vLLM** | ⭐⭐⭐⭐⭐ 高 | ⭐⭐⭐ 低 | PagedAttention、连续批处理、KV缓存 | 所有主流LLM | 中 |
| **TGI** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | HuggingFace生态、安全防护 | HF模型 | 中 |
| **Ollama** | ⭐⭐ | ⭐⭐ | 一键部署、模型管理便捷 | 7B-72B | 低 |
| **Triton + TRT-LLM** | ⭐⭐⭐⭐⭐ 极高 | ⭐⭐⭐⭐⭐ 极低 | 多框架支持、动态批处理 | NVIDIA优化模型 | 高 |
| **llama.cpp** | ⭐ | ⭐⭐ | CPU运行、ARM优化 | GGUF格式 | 低 |
| **SGLang** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | RadixAttention、结构化生成 | 主流模型 | 中 |

**vLLM部署命令：**

```bash
# 安装
pip install vllm

# 启动服务（单GPU）
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2.5-7B-Instruct \
    --tensor-parallel-size 1 \
    --gpu-memory-utilization 0.9 \
    --max-num-seqs 256 \
    --port 8000

# 请求示例
curl http://localhost:8000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "Qwen/Qwen2.5-7B-Instruct", "messages": [{"role": "user", "content": "Hello"}]}'
```

### 9. Word Embedding与LLM Embedding的关系

> Word Embedding（Word2Vec/Glove）是静态向量，每个词一个固定向量
> LLM Embedding（BERT/GPT）是上下文相关的动态向量，同一个词在不同上下文中有不同向量

**对比：**

| 维度 | Word2Vec | BERT Embedding |
|------|---------|---------------|
| 特性 | 静态词向量 | 上下文感知 |
| "苹果"向量 | 始终相同 | "苹果手机" vs "苹果水果" 不同 |
| 训练数据 | 少量（几亿词） | 大量（TB级语料） |
| 参数量 | ~10M | 100M-1B |
| 语义效果 | 一般 | ⭐⭐⭐⭐ |

### 10. 对比学习在Embedding中的应用

对比学习（Contrastive Learning）是现代Embedding模型的核心训练范式：

```python
# SimCSE核心思想：同一句子两次Dropout得到正样本对
# 损失函数：InfoNCE Loss

"""
L = -log( exp(sim(h_i, h_i+)/τ) / Σ exp(sim(h_i, h_j-)/τ) )

其中：
- h_i, h_i+ : 正样本对（同一句子两次编码）
- h_j- : 负样本（其他句子）
- τ : 温度系数
- sim : 余弦相似度
"""
```

常用Embedding模型：

| 模型 | 参数量 | 向量维度 | 最大长度 | 中文效果 | 推荐场景 |
|------|-------|---------|---------|---------|---------|
| bge-small-zh-v1.5 | 24M | 512 | 512 | ⭐⭐⭐ | 轻量场景 |
| bge-base-zh-v1.5 | 102M | 768 | 512 | ⭐⭐⭐⭐ | 通用RAG |
| bge-large-zh-v1.5 | 326M | 1024 | 512 | ⭐⭐⭐⭐⭐ | 高质量检索 |
| bge-m3 | 567M | 1024 | 8192 | ⭐⭐⭐⭐⭐ | 多语/长文档 |
| text-embedding-3-small | - | 1536 | 8191 | ⭐⭐⭐⭐ | OpenAI生态 |
| text-embedding-3-large | - | 3072 | 8191 | ⭐⭐⭐⭐⭐ | 精度要求高 |

### 11. 大模型的注意力机制与Transformer

**核心公式**：`Attention(Q,K,V) = softmax(QK^T/√d_k)V`

```
Q: Query (查询向量)    — 当前token想找哪些相关信息
K: Key   (键向量)      — 每个token能提供的标记
V: Value (值向量)       — 每个token的实际内容
```

**Transformer架构演进：**

| 架构 | 核心创新 | 代表模型 | 特点 |
|------|---------|---------|------|
| Encoder-only | 双向注意力 | BERT、RoBERTa | 适合理解任务（分类、NER） |
| Decoder-only | 因果注意力 | GPT系列、Qwen、LLaMA | 适合生成任务 | 
| Encoder-Decoder | 交叉注意力 | T5、BART | 适合序列转换（翻译、摘要） |
| MoE | 稀疏专家混合 | Mixtral、DeepSeek-V2 | 相同计算量更大参数量 |

### 12. 提示词工程的高级技术

| 技术 | 原理 | 适用场景 |
|------|------|---------|
| **Chain-of-Thought (CoT)** | 引导模型逐步推理 | 数学、逻辑推理 |
| **Few-Shot** | 提供示例 | 分类、格式转换 |
| **System Prompt** | 设定角色和约束 | 所有场景的基础 |
| **Role-Playing** | 赋予专业角色身份 | 客服、医生、律师 |
| **Negative Prompt** | 告诉模型不要做什么 | 避免幻觉、格式约束 |
| **XML/JSON Prompt** | 结构化提示模板 | 结构化输出 |

### 13. 大模型幻觉问题与缓解

| 幻觉类型 | 表现 | 原因 | 缓解方法 |
|---------|------|------|---------|
| 事实性幻觉 | 编造不存在的事实 | 训练数据偏差/模型记忆错误 | RAG + 外部知识库 |
| 忠实性幻觉 | 不遵循用户指令 | 对齐不足 | 更好的System Prompt |
| 输入冲突 | 忽略上下文信息 | 注意力衰减 | Position encoding优化 |
| 逻辑幻觉 | 逻辑推理错误 | 模型能力局限 | CoT、Self-Consistency |

**RAG方案是缓解幻觉的最佳实践**：用检索结果约束LLM的回答范围。

### 14. Model Context Protocol (MCP) 初探

MCP是由Anthropic提出的模型-工具通信协议标准，核心概念：

```
客户端（Host）<--MCP协议--> 服务端（Server）
    |                            |
  工具调用                 暴露工具/资源/提示
```

MCP vs Function Calling：

| 维度 | Function Calling | MCP |
|------|----------------|-----|
| 定义方式 | 应用层代码定义 | 协议层定义 |
| 复用性 | 每个应用重写 | 一次开发多处复用 |
| 动态工具 | 需重新部署 | 运行时动态发现 |
| 生态 | OpenAI/Anthropic | 正快速发展 |
| 适用 | 简单工具 | 复杂工具/多系统 |

### 15. 多模态大模型与RAG

多模态RAG（MM-RAG）扩展了传统RAG的能力：

```python
# 多模态文档处理流程
"""
文本提取: OCR (PaddleOCR) → 文字分块
图片描述: Caption模型 (CogVLM/Qwen-VL) → 图片描述文本
表格解析: 表格转Markdown
结构化存储: text + caption + table → 一起向量化
"""

# 多模态检索：文本查询 → 检索相关文本块和图片
```

---

## 三、实战场景题（12题·含完整代码）

### 1. PgVector搭建与向量检索示例

```python
"""
环境准备：
1. 安装PostgreSQL 15+：apt install postgresql
2. 安装pgvector：git clone https://github.com/pgvector/pgvector && cd pgvector && make install
3. 创建扩展：CREATE EXTENSION vector;
"""

import psycopg2
import numpy as np
from sentence_transformers import SentenceTransformer

# 1. 连接数据库
conn = psycopg2.connect(
    host="localhost",
    port=5432,
    database="rag_db",
    user="postgres",
    password="123456"
)
cur = conn.cursor()

# 2. 创建表（带vector类型）
cur.execute("""
    CREATE EXTENSION IF NOT EXISTS vector;
    
    CREATE TABLE IF NOT EXISTS documents (
        id SERIAL PRIMARY KEY,
        title TEXT,
        content TEXT,
        embedding vector(768)
    );
    
    CREATE INDEX IF NOT EXISTS idx_documents_embedding
        ON documents USING ivfflat (embedding vector_cosine_ops)
        WITH (lists = 100);
""")
conn.commit()

# 3. 初始化Embedding模型
model = SentenceTransformer("BAAI/bge-base-zh-v1.5")

# 4. 插入文档
docs = [
    "RAG（检索增强生成）是一种结合检索与生成的技术架构",
    "向量数据库专门用于存储和检索高维向量数据",
    "PgVector是PostgreSQL的向量检索插件",
    "LangChain是一个用于构建LLM应用的框架"
]

for doc in docs:
    embedding = model.encode(doc).tolist()
    cur.execute(
        "INSERT INTO documents (title, content, embedding) VALUES (%s, %s, %s::vector)",
        ("示例文档", doc, embedding)
    )
conn.commit()

# 5. 向量检索
query = "什么是向量数据库？"
query_embedding = model.encode(query).tolist()

cur.execute("""
    SELECT id, content, 1 - (embedding <=> %s::vector) AS similarity
    FROM documents
    ORDER BY embedding <=> %s::vector
    LIMIT 3
""", (query_embedding, query_embedding))

results = cur.fetchall()
for row in results:
    print(f"ID: {row[0]}, 相似度: {row[2]:.4f}, 内容: {row[1]}")

cur.close()
conn.close()
```

### 2. LangChain结构化输出（Pydantic输出解析器）

```python
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain.output_parsers import OutputFixingParser

# 定义输出结构
class Resume(BaseModel):
    """简历解析结果"""
    name: str = Field(description="候选人姓名")
    age: Optional[int] = Field(None, description="年龄")
    phone: Optional[str] = Field(None, description="手机号")
    email: Optional[str] = Field(None, description="邮箱")
    skills: list[str] = Field(description="技能列表")
    years_of_experience: Optional[float] = Field(None, description="工作年限")
    education: Optional[str] = Field(None, description="最高学历")
    
    @field_validator("age")
    @classmethod
    def validate_age(cls, v):
        if v is not None and (v < 16 or v > 100):
            raise ValueError(f"年龄不在合理范围: {v}")
        return v
    
    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v):
        if v is not None and not v.isdigit():
            raise ValueError(f"手机号格式错误: {v}")
        return v

# 方式1：OutputParser
parser = PydanticOutputParser(pydantic_object=Resume)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个简历解析专家，请严格按指定格式输出。\n{format_instructions}"),
    ("human", "请解析以下简历：{resume_text}")
])

llm = ChatOpenAI(model="gpt-4o", temperature=0)

# 使用OutputFixingParser（自动修复格式错误）
fixing_parser = OutputFixingParser.from_llm(parser=parser, llm=llm)

chain = prompt | llm | fixing_parser
result = chain.invoke({
    "resume_text": "张三，28岁，电话13800138000，邮箱zhangsan@email.com，精通Python、Java、AI开发，3年工作经验，硕士学历",
    "format_instructions": parser.get_format_instructions()
})
print(result)
# Resume(name='张三', age=28, phone='13800138000', email='zhangsan@email.com', skills=['Python', 'Java', 'AI开发'], years_of_experience=3.0, education='硕士')

# 方式2：with_structured_output（推荐）
structured_llm = llm.with_structured_output(Resume, method="function_calling")
result2 = structured_llm.invoke("李四，25岁，本科学历，熟悉Spring Boot和微服务架构，一年经验")
print(result2)
```

### 3. Function Calling实现（天气查询+计算器+数据库查询）

```python
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate
import json
import math
from datetime import datetime

# --- 定义工具 ---

@tool
def calculator(expression: str) -> str:
    """
    执行数学计算
    Args:
        expression: 数学表达式，如 "1 + 2 * 3"
    """
    # 安全计算：限制可用的数学函数
    allowed_names = {
        "abs": abs, "round": round, "min": min, "max": max,
        "sum": sum, "pow": pow, "sqrt": math.sqrt,
        "sin": math.sin, "cos": math.cos, "tan": math.tan,
        "log": math.log, "log10": math.log10,
        "pi": math.pi, "e": math.e
    }
    try:
        # 安全eval
        result = eval(expression, {"__builtins__": {}}, allowed_names)
        return f"计算结果: {result}"
    except Exception as e:
        return f"计算失败: {e}"

@tool
def get_weather(city: str, date: str = None) -> str:
    """
    获取城市天气信息
    Args:
        city: 城市名称
        date: 日期（格式：YYYY-MM-DD），默认今天
    """
    if date is None:
        date = datetime.now().strftime("%Y-%m-%d")
    
    # 模拟天气API
    fake_weather = {
        ("北京", "2026-07-22"): "晴，27-35℃，南风2级",
        ("上海", "2026-07-22"): "多云转阴，29-37℃，东南风3级",
        ("深圳", "2026-07-22"): "雷阵雨，26-32℃，西南风2级",
    }
    return fake_weather.get((city, date), f"{city}{date}天气：多云，25-30℃")

@tool
def query_database(sql: str) -> list:
    """
    执行SQL查询（模拟）
    Args:
        sql: SQL查询语句
    """
    # 模拟数据库
    mock_data = {
        "select count(*) from users": [{"count": 1024}],
        "select * from users limit 5": [
            {"id": i, "name": f"用户{i}", "role": "admin" if i == 1 else "user"}
            for i in range(1, 6)
        ]
    }
    return mock_data.get(sql.lower().strip(), [{"message": "查询成功", "rows": 0}])

# --- 构建Agent执行器 ---

tools = [calculator, get_weather, query_database]
llm = ChatOpenAI(model="gpt-4o", temperature=0)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个智能助手，可以使用工具回答用户问题，请给出完整解答。"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}")
])

agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,
    max_iterations=5,
    handle_parsing_errors=True
)

# --- 执行 ---
queries = [
    "计算 12 * (3 + 5)^2 的结果",
    "北京今天天气怎么样？",
    "查询用户总数"
]

for q in queries:
    print(f"\n{'='*50}")
    print(f"用户: {q}")
    result = agent_executor.invoke({"input": q})
    print(f"助手: {result['output']}")
```

### 4. 从零搭建RAG知识库

```python
"""
全流程RAG知识库：分块 → 向量化 → 检索 → 生成
"""

from typing import List, Dict, Optional
from dataclasses import dataclass
from sentence_transformers import SentenceTransformer
import numpy as np
from openai import OpenAI

@dataclass
class Document:
    text: str
    metadata: Dict

class TextSplitter:
    """递归字符文本分块器"""
    def __init__(self, chunk_size=500, chunk_overlap=50, separators=None):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.separators = separators or ["\n\n", "\n", "。", "，", " ", ""]
    
    def split_text(self, text: str) -> List[str]:
        chunks = []
        current_chunk = ""
        
        for char in text:
            current_chunk += char
            if len(current_chunk) >= self.chunk_size:
                chunks.append(current_chunk)
                current_chunk = current_chunk[-self.chunk_overlap:] if self.chunk_overlap else ""
        
        if current_chunk:
            chunks.append(current_chunk)
        return chunks

class EmbeddingStore:
    """简单向量存储（内存实现）"""
    def __init__(self, model_name: str = "BAAI/bge-base-zh-v1.5"):
        self.model = SentenceTransformer(model_name)
        self.documents: List[Document] = []
        self.embeddings: List[np.ndarray] = []
    
    def add_documents(self, documents: List[Document]):
        for doc in documents:
            emb = self.model.encode(doc.text)
            self.documents.append(doc)
            self.embeddings.append(emb)
    
    def search(self, query: str, top_k: int = 5) -> List[Document]:
        query_emb = self.model.encode(query)
        scores = []
        for i, doc_emb in enumerate(self.embeddings):
            score = np.dot(query_emb, doc_emb) / (np.linalg.norm(query_emb) * np.linalg.norm(doc_emb))
            scores.append((i, score))
        
        scores.sort(key=lambda x: x[1], reverse=True)
        return [self.documents[i] for i, _ in scores[:top_k]]

class RAGPipeline:
    """RAG全流程"""
    def __init__(self, embedding_store: EmbeddingStore, llm_model: str = "gpt-4o"):
        self.store = embedding_store
        self.llm = OpenAI()
        self.llm_model = llm_model
    
    def query(self, question: str, top_k: int = 5) -> str:
        # 1. 检索
        relevant_docs = self.store.search(question, top_k)
        context = "\n\n".join([doc.text for doc in relevant_docs])
        
        # 2. 生成
        prompt = f"""基于以下参考信息回答问题。如果你不知道答案，请说"未在资料中找到相关信息"。

参考信息：
{context}

问题：{question}

回答："""
        
        response = self.llm.chat.completions.create(
            model=self.llm_model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=500
        )
        return response.choices[0].message.content

# --- 使用示例 ---
if __name__ == "__main__":
    # 准备文档
    raw_docs = [
        "RAG（Retrieval-Augmented Generation）检索增强生成是一种将信息检索与文本生成相结合的技术。",
        "LangChain是一个构建LLM应用的开源框架，支持模型IO、RAG、Agent等核心能力。",
        "PgVector是PostgreSQL的向量检索扩展，支持IVFFlat和HNSW两种索引算法。",
        "向量数据库通过高维向量表示语义，实现近似最近邻搜索（ANN）。",
        "Function Calling允许LLM根据用户输入选择并调用外部工具函数。"
    ]
    
    # 分块（简单示例，不分块直接用）
    documents = [Document(text=doc, metadata={"source": "lecture"}) for doc in raw_docs]
    
    # 构建索引
    store = EmbeddingStore()
    store.add_documents(documents)
    
    # RAG查询
    rag = RAGPipeline(store)
    answer = rag.query("什么是RAG？")
    print(f"回答: {answer}")
```

### 5. LangChain + SQL数据库查询Agent

```python
from langchain_community.agent_toolkits import create_sql_agent
from langchain_community.utilities import SQLDatabase
from langchain_openai import ChatOpenAI
from langchain.agents import AgentType
from sqlalchemy import create_engine
from langchain.agents.agent_toolkits import SQLDatabaseToolkit

# 1. 连接数据库（内存SQLite示例）
engine = create_engine("sqlite:///./test.db")
# 或连接PgSQL: engine = create_engine("postgresql://postgres:123456@localhost/rag_db")

# 2. 建测试表
with engine.connect() as conn:
    conn.execute("""
        CREATE TABLE IF NOT EXISTS employees (
            id INTEGER PRIMARY KEY,
            name VARCHAR(100),
            department VARCHAR(50),
            salary DECIMAL(10,2),
            hire_date DATE
        )
    """)
    conn.execute("""
        INSERT INTO employees VALUES
        (1, '张三', '技术部', 35000, '2023-01-15'),
        (2, '李四', '产品部', 30000, '2022-06-01'),
        (3, '王五', '技术部', 42000, '2021-03-20'),
        (4, '赵六', '市场部', 25000, '2024-02-10'),
        (5, '钱七', '技术部', 50000, '2020-07-01')
    """)
    conn.commit()

# 3. 创建SQL Agent
db = SQLDatabase(engine)
llm = ChatOpenAI(model="gpt-4o", temperature=0)
toolkit = SQLDatabaseToolkit(db=db, llm=llm)

agent_executor = create_sql_agent(
    llm=llm,
    toolkit=toolkit,
    agent_type=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
    verbose=True,
    max_iterations=10,
    handle_sql_errors=True
)

# 4. 查询
questions = [
    "技术部的平均薪资是多少？",
    "列出薪资最高的员工",
    "2023年入职的员工有哪些？",
]

for q in questions:
    print(f"\n用户: {q}")
    result = agent_executor.invoke({"input": q})
    print(f"回答: {result['output']}")
```

### 6. Ollama私有化部署 + LangChain集成

```python
"""
前置准备：
1. 安装Ollama：https://ollama.ai
2. 拉取模型：ollama pull qwen2.5:7b
3. 启动服务：ollama serve
"""

from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 1. 连接本地Ollama模型
llm = ChatOllama(
    model="qwen2.5:7b",
    temperature=0.3,
    num_predict=2048,
    top_k=10,
    top_p=0.9,
    repeat_penalty=1.1
)

# 2. 简单对话
response = llm.invoke("用一句话解释什么是RAG")
print(response.content)

# 3. 带历史的多轮对话
from langchain_core.messages import HumanMessage, AIMessage, SystemMessage

messages = [
    SystemMessage(content="你是一个AI助教，专注于RAG技术问答。"),
    HumanMessage(content="什么是向量数据库？"),
    AIMessage(content="向量数据库是一种专门用于存储和检索高维向量数据的数据库系统。"),
    HumanMessage(content="PgVector和专门向量库相比有什么优势？")
]
response = llm.invoke(messages)
print(response.content)

# 4. 构建检索链
prompt = ChatPromptTemplate.from_template("""
基于以下参考信息回答问题：

{context}

问题：{question}
""")

chain = prompt | llm | StrOutputParser()

context = """
PgVector是PostgreSQL的向量检索扩展，优势：
1. 不需要额外部署专用向量库，随PgSQL一起
2. 支持ACID事务，与传统字段ACID兼容
3. 两阶段查询：结构化条件过滤 + 向量检索
4. 运维成本低，和现有PgSQL统一管理
"""

result = chain.invoke({
    "context": context,
    "question": "PgVector的主要优势有哪些？"
})
print(result)
```

### 7. MyBatis整合PgVector实现向量检索（Java完整示例）

```java
// ============================================
// 1. pom.xml 依赖
// ============================================
/*
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.1</version>
    </dependency>
</dependencies>
*/

// ============================================
// 2. Entity
// ============================================
public class DocumentVector {
    private Long id;
    private String title;
    private String content;
    private List<Float> embedding; // 768维向量
    // getters & setters omitted for brevity
}

// ============================================
// 3. Mapper
// ============================================
@Mapper
public interface DocumentVectorMapper {
    
    // 插入文档（向量类型处理）
    @Insert("""
        INSERT INTO documents (title, content, embedding)
        VALUES (#{title}, #{content}, #{embedding}::vector)
    """)
    int insert(DocumentVector doc);
    
    // 向量检索：余弦相似度排序
    @Select("""
        SELECT id, title, content,
               1 - (embedding <=> CAST(#{queryVector} AS vector)) AS similarity
        FROM documents
        ORDER BY embedding <=> CAST(#{queryVector} AS vector)
        LIMIT #{topK}
    """)
    @Results({
        @Result(column = "id", property = "id"),
        @Result(column = "title", property = "title"),
        @Result(column = "content", property = "content"),
        @Result(column = "similarity", property = "similarity")
    })
    List<DocumentVector> vectorSearch(@Param("queryVector") String queryVector, 
                                      @Param("topK") int topK);
    
    // 混合检索：全文检索 + 向量检索
    @Select("""
        WITH vector_scores AS (
            SELECT id, title, content,
                   1 - (embedding <=> CAST(#{queryVector} AS vector)) AS vector_score
            FROM documents
            ORDER BY embedding <=> CAST(#{queryVector} AS vector)
            LIMIT #{topK}
        ),
        text_scores AS (
            SELECT id,
                   ts_rank(to_tsvector('simple', content), 
                           plainto_tsquery('simple', #{keyword})) AS text_score
            FROM documents
            WHERE to_tsvector('simple', content) @@ plainto_tsquery('simple', #{keyword})
        )
        SELECT v.id, v.title, v.content,
               COALESCE(#{alpha} * v.vector_score + #{beta} * t.text_score, v.vector_score) AS combined_score
        FROM vector_scores v
        LEFT JOIN text_scores t ON v.id = t.id
        ORDER BY combined_score DESC
        LIMIT #{topK}
    """)
    List<Map<String, Object>> hybridSearch(@Param("queryVector") String queryVector,
                                           @Param("keyword") String keyword,
                                           @Param("topK") int topK,
                                           @Param("alpha") double alpha,
                                           @Param("beta") double beta);
}

// ============================================
// 4. Service
// ============================================
@Service
public class RAGService {
    
    @Autowired
    private DocumentVectorMapper mapper;
    
    // 使用Python Embedding服务获取向量
    public List<DocumentVector> search(String query, int topK) {
        // 1. 调用Python Embedding服务获取向量
        String queryVector = callEmbeddingService(query);
        // 2. 向量检索
        return mapper.vectorSearch(queryVector, topK);
    }
    
    private String callEmbeddingService(String text) {
        // 调用Python侧的embedding接口（如Flask）
        RestTemplate rest = new RestTemplate();
        String url = "http://localhost:5001/embed?text=" + URLEncoder.encode(text);
        return rest.getForObject(url, String.class);
    }
    
    // 混合检索
    public List<DocumentVector> hybridSearch(String query, int topK) {
        String queryVector = callEmbeddingService(query);
        List<Map<String, Object>> results = mapper.hybridSearch(
            queryVector, query, topK, 0.7, 0.3
        );
        // 转换结果
        return results.stream().map(this::convert).collect(Collectors.toList());
    }
}
```

```python
# Python侧Embedding服务
from flask import Flask, request
from sentence_transformers import SentenceTransformer

app = Flask(__name__)
model = SentenceTransformer("BAAI/bge-base-zh-v1.5")

@app.route("/embed")
def embed():
    text = request.args.get("text", "")
    if not text:
        return "[]"
    embedding = model.encode(text)
    # 返回格式如 "[0.123, 0.456, ...]" 的字符串
    return str(embedding.tolist())

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
```

### 8. RAG检索优化（Query Rewriting + HyDE + Hybrid Search）

```python
from typing import List
from sentence_transformers import SentenceTransformer, CrossEncoder
from openai import OpenAI
import numpy as np

class AdvancedRetriever:
    """高级检索器：查询改写 + HyDE + 混合检索"""
    
    def __init__(self, model_name="BAAI/bge-base-zh-v1.5"):
        self.embedder = SentenceTransformer(model_name)
        self.reranker = CrossEncoder("BAAI/bge-reranker-v2-m3")
        self.llm = OpenAI()
    
    def query_rewrite(self, question: str, history: List[str] = None) -> str:
        """查询改写：去指代、补全、扩展"""
        history_text = "\n".join(history[-3:]) if history else "无"
        
        prompt = f"""你是搜索查询优化助手。请将用户的问题改写得更加清晰、完整、适合检索。

对话历史：
{history_text}

原始问题：{question}

请直接输出改写后的查询（不要解释）："""
        
        response = self.llm.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.1
        )
        return response.choices[0].message.content.strip()
    
    def hyde(self, question: str) -> str:
        """HyDE (Hypothetical Document Embeddings)： 
           先生成假设性答案，再用答案的向量检索"""
        prompt = f"请详细回答以下问题（作为一个标准的文档段落）：\n{question}"
        
        response = self.llm.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3
        )
        return response.choices[0].message.content
    
    def multi_query(self, question: str, n: int = 3) -> List[str]:
        """多查询扩展：生成多个角度的查询"""
        prompt = f"请将以下问题改写为{n}个不同的表述方式，每行一个，用于向量检索：\n{question}"
        response = self.llm.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.5
        )
        lines = response.choices[0].message.content.strip().split("\n")
        return [line.strip("- ").strip() for line in lines if line.strip()][:n]
    
    def hybrid_search(self, query: str, documents: List[str], 
                      top_k: int = 5, alpha: float = 0.5) -> List[str]:
        """混合检索：BM25关键词 + 向量语义"""
        # 1. 关键词匹配（简单实现）
        query_terms = set(query.lower().split())
        keyword_scores = []
        for i, doc in enumerate(documents):
            doc_terms = set(doc.lower().split())
            overlap = len(query_terms & doc_terms)
            keyword_scores.append(overlap / max(len(query_terms), 1))
        
        # 2. 语义匹配
        query_emb = self.embedder.encode(query)
        semantic_scores = []
        for doc in documents:
            doc_emb = self.embedder.encode(doc)
            score = np.dot(query_emb, doc_emb) / (np.linalg.norm(query_emb) * np.linalg.norm(doc_emb))
            semantic_scores.append(score)
        
        # 3. 归一化并融合
        k_max, s_max = max(keyword_scores) or 1, max(semantic_scores) or 1
        combined = [
            alpha * (kw / k_max) + (1 - alpha) * (sem / s_max)
            for kw, sem in zip(keyword_scores, semantic_scores)
        ]
        
        # 4. 排序
        indices = sorted(range(len(combined)), key=lambda i: combined[i], reverse=True)
        return [documents[i] for i in indices[:top_k]]
    
    def rerank(self, query: str, documents: List[str], top_k: int = 3) -> List[str]:
        """使用CrossEncoder重排序"""
        pairs = [[query, doc] for doc in documents]
        scores = self.reranker.predict(pairs)
        ranked = sorted(zip(documents, scores), key=lambda x: x[1], reverse=True)
        return [doc for doc, _ in ranked[:top_k]]
```

### 9. 使用Spring AI构建RAG应用（Java）

```xml
<!-- pom.xml 依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.3
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 768
```

```java
// Java Spring AI RAG Service
@Service
public class RagService {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ChatClient chatClient;
    
    public String query(String question) {
        // 1. 检索相关文档
        List<Document> similarDocs = vectorStore.similaritySearch(
            SearchRequest.query(question)
                .withTopK(5)
                .withSimilarityThreshold(0.7)
        );
        
        // 2. 构建上下文
        String context = similarDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
        
        // 3. 生成回答
        return chatClient.prompt()
            .user(u -> u.text("""
                基于以下参考信息回答问题：
                
                {context}
                
                问题：{question}
                """)
                .param("context", context)
                .param("question", question))
            .call()
            .content();
    }
}
```

### 10. 监督数据集构建与微调pipeline

```python
from datasets import Dataset
import json

# 1. 构建SFT数据格式（Qwen格式）
def build_sft_data():
    conversations = [
        {
            "messages": [
                {"role": "system", "content": "你是一个RAG技术专家。"},
                {"role": "user", "content": "什么是RAG中的检索模块？"},
                {"role": "assistant", "content": "RAG检索模块负责从知识库中检索与用户查询相关的文档片段。核心组件包括：查询编码器（Embedding model）、向量索引（如HNSW/IVFFlat）、检索器（Retriever）和重排序器（ReRanker）。"}
            ]
        },
        # ... 更多对话
    ]
    
    with open("sft_data.jsonl", "w", encoding="utf-8") as f:
        for conv in conversations:
            f.write(json.dumps(conv, ensure_ascii=False) + "\n")

# 2. 使用LLaMA-Factory微调
"""
# 命令行方式：
git clone https://github.com/hiyouga/LLaMA-Factory.git
cd LLaMA-Factory

CUDA_VISIBLE_DEVICES=0 python src/train_bash.py \
    --stage sft \
    --model_name_or_path Qwen/Qwen2.5-7B-Instruct \
    --dataset sft_data.jsonl \
    --dataset_dir ./data \
    --template qwen \
    --finetuning_type lora \
    --lora_rank 8 \
    --output_dir ./qwen-sft-rag \
    --per_device_train_batch_size 2 \
    --gradient_accumulation_steps 8 \
    --num_train_epochs 3 \
    --learning_rate 2e-4 \
    --fp16

# 合并LoRA权重：
python src/export_model.py \
    --model_name_or_path Qwen/Qwen2.5-7B-Instruct \
    --adapter_name_or_path ./qwen-sft-rag \
    --template qwen \
    --finetuning_type lora \
    --export_dir ./qwen-sft-rag-merged
"""
```

### 11. Agent平台设计与Multi-Agent协作

```python
from typing import List, Dict, Any
from dataclasses import dataclass
from langchain_openai import ChatOpenAI
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.tools import tool
from langchain_core.prompts import ChatPromptTemplate

@dataclass
class Task:
    """Agent任务"""
    task_id: str
    description: str
    assigned_agent: str = None
    result: str = None
    status: str = "pending"  # pending / running / completed / failed

class AgentRegistry:
    """Agent注册中心"""
    def __init__(self):
        self.agents = {}
    
    def register(self, name: str, agent: AgentExecutor):
        self.agents[name] = agent
    
    def get_agent(self, name: str) -> AgentExecutor:
        return self.agents.get(name)

class Orchestrator:
    """编排器：负责任务分配与结果汇聚"""
    def __init__(self, registry: AgentRegistry):
        self.registry = registry
        self.llm = ChatOpenAI(model="gpt-4o", temperature=0)
    
    def assign_task(self, user_query: str) -> List[Task]:
        """分析用户需求，拆解任务"""
        prompt = f"""分析用户请求，拆解为需要不同Agent完成的子任务。

可用Agent：{list(self.registry.agents.keys())}

用户请求：{user_query}

请输出JSON格式的任务列表：
[{{"agent": "agent_name", "description": "子任务描述"}}]"""
        
        response = self.llm.invoke(prompt)
        tasks_data = json.loads(response.content)
        return [Task(task_id=str(i), description=t["description"], 
                     assigned_agent=t["agent"]) 
                for i, t in enumerate(tasks_data)]
    
    def execute(self, user_query: str) -> Dict[str, Any]:
        tasks = self.assign_task(user_query)
        results = {}
        
        for task in tasks:
            agent = self.registry.get_agent(task.assigned_agent)
            if agent:
                result = agent.invoke({"input": task.description})
                results[task.assigned_agent] = result["output"]
        
        # 汇聚结果
        result_prompt = f"""汇总以下Agent的执行结果，生成完整回答：

用户原问题：{user_query}

各Agent结果：
{json.dumps(results, ensure_ascii=False, indent=2)}
"""
        final_response = self.llm.invoke(result_prompt)
        return {
            "raw_results": results,
            "final_response": final_response.content
        }

# --- 注册Agent ---
registry = AgentRegistry()

rag_agent = create_tool_calling_agent(llm, [retrieval_tool, ...], rag_prompt)
registry.register("rag_agent", AgentExecutor(agent=rag_agent, tools=[retrieval_tool]))

sql_agent = create_tool_calling_agent(llm, [sql_tool, ...], sql_prompt)
registry.register("sql_agent", AgentExecutor(agent=sql_agent, tools=[sql_tool]))

# --- 执行 ---
orchestrator = Orchestrator(registry)
result = orchestrator.execute("查询上月用户增长数据并分析趋势")
print(result["final_response"])
```

### 12. 生产级RAG服务的性能优化

```python
# 1. 多级缓存设计
class RagCache:
    def __init__(self):
        self.query_cache = {}      # 短查询缓存（TTL: 5min）
        self.embedding_cache = {}   # 嵌入缓存（TTL: 24h）
    
    def get_or_compute_embedding(self, text: str, compute_fn):
        """计算前先在缓存中查找"""
        key = hash(text)
        if key in self.embedding_cache:
            return self.embedding_cache[key]
        
        emb = compute_fn(text)
        self.embedding_cache[key] = emb
        return emb

# 2. 异步批处理
import asyncio
from concurrent.futures import ThreadPoolExecutor

async def batch_search(queries: List[str], retriever, batch_size=32):
    """批量处理查询"""
    with ThreadPoolExecutor(max_workers=4) as executor:
        loop = asyncio.get_event_loop()
        tasks = []
        for i in range(0, len(queries), batch_size):
            batch = queries[i:i+batch_size]
            tasks.append(loop.run_in_executor(
                executor, lambda b: [retriever.search(q) for q in b], batch
            ))
        results = await asyncio.gather(*tasks)
    return [item for sublist in results for item in sublist]

# 3. 流式输出
def stream_rag_response(question: str, retriever, llm):
    """流式输出RAG回答"""
    docs = retriever.search(question)
    context = "\n".join([d.text for d in docs])
    
    prompt = f"基于以下信息回答问题：\n{context}\n\n问题：{question}"
    
    for chunk in llm.stream(prompt):
        yield chunk.content  # 逐token输出，减少TTFT（Time To First Token）

# 4. 请求合并
class RequestMerger:
    """相同查询合并（防止缓存击穿）"""
    def __init__(self):
        self._pending = {}
    
    async def execute(self, query: str, search_fn):
        if query not in self._pending:
            self._pending[query] = asyncio.create_task(search_fn(query))
        
        result = await self._pending[query]
        del self._pending[query]
        return result
```

---

## 四、手写代码题（8题）

### 1. 手写余弦相似度计算

```python
import numpy as np
import math

def cosine_similarity(vec_a: list[float], vec_b: list[float]) -> float:
    """计算两个向量的余弦相似度
    
    similarity = cos(θ) = (A·B) / (||A|| * ||B||)
    """
    if len(vec_a) != len(vec_b):
        raise ValueError("向量维度不一致")
    
    dot_product = sum(a * b for a, b in zip(vec_a, vec_b))
    norm_a = math.sqrt(sum(a ** 2 for a in vec_a))
    norm_b = math.sqrt(sum(b ** 2 for b in vec_b))
    
    if norm_a == 0 or norm_b == 0:
        return 0.0
    
    return dot_product / (norm_a * norm_b)

# 测试
a = [1, 2, 3]
b = [4, 5, 6]
print(f"余弦相似度: {cosine_similarity(a, b):.4f}")  # 0.9746
print(f"NumPy验证: {np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)):.4f}")
```

### 2. 手写文本分块器

```python
from typing import List

class RecursiveCharacterTextSplitter:
    """递归字符文本分块器"""
    
    def __init__(
        self,
        chunk_size: int = 500,
        chunk_overlap: int = 50,
        separators: List[str] = None
    ):
        self.chunk_size = chunk_size
        self.chunk_overlap = min(chunk_overlap, chunk_size // 2)
        self.separators = separators or ["\n\n", "\n", "。", "，", " ", ""]
    
    def split_text(self, text: str) -> List[str]:
        """递归分块"""
        chunks = []
        self._split_recursive(text, self.separators, chunks)
        return chunks
    
    def _split_recursive(self, text: str, separators: List[str], chunks: List[str]):
        """递归分割"""
        if len(text) <= self.chunk_size:
            chunks.append(text)
            return
        
        if not separators:
            # 无分隔符可用，强制截断
            chunks.append(text[:self.chunk_size])
            remaining = text[self.chunk_size - self.chunk_overlap:]
            if remaining:
                self._split_recursive(remaining, separators, chunks)
            return
        
        separator = separators[0]
        
        # 尝试用当前分隔符分割
        if separator in text:
            segments = text.split(separator)
            
            current_chunk = ""
            for segment in segments:
                candidate = current_chunk + (separator if current_chunk else "") + segment
                if len(candidate) <= self.chunk_size:
                    current_chunk = candidate
                else:
                    if current_chunk:
                        chunks.append(current_chunk)
                    # 保留重叠部分
                    self._split_recursive(segment, separators[1:], chunks)
                    current_chunk = ""
            
            if current_chunk:
                chunks.append(current_chunk)
        else:
            # 当前分隔符不适用，尝试下一个
            self._split_recursive(text, separators[1:], chunks)
    
    def split_documents(self, texts: List[str]) -> List[str]:
        """批量分块"""
        all_chunks = []
        for text in texts:
            all_chunks.extend(self.split_text(text))
        return all_chunks

# 测试
splitter = RecursiveCharacterTextSplitter(chunk_size=100, chunk_overlap=20)
text = "RAG（检索增强生成）是一种结合检索与生成的技术。它通过向量数据库检索相关文档。然后将检索结果作为上下文输入LLM生成回答。这种方法可以有效缓解幻觉问题。"
chunks = splitter.split_text(text)
for i, chunk in enumerate(chunks):
    print(f"Chunk {i+1} ({len(chunk)}字符): {chunk}")
```

### 3. 手写简单向量检索（暴力搜索）

```python
import numpy as np

class BruteForceVectorSearch:
    """暴力搜索向量检索器"""
    
    def __init__(self, dimension: int = 768, metric: str = "cosine"):
        self.dimension = dimension
        self.metric = metric  # cosine / euclidean / dot
        self.vectors = []
        self.documents = []
    
    def add(self, vector: list[float], document: str = ""):
        """添加向量和文档"""
        self.vectors.append(np.array(vector))
        self.documents.append(document)
    
    def add_batch(self, vectors: list[list[float]], documents: list[str]):
        """批量添加"""
        for v, d in zip(vectors, documents):
            self.add(v, d)
    
    def _distance(self, v1: np.ndarray, v2: np.ndarray) -> float:
        """计算距离"""
        if self.metric == "cosine":
            return 1 - np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))
        elif self.metric == "euclidean":
            return np.linalg.norm(v1 - v2)
        elif self.metric == "dot":
            return -np.dot(v1, v2)  # 负值表示越大越相似
        else:
            raise ValueError(f"不支持的距离度量: {self.metric}")
    
    def search(self, query_vector: list[float], top_k: int = 5) -> list[dict]:
        """最近邻搜索"""
        if not self.vectors:
            return []
        
        qv = np.array(query_vector)
        distances = [self._distance(qv, v) for v in self.vectors]
        
        # 排序取Top-K
        top_indices = np.argsort(distances)[:top_k]
        
        results = []
        for idx in top_indices:
            similarity = 1 - distances[idx] if self.metric == "cosine" else -distances[idx]
            results.append({
                "index": idx,
                "distance": distances[idx],
                "similarity": similarity,
                "document": self.documents[idx]
            })
        
        return results

# 测试
searcher = BruteForceVectorSearch(dimension=4, metric="cosine")

searcher.add([0.1, 0.2, 0.3, 0.4], "RAG技术文档")
searcher.add([0.9, 0.8, 0.7, 0.6], "LangChain框架")
searcher.add([0.5, 0.5, 0.5, 0.5], "PgVector教程")

results = searcher.search([0.12, 0.22, 0.32, 0.42], top_k=2)
for r in results:
    print(f"文档: {r['document']}, 相似度: {r['similarity']:.4f}")
```

### 4. 手写SQL向量查询

```sql
-- ============================================
-- PgVector向量检索SQL
-- ============================================

-- 1. 创建扩展和表
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_docs (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(64) NOT NULL,          -- 文档唯一标识
    title VARCHAR(500),                     -- 标题
    content TEXT NOT NULL,                  -- 文档内容
    embedding vector(768),                  -- 768维向量
    metadata JSONB DEFAULT '{}',            -- 元数据（来源、权限等）
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    -- 权限控制字段
    tenant_id VARCHAR(32) DEFAULT 'default',
    access_level INT DEFAULT 0,             -- 0:公开 1:内部 2:机密
    
    UNIQUE(doc_id)
);

-- 2. 创建索引
-- IVFFlat索引（数据量大时使用lists = sqrt(rows)）
CREATE INDEX idx_knowledge_ivfflat 
    ON knowledge_docs USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- HNSW索引（召回率优先）
-- CREATE INDEX idx_knowledge_hnsw 
--     ON knowledge_docs USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 200);

-- 3. 基础向量检索（带权限过滤）
SELECT 
    id,
    doc_id,
    title,
    content,
    1 - (embedding <=> '[...query_vector...]'::vector) AS similarity
FROM knowledge_docs
WHERE 
    -- 权限控制
    (access_level <= 0 OR tenant_id = 'current_tenant')
    -- 可选metadata过滤
    AND metadata->>'category' = 'technical'
ORDER BY embedding <=> '[...query_vector...]'::vector
LIMIT 5;

-- 4. 混合检索（全文+向量）
-- 需要安装zhparser或jieba用于中文分词
-- CREATE TEXT SEARCH CONFIGURATION chinese (PARSER = zhparser);
-- ALTER TEXT SEARCH CONFIGURATION chinese ADD MAPPING FOR n,v,a,i,e,l WITH simple;

WITH 
vector_results AS (
    SELECT 
        id, 
        content,
        1 - (embedding <=> '[...query_vector...]'::vector) AS vector_score
    FROM knowledge_docs
    ORDER BY embedding <=> '[...query_vector...]'::vector
    LIMIT 100
),
keyword_results AS (
    SELECT 
        id,
        content,
        ts_rank(
            to_tsvector('chinese', content),
            plainto_tsquery('chinese', '检索关键词')
        ) AS keyword_score
    FROM knowledge_docs
    WHERE to_tsvector('chinese', content) @@ plainto_tsquery('chinese', '检索关键词')
)
SELECT 
    COALESCE(v.id, k.id) AS id,
    COALESCE(v.content, k.content) AS content,
    COALESCE(v.vector_score, 0) * 0.7 + COALESCE(k.keyword_score, 0) * 0.3 AS combined_score
FROM vector_results v
FULL OUTER JOIN keyword_results k ON v.id = k.id
ORDER BY combined_score DESC
LIMIT 10;

-- 5. 分页检索 + 游标
-- 深度分页优化：使用游标而非OFFSET
SELECT id, title, content, similarity
FROM (
    SELECT id, title, content,
           1 - (embedding <=> '[...query_vector...]'::vector) AS similarity
    FROM knowledge_docs
    WHERE id > :last_seen_id  -- 游标
    ORDER BY embedding <=> '[...query_vector...]'::vector
) sub
LIMIT 20;
```

### 5. 手写JSON Schema结构化输出定义

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "JobCandidate",
  "description": "候选人简历结构化信息提取",
  "type": "object",
  "properties": {
    "basic_info": {
      "type": "object",
      "properties": {
        "name": { "type": "string", "description": "姓名" },
        "age": { "type": "integer", "minimum": 16, "maximum": 100 },
        "phone": { "type": "string", "pattern": "^1[3-9]\\d{9}$" },
        "email": { "type": "string", "format": "email" },
        "years_experience": { "type": "number", "minimum": 0 }
      },
      "required": ["name", "years_experience"]
    },
    "education": {
      "type": "object",
      "properties": {
        "degree": { 
          "type": "string", 
          "enum": ["高中", "大专", "本科", "硕士", "博士", "其他"]
        },
        "school": { "type": "string" },
        "major": { "type": "string" },
        "graduation_year": { "type": "integer", "minimum": 1990, "maximum": 2030 }
      }
    },
    "skills": {
      "type": "array",
      "items": { "type": "string" },
      "minItems": 1,
      "description": "技能列表"
    },
    "work_experience": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "company": { "type": "string" },
          "position": { "type": "string" },
          "start_date": { "type": "string", "pattern": "^\\d{4}-\\d{2}$" },
          "end_date": { "type": "string", "pattern": "^\\d{4}-\\d{2}$|至今" },
          "description": { "type": "string" }
        }
      }
    },
    "ai_related_score": {
      "type": "object",
      "properties": {
        "total_score": { "type": "integer", "minimum": 0, "maximum": 100 },
        "rag_score": { "type": "integer", "minimum": 0, "maximum": 100 },
        "agent_score": { "type": "integer", "minimum": 0, "maximum": 100 },
        "ml_basics_score": { "type": "integer", "minimum": 0, "maximum": 100 }
      },
      "description": "AI能力评分"
    }
  },
  "required": ["basic_info", "skills", "ai_related_score"],
  "additionalProperties": false
}
```

### 6. 手写Function Calling Schema定义

```python
# ============================================
# OpenAI Tool Schema定义
# ============================================

# Tool 1: 文档检索
document_search_tool = {
    "type": "function",
    "function": {
        "name": "search_documents",
        "description": "从知识库中检索与查询相关的文档",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "搜索查询语句"
                },
                "top_k": {
                    "type": "integer",
                    "description": "返回结果数量",
                    "minimum": 1,
                    "maximum": 20,
                    "default": 5
                },
                "filters": {
                    "type": "object",
                    "description": "过滤条件",
                    "properties": {
                        "category": {"type": "string"},
                        "date_from": {"type": "string", "format": "date"},
                        "date_to": {"type": "string", "format": "date"},
                        "tenant_id": {"type": "string"}
                    }
                }
            },
            "required": ["query"]
        }
    }
}

# Tool 2: 代码执行
code_executor_tool = {
    "type": "function",
    "function": {
        "name": "execute_python",
        "description": "在沙箱环境中执行Python代码（用于数据分析、计算等）",
        "parameters": {
            "type": "object",
            "properties": {
                "code": {
                    "type": "string",
                    "description": "要执行的Python代码"
                },
                "timeout": {
                    "type": "integer",
                    "description": "超时时间（秒）",
                    "default": 30,
                    "maximum": 60
                }
            },
            "required": ["code"]
        },
        "strict": True
    }
}

# Tool 3: 数据库查询
database_query_tool = {
    "type": "function",
    "function": {
        "name": "query_database",
        "description": "执行只读SQL查询（SELECT语句）",
        "parameters": {
            "type": "object",
            "properties": {
                "sql": {
                    "type": "string",
                    "description": "SELECT SQL查询语句"
                },
                "params": {
                    "type": "array",
                    "description": "查询参数",
                    "items": {"type": "string"}
                },
                "limit": {
                    "type": "integer",
                    "description": "结果数量限制",
                    "default": 50,
                    "maximum": 1000
                }
            },
            "required": ["sql"]
        },
        "strict": True
    }
}

# ============================================
# LangChain Tool @dataclass定义
# ============================================
from dataclasses import dataclass, field
from typing import Optional

@dataclass
class SearchDocumentsInput:
    """搜索文档的工具输入"""
    query: str
    top_k: int = 5
    filters: Optional[dict] = None

@dataclass
class ExecutePythonInput:
    """执行Python代码的工具输入"""
    code: str
    timeout: int = 30
    requirements: list[str] = field(default_factory=list)

@dataclass
class QueryDatabaseInput:
    """数据库查询的工具输入"""
    sql: str
    params: Optional[list] = None
    readonly: bool = True
```

### 7. 手写Embedding服务（Python Flask）

```python
from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import numpy as np
import time
from functools import lru_cache

app = Flask(__name__)

# 加载模型（全局单例）
model = SentenceTransformer("BAAI/bge-base-zh-v1.5")

# 简单缓存
@lru_cache(maxsize=10000)
def encode_text(text: str) -> np.ndarray:
    return model.encode(text)

@app.route("/embed", methods=["POST"])
def embed():
    """批量获取文本向量"""
    data = request.json
    if not data or "texts" not in data:
        return jsonify({"error": "请提供texts字段"}), 400
    
    texts = data["texts"]
    if isinstance(texts, str):
        texts = [texts]
    
    if len(texts) > 100:
        return jsonify({"error": "单次最多支持100条文本"}), 400
    
    start = time.time()
    
    # 批量编码
    if data.get("use_cache", True):
        embeddings = [encode_text(t).tolist() for t in texts]
    else:
        embeddings = model.encode(texts).tolist()
    
    elapsed = time.time() - start
    
    return jsonify({
        "embeddings": embeddings,
        "dimension": len(embeddings[0]) if embeddings else 0,
        "count": len(embeddings),
        "time_ms": round(elapsed * 1000, 2),
        "model": "BAAI/bge-base-zh-v1.5"
    })

@app.route("/search", methods=["POST"])
def search():
    """文本-to-文本检索（内部走向量相似度）"""
    data = request.json
    query = data["query"]
    documents = data["documents"]
    top_k = data.get("top_k", 5)
    
    query_emb = model.encode(query)
    doc_embs = model.encode(documents)
    
    scores = np.dot(doc_embs, query_emb) / (
        np.linalg.norm(doc_embs, axis=1) * np.linalg.norm(query_emb)
    )
    
    top_indices = np.argsort(scores)[-top_k:][::-1]
    
    results = [
        {
            "text": documents[i],
            "score": float(scores[i])
        }
        for i in top_indices
    ]
    
    return jsonify({"results": results})

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": "BAAI/bge-base-zh-v1.5"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, workers=4)
```

### 8. 手写RAG评估脚本（RAGAS简化版）

```python
"""
简化版RAG评估：计算Faithfulness和Answer Relevancy
"""

import json
from typing import List, Dict
from openai import OpenAI

class SimpleRAGEvaluator:
    """简化版RAG评估器"""
    
    def __init__(self, llm_model="gpt-4o-mini"):
        self.llm = OpenAI()
        self.model = llm_model
    
    def evaluate_faithfulness(self, answer: str, context: str) -> Dict:
        """评估回答是否忠实于上下文"""
        prompt = f"""评估AI回答是否忠实于给定的参考上下文。

参考上下文：
{context}

AI回答：
{answer}

请分析：
1. 回答中每个观点是否能在参考上下文中找到依据
2. 是否存在超出上下文的编造内容

输出JSON格式：
{{
    "faithfulness_score": 0.0-1.0,
    "supported_claims": ["有依据的观点1", ...],
    "unsupported_claims": ["无依据的观点1", ...],
    "analysis": "详细分析说明"
}}
"""
        response = self.llm.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
            temperature=0
        )
        return json.loads(response.choices[0].message.content)
    
    def evaluate_relevancy(self, question: str, answer: str) -> Dict:
        """评估回答与问题的相关性"""
        prompt = f"""评估AI回答是否紧密围绕用户问题。

用户问题：{question}

AI回答：{answer}

请判断：
1. 回答是否直接回答了问题
2. 是否有无关内容
3. 是否完整覆盖了问题的所有方面

输出JSON格式：
{{
    "relevancy_score": 0.0-1.0,
    "directness": "direct/partial/off_topic",
    "missing_aspects": ["未覆盖的方面", ...],
    "irrelevant_content": ["无关内容", ...],
    "analysis": "详细分析说明"
}}
"""
        response = self.llm.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
            temperature=0
        )
        return json.loads(response.choices[0].message.content)
    
    def evaluate(self, question: str, answer: str, context: str = None) -> Dict:
        """综合评估"""
        faithfulness = self.evaluate_faithfulness(answer, context) if context else {"faithfulness_score": "N/A"}
        relevancy = self.evaluate_relevancy(question, answer)
        
        return {
            "question": question,
            "faithfulness": faithfulness,
            "relevancy": relevancy,
            "ragas_score": (
                faithfulness.get("faithfulness_score", 0) + 
                relevancy.get("relevancy_score", 0)
            ) / 2 if isinstance(faithfulness.get("faithfulness_score"), (int, float)) else None
        }

# 使用示例
evaluator = SimpleRAGEvaluator()

question = "什么是RAG技术？"
answer = "RAG是一种检索增强生成技术，它结合了信息检索和文本生成。"
context = "RAG（Retrieval-Augmented Generation）检索增强生成是一种将信息检索与文本生成相结合的技术架构。"

result = evaluator.evaluate(question, answer, context)
print(json.dumps(result, ensure_ascii=False, indent=2))
```

---

## 五、系统设计题（5题）

### 1. 设计企业级RAG知识库系统

**系统架构：**

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    API Gateway   │ ──> │   RAG Service    │ ──> │   LLM Provider  │
│  (Kong/Gateway)  │     │  (Java/Spring)   │     │  (OpenAI/本地)  │
└─────────────────┘     └────────┬─────────┘     └─────────────────┘
                                  │
         ┌────────────────────────┼────────────────────────┐
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Document        │     │  Vector Store   │     │   Cache Layer   │
│  Processing      │     │  (PgVector)     │     │  (Redis/Caffeine)│
│  (解析→分块→向量化) │     │  HNSW Index     │     │  查询缓存/Embedding│
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

**数据库设计：**

```sql
-- 文档表
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(500),
    source_type VARCHAR(32),    -- pdf/word/html/markdown
    raw_content TEXT,
    status VARCHAR(16) DEFAULT 'pending',  -- pending/processing/ready/failed
    tenant_id VARCHAR(32) NOT NULL,
    creator VARCHAR(64),
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 文档块表
CREATE TABLE doc_chunks (
    id BIGSERIAL PRIMARY KEY,
    chunk_id VARCHAR(64) UNIQUE NOT NULL,
    doc_id VARCHAR(64) REFERENCES documents(doc_id),
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    token_count INT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_chunks_doc_id ON doc_chunks(doc_id);
CREATE INDEX idx_chunks_hnsw ON doc_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);

-- 访问权限表
CREATE TABLE doc_permissions (
    id BIGSERIAL PRIMARY KEY,
    doc_id VARCHAR(64) REFERENCES documents(doc_id),
    tenant_id VARCHAR(32) NOT NULL,
    permission_level INT DEFAULT 0,  -- 0:read 1:write 2:admin
    UNIQUE(doc_id, tenant_id)
);
```

**核心接口设计：**

```java
// REST API
POST   /api/v1/documents          // 上传文档
DELETE /api/v1/documents/{id}     // 删除文档
POST   /api/v1/rag/query          // RAG查询
POST   /api/v1/rag/feedback       // 反馈标注（打通评估闭环）

// RAG查询请求体
{
    "question": "什么是RAG？",
    "top_k": 5,
    "filters": {
        "tenant_id": "tech_dept",
        "date_from": "2025-01-01"
    },
    "options": {
        "enable_hyde": true,
        "enable_rerank": true,
        "stream": true
    }
}

// RAG查询响应体（流式）
{
    "answer": "RAG是...",         // 最终答案
    "chunks": [                    // 检索来源（用于溯源）
        {"chunk_id": "...", "content": "...", "score": 0.95, "source": "..."}
    ],
    "metadata": {
        "latency_ms": 1250,
        "model": "gpt-4o"
    }
}
```

### 2. 设计AI Agent平台架构

```
┌─────────────────────────────────────────────────────┐
│                    Agent Platform                     │
├─────────────────────────────────────────────────────┤
│                   控制平面                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ Agent    │  │ 工具     │  │ 监控 & 日志       │   │
│  │ 注册中心  │  │ 市场     │  │ Prometheus+Grafana│   │
│  └──────────┘  └──────────┘  └──────────────────┘   │
├─────────────────────────────────────────────────────┤
│                   执行平面                            │
│  ┌──────────────────┐  ┌──────────────────────┐     │
│  │  Agent 执行器     │  │  沙箱环境             │     │
│  │  (LangChain)     │  │  (gVisor/Firecracker)│     │
│  └──────────────────┘  └──────────────────────┘     │
├─────────────────────────────────────────────────────┤
│                   数据平面                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ RAG引擎   │  │ 记忆模块  │  │ 会话管理          │   │
│  │ (PgVector)│  │ (Redis)  │  │ (PgSQL)          │   │
│  └──────────┘  └──────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────┘
```

**Agent定义规范（JSON配置）：**

```json
{
  "agent_id": "customer-service-v1",
  "name": "智能客服Agent",
  "description": "处理客户咨询、工单创建、FAQ回答",
  "model": {
    "provider": "openai",
    "name": "gpt-4o",
    "temperature": 0.3,
    "max_tokens": 2048
  },
  "tools": [
    {"name": "rag_search", "config": {"top_k": 5}},
    {"name": "create_ticket", "auth_required": true},
    {"name": "query_order", "auth_required": true}
  ],
  "memory": {
    "type": "buffer",
    "max_tokens": 4000,
    "ttl_seconds": 3600
  },
  "rate_limit": {
    "rpm": 60,
    "concurrency": 5
  },
  "permissions": ["read:knowledge_base", "write:tickets"],
  "max_iterations": 10,
  "fallback_response": "抱歉，我无法处理该请求，将转接人工客服。"
}
```

### 3. 设计模型微调与部署Pipeline

```
┌─────────────────────────────────────────────────────────┐
│                    数据准备阶段                           │
│  数据采集 → 清洗 → 标注 → 格式转换(ShareGPT/alpaca)       │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│                    训练阶段                              │
│  基础模型下载 → LoRA配置 → 数据加载 → 训练(Trainer)       │
│  → 评估(Perplexity/BLEU) → Checkpoint保存               │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│                    部署阶段                              │
│  LoRA权重合并 → 模型量化(GGUF/AWQ/GPTQ) → vLLM启动       │
│  → API暴露 → 监控(Prometheus) → 日志(Loki)              │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────┐
│                   持续改进闭环                            │
│  用户反馈 → Bad Case收集 → 数据增强 → 重新训练            │
└─────────────────────────────────────────────────────────┘
```

**Pipeline配置（Kubernetes + Argo Workflow）：**

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Workflow
metadata:
  name: model-finetune-pipeline
spec:
  entrypoint: full-pipeline
  templates:
  - name: full-pipeline
    steps:
    - - name: data-prep
        template: data-preparation
    - - name: finetune
        template: lora-finetune
    - - name: evaluate
        template: model-evaluation
    - - name: merge-deploy
        template: merge-and-deploy

  - name: data-preparation
    container:
      image: python:3.11
      command: ["python", "/scripts/prepare_data.py"]
      resources:
        requests:
          cpu: "4"
          memory: "8Gi"

  - name: lora-finetune
    container:
      image: pytorch/pytorch:2.1.0-cuda12.1
      command: ["torchrun", "--nproc_per_node=4", "/scripts/finetune.py"]
      resources:
        requests:
          nvidia.com/gpu: 4
          memory: "128Gi"

  - name: model-evaluation
    container:
      image: python:3.11
      command: ["python", "/scripts/evaluate.py"]
      
  - name: merge-and-deploy
    container:
      image: python:3.11
      command: ["python", "/scripts/merge_deploy.py"]
```

### 4. 设计简历筛选与面试评估AI系统

```text
系统流程：
1. 简历解析 → 结构化输出（Pydantic Schema）
2. 岗位JD匹配 → 技能差距分析
3. 面试题目生成（基于岗位要求和简历）
4. 面试答案评估（RAG + LLM Judge）
5. 综合评分报告生成
```

```java
// 核心实体
public class ResumeEvaluation {
    private Resume resume;
    private JobRequirement jobReq;
    private Double matchScore;        // 0-100 匹配度
    private SkillGapAnalysis gaps;    // 技能差距分析
    private List<InterviewQuestion> questions;  // 面试题
    private String summary;           // 评估总结
}

public class SkillGapAnalysis {
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> partiallyMatchedSkills;
    private String recommendedLearningPath;
}

public class InterviewQuestion {
    private String category;     // technical/behavioral/situational
    private String difficulty;   // easy/medium/hard
    private String question;
    private String expectedAnswer;
    private String evaluationCriteria;
}
```

### 5. 设计混合检索系统

```text
架构：

                 用户查询
                    │
                    ▼
            ┌──────────────┐
            │  Query Router │ ── 判断查询类型
            └──────┬───────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   ┌────────┐ ┌────────┐ ┌────────┐
   │ 语义检索 │ │ 关键词  │ │ 结构化  │
   │(Embedding)│ │(BM25)  │ │ (SQL)  │
   └────┬───┘ └────┬───┘ └────┬───┘
        │          │          │
        └──────────┼──────────┘
                   ▼
            ┌──────────────┐
            │    Score      │
            │    Fusion     │  ── RRF(Reciprocal Rank Fusion)
            └──────┬───────┘
                   ▼
            ┌──────────────┐
            │   ReRanker   │  ── CrossEncoder
            └──────┬───────┘
                   ▼
              最终结果
```

**RRF融合算法：**

```python
def reciprocal_rank_fusion(
    rankings: list[list[str]],  # 多个检索结果列表
    k: int = 60                  # RRF常数
) -> list[tuple[str, float]]:
    """RRF融合：倒序排名加权"""
    scores = {}
    
    for rank_list in rankings:
        for rank, doc_id in enumerate(rank_list):
            if doc_id not in scores:
                scores[doc_id] = 0
            scores[doc_id] += 1 / (k + rank + 1)
    
    # 按分数降序排列
    return sorted(scores.items(), key=lambda x: x[1], reverse=True)

# 使用
semantic_results = ["doc1", "doc2", "doc3", "doc4", "doc5"]
keyword_results = ["doc3", "doc5", "doc1", "doc7", "doc9"]
sql_results = ["doc5", "doc8", "doc1", "doc3", "doc6"]

fused = reciprocal_rank_fusion([semantic_results, keyword_results, sql_results])
for doc_id, score in fused:
    print(f"{doc_id}: {score:.4f}")
```

---

## 六、常见坑点与最佳实践（15个坑点）

| # | 坑点 | 现象 | 原因 | 最佳实践 | 工程类别 |
|---|------|------|------|---------|---------|
| 1 | **向量维度不匹配** | 搜索返回0结果或异常 | Embedding模型输出维度与数据库定义维度不一致 | 始终用代码确认维度：`model.get_sentence_embedding_dimension()` | 部署 |
| 2 | **IVFFlat索引参数不合理** | 召回率极低 | `lists`参数设太大（如百万级数据设lists=4） | `lists = sqrt(rows)`，`probes = 10-20` | 数据库 |
| 3 | **HNSW内存溢出** | OOM或响应极慢 | ef_construction设过大，或数据量超出内存 | `m=16, ef_construction=200`开始调优 | 部署 |
| 4 | **中文分词不生效** | 关键词检索结果差 | PgSQL未配置中文分词器（zhparser/jieba） | 安装zhparser，或用jieba在应用层分词 | RAG |
| 5 | **Embedding模型过时** | 语义检索效果差 | 仍用text2vec-base-chinese等老旧模型 | 换BGE-m3 / bge-large-zh-v1.5 | RAG |
| 6 | **LLM输出格式不稳定** | Pydantic解析失败 | 模型没有严格遵循JSON格式输出 | 使用`with_structured_output(method="function_calling")`，或加`strict=True`断言 | LangChain |
| 7 | **Function Calling多轮循环** | Agent陷入无限循环 | 没有设置`max_iterations` | 设置`max_iterations=10`，添加`handle_parsing_errors` | Agent |
| 8 | **RAG上下文超长** | Token浪费，甚至超max_context | 检索结果过多，未做裁剪 | Top-K控制在3-5个，并按相关性裁剪 | RAG |
| 9 | **并发过高导致数据库Crash** | 向量检索耗时激增 | 没有限流和连接池 | 配置PgSQL连接池；vLLM设`max_num_seqs`；加Redis缓存 | 部署 |
| 10 | **Streaming模式下引用丢失** | 流式输出没有来源标注 | Streaming使得后处理逻辑复杂 | 先做RAG检索，再流式输出，引用信息放在响应头部 | 架构 |
| 11 | **微调数据集分布不均** | 模型在某些场景表现差 | 训练数据中某些类别太少 | 训练前分析数据分布，做数据增强或重采样 | 微调 |
| 12 | **模型量化精度损失** | 推理结果质量下降 | INT4量化参数未校准 | 用GPTQ/AWQ带校准集量化；关键场景用FP16 | 部署 |
| 13 | **Token消耗不计成本** | API账单爆炸 | 开发环境未限制大模型调用 | 设用户/接口级别限额（Rate Limit）；简单查询用Mini模型 | 成本 |
| 14 | **多租户数据隔离失败** | A用户搜到B用户数据 | 向量检索SQL未加tenant_id过滤 | 每张表加tenant_id列，所有检索SQL强制WHERE tenant_id=xxx | 架构 |
| 15 | **不加System Prompt** | 回答天马行空 | 仅给用户问题，未设定角色和约束 | 设计完善的System Prompt模板，包含角色、要求、输出格式 | LLM |

---

## 七、面试回答模板（Top 5高频题）

### 1. "RAG项目中向量数据库如何选型？PgVector适用场景？"

**回答框架：**

> RAG项目中的向量数据库选型需要综合考虑技术栈、数据规模、运维能力和预算。
>
> **选型决策：**
> 1. 如果团队以 Java 为主、已经使用 PostgreSQL，优先选择 **PgVector**——无需引入新的基础设施，降本增效
> 2. 如果数据规模超过千万级，需要分布式扩展，选择 **Milvus**
> 3. 如果是快速验证原型，选择 **Chroma** 或 **Pinecone**
>
> **PgVector的适用场景：**
> - 中小企业 RAG 项目（百万级文档量）
> - Java + Spring Boot 技术栈已有 PgSQL
> - 需要同时支持结构化查询和向量检索（混合检索有天然优势）
> - 对 ACID 事务有要求的场景（如知识库管理后台）
>
> **PgVector的局限性：**
> - 分布式能力依赖 PgSQL 扩展（Citus/Pgpool），不如 Milvus 原生
> - 千万级以上数据量 HNSW 索引构建较慢
> - 无原生 GPU 加速（但通常 Embedding 在应用层，检索在 CPU 上足够）

### 2. "Function Calling的原理和实现方式？"

**回答框架：**

> Function Calling（工具调用）是让 LLM 能够根据用户输入，"选择并调用"外部工具函数的机制。需要指出的是，**模型并不真正执行函数**，而是输出结构化的函数调用指令（tool_calls），由应用层解析并执行。
>
> **执行流程（4步）：**
> 1. **工具定义（Schema）**：将函数签名以 JSON Schema 格式传入模型
> 2. **模型决策**：根据用户意图，模型判断是否需要调用工具，输出 tool_calls（包含函数名和参数）
> 3. **应用执行**：解析 tool_calls，调用本地函数，返回结果
> 4. **结果整合**：将工具返回结果再次发给模型，生成最终自然语言回答
>
> **实现方式对比：**
> - **OpenAI原生**：直接在 API 参数中传 tools 数组
> - **LangChain Agent**：用 `@tool` 装饰器 + `create_tool_calling_agent` + `AgentExecutor`
> - **Spring AI**：Java 中 `@Tool` 注解 + `ToolCallback`
>
> **注意事项：**
> - 工具描述要精确（影响到模型选择正确工具的准确率）
> - 一定要设置 `max_iterations`（防止无限循环）
> - 生产环境做超时和错误处理

### 3. "LangChain中的结构化输出有哪些方法？"

**回答框架：**

> LangChain 支持多种结构化输出方式，核心目的是将模型的文本输出解析为类型化的数据结构。
>
> **主要方法：**
> 1. **PydanticOutputParser**：定义 Pydantic 模型，parser 自动从文本提取。适合需要字段校验的场景
> 2. **JsonOutputParser**：只需指定 JSON Schema，轻量级方案
> 3. **with_structured_output()**：v0.3+ 推荐方式，一行代码完成，支持 method="function_calling" 或不指定
> 4. **OutputFixingParser**：在 Parser 外层套一层，当第一次解析失败时让 LLM 自动修复
>
> **选型建议：**
> ```python
> # 新项目推荐（最稳定、字段校验最强）
> structured_llm = llm.with_structured_output(MyPydanticModel)
> result = structured_llm.invoke("xxx")
>
> # 需要错误自愈的场景
> from langchain.output_parsers import OutputFixingParser
> fixing_parser = OutputFixingParser.from_llm(parser=pydantic_parser, llm=llm)
> ```
>
> **底层原理：** `method="function_calling"` 时，LangChain 自动将 Pydantic/JSON Schema 转为 Function Calling schema；不指定时使用 JSON mode。Function Calling 方式更稳定，推荐作为首选。

### 4. "AI大模型岗位需要哪些能力？学习路线如何规划？"

**回答框架：**

> **核心能力矩阵：**
>
> 硬技能（60%）：
> - 编程：Python 必须精通（PyTorch/FastAPI/LangChain），Java 加分（Spring AI/工程团队配合）
> - AI基础：Transformer 架构、Attention 机制、Embedding 原理
> - RAG工程：向量数据库、文档解析、检索优化、评估体系
> - 模型部署：vLLM/Ollama、LoRA微调、模型量化
>
> 软技能（40%）：
> - 快速学习能力（AI 领域月月更新）
> - 工程落地思维（不是 Demo，是生产级）
> - 业务理解力（把 AI 能力转化为业务价值）
>
> **成长路径（以年薪60W+为目标）：**
>
> **阶段1：入门（0-6个月）**
> - Python基础 → LangChain基础 → 调用 OpenAI API 做简单 RAG
> - 目标：独立搭建一个简单的 RAG Demo
>
> **阶段2：工程化（6-12个月）**
> - 学习 pgvector + Spring AI 整合 → Function Calling → Agent 开发
> - 深入学习 Embedding 和重排序
> - 目标：搭建企业级 RAG 系统，处理权限、延迟、成本问题
>
> **阶段3：进阶（12-24个月）**
> - LoRA 微调实战 → 模型量化与部署（vLLM/TGI）
> - 参与开源项目（LangChain/LlamaIndex）
> - 目标：具备独/立负责 AI 模块的能力
>
> **2025年一线城市薪资参考：**
> - 初级（1年）：30-45W
> - 中级（2-3年）：45-70W
> - 高级（3-5年）：70-100W
> - 专家/架构（5年+）：100-150W+

### 5. "模型私有化部署方案有哪些？各有何优缺点？"

**回答框架：**

> 目前主流方案有四种：
>
> **1. Ollama（推荐入门和开发环境）**
> - 优点：一键安装、模型管理简单、兼容 OpenAI API
> - 缺点：生产高并发不支持、无动态批处理
> - 适用：本地开发、小团队内部使用
>
> **2. vLLM（推荐生产环境）**
> - 优点：PagedAttention 极大降低显存、连续批处理、高吞吐
> - 缺点：仅支持特定模型架构、配置较复杂
> - 适用：高并发生产环境、在线推理服务
>
> **3. TGI（HuggingFace）**
> - 优点：与 HF 生态深度融合、安全防护机制
> - 缺点：不支持 vLLM 的 PagedAttention，吞吐略低
> - 适用：HF 生态部署、企业级场景
>
> **4. Triton + TensorRT-LLM（NVIDIA 生态）**
> - 优点：极致性能、多框架支持（PyTorch/TF/ONNX）
> - 缺点：部署最复杂、NVIDIA 卡独占
> - 适用：大规模 GPU 集群、延迟要求极致的场景
>
> **选型建议：**
> ```
> 消费级显卡(4090) → Ollama
> 单A100/A800 → vLLM
> 多卡集群+A100 → vLLM 或 Triton+TRT-LLM
> HF深度用户 → TGI
> CPU/边缘 → llama.cpp
> ```
>
> **关键指标对比：** 假设部署 Qwen2.5-7B
> - vLLM：吞吐 ~1000 tokens/s/A100，首token延迟 60ms
> - Ollama：吞吐 ~200 tokens/s/4090，首token延迟 150ms
> - 量化(AWQ 4bit)：显存需求降低 60%，精度损失约 1-2%

---

## 八、快速查漏补缺Checklist（30项）

### 基础理论（6项）
- [ ] 理解RAG完整工作流程（数据准备→索引→检索→生成）
- [ ] 知道向量数据库与传统数据库的区别
- [ ] 理解Function Calling的执行流程（模型→Tool→执行→整合）
- [ ] 理解Embedding模型与LLM的区别
- [ ] 知道Transformer的Attention机制核心公式
- [ ] 了解大模型幻觉的类型和缓解方案

### PgVector与向量检索（5项）
- [ ] 能写出CREATE EXTENSION vector和建表SQL
- [ ] 理解IVFFlat和HNSW索引的区别与适用场景
- [ ] 能写出带余弦距离的SELECT查询
- [ ] 知道如何在Java/MyBatis中调用PgVector
- [ ] 理解混合检索（关键词+向量）的实现方式

### LangChain与Function Calling（4项）
- [ ] 能写出PydanticOutputParser或with_structured_output示例
- [ ] 能使用LangChain创建Tool-Calling Agent
- [ ] 理解LLM vs ChatModel的区别
- [ ] 了解LangChain Model I/O三层结构

### 微调与部署（4项）
- [ ] 理解LoRA微调的核心原理（低秩分解）
- [ ] 知道vLLM / Ollama / TGI的区别与选型
- [ ] 了解模型量化（AWQ/GPTQ）的基本原理
- [ ] 了解全量微调 vs LoRA vs QLoRA的区别

### 工程化与架构（5项）
- [ ] 了解企业RAG面临的6大挑战（数据更新/权限/延迟等）
- [ ] 理解Hybrid Search的RRF融合算法
- [ ] 了解ReRanker的使用场景
- [ ] 了解多级缓存设计（查询缓存 + Embedding缓存）
- [ ] 了解多租户数据隔离实现方案

### 求职与面试（6项）
- [ ] 能清晰讲述RAG项目从0到1的搭建经验
- [ ] 能回答向量数据库选型问题（PgVector vs Milvus vs Pinecone）
- [ ] 能描述AI Agent在企业中的典型落地场景
- [ ] 了解2025年AI岗位分类和薪资范围
- [ ] 知道自己的学习路径规划和目标定位
- [ ] 准备了一个完整的RAG Demo项目可以展示

---

> 🎯 **最后建议：**
> 1. **动手 > 背书**：面试官更看重你实际搭过RAG项目，而不是背概念。建议用本宝典中的示例代码动手跑一遍
> 2. **Java+AI是差异化优势**：纯Python背景的候选人非常多，同时熟练掌握Java+Python的RAG工程师是稀缺人才
> 3. **关注Agent落地**：2025-2026年Agent将从Demo走向生产级，这是面试中的核心加分项
> 4. **系统性 > 碎片化**：按照本宝典的结构系统学习（基础→原理→实战→架构→面试），避免东拼西凑
