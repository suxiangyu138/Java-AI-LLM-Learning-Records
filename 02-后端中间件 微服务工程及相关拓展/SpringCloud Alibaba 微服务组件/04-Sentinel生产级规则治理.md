# 04 - Sentinel 生产级规则治理

> Sentinel Dashboard 的规则在重启后会丢失——这不是 Bug，是设计。规则持久化、集群流控、网关流控，才是 Sentinel 生产化的三道必过的坎。

---

## 📚 目录

1. [规则持久化：三种方案对比](#1-规则持久化三种方案对比)
2. [Nacos 持久化完整实现](#2-nacos-持久化完整实现)
3. [集群流控架构与部署](#3-集群流控架构与部署)
4. [Gateway 网关流控集成](#4-gateway-网关流控集成)
5. [监控埋点与 Prometheus 集成](#5-监控埋点与-prometheus-集成)
6. [规则配置最佳实践](#6-规则配置最佳实践)
7. [生产环境 Checklist](#7-生产环境-checklist)

---

## 1. 规则持久化：三种方案对比

### 1.1 为什么需要持久化

```text
Sentinel 默认的规则存储：JVM 内存
  → Dashboard 推送规则 → 写入应用内存
  → 应用重启 → 💨 规则消失！

生产环境必须持久化，否则：
  → 每次重启都要重新配置规则
  → 规则变更没有审计记录
  → 多实例无法共享同一套规则
```

### 1.2 三种方案

```text
┌──────────────┬───────────────┬───────────────┬──────────────┐
│    方案       │   Nacos 持久化 │  本地文件      │  Apollo      │
├──────────────┼───────────────┼───────────────┼──────────────┤
│ 配置方式      │ Nacos 控制台  │ 本地 YAML     │ Apollo 控制台│
│ 动态修改      │ ✅ 实时       │ ❌ 需重启     │ ✅ 实时      │
│ Dashboard 写  │ ✅ 支持       │ ❌ 不支持     │ ⚠️ 需改造    │
│ 多实例共享    │ ✅            │ ❌ 各自独立   │ ✅           │
│ 审计日志      │ ✅ Nacos 记录 │ ❌            │ ✅           │
│ 推荐场景      │ ⭐ 生产首选    │ 开发/简单场景 │ 已有 Apollo  │
└──────────────┴───────────────┴───────────────┴──────────────┘
```

---

## 2. Nacos 持久化完整实现

### 2.1 架构图

```text
实现模式：PUSH 模式

  ┌─────────────────┐
  │ Sentinel         │  1. 管理员在 Dashboard 创建/修改规则
  │ Dashboard        │  2. Dashboard 将规则推送到 Nacos
  └────────┬────────┘
           │ PUSH
  ┌────────▼────────┐
  │      Nacos       │  3. Nacos 持久化存储规则
  │  (配置中心)      │  4. 应用监听 Nacos 配置变更
  └────────┬────────┘
           │ 长轮询
  ┌────────▼────────┐
  │  应用实例 A      │  5. 配置变更 → 更新应用内存中的规则
  │  应用实例 B      │     所有实例同步更新
  │  应用实例 C      │
  └─────────────────┘
```

### 2.2 依赖配置

```xml
<!-- Sentinel + Nacos 持久化 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

### 2.3 应用端配置

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: sentinel-dashboard:8080
        port: 8719  # 与 Dashboard 通信的端口

      # ===== 规则持久化到 Nacos =====
      datasource:
        # 流控规则
        flow:
          nacos:
            server-addr: nacos:8848
            namespace: ${NACOS_NAMESPACE}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-flow-rules
            data-type: json
            rule-type: flow

        # 熔断规则
        degrade:
          nacos:
            server-addr: nacos:8848
            namespace: ${NACOS_NAMESPACE}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-degrade-rules
            data-type: json
            rule-type: degrade

        # 系统规则
        system:
          nacos:
            server-addr: nacos:8848
            namespace: ${NACOS_NAMESPACE}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-system-rules
            data-type: json
            rule-type: system

        # 授权规则
        authority:
          nacos:
            server-addr: nacos:8848
            namespace: ${NACOS_NAMESPACE}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-authority-rules
            data-type: json
            rule-type: authority
```

### 2.4 Nacos 中的规则 JSON 格式

```json
// order-service-flow-rules（在 Nacos 中配置）
[
  {
    "resource": "createOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "getOrder",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

```text
字段说明：
  resource         → 资源名（@SentinelResource 的值或 URL）
  grade            → 0=线程数, 1=QPS
  count            → 阈值
  strategy         → 0=直接, 1=关联, 2=链路
  controlBehavior  → 0=快速失败, 1=WarmUp, 2=排队等待
  clusterMode      → 是否集群流控
```

### 2.5 Dashboard 改造（让 Dashboard 写 Nacos）

```text
问题：Sentinel Dashboard 默认只写内存，需要改造才能写 Nacos

改造步骤：
1. 下载 Sentinel Dashboard 源码
2. 修改 Rule 的 Provider 和 Publisher
   将 InMemoryRuleRepository 替换为 NacosRuleRepository
3. 在 Dashboard 的 application.yml 中配置 Nacos 地址
4. 重新打包部署

或者使用社区改造版：
  → github.com/alibaba/Sentinel/issues/1213
  → 有社区 fork 已经集成了 Nacos 写入
```

---

## 3. 集群流控架构与部署

### 3.1 为什么需要集群流控

```text
单机流控的问题：
  → 实例 A 限流 100 QPS，实例 B 限流 100 QPS
  → 实际总 QPS 可能到 200
  → 但下游服务只能承受 150 QPS！

集群流控：
  → 所有实例共享一个 QPS 阈值 150
  → Token Server 统一分配令牌
  → 任何实例超过总限额 → 被限制
```

### 3.2 两种集群模式

```text
模式 1：独立 Token Server（推荐生产）
┌──────────────┐
│ Token Server │  ← 独立部署，专门管理令牌
│ (单点/集群)  │
└──────┬───────┘
  ┌────┼────┐
  ▼    ▼    ▼
┌───┐┌───┐┌───┐
│App││App││App│  ← 流控 Client，向 Token Server 申请令牌
└───┘└───┘└───┘

模式 2：嵌入式 Token Server（简单但不可靠）
  某一个应用实例被选为 Token Server
  问题：Token Server 宕机 → 全集群流控失效
```

### 3.3 配置示例

```yaml
spring:
  cloud:
    sentinel:
      # ===== 集群流控：Client 端配置 =====
      datasource:
        flow:
          nacos:
            ...
            rule-type: flow
      # 启用集群流控
      cluster:
        flow:
          client:
            server-host: token-server-1  # Token Server 地址
            server-port: 18730
            request-timeout: 3000
```

```json
// 集群流控规则（Nacos 中配置）
[
  {
    "resource": "createOrder",
    "grade": 1,
    "count": 150,           // 集群总 QPS 限额
    "clusterMode": true,     // ← 开启集群模式
    "clusterConfig": {
      "flowId": 1001,
      "thresholdType": 1,    // 1=全局阈值
      "fallbackToLocalWhenFail": true,  // 集群流控失败 → 降级单机流控
      "sampleCount": 10,
      "windowIntervalMs": 1000
    }
  }
]
```

---

## 4. Gateway 网关流控集成

### 4.1 两种集成方式

```text
方式 1：Sentinel Gateway Adapter（推荐）
  在网关层直接拦截，不进入后端服务
  依赖：spring-cloud-alibaba-sentinel-gateway

方式 2：后端服务独立 Sentinel
  网关不拦截，各服务自己限流
  缺点：流量已到达后端才拦截
```

### 4.2 Gateway 集成配置

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    sentinel:
      # Gateway 专属配置
      scg:
        fallback:
          mode: response   # 限流后的响应模式
          response-status: 429
          response-body: '{"code": 429, "message": "Too Many Requests"}'

      # Gateway 路由粒度流控
      datasource:
        gw-flow:
          nacos:
            server-addr: nacos:8848
            data-id: gateway-flow-rules
            group-id: SENTINEL_GROUP
            rule-type: gw_flow
```

### 4.3 Gateway 流控规则

```json
// gateway-flow-rules（Nacos 格式）
[
  {
    "resource": "order-service-route",      // 路由 ID
    "resourceMode": 0,                      // 0=路由粒度
    "grade": 1,                             // QPS
    "count": 200,
    "intervalSec": 1,
    "controlBehavior": 0,
    "burst": 0,
    "paramItem": {                          // 按参数限流
      "parseStrategy": 3,                   // 3=Header
      "fieldName": "X-User-Id",
      "pattern": "*",
      "matchStrategy": 0
    }
  },
  {
    "resource": "user-api-group",           // API 分组
    "resourceMode": 1,                      // 1=API 分组粒度
    "grade": 1,
    "count": 100
  }
]
```

### 4.4 自定义 Gateway 限流返回

```java
@Configuration
public class GatewaySentinelConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler blockExceptionHandler(
            List<BlockExceptionViewHandler> viewHandlers) {
        return new SentinelGatewayBlockExceptionHandler(
            viewHandlers, new DefaultErrorWebExceptionHandler());
    }

    @Bean
    public BlockExceptionViewHandler customBlockHandler() {
        return (exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

            // 根据规则类型返回不同消息
            String msg = switch (ex) {
                case FlowException fe     -> "请求过于频繁，请稍后重试";
                case DegradeException de  -> "服务暂时不可用，已降级处理";
                case AuthorityException ae -> "无权访问";
                default                   -> "服务被限制";
            };

            return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(("{\"code\":429,\"msg\":\"" + msg + "\"}")
                        .getBytes())));
        };
    }
}
```

---

## 5. 监控埋点与 Prometheus 集成

### 5.1 Sentinel 暴露的指标

```text
Sentinel 日志目录：${user.home}/logs/csp/

├── sentinel-record.log.{yyyy-MM-dd}  ← 拦截记录
│   每行：timestamp|resource|origin|count|pass|block|...
│
├── {appName}-metrics.log.{yyyy-MM-dd}  ← 秒级指标
│   每行：timestamp|resource|passQps|blockQps|successQps|
│         exceptionQps|rt|occupiedPassQps|...
│
└── {appName}-{pid}-metrics.log  ← 当前小时指标
```

### 5.2 Prometheus 集成

```xml
<!-- Sentinel Prometheus 扩展 -->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-prometheus-metric-exporter</artifactId>
</dependency>
```

```java
// 注册 Prometheus 数据源
@Configuration
public class SentinelPrometheusConfig {

    @PostConstruct
    public void init() {
        // Sentinel → Prometheus
        PrometheusMetricExporter exporter = new PrometheusMetricExporter();
        MetricExtensionManager.addMetricExtension(exporter);

        // 或者暴露为 HTTP 端点
        // GET /actuator/prometheus 中会包含 sentinel 指标
    }
}
```

```text
暴露的 Prometheus 指标：
  sentinel_pass_qps{resource="createOrder"}       → 通过 QPS
  sentinel_block_qps{resource="createOrder"}      → 拦截 QPS
  sentinel_success_qps{resource="createOrder"}    → 成功 QPS
  sentinel_exception_qps{resource="createOrder"}  → 异常 QPS
  sentinel_avg_rt{resource="createOrder"}         → 平均 RT
  sentinel_thread_count{resource="createOrder"}   → 并发线程数
  sentinel_degrade_open{resource="createOrder"}   → 熔断器是否打开(0/1)
```

---

## 6. 规则配置最佳实践

### 6.1 分级流控策略

```text
第一层：网关全局限流
  资源：Gateway 路由
  阈值：总 QPS 2000（保护后端整体）
  策略：快速失败

第二层：核心服务限流
  资源：createOrder, pay
  阈值：QPS 200（保护核心链路）
  策略：排队等待（削峰不拒绝）

第三层：慢调用熔断
  资源：getOrder
  规则：RT > 500ms 比例 > 50% → 熔断 30s
  策略：避免雪崩

第四层：热点参数限流
  资源：getProduct
  规则：参数 skuId=热销商品 → QPS 5000
       参数 skuId=普通商品 → QPS 500
  策略：热点商品给更高配额
```

### 6.2 规则模板

```json
// 通用流控规则模板
{
  "resource": "resourceName",
  "grade": 1,           // QPS 模式
  "count": 100,         // 阈值
  "strategy": 0,        // 直接模式
  "controlBehavior": 0, // 快速失败
  "clusterMode": false,
  "maxQueueingTimeMs": 500
}

// 通用熔断规则模板
{
  "resource": "resourceName",
  "grade": 0,              // 0=慢调用比例
  "count": 500,            // 慢调用 RT 阈值(ms)
  "timeWindow": 30,        // 熔断时长(s)
  "minRequestAmount": 10,  // 最小请求数（统计前提）
  "statIntervalMs": 1000,
  "slowRatioThreshold": 0.5  // 慢调用比例阈值
}
```

---

## 7. 生产环境 Checklist

```text
□ 规则管理
  □ 规则已持久化到 Nacos（不是 JVM 内存）
  □ 规则变更记录在 Nacos 审计日志中
  □ Sentinel Dashboard 可正常查看和修改规则
  □ 规则修改后有验证机制（灰度 → 全量）

□ 集群流控
  □ 评估是否需要集群流控（多实例 > 3 时强烈建议）
  □ Token Server 独立部署（非嵌入式）
  □ Token Server 至少 2 实例（高可用）
  □ 集群流控失败的降级策略已配置（fallbackToLocalWhenFail=true）

□ 监控告警
  □ Prometheus 采集 Sentinel 指标
  □ Grafana Dashboard 展示流控/熔断状态
  □ 告警规则：block_qps > 0 连续 1 分钟、熔断器打开通知
  □ sentinel-record.log 接入日志系统（ELK/Loki）

□ 规范
  □ @SentinelResource 必须指定 blockHandler 降级方法
  □ 降级返回的 JSON 格式统一
  □ 核心接口 + 非核心接口分级限流
  □ 定期压测验证限流阈值
```

---

> 🎯 **核心要点**：Sentinel 生产化的三件事——规则持久化到 Nacos（否则重启就丢）、集群流控（多实例共享阈值）、Gateway 网关流控（流量在入口拦截）。这三件事不做，Sentinel 就等于没上线。

---

**下一模块**：[05 - Seata 分布式事务内核](./05-Seata分布式事务内核.md)  
**返回总览**：[00 - 组件体系总览](./00-SpringCloudAlibaba组件体系总览.md)
