# 企业级 RAG AI 知识库助手完整项目模板

> 企业级 RAG（检索增强生成）AI 知识库助手完整项目模板，适配企业实际应用场景，支持多格式文档加载、精准检索溯源，可灵活扩展适配不同业务需求（如客服知识库、内部文档查询等）。

---

## 目录

- [1. 项目结构和文件清单](#1-项目结构和文件清单)
- [2. 项目依赖](#2-项目依赖)
- [3. 核心配置与工具（rag_core.py）](#3-核心配置与工具rag_corepy)
- [4. Web 用户界面（app.py）](#4-web-用户界面apppy)
- [5. 生产级 API 服务（main.py）](#5-生产级-api-服务mainpy)
- [6. 环境变量配置（.env.example）](#6-环境变量配置envexample)
- [7. 容器化配置（Dockerfile）](#7-容器化配置dockerfile)
- [8. 快速开始指南](#8-快速开始指南)

---

## 1. 项目结构和文件清单

```
llm-rag-project/
├── app.py                 # 主程序入口 (Streamlit UI，供前端交互使用)
├── main.py                # API服务入口 (FastAPI，供后端集成调用)
├── rag_core.py            # RAG核心逻辑封装 (文档加载、向量存储、问答链路)
├── requirements.txt       # 项目依赖清单 (指定版本，避免环境冲突)
├── Dockerfile             # 容器化配置 (标准化部署，适配生产环境)
└── .env.example           # 环境变量示例 (敏感信息配置，避免硬编码)
```

---

## 2. 项目依赖：requirements.txt

```txt
# ========== 核心框架（RAG核心依赖） ==========
langchain==0.1.10
langchain-openai==0.0.5
langchain-community==0.0.25

# ========== 向量数据库（轻量本地部署） ==========
chromadb==0.4.24

# ========== 文档加载（支持主流办公文档格式） ==========
pypdf==4.1.0                # PDF文档加载
python-docx==0.8.11         # Word文档加载
textLoader==0.1.0           # 文本文件加载

# ========== API与Web ==========
fastapi==0.109.2            # 生产级API框架
uvicorn==0.27.1             # FastAPI启动服务器
streamlit==1.31.1           # 快速构建Web UI

# ========== 数据处理 ==========
pandas==2.2.1
numpy==1.26.4

# ========== 环境变量 ==========
python-dotenv==1.0.1
```

---

## 3. 核心配置与工具：rag_core.py

### 3.1 导入与全局常量

```python
import os
from dotenv import load_dotenv
from langchain_community.document_loaders import PyPDFLoader, Docx2txtLoader, TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate

# 加载环境变量
load_dotenv()

# 全局常量
PERSIST_DIRECTORY = "./chroma_db"        # 向量数据库持久化目录
DOCUMENTS_DIRECTORY = "./docs"           # 待加载文档存放目录
RETRIEVE_TOP_K = 4                       # 检索最相似的文档块数量
LLM_MAX_TOKENS = 2000                    # 大模型最大输出token数
LLM_TEMPERATURE = 0.1                    # 大模型温度（低温减少幻觉）
```

### 3.2 Prompt 模板

```python
CUSTOM_PROMPT = PromptTemplate(
    input_variables=["context", "question"],
    template="""
你是专业的企业AI知识库助手，仅基于提供的上下文片段回答用户问题，严格遵循以下规则：
1. 回答必须精准、简洁，贴合企业实际业务场景，不添加无关内容；
2. 若上下文未包含问题相关信息，直接回复"根据当前信息无法回答该问题"，严禁编造答案；
3. 回答需标注引用来源，格式为"【来源：文件名，页码：X】"，多个来源用分号分隔；
4. 若同一问题有多个相关文档片段，整合信息后统一回答，避免重复。

上下文: {context}
用户问题: {question}
请给出详细、准确、结构化的回答：
"""
)
```

### 3.3 RAGAssistant 核心类

```python
class RAGAssistant:
    """
    RAG助手核心类，封装文档加载、向量存储、检索问答全流程
    采用面向对象设计，低耦合、高复用，便于后期扩展
    """

    def __init__(self):
        """初始化助手，加载或创建向量数据库，初始化大模型和问答链"""
        self.llm = None
        self.vector_store = None
        self.qa_chain = None
        self._init_llm()
        self._init_or_load_vector_store()

    def _init_llm(self):
        """初始化大语言模型，从环境变量读取配置"""
        api_key = os.getenv("OPENAI_API_KEY")
        model_name = os.getenv("MODEL_NAME", "gpt-3.5-turbo")
        if not api_key:
            raise ValueError("请在.env文件中配置OPENAI_API_KEY")
        self.llm = ChatOpenAI(
            api_key=api_key,
            model_name=model_name,
            temperature=LLM_TEMPERATURE,
            max_tokens=LLM_MAX_TOKENS,
            request_timeout=30
        )

    def _init_or_load_vector_store(self):
        """初始化或加载向量数据库，实现增量更新"""
        embeddings = OpenAIEmbeddings()
        if os.path.exists(PERSIST_DIRECTORY) and os.listdir(PERSIST_DIRECTORY):
            self.vector_store = Chroma(
                persist_directory=PERSIST_DIRECTORY,
                embedding_function=embeddings
            )
            print(f"成功加载已存在的向量库，路径：{PERSIST_DIRECTORY}")
        else:
            self._build_vector_store_from_docs(embeddings)
        self._build_qa_chain()

    def _build_vector_store_from_docs(self, embeddings):
        """
        从docs目录读取所有支持格式的文档，构建向量数据库并持久化
        时间复杂度: O(n)，n为文档总字符数
        空间复杂度: O(m)，m为文档分块数
        """
        all_documents = []
        if not os.path.exists(DOCUMENTS_DIRECTORY):
            os.makedirs(DOCUMENTS_DIRECTORY)
            print(f"目录 {DOCUMENTS_DIRECTORY} 已创建，请放入文档后重启程序")
            return
        for filename in os.listdir(DOCUMENTS_DIRECTORY):
            file_path = os.path.join(DOCUMENTS_DIRECTORY, filename)
            try:
                if filename.endswith(".pdf"):
                    loader = PyPDFLoader(file_path)
                elif filename.endswith(".docx"):
                    loader = Docx2txtLoader(file_path)
                elif filename.endswith(".txt"):
                    loader = TextLoader(file_path, encoding='utf-8')
                else:
                    print(f"不支持的文件格式: {filename}，跳过")
                    continue
                documents = loader.load()
                for doc in documents:
                    doc.metadata["filename"] = filename
                all_documents.extend(documents)
                print(f"成功加载文档: {filename}，共{len(documents)}页")
            except Exception as e:
                print(f"加载文档 {filename} 失败: {str(e)}，跳过")
        if not all_documents:
            print("未加载到任何有效文档，请检查docs目录")
            return
        # 文本分块
        text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=800,
            chunk_overlap=80,
            length_function=len,
            separators=["\n\n", "\n", ". ", " ", ""]
        )
        splits = text_splitter.split_documents(all_documents)
        self.vector_store = Chroma.from_documents(
            documents=splits,
            embedding=embeddings,
            persist_directory=PERSIST_DIRECTORY
        )
        self.vector_store.persist()
        print(f"向量数据库构建完成，共存储 {len(splits)} 个文档块")

    def _build_qa_chain(self):
        """构建检索问答链，关联大模型和向量检索器"""
        if not self.vector_store or not self.llm:
            raise RuntimeError("向量库或大模型未初始化")
        retriever = self.vector_store.as_retriever(
            search_kwargs={"k": RETRIEVE_TOP_K},
            search_type="similarity"
        )
        self.qa_chain = RetrievalQA.from_chain_type(
            llm=self.llm,
            chain_type="stuff",
            retriever=retriever,
            chain_type_kwargs={"prompt": CUSTOM_PROMPT},
            return_source_documents=True
        )

    def ask(self, question):
        """
        对外暴露的标准化问答接口
        时间复杂度: O(n + m log m)，n为文档分块数，m为检索结果数
        空间复杂度: O(m)，m为检索结果数
        """
        if not self.qa_chain:
            raise RuntimeError("QA问答链未初始化")
        result = self.qa_chain.invoke({"query": question})
        answer = result["result"].strip()
        sources = []
        for doc in result["source_documents"]:
            source_info = {
                "source": doc.metadata.get("source", "未知路径"),
                "filename": doc.metadata.get("filename", "未知文件"),
                "page": doc.metadata.get("page", "未知页码")
            }
            if source_info not in sources:
                sources.append(source_info)
        return {"answer": answer, "sources": sources}
```

---

## 4. Web 用户界面：app.py

```python
import streamlit as st
from rag_core import RAGAssistant

# 页面基础配置
st.set_page_config(
    page_title="企业AI知识库助手",
    page_icon="📚",
    layout="wide",
    initial_sidebar_state="expanded"
)

# 初始化会话状态
if "rag_assistant" not in st.session_state:
    try:
        st.session_state.rag_assistant = RAGAssistant()
        st.success("知识库助手初始化成功，可开始提问！")
    except Exception as e:
        st.error(f"初始化失败: {str(e)}")
        st.stop()

# 页面标题
st.title("📚 企业AI知识库助手")
st.caption("基于RAG技术的企业级智能问答系统 | 安全·准确·可溯源 | 支持PDF/Word/TXT文档查询")

# 提问区域
st.subheader("请输入您的问题", divider="gray")
question = st.text_input(
    label="提问输入框",
    placeholder="例如：公司的差旅报销标准是什么？",
    label_visibility="collapsed"
)

# 提问处理
if st.button("提交提问", type="primary", use_container_width=True):
    if not question.strip():
        st.warning("请输入有效的问题后再提交！")
    else:
        with st.spinner("正在检索知识库并生成回答..."):
            try:
                response = st.session_state.rag_assistant.ask(question)
                st.subheader("回答结果", divider="gray")
                st.write(response["answer"])
                st.subheader("参考来源", divider="gray")
                if response["sources"]:
                    for idx, source in enumerate(response["sources"], 1):
                        st.write(f"{idx}. 文件：{source['filename']} | 路径：{source['source']} | 页码：{source['page']}")
                else:
                    st.write("无明确参考来源（回答基于模型通用知识）")
            except Exception as e:
                st.error(f"处理失败: {str(e)}，请检查配置或重试！")

# 侧边栏说明
with st.sidebar:
    st.header("📖 使用说明", divider="blue")
    st.write("1. 文档准备：将PDF/Word/TXT文档放入项目根目录的 `./docs` 文件夹")
    st.write("2. 重启程序：放入文档后，重启Web服务，确保文档被加载到知识库")
    st.write("3. 提问技巧：提问请尽量具体，提升回答精准度")
    st.write("4. 注意事项：若回答显示"无法回答"，请检查文档是否包含相关内容")
    st.header("⚙️ 配置提示", divider="blue")
    st.write("请确保已复制 .env.example 为 .env，并正确配置 OPENAI_API_KEY")
```

---

## 5. 生产级 API 服务：main.py

```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from rag_core import RAGAssistant
import uvicorn

# 初始化FastAPI应用
app = FastAPI(
    title="企业AI知识库RAG API",
    version="1.0.0",
    description="基于RAG技术的企业级智能问答API，支持多格式文档检索，返回带溯源信息的回答",
    docs_url="/docs",
    redoc_url="/redoc"
)

# 全局初始化RAG助手
try:
    rag_assistant = RAGAssistant()
except Exception as e:
    raise RuntimeError(f"RAG助手初始化失败: {str(e)}")

# 请求数据模型
class QuestionRequest(BaseModel):
    question: str = Field(
        ..., title="用户问题",
        description="用户需要查询的自然语言问题",
        min_length=1, max_length=500
    )

# 响应数据模型
class SourceInfo(BaseModel):
    source: str = Field(description="文档路径")
    filename: str = Field(description="文档名称")
    page: str = Field(description="文档页码")

class AnswerResponse(BaseModel):
    answer: str = Field(description="问题的回答内容")
    sources: list[SourceInfo] = Field(description="回答的参考来源列表")

# 核心API路由
@app.post("/qa", response_model=AnswerResponse, summary="智能问答接口", tags=["核心功能"])
async def qa(request: QuestionRequest):
    """接收用户问题，返回AI回答及参考来源"""
    try:
        result = rag_assistant.ask(request.question)
        return AnswerResponse(answer=result["answer"], sources=result["sources"])
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"服务器内部错误: {str(e)}")

# 健康检查接口
@app.get("/health", summary="健康检查接口", tags=["基础功能"])
async def health():
    """健康检查接口，用于监控服务存活状态"""
    return {"status": "healthy", "message": "RAG API服务运行正常"}

# 启动服务器
if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        workers=4
    )
```

---

## 6. 环境变量配置：.env.example

```bash
# 请复制此文件并重命名为 .env，填入实际配置信息（不要提交到代码仓库）

# 大模型API密钥（必填）
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 大模型名称（可选，默认gpt-3.5-turbo）
MODEL_NAME=gpt-3.5-turbo

# 可选配置：国内模型API端点（若使用国内大模型，取消注释并修改）
# OPENAI_API_BASE=https://api.example.com/v1

# 可选配置：向量数据库持久化路径
# PERSIST_DIRECTORY=./custom_chroma_db
```

---

## 7. 容器化配置：Dockerfile

```dockerfile
# 基础镜像（轻量Python镜像）
FROM python:3.11-slim

# 设置工作目录
WORKDIR /app

# 复制依赖文件并安装
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && pip install curl

# 复制项目代码
COPY . .

# 创建docs目录（用于挂载外部文档）
RUN mkdir -p /app/docs \
    && chmod 755 /app/docs

# 暴露服务端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动命令
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4"]
```

---

## 8. 快速开始指南

### 8.1 准备环境

| 工具 | 说明 |
|------|------|
| Python 3.11 | 基础运行环境 |
| Git | 版本管理 |
| Docker Desktop | 可选，用于容器化部署 |

### 8.2 创建项目

```bash
mkdir -p llm-rag-project/docs
cd llm-rag-project
```

将上述 6 个文件（app.py、main.py、rag_core.py、requirements.txt、Dockerfile、.env.example）创建到项目目录中。

### 8.3 配置密钥

```bash
cp .env.example .env
# 编辑 .env 文件，填入 OPENAI_API_KEY
```

### 8.4 放入文档

将需要查询的 PDF、Word 或 TXT 文档放入 `./docs` 文件夹。

### 8.5 运行与测试

#### 安装依赖

```bash
pip install -r requirements.txt
```

#### 启动 Web UI

```bash
streamlit run app.py
```

浏览器自动打开 `http://localhost:8501`

#### 启动 API 服务

```bash
python main.py
```

访问 `http://localhost:8000/docs` 查看 API 文档并在线调试。

#### Docker 部署

```bash
# 构建镜像
docker build -t llm-rag-app .

# 运行容器
docker run -d -p 8000:8000 \
  -v $(pwd)/docs:/app/docs \
  --env-file .env \
  --name llm-rag-container \
  llm-rag-app
```

访问 `http://localhost:8000/docs` 即可调试 API。

---

> 此项目模板是标准的企业级 RAG 应用骨架，适配各类企业知识库场景。具备高可扩展性，可在此基础上快速扩展功能，例如集成微信/钉钉机器人、添加用户权限管理、支持更多文档格式（Excel、PPT）、对接企业内部系统等。
