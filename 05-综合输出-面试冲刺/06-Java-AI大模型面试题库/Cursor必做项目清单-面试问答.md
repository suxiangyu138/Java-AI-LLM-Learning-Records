# Cursor 必做项目清单 面试问答清单
> 🎯 基于 Cursor 从入门到全栈 AI 开发的项目清单，涵盖面试高频问题与完美解答方案。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Cursor IDE 和传统的 VS Code + GitHub Copilot 有什么区别？为什么选择 Cursor？

**面试官意图：** 考察对 AI 编程工具选型的理解深度和实操经验。

**完美解答：**

| 维度 | VS Code + Copilot | Cursor IDE |
|------|-------------------|------------|
| 底层架构 | VS Code 插件 | 基于 VS Code 的独立 IDE |
| AI 交互 | 内联补全 + Chat 面板 | 内联补全 + Chat + Composer（多文件编辑）+ Ctrl K |
| 多文件编辑 | 不支持（逐文件操作） | Composer 支持多文件同时修改 |
| 上下文理解 | 当前文件 + 附近代码 | 整个项目索引 + 代码库搜索 |
| 代码库问答 | 不支持 | 支持 @Codebase 直接提问整个项目 |
| 应用场景 | 代码补全为主 | 代码生成 + 重构 + Debug + 文档全流程 |

**核心差异：**

1. **多文件编辑能力**：Cursor 的 Composer 功能可以同时生成/修改多个文件，比如生成 CRUD 时一次性创建 Entity/Mapper/Service/Controller，这是 VS Code + Copilot 做不到的
2. **项目级问答**：在 Cursor 中输入 `@Codebase 这个项目用了哪些设计模式？`，它能分析整个项目回答
3. **Diffs 预览**：每次修改前都能看到 diff，可以逐行审核，避免 AI 改出没预料到的问题

**延伸追问应对：** 如果面试官问"那用 Cursor 还是 Claude Code 好"，回答：两者定位不同，Cursor 是 IDE 内嵌式 AI 辅助，更适合日常编码的"边写边问"；Claude Code 是终端 Agent 模式，更适合批量重构、大型架构改动和 CI/CD 自动化。我两个都用，场景互补。

---

### Q2：你平时用 Cursor 的哪些功能最多？怎么设计高效的 Prompt？

**面试官意图：** 考察是否真正把 Cursor 用到了工程中，还是只停留在"尝鲜"阶段。

**完美解答：**

**我的 Cursor 功能使用频率排名：**

| 功能 | 使用频率 | 使用场景 |
|------|---------|----------|
| Ctrl+K（代码生成/编辑） | 每天 50+ 次 | 生成函数、修改逻辑、补充注释 |
| Composer（多文件编辑） | 每天 10+ 次 | 生成完整模块、批量重构 |
| Chat 面板 | 每天 20+ 次 | 代码审查、技术咨询、调试 |
| @Codebase 项目问答 | 每天 5+ 次 | 理解项目结构、查找用法 |
| Bug 自动修复 | 每周 15+ 次 | 选中错误代码让 AI 修复 |

**高效的 Prompt 公式：**

我总结了一套"角色 + 需求 + 技术栈 + 规范约束 + 输出格式"的五要素 Prompt：

```
// 不好的 Prompt：
"帮我写个用户管理接口"

// 好的 Prompt（五要素齐全）：
你是一个 Java 高级工程师，帮我为系统用户模块开发一个分页查询接口。
技术栈：Spring Boot 3.2 + MyBatis-Plus 3.5 + Jakarta Validation
要求：
1. 支持按用户名、邮箱、状态筛选
2. 使用 @Validated 做参数校验
3. 返回 Result<PageResult<UserVO>> 统一格式
4. 包含 Swagger 注解
5. name 字段必须做防 XSS 转义

输出：Controller + Service + Mapper 三个文件的完整代码。
```

**实际效果对比：**
- 模糊 Prompt：生成的代码可能需要反复修改 3-5 轮
- 结构化 Prompt：一次生成的代码直接可用率约 80%，只需微调

> 💡 **面试亮点**：讲出"五要素Prompt"这种方法论，说明你形成了可复用的 AI 交互模式，而不是随便用用。

