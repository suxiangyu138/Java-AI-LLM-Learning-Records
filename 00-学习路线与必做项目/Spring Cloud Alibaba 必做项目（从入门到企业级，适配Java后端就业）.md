# Spring Cloud Alibaba 实战项目清单

## 一、核心能力

Spring Cloud Alibaba 是 Java 微服务架构核心解决方案，必须掌握：

- 服务注册与发现
- 配置中心
- 远程调用与负载均衡
- 网关路由与限流
- 分布式事务
- 熔断降级
- 链路追踪

---

## 二、技术能力拆解

- 注册层：Nacos 服务注册 / 服务发现 / 心跳检测
- 配置层：Nacos 配置中心 / 动态配置 / 配置共享
- 调用层：OpenFeign 声明式调用 / 负载均衡 / 超时重试
- 网关层：Gateway 路由转发 / 跨域 / 统一鉴权
- 限流层：Sentinel 流量控制 / 熔断降级 / 热点参数限流
- 事务层：Seata AT / TCC / SAGA 分布式事务
- 追踪层：SkyWalking / Sleuth 链路追踪

---

## 三、必做项目（求职优先级）

### 1. 微服务基础商城（极简版）
**定位**：入门必做，吃透核心组件

**技术栈**：SpringBoot + Spring Cloud Alibaba + Nacos + OpenFeign + Gateway

**实现**：
- 用户服务
- 商品服务
- 订单服务
- 网关统一入口
- 服务注册与发现
- 远程调用
- 配置中心

**核心知识点**：
- 微服务业务拆分原则
- Nacos 服务注册与配置管理
- OpenFeign 声明式远程调用
- Gateway 路由转发与跨域处理
- 负载均衡

**产出**：
- 微服务基础能力

**耗时**：5~7天

---

### 2. 网关限流熔断系统
**技术栈**：SpringBoot + Gateway + Sentinel + Nacos

**实现**：
- 接口限流
- 熔断降级
- 热点参数限流
- 自定义限流规则
- 降级策略配置
- Nacos 持久化规则

**核心知识点**：
- 限流 / 熔断 / 降级的区别
- Sentinel 流量控制
- 熔断策略
- 系统自适应保护
- 网关层统一流量管控

**产出**：
- 限流熔断能力

**耗时**：3~4天

---

### 3. 分布式电商订单系统
**定位**：校招核心，企业刚需

**技术栈**：SpringBoot + Spring Cloud Alibaba + Seata + RabbitMQ + Redis + MySQL

**实现**：
- 下单流程
- 库存扣减
- 订单创建
- 支付回调
- 超时关单
- 分布式事务一致性

**核心知识点**：
- Seata AT 模式分布式事务
- 库存超卖问题解决
- 分布式锁（Redisson）
- 延时队列实现超时关单
- 本地消息表
- 可靠消息最终一致性

**产出**：
- 分布式事务与一致性保障能力

**耗时**：7~10天

---

### 4. 分布式权限管理系统
**技术栈**：SpringBoot + Spring Cloud Alibaba + Spring Security + Gateway + Redis

**实现**：
- 多服务统一认证
- JWT 令牌签发
- RBAC 权限模型
- 分布式会话共享
- 接口鉴权
- 网关层鉴权

**核心知识点**：
- 微服务统一认证授权方案
- JWT 令牌签发 / 校验 / 刷新
- 网关层鉴权
- 服务内接口权限控制
- 分布式会话 / Redis 共享存储

**产出**：
- 微服务权限管理能力

**耗时**：5~7天

---

### 5. 秒杀系统
**定位**：简历王牌，高并发核心

**技术栈**：SpringBoot + Spring Cloud Alibaba + Redis + RabbitMQ + Sentinel + Seata

**实现**：
- 商品秒杀
- 库存预热
- 限流防刷
- 异步下单
- 订单削峰
- 超卖防护
- 黑名单机制

**核心知识点**：
- 秒杀架构设计
- 库存预热
- 令牌桶限流
- 接口防刷
- 异步削峰
- 消息队列解耦
- 缓存穿透 / 击穿 / 雪崩解决方案

