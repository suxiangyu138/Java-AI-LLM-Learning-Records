# 阶段 5：项目实战与 Java 联动

> **目标**：Python 不搞成独立求职方向，而是服务你的 Java+AI 简历
> **核心**：Java 做工程 → Python 做 AI → HTTP 桥接 → 简历亮点
> **产出**：1–2 个可写入简历的 "Java 后端 + Python AI 服务" 联动项目

---

## 📌 章节定位

本章是整个 Python 学习路线的**落地环节**。前面四个阶段让你"会用 Python"，这个阶段让你"用得有价值"。

核心思路：**Java 擅长工程化、业务逻辑、权限管控 → Python 擅长 AI/数据处理/文本分析 → 两者通过 REST API 协作**。

---

## 🎯 核心章节

### 1. 架构模式：Java 做主，Python 做辅

```
┌──────────────────────────────────────────────────────┐
│                    用户请求                            │
└──────────────────────┬───────────────────────────────┘
                       ▼
┌──────────────────────────────────────────────────────┐
│              Java Spring Boot 后端                     │
│  ┌─────────┐  ┌──────────┐  ┌────────────────────┐   │
│  │ 认证鉴权  │  │ 业务逻辑  │  │ HTTP Client        │   │
│  │ (JWT)   │  │ (Service) │  │ → 调用 Python 服务  │   │
│  └─────────┘  └──────────┘  └─────────┬──────────┘   │
└───────────────────────────────────────┼──────────────┘
                                        │ HTTP (JSON)
                       ┌────────────────▼──────────────┐
                       │      Python FastAPI 服务        │
                       │  ┌──────────┐ ┌─────────────┐  │
                       │  │ Embedding │ │ 模型推理     │  │
                       │  │ 向量化    │ │ (PyTorch)   │  │
                       │  └──────────┘ └─────────────┘  │
                       │  ┌──────────┐ ┌─────────────┐  │
                       │  │ 向量检索  │ │ 数据分析     │  │
                       │  │ (向量DB)  │ │ (Pandas)    │  │
                       │  └──────────┘ └─────────────┘  │
                       └────────────────────────────────┘
```

### 2. 项目一：智能文档问答系统（Java + Python RAG）

**适合写进简历的项目**，展示你"后端工程 + AI 落地"的综合能力。

#### 项目架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Java 后端    │────→│ Python RAG   │────→│  向量数据库    │
│  Spring Boot │←────│ FastAPI 服务  │←────│  Chroma/Milvus│
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │
       ▼                    ▼
  ┌─────────┐        ┌──────────────┐
  │ MySQL   │        │ LLM API      │
  │ 用户/文档 │        │ (OpenAI 等)   │
  └─────────┘        └──────────────┘
```

#### Java 侧职责

- 用户注册/登录（Spring Security + JWT）
- 文档上传管理（文件存储、元数据管理）
- 对话会话管理（Session 创建、历史记录）
- 权限控制（谁能访问哪些文档）
- 调用 Python RAG 服务获取回答

#### Python 侧职责

- 文档解析（PDF/Word/HTML → 纯文本）
- 文本分块（Chunk 策略）
- Embedding 生成（调用本地模型或 API）
- 向量检索（查询语义最相关的 Chunk）
- 与 LLM API 交互（构造 Prompt + 解析结果）

```python
# Python RAG 服务核心代码骨架
# rag_service/main.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import chromadb
from chromadb.utils import embedding_functions

app = FastAPI(title="RAG 服务", version="1.0.0")

# --- 向量数据库初始化 ---
chroma_client = chromadb.PersistentClient(path="./chroma_db")
embedding_fn = embedding_functions.SentenceTransformerEmbeddingFunction(
    model_name="BAAI/bge-small-zh-v1.5"
)

# --- 请求/响应模型 ---
class IndexRequest(BaseModel):
    """文档入库请求"""
    doc_id: str
    chunks: List[str]
    metadata: Optional[dict] = None

class QueryRequest(BaseModel):
    """检索问答请求"""
    question: str
    top_k: int = 5
    collection_name: str = "default"

