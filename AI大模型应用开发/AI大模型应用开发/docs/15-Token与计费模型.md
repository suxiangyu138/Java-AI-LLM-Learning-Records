# 15 — Token 与计费模型

> **目标**：深入理解 Token 计算、各厂商计费模式，掌握成本优化策略和企业级成本管控。

---

## 1. Token 深度解析

### 1.1 Token 是什么（再深入）

```
Token ≠ 字符，≠ 单词，而是"子词"（Subword）。

BPE 分词算法示例（GPT 系列）：
  "unbelievable" → ["un", "belie", "vable"]
  "tokenization" → ["token", "ization"]
  "ChatGPT"       → ["Chat", "G", "PT"]

中文分词示例：
  "人工智能大模型" → ["人工", "智能", "大", "模型"]  (4 tokens)
  "你好世界"       → ["你好", "世界"]                (2 tokens)

Token 消耗经验法则：
  中文：1 字 ≈ 0.5-0.7 token → 1000 字中文 ≈ 600 tokens
  英文：1 词 ≈ 1.3 tokens    → 1000 词英文 ≈ 1300 tokens
  代码：1 行代码 ≈ 5-15 tokens（因语言而异）
```

### 1.2 为什么不直接用字符

```
Token 化的优势：
  ✅ 减少词表大小（不可能是无限字符组合）
  ✅ 处理未见过的词（OOV 问题）：子词可以组合
  ✅ 平衡效率和语义：比字符大，比词小
  ✅ 多语言通用
```

### 1.3 Token 计算工具

```java
@Service
public class TokenCounter {

    // 方案 1：调用 API 获取实际 Token 数
    public int countViaApi(String text, ChatModel chatModel) {
        // 某些 API 返回 usage 信息
        var prompt = new Prompt(text);
        var response = chatModel.call(prompt);
        // response.getMetadata().getUsage().getTotalTokens()
        return 0;
    }

    // 方案 2：本地估算
    public int estimateTokens(String text) {
        int chineseChars = 0;
        int englishWords = 0;
        int others = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else if (Character.isLetter(c)) {
                englishWords++;
            } else {
                others++;
            }
        }

        // 中文约 1.5 字符/token，英文约 0.75 词/token
        return (int) (chineseChars / 1.5 + englishWords * 1.3 + others * 0.5);
    }

    // 方案 3：调用专门的 Tokenizer API
    // OpenAI: https://api.openai.com/v1/tokenizers (内部接口)
}
```

---

## 2. 各厂商计费对比

### 2.1 国际厂商（截至 2025 中）

| 模型 | 输入价格 (每 1M tokens) | 输出价格 (每 1M tokens) |
|------|------------------------|------------------------|
| **GPT-4o** | $2.50 | $10.00 |
| **GPT-4o-mini** | $0.15 | $0.60 |
| **Claude Opus 4** | $15.00 | $75.00 |
| **Claude Sonnet 4** | $3.00 | $15.00 |
| **Claude Haiku 4.5** | $0.80 | $4.00 |
| **Gemini 2.5 Pro** | $1.25 | $10.00 |
| **Gemini 2.5 Flash** | $0.15 | $0.60 |
| **DeepSeek-V3** | $0.27 | $1.10 |

### 2.2 国内厂商（参考）

| 模型 | 输入价格 (¥/1M tokens) | 输出价格 (¥/1M tokens) |
|------|------------------------|------------------------|
| **通义千问 Qwen-Max** | ¥4.00 | ¥12.00 |
| **通义千问 Qwen-Plus** | ¥0.80 | ¥2.00 |
| **通义千问 Qwen-Turbo** | ¥0.30 | ¥0.60 |
| **文心一言 ERNIE 4.0** | ¥12.00 | ¥12.00 |
| **豆包 Doubao-Pro** | ¥0.80 | ¥2.00 |
| **豆包 Doubao-Lite** | ¥0.30 | ¥0.60 |
| **GLM-4** | ¥5.00 | ¥5.00 |
| **DeepSeek-V2** | ¥1.00 | ¥2.00 |

---

## 3. 成本优化策略

### 3.1 策略全景

