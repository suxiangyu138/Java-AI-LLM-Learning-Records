# 11 - Java AI 框架面试题与选型

> 🎯 Spring AI 和 LangChain4j 都是面试热点 — AiServices 原理、@Tool 执行流程、RAG 链路、流式实现

## 高频面试题

**Q1: Spring AI 怎么屏蔽多供应商差异？**
```text
统一的 ChatModel 接口 → 各供应商实现（OpenAiChatModel/OllamaChatModel）
+ 自动配置（spring.ai.xxx.api-key）→ 改配置即可切换
```

**Q2: LangChain4j AiServices 的实现原理？**
```text
JDK 动态代理：定义 Interface → AiServices.create() 生成代理对象
→ 方法调用被拦截 → 转为 LLM 请求（自动注入 @SystemMessage/@Tool）
→ 返回结果映射为方法返回值类型
```

**Q3: @Tool 的执行流程？**
```text
① Spring AI 扫描 @Tool 注解方法 → 生成 JSON Schema
② 传给 LLM（作为 tools 参数）
③ LLM 决定调用 → 返回 function_call(name, args)
④ Spring AI 反射执行方法 → 结果返回 LLM → 生成最终回复
```

**Q4: RAG 在 Spring AI 中的实现？**
```text
DocumentReader(加载) → TextSplitter(切片) → EmbeddingClient(向量化)
→ VectorStore(入库) → QuestionAnswerAdvisor(检索+注入) → ChatClient(生成)
```

**Q5: Spring AI 怎么实现流式输出？**
```text
ChatClient.prompt().stream().content() → 返回 Flux<String>
→ 基于 Project Reactor → SSE 推送 → 前端逐 token 展示
```

**Q6: Spring AI vs LangChain4j 怎么选？**
```text
Spring Boot 项目 → Spring AI（原生、自动配置）
复杂 Agent → LangChain4j（AiServices + Chain 更灵活）
非 Spring → LangChain4j（无框架依赖）
```

## 完整选型矩阵

| 需求 | Spring AI | LangChain4j | 其他 |
|------|:---:|:---:|------|
| Spring Boot 集成 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | — |
| 简单 Chat | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | — |
| RAG | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | — |
| Function Calling | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | — |
| 多轮对话记忆 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | — |
| Agent 编排 | ⭐⭐⭐ | ⭐⭐⭐⭐ | — |
| 流式输出 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | — |
| 文档处理 | ⭐⭐⭐ | ⭐⭐⭐⭐ | LlamaIndex ⭐⭐⭐⭐⭐ |
| 多模态 | ⭐⭐⭐⭐ | ⭐⭐ | — |
| 生产监控 | ⭐⭐⭐⭐ | ⭐⭐ | — |
| 社区活跃 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | — |
| 学习成本 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | — |

## 架构评审清单

```text
Java AI 项目上线前检查：

  □ 模型供应商有容灾（至少 2 个 API 或 1 API+1 自建）
  □ 对话历史有持久化（Redis/DB，应用重启不丢失）
  □ API Key 不硬编码（K8s Secret/环境变量/Vault）
  □ 流式输出超时有处理（30s 超时 + 重连）
  □ 敏感信息过滤已开启
  □ RAG 向量库和 Embedding 模型一致
  □ @Tool 方法有权限控制（读/写/删分级）
  □ 成本监控已配置（Token 日消耗 + 告警）
```