class QueryResponse(BaseModel):
    """检索结果"""
    answer: str
    sources: List[dict]  # [{chunk, score, doc_id}, ...]

# --- API 端点 ---
@app.post("/index")
async def index_document(req: IndexRequest):
    """将文档分块向量化并存入向量数据库。"""
    collection = chroma_client.get_or_create_collection(
        name=req.collection_name if hasattr(req, 'collection_name') else "default",
        embedding_function=embedding_fn,
    )
    ids = [f"{req.doc_id}_chunk_{i}" for i in range(len(req.chunks))]
    collection.add(
        documents=req.chunks,
        ids=ids,
        metadatas=[{"doc_id": req.doc_id, **req.metadata} if req.metadata else {"doc_id": req.doc_id}
                   for _ in req.chunks],
    )
    return {"status": "ok", "chunks_indexed": len(req.chunks)}

@app.post("/query", response_model=QueryResponse)
async def query_rag(req: QueryRequest):
    """检索相关文档块并生成回答。"""
    collection = chroma_client.get_collection(
        name=req.collection_name,
        embedding_function=embedding_fn,
    )
    results = collection.query(query_texts=[req.question], n_results=req.top_k)

    # 构建上下文 Prompt
    context = "\n\n".join(results["documents"][0])
    # prompt = f"基于以下内容回答问题:\n{context}\n\n问题: {req.question}"

    # 调用 LLM（这里简化处理）
    # answer = call_llm(prompt)
    answer = f"[基于 {len(results['documents'][0])} 个相关块生成]"

    sources = [
        {"chunk": doc[:200] + "...", "score": score, "doc_id": meta.get("doc_id", "")}
        for doc, score, meta in zip(
            results["documents"][0],
            results["distances"][0],
            results["metadatas"][0],
        )
    ]
    return QueryResponse(answer=answer, sources=sources)

@app.get("/health")
async def health():
    return {"status": "ok"}
```

```java
// Java 侧调用 Python RAG 服务的 HTTP Client
// RagClient.java

@Service
public class RagClient {

    private final RestClient restClient;

