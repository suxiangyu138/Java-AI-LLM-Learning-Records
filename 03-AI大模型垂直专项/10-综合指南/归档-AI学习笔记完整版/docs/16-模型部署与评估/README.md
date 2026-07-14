# 第16步：模型部署与评估

> **阶段目标：** 掌握AI模型和应用的本地与云部署方案，建立性能监控和成本优化体系  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** FastAPI开发 + 微调基础 + Docker基础  

---

## 📚 目录

- [16.1 部署方案总览](#161-部署方案总览)
- [16.2 本地部署方案](#162-本地部署方案)
- [16.3 Docker容器化部署](#163-docker容器化部署)
- [16.4 云端部署](#164-云端部署)
- [16.5 性能评估指标](#165-性能评估指标)
- [16.6 监控与告警](#166-监控与告警)
- [16.7 成本优化策略](#167-成本优化策略)
- [16.8 阶段练习](#168-阶段练习)

---

## 16.1 部署方案总览

### 16.1.1 部署架构全景

```
部署三个层次：

L1: 调用第三方API
  最简单、最快上线
  例：直接调用OpenAI API，无需部署模型
  
L2: 部署开源模型 + API服务
  自托管开源模型（Qwen/LLaMA）
  通过vLLM/TGI提供OpenAI兼容API
  
L3: 部署微调模型 + 完整AI应用
  自托管微调后的模型
  包含RAG、Agent等完整功能
  企业级监控和运维

选择建议：
┌────────────────┬──────────┬──────────┬──────────┐
│    需求         │  L1      │   L2     │   L3     │
├────────────────┼──────────┼──────────┼──────────┤
│ 快速原型        │  ✅      │  ❌      │  ❌      │
│ 数据隐私要求    │  ❌      │  ✅      │  ✅      │
│ 成本敏感        │  看量    │  ✅      │  ✅      │
│ 需要定制模型    │  ❌      │  ❌      │  ✅      │
│ 高可用SLA      │  ✅      │  自建    │  自建    │
└────────────────┴──────────┴──────────┴──────────┘
```

---

## 16.2 本地部署方案

### 16.2.1 vLLM：高性能推理引擎

```bash
# vLLM — 目前最快的开源LLM推理引擎之一
# 特点：PagedAttention, 连续批处理, 量化支持

# 安装
pip install vllm

# 启动服务
python -m vllm.entrypoints.openai.api_server \
    --model Qwen/Qwen2-7B-Instruct \
    --tensor-parallel-size 1 \       # GPU数量
    --max-model-len 8192 \           # 最大上下文长度
    --gpu-memory-utilization 0.90 \  # GPU显存使用率
    --dtype auto \
    --port 8000

# 调用（OpenAI兼容格式）
# curl http://localhost:8000/v1/chat/completions \
#   -H "Content-Type: application/json" \
#   -d '{"model": "Qwen/Qwen2-7B-Instruct", "messages": [...]}'
```

### 16.2.2 Ollama：最简本地部署

```bash
# Ollama — 最简单的本地模型运行方式
# 官网：https://ollama.com

# 安装后直接运行
ollama run qwen2:7b        # 7B模型
ollama run llama3:8b       # LLaMA 3
ollama run qwen2:0.5b      # 小模型，学习和测试用

# API调用（Ollama默认端口11434）
curl http://localhost:11434/api/chat -d '{
  "model": "qwen2:7b",
  "messages": [{"role": "user", "content": "你好"}],
  "stream": false
}'

# 自定义Modelfile微调
# Modelfile:
# FROM qwen2:7b
# PARAMETER temperature 0.7
# SYSTEM "你是一个专业的Python编程助手"
# ollama create my-assistant -f Modelfile
```

### 16.2.3 Llama.cpp：CPU推理

```python
# Llama.cpp — CPU也能跑大模型的方案
# 通过GGUF量化格式
# pip install llama-cpp-python

from llama_cpp import Llama

# 加载GGUF格式模型
llm = Llama(
    model_path="./qwen2-7b-instruct-q4_k_m.gguf",
    n_ctx=4096,             # 上下文长度
    n_threads=8,            # CPU线程数
    n_gpu_layers=0,         # GPU层数 (0=纯CPU)
    verbose=False,
)

# 生成
response = llm.create_chat_completion(
    messages=[
        {"role": "system", "content": "你是一个AI助手"},
        {"role": "user", "content": "解释什么是docker"},
    ],
    temperature=0.7,
    max_tokens=512,
)

print(response['choices'][0]['message']['content'])
```

### 16.2.4 本地部署对比

```
┌──────────────┬──────────┬──────────┬──────────┬───────────┐
│   方案        │  速度    │  GPU需求  │  易用性  │  适用场景  │
├──────────────┼──────────┼──────────┼──────────┼───────────┤
│ vLLM         │ ★★★★★   │ 需要GPU  │ ★★★     │ 生产环境   │
│ TGI (HF)     │ ★★★★    │ 需要GPU  │ ★★★     │ 生产环境   │
│ Ollama       │ ★★★     │ 可选GPU  │ ★★★★★   │ 本地开发   │
│ Llama.cpp    │ ★★      │ 不必须   │ ★★★★    │ CPU环境    │
│ Transformers │ ★★      │ 需要GPU  │ ★★★     │ 原型验证   │
└──────────────┴──────────┴──────────┴──────────┴───────────┘
```

---

## 16.3 Docker容器化部署

### 16.3.1 Dockerfile

```dockerfile
# Dockerfile — AI应用容器化
FROM python:3.11-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 安装Python依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 创建非root用户
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# 启动服务
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 16.3.2 docker-compose.yml

```yaml
# docker-compose.yml — 完整AI应用栈
version: '3.8'

services:
  # API服务
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - DATABASE_URL=postgresql://user:pass@db:5432/ai_app
      - REDIS_URL=redis://redis:6379
      - CHROMA_HOST=chroma
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./app:/app/app       # 开发时热重载
      - model_cache:/models  # 模型缓存
    networks:
      - ai-network
    restart: unless-stopped

  # 数据库
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
      POSTGRES_DB: ai_app
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U user -d ai_app"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ai-network

  # 缓存
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - ai-network

  # 向量数据库
  chroma:
    image: chromadb/chroma:latest
    ports:
      - "8001:8000"
    volumes:
      - chroma_data:/chroma/chroma
    environment:
      - IS_PERSISTENT=TRUE
      - ANONYMIZED_TELEMETRY=FALSE
    networks:
      - ai-network

  # 模型推理服务（可选：使用vLLM）
  vllm:
    image: vllm/vllm-openai:latest
    ports:
      - "8002:8000"
    volumes:
      - model_cache:/models
    command:
      - "--model"
      - "/models/Qwen2-7B-Instruct"
      - "--max-model-len"
      - "8192"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    networks:
      - ai-network

  # Nginx反向代理
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - api
    networks:
      - ai-network

volumes:
  postgres_data:
  redis_data:
  chroma_data:
  model_cache:

networks:
  ai-network:
    driver: bridge
```

### 16.3.3 Nginx配置

```nginx
# nginx.conf — 生产级反向代理
upstream api_backend {
    server api:8000;
    keepalive 32;
}

server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # 限流
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
    limit_req zone=api_limit burst=20 nodelay;

    # 流式响应必须禁用缓冲
    location /v1/chat/stream {
        proxy_pass http://api_backend;
        proxy_buffering off;           # 关键！
        proxy_cache off;
        proxy_set_header X-Accel-Buffering no;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding on;
    }

    # 普通API
    location / {
        proxy_pass http://api_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_read_timeout 300s;  # LLM调用可能很慢
        proxy_connect_timeout 10s;
    }
}
```

---

## 16.4 云端部署

### 16.4.1 云平台对比

```
┌────────────────┬──────────┬──────────┬──────────┬──────────┐
│    平台         │  模型推理 │  性价比   │  易用性  │  国内可用  │
├────────────────┼──────────┼──────────┼──────────┼──────────┤
│ 阿里 PAI/灵积  │  一站式  │  ★★★★   │  ★★★★   │  ✅      │
│ 华为 ModelArts │  全面    │  ★★★    │  ★★★    │  ✅      │
│ 百度 BML       │  百度系  │  ★★★    │  ★★★    │  ✅      │
│ AWS SageMaker  │  最完善  │  ★★★    │  ★★     │  ❌      │
│ GCP Vertex AI  │  Gemini  │  ★★★★   │  ★★★    │  ❌      │
│ Replicate      │  最简单  │  ★★     │  ★★★★★  │  ❌      │
│ HuggingFace    │  社区化  │  ★★★    │  ★★★★   │  部分    │
└────────────────┴──────────┴──────────┴──────────┴──────────┘
```

### 16.4.2 云部署关键决策

```python
"""
云部署关键决策：

1. 使用托管API还是自部署模型？
   
   托管API（如通义千问API）:
   ✅ 零运维，开箱即用
   ✅ 弹性伸缩
   ❌ 按量付费，大量使用成本高
   ❌ 数据安全顾虑
   
   自部署模型:
   ✅ 数据不外传
   ✅ 完全可控
   ✅ 大量使用时成本更低
   ❌ 需要运维能力
   ❌ 需要GPU资源

2. 实例选择：
   - T4 (16GB): 可跑7B量化模型
   - A10 (24GB): 可跑7B全精度或13B量化
   - A100 (40/80GB): 可跑70B模型
   - H100: 最新最强，但贵且稀缺

3. 弹性伸缩：
   白天高峰: 2-4 GPU实例
   夜间低谷: 1 GPU实例或0 (节省60-80%成本)
"""
```

---

## 16.5 性能评估指标

### 16.5.1 模型质量指标

```python
"""
模型质量评估维度：

1. 自动评估指标:
   - BLEU/ROUGE: 文本生成质量（翻译/摘要）
   - Perplexity: 语言模型困惑度（越低越好）
   - Accuracy/F1: 分类任务

2. LLM-as-Judge:
   用GPT-4等强模型评估弱模型的输出质量
   
3. 人工评估:
   - 有用性 (Helpfulness)
   - 真实性 (Truthfulness)
   - 无害性 (Harmlessness)
   - 遵循指令能力 (Instruction Following)

4. 业务指标:
   - 用户满意度
   - 任务完成率
   - 人工介入率
"""

class ModelEvaluator:
    """模型评估器"""
    
    def __init__(self, judge_model: str = "gpt-4o"):
        self.judge_model = judge_model
    
    def llm_as_judge(self, question: str, answer: str,
                     reference: str = None,
                     criteria: List[str] = None) -> dict:
        """用LLM评估回答质量"""
        if criteria is None:
            criteria = ["准确性", "完整性", "有用性", "语言流畅度"]
        
        criteria_text = "\n".join([f"{i+1}. {c}" for i, c in enumerate(criteria)])
        
        prompt = f"""
请以严格的评审标准评估以下AI回答的质量。

# 问题
{question}

# AI回答
{answer}

# 参考标准答案（如有）
{reference or "无参考答案"}

# 评估维度
{criteria_text}

# 输出格式
请以JSON格式返回评估结果：
{{
    "scores": {{
        "准确性": 1-5分,
        "完整性": 1-5分,
        ...
    }},
    "overall_score": 1-5分,
    "strengths": ["优点1", "优点2"],
    "weaknesses": ["缺点1", "缺点2"],
    "hallucination_detected": true/false,
    "hallucination_details": "如有幻觉，详细说明"
}}
"""
        
        response = client.chat.completions.create(
            model=self.judge_model,
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"},
            temperature=0.1,
        )
        
        return json.loads(response.choices[0].message.content)
    
    def benchmark_suite(self, model, test_cases: List[dict]) -> dict:
        """运行完整评估套件"""
        results = []
        total_score = 0
        
        for case in test_cases:
            # 模型生成回答
            answer = model.generate(case["question"])
            # LLM-as-Judge评估
            eval_result = self.llm_as_judge(
                case["question"], answer, case.get("reference")
            )
            results.append({
                "question": case["question"],
                "answer": answer,
                "evaluation": eval_result,
            })
            total_score += eval_result["overall_score"]
        
        avg_score = total_score / len(test_cases) if test_cases else 0
        
        return {
            "average_score": avg_score,
            "total_cases": len(test_cases),
            "hallucination_rate": sum(
                1 for r in results if r["evaluation"].get("hallucination_detected")
            ) / len(results),
            "details": results,
        }
```

### 16.5.2 性能基准

```python
"""
推理性能关键指标：

1. TTFT (Time To First Token): 首Token延迟
   用户感知: < 500ms: 流畅, 500ms-2s: 可接受, > 2s: 慢
   优化: prompt caching, 预热模型

2. TPS (Tokens Per Second): 生成速度
   阅读速度: 约5-10 tokens/s (人类)
   模型输出: vLLM可达50-100+ tokens/s (7B模型)
   
3. Throughput (吞吐量): 
   QPS (Queries Per Second)
   优化: continuous batching, quantization

4. Memory Usage:
   模型加载: 参数量 × 精度
   推理时: 模型 + KV Cache + 中间激活
"""

# ========== 性能测试 ==========
import time
import asyncio

async def benchmark_inference(endpoint: str, prompt: str, 
                               n_requests: int = 100,
                               concurrency: int = 10) -> dict:
    """推理性能压测"""
    async def single_request():
        start = time.time()
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{endpoint}/v1/chat/completions",
                json={
                    "messages": [{"role": "user", "content": prompt}],
                    "max_tokens": 256,
                },
                timeout=60,
            )
        latency = time.time() - start
        return latency
    
    # 并发测试
    latencies = []
    semaphore = asyncio.Semaphore(concurrency)
    
    async def bounded_request():
        async with semaphore:
            return await single_request()
    
    tasks = [bounded_request() for _ in range(n_requests)]
    latencies = await asyncio.gather(*tasks)
    
    latencies.sort()
    
    return {
        "total_requests": n_requests,
        "concurrency": concurrency,
        "avg_latency": np.mean(latencies),
        "p50_latency": np.percentile(latencies, 50),
        "p95_latency": np.percentile(latencies, 95),
        "p99_latency": np.percentile(latencies, 99),
        "min_latency": np.min(latencies),
        "max_latency": np.max(latencies),
        "throughput": n_requests / sum(latencies) * concurrency,  # QPS
    }
```

---

## 16.6 监控与告警

### 16.6.1 Prometheus + Grafana

```python
# ========== FastAPI集成Prometheus ==========
from prometheus_fastapi_instrumentator import Instrumentator
from prometheus_client import Counter, Histogram, Gauge

# 初始化监控
instrumentator = Instrumentator()
instrumentator.instrument(app).expose(app)

# 自定义指标
llm_request_count = Counter(
    'llm_requests_total', 'LLM请求总数',
    ['model', 'status'],
)

llm_request_latency = Histogram(
    'llm_request_latency_seconds', 'LLM请求延迟',
    ['model'],
    buckets=[0.1, 0.5, 1, 2, 5, 10, 30, 60],
)

llm_token_usage = Counter(
    'llm_tokens_total', 'LLM Token使用量',
    ['model', 'type'],  # type: prompt/completion
)

active_streams = Gauge(
    'active_streams', '当前活跃流数量',
)

# ========== 在API中使用 ==========
@app.post("/v1/chat/completions")
async def chat(request: ChatRequest):
    start_time = time.time()
    
    try:
        response = await process_chat(request)
        
        # 记录指标
        llm_request_count.labels(
            model=request.model, status="success"
        ).inc()
        llm_request_latency.labels(
            model=request.model
        ).observe(time.time() - start_time)
        llm_token_usage.labels(
            model=request.model, type="prompt"
        ).inc(response.usage.prompt_tokens)
        llm_token_usage.labels(
            model=request.model, type="completion"
        ).inc(response.usage.completion_tokens)
        
        return response
    except Exception:
        llm_request_count.labels(
            model=request.model, status="error"
        ).inc()
        raise
```

### 16.6.2 告警规则

```yaml
# prometheus-alerts.yml
groups:
  - name: ai_service_alerts
    rules:
      # API错误率过高
      - alert: HighErrorRate
        expr: rate(llm_requests_total{status="error"}[5m]) / rate(llm_requests_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "AI服务错误率超过5%"
          description: "过去5分钟错误率 {{ $value | humanizePercentage }}"

      # API延迟过高
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(llm_request_latency_seconds_bucket[5m])) > 30
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P95延迟超过30秒"

      # Token用量异常
      - alert: TokenSpike
        expr: rate(llm_tokens_total[1h]) > 100000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Token使用量异常增长"

      # 服务宕机
      - alert: ServiceDown
        expr: up{job="ai-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "AI服务不可用"
```

---

## 16.7 成本优化策略

```python
"""
AI应用成本优化完整方案：

1. 模型选择优化
   ├── 任务路由: 简单任务→小模型, 复杂任务→大模型
   ├── 模型量化: FP32 → FP16 → INT8 → INT4
   └── 模型蒸馏: 用大模型训练小模型

2. 推理优化
   ├── Prompt Caching: 重复的System Prompt只计算一次
   ├── Speculative Decoding: 小模型快速生成候选，大模型验证
   ├── Continuous Batching: 动态批处理提高GPU利用率
   └── KV Cache优化: 复用已计算的KV Cache

3. API调用优化
   ├── 语义缓存: 相似问题返回缓存答案
   ├── Token优化: 精简Prompt，减少不必要的Token
   └── 输出控制: 限制max_tokens，避免生成过长

4. 架构优化
   ├── 冷热分层: 热数据用Redis，冷数据用S3
   ├── 自动扩缩: 根据QPS自动调整实例数
   └── Spot实例: 使用竞价实例（可节省60-90%）
"""

class CostOptimizer:
    """成本优化器"""
    
    def __init__(self, cost_tracker):
        self.tracker = cost_tracker
        self.cache = AICache(redis_client)
    
    def select_model(self, task: str, complexity: str = "auto") -> str:
        """智能模型选择"""
        if complexity == "auto":
            complexity = self._estimate_complexity(task)
        
        model_map = {
            "simple": "gpt-4o-mini",    # $0.15/1M input
            "medium": "gpt-4o",         # $5/1M input
            "complex": "gpt-4o",        # $5/1M input
        }
        
        return model_map.get(complexity, "gpt-4o-mini")
    
    def optimize_prompt(self, prompt: str) -> str:
        """Prompt Token优化"""
        # 去除多余空白
        prompt = ' '.join(prompt.split())
        # 缩短不必要的描述
        # 移除多余的示例
        return prompt
    
    def cache_check(self, prompt: str, model: str) -> Optional[str]:
        """检查语义缓存"""
        # 用Embedding相似度找缓存回答
        cached = self.cache.cache_llm_response(prompt, model, temperature=0)
        if cached:
            self.tracker.record_cache_hit()
            return cached
        return None
    
    def _estimate_complexity(self, task: str) -> str:
        """估算任务复杂度"""
        simple_indicators = ["是什么", "翻译", "分类", "总结", "提取"]
        complex_indicators = ["分析", "设计", "实现", "优化", "对比", "推理"]
        
        task_lower = task.lower()
        
        complex_count = sum(1 for w in complex_indicators if w in task_lower)
        
        if complex_count >= 2 or "代码" in task_lower:
            return "complex"
        elif complex_count == 1:
            return "medium"
        else:
            return "simple"
```

---

## 16.8 阶段练习

### 练习1：Docker部署
将自己的AI应用打包成Docker镜像，使用docker-compose启动完整服务栈。

### 练习2：性能压测
用locust或自定义脚本对你的AI服务进行压测，找出性能瓶颈。

### 练习3：监控Dashboard
搭建Prometheus+Grafana，为AI服务创建监控面板和告警规则。

---

> **✅ 阶段完成检查清单：**
> - [ ] 能用Docker容器化部署AI应用
> - [ ] 了解vLLM/Ollama等本地推理方案
> - [ ] 理解模型评估的核心指标
> - [ ] 搭建了监控和告警体系
> - [ ] 掌握了成本优化的核心策略
> - [ ] 完成3个阶段练习
>
> **下一步：** [第17步：综合AI大模型项目落地](../17-综合AI大模型项目落地/README.md)
