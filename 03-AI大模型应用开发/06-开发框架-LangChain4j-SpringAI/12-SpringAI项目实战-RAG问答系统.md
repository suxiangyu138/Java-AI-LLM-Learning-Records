# 12 - Spring AI 项目实战：RAG 问答系统

> 🎯 从零搭建一个生产级 RAG 知识库问答系统 — 完整项目结构、Maven 配置、应用代码、Docker Compose 部署。Spring Boot 4.1 + Spring AI 2.0 + Java 21 + PostgreSQL/pgvector + Redis

---

## 目录

1. [项目概述与技术选型](#1-项目概述与技术选型)
2. [Maven 依赖与配置](#2-maven-依赖与配置)
3. [完整项目结构](#3-完整项目结构)
4. [核心代码实现](#4-核心代码实现)
5. [Docker Compose 部署](#5-docker-compose-部署)
6. [生产级增强清单](#6-生产级增强清单)

---

## 1. 项目概述与技术选型

```text
项目名：knowledge-qa — 企业内部知识库 RAG 问答系统

功能：
├── 文档上传（PDF/Word/Markdown/TXT）→ 自动解析/分块/向量化入库
├── 知识库问答 → 检索相关片段 → 注入 LLM → 生成答案（附引用来源）
├── 多轮对话记忆（按会话隔离，Redis 持久化）
├── 流式 SSE 输出（打字机效果）
└── 管理端：文档列表/删除/重建索引

技术栈：
├── Spring Boot 4.1 + Java 21
├── Spring AI 2.0 + ChatClient + RAG Advisor
├── PostgreSQL 16 + pgvector（关系数据 + 向量数据同一数据库）
├── Redis 7（会话记忆 + 分布式限流）
├── Apache Tika（文档解析，支持 PDF/Word/PPT/Excel/HTML）
└── Docker Compose 一键部署
```

---

## 2. Maven 依赖与配置

### 2.1 pom.xml（核心依赖）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependencies>
    <!-- Spring AI 核心 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- RAG 组件 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-tika-document-reader</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>

    <!-- Web + 流式 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- 数据库 + 缓存 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2.2 application.yml

```yaml
spring:
  application.name: knowledge-qa
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}                        # 环境变量注入
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-5-turbo
          temperature: 0.3                              # RAG 场景温度建议 0.1-0.3
      embedding:
        options:
          model: text-embedding-3-small
    vectorstore:
      pgvector:
        host: localhost
        port: 5432
        database: knowledge_qa
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        index-type: HNSW                                # HNSW 索引，毫秒级检索
        dimensions: 1536                                # text-embedding-3-small = 1536 维
  data:
    redis:
      host: localhost
      port: 6379
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_qa
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

---

## 3. 完整项目结构

```text
knowledge-qa/
├── pom.xml
├── docker-compose.yml
├── src/main/java/com/example/knowledgeqa/
│   ├── KnowledgeQaApplication.java
│   ├── config/
│   │   ├── ChatClientConfig.java          # ChatClient Bean（注入 RAG Advisor）
│   │   └── RedisConfig.java               # Redis 序列化配置
│   ├── controller/
│   │   ├── ChatController.java            # REST 聊天接口
│   │   └── DocumentController.java        # 文档管理接口
│   ├── service/
│   │   ├── DocumentIngestionService.java   # 文档解析/分块/向量化入库
│   │   └── ChatService.java               # 问答业务逻辑
│   ├── model/
│   │   ├── DocumentRecord.java             # JPA 实体现有文件元数据
│   │   └── ChatRequest.java                # 聊天请求 DTO
│   ├── repository/
│   │   └── DocumentRepository.java         # JPA 仓库
│   └── limiter/
│       └── RateLimiterAspect.java          # Redis+Lua 分布式限流
└── src/main/resources/
    ├── application.yml
    └── prompts/
        └── system-qa.st                     # System Prompt 模板
```

---

## 4. 核心代码实现

### 4.1 ChatClient 配置（注入 RAG Advisor）

```java
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  VectorStore vectorStore) {
        // RAG Advisor：自动检索 + 注入上下文
        var ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .topK(4)                         // 召回 4 个最相关片段
            .similarityThreshold(0.7)        // 相似度阈值 0.7
            .build();

        return builder
            .defaultAdvisors(ragAdvisor)
            .build();
    }
}
```

### 4.2 文档解析与向量化入库

```java
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingest(MultipartFile file) {
        // ① 临时保存上传文件
        Path tmpFile = Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tmpFile);

        // ② Tika 解析文档（自动识别 PDF/Word/Excel/PPT/HTML/Markdown）
        var reader = new TikaDocumentReader(new FileSystemResource(tmpFile));

        // ③ Token 级分块（800 Token + 100 重叠）
        var splitter = new TokenTextSplitter(800, 100, 5, 1000, true);

        // ④ 注入元数据（文件名、上传时间）
        List<Document> documents = reader.read();
        documents.forEach(doc -> {
            doc.getMetadata().put("source", file.getOriginalFilename());
            doc.getMetadata().put("upload_time", Instant.now().toString());
        });

        // ⑤ 分块 → 向量化 → 写入 pgvector
        List<Document> chunks = splitter.split(documents);
        vectorStore.add(chunks);

        // ⑥ 清理临时文件
        Files.deleteIfExists(tmpFile);
    }
}
```

### 4.3 聊天接口（SSE 流式 + 对话记忆）

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        // 每个请求重新构建（可动态注入不同 Advisor）
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody ChatRequest request) {
        // 流式输出：Flux<String> → Flux<ServerSentEvent>
        return chatClient.prompt()
            .user(request.message())
            .advisors(a -> a.param("chat_memory_conversation_id", request.sessionId()))
            .stream()
            .content()
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build());
    }
}
```

### 4.4 Redis + Lua 分布式限流

```java
@Aspect
@Component
public class RateLimiterAspect {

    private final StringRedisTemplate redis;

    private static final String SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current = redis.call('INCR', key)
        if current == 1 then
            redis.call('EXPIRE', key, window)
        end
        if current > limit then
            return 0
        end
        return 1
        """;

    @Around("@annotation(rateLimited)")
    public Object check(ProceedingJoinPoint jp, RateLimited rateLimited) {
        // 按 IP + 接口 生成限流 Key
        String key = "rate:" + getClientIp() + ":" + jp.getSignature().getName();
        Long allowed = redis.execute(
            new DefaultRedisScript<>(SCRIPT, Long.class),
            List.of(key),
            String.valueOf(rateLimited.limit()),
            String.valueOf(rateLimited.windowSeconds())
        );
        if (allowed == 0) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }
        return jp.proceed();
    }
}
```

### 4.5 System Prompt 模板

```text
# system-qa.st
你是一个专业的企业知识库助手。请基于以下参考文档回答用户的问题。

## 规则
1. 如果参考文档包含答案，请准确引用并标注来源
2. 如果参考文档不包含答案，请明确告知"根据现有知识库，我无法回答这个问题"
3. 回答简洁、专业，尽量使用列表或分点陈述

## 参考文档
{question_answer_context}

## 用户问题
{input}
```

---

## 5. Docker Compose 部署

```yaml
version: "3.8"
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - DB_USERNAME=knowledge_qa
      - DB_PASSWORD=secret123
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_AI_VECTORSTORE_PGVECTOR_HOST=postgres
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started

  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: knowledge_qa
      POSTGRES_USER: knowledge_qa
      POSTGRES_PASSWORD: secret123
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U knowledge_qa"]
      interval: 5s

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

volumes:
  pgdata:
  redis_data:
```

---

## 6. 生产级增强清单

| 增强项 | 方案 | 状态 |
|--------|------|:---:|
| 会话记忆持久化 | `RedisChatMemory`（按 sessionId 隔离） | ✅ |
| 分布式限流 | Redis + Lua 脚本（按 IP/用户/全局） | ✅ |
| 异步任务处理 | Redis Stream 处理耗时向量化任务 | ⬜ |
| 多模型路由 | 简单问题→DeepSeek（省钱）、复杂→Claude（精确） | ⬜ |
| 监控告警 | Prometheus + Grafana（Token 消耗/延迟/QPS） | ⬜ |
| 向量库升级 | pgvector（起步）→ Milvus（千万级以上向量） | 按需 |
| CI/CD | GitHub Actions + setup-uv + Docker 多阶段构建 | ⬜ |

---

> 🎯 **核心要点**：Spring AI RAG 项目的六步流水线 — **文档上传 → Tika 解析 → TokenTextSplitter 分块 → Embedding 向量化 → pgvector 入库 → ChatClient + QuestionAnswerAdvisor 检索问答**。生产落地三件套：**Redis 会话记忆 + Redis+Lua 分布式限流 + Docker Compose 一键部署**。

**下一模块**：[13-LangChain4j项目实战-Agent系统](13-LangChain4j项目实战-Agent系统.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
