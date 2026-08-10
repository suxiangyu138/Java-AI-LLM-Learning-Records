# 04 - Doubao API 开发实战

> 🎯 豆包 API 完全兼容 OpenAI 协议 — 改 base_url 即可接入。本章覆盖 API Key 获取、三语言调用（Python/HTTP/Java）、流式输出、Coding Plan 双协议端点与错误排查全流程

---

## 目录

1. [接入前准备：账号与 API Key](#1-接入前准备账号与-api-key)
2. [接入点：OpenAI 兼容三行配置](#2-接入点openai-兼容三行配置)
3. [Python 调用：对话 / 流式 / 思考](#3-python-调用对话--流式--思考)
4. [HTTP 原生调用：Java/Go/.NET 场景](#4-http-原生调用javagonet-场景)
5. [响应结构解读](#5-响应结构解读)
6. [生产增强：重试与超时](#6-生产增强重试与超时)
7. [Coding Plan 双协议端点](#7-coding-plan-双协议端点)
8. [错误排查速查](#8-错误排查速查)
9. [练习](#9-练习)

---

## 1. 接入前准备：账号与 API Key

1. 注册火山引擎账号并完成**实名认证**（未实名时创建按钮灰显，个人实名即可）
2. 进入「火山方舟控制台 → API Key 管理」→ 创建 API Key → 立即复制保存（仅显示一次，丢失需重新创建）
3. 控制台「在线推理」中开通所需模型（不同模型需分别开通）

鉴权方式：HTTP 请求头 `Authorization: Bearer <API Key>`，与 OpenAI 完全一致。两条安全纪律：

- API Key 仅服务端使用，严禁硬编码与前端暴露；泄露立即重置
- API Key 仅能访问当前项目空间的模型，跨项目需切换空间重新创建

**企业级接入**（子账号）三步缺一不可：① API Key 管理中绑定子账号 UID；② 请求头携带 `X-Volc-Security-Uid`；③ IAM 授予对应策略。缺任一步都会 403。

## 2. 接入点：OpenAI 兼容三行配置

豆包标准 API 的接入点：`https://ark.cn-beijing.volces.com/api/v3`。两条铁律：

- **必须显式指定 model**：火山方舟没有默认模型路由，不填 model 返回 404
- **base_url 末尾不要拼路径**：SDK 会自动追加 `/chat/completions`，手拼会 404

```python
# ① 安装官方 OpenAI SDK（pip install openai）
# ② 配置：只改 base_url 和 api_key
from openai import OpenAI

client = OpenAI(
    api_key=os.getenv("ARK_API_KEY"),
    base_url="https://ark.cn-beijing.volces.com/api/v3"
)

# ③ 调用：model 用控制台模型 ID 或 ep- 接入点
response = client.chat.completions.create(
    model="doubao-seed-2-1-pro-260623",
    messages=[{"role": "user", "content": "你好"}]
)
print(response.choices[0].message.content)
```

**Model ID 与接入点（ep-）怎么选**：直接填 Model ID 是最简路径，模型名即调用名，但版本更新后模型 ID 会变，代码要同步改。创建推理接入点（控制台 → 在线推理 → 创建接入点）得到 `ep-2026xxxx-xxxxx` 形式的 ID，业务代码绑定接入点、模型升级在控制台切换，**代码零改动**——这是生产环境的推荐做法，也是 Evolving 持续迭代版（每月 2-4 次更新）的配套姿势。接入点还支持版本回退与负载配置，切换成本完全收在平台侧。

```javascript
// Node.js 同样兼容
import OpenAI from "openai";
const client = new OpenAI({
  apiKey: process.env.ARK_API_KEY,
  baseURL: "https://ark.cn-beijing.volces.com/api/v3"
});
```

## 3. Python 调用：对话 / 流式 / 思考

```python
# 流式对话
response = client.chat.completions.create(
    model="doubao-seed-2-1-pro-260623",
    messages=[{"role": "user", "content": "讲个短笑话"}],
    stream=True
)
for chunk in response:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="")

# 深度思考（非标准参数必须走 extra_body！）
response = client.chat.completions.create(
    model="doubao-seed-2-1-pro-260623",
    messages=[{"role": "user", "content": "解方程 x²+2x-3=0"}],
    extra_body={"thinking": {"type": "enabled"}}   # enabled/disabled/auto
)
msg = response.choices[0].message
print("思考：", msg.reasoning_content)    # 思考过程
print("回答：", msg.content)              # 最终回答
```

thinking 是火山方舟的非标准参数，**写在请求顶层会报错**，必须放 `extra_body`（Python）或等价位置。思考的详细机制见 05 篇。

多项目治理建议在接入时就规划：不同业务线用**不同 API Key**（方舟 Key 支持独立创建与重置），配合控制台项目空间隔离，成本账单按 Key 维度对账；企业多环境（dev/test/prod）建议每环境独立 Key + 独立限额，避免测试流量挤占生产 TPM。

## 4. HTTP 原生调用：Java/Go/.NET 场景

```bash
curl -X POST "https://ark.cn-beijing.volces.com/api/v3/chat/completions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ARK_API_KEY" \
  -d '{
    "model": "doubao-seed-2-1-pro-260623",
    "messages": [{"role": "user", "content": "你好"}],
    "stream": false
  }'
```

响应结构标准：`choices[0].message.content` 是正文，`usage` 含 `prompt_tokens` / `completion_tokens` / `total_tokens`，思考场景另有 `reasoning_tokens`。**Java 生产环境不要手写 HTTP 调用**——豆包要求签名头（X-Content-Sha256/X-Signature），手写极易 401，务必用官方 `volcengine-java-sdk-ark-runtime`（见 09 篇）。

## 5. 响应结构解读

非流式响应是标准 OpenAI 结构，四个字段各有用处：

```json
{
  "id": "chatcmpl-xxx",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "北京今天晴，25 度。",
      "reasoning_content": null,
      "tool_calls": null
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 15,
    "completion_tokens": 12,
    "total_tokens": 27,
    "reasoning_tokens": 0
  }
}
```

- `choices[0].message.content`：最终回答正文，业务逻辑只读这里
- `reasoning_content`：思考内容（开启 thinking 时非空，见 05 篇）
- `tool_calls`：工具调用数组（见 06 篇），无调用时为 null
- `finish_reason`：`stop`（正常结束）/ `length`（达到 max_completion_tokens 截断）/ `tool_calls`（等待工具结果回传）
- `usage.reasoning_tokens`：思考 token 数，成本核算时与 completion_tokens 一并计入输出计费

## 6. 生产增强：重试与超时

- **官方 SDK 内置重试**：openai-python SDK 默认重试 2 次（指数退避），覆盖网络抖动与 429
- **超时三层心智**：连接超时（默认 10s 足够）→ 读取超时（长上下文/思考任务需 60-90s+）→ 总超时（SSE 流式用无界或 120s+，流结束由 finish_reason 判断）
- **重试纪律**：幂等请求（纯问答）可自动重试；非幂等请求（下单、支付类工具执行后）不要盲目重试，防重复扣款
- **429 处理**：观察响应头 `Retry-After`，优先退避重试；连续 3 次失败切换降级模型（见 09 篇）
- **日志纪律**：每调用记录 model、finish_reason、usage 三项，思考任务额外记录 reasoning_tokens——没有 usage 日志就无法做成本复盘（见 08 篇）

## 7. Coding Plan 双协议端点

AI 编程套餐（Coding Plan）走独立端点，按协议分流：

| 端点 | 协议 | 适用工具 |
|------|------|----------|
| `https://ark.cn-beijing.volces.com/api/coding` | Anthropic 协议 | Claude Code、Cline 等 |
| `https://ark.cn-beijing.volces.com/api/coding/v3` | OpenAI 协议 | Cursor、VSCode 扩展、TRAE、Roo Code 等 |

> ⚠️ `/api/v3` 路径对 Coding Plan 已弃用：误用会**无法消耗套餐额度，转而按 API 调用额外计费**。套餐内模型名直接用逻辑名（如 `doubao-seed-2.0-code`、`ark-code-latest`），控制台切换模型 3-5 分钟生效。

## 8. 错误排查速查

| 错误码 | 原因 | 解决方案 |
|--------|------|----------|
| 401 Unauthorized | API Key 错误（误用 Key ID 而非完整密钥） | 核对控制台完整密钥字符串 |
| 404 Not Found | base_url 路径错 / 协议不匹配 / 未填 model | 标准 API 用 `/api/v3`，Coding Plan 用 `/api/coding*`，显式填 model |
| 403 Forbidden | 子账号 UID 未绑定或 IAM 未授权 | 补 X-Volc-Security-Uid 头 + IAM 策略 |
| 429 Too Many Requests | 触发 TPM/RPM 限流 | 指数退避重试，或升级 TPM 保障包 |
| 400 Bad Request | 参数不合法（如 thinking 写在顶层） | 按文档核对请求体，非标准参数走 extra_body |

排查顺序固定：**先环境（base_url/密钥）后代码（model/参数）再策略（限流/权限）**，与通用 API 排错方法论一致。生产环境建议把「连通性探测」做成服务启动自检：启动时发一条最小请求（model 填已开通的轻量模型），失败即告警——把配置错误挡在发布前，而不是等用户请求 404 才发现。两个容易被忽略的隐蔽原因：一是项目空间不对——API Key 与模型不在同一空间时提示权限类错误，检查控制台左上角当前空间；二是模型未开通——新账号即使 Key 正确，未在「在线推理」开通的模型也会报 404，先去控制台确认模型状态。

## 9. 练习

1. 写出豆包标准 API 的 base_url 与两个必填参数
2. thinking 参数为什么必须放 extra_body？写在顶层会发生什么？
3. Coding Plan 误用 `/api/v3` 端点会有什么后果？
4. 企业子账号接入的三个必做步骤是什么？
5. 收到 404 时按什么顺序排查？

---

> 🎯 **核心要点**：豆包 API = **OpenAI 兼容（改 base_url 即用）+ 显式 model（不填 404）+ Bearer 鉴权**。三个最易踩的坑：thinking 走 extra_body、Coding Plan 走专用端点（误用按量计费）、Java 必须用官方 SDK（签名头）。排查顺序：环境 → 代码 → 策略。

**下一模块**：[05-深度思考与推理控制](05-深度思考与推理控制.md) / **返回总览**：[00-总览](00-Doubao知识体系总览.md)
