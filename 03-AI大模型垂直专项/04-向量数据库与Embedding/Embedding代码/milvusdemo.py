from pymilvus import MilvusClient, DataType
from sentence_transformers import SentenceTransformer
import numpy as np

# 1. 初始化 Milvus 客户端
client = MilvusClient(uri="http://localhost:19530")

# 2. 定义集合参数
collection_name = "embedding_demo"
dim = 384  # all-MiniLM-L6-v2 模型的向量维度

# 如果集合已存在，先删除（方便测试）
if client.has_collection(collection_name):
    client.drop_collection(collection_name)

# 创建集合
client.create_collection(
    collection_name=collection_name,
    dimension=dim,
    primary_field_name="id",
    vector_field_name="embedding",
    metric_type="COSINE",  # 余弦相似度
    auto_id=True
)

# 3. 加载 Embedding 模型
model = SentenceTransformer('all-MiniLM-L6-v2')

# 4. 准备数据
sentences = [
    "西红柿炒蛋怎么做",
    "番茄炒蛋的做法",
    "Java后端开发学习路线",
    "Python 入门教程"
]

# 生成向量
embeddings = model.encode(sentences)

# 插入数据到 Milvus
data = [
    {"embedding": emb}
    for emb in embeddings
]
client.insert(collection_name=collection_name, data=data)

# 5. 构建索引
client.create_index(
    collection_name=collection_name,
    index_type="HNSW",
    metric_type="COSINE",
    params={"M": 8, "efConstruction": 64}
)

# 6. 向量检索
query_text = "番茄炒蛋的做法"
query_embedding = model.encode([query_text])

# 执行相似度搜索
results = client.search(
    collection_name=collection_name,
    data=query_embedding,
    limit=2,
    output_fields=["id"]
)

# 打印结果
print("查询文本:", query_text)
print("最相似的结果:")
for res in results[0]:
    print(f"ID: {res['id']}, 相似度: {res['distance']:.4f}, 对应句子: {sentences[res['id']]}")
