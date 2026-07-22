# RAG 必做项目清单 面试问答
> 🎯 基于 RAG 实战项目清单，涵盖面试高频问题与完美解答方案，帮助你在 Java 后端 + AI 方向脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请详细解释 RAG 的工作原理和完整链路
**面试官意图：** 考察你是否真正理解 RAG 从文档到答案的全流程，而不是只背概念。

**完美解答：**

RAG（Retrieval-Augmented Generation）的核心思想是"先检索，后生成"——在让大模型回答问题之前，先从外部知识库中检索出相关文档片段，作为上下文拼接到 Prompt 中，再让模型基于这些真实材料生成答案。

完整的 RAG 链路分为四大阶段：

**第一阶段：文档处理与入库（离线过程）**
- 文档加载：从 PDF、TXT、Markdown、网页等多源格式读取内容
- 文本清洗：去除噪声、页眉页脚、特殊字符
- 文本分块（Chunking）：按段落、句子、固定长度等方式切分，通常使用递归字符分割器（RecursiveCharacterTextSplitter），chunk size 控制在 300-800 tokens，chunk overlap 为 10%-20%
- 向量化（Embedding）：用嵌入模型将每个文本块转换为高维向量
- 向量入库：将向量及其元数据（来源、页码、时间戳等）写入向量数据库

**第二阶段：检索（在线过程）**
- 用户输入 Query
- 对 Query 做同样 Embedding 转换
- 在向量库中执行相似度检索（余弦相似度 / L2 / 内积），召回 TopK 最相关片段
- 可选：混合检索（向量 + 关键词 BM25）→ RRF 融合排序 → Reranker 重排序

**第三阶段：生成**
- 将检索到的文档片段作为上下文，与用户问题拼接成 Prompt
- 调用大模型生成最终答案
- 在答案中标注引用来源（溯源）

**第四阶段：多轮对话（可选）**
- 对历史对话做压缩或摘要
- 对当前问题做改写（Query Rewrite），使其脱离上下文也能独立检索

**延伸追问应对：** 如果追问"RAG 和 Fine-tuning 的区别"，从"知识更新成本、幻觉控制、领域适配深度"三个维度对比——RAG 零成本更新知识、即插即用；Fine-tuning 深度内化知识但更新需要重新训练。

---

### Q2：请对比向量检索和关键词检索的优缺点
**面试官意图：** 考察你理解两种检索范式的本质差异。

**完美解答：**

| 对比维度 | 向量检索（稠密检索） | 关键词检索（稀疏检索） |
|----------|---------------------|----------------------|
| 原理 | 语义相似度匹配 | 词频 / 倒排索引匹配 |
| 同义词处理 | 能匹配（"电脑"→"计算机"） | 不能直接匹配 |
| 精确匹配 | 弱 | 强 |
| 长尾 / 专有名词 | 表现一般 | 精确匹配效果好 |
| 计算资源 | 需要 GPU 做 Embedding | CPU 即可 |
| 延迟 | 稍高（向量计算） | 低 |
| 冷启动 | 需要训练 Embedding 模型 | 无需训练 |

**关键结论：** 两者是互补关系。向量检索擅长语义理解，关键词检索擅长精确匹配。企业级 RAG 应将两者结合为"混合检索"，通过 RRF（Reciprocal Rank Fusion）或加权融合策略综合排序，显著提升召回质量。

**延伸追问应对：** 如果问"RRF 具体如何计算"，回答：RRF 不依赖分数绝对值，而是基于排名位置 `score = sum(1 / (k + rank_i))`，k 通常取 60。这种方法的好处是不同检索系统返回的分数不可比时，仍然能公平融合。

---

### Q3：RAG 系统面临哪三大挑战？你是如何解决的？
**面试官意图：** 考察你对 RAG 痛点的深度理解。

**完美解答：**

**挑战一：检索质量不佳（召回低）**
- 问题：检索到的文档片段相关度不够，或者漏掉了关键信息
- 解决策略：
  - 混合检索（向量 + 关键词），互补优缺
  - 分块策略调优：chunk size 300-800、overlap 10-20%、语义分块
  - 重排序（Reranker）：用 CrossEncoder 对 TopK 结果二次打分过滤
  - Query 改写：将模糊问题转为更具体的检索 Query

