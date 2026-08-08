# Java 集成：LangChain4j 与 Spring AI

> Java 后端的 Agent 数据库交互：三条成熟路线（@AiService 声明式 / 双 Agent 工具链 / SqlDatabaseContentRetriever）+ JSQLParser 安全校验 + 企业架构。本仓库 Java 生态核心篇。

## 1. Java 侧的三种模式总览

| 模式 | 做法 | 适合 | 安全级别 |
|---|---|---|---|
| A. @AiService 声明式 | 接口注解直接生成 SQL | 快速交付、单表域 | 中（提示词约束） |
| B. 双 Agent 工具链 | Supervisor + SqlAgent 多工具 | 生产级、复杂 schema | 高（工具隔离+校验） |
| C. SqlDatabaseContentRetriever | RAG 组件生成并执行 SQL | 检索增强问答 | 警告：@Experimental |

> 🎯 核心要点：**模式 A 快但糙（靠提示词），模式 B 稳但重（靠工具链）**——生产系统选 B，Demo 与内部工具选 A。

## 2. 模式 A：@AiService 声明式 Text-to-SQL

```java
@AiService
public interface TextToSQLService {
    @SystemMessage("""
        你是资深数据工程师。根据用户自然语言与提供的数据库 Schema 生成标准 SQL。
        【严格规则】
        1. 仅使用提供的表/字段，禁止编造
        2. 仅允许 SELECT，严禁 DML/DDL
        3. 适配 MySQL 语法
        """)
    SQLResult generateSQL(@V("schema") String schema,
                          @V("fewShotExamples") String examples,
                          @V("question") String question);
}

public record SQLResult(String sql, String explanation,
                        boolean needsConfirmation) {}
```

| 要点 | 说明 |
|---|---|
| 结构化输出 | record 强制 JSON 化（sql/explanation/needsConfirmation） |
| 依赖版本 | langchain4j-spring-boot-starter（v0.36+） |
| 安全 | 输出 SQL 仍需 JSQLParser 校验后再执行（见第 4 节） |

## 3. 模式 B：双 Agent + 工具链（生产推荐）

```text
SupervisorAgent（决策层）
   ├── 路由：查数 → SqlAgent；闲聊 → 直接答
   └── 汇总：把 SqlAgent 结果组织成回答
SqlAgent（数据库专家）
   ├── QueryAllTablesTool    全部表名
   ├── QueryTableSchemaTool  表结构（含样例行）
   └── ExecuteSqlQueryTool   执行查询（只读校验）
```

```java
public interface AgentTool {
    String name();
    String description();
    Object invoke(String argsJson);
}

@Component
public class ExecuteSqlQueryTool implements AgentTool {
    private final JdbcTemplate jdbc;                 // 只读数据源

    @Override
    public Object invoke(String argsJson) {
        String sql = JsonPath.read(argsJson, "$.sql");
        sql = SqlValidator.validateReadOnly(sql, allowedTables);  // 见第 4 节
        sql = SqlValidator.clampLimit(sql, 100);
        return jdbc.queryForList(sql).stream().limit(100).toList();
    }
}
```

```java
// 注册：Spring 自动装配所有 AgentTool
@Component
public class SqlAgent {
    public SqlAgent(@Autowired List<AgentTool> tools,
                    @Value("${agent.allowed-tables}") Set<String> allowedTables) { ... }
}
```

| 工程点 | 做法 |
|---|---|
| 数据源隔离 | 查询专用 DataSource（只读角色），与业务写库物理分离 |
| 表白名单 | `agent.allowed-tables` 配置化，未授权表拒绝 |
| 工具发现 | `@Autowired List<AgentTool>` 自动收集，扩展只加类 |
| 自纠错 | 执行异常回喂模型重写（≤3 次） |

## 4. JSQLParser 安全校验（Java 侧标配）

```java
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;

public class SqlValidator {
    private static final Set<String> FORBIDDEN = Set.of(
        "insert", "update", "delete", "drop", "alter", "truncate", "create");

    public static String validateReadOnly(String sql, Set<String> allowedTables) {
        // 1. AST 解析：非 SELECT 直接拒绝
        var statement = CCJSqlParserUtil.parse(sql);
        if (!(statement instanceof Select select)) {
            throw new SqlGuardException("仅支持 SELECT 查询");
        }
        // 2. 表名白名单（AST 遍历可穿透 join/子查询/CTE/别名）
        for (String table : extractTables(select)) {
            if (!allowedTables.contains(table.toLowerCase())) {
                throw new SqlGuardException("表 " + table + " 未授权");
            }
        }
        return sql;
    }
}
```

| 校验项 | 说明 |
|---|---|
| 语句类型 | 仅 SELECT 通过（AST 级，非字符串过滤） |
| 表白名单 | AST 提取表名（含 join/CTE/子查询），过白名单 |
| 敏感列 | 提取 SelectItem 检测 password/ssn 等列名拒绝 |
| 强制 LIMIT | 无 LIMIT 时 AST 注入 `LIMIT 100` |
| 错误脱敏 | 异常信息清洗后再回喂模型 |

## 5. 模式 C：SqlDatabaseContentRetriever（RAG 式）