    public RagClient(@Value("${rag.service.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    public QueryResponse query(String question, int topK) {
        var request = Map.of(
            "question", question,
            "top_k", topK,
            "collection_name", "knowledge_base"
        );

        return restClient.post()
                .uri("/query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(QueryResponse.class);
    }

    public void indexDocument(String docId, List<String> chunks) {
        var request = Map.of("doc_id", docId, "chunks", chunks);
        restClient.post()
                .uri("/index")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}

// Spring Boot 3.2+ 使用 RestClient（推荐）
// Spring Boot 3.0 以下可用 RestTemplate
// 或用 Feign / WebClient / Retrofit
```

### 3. 项目二：代码质量分析工具链

**纯 Python 自动化脚本**，服务你的 Java 项目。

```python
#!/usr/bin/env python3
"""
项目代码质量分析 —— 扫描 Java 项目，生成可视化报告。
展示能力：subprocess(调用工具) + Pandas(数据处理) + Matplotlib(可视化)
"""
import subprocess
import json
from pathlib import Path
from collections import Counter, defaultdict
import pandas as pd
import matplotlib.pyplot as plt


class JavaProjectAnalyzer:
    """Java 项目代码质量分析器。"""

    def __init__(self, project_root: Path):
        self.root = Path(project_root).resolve()

    def count_lines(self) -> pd.DataFrame:
        """统计各模块代码行数（去掉空行和注释）。"""
        records = []
        for java_file in self.root.rglob("*.java"):
            rel_path = java_file.relative_to(self.root)
            module = rel_path.parts[1] if len(rel_path.parts) > 2 else "root"
            content = java_file.read_text(encoding="utf-8")
            lines = [l for l in content.split("\n")
                     if l.strip() and not l.strip().startswith("//")
                     and not l.strip().startswith("*")
                     and not l.strip().startswith("/*")]
            records.append({
                "module": module,
                "file": str(rel_path),
                "total_lines": len(content.split("\n")),
                "code_lines": len(lines),
            })
        return pd.DataFrame(records)

    def analyze_imports(self) -> dict:
        """分析依赖导入频率。"""
        imports = Counter()
        for java_file in self.root.rglob("*.java"):
            for line in java_file.read_text(encoding="utf-8").split("\n"):
                if line.strip().startswith("import "):
                    pkg = line.strip().split("import ")[1].rstrip(";")
                    top_level = ".".join(pkg.split(".")[:2])
                    imports[top_level] += 1
        return imports.most_common(20)

    def checkstyle_report(self, checkstyle_jar: str, config_xml: str) -> str:
        """调用 Checkstyle 生成检查报告。"""
        result = subprocess.run(
            ["java", "-jar", checkstyle_jar, "-c", config_xml,
             str(self.root), "-f", "xml"],
            capture_output=True, text=True,
        )
        return result.stdout if result.returncode <= 1 else result.stderr

    def generate_report(self, output_dir: Path):
        """生成完整分析报告。"""
        output_dir = Path(output_dir)
        output_dir.mkdir(exist_ok=True)

        # 1. 代码行数统计
        df = self.count_lines()
        df.to_csv(output_dir / "line_counts.csv", index=False)

        # 2. 模块统计
        module_stats = df.groupby("module").agg(
            files=("file", "count"),
            total_lines=("total_lines", "sum"),
            code_lines=("code_lines", "sum"),
        ).sort_values("code_lines", ascending=False)

        # 3. 导入分析
        top_imports = self.analyze_imports()

        # 4. 可视化
        fig, axes = plt.subplots(1, 3, figsize=(18, 6))

        # 模块代码量柱状图
        module_stats["code_lines"].plot.bar(ax=axes[0], color="steelblue")
        axes[0].set_title("各模块代码行数")
        axes[0].set_ylabel("行数")

        # 代码密度饼图
        comment_ratio = (df["total_lines"].sum() - df["code_lines"].sum()) / df["total_lines"].sum() * 100
        axes[1].pie(
            [100 - comment_ratio, comment_ratio],
            labels=["有效代码", "注释/空行"],
            autopct="%1.1f%%",
            colors=["#4CAF50", "#9E9E9E"],
        )
        axes[1].set_title("代码密度")

        # TOP 10 依赖
        pkgs, counts = zip(*top_imports[:10]) if top_imports else ([], [])
        axes[2].barh(list(pkgs), list(counts), color="coral")
        axes[2].set_title("TOP 10 依赖包")
        axes[2].invert_yaxis()

        plt.tight_layout()
        plt.savefig(output_dir / "report.png", dpi=150, bbox_inches="tight")

        # 5. 保存汇总数据
        summary = {
            "total_files": len(df),
            "total_code_lines": int(df["code_lines"].sum()),
            "modules": len(module_stats),
            "top_import": top_imports[0] if top_imports else ("N/A", 0),
        }
        with open(output_dir / "summary.json", "w", encoding="utf-8") as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)

        print(f"报告已生成至: {output_dir}")
        print(f"  总文件数: {summary['total_files']}")
        print(f"  总代码行: {summary['total_code_lines']}")
        return summary


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Java 项目代码分析工具")
    parser.add_argument("project_dir", type=Path, help="Java 项目根目录")
    parser.add_argument("-o", "--output", type=Path, default=Path("analysis_report"))
    args = parser.parse_args()

    analyzer = JavaProjectAnalyzer(args.project_dir)
    analyzer.generate_report(args.output)
```

### 4. Java ↔ Python 通信方案对比

| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **HTTP REST** | ⭐ 推荐，通用 | 解耦、跨语言、易调试 | 网络开销 |
| **gRPC** | 高性能、流式 | 性能好、强类型 | 配置复杂 |
| **消息队列** | 异步任务 | 解耦、削峰 | 增加运维 |
| **subprocess** | 脚本/CLI 调用 | 简单直接 | 每次启动开销 |
| **GraalVM** | 嵌入式 | 零网络开销 | 需要 GraalVM |
| **Jupyter** | 探索分析 | 交互式 | 非生产 |

### 5. Docker 部署方案（Java + Python 联动）

```dockerfile
# Python 服务 Dockerfile
# Dockerfile.python

FROM python:3.11-slim

WORKDIR /app

# 先复制依赖文件，利用 Docker 缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY rag_service/ ./rag_service/

# 暴露端口
EXPOSE 8000

CMD ["uvicorn", "rag_service.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

```yaml
# docker-compose.yml —— 一键启动 Java + Python
version: "3.8"

services:
  java-backend:
    build:
      context: .
      dockerfile: Dockerfile.java
    ports:
      - "8080:8080"
    environment:
      - RAG_SERVICE_URL=http://python-rag:8000
      - DB_URL=jdbc:mysql://mysql:3306/app
    depends_on:
      - mysql
      - python-rag

  python-rag:
    build:
      context: ./python
      dockerfile: Dockerfile.python
    ports:
      - "8000:8000"
    volumes:
      - ./chroma_db:/app/chroma_db
      - ./models:/app/models
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: app
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

### 6. 错误处理与容错

```java
// Java 侧调用 Python 服务时的容错处理

@Service
public class ResilientRagClient {

    private final RestClient restClient;

    public QueryResponse queryWithRetry(String question) {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return callRagService(question);
            } catch (TimeoutException e) {
                if (attempt == maxRetries - 1) throw new ServiceUnavailableException("RAG 服务超时");
                log.warn("RAG 服务超时，重试 {}/{}", attempt + 1, maxRetries);
            } catch (IOException e) {
                if (attempt == maxRetries - 1) throw new ServiceUnavailableException("RAG 服务不可用");
                log.warn("RAG 服务连接失败，重试 {}/{}", attempt + 1, maxRetries);
            }
        }
        throw new ServiceUnavailableException("RAG 服务不可用");
    }

    // Spring Boot 3.1+ 可使用 @Retryable 注解简化
    @Retryable(
        retryFor = {IOException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public QueryResponse query(String question) {
        return callRagService(question);
    }

    @Recover
    public QueryResponse fallback(Exception e, String question) {
        log.error("RAG 服务最终失败: {}", e.getMessage());
        return QueryResponse.error("AI 服务暂时不可用，请稍后重试");
    }

    private QueryResponse callRagService(String question) {
        // ... HTTP 调用逻辑
        return null;
    }
}
```

---

## 📋 项目检查清单

启动任何一个 Java + Python 联动项目前，确认以下事项：

- [ ] Python 服务有 `/health` 端点，Java 侧启动时主动检查
- [ ] HTTP 调用设置合理的 connect/read timeout
- [ ] Java 侧对 Python 服务调用做了重试和降级
- [ ] Python 服务有异常处理，不直接崩溃
- [ ] 敏感配置（API Key、密码）使用环境变量，不硬编码
- [ ] Python 服务有 Dockerfile，可独立部署
- [ ] docker-compose.yml 可一键启动完整环境
- [ ] README 有明确的启动步骤

---

## 🏆 简历写法建议

> **"基于 Java Spring Boot + Python FastAPI 构建智能文档问答系统"**

**技术栈**：Java 17, Spring Boot 3, Spring Security, MySQL, Python 3.11, FastAPI, PyTorch, ChromaDB, Docker

**职责与成果**：
- Java 侧负责用户认证、文档管理、权限控制，Python 侧负责文本 Embedding、向量检索与 LLM 交互
- 采用 REST API 实现双语言服务解耦，通过 Docker Compose 一键部署
- Python 服务使用 FastAPI 自动生成 Swagger 文档，支持 Java 团队快速对接
- 实现重试与降级策略，Python 服务不可用时 Java 侧自动 fallback 为兜底回答

---

> **上一阶段** ← [04-AI 与数据科学生态](04-AI与数据科学生态.md)
> **下一阶段** → [06-开发环境与工具链](06-开发环境与工具链.md)