**挑战二：生成幻觉（Answer Hallucination）**
- 问题：模型生成的内容不在检索到的文档中
- 解决策略：
  - 在 Prompt 中明确约束："仅基于提供的上下文回答，如果没有相关信息，请说明不知道"
  - 答案溯源：在回答中标注引用页码 / 段落，便于验证
  - 设置置信度阈值：检索分数低于阈值时不回答，改为反问

**挑战三：多轮对话上下文断裂**
- 问题：第二问"它的作者是谁"，脱离上下文无法检索
- 解决策略：
  - 对话历史压缩：用 LLM 对历史对话做摘要
  - Query Rewrite：将代词照应替换为具体实体，如"它"→《书名》
  - 上下文感知检索：将历史对话与当前问题拼接后一起检索

> 💡 **面试加分技巧：** 主动说出这三个挑战及其解决方案，面试官会认为你做过真实项目而不是只读过文档。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：请描述你如何实现一个单 PDF 文档的极简 RAG 问答系统
**面试官意图：** 考察 RAG 最小可行链路的落地能力。

**完美解答：**

我实现过单 PDF 的 RAG 问答系统，技术栈是 LangChain + Ollama（Qwen）+ Chroma。核心流程如下：

**第一步：PDF 加载与分块**
```python
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter

loader = PyPDFLoader("document.pdf")
docs = loader.load()

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
    separators=["\n\n", "\n", "。", "，", " ", ""]
)
chunks = text_splitter.split_documents(docs)
```

**第二步：向量化与存储**
```python
from langchain_community.embeddings import HuggingFaceBgeEmbeddings
from langchain_community.vectorstores import Chroma

embeddings = HuggingFaceBgeEmbeddings(model_name="BAAI/bge-large-zh-v1.5")
vectorstore = Chroma.from_documents(chunks, embeddings, persist_directory="./chroma_db")
```

**第三步：检索增强问答**
```python
from langchain.chains import RetrievalQA
from langchain_community.chat_models import ChatOllama

llm = ChatOllama(model="qwen2:7b")
retriever = vectorstore.as_retriever(search_kwargs={"k": 4})

qa_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=retriever,
    return_source_documents=True
)
result = qa_chain.invoke({"query": "文档中提到的主要技术方案是什么？"})
```

**遇到的问题与解决：**
- **中文分块边界问题：** 英文按空格 / 换行切割即可，中文需要加入句号、逗号作为分隔符
- **答案溯源：** 返回 source_documents 中包含页码和 chunk 内容，展示在回答下方

**效果：** 2 天完成最小可运行 Demo，对 50 页以下的 PDF 问答准确率约 85%。

---

### Q5：多文档通用知识库 RAG 相比单文档增加了哪些复杂性？
**面试官意图：** 考察工程化思维和扩展能力。

**完美解答：**

从单文档到多文档知识库，复杂度是指数增长的，主要体现在以下方面：

**1. 增量入库与去重**
- 需要设计文档 ID 生成策略（如 MD5 摘要），避免重复入库
- 实现 upsert 语义：相同文档再次上传时更新而非追加
```python
doc_id = hashlib.md5(content.encode()).hexdigest()
vectorstore.update_document(document_id=doc_id, document=new_chunk)
```

**2. 跨文档融合答案**
- 一个问题可能涉及多个文档的内容，需要从不同来源拼接片段
- 实现方式：提高检索 TopK 值（如 6-8），让 LLM 自行综合
- 需要在 Prompt 中明确区分来源，例如标注 `[来源：文档A-第3页]`

**3. 统一元数据管理**
- 每篇文档绑定元数据：文档名、上传时间、分类标签、权限级别
- 检索时通过元数据过滤缩小范围，提升精准度

**4. 持久化与批量处理**
- Milvus / Chroma 持久化存储，服务重启不丢失数据
- 批量文档上传时需要考虑队列和异步处理，避免阻塞

> 💡 **亮点话术：** "我设计了 MD5 去重 + 元数据绑定 + 跨文档溯源的多文档架构，支撑了 200+ 文档的知识库在线运行。"

---