---

### Q3：你用 Cursor 做过代码审查和性能调优吗？具体是怎么操作的？

**面试官意图：** 考察利用 AI 做代码质量管控的工程化思维。

**完美解答：**

我总结了 Cursor 代码审查的"三查三改"方法论：

**三查：让 Cursor 从三个维度审查代码**

```text
// 审查 1：安全性审查
选中代码 → Ctrl+K → 输入：
"审查这段代码的安全性问题：SQL 注入、XSS、CSRF、敏感信息泄露"

// 审查 2：性能审查
"审查这段代码的性能瓶颈：循环内查询、N+1 问题、内存浪费、不必要的对象创建"

// 审查 3：可维护性审查
"审查违反 Clean Code 原则的地方：过长函数、命名不规范、缺少注释、重复代码"
```

**三改：按严重程度分层处理**

| 严重级别 | 处理方式 | 示例 |
|----------|---------|------|
| P0（阻塞） | Cursor 直接修复 + 人工确认 | SQL 注入、空指针 |
| P1（重要） | Cursor 修改后人工审查 | 性能瓶颈、资源泄漏 |
| P2（建议） | 批量处理 | 命名规范、代码格式 |

**实际案例：性能审查发现的 N+1 问题**

```java
// 问题代码
@OneToMany
private List<Order> orders;

// Controller 中查询
List<User> users = userService.list();
for (User user : users) {
    // 每次循环触发一次 SQL 查询
    System.out.println(user.getOrders().size());
}

// Cursor 审查后给出的优化方案
// 方案 1：JOIN FETCH
@Query("SELECT u FROM User u JOIN FETCH u.orders")
List<User> findAllWithOrders();

// 方案 2：@EntityGraph
@EntityGraph(attributePaths = "orders")
@Query("SELECT u FROM User u")
List<User> findAllWithOrders();
```

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你怎么用 Cursor 生成 Spring Boot 的 CRUD 代码？对比手写效率提升多少？

**面试官意图：** 考察实际开发效率提升的量化数据。

**完美解答：**

**Prompt 示例：**
```
基于以下表结构，生成 Spring Boot 3 + MyBatis-Plus 的商品管理完整 CRUD：

CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `status` tinyint DEFAULT '1' COMMENT '状态 1上架 0下架',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`)
);

要求：
- 分页查询 + 按分类/名称/价格范围筛选
- 新增时校验分类是否存在
- 批量上下架接口
- 统一返回体 Result，全局异常处理
```

**Cursor 使用 Composer 一次性生成：**
```
src/main/java/com/example/product/
├── entity/Product.java
├── mapper/ProductMapper.java (含分页插件)
├── service/ProductService.java + impl
├── controller/ProductController.java (7 个接口)
├── dto/ProductQueryDTO.java
├── dto/ProductCreateDTO.java
├── dto/ProductUpdateDTO.java
└── dto/BatchStatusDTO.java
```

**效率对比：**
```text
任务：30 张表的 CRUD 后台管理系统

手写工作量：
  每张表平均：Entity(10min) + Mapper(5min) + Service(15min) + Controller(15min) + DTO(10min) = 55min
  30 张表：1650分钟 ≈ 27.5 小时 ≈ 3.5 个工作日

Cursor AI 辅助：
  每张表平均：写 Prompt(2min) + 审查代码(5min) + 微调(3min) = 10min
  30 张表：300分钟 ≈ 5 小时

效率提升：约 5-6 倍
```

**但需要注意**：AI 生成的代码需要关注的点：
1. 事务边界是否正确？（Service 层是否加了 @Transactional？）
2. 唯一键约束的处理？（重复时应该怎么返回？）
3. 并发场景下的数据一致性问题？

> 🎯 **面试关键**：能说出效率提升的具体数据（5-6 倍），说明你真的量化对比过，不是凭感觉说。

---

### Q5：你用 Cursor 搭建过 RAG 项目吗？具体怎么做的？

**面试官意图：** 考察 AI 应用开发和 AI 辅助工具结合使用的实际经验。

**完美解答：**

我用 Cursor 的 Composer 功能搭建了两个核心 RAG 项目：

**项目一：Ollama + LangChain RAG 极速搭建**

```
Prompt:
帮我生成一个 Python RAG 项目，使用 LangChain + Ollama + ChromaDB。

