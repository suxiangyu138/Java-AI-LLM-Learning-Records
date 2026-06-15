# 🚀 大模型 API 调用实践

> **核心摘要**：掌握 OpenAI 兼容 API 的标准化调用方式，覆盖 DeepSeek、通义千问、智谱 AI 等主流平台。本文从 HTTP 协议基础到生产级封装（重试、流式输出、Key 安全、Java 后端集成），提供可直接复用的完整代码。

> **前置阅读**：[[快速精通GPT]]、[[快速精通Gemini]]

---

## 目录

1. [OpenAI 兼容 API 协议](#一openai-兼容-api-协议)
2. [OpenAI SDK 调用](#二openai-sdk-调用)
3. [原生 HTTP 调用](#三原生-http-调用)
4. [生产级错误处理与重试](#四生产级错误处理与重试)
5. [API Key 安全管理](#五api-key-安全管理)
6. [Java 后端集成](#六java-后端集成)
7. [完整项目骨架](#七完整项目骨架)

---

## 一、OpenAI 兼容 API 协议

主流国产大模型（DeepSeek、通义千问、智谱 GLM、Moonshot）均兼容 OpenAI SDK 的接口格式，**同一套代码可切换不同模型**。

### 通用消息格式

```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "你是一个Java技术专家"},
    {"role": "user", "content": "Spring Boot如何配置多数据源？"},
    {"role": "assistant", "content": "可以使用@ConfigurationProperties..."},
    {"role": "user", "content": "那动态数据源呢？"}
  ],
  "temperature": 0.7,
  "max_tokens": 2048,
  "stream": false
}
```

### 各平台 API 地址速查

| 平台 | API Base URL | 模型示例 |
|---|---|---|
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| 智谱 AI | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` |
| Moonshot | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` |
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |

---

## 二、OpenAI SDK 调用

### 2.1 安装与配置

```bash
pip install openai
```

```python
from openai import OpenAI

# DeepSeek
client = OpenAI(
    api_key="sk-your-deepseek-key",
    base_url="https://api.deepseek.com/v1"
)

# 通义千问
client = OpenAI(
    api_key="sk-your-qwen-key",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
)
```

### 2.2 基础调用

```python
def chat(prompt: str, system_prompt: str = "你是一个有用的AI助手") -> str:
    """单轮对话"""
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": prompt}
        ],
        temperature=0.7,
        max_tokens=2048
    )
    return response.choices[0].message.content

# 使用
answer = chat("Java中HashMap和ConcurrentHashMap的区别？")
print(answer)
```

### 2.3 多轮对话（上下文记忆）

```python
class Conversation:
    """多轮对话管理器"""

    def __init__(self, system_prompt: str = "你是一个有用的AI助手"):
        self.messages: list[dict] = [
            {"role": "system", "content": system_prompt}
        ]

    def send(self, user_input: str) -> str:
        self.messages.append({"role": "user", "content": user_input})

        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=self.messages,
            temperature=0.7,
            max_tokens=2048
        )
        assistant_msg = response.choices[0].message.content
        self.messages.append({"role": "assistant", "content": assistant_msg})

        # Token 管理：超过阈值时自动裁剪早期消息
        self._trim_if_needed(max_messages=20)
        return assistant_msg

    def _trim_if_needed(self, max_messages: int):
        """保留 system prompt + 最近 N 条消息"""
        if len(self.messages) > max_messages + 1:
            self.messages = [self.messages[0]] + self.messages[-(max_messages):]

    def clear(self):
        self.messages = [self.messages[0]]  # 保留 system prompt
```

### 2.4 流式输出

```python
def chat_stream(prompt: str) -> str:
    """流式输出——逐字返回，提升用户体验"""
    stream = client.chat.completions.create(
        model="deepseek-chat",
        messages=[{"role": "user", "content": prompt}],
        stream=True
    )

    full_response = ""
    for chunk in stream:
        if chunk.choices[0].delta.content:
            content = chunk.choices[0].delta.content
            print(content, end="", flush=True)  # 实时打印
            full_response += content
    return full_response
```

---

## 三、原生 HTTP 调用

当环境受限（无法安装 `openai` 包）或需要精细控制时，可直接使用 HTTP 请求：

```python
import requests
import json


def call_deepseek_direct(prompt: str, api_key: str) -> str:
    """直接 HTTP 调用 DeepSeek API"""
    response = requests.post(
        url="https://api.deepseek.com/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        },
        json={
            "model": "deepseek-chat",
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.7,
            "max_tokens": 2048
        },
        timeout=30
    )
    response.raise_for_status()
    data = response.json()
    return data["choices"][0]["message"]["content"]


def call_with_stream_direct(prompt: str, api_key: str):
    """原生流式调用"""
    response = requests.post(
        url="https://api.deepseek.com/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        },
        json={
            "model": "deepseek-chat",
            "messages": [{"role": "user", "content": prompt}],
            "stream": True
        },
        stream=True,
        timeout=60
    )

    for line in response.iter_lines():
        if line:
            line = line.decode("utf-8")
            if line.startswith("data: "):
                data_str = line[6:]
                if data_str == "[DONE]":
                    break
                chunk = json.loads(data_str)
                content = chunk["choices"][0]["delta"].get("content", "")
                if content:
                    yield content
```

---

## 四、生产级错误处理与重试

```python
import time
from typing import Generator


class LLMService:
    """生产级大模型调用封装"""

    def __init__(self, client: OpenAI, model: str = "deepseek-chat"):
        self.client = client
        self.model = model

    def chat_with_retry(
        self,
        messages: list[dict],
        max_retries: int = 3,
        timeout: int = 30
    ) -> str:
        """带指数退避重试的对话调用"""
        for attempt in range(max_retries):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=messages,
                    temperature=0.7,
                    timeout=timeout
                )
                return response.choices[0].message.content

            except Exception as e:
                error_msg = str(e)

                # 不可重试的错误：直接抛出
                if "authentication" in error_msg.lower():
                    raise RuntimeError(f"API Key 无效: {e}")
                if "invalid_request" in error_msg.lower() and "content_filter" not in error_msg.lower():
                    raise

                # 可重试的错误
                if attempt == max_retries - 1:
                    raise RuntimeError(f"重试 {max_retries} 次后仍失败: {e}")

                wait = 2 ** attempt  # 1s → 2s → 4s
                print(f"[Retry {attempt + 1}/{max_retries}] {error_msg[:100]}，{wait}s 后重试...")
                time.sleep(wait)

    def chat_stream_safe(
        self,
        prompt: str,
        max_retries: int = 2
    ) -> Generator[str, None, None]:
        """安全的流式调用"""
        for attempt in range(max_retries):
            try:
                stream = self.client.chat.completions.create(
                    model=self.model,
                    messages=[{"role": "user", "content": prompt}],
                    stream=True,
                    timeout=60
                )
                for chunk in stream:
                    content = chunk.choices[0].delta.content or ""
                    if content:
                        yield content
                return

            except Exception:
                if attempt == max_retries - 1:
                    raise
                time.sleep(2 ** attempt)
```

> **重点**：重试策略应区分"可重试错误"（超时、限流、5xx）和"不可重试错误"（认证失败、请求格式错误），避免无效重试浪费资源。

---

## 五、API Key 安全管理

```python
# ❌ 永远不要把 Key 硬编码在代码里
API_KEY = "sk-xxxxxxxx"  # 危险！

# ✅ 使用环境变量
import os
api_key = os.environ.get("DEEPSEEK_API_KEY")

# ✅ 使用 .env 文件 + python-dotenv
# .env 文件：
# DEEPSEEK_API_KEY=sk-xxxxxxxx
# DEEPSEEK_BASE_URL=https://api.deepseek.com/v1

from dotenv import load_dotenv
load_dotenv()

api_key = os.getenv("DEEPSEEK_API_KEY")
base_url = os.getenv("DEEPSEEK_BASE_URL")

client = OpenAI(api_key=api_key, base_url=base_url)

# 记得把 .env 加入 .gitignore
# echo ".env" >> .gitignore
```

> **注意**：对于 Java 后端项目，API Key 应配置在 `application.yml` 或环境变量中，避免提交到版本控制。

---

## 六、Java 后端集成

```java
// RestTemplate 方式
@Service
public class LLMService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public LLMService(RestTemplateBuilder builder) {
        this.restTemplate = builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String chat(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "model", "deepseek-chat",
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "temperature", 0.7,
            "max_tokens", 2048
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            apiUrl + "/v1/chat/completions", request, Map.class
        );

        Map<String, Object> choice = ((List<Map>) response.getBody().get("choices")).get(0);
        return (String) ((Map) choice.get("message")).get("content");
    }

    // 流式调用（WebFlux）
    public Flux<String> chatStream(String prompt) {
        WebClient webClient = WebClient.builder()
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();

        Map<String, Object> body = Map.of(
            "model", "deepseek-chat",
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", true
        );

        return webClient.post()
            .uri(apiUrl + "/v1/chat/completions")
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
            .map(line -> { /* 解析 JSON 提取 content */ return ""; });
    }
}
```

---

## 七、完整项目骨架

```python
# chatbot.py — 可直接运行的聊天机器人
import os
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

class ChatBot:
    def __init__(self):
        self.client = OpenAI(
            api_key=os.getenv("DEEPSEEK_API_KEY"),
            base_url=os.getenv("DEEPSEEK_BASE_URL")
        )
        self.history: list[dict] = [
            {"role": "system", "content": "你是一个友好的AI助手，用中文回答。"}
        ]

    def chat(self, user_input: str) -> str:
        self.history.append({"role": "user", "content": user_input})
        try:
            response = self.client.chat.completions.create(
                model="deepseek-chat",
                messages=self.history[-10:],  # 最近10轮
                temperature=0.7,
                max_tokens=1024
            )
            reply = response.choices[0].message.content
            self.history.append({"role": "assistant", "content": reply})
            return reply
        except Exception as e:
            return f"出错啦: {e}"

    def run(self):
        print("智能助手已启动！(输入 'quit' 退出)\n")
        while True:
            user_input = input("You: ")
            if user_input.lower() == "quit":
                print("再见！")
                break
            reply = self.chat(user_input)
            print(f"AI: {reply}\n")

if __name__ == "__main__":
    ChatBot().run()
```

---

## 快速调试检查清单

- [ ] API Key 是否正确设置？`echo $DEEPSEEK_API_KEY`
- [ ] Base URL 是否以 `/v1` 结尾？不要有多余的 `/`
- [ ] 网络是否能访问 API 域名？（国内服务器可能需代理）
- [ ] `stream=True` 时是否用了 `iter_lines()` 而非 `.json()`？
- [ ] `.env` 文件是否在 `.gitignore` 中？

---

## 核心要点回顾

- 主流大模型 API 均兼容 OpenAI 协议格式，同一套代码可切换不同模型
- OpenAI SDK 提供最简洁的调用方式；原生 HTTP 调用适用于受限环境
- 生产环境必须实现错误分类、指数退避重试和流式输出
- API Key 应通过环境变量或 .env 文件管理，严禁硬编码
- Java 后端可通过 RestTemplate / WebClient（流式）集成大模型 API

## 参考资料

1. OpenAI API 文档：https://platform.openai.com/docs/api-reference
2. DeepSeek API 文档：https://platform.deepseek.com/api-docs
3. 通义千问 API 文档：https://help.aliyun.com/zh/dashscope/
4. 智谱 AI API 文档：https://open.bigmodel.cn/dev/api
5. Spring WebClient 文档：https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
