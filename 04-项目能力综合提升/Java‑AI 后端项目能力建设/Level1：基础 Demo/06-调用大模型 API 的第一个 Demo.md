# 06 调用大模型 API 的第一个 Demo

> Level1 的转折点：从纯后端进入 AI。用两种方式（手写 HTTP 与 Spring AI）调用 DeepSeek V4 大模型，理解 OpenAI 兼容协议，跑通"Java 发请求 → 大模型回答"的第一条 AI 链路。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [OpenAI 兼容协议：先理解再写代码](#2-openai-兼容协议先理解再写代码)
3. [准备：API Key 与模型名](#3-准备api-key-与模型名)
4. [方式一：HttpClient 手写调用](#4-方式一httpclient-手写调用)
5. [方式二：Spring AI ChatClient](#5-方式二spring-ai-chatclient)
6. [流式响应：逐字返回](#6-流式响应逐字返回)
7. [常见坑](#7-常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 目标与验收

本 Demo 的产出：一个 `/ai/chat` 接口，POST 一句用户消息，返回大模型回答；再做一个流式版本（SSE 逐字返回）。验收标准：**能不看文档写出请求体结构**（messages 数组、role、model 字段）；**能讲清为什么用 OpenAI 兼容协议**；**流式与一次性调用的差异能说清**。版本基线（2026-08）：DeepSeek V4 系列、Spring AI 2.0 GA（2026-06-12）。

## 2. OpenAI 兼容协议：先理解再写代码

2026 年大模型 API 的事实标准是 **OpenAI 兼容协议**——不管底层是 DeepSeek、Qwen 还是 MiniMax，都提供同一套 HTTP 接口格式。这意味着**学会一次，所有模型通用**：只需改 base_url、api_key、model 三个配置。协议核心是 chat/completions 接口：

```json
POST https://api.deepseek.com/chat/completions
{
  "model": "deepseek-v4-flash",
  "messages": [
    {"role": "system", "content": "你是 Java 学习助手"},
    {"role": "user", "content": "什么是 RAG？"}
  ],
  "stream": false
}
```

理解三个字段：**model**（模型名，见第 3 节，2026-08 必须用 V4 系列新名）；**messages**（对话历史数组——这是多轮对话的载体，system 设定角色、user 用户消息、assistant 模型回答；"上下文"就是把多轮消息都带上）；**stream**（false 一次性返回，true 流式逐块返回）。响应结构：`choices[0].message.content` 是回答内容，`usage` 是 token 消耗。把请求体与响应结构背下来——这是后续所有 AI 链路（RAG、Agent）的地基，面试必问。

## 3. 准备：API Key 与模型名

两个准备动作：**注册 DeepSeek 开放平台拿 API Key**（platform.deepseek.com，充值少量余额，新用户常有赠送额度），Key 格式 `sk-...`，妥善保管——它是你的"数据库密码"，不要提交进代码仓库（Level1 放环境变量或配置文件，Level2 讲密钥管理）。**模型名用 2026-08 的新命名**：主力是 `deepseek-v4-flash`（快、便宜）与 `deepseek-v4-pro`（强、贵），旧名 `deepseek-chat`/`deepseek-reasoner` 已于 2026-07-24 弃用——**用旧名会直接报错或静默走旧模型**，这是 2026 年最典型的"照着老教程写不出来"的原因。价格参考（2026-08，可能调整）：flash 输入约 1 元/百万 token、输出约 2 元/百万 token，Pro 约 3 倍——本 Demo 消耗不到 1 分钱。

验证 Key 是否可用：命令行用 curl 发一次请求（Windows PowerShell 里 curl 是 Invoke-WebRequest 别名，用 `curl.exe` 或 postman/IDEA HTTP Client）：

```bash
curl https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer sk-你的Key" \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"deepseek-v4-flash\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"
```

返回带 `choices` 字段的 JSON 即 Key 可用——先排除 Key 与网络问题，再写 Java 代码，排查顺序永远是"先工具后代码"。

## 4. 方式一：HttpClient 手写调用

第一种方式不用任何 AI 框架，用 JDK 自带 HttpClient——理解协议本身。JDK 11+ 的 HttpClient 已够用，无需引第三方 HTTP 库：

```java
@Service
public class LlmClient {
    private static final String URL = "https://api.deepseek.com/chat/completions";
    private static final String API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private final HttpClient http = HttpClient.newHttpClient();

    public String chat(String userMessage) throws Exception {
        String body = """
            {"model":"deepseek-v4-flash",
             "messages":[{"role":"system","content":"你是Java学习助手"},
                         {"role":"user","content":"%s"}],
             "stream":false}
            """.formatted(userMessage);
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        // 解析 JSON：用 Jackson 读取 choices[0].message.content
        return parseContent(resp.body());
    }
}
```

三个要点：**文本块（`"""`）拼 JSON**——02 篇学过的新语法在这派上大用场，注意 userMessage 里有双引号要转义或做 JSON 转义（用 Jackson 生成 body 更稳）；**API Key 放环境变量**（`System.getenv`），不硬编码进代码；**解析用 Jackson**（Boot 自带），手写字符串截取是坑。跑通后尝试把 system 提示词改一句（"用一句话回答"），观察回答风格变化——这是"提示词即配置"的第一次体感。

**多轮对话的升级**顺手做掉——它是 RAG 与 Agent 的必备技能：把 messages 数组从"一条 user 消息"扩展为**历史数组**（每次请求带上之前的 user/assistant 消息），模型就有上下文记忆。实现：本地维护一个 `List<Message>`（Demo 阶段放内存），每次对话 append 用户消息、调接口、append 模型回答，下次请求把整个数组带上。注意上下文有长度上限（模型上下文窗口），历史太长要裁剪——**"上下文管理"的概念从这一刻开始建立**，Level2 会用向量库与摘要做系统化处理。

## 5. 方式二：Spring AI ChatClient

第二种方式用 Spring AI 2.0——它是 Spring 官方 AI 框架，主 API 是 **ChatClient**（2.0 的核心抽象，流式/工具调用/结构化输出一站式）。在 03 篇工程加依赖 `spring-ai-openai-spring-boot-starter`（DeepSeek 兼容 OpenAI，直接用它），配置三行：

```properties
spring.ai.openai.base-url=https://api.deepseek.com
spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
spring.ai.openai.chat.options.model=deepseek-v4-flash
```

```java
@RestController
public class AiController {
    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem("你是 Java 学习助手").build();
    }

    @PostMapping("/ai/chat")
    public String chat(@RequestBody String userMessage) {
        return chatClient.prompt().user(userMessage).call().content();
    }
}
```

对比两种方式的意义：手写版让你**理解协议**（请求体、鉴权、响应结构），Spring AI 版让你**理解框架**（自动配置 + 依赖注入——03 篇的机制在这里体现：starter 一加，ChatClient 自动可用）。面试被问"Spring AI 和手写有什么区别"的答案：框架封装了协议细节（base-url 配置、错误处理、流式适配），业务代码只需关心 prompt 与返回。**建议两个都写一遍**——只写框架版的人不懂协议，只写手写版的人不懂工程化，都写的人面试两头都站得住。

## 6. 流式响应：逐字返回

把接口升级为流式——回答像打字机一样逐字出现，这是 AI 应用的用户体验标配。Spring AI 的流式一行改动：

```java
@GetMapping(value = "/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String message) {
    return chatClient.prompt().user(message).stream().content();
}
```

返回类型变成 `Flux<String>`（响应式流），配合 WebFlux 的 SSE 输出格式。理解流式的本质：**模型逐 token 生成，每生成一段就推给客户端**，而不是等全部生成完——所以首字延迟远小于总耗时。验证方式：浏览器直接访问接口地址，能看到内容逐段刷出来（而不是等几秒一次出现）。**为什么需要流式**：大模型回答长内容要几十秒，一次性返回让用户干等（体验差且可能超时），流式让用户 1 秒内看到第一个字。注意：流式接口需要 `spring-boot-starter-webflux` 依赖与 SSE 输出格式（TEXT_EVENT_STREAM_VALUE），手动配置在 Level2 讲，Demo 阶段用注解默认即可。

## 7. 常见坑

**401 鉴权失败**：API Key 错误、过期或格式不对（多了引号、少了 sk- 前缀）。先用 curl 验证 Key。

**404 或模型不存在**：模型名写错（还在用 deepseek-chat 旧名）。2026-08 用 deepseek-v4-flash / deepseek-v4-pro。

**429 限流**：请求太频繁或余额不足。检查余额，加短暂延时重试。

**超时**：默认 HttpClient 无超时，模型响应慢时会一直挂着。设 `HttpRequest` 的 timeout，或 Spring AI 的 `spring.ai.openai.chat.options.timeout`。

**中文乱码/JSON 解析失败**：请求体编码或响应编码问题。确认 Content-Type 为 application/json，字符编码 UTF-8。

**SSL 证书错误**：公司网络代理或自签证书环境。Demo 阶段换网络解决，不做证书绕过（绕过是安全坏习惯）。

## 8. 核心要点

1. OpenAI 兼容协议是事实标准：学会一次所有模型通用，请求体三字段（model/messages/stream）必背。
2. 模型名用 V4 新名（deepseek-v4-flash/pro），旧名已弃用；Key 放环境变量不提交代码。
3. 先 curl 验证 Key 再写 Java 代码，排查顺序"先工具后代码"。
4. 手写 HttpClient 理解协议，Spring AI ChatClient 理解框架，两个都写一遍。
5. 流式是体验标配：Flux 逐 token 推流，首字延迟远小于总耗时。

> 🎯 **核心要点**：本 Demo 的完成标志是**"协议与框架两层都理解"**——手写版证明你懂 AI 链路底层，框架版证明你懂工程化封装。这两层的理解叠加，是 Level2 里 RAG、Agent、工具调用一切 AI 功能的地基。

---

**下一模块**：[07 RAG 入门 Demo](./07-RAG%20入门%20Demo.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
