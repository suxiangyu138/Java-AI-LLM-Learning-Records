# 03 - Spring AI RAG 实战

> 🎯 Spring AI 的 RAG 原生集成 — DocumentReader → Splitter → VectorStore → Retriever → ChatClient，一条链走完

## 1. RAG 完整链路

```java
@Configuration
public class RAGConfig {
    
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        // Chroma 向量库
        return new ChromaVectorStore(embeddingClient);
        // 或 Milvus / Pinecone / PGVector
    }
    
    @Bean
    public ChatClient ragChatClient(ChatModel chatModel, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
            .build();
    }
}

// 使用 → 自动检索 + 生成
@RestController
public class RAGController {
    @Autowired
    private ChatClient ragChatClient;
    
    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return ragChatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

## 2. 文档入库 Pipeline

```java
@Component
public class DocumentIndexer {
    
    @Autowired
    private VectorStore vectorStore;
    
    // ① 读取文档
    public void indexDirectory(String path) {
        // PDF 读取器
        var pdfReader = new PagePdfDocumentReader(path);
        // Markdown 读取器
        var mdReader = new MarkdownDocumentReader(path);
        // 或通配符批量
        var reader = new PagePdfDocumentReader(
            new FileSystemResource(path),
            new FilePatternDocumentReader("classpath:/docs/**/*.md")
        );
        
        // ② 切片
        var splitter = new TokenTextSplitter(500, 400, 50, 10, true);
        List<Document> documents = splitter.apply(reader.get());
        
        // ③ 入库
        vectorStore.add(documents);
    }
}
```

## 3. 检索策略

```java
// 基础检索
List<Document> docs = vectorStore.similaritySearch(
    SearchRequest.query("查询内容").withTopK(5)
);

// 阈值过滤
SearchRequest request = SearchRequest.query(query)
    .withTopK(10)
    .withSimilarityThreshold(0.7);   // 只返回相似度>0.7的

// 元数据过滤
SearchRequest request = SearchRequest.query(query)
    .withFilterExpression("source == 'handbook.pdf' AND page > 10");
```

## 4. Advisor 增强检索

```java
ChatClient client = ChatClient.builder(model)
    .defaultAdvisors(
        // QuestionAnswerAdvisor = RAG 核心
        new QuestionAnswerAdvisor(vectorStore,
            SearchRequest.query(query).withTopK(5)
        ),
        // 对话记忆
        new ChatMemoryAdvisor(),
        // Prompt 增强（强制引用来源）
        new PromptAdvisor("""
            基于参考资料回答。如果资料中没有，说'未找到相关信息'。
            引用格式：[来源：{source}]
            """)
    )
    .build();
```

## 5. 完整 RAG 示例

```java
@Service
public class EnterpriseRAGService {
    
    @Autowired private ChatClient ragClient;
    @Autowired private VectorStore vectorStore;
    
    public record RAGResponse(String answer, List<String> sources) {}
    
    public RAGResponse ask(String question) {
        // 检索
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(5)
        );
        
        // 生成
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
        
        String answer = ragClient.prompt()
            .user("参考资料：\n{context}\n\n问题：{question}")
            .call()
            .content();
        
        List<String> sources = docs.stream()
            .map(d -> d.getMetadata().get("source").toString())
            .toList();
        
        return new RAGResponse(answer, sources);
    }
}
```
