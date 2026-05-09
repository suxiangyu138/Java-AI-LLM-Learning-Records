# Developer profile

- 开发者：安徽大学 计算机科学与技术 本科生
- 职业方向：Java 后端开发 + AI 大模型应用开发，长期目标是合肥互联网稳定中产
- 常用语言：Java、Python、SQL，优先使用简体中文沟通
- IDE / 工具偏好：强烈偏好 VSCode，不使用 IDEA / PyCharm 等重型 IDE
- 学习目标：通过高质量项目和代码积累，增强简历竞争力和面试实战能力

# Collaboration preferences

- 回答风格：希望 Claude 保持理性、严谨，给出清晰的技术推理和多步方案，不需要废话寒暄
- 代码输出：Java/Python 代码要求可直接运行，结构清晰，默认不加注释，命名规范
- 建议方式：如果有更优实现，请先指出问题，再给出改进版本，并简要说明改进点

# Project overview

- 项目类型：Java Spring Boot 为主的后端服务，可能包含前端（Vue/React）和 AI/RAG 模块
- 核心目标：
  - 作为简历项目和实战练习，突出工程化、可维护性和性能
  - 支持后续扩展为多模块/微服务架构
- 典型技术栈：
  - 后端：Spring Boot、Spring MVC、Spring Security（或其他鉴权机制）
  - 数据库：MySQL + MyBatis / MyBatis-Plus
  - 缓存：Redis
  - 消息队列（可选）：Kafka / RabbitMQ
  - AI 能力：调用大模型 API（如 Claude）、本地/远程 RAG 服务

# Code style & structure

- 分层架构：
  - controller：只处理 HTTP 协议、参数校验和结果包装，不写复杂业务逻辑
  - service：承载业务逻辑、事务边界和组合调用
  - repository / mapper：只负责数据库访问
  - domain / model：领域对象，避免在业务逻辑中直接使用数据库实体
- 命名约定：
  - Java 类名使用大驼峰；方法和变量使用小驼峰
  - Controller 结尾统一使用 `*Controller`，Service 使用 `*Service`/`*ServiceImpl`
  - DTO/VO 明确区分，例如 `UserDTO`、`UserVO`
- 事务与异常：
  - 事务尽量控制在 Service 层；避免在 Controller 里开启事务
  - 统一异常处理：使用全局异常处理器（如 `@ControllerAdvice`），返回统一错误结构
- 日志：
  - 使用 slf4j，统一使用占位符写法：`log.info("xxx {}", value)`
  - 禁止在日志里打印敏感信息（密码、token、密钥）

# Database & performance rules

- MySQL 使用 InnoDB，引擎默认 utf8mb4
- 所有表必备字段：主键 id、创建时间、更新时间，必要时加逻辑删除标记
- 查询规范：
  - 禁止无 where 的全表扫描上线
  - 大分页或导出场景优先考虑游标/流式处理/异步任务
- 索引：
  - 频繁查询条件和关联字段必须建索引
  - 避免在高基数字段上建立冗余索引
- Redis：
  - 读多写少接口可以使用缓存；写操作后要考虑缓存失效策略
  - 注意防止缓存雪崩、击穿、穿透问题

# Security & API design

- 所有对外接口默认需要鉴权，开放接口需要显式标注并重新确认风险
- 参数校验：
  - 使用 Spring Validation 或自定义校验逻辑
  - 对分页参数、ID、关键字段做范围与格式校验
- 防护：
  - 所有数据库操作默认防 SQL 注入
  - 对外错误信息尽量模糊化，不暴露内部实现细节
- API 设计：
  - 使用 RESTful 风格，路径语义清晰，避免动词堆叠
  - 响应结构统一：包含 code / message / data

# Testing & quality

- 单元测试优先覆盖：
  - 复杂业务逻辑
  - 金融/计费/积分相关逻辑
  - 与外部系统交互的关键流程
- 建议：
  - 每次大改动前后，都可以让 Claude 使用 code-review 和安全相关技能做一次全面检查
  - 保存关键重构的前后差异说明，便于之后复盘

# AI & RAG integration

- AI 调用：
  - 尽量封装统一的 AIClient 层，避免在业务代码中到处散落大模型调用
  - 对提示词（prompt）使用配置/文件管理，便于版本控制和调优
- RAG 项目注意事项：
  - 预留向量库/检索服务接口层，不与具体实现强耦合
  - 输入输出结构尽量类型安全，可在 Java 侧建立对应 DTO

# How Claude should help in this project

- 优先任务：
  - 代码审查（bug、安全问题、重构建议）
  - 需求拆解与迭代规划
  - 接口文档和技术文档生成
  - 性能和数据库设计优化建议
- 交互习惯：
  - 当需求不明确时，先提 1~2 个关键澄清问题，而不是直接拍脑袋实现
  - 对于涉及架构或安全的建议，请给出权衡理由，而不是只给代码
