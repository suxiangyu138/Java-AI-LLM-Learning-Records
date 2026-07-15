# Spring Cloud Alibaba 核心知识点

> **定位**：阿里巴巴开源微服务解决方案，在 Netflix 组件停更后成为企业级主流选择。

---

## 一、核心组件

| 组件 | 功能 | 替代 Netflix |
|------|------|------------|
| **Nacos** | 注册中心 + 配置中心 | Eureka + Config |
| **Sentinel** | 限流、熔断、降级 | Hystrix |
| **OpenFeign** | 声明式服务调用 | Ribbon + RestTemplate |
| **Gateway** | API 网关 | Zuul |
| **Seata** | 分布式事务（AT/TCC 模式） | — |
| **RocketMQ** | 消息驱动 | — |

## 二、Nacos 注册与配置中心

| 功能 | 说明 |
|------|------|
| 服务注册 | 启动时注册元数据 + 心跳（默认 5s），自动剔除无心跳实例 |
| 配置管理 | 统一管理 + 动态刷新 + 多环境隔离 |
| 架构 | AP/CP 双模切换 |

## 三、Sentinel 流控与熔断

| 能力 | 说明 |
|------|------|
| 流量控制 | QPS / 线程数 / 响应时间维度 |
| 熔断降级 | 慢调用/异常比例触发 |
| 规则配置 | 控制台动态热更新，支持注解式和代码式 |

## 四、负载均衡策略

| 策略 | 说明 |
|------|------|
| `RoundRobinRule` | 轮询（默认） |
| `RandomRule` | 随机 |
| `WeightedResponseTimeRule` | 响应时间加权 |
| `AvailabilityFilteringRule` | 过滤故障/高并发节点 |
| `ZoneAvoidanceRule` | 区域+可用性综合 |

## 五、版本兼容

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2021.0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

| Spring Boot | Spring Cloud | Alibaba Cloud |
|:----------:|:-----------:|:------------:|
| 2.3.x | Hoxton | 2.2.x |
| 2.7.x | 2021.x | 2021.0.1.0 |
