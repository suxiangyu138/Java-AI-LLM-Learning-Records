全套可直接运行：BGE-M3 + Ollama Embedding + Milvus 完整链路
 
环境准备
1. 本地已安装 Ollama
2. 拉取最强开源嵌入模型：
    bash
    ollama pull bge-m3
 
3. Docker 启动 Milvus
    bash
    docker run -d -p 19530:19530 --name milvus milvusdb/milvus:latest
 
4. 安装依赖
    bash
    pip install langchain langchain-community pymilvus
 
 
完整可运行代码
python
from langchain_community.embeddings import OllamaEmbeddings
from langchain_community.vectorstores import Milvus
from langchain.text_splitter import CharacterTextSplitter

# 1. 初始化本地 Embedding 模型 BGE-M3
embedding = OllamaEmbeddings(
    model="bge-m3",
    base_url="http://localhost:11434"
)

# 2. 测试文档
docs_content = [
    "RabbitMQ 是一款基于 AMQP 协议的消息队列，用于异步解耦、削峰填谷。",
    "Elasticsearch 基于倒排索引，擅长全文检索与海量数据模糊查询。",
    "Docker 可以快速部署中间件，隔离环境，解决软件冲突问题。"
]

# 3. 文本切块
splitter = CharacterTextSplitter(chunk_size=200, chunk_overlap=20)
split_docs = splitter.create_documents(docs_content)

# 4. 连接 Milvus 并写入向量
vector_db = Milvus.from_documents(
    documents=split_docs,
    embedding=embedding,
    connection_args={"host": "localhost", "port": "19530"},
    collection_name="ai_knowledge",
    drop_old=True
)

# 5. 语义检索
query = "消息队列有什么作用"
res = vector_db.similarity_search(query, k=2)

# 6. 打印结果
for idx, doc in enumerate(res):
    print(f"【结果{idx+1}】{doc.page_content}")
 
 
链路拆解（对应你学过的所有知识）
1. BGE-M3（Embedding模型）
    文本 → 多维语义向量
2. 切块 Chunk
    防止文本过长、向量精度下降
3. Milvus（向量数据库）
    存储向量+原始文本，持久化
4. 相似度检索
    问题也转向量，计算余弦距离，召回语义匹配内容
5. 后续拼接 Prompt 喂给大模型 = 标准 RAG
 
无缝扩展 RAG 完整问答
追加代码即可实现「检索+大模型回答」：
python
from langchain_openai import ChatOpenAI
from langchain.chains import RetrievalQA
llm = ChatOpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama",
    model="qwen"
)
rag_chain = RetrievalQA.from_chain_type(
    llm=llm,
    retriever=vector_db.as_retriever(k=2)
)
answer = rag_chain.invoke("消息队列有什么作用")
print("\n【RAG最终回答】")
print(answer["result"])
 
 
你整套 AI 技术栈闭环汇总
- 大模型：Ollama（Qwen/DeepSeek）
- 嵌入模型：BGE-M3
- 向量库：Milvus
- 框架：LangChain
- 增强方案：RAG
- 高级能力：Agent + MCP + 自定义Skill