### Q6：混合检索 RAG 具体怎么实现？RRF 融合是如何做的？
**面试官意图：** 考察检索优化的深度，这是区分初级和高级工程师的关键点。

**完美解答：**

混合检索的精髓在于"向量检索理解语义 + 关键词检索精准匹配"双通道互补。

**架构设计：**
- 向量通道：Milvus / Chroma，负责语义召回
- 关键词通道：Elasticsearch，负责 BM25 关键词精确匹配
- 融合层：RRF 融合排序 + CrossEncoder 重排序

**RRF 融合实现：**
```python
def reciprocal_rank_fusion(results_vectors, results_keywords, k=60):
    scores = {}
    for rank, doc_id in enumerate(results_vectors):
        scores[doc_id] = scores.get(doc_id, 0) + 1 / (k + rank + 1)
    for rank, doc_id in enumerate(results_keywords):
        scores[doc_id] = scores.get(doc_id, 0) + 1 / (k + rank + 1)
    return sorted(scores.items(), key=lambda x: x[1], reverse=True)
```

**Reranker 精排：**
```python
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CrossEncoderReranker
from langchain_community.cross_encoders import HuggingFaceCrossEncoder

reranker = CrossEncoderReranker(
    model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-large")
)
compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker, base_retriever=retriever
)
```

**效果提升：** 纯向量检索准确率 78% → 混合检索 87% → 加重排序 93%+。

> ⚠️ **注意：** 混合检索不是无脑加分——如果向量检索已经很好（92%+），再加关键词检索可能提升有限，此时应优先优化分块策略和 Embedding 模型选型。

---

### Q7：多轮对话感知 RAG 如何实现上下文理解？
**面试官意图：** 考察对话管理设计能力。

**完美解答：**

多轮对话 RAG 的难点在于：用户第二问通常省略了上下文信息（如"它的原理是什么？"中的"它"），直接检索会完全失效。

**我采用的解决方案：**

**步骤一：对话历史管理**
```python
from langchain.memory import ConversationSummaryMemory

memory = ConversationSummaryMemory(
    llm=llm,
    max_token_limit=500,
    return_messages=True
)
```

**步骤二：Query Rewrite（问题改写）**
```python
rewrite_prompt = """基于对话历史，将用户最新问题改写为可以独立检索的问题。
历史：{chat_history}
用户最新问题：{question}
请输出改写后的独立问题："""
```

将"它的作者是谁"改写为"《三体》的作者是谁"，然后使用改写后的问题去向量库检索。

**步骤三：上下文感知检索**
- 改写 Query 后再检索，确保检索相关性
- 检索结果与历史对话一起拼接成完整 Prompt

**效果：** 三问以后的准确率从 40% 提升到 82%，明显改善了连续对话体验。

---

### Q8：SpringBoot 企业级 RAG 后端服务如何设计接口架构？
**面试官意图：** 考察 Java 后端与 AI 结合的能力。

**完美解答：**

我设计的企业级 RAG 后端基于 SpringBoot + Spring AI + Milvus，包含以下核心接口：

```java
@RestController
@RequestMapping("/api/rag")
public class RagController {

    // 文档上传与入库
    @PostMapping("/document/upload")
    public Result<String> uploadDocument(@RequestParam MultipartFile file) {
        // 1. PDF/TXT 解析
        // 2. 文本分块
        // 3. Embedding 向量化
        // 4. Milvus 入库（带元数据）
        // 5. 返回文档 ID
    }

    // 检索接口
    @PostMapping("/retrieve")
    public Result<List<DocumentChunk>> retrieve(@RequestBody QueryRequest request) {
        // 1. 向量检索
        // 2. 可选混合检索
        // 3. 权限过滤
        // 4. 返回 TopK
    }

    // 问答接口（核心）
    @PostMapping("/chat")
    public Result<AnswerResponse> chat(@RequestBody ChatRequest request) {
        // 1. Query Rewrite（多轮）
        // 2. 检索
        // 3. Reranker
        // 4. Prompt 拼接
        // 5. LLM 生成
        // 6. 答案溯源
    }

    // 文档管理
    @DeleteMapping("/document/{docId}")
    public Result<Void> deleteDocument(@PathVariable String docId) {
        // 删除向量库中的对应数据
    }
}
```