```java
SqlDatabaseContentRetriever retriever = SqlDatabaseContentRetriever.builder()
    .dataSource(readOnlyDataSource)
    .sqlDialect("PostgreSQL")
    .chatModel(chatModel)
    .maxRetries(3)
    .build();

List<Content> results = retriever.retrieve(Query.from("显示最近一个月销量最高的产品"));
```

| 特性 | 说明 |
|---|---|
| 动态元数据 | JDBC DatabaseMetaData 实时建 schema 上下文（可缓存 ~5 分钟） |
| 自纠错 | 执行失败回喂 LLM 重试（maxRetries） |
| 多阶段优化 | SqlOptimizer 12 类规则（索引建议/join 顺序/子查询） |
| 过滤 | 表/列过滤（排除 password 列） |

> ⚠️ **官方警告**：此类标 @Experimental 且"有危险"——数据库用户必须极受限只读；JSQLParser 只验证了 SELECT 语句，**不能保证查询无害**（如 SELECT 调用副作用函数）。生产用请搭配第 4 节完整校验。

## 6. 企业架构参考（腾讯云 2026 案例）

| 层 | 选型 |
|---|---|
| 后端 | Java 17 + Spring Boot 3.2+ |
| AI 框架 | LangChain4j（20+ 供应商 / 15+ SQL 方言） |
| LLM | Qwen-max / GLM-4 / GPT-4o 等强推理模型 |
| SQL 校验 | JSQLParser（AST 只读 + 表白名单） |
| Schema 管理 | Redis 缓存 + Milvus/PGVector 向量存 few-shot |
| 执行 | JdbcTemplate + 行级权限过滤 + HikariCP |
| 可观测 | OpenTelemetry + SQL 审计看板 |

**实测效果**（该案例报告）：平均查询准确率 89%（裸 LLM API 72%、专用 SQL 工具 81%）；动态元数据策略让 schema 变更后准确率从 58% 恢复到 97%；schema 缓存把元数据处理从 ~120ms 降到 <15ms。

## 6.5 依赖与版本速查

```xml
<!-- pom.xml 核心依赖（2026-08 基准） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.36.0</version>          <!-- 持续迭代，锁版发布 -->
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-experimental-sql</artifactId>  <!-- 模式 C 专用，@Experimental -->
</dependency>
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.9</version>             <!-- AST 校验 -->
</dependency>
<!-- JDBC 驱动按库选：mysql-connector-j / postgresql -->
```

| 提示 | 说明 |
|---|---|
| 版本锁定 | LangChain4j 迭代快，锁定版本 + 发布前查 release notes |
| 方言覆盖 | langchain4j 支持 15+ SQL 方言，配置 `sqlDialect` 与库一致 |
| 只读数据源 | 模式 B/C 必须用独立只读 DataSource（与业务写库物理分离，配 HikariCP 4-8 连接） |

## 7. 与 MCP 的结合（Java 侧 2026 方向）

```text
方案一：Java Agent 直接调 MCP 数据库服务器
   LangChain4j 1.0+ 原生支持 MCP 客户端（stdio/streamable http）
   好处：复用 DBHub 等成熟服务器，连接逻辑零开发

方案二：把 Java 工具链暴露为 MCP Server
   Spring AI / LangChain4j 将自研工具注册为 MCP 端点
   好处：同一套工具服务所有客户端
```

> 💡 Google 2026 官方 Codelab（Cymbal Transit）即"LangChain4J + MCP Toolbox Java SDK 多 Agent"模式——Java 侧 MCP 已是一等公民。

## 8. 面试速记

| 问题 | 要点 |
|---|---|
| Java 侧 Text-to-SQL 怎么落地？ | 三模式：@AiService 声明式 / 双 Agent 工具链 / SqlDatabaseContentRetriever；生产选工具链 |
| 生成的 SQL 怎么保证安全？ | JSQLParser AST 校验：仅 SELECT + 表白名单 + 敏感列 + 强制 LIMIT；DB 层只读角色兜底 |
| Schema 上下文怎么给？ | 按域拆分 ≤20 表 + 样例行 + 口径注入；Redis 缓存元数据（120ms→15ms） |
| Java 侧 MCP 怎么用？ | LangChain4j 原生 MCP 客户端接 DBHub；或把自研工具注册为 MCP Server 供所有客户端复用 |

> 🎯 核心要点：Java 侧的护城河在**工程规范**——数据源隔离（只读）、AST 校验（JSQLParser）、配置化白名单、OTel 审计——语言本身不产生智能，产生的是可控性。

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-数据库交互总览](00-数据库交互总览.md)

## 参考来源

- [SqlDatabaseContentRetriever（LangChain4j Docs）](https://docs.langchain4j.dev/apidocs/dev/langchain4j/experimental/rag/content/retriever/sql/SqlDatabaseContentRetriever.html)
- [LangChain4j 企业级 NL2SQL 三大策略（GitCode）](https://blog.gitcode.com/76d074a2eb4acc9e90fa92c597593fd1.html)
- [数据库操作智能体实现（腾讯云）](https://cloud.tencent.cn/developer/article/2627438)
- [解决方案：自然语言转 SQL 智能数据 AI 查询助手（腾讯云）](https://cloud.tencent.cn/developer/article/2701905)
- [Cymbal Transit：LangChain4J + MCP Toolbox Java SDK 多 Agent（Google Codelabs）](https://codelabs.developers.google.com/cymbal-bus-agent-mcp-toolbox-java)
