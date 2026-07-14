# 第6步：大模型API应用基础

> **阶段目标：** 掌握主流大模型API的使用方法，理解Token定价机制，建立API Key安全管理体系  
> **预计学时：** 1-2周（每天3-4小时）  
> **前置要求：** Prompt基础概念 + Python编程  

---

## 📚 目录

- [6.1 主流大模型API概览](#61-主流大模型api概览)
- [6.2 OpenAI API完整指南](#62-openai-api完整指南)
- [6.3 多Provider统一调用](#63-多provider统一调用)
- [6.4 Token定价与成本控制](#64-token定价与成本控制)
- [6.5 API Key安全管理](#65-api-key安全管理)
- [6.6 企业级API封装](#66-企业级api封装)
- [6.7 阶段练习](#67-阶段练习)
- [6.8 常见问题](#68-常见问题)

---

## 6.1 主流大模型API概览

### 6.1.1 市场格局

```
2024年主流API Provider

┌─────────────────────────────────────────────────────────┐
│ Provider     │ 代表模型         │ 特点                  │
├──────────────┼─────────────────┼───────────────────────┤
│ OpenAI       │ GPT-4o, GPT-4   │ 生态最完善, 性能领先   │
│ Anthropic    │ Claude 4.0      │ 长上下文, 安全性强     │
│ Google       │ Gemini 1.5      │ 多模态原生, 超长上下文 │
│ 阿里         │ 通义千问         │ 中文能力强, 国内可用   │
│ 百度         │ 文心一言         │ 国内合规, 中文优化    │
│ 字节         │ 豆包(Doubao)    │ 性价比高, 国内可用    │
│ 智谱         │ ChatGLM         │ 开源+API双模式        │
│ DeepSeek     │ DeepSeek-V2     │ 性价比极高            │
│ 月之暗面      │ Kimi            │ 超长上下文, 中文强    │
└─────────────────────────────────────────────────────────┘
```

### 6.1.2 API调用统一模式

```python
# 所有大模型API的调用模式高度相似：
# 
# 1. 创建客户端（需要API Key）
# 2. 构造请求（Messages + 参数）
# 3. 发送请求
# 4. 解析响应（提取content + usage）
#
# 理解了OpenAI的API格式，其他Provider基本一致
```

---

## 6.2 OpenAI API完整指南

### 6.2.1 基础调用

```python
import openai
import os

# 初始化客户端
client = openai.OpenAI(
    api_key=os.getenv("OPENAI_API_KEY"),
    # base_url="https://api.openai.com/v1",  # 默认
    # 国内代理示例：base_url="https://your-proxy.com/v1",
)

# 最简单的调用
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[
        {"role": "system", "content": "你是一个有用的AI助手"},
        {"role": "user", "content": "解释什么是REST API"},
    ],
)

# 提取结果
content = response.choices[0].message.content
usage = response.usage

print(f"回复: {content}")
print(f"输入Token: {usage.prompt_tokens}")
print(f"输出Token: {usage.completion_tokens}")
print(f"总Token: {usage.total_tokens}")
```

### 6.2.2 核心参数详解

```python
# ========== 完整参数列表 ==========
response = client.chat.completions.create(
    model="gpt-4o",                    # 模型名称
    messages=[...],                    # 对话历史
    
    # ——— 生成控制 ———
    temperature=0.7,                   # 随机性 (0-2)
    top_p=0.9,                         # 核采样
    max_tokens=1024,                   # 最大输出Token数
    
    # ——— 输出控制 ———
    n=1,                               # 生成几个回复
    stop=["\n\n", "END"],             # 停止词
    presence_penalty=0,                # 话题新颖度 (-2.0 ~ 2.0)
    frequency_penalty=0,               # 用词多样性 (-2.0 ~ 2.0)
    
    # ——— 高级功能 ———
    stream=False,                      # 流式输出（第13步详解）
    response_format={"type": "json_object"},  # JSON模式
    seed=42,                           # 确定性输出
    logprobs=True,                     # 返回概率
    top_logprobs=5,                    # 返回top-5的Token概率
    
    # ——— 工具调用 ———
    tools=[...],                       # Function Calling（第14步详解）
    tool_choice="auto",
)

# ========== presence_penalty vs frequency_penalty ==========
"""
presence_penalty (存在惩罚):
  正值 → 鼓励模型谈论新话题
  负值 → 鼓励模型围绕同一话题

frequency_penalty (频率惩罚):
  正值 → 减少重复用词
  负值 → 允许更多重复

控制幻觉的典型设置:
  temperature=0.3, presence_penalty=0.5
  低温保证准确性, 存在惩罚减少重复
"""
```

### 6.2.3 多轮对话

```python
class ConversationManager:
    """多轮对话管理器 — 自动管理上下文窗口"""
    
    def __init__(self, client, model: str = "gpt-4o",
                 system_prompt: str = "", max_history: int = 20):
        self.client = client
        self.model = model
        self.max_history = max_history
        self.messages = []
        
        if system_prompt:
            self.messages.append({"role": "system", "content": system_prompt})
    
    def chat(self, user_input: str, **kwargs) -> str:
        """发送消息并获取回复"""
        self.messages.append({"role": "user", "content": user_input})
        
        response = self.client.chat.completions.create(
            model=self.model,
            messages=self.messages[-self.max_history:],  # 只保留最近N轮
            **kwargs,
        )
        
        assistant_message = response.choices[0].message.content
        self.messages.append({"role": "assistant", "content": assistant_message})
        
        return assistant_message
    
    def clear_history(self):
        """清空对话历史（保留system prompt）"""
        self.messages = self.messages[:1] if self.messages and \
            self.messages[0]["role"] == "system" else []
    
    def get_token_count(self) -> int:
        """估算当前对话的Token总数"""
        total = ""
        for m in self.messages:
            total += m["content"]
        # 精确计算请用tiktoken
        return len(total)


# 使用
conv = ConversationManager(
    client=client,
    model="gpt-4o",
    system_prompt="你是一个Python编程专家，回答要简洁实用。",
)

print(conv.chat("如何在Python中读取CSV文件？"))
print(conv.chat("那怎么写入呢？"))  # 模型记得上一轮的问题
```

### 6.2.4 JSON结构化输出

```python
# ========== 方法1: JSON Mode (更可靠) ==========
response = client.chat.completions.create(
    model="gpt-4o",
    response_format={"type": "json_object"},
    messages=[
        {"role": "system", "content": "提取文本中的信息，返回JSON"},
        {"role": "user", "content": """
        提取以下文本中的关键信息：
        "张三，28岁，软件工程师，工作地点在北京"
        
        返回格式：
        {"name": "姓名", "age": 年龄数字, "job": "职业", "city": "城市"}
        """},
    ],
)

import json
info = json.loads(response.choices[0].message.content)
print(f"姓名: {info['name']}, 年龄: {info['age']}")

# ========== 方法2: Prompt中指定格式 ==========
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{
        "role": "user",
        "content": """
        分析以下评论的情感，只返回JSON格式，不要其他内容：
        评论："这个产品质量不错，但物流太慢了"
        
        格式：{"sentiment": "正面/负面/中性", "score": 0-1分, "keywords": [...], "summary": "简短总结"}
        """
    }],
    temperature=0.1,  # 低温度确保格式稳定
)
```

---

## 6.3 多Provider统一调用

### 6.3.1 统一接口设计

```python
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional, Generator
from dataclasses import dataclass
from enum import Enum

@dataclass
class Message:
    role: str
    content: str

@dataclass
class LLMResponse:
    content: str
    model: str
    usage: Dict[str, int]  # {"prompt_tokens": N, "completion_tokens": M}
    finish_reason: str
    raw_response: Any = None

class LLMProvider(ABC):
    """大模型Provider统一接口"""
    
    @abstractmethod
    def chat(self, messages: List[Message], **kwargs) -> LLMResponse:
        """同步聊天"""
        pass
    
    @abstractmethod
    def chat_stream(self, messages: List[Message], **kwargs) -> Generator[str, None, None]:
        """流式聊天"""
        pass


class OpenAIProvider(LLMProvider):
    """OpenAI实现"""
    
    def __init__(self, api_key: str, base_url: str = None):
        self.client = openai.OpenAI(
            api_key=api_key,
            base_url=base_url or "https://api.openai.com/v1",
        )
    
    def chat(self, messages: List[Message], 
             model: str = "gpt-4o", **kwargs) -> LLMResponse:
        response = self.client.chat.completions.create(
            model=model,
            messages=[{"role": m.role, "content": m.content} for m in messages],
            **kwargs,
        )
        
        choice = response.choices[0]
        return LLMResponse(
            content=choice.message.content,
            model=response.model,
            usage={
                "prompt_tokens": response.usage.prompt_tokens,
                "completion_tokens": response.usage.completion_tokens,
            },
            finish_reason=choice.finish_reason,
            raw_response=response,
        )
    
    def chat_stream(self, messages: List[Message], 
                    model: str = "gpt-4o", **kwargs):
        stream = self.client.chat.completions.create(
            model=model,
            messages=[{"role": m.role, "content": m.content} for m in messages],
            stream=True,
            **kwargs,
        )
        for chunk in stream:
            if chunk.choices[0].delta.content:
                yield chunk.choices[0].delta.content


class AnthropicProvider(LLMProvider):
    """Anthropic Claude 实现"""
    
    def __init__(self, api_key: str):
        import anthropic
        self.client = anthropic.Anthropic(api_key=api_key)
    
    def chat(self, messages: List[Message],
             model: str = "claude-sonnet-4-6", **kwargs) -> LLMResponse:
        # Anthropic 的 system prompt 需要单独提取
        system_msg = ""
        chat_messages = []
        for m in messages:
            if m.role == "system":
                system_msg = m.content
            else:
                chat_messages.append({"role": m.role, "content": m.content})
        
        response = self.client.messages.create(
            model=model,
            system=system_msg,
            messages=chat_messages,
            max_tokens=kwargs.get("max_tokens", 1024),
            temperature=kwargs.get("temperature", 0.7),
        )
        
        return LLMResponse(
            content=response.content[0].text,
            model=response.model,
            usage={
                "prompt_tokens": response.usage.input_tokens,
                "completion_tokens": response.usage.output_tokens,
            },
            finish_reason=response.stop_reason,
        )
    
    def chat_stream(self, messages: List[Message], 
                    model: str = "claude-sonnet-4-6", **kwargs):
        system_msg = ""
        chat_messages = []
        for m in messages:
            if m.role == "system":
                system_msg = m.content
            else:
                chat_messages.append({"role": m.role, "content": m.content})
        
        with self.client.messages.stream(
            model=model,
            system=system_msg,
            messages=chat_messages,
            max_tokens=kwargs.get("max_tokens", 1024),
        ) as stream:
            for text in stream.text_stream:
                yield text
```

---

## 6.4 Token定价与成本控制

### 6.4.1 价格对照表（2024年参考）

```python
# ========== Token定价 (美元/1M tokens) ==========
PRICING = {
    # OpenAI
    "gpt-4o":           {"input": 5.00,  "output": 15.00},
    "gpt-4o-mini":      {"input": 0.15,  "output": 0.60},
    "gpt-4-turbo":      {"input": 10.00, "output": 30.00},
    "gpt-3.5-turbo":    {"input": 0.50,  "output": 1.50},
    
    # Anthropic
    "claude-opus-4-8":  {"input": 15.00, "output": 75.00},
    "claude-sonnet-4-6":{"input": 3.00,  "output": 15.00},
    "claude-haiku-4-5": {"input": 0.25,  "output": 1.25},
    
    # Google
    "gemini-1.5-pro":   {"input": 3.50,  "output": 10.50},
    "gemini-1.5-flash": {"input": 0.075, "output": 0.30},
    
    # DeepSeek (极便宜)
    "deepseek-chat":    {"input": 0.14,  "output": 0.28},
}

# 价格差异可达100倍+ 
# gpt-4o:     $20/1M tokens
# gpt-4o-mini: $0.75/1M tokens
# deepseek:    $0.42/1M tokens
```

### 6.4.2 成本计算器

```python
class CostCalculator:
    """API调用成本追踪器"""
    
    def __init__(self, pricing: dict = None):
        self.pricing = pricing or PRICING
        self.total_cost = 0.0
        self.total_tokens = {"input": 0, "output": 0}
        self.call_history = []
    
    def calculate(self, model: str, prompt_tokens: int, 
                  completion_tokens: int) -> float:
        """计算单次调用成本"""
        if model not in self.pricing:
            print(f"⚠️ 未知模型 {model}，按gpt-4o价格计算")
            model = "gpt-4o"
        
        p = self.pricing[model]
        cost = (prompt_tokens / 1_000_000) * p["input"] + \
               (completion_tokens / 1_000_000) * p["output"]
        
        # 记录
        self.total_cost += cost
        self.total_tokens["input"] += prompt_tokens
        self.total_tokens["output"] += completion_tokens
        self.call_history.append({
            "model": model,
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "cost": cost,
        })
        
        return cost
    
    def summary(self) -> str:
        """成本汇总报告"""
        return f"""
        ╔═══════════════════════════════╗
        ║     API 成本汇总报告          ║
        ╠═══════════════════════════════╣
        ║  总调用次数: {len(self.call_history):>14} ║
        ║  总输入Token: {self.total_tokens['input']:>12,} ║
        ║  总输出Token: {self.total_tokens['output']:>12,} ║
        ║  总成本:     ${self.total_cost:>13.4f} ║
        ╚═══════════════════════════════╝
        """
    
    def estimate_monthly(self, calls_per_day: int, 
                         avg_prompt_tokens: int = 500,
                         avg_completion_tokens: int = 1000,
                         model: str = "gpt-4o-mini") -> float:
        """估算月度成本"""
        cost_per_call = self.calculate(model, avg_prompt_tokens, avg_completion_tokens)
        monthly = cost_per_call * calls_per_day * 30
        print(f"预估月度成本 ({model}): ${monthly:.2f}")
        print(f"  假设: {calls_per_day}次/天, {avg_prompt_tokens}+{avg_completion_tokens} tokens/次")
        return monthly


# ========== 省钱技巧 ==========
"""
1. 任务分级：简单任务用小模型，复杂任务用大模型
   - 文本分类/摘要/简单问答 → gpt-4o-mini
   - 复杂推理/代码生成/创意写作 → gpt-4o
   - 成本可以差10-30倍！

2. Prompt优化：精简System Prompt，减少不必要的示例
   - 每1000 tokens ≈ $0.005~$0.03

3. 缓存利用：相同的System Prompt可以缓存
   - Anthropic支持Prompt Caching，成本降低90%

4. 输出控制：设置合理的max_tokens，不要太大
   - 默认4096已经很充足，多数场景1024就够了
"""
```

---

## 6.5 API Key安全管理

### 6.5.1 企业级Key管理方案

```python
# ========== 配置文件结构 ==========
# .env (不提交到Git！)
"""
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxx
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxx
GOOGLE_API_KEY=xxxxxxxxxxxx
CUSTOM_BASE_URL=https://your-proxy.com/v1
"""

# config.py
import os
from enum import Enum
from dataclasses import dataclass
from typing import Optional, Dict
from dotenv import load_dotenv

load_dotenv()  # 加载.env文件

class ModelProvider(Enum):
    OPENAI = "openai"
    ANTHROPIC = "anthropic"
    GOOGLE = "google"
    DEEPSEEK = "deepseek"

@dataclass
class APIConfig:
    provider: ModelProvider
    api_key: str
    base_url: str
    default_model: str
    max_retries: int = 3
    timeout: int = 60

class APIConfigManager:
    """企业级API配置管理器"""
    
    def __init__(self):
        self._configs: Dict[ModelProvider, APIConfig] = {}
        self._load_configs()
    
    def _load_configs(self):
        """从环境变量加载所有配置"""
        # OpenAI
        if os.getenv("OPENAI_API_KEY"):
            self._configs[ModelProvider.OPENAI] = APIConfig(
                provider=ModelProvider.OPENAI,
                api_key=os.getenv("OPENAI_API_KEY"),
                base_url=os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                default_model=os.getenv("OPENAI_DEFAULT_MODEL", "gpt-4o-mini"),
            )
        
        # Anthropic
        if os.getenv("ANTHROPIC_API_KEY"):
            self._configs[ModelProvider.ANTHROPIC] = APIConfig(
                provider=ModelProvider.ANTHROPIC,
                api_key=os.getenv("ANTHROPIC_API_KEY"),
                base_url="https://api.anthropic.com",
                default_model="claude-sonnet-4-6",
            )
        
        # DeepSeek (兼容OpenAI格式)
        if os.getenv("DEEPSEEK_API_KEY"):
            self._configs[ModelProvider.DEEPSEEK] = APIConfig(
                provider=ModelProvider.DEEPSEEK,
                api_key=os.getenv("DEEPSEEK_API_KEY"),
                base_url="https://api.deepseek.com",
                default_model="deepseek-chat",
            )
    
    def get(self, provider: ModelProvider) -> APIConfig:
        """获取配置"""
        if provider not in self._configs:
            raise ValueError(f"未配置 {provider.value}，请检查.env文件")
        return self._configs[provider]
    
    def list_available(self) -> list:
        """列出所有可用的Provider"""
        return list(self._configs.keys())


# ========== 安全检查清单 ==========
"""
✅ .env文件已加入.gitignore
✅ API Key使用环境变量，不在代码中硬编码
✅ 生产环境使用密钥管理服务（AWS Secrets Manager / Azure Key Vault）
✅ API Key有使用限额和预算告警
✅ 定期轮换API Key
✅ 开发/测试/生产环境使用不同的API Key
✅ 监控API调用量和异常使用模式
"""
```

---

## 6.6 企业级API封装

### 6.6.1 带重试和降级的API客户端

```python
import time
import logging
from functools import wraps
from typing import Callable, TypeVar

logger = logging.getLogger(__name__)
T = TypeVar('T')

class LLMAPIError(Exception):
    """LLM API 统一异常"""
    pass

class RateLimitError(LLMAPIError):
    """速率限制错误"""
    pass

class InvalidAPIKeyError(LLMAPIError):
    """API Key无效"""
    pass

def retry_with_backoff(
    max_retries: int = 3,
    initial_delay: float = 1.0,
    backoff_factor: float = 2.0,
    retryable_exceptions: tuple = (RateLimitError, ConnectionError),
):
    """指数退避重试装饰器"""
    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        @wraps(func)
        def wrapper(*args, **kwargs):
            delay = initial_delay
            last_exception = None
            
            for attempt in range(max_retries + 1):
                try:
                    return func(*args, **kwargs)
                except retryable_exceptions as e:
                    last_exception = e
                    if attempt < max_retries:
                        logger.warning(
                            f"API调用失败 (尝试{attempt+1}/{max_retries+1}): {e} "
                            f"— {delay:.1f}秒后重试"
                        )
                        time.sleep(delay)
                        delay *= backoff_factor
                    else:
                        logger.error(f"API调用最终失败 (已重试{max_retries}次)")
                        raise
            
            raise last_exception
        return wrapper
    return decorator


class LLMAPIClient:
    """
    企业级LLM API客户端
    - 自动重试（指数退避）
    - 模型降级（主模型不可用时切换备用模型）
    - 成本追踪
    - 请求日志
    """
    
    def __init__(self, config_manager: APIConfigManager):
        self.config_manager = config_manager
        self.cost_calc = CostCalculator()
        self.logger = logging.getLogger(__name__)
    
    @retry_with_backoff(max_retries=3)
    def chat(self, messages: List[Dict[str, str]],
             model: str = None,
             provider: ModelProvider = None,
             fallback_models: List[str] = None,
             **kwargs) -> LLMResponse:
        """
        发送聊天请求，支持模型降级
        
        Args:
            messages: 对话消息
            model: 首选模型（None则用Provider默认）
            provider: API提供商
            fallback_models: 降级模型列表（按优先级排列）
        """
        provider = provider or ModelProvider.OPENAI
        config = self.config_manager.get(provider)
        model = model or config.default_model
        
        models_to_try = [model] + (fallback_models or [])
        
        for m in models_to_try:
            try:
                self.logger.info(f"调用 {provider.value}/{m}")
                
                # 根据Provider选择客户端
                if provider == ModelProvider.OPENAI:
                    response = self._call_openai(config, messages, m, **kwargs)
                elif provider == ModelProvider.ANTHROPIC:
                    response = self._call_anthropic(config, messages, m, **kwargs)
                else:
                    raise ValueError(f"不支持的Provider: {provider}")
                
                # 记录成本
                self.cost_calc.calculate(
                    m,
                    response.usage.get("prompt_tokens", 0),
                    response.usage.get("completion_tokens", 0),
                )
                
                return response
                
            except RateLimitError:
                if m != models_to_try[-1]:
                    self.logger.warning(f"模型 {m} 速率限制，降级到下一个模型...")
                    continue
                raise
            except Exception as e:
                if m != models_to_try[-1]:
                    self.logger.warning(f"模型 {m} 调用失败({e})，降级...")
                    continue
                raise LLMAPIError(f"所有模型调用失败: {models_to_try}")
    
    def _call_openai(self, config: APIConfig, messages, model, **kwargs):
        """OpenAI调用实现"""
        client = openai.OpenAI(api_key=config.api_key, base_url=config.base_url)
        
        try:
            response = client.chat.completions.create(
                model=model, messages=messages, **kwargs
            )
        except openai.RateLimitError as e:
            raise RateLimitError(str(e)) from e
        except openai.AuthenticationError as e:
            raise InvalidAPIKeyError(f"OpenAI API Key无效: {e}") from e
        
        return LLMResponse(
            content=response.choices[0].message.content,
            model=response.model,
            usage={
                "prompt_tokens": response.usage.prompt_tokens,
                "completion_tokens": response.usage.completion_tokens,
            },
            finish_reason=response.choices[0].finish_reason,
        )
    
    def _call_anthropic(self, config, messages, model, **kwargs):
        """Anthropic调用实现"""
        # ... 类似实现
        pass
```

---

## 6.7 阶段练习

### 练习1：多Provider对比
同一 Prompt 分别调用 OpenAI、Anthropic、DeepSeek，对比响应质量、速度和成本。

### 练习2：API封装库
基于本节的代码，构建一个支持3个以上Provider的统一API调用库。

### 练习3：成本监控Dashboard
写一个脚本，读取调用历史，生成Token使用和成本的图表报告。

---

## 6.8 常见问题

### Q1: 如何选择API Provider？

| 场景 | 推荐 |
|------|------|
| 英语任务，追求质量 | GPT-4o / Claude |
| 中文任务，追求质量 | 通义千问 / GPT-4o |
| 追求性价比 | DeepSeek / GPT-4o-mini |
| 国内部署，合规要求 | 通义千问 / 文心一言 / 豆包 |
| 超长上下文 | Gemini 1.5 Pro / Kimi |

### Q2: API调用失败怎么办？

优先级：重试 → 降级模型 → 返回缓存 → 报错

### Q3: 怎么估算项目API成本？

1. 定义用户场景（每次调用多少Token）
2. 预估调用量（日活 × 每人调用次数）
3. 用CostCalculator计算

---

> **✅ 阶段完成检查清单：**
> - [ ] 能独立调用OpenAI API完成对话
> - [ ] 掌握了temperature/top_p/max_tokens等核心参数
> - [ ] 能计算Token数和API调用成本
> - [ ] API Key使用环境变量管理
> - [ ] 实现了带重试和降级的API客户端
> - [ ] 完成3个阶段练习
>
> **下一步：** [第7步：Prompt Engineering](../07-PromptEngineering/README.md)