```
成本优化金字塔：

        ┌─────────────┐
        │ 模型路由      │ ← 省 50-80%（简单任务用便宜模型）
        ├─────────────┤
        │ 语义缓存      │ ← 省 30-60%（重复问题缓存答案）
        ├─────────────┤
        │ Prompt 压缩   │ ← 省 10-30%（减少 Input Token）
        ├─────────────┤
        │ 输出限制       │ ← 省 20-40%（限制 max_tokens）
        ├─────────────┤
        │ 量化 + 本地   │ ← 省 50-90%（高频调用自部署 SLM）
        └─────────────┘
```

### 3.2 模型路由实现

```java
@Service
public class CostOptimizedRouter {

    // 按能力分级路由
    public ChatModel route(String userMessage) {
        int complexity = estimateComplexity(userMessage);

        if (complexity <= 3) {
            return cheapModel;      // gpt-4o-mini / qwen-turbo
        } else if (complexity <= 7) {
            return midModel;        // gpt-4o / qwen-plus
        } else {
            return powerfulModel;   // claude-opus / qwen-max
        }
    }

    private int estimateComplexity(String message) {
        int score = 0;
        if (message.length() > 500) score += 2;
        if (message.contains("代码") || message.contains("code")) score += 2;
        if (message.contains("推理") || message.contains("分析")) score += 2;
        if (message.contains("数学") || message.contains("计算")) score += 3;
        return score;
    }
}

/**
 * 阶梯路由：
 * 先尝试便宜的，不满意再升级
 */
@Service
public class TieredRouter {

    public String smartRoute(String message) {
        // Tier 1: 本地 SLM（最便宜）
        try {
            String result = localModel.call(message);
            if (qualityCheck(result)) return result;
        } catch (Exception ignored) {}

        // Tier 2: 云小模型
        try {
            String result = cheapModel.call(message);
            if (qualityCheck(result)) return result;
        } catch (Exception ignored) {}

        // Tier 3: 云大模型（兜底）
        return powerfulModel.call(message);
    }
}
```

### 3.3 语义缓存

```java
@Service
public class SemanticCacheService {

    private final VectorStore cacheStore;
    private final EmbeddingModel embeddingModel;
    private final MeterRegistry meterRegistry;

    private final Counter cacheHits = Counter.builder("ai.cache.hits").register(meterRegistry);
    private final Counter cacheMisses = Counter.builder("ai.cache.misses").register(meterRegistry);
    private final Counter tokensSaved = Counter.builder("ai.cache.tokens_saved").register(meterRegistry);

    /**
     * 语义缓存 + 成本追踪
     */
    public Optional<CachedAnswer> get(String question) {
        List<Document> results = cacheStore.similaritySearch(
            SearchRequest.query(question)
                .withTopK(1)
                .withSimilarityThreshold(0.92)
        );

        if (!results.isEmpty()) {
            cacheHits.increment();
            int tokensSaved = estimateTokens(results.get(0).getMetadata().get("answer").toString());
            this.tokensSaved.increment(tokensSaved);

            return Optional.of(new CachedAnswer(
                results.get(0).getMetadata().get("answer").toString(),
                results.get(0).getScore()
            ));
        }

        cacheMisses.increment();
        return Optional.empty();
    }

    record CachedAnswer(String answer, double similarity) {}
}
```

### 3.4 Prompt 压缩

```java
@Service
public class PromptCompressor {

    /**
     * LLMLingua 风格压缩：减少冗余 Prompt
     */
    public String compressPrompt(String prompt) {
        // 压缩策略：
        // 1. 移除多余空白
        // 2. 精简描述（保持语义）
        // 3. 移除不必要的礼貌用语

        return prompt
            .replaceAll("\\s+", " ")                           // 合并空白
            .replace("请帮我", "")                               // 去掉礼貌用语
            .replace("谢谢", "")
            .trim();
    }

    /**
     * 长上下文压缩：摘要历史消息
     */
    public String summarizeHistory(List<Message> history) {
        if (history.size() <= 10) {
            return history.stream()
                .map(Message::getContent)
                .collect(Collectors.joining("\n"));
        }

        // 前 10 轮做摘要
        String summary = chatClient.prompt()
            .system("将以下对话历史进行简洁摘要，保留关键信息")
            .user(history.subList(0, 10).stream()
                .map(Message::getContent)
                .collect(Collectors.joining("\n")))
            .call()
            .content();

        // 摘要 + 最近几轮
        return "[历史摘要] " + summary + "\n" +
            "[最近对话]\n" +
            history.subList(Math.max(0, history.size() - 4), history.size())
                .stream().map(Message::getContent)
                .collect(Collectors.joining("\n"));
    }
}
```

