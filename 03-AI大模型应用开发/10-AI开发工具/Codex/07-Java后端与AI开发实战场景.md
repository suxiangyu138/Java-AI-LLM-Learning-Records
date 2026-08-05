# 07 - Java 后端与 AI 开发实战场景

> 🎯 Codex 在 Java 后端 + AI 大模型开发中的五大落地场景——从 CRUD 到微服务、从训练脚本到运维自动化

---

## 1. 后端开发（Java 核心场景）

### CRUD 接口全自动生成

```text
需求：
"在现有 Spring Boot 3 项目中添加 Product 实体，
生成完整的 CRUD 接口，包含：
- Entity + DTO + Mapper(MyBatis Plus) + Service + Controller
- 分页查询 + 条件筛选
- 参数校验(@Valid)
- Swagger 文档注解
- 单元测试覆盖
风格参考项目中已有的 User 模块。"

Codex 产出：
  Product.java (Entity)
  ProductDTO.java
  ProductMapper.java
  ProductService.java (含分页+条件查询)
  ProductController.java (RESTful)
  ProductServiceTest.java (含 Mock)
```

### 微服务配置

```text
需求：
"生成 Spring Cloud Gateway 路由配置：
- /api/users/** → user-service
- /api/orders/** → order-service
- 添加 JWT 鉴权过滤器
- 限流配置（令牌桶，每秒 100 请求）"
```

### 数据库代码

```text
需求：
"根据以下建表语句生成 MyBatis Plus 完整代码：
CREATE TABLE orders (...)
包含：Entity + Mapper + 复杂查询（多表关联 + 聚合）"
```

---

## 2. AI 大模型工程（Python 为主）

```text
需求：
"用 FastAPI 封装 vLLM 推理服务：
- /chat 端点（OpenAI 兼容格式）
- /chat/stream 流式端点（SSE）
- 请求队列 + 并发控制（max 10）
- 健康检查 + Prometheus metrics"

Codex 产出：
  完整的 FastAPI 服务代码 + Dockerfile + docker-compose.yml
```

```text
需求：
"为 Qwen2-7B 的 LoRA 微调写完整的训练脚本：
- 数据集处理（Alpaca 格式 → tokenize）
- LoRA 配置（r=8, alpha=16）
- Trainer + DeepSpeed ZeRO-2"
```

---

## 3. 运维自动化

```text
需求：
"写一个 Python 脚本，监控 GPU 使用率：
- 每 30 秒检查一次
- GPU 利用率 > 90% → 企微告警
- GPU 显存 > 95% → 企微告警 + 邮件
- 生成每日 GPU 使用报告（CSV）"
```

---

## 4. 代码维护

```text
需求：
"批量重构：将项目中所有 Date 类型改为 LocalDateTime。
涉及：Entity / DTO / Service 的参数和返回值。
注意：MyBatis XML 的日期格式化也要同步修改。"
```

---

## 5. 编程学习（Java 后端视角）

```text
需求：
"逐行解释 Spring Boot 自动配置原理的源码：
- @SpringBootApplication → @EnableAutoConfiguration → AutoConfigurationImportSelector
- 每一步做了什么、为什么这样设计
- 输出格式：源码 → 解释 → 设计意图"
```

---

## 6. 使用渠道选择

| 场景 | 推荐渠道 | 原因 |
|------|----------|------|
| 日常开发 | ChatGPT 内置 Codex | 对话灵活、结合多模态 |
| 批量重构 | Codex CLI | 本地 Git 集成、免费 |
| 零环境开发 | Codex Web | 云端沙箱、不需本地配置 |
| 企业集成 | API | 嵌入 CI/CD 流水线 |
