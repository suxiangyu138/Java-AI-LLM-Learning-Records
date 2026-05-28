## CC Switch 配置 Qwen 速查手册

### 核心步骤（3步到位）
1. 打开 CC Switch → 点 **+ (Add Provider)** 添加提供者
2. 按下方配置表填写字段（选百炼或本地）
3. 点**启用** → 终端运行 `claude` 测试

***

### 方案A：阿里云百炼（联网/按量计费）

```
Provider Name: 阿里云百炼
API Type: Anthropic Compatible
Base URL: https://dashscope.aliyuncs.com/api/v2/apps/claude-code-proxy
API Key: sk-xxxxx（去百炼控制台创建）
Model: qwen-plus / qwen-turbo / qwen-max
```

**获取 Key**：https://dashscope.console.aliyun.com/ → API-KEY 管理 → 创建并复制 [github](https://github.com/farion1231/cc-switch/issues/861)

***

### 方案B：本地 Ollama（离线/免费）

```bash
# 前置操作
ollama pull qwen:7b
ollama serve
```

```
Provider Name: Qwen-Ollama
API Type: OpenAI Compatible
Base URL: http://localhost:11434/v1
API Key: ollama（随便填）
Model: qwen:7b（ollama list 查看）
```


***

### 报错速查

| 错误 | 解决 |
|------|------|
| `401 Unauthorized` | 百炼 Key 错了，重新生成 |
| `Connection refused` | Ollama 没启动，运行 `ollama serve` |
| 代理报错 | 关代理或排除 localhost/百炼域名 |
| `MAX_THINKING_TOKENS` 报错 | 高级配置删该字段或设为 0 |

 [github](https://github.com/farion1231/cc-switch/issues/861)

***

### 选择建议
- **求稳定+最新能力** → 百炼（有免费额度）
- **求隐私+零成本** → Ollama（需本地算力）
- **两个都配** → CC Switch 一键切换