---

## 4. 企业成本管控

### 4.1 成本追踪系统

```java
@Entity
@Table(name = "ai_usage_logs")
public class AiUsageLog {
    @Id @GeneratedValue
    private Long id;

    private String userId;
    private String department;
    private String model;
    private String endpoint;          // chat / rag / agent

    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal cost;          // 精确到小数点后 6 位

    private Long latencyMs;
    private Boolean cached;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

@Service
public class CostTracker {

    private final AiUsageLogRepository repository;
    private final Map<String, Pricing> pricingTable = new ConcurrentHashMap<>();

    @PostConstruct
    void initPricing() {
        pricingTable.put("gpt-4o", new Pricing(2.50, 10.00));
        pricingTable.put("gpt-4o-mini", new Pricing(0.15, 0.60));
        pricingTable.put("claude-sonnet-4", new Pricing(3.00, 15.00));
        // ...
    }

    public void track(String userId, String dept, String model,
                       int inputTokens, int outputTokens, long latencyMs) {
        Pricing pricing = pricingTable.get(model);
        if (pricing == null) return;

        BigDecimal cost = pricing.calculate(inputTokens, outputTokens);

        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setDepartment(dept);
        log.setModel(model);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setCost(cost);
        log.setLatencyMs(latencyMs);

        repository.save(log);
    }

    record Pricing(double inputPricePerM, double outputPricePerM) {
        BigDecimal calculate(int inputTokens, int outputTokens) {
            double cost = (inputTokens / 1_000_000.0) * inputPricePerM
                        + (outputTokens / 1_000_000.0) * outputPricePerM;
            return BigDecimal.valueOf(cost);
        }
    }
}
```

### 4.2 预算告警

```java
@Component
public class BudgetAlertService {

    private static final BigDecimal DAILY_BUDGET = new BigDecimal("100.00");  // $100/天
    private static final BigDecimal MONTHLY_BUDGET = new BigDecimal("2000.00");

    @Scheduled(fixedRate = 300_000)  // 每 5 分钟检查
    public void checkBudget() {
        LocalDate today = LocalDate.now();

        BigDecimal dailySpent = usageRepository.sumCost(
            today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        BigDecimal monthlySpent = usageRepository.sumCost(
            today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay());

        // 达 80% 发提醒，达 95% 发告警
        if (dailySpent.compareTo(DAILY_BUDGET.multiply(new BigDecimal("0.95"))) > 0) {
            alertService.sendAlert("日预算即将用完",
                String.format("已花费 $%.2f / $%.2f", dailySpent, DAILY_BUDGET));
        } else if (dailySpent.compareTo(DAILY_BUDGET.multiply(new BigDecimal("0.80"))) > 0) {
            notificationService.notify("日预算已用 80%",
                String.format("已花费 $%.2f", dailySpent));
        }
    }
}
```

---

## 5. 快速复习

```
□ Token ≈ 子词，中 1 字 ≈ 0.6 token，英 1 词 ≈ 1.3 token
□ Input/Output Token 分别计费，Output 通常贵 3-5 倍
□ 成本优化金字塔：路由 > 缓存 > 压缩 > 限输出 > 本地
□ 模型路由：简单任务走便宜模型，复杂任务走贵模型
□ 语义缓存：相似问题直接返回缓存答案，省 30-60%
□ Prompt 压缩：精简 Prompt + 摘要长历史
□ 企业成本管控：使用日志 + 成本追踪 + 预算告警
□ 成本仪表盘：按用户/部门/模型/时间维度统计
```

---

> **下一步**：[16 — AI 应用安全与合规](./16-AI应用安全与合规.md)
