# SpringCloud 实战项目清单

## 一、核心能力

SpringCloud 是 Java 微服务架构核心解决方案，必须掌握：

- 微服务拆分与设计
- 服务注册与发现
- 配置中心管理
- 远程调用与负载均衡
- 网关路由与鉴权
- 熔断限流降级
- 分布式事务
- 链路追踪与可观测性

---

## 二、技术能力拆解

- 拆分层：按业务域拆分 / 父子 Maven 聚合工程
- 注册层：Nacos 服务注册 / 发现 / 健康检查
- 配置层：Nacos 配置中心 / 动态配置 / 多环境隔离
- 调用层：OpenFeign 声明式调用 / 负载均衡 / 超时重试
- 网关层：Gateway 路由 / 断言 / 过滤器 / 鉴权 / 限流
- 容错层：Sentinel 流量控制 / 熔断降级 / 热点限流
- 事务层：Seata AT / TCC 分布式事务
- 认证层：OAuth2 / JWT / 统一认证中心
- 消息层：SpringCloud Stream / RabbitMQ
- 调度层：XXL-Job 分布式定时任务
- 追踪层：SkyWalking / Sleuth + Zipkin
- 部署层：Docker / Docker Compose / 集群高可用

---

## 三、必做项目（求职优先级）

### 1. 单体项目拆分微服务
**定位**：入门必做，建立微服务思维

**技术栈**：SpringBoot + SpringCloud

**实现**：
- 微服务拆分原则：按业务域拆分
- 用户服务
- 订单服务
- 商品服务
- 父子 Maven 多模块聚合工程
- 服务提供者 / 消费者模型

**核心知识点**：
- 微服务拆分原则
- Maven 聚合工程
- 服务间基础调用

**产出**：
- 微服务拆分能力

**耗时**：2天

---

### 2. Nacos 注册中心与配置中心
**技术栈**：SpringCloud Alibaba + Nacos

**实现**：
- 服务注册
- 服务发现
- 健康检查
- 动态配置中心
- 配置热更新
- 命名空间
- 分组
- 多环境隔离

**核心知识点**：
- Nacos 服务注册与发现
- 配置中心管理
- 动态配置热更新
- 多环境隔离

**产出**：
- 注册中心与配置中心能力

**耗时**：2天

---

### 3. OpenFeign 声明式远程调用
**技术栈**：OpenFeign + Nacos

**实现**：
- 接口式远程调用
- 负载均衡
- 超时控制
- 日志打印
- 请求拦截
- 熔断降级前置

**核心知识点**：
- OpenFeign 声明式调用
- 负载均衡
- 超时重试
- 请求拦截

**产出**：
- 远程调用能力

**耗时**：1天

---

### 4. SpringCloud Gateway 网关开发
**技术栈**：SpringCloud Gateway

**实现**：
- 路由配置
- 断言
- 过滤器
- 全局跨域
- 鉴权校验
- 限流
- 动态路由
- 负载均衡

**核心知识点**：
- Gateway 路由配置
- 断言与过滤器
- 统一鉴权
- 流量管控

**产出**：
- 网关开发能力

**耗时**：2~3天

---

### 5. 整合 Sentinel 熔断限流
**技术栈**：SpringCloud Alibaba + Sentinel

**实现**：
- 流量控制
- 熔断降级
- 热点限流
- 系统规则
- 授权规则
- 控制台可视化配置
- 规则持久化

**核心知识点**：
- Sentinel 流量控制
- 熔断降级
- 热点限流
- 系统保护

**产出**：
- 熔断限流能力

**耗时**：2天

---

### 6. 分布式事务 Seata
**技术栈**：Seata + Nacos + MySQL

**实现**：
- AT 模式分布式事务
- TCC 模式原理
- 跨服务数据一致性
- 事务回滚
- 微服务事务协调

**核心知识点**：
- Seata AT / TCC 模式
- 分布式事务协调
- 数据一致性保障

**产出**：
- 分布式事务能力

**耗时**：3天

---

### 7. 分布式登录认证（OAuth2 + JWT）
**技术栈**：SpringSecurity + OAuth2 + JWT

**实现**：
- 统一认证中心
- 令牌发放与校验
- 网关统一鉴权
- 权限控制
- 刷新令牌
- 会话管理

**核心知识点**：
- OAuth2 认证流程
- JWT 令牌机制
- 统一认证中心
- 网关鉴权

**产出**：
- 微服务统一认证能力

**耗时**：3~4天

---

### 8. 微服务整合大模型服务
**定位**：差异化核心，微服务 + AI 融合

**技术栈**：SpringCloud Gateway + Spring AI + Ollama + Nacos

**实现**：
- 大模型服务注册到 Nacos
- 网关路由大模型请求
- 网关限流
- 多模型负载均衡调用
- 统一鉴权

**核心知识点**：
- 大模型微服务化
- 网关路由大模型服务
- 限流与负载均衡
- 多业务模块统一调用

**产出**：
- 企业级大模型微服务能力

**耗时**：3天

---

## 四、进阶项目（提升上限）

