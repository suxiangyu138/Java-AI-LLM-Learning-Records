# 07 RAG 入门 Demo

> 让大模型"读过你的资料"：用本地 Embedding 模型做文本向量化，实现"分块 → 向量化 → 检索 → 注入提示词 → 生成"的完整 RAG 最小链路。这是 Level2 知识库项目的核心预演。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [RAG 是什么：先理解再动手](#2-rag-是什么先理解再动手)
3. [环境准备：Embedding 模型](#3-环境准备embedding-模型)
4. [分块与向量化](#4-分块与向量化)
5. [检索与注入](#5-检索与注入)
6. [完整链路验证](#6-完整链路验证)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：输入几段自定义文本（比如你自己写的学习笔记），提问时模型能引用这些文本回答——**模型没学过的知识，通过检索注入回答出来**。验收标准：**能讲清 RAG 四步（分块/向量化/检索/注入）各解决了什么问题**；**检索不到相关内容时回答会怎样**（这是理解 RAG 边界的关键实验）；**能说清为什么 RAG 比"把全部资料塞进提示词"好**。

## 2. RAG 是什么：先理解再动手

RAG（检索增强生成）解决大模型的**知识时效与知识边界**问题：模型训练数据有截止时间、没有你的私有资料，直接问"我的项目里 XX 怎么写的"它答不出。RAG 的思路：**不改变模型，改变输入**——把相关资料先检索出来，拼进提示词再让模型回答。核心链路四步，每步解决一个问题：

1. **分块（Chunking）**：文档太长，一次塞不进提示词（且塞进去也稀释注意力）——切成几百字的小块；
2. **向量化（Embedding）**：模型不懂字符串比较，把文本变成向量（一串数字）——语义相近的文本向量距离近；
3. **检索（Retrieval）**：把问题也变成向量，找出与问题最相似的几个块——只取相关部分；
4. **注入（Injection）**：把检索结果拼进提示词（"根据以下资料回答：…"）——让模型基于资料生成。

为什么不用"全部塞进去"：一是 token 成本（几万字全塞 = 巨额费用），二是质量（无关内容干扰模型判断，长文本的注意力被稀释）。RAG 的哲学是**"只给模型需要的"**。面试必答的一句话：**RAG = 检索（拿什么回答）+ 生成（怎么回答）**，检索质量决定回答质量的上限。

## 3. 环境准备：Embedding 模型

向量化需要 Embedding 模型，2026-08 的省心方案是**本地跑**：用 Ollama 拉一个轻量 embedding 模型（如 `bge-m3` 或 `nomic-embed-text`，几百 MB，本机可跑），不依赖外部 API、不花钱。安装三步：装 Ollama（官网下载安装包）→ `ollama pull bge-m3` 拉模型 → `ollama serve` 确认服务起来（默认端口 11434）。验证 Embedding 可用：

```bash
curl http://localhost:11434/api/embeddings -d '{"model":"bge-m3","prompt":"你好"}'
# 返回 {"embedding":[0.012, -0.034, ...]}  → 一串向量即成功
```

两条替代路线知道即可：**云端 API**（DeepSeek 等平台的 embedding 接口，OpenAI 兼容）与 **Spring AI 的 EmbeddingModel 抽象**（`spring-ai-ollama-spring-boot-starter`，配置 model 名即可注入）。Demo 阶段用 Ollama 本地方案最稳——网络问题最少，且向量化是高频调用，本地零成本。

## 4. 分块与向量化

分块策略是本 Demo 唯一需要"调"的地方，先写一个朴素的按长度切分：每块 500 字符、重叠 50 字符（重叠防止关键内容恰好被切断在块边界）。为什么需要重叠：句子可能跨越切分点，重叠保证上下文连续——这是 RAG 调优的第一个参数，Level2 会系统性调优，这里先理解"为什么有重叠"。

```java
List<String> chunk(String text, int size, int overlap) {
    List<String> chunks = new ArrayList<>();
    for (int i = 0; i < text.length(); i += size - overlap) {
        chunks.add(text.substring(i, Math.min(i + size, text.length())));
    }
    return chunks;
}
```

向量化调用本地 Ollama（沿用 06 篇的 HttpClient 技能）：

```java
List<Float> embed(String text) throws Exception {
    String body = "{\"model\":\"bge-m3\",\"prompt\":" + JsonUtils.toJson(text) + "}";
    // POST http://localhost:11434/api/embeddings，解析返回的 embedding 数组
}
```

Demo 阶段把"文本 → 向量"的结果存内存（Map 或 List），**不接数据库**——Level2 才用向量数据库（Milvus 等）做规模化存储与检索。这一步的目标是理解向量是什么：一串 1024 维的浮点数，语义相近的文本向量距离更近（用余弦相似度衡量）。

## 5. 检索与注入

检索两步：**问题向量化**（用同一个 embedding 模型）→ **与所有文档块向量算相似度** → 取 Top 2-3 块。余弦相似度公式：`cos = (A·B) / (|A||B|)`，值越接近 1 越相似。Demo 用朴素实现（两层 for 循环），理解原理优先：

```java
double cosine(List<Float> a, List<Float> b) {
    double dot = 0, na = 0, nb = 0;
    for (int i = 0; i < a.size(); i++) {
        dot += a.get(i) * b.get(i); na += a.get(i) * a.get(i); nb += b.get(i) * b.get(i);
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
}
```

注入：把检索到的 Top 块拼进提示词——用文本块模板：

```java
String prompt = """
    根据以下资料回答用户问题。资料中没有的内容，请直接说明"资料中没有相关信息"。
    资料：
    %s
    问题：%s
    """.formatted(String.join("\n", topChunks), question);
```

注入模板的三句话设计是 RAG 的质量关键：**"根据以下资料回答"**（限定信息来源）、**"没有的内容直接说明"**（防模型编造——幻觉治理的第一道防线）、**资料在前问题在后**（让模型先读资料再作答）。把模板改一版（去掉"没有就说明"这句），对比模型回答的编造率——这个对比实验是理解"提示词设计影响 RAG 质量"的最直观证据。

## 6. 完整链路验证

把四步串成一个方法，验证链路是否真的"引用资料回答"：

```java
String ask(String question, List<Document> docs) {
    List<Float> qVec = embed(question);
    List<Document> top = docs.stream()
        .sorted(comparingDouble(d -> -cosine(qVec, d.vector())))
        .limit(3).toList();                      // 检索 Top3
    return llmClient.chat(buildPrompt(top, question));   // 注入 + 生成
}
```

三个验证实验必做：**正常问答**（资料里有答案 → 回答正确，且能引用资料内容）；**边界问答**（资料里没有的问题 → 模型说明"资料中没有"）；**对照实验**（同样的问题，不检索直接问模型 → 模型答不出或胡编）。三个实验做完，RAG 的原理与价值就完全具象化了。Demo 完成后再做一个小优化：把 top 数量与分块大小各调一次，观察回答质量变化——**亲手感受"参数影响效果"**，是 Level2 调优叙事的第一手素材。

## 7. 常见坑

**Ollama 服务没起**：`ollama serve` 在前台运行，另开终端调用；或确认 11434 端口没被占用。

**embedding 模型名写错**：`ollama list` 查看已拉取的模型，用准确的模型名。

**向量维度不一致**：两个不同模型产出的向量维度不同，算相似度会报错或结果无意义——全程只用同一个模型。

**返回"资料中没有"但资料里有**：检索没召回相关内容——分块太小/重叠不够、top 数量太少、问题表述与资料用词差异大（语义检索的边界）。调大 top、调整分块后重试。

**回答引用了资料外内容**：注入模板没限定"只根据资料回答"，或资料本身含无关内容。改模板 + 检查分块。

## 8. 核心要点

1. RAG 四步：分块（塞得下）、向量化（能比较）、检索（只取相关）、注入（限定来源）。
2. RAG 优于"全塞提示词"：省 token、提质量；检索质量决定回答上限。
3. 本地 Embedding 用 Ollama + bge-m3，零成本零网络依赖。
4. 注入模板三要素：根据资料回答、没有就直说、资料在前问题在后。
5. 三个验证实验（正常/边界/对照）做一遍，RAG 原理完全具象化。

> 🎯 **核心要点**：本 Demo 的完成标志不是"链路能跑"，而是**"检索不到时会发生什么"这个实验做过**——理解 RAG 的边界（依赖检索质量、可能答非所问），比只会跑通链路重要得多。这个边界认知是 Level2 设计知识库评估体系的起点。

---

**下一模块**：[08 Java + AI 最小闭环 Demo](./08-Java%20%2B%20AI%20最小闭环%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