功能需求：
1. 支持 PDF、TXT 文档上传
2. 文档分块（500字符，50重叠）
3. 使用 Ollama embedding 模型做向量化
4. ChromaDB 存储和检索
5. 检索后拼接 Prompt 调用 Ollama 生成答案
6. 流式输出回答
7. Flask 提供 API 接口
```

**Cursor 一次生成的核心文件：**
```
rag_project/
├── app.py                    # Flask API 入口
├── document_loader.py        # PDF/TXT 文档解析
├── text_splitter.py          # 智能分块
├── vector_store.py           # ChromaDB 操作类
├── rag_pipeline.py           # 检索 + 生成核心链路
├── config.py                 # 配置文件
└── requirements.txt
```

**关键代码（Cursor 生成）：**
```python
# rag_pipeline.py
from langchain_community.embeddings import OllamaEmbeddings
from langchain_community.llms import Ollama
from langchain_community.vectorstores import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.chains import RetrievalQA

class RAGPipeline:
    def __init__(self, model_name="qwen2.5:7b"):
        self.embeddings = OllamaEmbeddings(model="bge-m3")
        self.llm = Ollama(model=model_name, temperature=0.1)
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=50
        )
    
    def ingest_document(self, file_path):
        # 加载和分块
        from document_loader import load_document
        docs = load_document(file_path)
        chunks = self.text_splitter.split_documents(docs)
        
        # 存入向量库
        vectorstore = Chroma.from_documents(
            documents=chunks,
            embedding=self.embeddings,
            persist_directory="./chroma_db"
        )
        return len(chunks)
    
    def query(self, question):
        vectorstore = Chroma(
            persist_directory="./chroma_db",
            embedding_function=self.embeddings
        )
        
        qa_chain = RetrievalQA.from_chain_type(
            llm=self.llm,
            retriever=vectorstore.as_retriever(search_kwargs={"k": 5}),
            return_source_documents=True
        )
        
        result = qa_chain({"query": question})
        return {
            "answer": result["result"],
            "sources": [doc.metadata["source"] for doc in result["source_documents"]]
        }
```

**项目二：SpringBoot + Milvus 企业级 RAG**

```java
// Cursor 生成的 Milvus RAG 服务
@Service
public class RagService {
    
    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;
    private final ChatService chatService;
    
    @Value("${rag.top-k:5}")
    private int topK;
    
    @Value("${rag.similarity-threshold:0.6}")
    private double similarityThreshold;
    
    /**
     * 检索 + 生成
     */
    public ChatResponse chat(String question, String userId) {
        // 1. 查询重写
        String rewrittenQuestion = queryRewrite(question);
        
        // 2. 向量检索
        List<Document> documents = milvusService.search(
            embeddingService.embed(rewrittenQuestion),
            topK,
            similarityThreshold,
            userId  // 多租户过滤
        );
        
        // 3. 重排序
        documents = reranker.rerank(rewrittenQuestion, documents);
        
        // 4. 构建上下文
        String context = buildContext(documents);
        
        // 5. 生成回答
        String answer = chatService.generate(rewrittenQuestion, context);
        
        // 6. 返回结果（含引用来源）
        return new ChatResponse(answer, buildCitations(documents));
    }
}
```

> 💡 **面试亮点**：展示 Python + Java 两种语言的 RAG 实现，说明你既有快速原型能力（Python），也有企业级开发能力（Java），这是很加分的组合。

---

### Q6：Cursor 生成的代码质量怎么样？你遇到过哪些坑？

**面试官意图：** 考察对 AI 生成代码的批判性评估能力和实际踩坑经验。

**完美解答：**

**总体评价：** Cursor 在代码生成上表现优秀，但有以下典型问题需要人工把关：

**坑 1：版本 API 过时**
```text
Cursor 可能生成使用了旧版 API 的代码，比如：
- 使用已弃用的 Feign(@FeignClient) 而非新版声明式 HTTP 调用
- 使用旧的 javax.validation 而非 jakarta.validation
- 使用已被替代的 Spring Security 配置方式