### 9. 分布式锁 Redisson
**技术栈**：Redisson + Redis

**实现**：
- 可重入锁
- 公平锁
- 读写锁
- 锁超时
- 自动续期
- 解锁防误删

**产出**：
- 分布式锁能力

---

### 10. 微服务异步通信 RabbitMQ
**技术栈**：SpringCloud Stream + RabbitMQ

**实现**：
- 消息生产者 / 消费者绑定
- 消息重试
- 死信队列
- 消息幂等
- 异步解耦
- 削峰填谷

**产出**：
- 微服务异步通信能力

---

### 11. 分布式定时任务 XXL-Job
**技术栈**：XXL-Job + SpringBoot

**实现**：
- 任务注册
- 动态调度
- 分片广播
- 任务日志
- 失败告警
- 执行监控

**产出**：
- 分布式定时任务能力

---

### 12. 链路追踪与可观测性
**技术栈**：SkyWalking / Sleuth + Zipkin

**实现**：
- 分布式链路追踪
- 调用链可视化
- 接口耗时分析
- 异常定位
- 日志 / 指标 / 链路监控

**产出**：
- 微服务可观测性能力

---

### 13. 微服务容器化与 Docker Compose 部署
**技术栈**：Docker + Docker Compose

**实现**：
- 微服务多阶段构建镜像
- 一键编排所有微服务 / 中间件
- 网络互通
- 数据持久化

**产出**：
- 微服务容器化部署能力

---

### 14. 微服务高可用与集群部署
**技术栈**：Nacos 集群 + Sentinel 集群 + 多实例部署

**实现**：
- 注册中心集群
- 网关集群
- 服务多实例
- 故障自动剔除
- 灰度发布
- 蓝绿部署

**产出**：
- 微服务高可用能力

---

## 五、必练核心知识点

### 1. Nacos 服务注册
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

### 2. OpenFeign 远程调用
```java
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/user/{id}")
    User getUserById(@PathVariable Long id);
}
```

### 3. Gateway 路由配置
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
```

### 4. Sentinel 限流
```java
@SentinelResource(value = "orderCreate", 
    blockHandler = "handleBlock")
public Result createOrder() {
    // 业务逻辑
}
```

### 5. Seata 分布式事务
```java
@GlobalTransactional
public void createOrder() {
    // 扣减库存
    // 创建订单
    // 扣减余额
}
```

---

## 六、项目架构标准

必须覆盖：

- 统一采用 SpringCloud Alibaba 技术栈
- 每个项目覆盖：服务注册 / 调用 / 安全 / 容错 / 可观测
- 代码上传 GitHub
- README 写明：架构图 / 技术栈 / 业务痛点
- 突出微服务拆分 / 分布式事务 / 网关鉴权 / 大模型整合

---

## 七、简历表达（核心关键词）

- SpringCloud Alibaba 微服务架构
- Nacos 服务注册与配置中心
- SpringCloud Gateway 网关路由与鉴权
- OpenFeign 声明式远程调用
- Sentinel 熔断限流降级
- Seata 分布式事务一致性
- OAuth2 + JWT 统一认证中心
- SkyWalking 分布式链路追踪
- Docker Compose 微服务容器化部署
- 微服务整合大模型服务

---

## 八、技术栈推荐

- 核心框架：SpringCloud Alibaba
- 注册 & 配置：Nacos
- 网关：SpringCloud Gateway
- 通信：OpenFeign
- 容错：Sentinel
- 事务：Seata
- 认证：SpringSecurity + OAuth2 + JWT
- 消息：SpringCloud Stream + RabbitMQ
- 调度：XXL-Job
- 追踪：SkyWalking / Sleuth + Zipkin
- 部署：Docker + Docker Compose

---

## 九、你的优先级

结合你的 **Java 后端 + AI 大模型应用** 路线，最值得优先完成的是：

1. Nacos 注册 & 配置中心
2. SpringCloud Gateway 网关
3. OpenFeign 远程调用 + Sentinel 熔断限流
4. Seata 分布式事务
5. OAuth2 统一认证中心
6. 微服务整合大模型服务

---

## 十、练手路线

### 第一阶段：微服务基础
先做：
- 单体项目拆分微服务
- Nacos 注册中心与配置中心
- OpenFeign 声明式远程调用

### 第二阶段：网关与容错
再做：
- SpringCloud Gateway 网关开发
- 整合 Sentinel 熔断限流

### 第三阶段：分布式能力
再做：
- 分布式事务 Seata
- 分布式登录认证（OAuth2 + JWT）

### 第四阶段：差异化竞争力
最后做：
- 微服务整合大模型服务
- 微服务容器化与 Docker Compose 部署
- 链路追踪与可观测性

---

## 十一、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：架构图、技术栈、功能模块、部署步骤、核心难点
- 项目必须体现：微服务拆分、注册发现、远程调用、网关路由、熔断限流、分布式事务
- 至少 1 个完整业务 Demo
- 能清晰展示微服务架构设计思维
- 项目亮点突出高并发与 AI 融合能力

---