**产出**：
- 高并发架构设计能力

**耗时**：7~10天

---

### 6. 微服务化 AI 智能问答平台
**定位**：差异化核心，微服务 + AI 融合

**技术栈**：SpringBoot + Spring Cloud Alibaba + Spring AI + Milvus + Vue3 + WebSocket

**实现**：
- 用户中心服务
- 知识库服务
- AI 对话服务
- 网关统一入口
- 限流熔断
- 流式对话
- 会话分布式存储

**核心知识点**：
- AI 能力微服务拆分
- 大模型调用限流
- Token 配额管控
- 流式对话网关转发
- 会话分布式存储

**产出**：
- 微服务 + AI 融合能力

**耗时**：10~15天

---

## 四、进阶项目（提升上限）

### 7. 分布式日志 & 链路追踪平台
**技术栈**：SpringBoot + Spring Cloud Alibaba + SkyWalking + ELK

**实现**：
- 全链路日志采集
- 调用链追踪
- 异常告警
- 日志检索
- 耗时分析

**产出**：
- 分布式追踪与日志分析能力

---

### 8. 全栈智慧商城（前后端分离）
**技术栈**：SpringBoot + Spring Cloud Alibaba + Vue3 + Redis + RabbitMQ + Seata

**实现**：
- 首页
- 购物车
- 订单
- 支付
- 秒杀
- 后台管理
- 数据大屏

**产出**：
- 全栈微服务项目

---

## 五、必练核心知识点

### 1. Nacos 服务注册与配置
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
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

- 微服务拆分合理
- 服务注册与发现
- 配置中心管理
- 远程调用与负载均衡
- 网关统一入口
- 限流熔断降级
- 分布式事务
- 统一异常处理
- 日志追踪

---

## 七、简历表达（核心关键词）

- Spring Cloud Alibaba 微服务架构
- Nacos 服务注册与配置中心
- OpenFeign 声明式远程调用
- Gateway 网关路由与统一鉴权
- Sentinel 限流熔断降级
- Seata 分布式事务一致性
- RabbitMQ 异步削峰解耦
- SkyWalking 分布式链路追踪
- 高并发秒杀系统架构设计
- 微服务 + AI 大模型融合

---

## 八、技术栈推荐

- 微服务：Spring Cloud Alibaba
- 注册中心：Nacos
- 配置中心：Nacos
- 网关：Gateway
- 限流熔断：Sentinel
- 分布式事务：Seata
- 远程调用：OpenFeign
- 消息队列：RabbitMQ
- 缓存：Redis
- 数据库：MySQL
- 链路追踪：SkyWalking
- 日志：ELK
- 部署：Docker / Kubernetes

---

## 九、你的优先级

结合你的 **Java 后端 + AI 大模型应用** 路线，最值得优先完成的是：

1. 微服务基础商城（极简版）
2. 分布式电商订单系统
3. 秒杀系统
4. 微服务化 AI 智能问答平台

---

## 十、练手路线

### 第一阶段：微服务基础
先做：
- 微服务基础商城（极简版）
- 网关限流熔断系统

目标：
- 吃透 Nacos / Feign / Gateway / Sentinel 核心组件

### 第二阶段：分布式能力
再做：
- 分布式电商订单系统
- 分布式权限管理系统

目标：
- 掌握分布式事务 / MQ / 分布式锁

### 第三阶段：高并发架构
再做：
- 秒杀系统
- 分布式日志 & 链路追踪平台

目标：
- 高并发架构设计 / 限流防刷 / 缓存优化

### 第四阶段：差异化竞争力
最后做：
- 微服务化 AI 智能问答平台
- 全栈智慧商城

目标：
- 微服务 + AI 融合 / 全链路闭环

---

## 十一、项目成果要求

- GitHub 完整代码（结构清晰）
- README 写明：项目背景、技术栈、功能模块、架构图、部署步骤
- 项目必须体现：微服务拆分、注册发现、远程调用、网关路由、限流熔断、分布式事务
- 至少 1 个完整业务 Demo
- 能清晰展示微服务架构设计思维
- 项目亮点突出高并发与 AI 融合能力

---