# 10 - MyBatis 与 AI 场景结合

> 🎯 AI 开发中 MyBatis 有了新角色——LLM 动态生成 SQL、数据向量化存储、RAG 数据管理。本章覆盖三个 AI+MyBatis 的实用场景

---

## 目录

1. [LLM 动态生成 MyBatis SQL](#1-llm-动态生成-mybatis-sql)
2. [向量化数据存储](#2-向量化数据存储)
3. [RAG 数据源管理](#3-rag-数据源管理)

---

## 1. LLM 动态生成 MyBatis SQL

```java
// AI 自然语言 → 动态 SQL 查询
@Service
public class AISearchService {
    private final UserMapper userMapper;

    public List<User> aiSearch(String naturalLanguageQuery) {
        // 1. LLM 将自然语言 → SQL 条件
        String aiPrompt = """
            将以下自然语言查询转为 JSON 查询条件：
            查询：%s
            可用字段：name(String), age(Integer), status(String), createTime(Date)
            返回格式：{"conditions":[{"field":"...","op":"eq|like|gt|between","value":"..."}]}
            """.formatted(naturalLanguageQuery);

        String aiResponse = llm.chat(aiPrompt);
        List<Condition> conditions = parseAIResponse(aiResponse);

        // 2. 构造 MyBatis 动态查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        for (Condition c : conditions) {
            switch (c.op()) {
                case "eq" -> wrapper.eq(getField(c.field()), c.value());
                case "like" -> wrapper.like(getField(c.field()), c.value());
                case "gt" -> wrapper.gt(getField(c.field()), c.value());
            }
        }
        return userMapper.selectList(wrapper);
    }
}
```

## 2. 向量化数据存储

```java
// 场景：从数据库加载文本 → Embedding → 存入向量数据库
@Service
public class EmbeddingPipeline {
    private final UserMapper userMapper;

    public void embedAllUsers() {
        // 1. 流式读取（大数据量）
        userMapper.selectList(null).stream()
            .map(User::getProfile)              // 提取需要向量化的文本
            .map(embeddingService::embed)        // 调用 Embedding API
            .forEach(vectorDB::insert);          // 存入向量数据库
    }
}

// MyBatis Mapper 支持流式读取
@Select("SELECT * FROM documents")
@Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 100)
@ResultType(Document.class)
Cursor<Document> streamAll();  // 游标读取，内存友好
```

## 3. RAG 数据源管理

```java
// RAG 的离线数据管理全部走 MyBatis
@Mapper
public interface RAGDataSourceMapper {
    @Insert("INSERT INTO knowledge_base (title, content, embedding_status) " +
            "VALUES (#{title}, #{content}, 'PENDING')")
    void insertDocument(Document doc);

    @Select("SELECT * FROM knowledge_base WHERE embedding_status = 'PENDING' LIMIT 100")
    List<Document> findPendingEmbeddings();

    @Update("UPDATE knowledge_base SET embedding_status = 'DONE' WHERE id = #{id}")
    void markEmbedded(Long id);

    @Select("SELECT * FROM knowledge_base WHERE MATCH(title, content) AGAINST(#{keyword})")
    List<Document> fullTextSearch(String keyword);
}
```

## 核心要点回顾

- AI 场景 MyBatis 三用法：LLM→SQL / 流式读取→Embedding / RAG 数据管理
- `Cursor<T>` 流式读取大数据集（避免 OOM）
- MyBatis 的可预测 SQL 更适合 LLM 生成（对比 JPA 的不可预测 SQL）
- 向量化场景 MyBatis 负责数据读取和状态管理，向量化本身由 Embedding API 完成

## 参考资料

1. MyBatis Cursor 文档
2. LangChain4j SQL 集成