**工程化亮点：**
- 全局异常处理：用 `@ControllerAdvice` 统一处理各类异常
- 限流：Guava RateLimiter / Redis 限流，防止 LLM 被打爆
- 异步处理：`@Async` + 线程池处理大文档上传
- API 文档：Knife4j / Swagger 自动生成接口文档
- 统一返回结构：`Result<T>` 包含 code、message、data、traceId

> 💡 **面试亮点：** "我的设计遵循 RESTful 规范，接口粒度按文档管理、检索、问答三层划分，方便前端和其他微服务调用。"

---

### Q9：Docker Compose 一键部署 RAG 全栈如何编排服务？
**面试官意图：** 考察部署运维能力。

**完美解答：**

```yaml
version: '3.8'
services:
  ollama:
    image: ollama/ollama
    volumes:
      - ./ollama_data:/root/.ollama
    ports:
      - "11434:11434"
    networks:
      - rag_network

  milvus:
    image: milvusdb/milvus:v2.3.0
    volumes:
      - ./milvus_data:/var/lib/milvus
    environment:
      - ETCD_ENDPOINTS=etcd:2379
    ports:
      - "19530:19530"
    networks:
      - rag_network

  rag-backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - OLLAMA_URL=http://ollama:11434
      - MILVUS_HOST=milvus
    depends_on:
      - ollama
      - milvus
    networks:
      - rag_network

networks:
  rag_network:
    driver: bridge
```

**设计要点：**
- 服务依赖配置：`depends_on` 确保启动顺序
- 数据持久化：用 `volumes` 挂载 Ollama 模型和 Milvus 数据
- 网络互通：自定义 bridge 网络，服务名互相解析
- 环境变量管理：通过 `environment` 配置连接信息，避免硬编码

> ⚠️ **踩坑提醒：** Milvus 启动依赖 etcd 和 minio，需要先启动依赖服务。实际项目中我用了一个 `wait-for-it.sh` 脚本来确保依赖就绪后再启动应用服务。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q10：如果让你设计一个企业级 RAG 平台，你会考虑哪些架构要素？
**面试官意图：** 考察系统设计能力和全局思维。

**完美解答：**

企业级 RAG 平台不仅仅是"检索 + 生成"，而是一个完整的知识管理基础设施。我会分层设计：

**接入层**
- 统一 API 网关（Spring Cloud Gateway / Nginx）
- 认证鉴权：OAuth2 / JWT，多租户隔离
- 限流熔断：Sentinel / Resilience4j
- 流量分发：读写分离（读多写少）

**文档处理层**
- 支持多格式：PDF、Word、Markdown、网页、图片（OCR）
- 分块策略可配置：按 token 数、按语义段落、按文档结构
- 去重与版本管理：MD5 去重 + 文档版本号

**向量存储层**
- Milvus 集群：分片 + 副本，支持水平扩展
- 索引策略：根据数据量选择 IVF_FLAT / HNSW / FLAT
- 冷热数据分离：热数据 SSD、冷数据压缩存储
- 标量字段过滤：支持按时间、分类、权限字段过滤

**检索增强层**
- 混合检索：向量 + BM25 + RRF
- 重排序：CrossEncoder Reranker 二次精排
- Query 理解：改写、扩展、消歧
- 多轮对话：Memory + Query Rewrite

**生成层**
- 多模型适配：Ollama / 云端 API 统一抽象
- Prompt 模板管理：场景化模板库
- 溯源机制：引用标注、置信度评分
- 幻觉检测：用另一个 LLM 或规则引擎校验答案是否在上下文中

**运维层**
- 可观测性：Prometheus + Grafana 监控（QPS、延迟、召回率）
- 日志链路：ELK 收集检索与问答日志
- 持续改进：用户反馈 → bad case 分析 → 优化分块/检索/模型

> 🎯 **面试加分金句：** "企业级 RAG 平台的核心不是把 LLM 接入知识库，而是构建一个可治理、可观测、可持续优化的知识基础设施。"

---

### Q11：RAG 系统如何做权限隔离和多租户设计？
**面试官意图：** 考察企业级工程化思维。

**完美解答：**

