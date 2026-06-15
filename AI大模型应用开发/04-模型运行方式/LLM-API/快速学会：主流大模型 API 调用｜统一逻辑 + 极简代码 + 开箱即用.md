# 🚀 主流大模型 API 调用：统一逻辑 + 极简代码 + 开箱即用

> **核心摘要**：所有主流大模型 API 遵循统一的 HTTPS + POST + JSON + API-Key 协议。本文提供各平台的极简调用代码（原始 HTTP 与 OpenAI 兼容 SDK 两种方式），涵盖通义千问、智谱 GLM、DeepSeek、本地 Ollama，以及 Java 后端集成方案。

> **前置阅读**：[[大模型API调用实践]]、[[LLM API 全面解析]]

---

## 一、核心底层原理（所有大模型通用）

### 统一协议

| 维度 | 标准 |
|---|---|
| 协议 | HTTPS |
| 请求方式 | POST |
| 数据格式 | JSON |
| 身份认证 | API-Key 请求头携带（`Authorization: Bearer`） |

### 通用调用流程

```
构造请求体（模型 + 上下文 + 参数）
→ 携带 Authorization 密钥
→ 发送 POST 请求
→ 解析 JSON 回答
```

### 两种模式

- **普通问答**：一次性返回全部结果
- **流式输出（Stream）**：逐字返回，打字机效果

---

## 二、通用参数（所有模型通用）

| 参数 | 说明 |
|---|---|
| `model` | 模型名称（`gpt-4o`、`qwen-turbo`、`glm-4-flash`、`deepseek-chat`） |
| `messages` | 对话上下文，`role`（user/assistant/system）+ `content` |
| `temperature` | 随机性 0~1，越高越放飞，越低越严谨 |
| `stream` | 是否流式输出 |

### 统一消息结构

```json
[
  {"role": "system", "content": "你是 Java 技术专家"},
  {"role": "user", "content": "解释 JVM 内存模型"},
  {"role": "assistant", "content": "JVM 内存分为堆、栈、方法区..."}
]
```

---

## 三、主流平台极简调用

### 3.1 通义千问（阿里云 Qwen）

```python
import requests

url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
headers = {
    "Authorization": "Bearer 你的API_KEY",
    "Content-Type": "application/json"
}
body = {
    "model": "qwen-turbo",
    "input": {"messages": [{"role": "user", "content": "Java是什么"}]},
    "parameters": {"temperature": 0.7}
}
res = requests.post(url, headers=headers, json=body)
print(res.json()["output"]["text"])
```

### 3.2 智谱 AI GLM

```python
url = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
headers = {
    "Authorization": "Bearer 你的API_KEY",
    "Content-Type": "application/json"
}
body = {
    "model": "glm-4-flash",
    "messages": [{"role": "user", "content": "解释RabbitMQ"}],
    "temperature": 0.7
}
res = requests.post(url, headers=headers, json=body)
print(res.json()["choices"][0]["message"]["content"])
```

### 3.3 DeepSeek

```python
url = "https://api.deepseek.com/v1/chat/completions"
body = {
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "ES倒排索引原理"}]
}
res = requests.post(url, headers=headers, json=body)
```

### 3.4 OpenAI 兼容标准写法（推荐）

当前 90% 国内大模型兼容 OpenAI 格式，包括 DeepSeek、通义千问、GLM、豆包、星火、本地 Ollama。

```bash
pip install openai
```

```python
from openai import OpenAI

client = OpenAI(
    api_key="你的KEY",
    base_url="对应厂商地址"  # 此处切换不同平台
)

resp = client.chat.completions.create(
    model="模型名",  # 此处切换不同模型
    messages=[{"role": "user", "content": "介绍Docker"}],
    temperature=0.7
)
print(resp.choices[0].message.content)
```

> **重点**：一套代码只需修改 `base_url` 和 `model` 两行配置，即可切换所有主流大模型。

---

## 四、本地大模型 API（Ollama）

本地部署 Ollama 后自带 OpenAI 兼容接口，无需 API Key：

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"  # 占位符，实际不校验
)

res = client.chat.completions.create(
    model="llama3",
    messages=[{"role": "user", "content": "解释经济危机"}]
)
print(res.choices[0].message.content)
```

---

## 五、Java 后端调用

核心逻辑不变：HTTP POST + JSON 请求体 + Header 带 Token。

```java
// 使用 RestTemplate 调用大模型 API
@Service
public class LLMService {

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public String chat(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            apiUrl + "/v1/chat/completions", request, Map.class
        );

        // 解析返回结果
        Map<String, Object> choice = ((List<Map>) response.getBody().get("choices")).get(0);
        return (String) ((Map) choice.get("message")).get("content");
    }
}
```

> **企业常用**：OkHttp、RestTemplate、WebClient（流式）。建议封装统一工具类，一键切换多模型。

---

## 六、流式输出（打字机效果）

1. 请求加参数：`"stream": true`
2. 响应格式：`text/event-stream` 流数据
3. 分段解析、拼接内容

所有厂商逻辑完全一致，适合构建 AI 聊天网页 / 客户端。

---

## 核心要点回顾

- 所有大模型 API 协议统一：HTTPS + POST + JSON + API-Key
- OpenAI 兼容标准是事实上的行业标准，一套代码覆盖 90% 平台
- 本地 Ollama 提供同样的 OpenAI 兼容接口，免费无 key
- Java 后端通过 RestTemplate / WebClient 即可集成
- 关键三要素：请求地址 + API_KEY + messages 对话结构

## 参考资料

1. OpenAI API 文档：https://platform.openai.com/docs/api-reference
2. DeepSeek API 文档：https://platform.deepseek.com/api-docs
3. 通义千问 API 文档：https://help.aliyun.com/zh/dashscope/
4. 智谱 AI API 文档：https://open.bigmodel.cn/dev/api
5. Ollama 官方文档：https://ollama.ai/blog/openai-compatibility
