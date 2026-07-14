from sentence_transformers import SentenceTransformer
import chromadb

# --------------------------
# 1. 初始化：加载向量化模型 + ChromaDB 客户端
# --------------------------
# 加载轻量开源文本向量化模型
model = SentenceTransformer('all-MiniLM-L6-v2')

# 初始化 ChromaDB 客户端（本地文件存储）
client = chromadb.Client()

# 如果集合已存在，先删除避免报错
if client.list_collections():
    for col in client.list_collections():
        if col.name == "knowledge_base":
            client.delete_collection("knowledge_base")

# 创建向量集合（相当于 Milvus 的 Collection）
collection = client.create_collection(
    name="knowledge_base",
    metadata={"hnsw:space": "cosine"}  # 使用余弦相似度计算距离
)

# --------------------------
# 2. 准备真实文本数据并向量化
# --------------------------
documents = [
    "Java 是一种跨平台的面向对象编程语言，广泛用于后端开发。",
    "Milvus 是一个高性能的开源向量数据库，专为 AI 应用设计。",
    "ChromaDB 是轻量级的本地向量数据库，无需启动服务即可使用。",
    "RAG（检索增强生成）是一种结合检索与大模型的技术，用于提升生成内容的准确性。",
    "WSL2 是 Windows 上的 Linux 子系统，可用于运行 Docker 容器和开发环境。"
]

# 生成文本向量
embeddings = model.encode(documents).tolist()

# 存入 ChromaDB
collection.add(
    embeddings=embeddings,
    documents=documents,
    ids=[f"doc_{i+1}" for i in range(len(documents))]
)

print("✅ 文本向量化并存储完成！")

# --------------------------
# 3. 向量相似度检索（模拟用户提问）
# --------------------------
user_query = "什么是向量数据库？"
query_embedding = model.encode([user_query]).tolist()

results = collection.query(
    query_embeddings=query_embedding,
    n_results=3  # 返回最相关的 3 条文本
)

# 格式化输出结果
print(f"\n🔍 用户提问：{user_query}")
print("📚 相关知识库内容：")
for idx, doc in enumerate(results['documents'][0], 1):
    distance = results['distances'][0][idx-1]
    print(f"{idx}. 相关度: {(1-distance)*100:.1f}% | 内容: {doc}")