**方案一：单 Collection + 元数据过滤（适用于中小规模）**
- 所有文档共用一个 Milvus Collection
- 每个文档的元数据中包含 `tenant_id`、`department_id`、`owner` 字段
- 检索时在请求中携带用户信息，通过元数据过滤实现隔离
```python
vectorstore.similarity_search(
    query,
    k=5,
    filter={"tenant_id": {"$eq": current_user.tenant_id}}
)
```
- 优点：实现简单，资源利用率高
- 缺点：数据量大时过滤性能下降

**方案二：多 Collection / Partition 隔离（适用于大规模）**
- 每个租户一个独立 Collection 或 Partition
- 检索时直接定位到对应的 Collection
- 优点：物理隔离，性能不受其他租户影响
- 缺点：管理成本高，租户数量多时 Collection 数爆炸

**推荐方案：** 混合策略
- 中小租户（<100万向量）：共用 Collection + 标量字段过滤
- 大租户（>1000万向量）：独立 Collection
- 定期评估分区数据量，自动迁移

> ⚠️ **注意：** 权限隔离不是只做检索过滤就够了——文档上传、删除、修改等操作也要做权限校验，做到"全链路权限管控"。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q12：线上 RAG 服务，用户反馈很多问题没有召回相关文档，怎么排查？
**面试官意图：** 考察故障排查的思维路径。

**完美解答：**

我会按照"从数据到算法到工程"的自下而上排查路径：

**第一步：检查数据层**
1. 确认文档是否成功入库：查向量库中对应文档 ID 是否存在
2. 检查分块大小是否合理：chunk 过小可能丢失上下文，过大则语义模糊
3. 查看 Embedding 质量：随机抽几个问题，手动计算 Query 与检索结果的余弦相似度——如果相似度明显低，说明 Embedding 模型不适合当前语料

**第二步：检查检索层**
1. 调整 TopK 值：从 3 逐步增加到 10，看召回是否改善
2. 降低相似度阈值：如果阈值过高会过滤掉部分相关内容
3. 对比向量检索 vs 关键词检索：分别测试两种方式，定位是语义理解问题还是精确匹配问题
4. 查看 Query 质量：用户问题本身是否模糊？需要 Query Rewrite 吗？

**第三步：检查生成层**
1. 检查 Prompt 模板：上下文拼接是否正确？是否有 token 截断导致关键片段被裁？
2. 检查 LLM 参数：temperature 是否过高导致回答偏离上下文？

**第四步：工程排查**
1. 检查检索接口耗时：如果延迟过高，可能是索引未构建或资源不足
2. 查看日志：确认检索请求和返回内容的完整链路

**核心排查工具：**
- 一个简单的"检索调试面板"：输入 query，展示召回的 TopK 结果及其相似度分数
- Bad Case 数据集：收集用户反馈差的问题，批量重跑对比

> 🎯 **总结：** 80% 的召回问题要么是分块不合理，要么是 Embedding 选型不合适，要么是 Query 需要改写。先检查这三个，往往能解决大部分问题。

---

### Q13：你的 RAG 服务响应延时很高（5秒+），怎么优化？
**面试官意图：** 考察性能优化能力。

**完美解答：**

RAG 服务的延迟瓶颈通常在这几个环节，我按优先级排序优化：

**1. 检索层优化（性价比最高）**
- 索引调优：确保向量库使用了恰当的索引类型（HNSW > IVF_FLAT > FLAT）
- 减少 TopK：从 10 降到 5，延迟降低约 40%
- 缓存热点查询：Redis 缓存高频 Query 的检索结果
```java
@Cacheable(value = "retrieval", key = "#query.hashCode()")
public List<DocumentChunk> retrieve(String query) {
    // 向量检索
}
```

**2. Embedding 层优化**
- 使用 GPU 推理：Embedding 模型从 CPU 切换到 GPU，延迟从 200ms 降到 20ms
- 批处理：多个请求合并成 batch，一次推理
- Embedding 结果缓存：相同文本复用已计算的向量

**3. LLM 生成层优化**
- 流式输出：首 token 时间（TTFT）降低到 200ms 内
- 模型量化：使用 4-bit 量化模型，推理速度提升 2-3 倍
- 减少 max_tokens：从 1024 降到 512，生成时间减半

