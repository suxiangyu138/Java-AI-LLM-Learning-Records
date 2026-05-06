from sentence_transformers import SentenceTransformer
import numpy as np

# 加载支持多语言（含中文）的模型
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')

sentences = [
    "西红柿炒蛋怎么做",
    "番茄炒蛋的做法",
    "Java后端开发学习路线"
]

embeddings = model.encode(sentences)

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

sim1 = cosine_similarity(embeddings[0], embeddings[1])
sim2 = cosine_similarity(embeddings[0], embeddings[2])

print(f"西红柿炒蛋 ↔ 番茄炒蛋 相似度：{sim1:.4f}")
print(f"西红柿炒蛋 ↔ Java开发  相似度：{sim2:.4f}")