应对：在 Prompt 中明确声明版本号，如 "Spring Boot 3.2 + Jakarta Validation"
```

**坑 2：过度设计**
```java
// Cursor 有时会"想太多"，生成不必要的抽象层
// 对于简单的单表 CRUD，它可能生成：
// BaseService<T> + AbstractServiceImpl<T> + ProductService extends BaseService<Product>
// 对于简单场景，直接生成 ProductService + Impl 就够了，过度设计反而增加维护成本

// 我的原则：CRUD 用模板生成，复杂业务逻辑手工设计
```

**坑 3：业务逻辑幻觉**
```text
Cursor 可能"编造"业务规则。比如生成订单模块时：
- 自动生成一个"满减优惠"计算逻辑，但实际需求根本没有
- 自动添加"积分抵扣"功能，但这是不存在的需求

应对：Prompt 精准定义业务边界，AI 只做我明确要求的事，不要过度生成。
```

**坑 4：测试覆盖不全**
```java
// Cursor 生成的测试通常覆盖"快乐路径"，但边界测试不足
// 需要人工补充：
// 1. 极大数据量场景
// 2. 并发场景
// 3. 外部依赖全部异常的场景
// 4. 时间边界（跨天、跨月）
```

**我的质量控制流程：**
```
Cursor 生成代码 → 
1. 检查异常处理是否完整
2. 检查边界场景是否覆盖
3. 检查是否有不必要的"幻觉功能"
4. 运行所有测试
5. 静态代码分析（SonarQube）
6. 人工 Code Review（重点关注业务逻辑）
```

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：你怎么在团队中推广 Cursor 的使用？团队的接受度和协作模式怎么调整？

**面试官意图：** 考察技术领导力和团队协作视角。

**完美解答：**

推广 AI 辅助开发最大的难点不是技术，是**思维转变**。

**我的推广策略分四步：**

**第一步：树立标杆（2 周）**
- 自己先用 Cursor 完成一个完整的模块开发
- 记录效率对比数据（手写 vs AI 辅助的时间对比）
- 向团队展示具体效果，不是讲"AI 多厉害"，而是讲"同样功能快了 3 倍"

**第二步：设定规范（1 周）**
```yaml
# 团队 AI 辅助开发规范
# 1. 什么场景用 AI
#    - CRUD 代码生成：必须用
#    - 单元测试生成：推荐用
#    - 代码重构：建议用
#    - 复杂业务逻辑：AI 辅助，人工主导

# 2. 什么场景不用 AI
#    - 加密和安全相关代码
#    - 支付/资金相关逻辑
#    - 核心算法实现

# 3. AI 生成代码的要求
#    - 必须人工审查后才能提交
#    - 必须通过所有测试
#    - 必须标注"AI 辅助生成"以便追溯
```

**第三步：结对编程式推广（2 周）**
- 和每个团队成员结对 2-3 天
- 手把手演示：怎么写 Prompt、怎么审查 AI 代码、怎么快速修正
- 收集每个人的反馈，整理 FAQ

**第四步：建立最佳实践库**
```markdown
# 团队 Cursor 使用最佳实践

## 高效 Prompt 模板
- Java CRUD 生成模板
- 前端页面生成模板
- 测试用例生成模板

## 常见问题处理
- AI 生成代码不满足规范 → 在 Prompt 加明确的规范约束
- AI 理解不了复杂业务 → 先拆成小任务，逐个生成
```

> 🎯 **面试关键**：讲"推广 AI 工具"而不是"我自己用 AI"，说明你有团队视角和工程管理思维，这对高级开发/技术 Leader 岗位是很重要的加分项。

---

### Q8：Cursor 在 Docker 容器化部署方面能帮到什么？

**面试官意图：** 考察 DevOps 自动化和 Cursor 结合使用的工程化能力。

**完美解答：**

**用 Cursor 生成 Dockerfile（多阶段构建）：**

```dockerfile
# Prompt: "为 Spring Boot 项目生成多阶段构建 Dockerfile，优化镜像大小"

