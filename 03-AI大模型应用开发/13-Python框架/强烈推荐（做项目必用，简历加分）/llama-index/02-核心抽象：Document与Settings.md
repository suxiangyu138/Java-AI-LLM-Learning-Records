# 02 - 核心抽象：Document 与 Settings

> 本体系第二课：LlamaIndex 的数据模型——Document 进、Node 出、Settings 管全局——"一切数据先成 Document，一切配置走 Settings"

---

## 📚 目录

1. [一句话定位](#1-一句话定位)
2. [Document：原始文档](#2-document原始文档)
3. [Node：最小检索单元](#3-node最小检索单元)
4. [Metadata：元数据三作用](#4-metadata元数据三作用)
5. [Settings：全局配置](#5-settings全局配置)
6. [使用流](#6-使用流)
7. [异步姿势与常见坑](#7-异步姿势与常见坑)
8. [练习 5 题](#8-练习-5-题)
9. [本节验收](#9-本节验收)

---

## 1. 一句话定位

**Document 是"输入"，Node 是"处理单元"，Settings 是"全局开关"——三者构成 LlamaIndex 的数据心智**：

```text
数据模型
├── Document：一份原始文档（文本 + 元数据 + 唯一 ID）——最外层
├── Node：切分后的最小检索单元（文本片段 + 元数据 + embedding）——内层
├── Metadata：元数据（来源/时间/标签——检索过滤的依据）
└── Settings：全局配置（LLM/Embedding/切分参数——一个对象管全部）
    ——"数据流的形状：Document（整）→ 切分 → Node（碎）→ 入库 → 检索"
```

**定位心智**：**"理解 LlamaIndex 的第一关：数据不是'文本字符串'，是'带元数据的对象'"**——"**Document 到 Node 是'整到碎'的变换——索引/检索/合成全在 Node 层工作——'元数据跟着节点走（过滤靠它），embedding 长在节点上（相似度靠它）'"**（"**为什么对象化：字符串只有内容，对象有'内容 + 身份 + 属性'——检索过滤/溯源/去重全靠身份和属性'"**）。

## 2. Document：原始文档

**Document 是数据流的入口——一切加载器的输出形态**：

```python
from llama_index.core import Document

doc = Document(
    text="LlamaIndex 是数据框架……",
    metadata={"source": "blog.md", "author": "张三", "date": "2026-08-01"},
    doc_id="doc-001",        # 不传则自动生成
    excluded_llm_metadata_keys=["date"],   # 不喂给 LLM 的元数据
)
```

**要点**：① `text` 是唯一必填——加载器（SimpleDirectoryReader 等）返回的就是 `List[Document]`；② `metadata` 默认会拼进上下文（token 成本！）——不想让 LLM 看到的键放 `excluded_llm_metadata_keys`；③ `doc_id` 用于去重/追踪——**"一个 Document = 一份完整来源，不是一段话"**。

**边界**：跨文档检索时，同源多页内容建议保持为多个 Document（元数据可区分页），不要手动拼一个大字符串——**"Document 的粒度 = 溯源粒度"**——"一份 PDF 一页一个 Document 是常规操作"。

## 3. Node：最小检索单元

**Node 是检索/合成的操作对象——由 Document 切分而来**：

```python
from llama_index.core.node_parser import SentenceSplitter

splitter = SentenceSplitter(chunk_size=512, chunk_overlap=20)
nodes = splitter.get_nodes_from_documents([doc])

print(nodes[0].node_id)      # 唯一 ID（自动生成）
print(nodes[0].text)         # 文本片段
print(nodes[0].metadata)     # 继承自 Document
print(nodes[0].relationships)  # 关联：源 Document、前后节点（SourceNode 关系）
```

**要点**：① 每个 Node 自动带 `node_id`、`relationships`（指向源 Document 与相邻节点）——引用溯源靠它；② `metadata` 从 Document 继承——**但只有 `Document(text=..., metadata={...})` 显式传的才继承**，加载器默认不一定带；③ embedding 在入库时才生成，Node 本身不含向量——**"Node = 切分产物，Index = 嵌入后的组织形态（04 篇）"**；④ Node 的 text 是"检索单位"——**"回答的粒度 = Node 的粒度——切分策略（04 篇）直接决定回答质量"**。

## 4. Metadata：元数据三作用

**元数据是检索质量的隐形杠杆——三个用途**（宁少勿滥：只放会被用到的键，杂物键徒增 token 成本）：

```text
元数据三作用
├── ① 过滤：检索前按来源/日期/标签筛（metadata_filters——05 篇）
├── ② 溯源：回答带引用（file_name/page 拼进回答——企业必备）
└── ③ 去重：文档 hash 存 metadata，增量更新靠它（04 篇坑）
    ——"元数据不是装饰——过滤/溯源/去重的唯一抓手"
```

**陷阱**：元数据默认全量拼进 LLM 上下文（token 成本）——**"只留检索和溯源需要的键，其余进 excluded 列表"**；向量库的 metadata filter 能力有限（部分库不支持范围查询）——**"过滤需求先在 LlamaIndex 层想清楚，再选向量库"**。

## 5. Settings：全局配置

**Settings 取代了旧版散落的全局变量——一个对象管全部**：

```python
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI
from llama_index.embeddings.openai import OpenAIEmbedding
from llama_index.core.node_parser import SentenceSplitter

Settings.llm = OpenAI(model="gpt-5-mini", temperature=0.1)
Settings.embed_model = OpenAIEmbedding(model="text-embedding-3-small")
Settings.node_parser = SentenceSplitter(chunk_size=512, chunk_overlap=20)
Settings.num_outputs = 512      # LLM 输出上限
Settings.context_window = 16384 # 上下文窗口（提示词组装按此截断）
```

**要点**：① 设一次全局生效——索引构建/查询引擎/Agent 全读它；② 不用 OpenAI 时换类即可——**"DeepSeek/本地 vLLM 走 OpenAI 兼容端点（llama-index-llms-openai + base_url 一行切换——与 vLLM 体系的接入姿势相同）"**；③ `context_window` 不设会爆 token——**"Settings = 项目级配置——写在入口处，不要散在各文件"**；④ 中文项目建议显式设 `Settings.llm` 的 system 语言——**"默认提示词是英文模板，中文产品要定制（05 篇 update_prompts）"**。

**换模型演示**（应用代码零改动，只换 Settings）：

```python
from llama_index.llms.openai import OpenAI

# OpenAI
Settings.llm = OpenAI(model="gpt-5-mini")
# 本地 vLLM（同一类，只改 base_url 与 model 名）
Settings.llm = OpenAI(model="Qwen2.5-7B", api_base="http://localhost:8000/v1")
```

**要点**：LlamaIndex 的模型接入与 LangChain 同模式——**"统一接口 + 换配置不换代码——'接入抽象是框架的第一价值'"**（对比见 LangChain 体系 03 篇 ChatModel）；**embedding 模型同理**——`Settings.embed_model` 换实现即可，索引/检索代码不动——"**模型接入的三处配置（llm/embed_model/向量库）全部集中在一个 Settings——'改配置不碰业务'"**。

## 6. 使用流

1. 加载：`SimpleDirectoryReader(...).load_data()` → `List[Document]`（03 篇）
2. 切分：`Settings.node_parser.get_nodes_from_documents(docs)` → `List[Node]`（04 篇）
3. 嵌入入库：`VectorStoreIndex(nodes)`（04 篇）
4. 查询：`index.as_query_engine().query(...)`（05 篇）

**异步姿势**：LlamaIndex 全链路支持异步——`await index.as_query_engine().aquery(...)`、`aget_nodes_from_documents`——**"FastAPI 服务必用异步（01-Python 异步体系）"**。

**调试三连**（数据模型层排障）：① 打印 `len(nodes)` 与 `nodes[0].text` 前 100 字——"切分有没有生效"；② 打印 `nodes[0].metadata`——"元数据有没有继承"；③ 打印 `nodes[0].node_id` 两次加载是否稳定——"doc_id 有没有漂移"。**"数据模型层 90% 的坑，三步打印都能定位"**——定位不了再上可观测（09 篇 OTel）。

**多 Document 批处理**：加载器返回的是 `List[Document]`——批量操作时**每个 Document 的 metadata 独立维护**（来源/时间不同），不要合并成一个超大 Document——"一个 Document = 一个溯源单元——合并了引用就废了"。

## 7. 异步姿势与常见坑

**异步是生产标配（FastAPI 服务必用）**：

```python
# 加载/切分/查询全链路都有 async 版本
docs = await SimpleDirectoryReader("./data").aload_data()
nodes = await splitter.aget_nodes_from_documents(docs)
response = await query_engine.aquery("问题")

# 流式输出（打字机效果——SSE 场景）
stream = await query_engine.aquery("问题", streaming=True)
async for chunk in stream.async_response_gen():
    print(chunk, end="")
```

**五个常见坑**：

```text
常见坑
├── ① metadata 值必须是标量（str/int/float）——列表/字典多数向量库存不了
├── ② doc_id 要稳定——重建索引后引用会失效，用业务 ID（如文件名）做 doc_id
├── ③ embedding 不进 metadata——向量是独立字段，别混
├── ④ 同步阻塞混进 async 代码——asyncio 卡死（01-Python 异步体系）
└── ⑤ Settings 在 import 时就被读取——必须在任何索引/引擎创建前配置
    ——"五个坑 = 从原型到生产的典型翻车点"
```

**坑心智**：**"元数据是'标量字典'、doc_id 是'稳定业务键'、Settings 是'最先执行'"**——"**报错就按这三条对——'大多数元数据/持久化报错都在这三句话里'"**。

## 8. 练习 5 题

1. Document 和 Node 的区别？（整 vs 碎——原始文档 vs 检索单元）
2. Node 上的三个关键字段？（node_id/text/metadata/relationships）
3. 元数据三个作用？（过滤/溯源/去重）
4. Settings 里最该设的四样？（llm/embed_model/node_parser/context_window）
5. 元数据为什么要排除键？（token 成本——不想喂给 LLM 的键）

## 9. 本节验收

**验收动作**：① 手写一个带元数据的 Document 并切分查看 Node 字段；② 配置 Settings 四件套；③ 验证元数据继承规则（显式传 vs 不传）——**"Document/Node/Metadata/Settings = 数据模型四件套——本体系的底层心智"**。

> 🎯 **核心要点**：**Document 进、Node 出、Settings 管全局**；元数据三作用（**过滤/溯源/去重**）；**node_id 与 relationships 是溯源根基**；**Settings 四件套（llm/embed_model/node_parser/context_window）**——"数据不是字符串，是带元数据的对象"。

---

**上一模块**：[01-LlamaIndex是什么.md](./01-LlamaIndex是什么.md) / **下一模块**：[03-文档加载：LlamaHub与LlamaParse.md](./03-文档加载：LlamaHub与LlamaParse.md)
