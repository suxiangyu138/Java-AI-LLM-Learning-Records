# SpringBoot 实战项目清单

## 一、核心能力

SpringBoot 是 Java 后端开发核心框架，必须掌握：

- 自动配置与约定优于配置
- MVC 架构与 RESTful 接口
- 持久层整合（MyBatis / MyBatis-Plus）
- 中间件整合（Redis / RabbitMQ / ES）
- 高并发场景解决方案
- AI 大模型应用整合

---

## 二、技术能力拆解

- 基础层：自动配置 / YAML 配置 / 参数校验 / 全局异常
- Web 层：Controller / 统一返回体 / 跨域 / 拦截器
- 持久层：MyBatis-Plus / CRUD / 分页 / 多表联查
- 缓存层：Redis 三大问题 / 分布式锁 / 热点缓存
- 消息层：RabbitMQ 交换机 / 死信队列 / 异步解耦
- 搜索层：Elasticsearch 分词 / 高亮 / 聚合
- 文件层：上传 / 下载 / OSS / 断点续传
- 并发层：限流 / 防刷 / 削峰 / 防超卖
- AI 层：大模型封装 / RAG / 向量检索 / 流式输出

---

## 三、必做项目（求职优先级）

### 1. 个人博客后端
**定位**：入门必做，夯实 SpringBoot 核心基础

**技术栈**：SpringBoot + MyBatis-Plus + MySQL + Knife4j

**实现**：
- 用户登录注册
- 文章发布 / 编辑 / 删除
- 评论功能
- 分类管理
- 标签管理
- 权限控制
- 分页查询
- 条件查询

**核心知识点**：
- 自动配置
- YAML 配置
- 参数校验
- CRUD
- 分页 / 条件查询
- 多表联查
- 全局异常处理
- 统一返回体
- 跨域解决

**产出**：
- SpringBoot 基础能力

**耗时**：5~7天

---

### 2. 前后端分离权限管理系统
**技术栈**：SpringBoot + MyBatis-Plus + JWT + Redis

**实现**：
- 用户管理
- 角色管理
- 菜单管理
- 权限管理
- RBAC 权限模型
- JWT 令牌认证
- 接口权限拦截
- 动态菜单
- 登录状态缓存
- 密码加密

**核心知识点**：
- RBAC 权限模型
- JWT 令牌签发 / 校验 / 刷新
- 接口权限拦截
- 动态菜单
- Redis 缓存登录状态
- 密码加密（BCrypt）

**产出**：
- 企业级权限管理能力

**耗时**：6~8天

---

### 3. Redis 缓存商品详情系统
**技术栈**：SpringBoot + Redis + MySQL

**实现**：
- 商品详情缓存
- 首页热点数据缓存
- 缓存穿透：布隆过滤器 / 缓存空值
- 缓存击穿：互斥锁 / 逻辑过期
- 缓存雪崩：随机过期时间
- 分布式锁防超卖
- 热点数据优化

**核心知识点**：
- 缓存穿透 / 击穿 / 雪崩解决方案
- 分布式锁
- 热点数据缓存
- 缓存更新策略
- Redis 数据结构应用

**产出**：
- 企业级缓存能力

**耗时**：4~5天

---

### 4. RabbitMQ 异步订单通知系统
**技术栈**：SpringBoot + RabbitMQ + MySQL

**实现**：
- 订单下单后发送 MQ
- 异步短信通知
- 异步邮件通知
- 死信队列处理失败消息
- 延迟队列实现订单超时取消
- 消息可靠性保障

**核心知识点**：
- Fanout / Direct / Topic 交换机
- 死信队列
- 消息可靠性
- 异步解耦
- 削峰填谷
- 消息重试

**产出**：
- 异步消息处理能力

**耗时**：4~5天

---

### 5. 高并发秒杀系统
**定位**：简历王牌，高并发核心

**技术栈**：SpringBoot + Redis + RabbitMQ + MySQL

**实现**：
- 秒杀商品预热
- Redis 库存预扣
- 分布式锁防超卖
- 接口限流防刷
- 异步下单
- RabbitMQ 削峰
- 订单异步处理
- 黑名单机制

**核心知识点**：
- 库存预扣
- 限流防刷
- 分布式锁
- 异步削峰
- 事务控制
- 接口防刷

**产出**：
- 高并发秒杀能力

**耗时**：7~10天

---

### 6. 本地大模型 RAG 知识库问答系统
**定位**：差异化核心，Java + AI 融合

**技术栈**：SpringBoot + Spring AI + Ollama + Milvus + PDF 解析

**实现**：
- PDF 文档上传
- 文档解析与分块
- 文本向量化
- 向量检索
- 检索增强生成
- 流式输出
- 会话管理
- 上下文记忆
- 多轮对话