# 第一阶段：构建
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# 第二阶段：运行（镜像瘦身）
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# 优化效果：从 JDK 镜像 400MB → JRE 镜像 120MB
```

**用 Cursor 生成 Docker Compose：**

```yaml
# Prompt: "生成 SpringBoot + MySQL + Redis + Milvus 的 docker-compose 配置"

version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/db
      SPRING_DATA_REDIS_HOST: redis
      MILVUS_HOST: milvus
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      milvus:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: db
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s

  milvus:
    image: milvusdb/milvus:latest
    ports:
      - "19530:19530"
    volumes:
      - milvus_data:/var/lib/milvus
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9091/healthz"]
      interval: 30s

volumes:
  mysql_data:
  redis_data:
  milvus_data:
```

**这种配置的价值**：
1. 以前写 Docker 配置：查文档 → 试配置 → 等构建（1-2 天）
2. 现在用 Cursor：描述需求 → 生成配置 → 微调启动（1-2 小时）

> 💡 面试中展示 AI 生成的 Docker Compose 文件，说明你不仅会用 AI 写业务代码，连基础设施配置也靠 AI 提效。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：你在 Cursor 中遇到最难的 Debug 场景是什么？怎么和 AI 协作解决的？

**面试官意图：** 考察 Debug 能力和与 AI 协作解决问题的方法论。

**完美解答：**

**最难的 Debug 场景：Spring Boot 应用偶尔触发 BeanCurrentlyInCreationException**

**背景**：应用启动时，约 10% 的几率抛出 `BeanCurrentlyInCreationException`，但重启后就好了。没有固定的复现步骤。

**排查过程：**

**第 1 步：提供完整信息给 Cursor**
```
把以下信息发给 Cursor：
1. 完整的错误堆栈
2. 涉及的 Bean 配置（@Configuration 类）
3. 循环依赖的 Bean 关系图

Cursor 分析后指出：A Config 中使用 @DependsOn 同时 B Config 
中的 @PostConstruct 又调用了 A 中的 Bean，形成了隐式循环依赖。
```

**第 2 步：让 Cursor 提供修复方案**
```java
// Cursor 给出了三种解法：

// 方案 1：延迟注入（推荐）
@Component
public class AService {
    @Lazy  // 延迟加载，打破循环
    @Autowired
    private BService bService;
}

// 方案 2：构造器注入分离
@Configuration
public class AConfig {
    // 把 A 和 B 的初始化逻辑拆到不同阶段
}

// 方案 3：使用 ApplicationEvent 解耦
@Component
public class AInitializer implements ApplicationListener<ApplicationReadyEvent> {
    // 应用完全启动后再执行初始化
}
```

**第 3 步：验证修复**
```
我选择了方案 1 + 方案 3 组合：用 @Lazy 解决循环依赖，用 ApplicationReadyEvent 
异步执行初始化逻辑。部署后连续重启 20 次，不再出现该异常。
```

> 🎯 **经验总结**：给 AI 的 Debug 信息越完整，定位越精准。我的 Debug Prompt 模板是：**错误信息 + 关键代码 + 复现条件 + 已尝试的排查步骤**。

---

### Q10：用 Cursor 做前后端分离开发时，怎么保证前后端接口一致性？

**面试官意图：** 考察全栈开发经验和对接口规范的工程意识。

**完美解答：**

前后端分离最大的坑就是"后端改了接口，前端不知道"或"两端对接口的理解不一致"。

**我的方案：**

**1. 统一接口规范定义**
```yaml
# Cursor 先生成接口规范文档
# api-规范.md

## 统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1711012345678
}
```

## 分页返回格式
```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

## 错误码规范
- 200: 成功
- 400: 参数错误
- 401: 未登录
- 403: 无权限
- 500: 服务端错误
```

**2. 前后端代码一起生成**
```
Prompt:
"帮我生成一个商品管理模块，包含：
1. Java 后端：Controller + Service + Mapper + DTO + VO
2. Vue 前端：列表页 + 新增/编辑弹窗 + 删除确认
3. 后端返回格式遵循 Result<T> 统一封装
4. 前端使用 Element Plus 表格 + 表单组件
5. 前后端接口数据字段保持一致"
```

**3. 接口文档自动生成**
```
使用 Cursor 生成 Swagger/SpringDoc 注解覆盖所有接口
然后自动生成 OpenAPI 规范文档
前端通过 openapi-generator 自动生成 TypeScript 类型定义

