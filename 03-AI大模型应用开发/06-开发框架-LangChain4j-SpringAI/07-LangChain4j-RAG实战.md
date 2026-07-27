# 07 - LangChain4j RAG 实战

> 🎯 LangChain4j 的 RAG = DocumentLoader → Splitter → EmbeddingStore → Retriever → ChatModel。丰富的数据源支持和灵活的检索配置

## 1. RAG 完整 Pipeline

```java
// ① 文档加载
Document pdfDoc = FileSystemDocumentLoader.loadDocument(
    Path.of("handbook.pdf"), new PdfDocumentParser()
);

// ② 切片
DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
List<TextSegment> segments = splitter.split(pdfDoc);

// ③ Embedding + 入库
EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("bge-m3").build();

EmbeddingStore<TextSegment> store = ChromaEmbeddingStore.builder()
    .baseUrl("http://localhost:8000")
    .collectionName("docs").build();

// 批量入库
List<Embedding> embeddings = embeddingModel.embedAll(
    segments.stream().map(TextSegment::text).toList()
).content();
store.addAll(embeddings, segments);
```

## 2. 检索与生成

```java
// 检索器
EmbeddingStoreRetriever retriever = EmbeddingStoreRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5)
    .minScore(0.7)
    .build();

// RAG 对话
String question = "公司年假怎么算？";
List<TextSegment> relevant = retriever.findRelevant(question);

// 拼接上下文 + 生成
String context = relevant.stream()
    .map(TextSegment::text)
    .collect(Collectors.joining("\n\n"));

String answer = model.generate("""
    基于以下资料回答问题。如果资料中没有，说"未找到相关信息"。
    
    资料：%s
    
    问题：%s
    """.formatted(context, question));
```

## 3. AiServices + RAG 声明式

```java
interface RagAssistant {
    @SystemMessage("""
        你是企业知识库助手。
        回答格式：[答案]
        来源：[引用文档名称]
        """)
    String answer(String question);
}

// ContentRetriever → 注入 MemoryId → AiServices 自动检索
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(store)
    .embeddingModel(embeddingModel)
    .maxResults(5).build();

RagAssistant assistant = AiServices.builder(RagAssistant.class)
    .chatLanguageModel(model)
    .contentRetriever(retriever)  // ← 注入检索器
    .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
    .build();

String answer = assistant.answer("请假要提前几天申请？");
```

## 4. 高级检索：查询改写 + 重排序

```java
// 查询改写
String rewrittenQuery = model.generate("""
    将用户口语化问题改写为搜索关键词：
    用户问题：%s
    关键词：
    """.formatted(userQuery));

// 混合检索
// Dense 检索（向量）
List<TextSegment> denseResults = retriever.findRelevant(rewrittenQuery);
// BM25 检索（关键词）
List<TextSegment> sparseResults = bm25Retriever.findRelevant(rewrittenQuery);
// RRF 融合
List<TextSegment> merged = rrfFusion(denseResults, sparseResults);
```

## 5. 多数据源联合检索

```java
// 多个检索器 → 联合搜索
ContentRetriever wikiRetriever = ... // 维基百科
ContentRetriever docRetriever = ...  // 内部文档
ContentRetriever codeRetriever = ... // 代码库

Assistant agent = AiServices.builder(Assistant.class)
    .contentRetrievers(wikiRetriever, docRetriever, codeRetriever)
    .build();
```