**4. 架构优化**
- 连接池配置：数据库 / HTTP 连接池大小调优
- 异步非阻塞：WebFlux 替代 Servlet，提升吞吐量
- 读写分离：文档入库走异步队列，检索走同步在线

**优化效果：** 经过上述优化，P99 延迟从 5.2s 降到 1.1s，吞吐量提升 4 倍。

---

### Q14：用户说 RAG 回答的内容明显不在提供的文档里（幻觉问题），怎么办？
**面试官意图：** 考察对 LLM 幻觉的认知和解决能力。

**完美解答：**

幻觉是 RAG 系统的老大难问题，我会从多个层面系统性地处理：

**1. Prompt 层面（最快见效）**
```text
你是一个严谨的问答助手。请严格基于以下提供的上下文回答问题。
- 如果上下文中包含答案，请结合上下文给出准确回答
- 如果上下文中没有答案，请明确说"根据提供的文档，我没有找到相关信息"
- 不要编造信息，不要推测
上下文：{context}
问题：{question}
```

**2. 溯源机制（强制约束）**
- 要求 LLM 在回答中标注引用来源（页码 / 段落编号）
- 前端展示引用高亮：用户可点击跳转到原文
- 通过溯源机制倒逼 LLM 只能基于检索内容作答

**3. 置信度过滤**
```python
if retrieval_score < 0.6:
    return "抱歉，我没有找到足够相关的信息来回答这个问题。"
```

**4. 事后检测**
- 使用另一个 LLM 或 NLI 模型验证生成的答案是否被检索内容支持
- 对于低置信度的答案，改为"不回答"或追加免责声明

**5. RAGAS 评估体系**
- 建立自动化评估流水线：faithfulness（忠实度）、answer_relevancy（相关性）、context_recall（召回率）
- 每次调整后跑评估，用数据说话

> 💡 **面试话术：** "我构建了'前约束 + 中溯源 + 后校验'的三层幻觉防控体系，将 hallucination 率从 15% 降低到 3% 以内。"

---

## 💎 面试加分金句

- "RAG 的本质不是让 LLM 记住知识，而是让 LLM 学会检索和利用知识。它解决的是大模型知识滞后和幻觉的核心痛点。"
- "企业级 RAG 的三大支柱是：检索质量、答案忠实度、系统可治理性——缺一不可。"
- "Chroma 适合原型验证和个人项目，Milvus 才是企业级生产的选择，选型要评估数据量、并发量、权限需求三个维度。"
- "衡量 RAG 系统质量，我主要看三个指标：召回率（Context Recall）、忠实度（Faithfulness）、答案相关度（Answer Relevancy）。"
- "RAG 项目最大的坑不是技术实现，而是'坏数据进，坏数据出'——文档清洗和分块策略直接决定了系统天花板。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| 为什么不用 Fine-tuning 代替 RAG？ | RAG 零成本更新知识、可溯源；Fine-tuning 适合让模型学习写作风格和格式 |
| 向量维度对检索效果有什么影响？ | 维度越高表达能力越强，但计算成本也越高；256-768 是常用范围 |
| 如何评估 RAG 系统的好坏？ | RAGAS 框架：faithfulness、answer_relevancy、context_precision、context_recall |
| 大模型上下文窗口越来越长，还需要 RAG 吗？ | 窗口再长也有成本限制，且长上下文中检索效率低；RAG 仍然是性价比最高的方案 |
| 你如何处理文档中的表格和图片？ | 表格转为 Markdown 格式，图片使用 OCR + 多模态 Embedding（CLIP 等） |
| 分块策略如何选择 chunk size？ | 取决于文档类型和 Embedding 模型；短文本用 256，长文本用 512-800，overlap 设为 10-20% |

## 🔗 关联知识点

- [Embedding必做项目清单-面试问答](./Embedding必做项目清单-面试问答.md)
- [Chroma必做项目清单-面试问答](./Chroma必做项目清单-面试问答.md)
- [Milvus必做项目清单-面试问答](./Milvus必做项目清单-面试问答.md)
- [LangChain必做项目-面试问答](./LangChain必做项目-面试问答.md)
- [Java 并发编程面试问答](../01-底层根基-Java核心底座/Java并发编程-面试问答.md)