这样后端改接口 → Swagger 文档自动更新 → 前端类型定义自动同步
再也不会出现"接口对不上"的问题
```

**4. 契约测试**
```java
// 后端提供 Contract Test，验证接口行为符合预期
@WebMvcTest(ProductController.class)
class ProductControllerContractTest {
    
    @Test
    void createProduct_ShouldReturn_201() throws Exception {
        // 这个测试就是前后端之间的"契约"
        // 前端依赖这个接口格式，后端不能随意更改
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"测试商品","price":99.99,"categoryId":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

---

### Q11：你的 Cursor .cursorrules 是怎么配置的？分享具体内容。

**面试官意图：** 考察 AI 辅助开发的规范化和个性化配置能力。

**完美解答：**

```markdown
# .cursorrules

## 项目背景
这是一个 Spring Boot 3.x + Vue 3 的全栈项目
Java 17 + MyBatis-Plus + MySQL 8.0

## 代码生成规范

### Java 后端规范
- 使用 @RequiredArgsConstructor 替代 @Autowired 字段注入
- Service 层必需接口 + Impl 分离结构
- Controller 统一使用 @RestController + @RequestMapping("/api/v1/xxx")
- 所有响应包装在 Result<T> 中
- 异常使用 GlobalExceptionHandler 统一处理
- 参数校验使用 Jakarta Validation 注解
- 所有 public 方法必须写 JavaDoc
- 禁止 System.out.println，统一使用 Lombok @Slf4j
- 包名使用小写：com.example.project.module

### 数据库规范
- 表名小写 + 下划线：sys_user, user_order
- 列名小写 + 下划线：created_at, updated_at
- 每表必含：id, created_at, updated_at
- 逻辑删除使用 is_deleted 字段

### 前端 Vue 规范
- 使用 Composition API + <script setup>
- 组件名 PascalCase：ProductTable.vue
- 状态管理使用 Pinia
- API 请求统一封装在 src/api/ 下
- 路由懒加载

### 测试规范
- Service 层使用 @ExtendWith(MockitoExtension.class)
- Controller 层使用 @WebMvcTest
- 每个 Service 方法至少一个"成功" + 一个"失败"测试

## 禁用项
- 不要生成 JSP / Thymeleaf（前后端分离）
- 不要使用 @Autowired 字段注入
- 不要使用 Date，使用 java.time.LocalDateTime
```

> 🎯 **面试价值**：分享具体的 `.cursorrules` 配置，说明你真正在工作中深入使用了 Cursor，并且有一套标准化的 AI 协作流程。这是"用过"和"精通"的区别。

---

### Q12：用 Cursor 生成前端代码时，怎么确保它和后端数据接口对齐？

**面试官意图：** 考察全栈开发中前后端联调的工程化能力。

**完美解答：**

前后端对齐是 AI 生成全栈代码时最大的挑战。我总结了一套"四步对齐法"：

**第一步：先定义接口契约，再生成代码**
```yaml
# 先让 Cursor 生成一个统一的接口定义文档
# api-contract.yaml
# 然后前后端代码都基于这个契约生成

openapi: 3.0.0
info:
  title: 用户管理 API
  version: 1.0.0

paths:
  /api/users/page:
    get:
      parameters:
        - name: page
          in: query
          schema: { type: integer, default: 1 }
        - name: size
          in: query
          schema: { type: integer, default: 10 }
      responses:
        '200':
          content:
            application/json:
              schema:
                type: object
                properties:
                  code: { type: integer, example: 200 }
                  data:
                    type: object
                    properties:
                      records: { type: array, items: { $ref: '#/components/schemas/UserVO' } }
                      total: { type: integer }
                      page: { type: integer }
                      size: { type: integer }
```

**第二步：后端先生成，前端再消费**
```
流程：
1. 用 Cursor 基于表结构生成 Java 后端（含 Swagger 注解）
2. 启动后端服务，获取 OpenAPI JSON（http://localhost:8080/v3/api-docs）
3. 用 openapi-generator 生成 TypeScript 类型定义
4. 前端代码中引用这些类型，天然与后端一致
```

```bash
# 自动生成 TypeScript 类型定义
npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts
```

```typescript
// 生成的类型定义（与后端 100% 对齐）
export interface UserVO {
  id: number;
  username: string;
  email: string | null;
  status: number;
  createdAt: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

// 前端代码中使用这些类型，天然类型安全
const response = await api.get<PageResult<UserVO>>('/api/users/page', { params })
```

**第三步：Cursor Composer 同时生成两端**
```
给 Cursor 的 Prompt：
"生成商品管理模块：
1. Java 后端接口（已有 OpenAPI 规范参考）
2. Vue 前端页面（使用上面 Step 2 生成的 TypeScript 类型）
3. 后端返回 Result<PageResult<ProductVO>> 格式
4. 前端 API 调用使用 axios + 响应拦截器"

Cursor 同时在 Composer 中生成后端 Controller 和前端 API 调用代码，
保证两端的字段名、类型、结构完全一致。
```

**第四步：契约测试验证**
```java
// 后端的 MockMvc 测试 — 持有契约，前端可以依赖
@WebMvcTest(UserController.class)
class UserControllerContractTest {
    
    @Test
    void pageQuery_ShouldReturnExpectedFormat() throws Exception {
        mockMvc.perform(get("/api/users/page")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            // 契约：code 必须为 200
            .andExpect(jsonPath("$.code").value(200))
            // 契约：data.records 必须是数组
            .andExpect(jsonPath("$.data.records").isArray())
            // 契约：必须包含 total 字段
            .andExpect(jsonPath("$.data.total").isNumber());
    }
}
```

> 🎯 **核心思想**：AI 生成代码很高效，但一致性需要靠工程流程保证——"先接口约定，再两端生成，最后契约测试兜底"。这个流程让我用 Cursor 生成全栈代码时，从未出现接口字段对不上的问题。

---

## 💎 面试加分金句

1. "我把 AI 定位为'超级实习生'——它产出的初稿质量很高，但终审和决策必须我来做。这种角色定位让我能同时保证效率和代码质量。"
2. "我用 Cursor 生成代码的平均时间只占项目总时间 30%，剩下的 70% 在审查、测试和架构设计上——AI 提效的是编码速度，不是决策质量。"
3. "配置 .cursorrules 是我每个新项目的第一件事，相当于给 AI 设定了'团队规范'，生成出来的代码风格天然就和大家一致。"
4. "AI 辅助开发最被低估的价值不是写代码，而是读代码——用 @Codebase 问问题的方式理解老项目，比读文档快 10 倍。"
5. "真正的全栈能力不是一个人写所有代码，而是能用 AI 打通前后端边界，确保接口规范、数据结构、业务流程在两端一致。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| 怎么保证 AI 代码的安全性？ | 人工审查 + 安全扫描工具（SonarQube）+ 限制 AI 不接触敏感代码 |
| AI 代码的 Bug 率比人工高吗？ | 生成阶段 Bug 率约 10-15%（主要是逻辑和边界问题），审查后降至 1-2% |
| Cursor 和 GitHub Copilot 选哪个？ | 看场景：单文件补全 Copilot 够用；多文件架构改动 Cursor 更强 |
| 你每天花多少时间审查 AI 代码？ | 生成 30 分钟，审查 20 分钟，比例约 6:4，审查时间必不可少 |
| 新项目用 Cursor 从 0 搭建要多久？ | 全栈项目 skeleton 生成约 2-3 小时，以前手写需要 2-3 天 |

## 🔗 关联知识点

- [ClaudeCode必做项目清单-面试问答](./ClaudeCode必做项目清单-面试问答.md) — Cursor 与 Claude Code 的对比和协同使用
- [三大核心项目-面试问答](./三大核心项目-面试问答.md) — Cursor 辅助开发秒杀/RAG/Agent 项目
- [前端必做实战项目-面试问答](./前端必做实战项目-面试问答.md) — Cursor 生成前端代码的最佳实践
- Docker/K8s — Cursor 辅助生成容器化部署配置
