# 08 Java + AI 最小闭环 Demo

> Level1 的毕业里程碑：把 04 的数据、05 的缓存、06 的模型调用、07 的 RAG 串成一个完整应用——「上传文档 → 向量入库 → 知识问答」，外加会话记录入库与缓存加速。这是 Level2 简历项目的迷你版预演。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [闭环设计：四模块与数据流](#2-闭环设计四模块与数据流)
3. [文档上传与入库](#3-文档上传与入库)
4. [知识问答链路](#4-知识问答链路)
5. [会话记录与缓存加速](#5-会话记录与缓存加速)
6. [亲手调优一个参数](#6-亲手调优一个参数)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：一个完整的知识问答应用——前端（或 Postman）上传文本/笔记 → 系统分块向量化入库 → 用户提问 → 系统检索资料 + 大模型生成回答 → 对话记录存 MySQL。验收标准：**四模块（接口/存储/AI/RAG）职责清晰可讲**；**全链路数据流能画出并讲清**；**能做一次参数调优并对比效果**；**独立重写任意一个模块**。这是 Level1 的"毕业设计"，完成它代表你已经打通 Java + AI 全链路。

## 2. 闭环设计：四模块与数据流

先设计再编码——闭环拆四个模块，每个模块的职责一句话讲清：**接口层**（Controller：接收上传与提问请求）；**数据层**（MySQL：文档元数据、对话记录；Redis：问答缓存）；**AI 层**（大模型调用：对话生成）；**RAG 层**（分块、向量化、相似度检索）。数据流一条线：**上传文档 → 存元数据 → 分块 → 向量化 → 存内存向量库 → 提问 → 检索 Top3 → 拼提示词 → 调大模型 → 存对话 → 返回**。

```text
[上传文档] → 接口层 → 数据层(存文档元数据) → RAG层(分块→向量化→存向量)
[提问]     → 接口层 → RAG层(问题向量→检索Top3) → AI层(注入+生成)
                     → 数据层(存对话记录) → 返回回答
```

设计时的两个取舍点提前想好（这是面试级思考）：**向量存内存**——Demo 阶段文档量小，List 存向量即可，重启丢失可接受（文档重新上传）；Level2 换 Milvus 才需要持久化向量。**上传与提问同步处理**——文档小、向量化快，同步返回即可；大文档的异步处理（MQ）是 Level2 内容。这个设计文档值得写进项目 README——它是 Level2 项目文档化的预演。

## 3. 文档上传与入库

接口一：上传文档。Spring MVC 的文件上传 + 元数据入库：

```java
@PostMapping("/docs/upload")
public Result upload(@RequestParam("file") MultipartFile file,
                     @RequestParam("title") String title) throws IOException {
    String content = new String(file.getBytes(), StandardCharsets.UTF_8);
    // 1. 存文档元数据到 MySQL（title、content、chunkCount、createTime）
    DocEntity doc = docService.save(title, content);
    // 2. 分块 + 向量化 + 存内存向量库（每块一个向量，记录 docId 与块序号）
    ragService.index(doc.getId(), content);
    return Result.ok(doc);
}
```

两个工程要点：**文件大小限制**——Spring Boot 默认单文件 1MB，`spring.servlet.multipart.max-file-size` 调大（Demo 设 10MB）；**编码**——读取文件内容显式指定 UTF-8（否则 Windows 上传的 GBK 文本乱码，与 04 篇的编码纪律一致）。RAG 层索引方法 `index()` 内部就是 07 篇的"分块 → 逐块向量化 → 存起来"，把 07 的代码整体搬过来并加上 docId 关联——**闭环的价值就是复用**，前面每个 Demo 的代码在这里汇合。

## 4. 知识问答链路

接口二：知识问答。检索 + 注入 + 生成 + 记录：

```java
@PostMapping("/chat")
public String chat(@RequestBody ChatRequest req) {
    // 1. 检索：问题向量化，与所有块算相似度，取 Top3
    List<Chunk> top = ragService.search(req.question(), 3);
    // 2. 注入：拼接"根据资料回答"提示词
    String prompt = buildPrompt(top, req.question());
    // 3. 生成：调大模型（06 篇的 llmClient）
    String answer = llmClient.chat(prompt);
    // 4. 记录：对话存 MySQL（question、answer、docIds、createTime）
    chatService.save(req.question(), answer, top.stream().map(Chunk::docId).toList());
    return answer;
}
```

链路里最容易出问题的一环是**空检索**：知识库还没有文档或检索不到相关内容时，回答质量失控。处理方式（Demo 级）：检索为空时返回提示"知识库暂无相关内容，请先上传文档"，不调大模型——**省一次调用也避免一次幻觉**。这个处理是"边界条件"思维的体现，Level2 的故障推演表里它是第一个条目。

## 5. 会话记录与缓存加速

把 04、05 的技能接入闭环，完成"全链路都有数据落地"：**对话记录表**（`chat_record`：id、doc_ids、question、answer、created_at）——每次问答存一条，前端可查历史，这也是 Level2 审计能力的雏形；**问答缓存**（Redis）——相同问题 5 分钟内命中缓存直接返回，不调大模型（省成本，大模型调用是花钱的，缓存是成本治理的第一手段）：

```java
public String chatWithCache(ChatRequest req) {
    String key = "chat:" + req.question().hashCode();
    String cached = redis.opsForValue().get(key);
    if (cached != null) return cached;                 // 命中缓存，零成本
    String answer = doChat(req);                        // 未命中：完整链路
    redis.opsForValue().set(key, answer, Duration.ofMinutes(5));
    return answer;
}
```

缓存 key 用问题哈希（问题本身含中文与特殊字符，直接做 key 太长），TTL 5 分钟——**相同问题短时间重复问是真实场景**（用户重复提问、前端重试），缓存命中率可观察：连问两次同一个问题，第二次接口毫秒级返回，日志无大模型调用。这里把 05 篇的"旁路缓存"在真实业务里用了一次，一致性问题的答案（先更新库再删缓存）在这里有具体落点。

## 6. 亲手调优一个参数

闭环完成后，做一次真实的参数调优并记录对比——这是 Level1 到 Level2 的能力跃迁动作：**选一个参数（推荐分块大小或 top-k），用同一组问题测两版效果，记录结果**。操作步骤：准备 5 个覆盖不同场景的测试问题（文档内明确答案的、需跨块组合的、文档外的）→ 当前参数跑一遍记录回答质量 → 改参数（分块 500 → 1000，或 top-k 3 → 5）→ 再跑一遍对比 → 记录哪个参数组合更好、为什么。

调优的价值不在"找到最优参数"，而在**建立"效果可度量"的方法**：测试集 + 对比记录，是 Level2 评估体系的最小原型。把这次调优的过程写进 README（参数、测试问题、对比结果、结论）——**"我调过参数且能讲出过程"**，比"我的 RAG 能跑"在面试里的价值高一个量级。

## 7. 常见坑

**上传文件乱码**：文件读取没指定 UTF-8，或上传端本身是 GBK。读取时显式 `StandardCharsets.UTF_8`，与前端约定 UTF-8。

**检索结果为空**：文档没入库成功（检查 docService.save 与 ragService.index 是否都执行）、向量维度不一致、问题与资料用词差异太大。按顺序排查：查库有文档吗 → 向量存了吗 → 相似度算对了吗。

**回答与资料无关**：注入提示词没限定来源，或 top-k 检索到的块不相关。改提示词模板，调大 top 并检查检索排序。

**内存向量库重启丢失**：Demo 设计如此（可接受），不要为了"重启数据还在"引入文件持久化——那是 Level2 的 Milvus 的职责。

**大文档上传超时**：同步向量化在文档大时会慢。Demo 限文件大小（10MB），超大文档放 Level2 做异步。

## 8. 核心要点

1. 闭环四模块（接口/数据/AI/RAG）职责清晰，数据流一条线能画能讲。
2. 复用是闭环的核心：07 的 RAG 代码、06 的模型调用、04/05 的数据技能在此汇合。
3. 边界处理先行：检索为空时提示而非硬调模型，省成本也防幻觉。
4. 对话记录入库（审计雏形）+ 问答缓存（成本治理第一手段）让链路完整落地。
5. 亲手做一次参数调优并记录对比——这是 Level2 评估体系的原型。

> 🎯 **核心要点**：本 Demo 是 Level1 的毕业考——**所有前置技能在一张数据流图上汇合**。完成它的标志不是"能用"，而是"每个模块的代码你都亲手写过、每个数据流的走向你都能画出来"。达到这个状态，Level2 工程化项目就只是把这张图做厚，而不是重新开始。

---

**下一模块**：[09 Demo 常见问题与调试速查](./09-Demo%20常见问题与调试速查.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