**核心知识点**：
- 大模型接口封装
- RAG 全链路
- 文档向量化
- 向量检索
- 检索增强生成
- 会话管理
- 上下文记忆

**产出**：
- 企业级 RAG 系统

**耗时**：7~10天

---

## 四、进阶项目（提升上限）

### 7. Elasticsearch 全文检索系统
**技术栈**：SpringBoot + Elasticsearch + MySQL

**实现**：
- 商品 / 文章全文检索
- 分词配置
- 高亮查询
- 分页聚合
- 数据同步
- 模糊搜索优化
- 关键词联想
- 搜索记录统计

**产出**：
- 全文搜索能力

---

### 8. 文件上传下载系统
**技术栈**：SpringBoot + 阿里云 OSS / 本地存储

**实现**：
- 文件上传
- 文件下载
- 文件预览
- 文件管理
- 文件大小 / 类型校验
- 断点续传
- 云存储整合
- 访问权限控制

**产出**：
- 文件处理能力

---

### 9. 分布式定时任务平台
**技术栈**：SpringBoot + XXL-Job

**实现**：
- 动态任务配置
- Cron 表达式
- 任务分片
- 失败重试
- 日志监控
- 定时备份
- 定时统计
- 定时推送

**产出**：
- 定时任务管理能力

---

### 10. AI 智能客服后端
**技术栈**：SpringBoot + Ollama + Redis

**实现**：
- 意图识别
- 知识库匹配
- 自动回复
- 人工转接
- 对话记录统计
- 问题智能分类
- 接口限流
- 会话隔离

**产出**：
- 智能客服能力

---

## 五、必练核心知识点

### 1. SpringBoot 自动配置
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2. 统一返回体
```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
}
```

### 3. 全局异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        return Result.error(e.getMessage());
    }
}
```

### 4. 参数校验
```java
@NotNull(message = "用户名不能为空")
@Length(min = 2, max = 20, message = "用户名长度2-20")
private String username;
```

### 5. Redis 缓存
```java
@Cacheable(value = "product", key = "#id")
public Product getById(Long id) {
    return productMapper.selectById(id);
}
```

---

## 六、项目架构标准

必须做到：

- 代码严格分层：Controller / Service / Dao / Entity / Config / Exception
- 统一规范：全局异常处理 / 统一返回体 / 参数校验 / 接口文档
- 所有项目上传 GitHub
- 编写标准 README.md（技术栈 + 功能 + 部署步骤 + 核心难点）
- 遵循阿里巴巴 Java 开发手册
- 代码可维护性高

---

## 七、简历表达（核心关键词）

- SpringBoot 自动配置与快速开发
- MyBatis-Plus 持久层整合
- JWT 令牌认证与 RBAC 权限管理
- Redis 缓存三大问题解决方案
- RabbitMQ 异步解耦与死信队列
- 高并发秒杀系统（库存预扣 / 限流 / 削峰）
- Elasticsearch 全文检索
- SpringBoot + Ollama RAG 知识库问答系统

---

## 八、技术栈推荐

- 框架：SpringBoot
- 持久层：MyBatis-Plus
- 数据库：MySQL
- 缓存：Redis
- 消息队列：RabbitMQ
- 搜索引擎：Elasticsearch
- 文件存储：阿里云 OSS / MinIO
- 接口文档：Knife4j / Swagger
- 定时任务：XXL-Job
- AI 框架：Spring AI
- 大模型：Ollama
- 向量库：Milvus

---

## 九、你的优先级

结合你的 **Java 后端 + AI 大模型应用** 路线，最值得优先完成的是：

1. 个人博客后端
2. 前后端分离权限管理系统
3. Redis 缓存商品详情系统
4. RabbitMQ 异步订单通知系统
5. 高并发秒杀系统
6. RAG 知识库问答系统

---

## 十、练手路线

### 第一阶段：SpringBoot 基础
先做：
- 个人博客后端
- 前后端分离权限管理系统

目标：
- 夯实 SpringBoot 核心基础

### 第二阶段：中间件整合
再做：
- Redis 缓存商品详情系统
- RabbitMQ 异步订单通知系统
- Elasticsearch 全文检索系统

目标：
- 掌握企业级中间件整合

### 第三阶段：高并发场景
再做：
- 高并发秒杀系统
- 分布式定时任务平台

目标：
- 解决高并发问题

### 第四阶段：AI 融合
最后做：
- RAG 知识库问答系统
- AI 智能客服后端

目标：
- 打造 Java + AI 复合竞争力

---

## 十一、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：项目背景、技术栈、功能模块、部署步骤、核心难点
- 项目必须体现：代码分层、统一规范、异常处理、参数校验、接口文档
- 至少 1 个完整业务 Demo
- 能清晰展示 SpringBoot 核心能力
- 项目亮点突出高并发与 AI 融合能力

---
