# 06 Proxy 部署与配置
> 网关的骨架：config.yaml 全解、起服务、虚拟 key、鉴权与团队管理

## 📚 目录
1. [Proxy 是什么：一个 OpenAI 兼容端点](#1-proxy-是什么一个-openai-兼容端点)
2. [config.yaml 配置全解](#2-configyaml-配置全解)
3. [启动与验证](#3-启动与验证)
4. [虚拟 Key：网关的权限核心](#4-虚拟-key网关的权限核心)
5. [鉴权与安全基线](#5-鉴权与安全基线)
6. [团队与多租户](#6-团队与多租户)
7. [管理 API 与 UI](#7-管理-api-与-ui)
8. [常见坑](#8-常见坑)
9. [核心要点](#9-核心要点)

---

## 1. Proxy 是什么：一个 OpenAI 兼容端点

```text
部署形态：
  应用 A ──┐
  应用 B ──┼──→ http://litellm-proxy:4000/v1  （OpenAI 兼容）
  应用 C ──┘        │
                    ├── OpenAI（真实 key 只在网关）
                    ├── Claude
                    ├── DeepSeek
                    └── 本地 vLLM

调用方视角：就是一个 OpenAI 服务（换 base_url 即可）
网关视角：路由/预算/审计/限流全在这
```

| Proxy 解决的 | 说明 |
|------------|------|
| key 集中 | 真实 key 只在网关，调用方拿虚拟 key |
| 语言无关 | OpenAI 兼容 → 任何语言 SDK 都能调 |
| 治理集中 | 预算/限流/审计/模型白名单一处配置 |
| 切换无感 | 换供应商 = 改网关配置 |

> 🎯 **核心要点**：**Proxy = "把 LiteLLM 变成服务"**——调用方只看到 OpenAI 兼容端点（换 base_url 即可），所有治理（key/预算/路由）收进网关。**虚拟 key 是核心设计**：真实 key 永不出网关。

## 2. config.yaml 配置全解

```yaml
# config.yaml —— Proxy 配置（v1.94）
model_list:                                  # ① 模型注册表（03 章）
  - model_name: pro-llm
    litellm_params: {model: gpt-4o}
  - model_name: pro-llm
    litellm_params: {model: anthropic/claude-sonnet-5}
  - model_name: cheap-llm
    litellm_params: {model: deepseek/deepseek-v4-flash}

litellm_settings:                            # ② 运行时设置
  drop_params: true                          # 忽略不支持的参数
  num_retries: 2
  request_timeout: 30
  max_budget: 1000                           # 全局预算（05 章）

general_settings:                            # ③ 安全与集成
  master_key: sk-master-xxx                  # ⚠️ 主 key（管理 API 用，生产环境变量）
  database_url: postgresql://...             # 数据库（记账/key 持久化）
  otel: true                                 # OpenTelemetry 集成

router_settings:                             # ④ 路由（04 章）
  routing_strategy: simple-shuffle
  allowed_fails: 3
  cooldown_time: 60

guardrails:                                  # ⑤ 内容安全（可选）
  - guardrail_name: input_moderation
    litellm_params: {guardrail: hide-secrets, mode: pre_call}
```

| 配置块 | 管什么 |
|--------|--------|
| `model_list` | 模型注册表（03 章） |
| `litellm_settings` | 运行时行为（重试/超时/预算） |
| `general_settings` | 主 key/数据库/可观测 |
| `router_settings` | 路由策略（04 章） |
| `guardrails` | 内容安全策略（输入输出） |

> ⚠️ 安全底线：**master_key 必须设置**（未设置 = 任何人都能调管理 API）；生产用环境变量（`LITELLM_MASTER_KEY`）而非明文 yaml（FC 体系 06 章 key 基线）。

## 3. 启动与验证

```bash
# 安装
pip install 'litellm[proxy]'

# 启动（读 config.yaml，默认 4000 端口）
litellm --config config.yaml --port 4000
# 或代码方式
# python -m litellm --config config.yaml

# 验证：OpenAI 兼容健康检查
curl http://localhost:4000/health/liveliness

# 调用（OpenAI SDK 直连）
# OpenAI SDK 换 base_url + 虚拟 key
```

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:4000/v1",   # ⭐ 唯一改动
    api_key="sk-virtual-key-xxx",           # 虚拟 key（不是真实 key）
)

resp = client.chat.completions.create(
    model="pro-llm",                        # 逻辑名（03 章）
    messages=[{"role": "user", "content": "你好"}],
)
```

> 🎯 **核心要点**：**调用方接入 Proxy = 改两个配置**（base_url + api_key）——任何语言的 OpenAI SDK 都能接。模型名用逻辑名（model_list 里定义的），调用方不需要知道物理模型。

## 4. 虚拟 Key：网关的权限核心

```bash
# 创建虚拟 key（管理 API）：限定模型 + 预算 + 速率
curl -X POST http://localhost:4000/key/generate \
  -H "Authorization: Bearer sk-master-xxx" \
  -d '{
    "models": ["pro-llm", "cheap-llm"],     # 可用模型白名单
    "budget": 50,                            # 预算（05 章）
    "budget_duration": "30d",                # 周期
    "max_parallel_requests": 10,             # 并发上限
    "tpm_limit": 100000,                     # 每分钟 token
    "rpm_limit": 60,                         # 每分钟请求
    "team_id": "team_cs"                     # 归属团队
  }'
# → {"key": "sk-virtual-abc...", ...}
```

| 虚拟 key 能力 | 防什么 |
|--------------|--------|
| 模型白名单 | 调用方只能用授权的模型 |
| 预算 | 超支自动拒 |
| TPM/RPM | 突发/滥用 |
| 并发上限 | 拖垮网关 |
| 团队归属 | 成本归集 |
| 可撤销 | 泄露即删 |

> 💡 **虚拟 key = 权限对象**：给每个调用方（应用/团队/用户）发独立 key——**每个 key 都是"受限身份"**，泄露一个只影响一个 key 的额度，可立即吊销。这是网关治理的第一能力。

## 5. 鉴权与安全基线

| 安全项 | 配置/做法 | 级别 |
|--------|----------|:---:|
| master_key | 必须设置（环境变量） | 必须 |
| 虚拟 key | 调用方一律用虚拟 key | 必须 |
| HTTPS | 反代 TLS（Nginx） | 必须 |
| 网络隔离 | 网关内网，不暴露公网 | 必须 |
| SSO/SAML | 管理面企业登录（Enterprise） | 进阶 |
| Guardrails | 输入输出内容策略 | 进阶 |
| 审计日志 | 管理操作全记录（Enterprise） | 进阶 |
| 密钥轮换 | 真实 key 定期轮换 | 运维 |

> ⚠️ 安全红线：**网关不暴露公网**（与 Scrapyd 6800 端口同理）——内网 + 反代 + 虚拟 key 三层；管理 API 只有 master key 能调。

## 6. 团队与多租户

```text
多团队治理模型：
  团队（Team）→ 多个用户（User）→ 多个虚拟 key
  ├── 团队预算（客服组 300 元/月）
  ├── 用户预算（个人额度）
  ├── 模型白名单（按团队）
  └── 成本归集（Spend Logs 按团队看）

企业版（Enterprise）额外：
  SSO/SAML 单点登录
  审计日志（管理操作）
  项目（projects）管理
  Guardrails 集中策略
```

```bash
# 团队管理 API（Enterprise/企业场景）
curl -X POST http://localhost:4000/team/new \
  -H "Authorization: Bearer sk-master-xxx" \
  -d '{"team_alias": "客服组", "max_budget": 300, "models": ["pro-llm"]}'
```

> 🎯 **核心要点**：**团队 = 预算与权限的容器**——"客服组 300 元/月、只能用 pro-llm、超支拒绝"用三个配置表达。多团队场景的治理模型：**全局预算 > 团队预算 > 用户预算 > key 预算**（05 章三级预算的完整形态）。

## 7. 管理 API 与 UI

```text
管理面（master key 鉴权）：
  /health         健康检查（liveliness/readiness）
  /model/info     模型列表与健康状态
  /key/generate   创建虚拟 key
  /key/delete     吊销
  /key/info       查询 key 详情
  /spend/logs     花费明细
  /team/*         团队管理
  /user/*         用户管理

UI（v1.94 Shared DataTable 迁移后）：
  Virtual Keys / Teams / Spend Logs / Guardrails 全部有管理界面
```

```bash
# 常用运维命令
curl http://localhost:4000/health/readiness      # 就绪
curl http://localhost:4000/model/info -H "Authorization: Bearer sk-master-xxx"   # 模型状态
curl "http://localhost:4000/spend/logs?start_date=2026-08-01" -H "Authorization: Bearer sk-master-xxx"
```

> 💡 管理 API = "**网关的运维接口**"——key 生命周期、模型健康、花费查询全在这里。生产脚本（创建 key/查花费/吊销）都用 API 自动化（07 章 CI 集成）。

## 8. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | master_key 未设 | 管理面裸奔 | 必须设置（环境变量） |
| 2 | 无数据库 | 重启丢 key/记录 | 配 Postgres（07 章） |
| 3 | 调用方拿真实 key | 泄密面大 | 一律虚拟 key |
| 4 | 网关公网暴露 | 被滥用/攻击 | 内网 + 反代 |
| 5 | drop_params 没开 | 参数报错 | true（各家参数差异） |
| 6 | 逻辑名拼错 | 404 模型不存在 | model_list 对照 |
| 7 | 版本升级配置不兼容 | 启动失败 | 锁定版本 + 迁移文档 |
| 8 | 预算设太紧 | 业务被拒 | 余量 + 告警先行 |

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Proxy = OpenAI 兼容端点，调用方只改 base_url + api_key |
| 2 | config.yaml 五块：模型/运行时/安全/路由/guardrails |
| 3 | master_key 必须设置；虚拟 key 是权限核心 |
| 4 | 虚拟 key：模型白名单 + 预算 + TPM/RPM + 可撤销 |
| 5 | 安全红线：内网 + 反代 + 虚拟 key + 密钥轮换 |
| 6 | 团队 = 预算与权限容器（全局 > 团队 > 用户 > key） |
| 7 | 管理 API：key 生命周期/模型健康/花费查询全自动化 |
| 8 | 八大坑：主 key/数据库/真实 key/公网/版本 |

---

**下一模块**：[07-Proxy 生产实践](07-Proxy生产实践.md) / **返回总览**：[00-LiteLLM知识体系总览](00-LiteLLM知识体系总览.md)

## 参考来源

- [LiteLLM 官方：Proxy 快速开始](https://docs.litellm.ai/docs/proxy/quick_start)
- [LiteLLM 官方：config.yaml 参考](https://docs.litellm.ai/docs/proxy/configs)
- [LiteLLM 官方：虚拟 key](https://docs.litellm.ai/docs/proxy/virtual_keys)
- [LiteLLM 官方：管理 API](https://docs.litellm.ai/docs/proxy/virtual_keys#manage-virtual-keys)
