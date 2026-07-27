# 15-分布式链路追踪-SkyWalking
> 🎯 SkyWalking是Apache顶级开源分布式链路追踪系统 — 掌握Trace可视化、性能监控、告警体系三大核心能力，是微服务排障与性能优化的必备技能

---

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶与微服务 → 层级2 P1就业必备 → 微服务可观测性基础设施
- **前置依赖**：Spring Cloud微服务体系（服务注册发现、OpenFeign、Gateway）、Docker基础
- **重要性**：⭐⭐⭐⭐⭐（生产环境微服务排障的"眼睛"，大厂面试必问）

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 理解分布式链路追踪的核心概念（TraceId、SpanId、parent-child关系）；能搭建SkyWalking环境并接入Spring Boot应用 |
| **熟练** | 能在SkyWalking UI中分析Trace、定位慢查询和慢方法；配置基本的告警规则；将TraceId集成到日志中 |
| **精通** | 理解Agent探针的插件机制；掌握SkyWalking的存储选型（Elasticsearch vs BanyanDB）；能自定义告警Webhook和gRPC上报Span |

### 1.3 学习路线

```
单体阶段          →    微服务阶段              →    云原生阶段
Console/Fiddler        SkyWalking/Zipkin          OpenTelemetry
零散排障               分布式链路追踪               统一观测标准
```

---

## 2. 分层理论讲解

### 2.1 分布式链路追踪基础概念

#### 2.1.1 为什么需要链路追踪

在微服务架构中，一个业务请求往往需要经过多个服务协同处理：

```
用户请求 → Gateway → Order-Service → User-Service → Inventory-Service → Database
```

当这个请求出现耗时高或报错时，传统排障手段的痛点：

| 问题 | 单体架构 | 微服务架构 |
|------|---------|-----------|
| **请求链路** | 单一线程调用栈 | 跨进程、跨网络、跨机器 |
| **耗时定位** | 一个方法即可定位 | 不知道瓶颈在哪个服务 |
| **错误排查** | 看日志就能找到 | 日志分散在N台机器上 |
| **调用关系** | 直接看代码 | 需要动态拓扑发现 |

> 💡 **一句话**：链路追踪就是给每个请求发一个"身份证"（TraceId），让所有经过的服务都能被串联起来。

#### 2.1.2 核心概念

| 概念 | 说明 | 类比 |
|------|------|------|
| **Trace** | 一次完整请求的全链路，从入口到出口 | 一整个快递从寄出到签收 |
| **Span** | Trace中的一个独立工作单元，每个Span有开始和结束时间 | 快递的每个中转站环节 |
| **TraceId** | 全局唯一的请求标识，贯穿整个Trace | 快递单号 |
| **SpanId** | Span的自身ID，标识单个工作单元 | 中转站的工单号 |
| **ParentSpanId** | 父Span的ID，构建Span之间的父子关系 | 上一站的中转编号 |

#### 2.1.3 Trace与Span的层级关系

```
Trace (TraceId: abc123)
    │
    ├── Span (SpanId: A, ParentSpanId: null)      ← Gateway 入口
    │   ├── Span (SpanId: B, ParentSpanId: A)      ← Order-Service
    │   │   ├── Span (SpanId: C, ParentSpanId: B)  ← User-Service (HTTP调用)
    │   │   └── Span (SpanId: D, ParentSpanId: B)  ← Inventory-Service (gRPC调用)
    │   │       └── Span (SpanId: E, ParentSpanId: D) ← MySQL查询
    │   └── Span (SpanId: F, ParentSpanId: A)      ← Redis缓存访问
    │
    └── ⏱ 总耗时 = A.endTime - A.startTime
```

> 🎯 **核心洞察**：Span的树形结构还原了请求的完整调用链，每个Span记录的信息包括：操作名称、开始/结束时间、状态（成功/失败）、标签（KV键值对）和日志（关键事件）。

#### 2.1.4 链路追踪数据模型

```text
Span {
  traceId:      "abc123...",        // 全局Trace ID
  spanId:       "A",                 // 当前Span ID
  parentSpanId: null,               // 父Span ID（根Span为null）
  operationName: "GET /api/order",  // 操作名称
  startTime:    1712345678000,      // 开始时间戳（微秒）
  endTime:      1712345679000,      // 结束时间戳（微秒）
  status:       OK,                 // 状态: OK / ERROR
  tags: {                           // 标签：HTTP方法、URL、状态码等
    "http.method": "GET",
    "http.url": "/api/order/123",
    "http.status_code": "200"
  },
  logs: [                           // 日志：关键事件记录
    { time: 1712345678500, value: "DB query start" },
    { time: 1712345678800, value: "DB query end" }
  ],
  refs: [                           // 跨进程引用
    { type: CrossProcess, parentSpanId: "X", parentTraceId: "abc123..." }
  ]
}
```

### 2.2 SkyWalking整体架构

#### 2.2.1 架构总图

```
┌─────────────────────────────────────────────────────────────────┐
│                     SkyWalking 三大核心模块                        │
└─────────────────────────────────────────────────────────────────┘

  Agent (探针)                  OAP 集群                  UI 控制台
 ┌─────────────┐         ┌──────────────────┐        ┌────────────┐
 │ Java Agent   │ gRPC   │ OAP Server        │ HTTP  │ SkyWalking │
 │ Spring Boot  │ ─────→ │ - Receiver        │ ───→  │ UI         │
 │ Tomcat       │         │ - Aggregator      │        │ - 拓扑图    │
 │ gRPC Plugin  │         │ - Query           │        │ - Trace    │
 │ ...          │         │ - Alarm           │        │ - 仪表盘    │
 └─────────────┘         └──────┬──────────────┘        └────────────┘
                                │
                                ↓
                        ┌────────────────┐
                        │   存储后端       │
                        │ Elasticsearch   │
                        │ / BanyanDB /    │
                        │ MySQL / H2      │
                        └────────────────┘
```

> ⚠️ **架构关键点**：Agent与OAP之间是**gRPC通信**（非HTTP），OAP与UI之间是**HTTP/GraphQL通信**，存储后端对接多种数据库。

#### 2.2.2 三大组件职责

| 组件 | 职责 | 部署方式 | 关键技术 |
|------|------|---------|---------|
| **Agent** | 无侵入地收集应用Trace和Metrics数据 | 以Java Agent方式挂载到应用JVM中 | ByteBuddy字节码增强、插件化架构 |
| **OAP Server** | 接收Agent上报数据，聚合分析，写入存储 | 独立Java进程，可集群部署 | gRPC接收、流式聚合、告警引擎 |
| **UI** | 可视化展示Trace、拓扑、指标、告警 | 独立Web应用，对接OAP的GraphQL接口 | GraphQL API、Ant Design |

### 2.3 SkyWalking核心组件详解

#### 2.3.1 Agent探针原理

SkyWalking Agent的核心机制是**Java Agent + ByteBuddy字节码增强**，在应用启动时通过 `-javaagent` 参数挂载，无需修改业务代码。

**Agent启动流程**：

```text
JVM启动 → premain() → ByteBuddy创建ClassFileTransformer
    ↓
加载插件定义（skywalking-plugin.def）
    ↓
匹配目标类（如 @RequestMapping 标记的Controller）
    ↓
创建拦截器，增强方法：方法进入时创建Span，方法退出时关闭Span
    ↓
将Span数据通过gRPC异步上报到OAP
```

**Agent核心配置（agent/config/agent.config）**：

```properties
# 服务名称（在UI中显示的名称）
agent.service_name=${SW_AGENT_NAME:my-service}

# OAP Server gRPC地址（集群用逗号分隔）
collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:127.0.0.1:11800}

# 采样率：0-1之间，1表示100%采样
agent.sample_n_per_3_secs=${SW_AGENT_SAMPLE:-1}

# 忽略特定请求路径
agent.ignore_excluded_paths=${SW_AGENT_IGNORE_EXCLUDED_PATHS:/health,/actuator}

# 全局Trace忽略参数
agent.operation_name_threshold=${SW_AGENT_OPERATION_NAME_THRESHOLD:150}

# 是否启用跨线程传播（如@Async）
agent.cross_thread_span=${SW_AGENT_CROSS_THREAD:true}

# 数据库慢查询阈值（毫秒）
agent.memory_pool_metric_threshold=${SW_AGENT_MEMORY_POOL_METRIC_THRESHOLD:1000}
```

> 💡 **核心优势**：SkyWalking Agent采用**无侵入式接入**，与Spring Boot、Spring Cloud、Tomcat、Dubbo、gRPC、MySQL等主流框架和中间件天然兼容，开箱即用。

#### 2.3.2 Agent支持的中间件插件

SkyWalking内置了丰富的插件，覆盖了几乎所有主流的中间件和框架：

**Web/网关层**：

| 插件 | 覆盖范围 |
|------|---------|
| Spring MVC | `@RequestMapping`、`@GetMapping`、`@PostMapping`等 |
| Spring Cloud Gateway | 路由转发、过滤器 |
| Zuul | 1.x 网关过滤器链路 |
| Tomcat | Servlet容器层面 |

**RPC/通信层**：

| 插件 | 覆盖范围 |
|------|---------|
| Spring Cloud OpenFeign | HTTP客户端调用链路 |
| RestTemplate | Spring RestTemplate调用 |
| WebClient | Spring WebFlux响应式客户端 |
| Dubbo | 消费者-提供者全链路 |
| gRPC | gRPC调用链路 |
| Apache HttpClient / OkHttp | 底层HTTP客户端 |

**数据存储层**：

| 插件 | 覆盖范围 |
|------|---------|
| MySQL | JDBC连接池、SQL执行耗时 |
| PostgreSQL | 连接池与慢查询 |
| Redis (Jedis / Lettuce) | Redis操作入库出库 |
| Elasticsearch | ES读写操作 |
| MongoDB | MongoDB CRUD |
| Kafka / RocketMQ | 消息生产与消费链路 |

**异步/并发**：

| 插件 | 覆盖范围 |
|------|---------|
| ThreadPoolExecutor | 线程池执行跨度传播 |
| @Async | Spring异步调用跟踪 |
| CompletableFuture | 异步编排链路 |

#### 2.3.3 OAP Server核心功能

OAP Server是SkyWalking的大脑，负责数据接收、聚合、分析和告警。

**OAP核心组件**：

```text
OAP Server
    │
    ├── Receiver ────────── 接收Agent上报的gRPC数据（Trace、Metrics、Log）
    │   ├── TraceReceiver      → 解析Trace数据，构建Span树
    │   ├── MetricReceiver     → 聚合Metrics数据（JVM、服务、端点指标）
    │   └── LogReceiver        → 接收日志数据（v8协议起支持）
    │
    ├── Aggregator ────────── 流式聚合分析
    │   ├── TraceAggregator    → 计算P50/P90/P99耗时、服务依赖关系
    │   ├── MetricAggregator   → 每分钟/每小时粒度聚合指标
    │   └── TopologyAggregator → 构建服务拓扑映射
    │
    ├── Query ─────────────── 提供查询API
    │   └── GraphQL Query API  → 供UI端调用，查询Trace、指标、拓扑
    │
    ├── Alarm ────────────── 告警引擎
    │   ├── AlarmCore          → 规则匹配与触发
    │   └── AlarmWebhook       → 回调通知外部系统
    │
    └── Storage ──────────── 数据持久化
        └── StorageClient      → 对接Elasticsearch/BanyanDB/H2/MySQL
```

#### 2.3.4 存储后端选型

| 存储后端 | 适用场景 | 优点 | 缺点 |
|---------|---------|------|------|
| **Elasticsearch** | 生产环境首选 | 成熟稳定、搜索能力强、生态丰富 | 资源消耗较大、运维复杂 |
| **BanyanDB** | SkyWalking原生存储 | 专为Observability设计、存储效率高、写入性能好 | 较新、社区规模小于ES |
| **MySQL** | 开发/测试环境 | 部署简单、查看方便 | 大并发写入性能差、不适合生产 |
| **H2** | 本地快速体验 | 嵌入运行、零配置 | 仅用于演示，数据不持久 |

> 💡 **选型推荐**：生产环境优先选择**Elasticsearch**（社区成熟、文档丰富）；如果对资源有极致要求且能承担稳定性风险，可以考虑BanyanDB。

### 2.4 Trace可视化与分析

#### 2.4.1 SkyWalking UI 功能矩阵

| 功能模块 | 说明 | 解决什么问题 |
|---------|------|-------------|
| **Dashboard** | 全局监控仪表盘 | 服务/服务实例/端点的健康状态一览 |
| **Topology** | 拓扑图 | 服务之间的调用关系可视化 |
| **Trace** | 链路追踪查询 | 检索和查看具体请求的调用链 |
| **Profile** | 方法级性能分析 | 线程采样，定位热点方法 |
| **Log** | 日志查询 | 关联TraceId的日志展示 |
| **Alarm** | 告警历史 | 查看历史告警记录和告警配置 |
| **Database** | 数据库慢查询分析 | 展示慢SQL及关联的Trace |

#### 2.4.2 Trace查询与分析方法

**Trace查询条件**：

```
搜索条件：
├── 时间范围（必选）：最近15分钟/30分钟/1小时/自定义
├── 服务名称：选择跟踪哪个服务
├── 端点名称：HTTP URL或gRPC方法名
├── TraceId：精确搜索某个Trace
├── 状态列表：全部/成功/错误
└── 最小耗时：只搜索超过指定毫秒数的慢请求
```

**Trace详情页解读**：

```
Trace: abc123def456                         总耗时: 2.35s │ 状态: ERROR
                                                                 │
Span 0: GET /api/order/detail               耗时: 2.35s   │ ──────────
├── Span 1: OrderService.getOrderDetail()   耗时: 2.30s   │  ┌───────┐
│   ├── Span 2: [SQL] SELECT * FROM order   耗时: 0.05s   │  │ 瓶颈  │
│   ├── Span 3: [HTTP] call user-service    耗时: 0.20s   │  │ 在这  │
│   │   └── Span 4: UserController.getUser  耗时: 0.18s   │  │ ！  │
│   │       └── Span 5: [SQL] SELECT user   耗时: 0.15s   │  └───────┘
│   └── Span 6: [Redis] GET cache:product   耗时: 1.90s   │  ← Redis慢查询!
└── Span 7: Gateway路由处理                   耗时: 0.01s   │
```

> 🎯 **定位瓶颈三板斧**：
> 1. 按耗时降序排列，找到耗时最长的Span
> 2. 查看该Span的tags，确认是SQL/HTTP/Redis/业务逻辑
> 3. 结合日志中的TraceId精准定位具体代码行

### 2.5 性能指标监控

#### 2.5.1 三大监控粒度

SkyWalking从三个层次提供性能指标：

| 粒度 | 监控对象 | 核心指标 |
|------|---------|---------|
| **Service** | 整个微服务 | SLA、吞吐量(CPM)、P50/P90/P99耗时、错误率 |
| **Service Instance** | 服务实例（Pod/进程） | JVM堆内存、GC频率与耗时、CPU使用率 |
| **Endpoint** | API端点（URL/gRPC方法） | 请求次数、响应时间、成功率 |

#### 2.5.2 服务拓扑图

拓扑图是SkyWalking UI的标志性功能，通过一个动态图展示：

- **节点**：每个圆圈代表一个服务
- **连线**：箭头表示服务之间的调用方向
- **颜色**：绿色（健康）、黄色（延迟高）、红色（错误率高）
- **数据**：悬停在线上显示QPS、延迟、成功率

```
  ┌─────────┐     200ms/95%     ┌──────────┐
  │ Gateway │ ───────────────→  │   Order   │
  │  (绿)   │ ←───────────────  │  Service  │
  └─────────┘    成功/5rps      │   (绿)    │
       │                         └──────────┘
       │ 15ms/100%                   │ 300ms/90%
       ↓                             ↓
  ┌─────────┐                  ┌──────────┐       ┌──────────┐
  │   Auth  │                  │   User   │ ───→  │   Redis  │
  │ Service │                  │ Service  │       │   (黄)   │
  │  (绿)   │                  │   (绿)   │       └──────────┘
  └─────────┘                  └──────────┘
                                     │ 50ms/99%
                                     ↓
                                ┌──────────┐
                                │   MySQL  │
                                │   (绿)   │
                                └──────────┘
```

> 💡 **健康判断标准**：拓扑图上如果某个服务变为黄色，表示P99耗时超过1s；变为红色表示错误率超过10%。

#### 2.5.3 服务实例热力图

热力图（Heatmap）用于分析单个服务实例的耗时分布：

```
响应时间热力图 - Order-Service (instance-1)
时间 →  10:00    10:01    10:02    10:03    10:04    10:05
        ┌─────────────────────────────────────────────────┐
0-10ms  │██████  ██████  ██████  ██████  ██████  ████░░  │ 正常
10-50ms │███░░░  ████░░  █████   ████░░  █████   ████░░  │ 一般
50-200ms│░░░░░░  ░░░░░░  ░░░░░░  ░░░░░░  ░░░░░░  ░░░░░░  │ 较慢
200ms+  │░░░░░░  ░░░░░░  ░░░░░░  ░░░░░░  ░░░░░░  ██░░░░  │ 超时
        └─────────────────────────────────────────────────┘
```

### 2.6 告警体系

#### 2.6.1 告警配置语法

SkyWalking的告警规则配置位于 `config/alarm-settings.yml`：

```yaml
# ⚠️ 告警规则：基于指标阈值
rules:
  # 规则1：端点P99耗时超过1000ms
  endpoint_p99_rule:
    # 指标名称（端点P99响应时间）
    metrics-name: endpoint_p99
    # 阈值（单位：毫秒）
    threshold: 1000
    # 操作符：> 大于, < 小于, >=, <=, =
    op: ">"
    # 评估周期（分钟）
    period: 3
    # 满足条件的次数达到此值才触发告警
    count: 3
    # 告警沉默时间（分钟）—— 同一个实体在沉默期内不再重复告警
    silence-period: 5
    # 告警严重程度
    message: 端点 {name} P99耗时 {value}ms 超过阈值 {threshold}ms

  # 规则2：服务成功率低于80%
  service_sla_rule:
    metrics-name: service_sla
    threshold: 80
    op: "<"
    period: 5
    count: 2
    silence-period: 10
    message: 服务 {name} 成功率 {value}% 低于阈值 {threshold}%

  # 规则3：服务实例JVM GC次数异常
  service_instance_gc_rule:
    metrics-name: service_instance_gc_youth_count
    threshold: 100
    op: ">"
    period: 3
    count: 1
    silence-period: 3
    message: 实例 {name} GC次数 {value} 超过阈值 {threshold}

# 🔗 Webhook回调配置（当告警触发时，SkyWalking调用此HTTP接口）
webhooks:
  # 企业微信Webhook
  - url: https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
    # POST JSON 格式：
    # {"receiver":"","status_code":200,"alarms":[{"scope":"端点","name":"/api/order","id0":0,...}]}
  # 钉钉Webhook
  - url: https://oapi.dingtalk.com/robot/send?access_token=xxx
  # 自定义告警系统
  - url: http://alert-system.example.com/skywalking/alarm
```

#### 2.6.2 告警Webhook消息格式

```json
[
  {
    "scopeId": 5,
    "scope": "SERVICE",
    "name": "order-service",
    "id0": 12,
    "id1": 0,
    "ruleName": "endpoint_p99_rule",
    "alarmMessage": "端点 /api/order/detail P99耗时 2345ms 超过阈值 1000ms",
    "startTime": 1712345678000,
    "tags": [
      {"key": "level", "value": "WARNING"}
    ]
  }
]
```

### 2.7 SkyWalking vs Zipkin vs Jaeger 全面对比

| 对比维度 | SkyWalking | Zipkin | Jaeger |
|---------|-----------|--------|--------|
| **开源组织** | Apache | Apache (原Twitter) | CNCF (原Uber) |
| **接入方式** | Java Agent无侵入式 | SDK侵入式/字节码 | SDK侵入式/Agent |
| **语言支持** | Java、.NET、Go、Node.js、Python、PHP | Java、Go、JavaScript、.NET、Python | Go、Java、Node.js、Python、C++ |
| **协议** | gRPC + HTTP | HTTP + Kafka | gRPC + HTTP |
| **存储后端** | Elasticsearch、BanyanDB、MySQL、H2 | Cassandra、Elasticsearch、MySQL | Elasticsearch、Kafka、Cassandra、Badger |
| **拓扑自动发现** | ✅ 自动生成服务拓扑 | ❌ 需要额外组件 | ❌ 部分支持 |
| **告警引擎** | ✅ 内置完整告警 | ❌ 无内置告警 | ❌ 需搭配Prometheus |
| **方法级Profile** | ✅ 线程采样分析 | ❌ | ❌ |
| **性能指标** | ✅ 内置JVM/服务/端点指标 | ❌ 仅Trace | ❌ 仅Trace+部分指标 |
| **日志集成** | ✅ TraceId自动注入日志 | ⚠️ 需手动配置 | ⚠️ 需手动配置 |
| **告警Webhook** | ✅ 内置 | ❌ | ❌ |
| **OpenTelemetry兼容** | ✅ 支持OTLP协议 | ✅ 原生支持 | ✅ 原生支持 |
| **UI易用性** | ⭐⭐⭐⭐⭐ 功能全面 | ⭐⭐⭐ 简洁但功能少 | ⭐⭐⭐⭐ 清晰但功能中等 |
| **性能开销** | 低（~5%性能影响） | 中（~10%性能影响） | 中（~10%性能影响） |
| **学习成本** | 低（Agent接入） | 中（需写拦截代码） | 中（需写拦截代码） |
| **社区活跃度** | 高（Apache顶级项目） | 中（维护期） | 高（CNCF毕业项目） |
| **生产环境推荐** | ✅ 首选（Java强生态） | ⚠️ 配合Spring Cloud Sleuth | ⚠️ 多语言环境 |

> 🎯 **选型结论**：
> - **Java微服务技术栈 → SkyWalking**（Agent无侵入、功能最全、运维成本最低）
> - **Spring Cloud + 轻量需求 → Zipkin**（配合Spring Cloud Sleuth/Micrometer Tracing快速集成）
> - **多语言混合架构 → Jaeger**（CNCF标准、OpenTelemetry生态好）

---

## 3. 高频踩坑与误区

### 3.1 Agent挂载失败

```text
❌ 错误现象：
java.lang.reflect.MalformedParametersException: Malformed parameter list
    at ...
    at java.base/java.lang.reflect.Executable.getParameters(Executable.java:...)

✅ 原因与解决方案：
- 原因：Java版本不兼容（SkyWalking 8.x最高支持Java 11，9.x才支持Java 17+）
- 解决：升级SkyWalking版本到9.x+，或者降级JDK到11
```

> ⚠️ **兼容性检查**：SkyWalking 9.0+ 支持 Java 8-21，低于9.0不支持Java 17。在升级JDK时务必同步升级SkyWalking版本。

### 3.2 Agent不生效

```text
❌ 错误现象：
接入Agent后SkyWalking UI中看不到服务数据

✅ 排查步骤：
1. 检查Agent日志：logs/skywalking-agent.log
   └── 常见错误：collector.backend_service 地址配错
2. 确认OAP Server是否正常（11800端口连通性）
3. 检查Agent参数顺序：-javaagent必须在-jar之前！
4. 检查应用是否为多ClassLoader场景（如Tomcat），需要配置：
   Dskywalking.agent.is_open_debugging_class=true
```

### 3.3 采样率过高导致OAP OOM

```text
❌ 错误现象：
OAP Server频繁Full GC或直接OOM崩溃

✅ 原因与解决方案：
- 原因：高并发场景下100%全量采样，Agent上报量过大
- 解决：合理配置采样率
  agent.sample_n_per_3_secs=${SW_AGENT_SAMPLE:3}
  （每3秒采样3条，默认-1表示全量）
```

> 💡 **生产采样建议**：日常环境3秒采样3条即可（sample_n_per_3_secs=3），问题排查时临时调高。

### 3.4 存储索引太大导致ES磁盘爆满

```text
❌ 错误现象：
Elasticsearch yellow/red status，磁盘使用率超过85%

✅ 解决方案：
1. SkyWalking 8.x默认索引TTL：
   - Trace数据：3天
   - Metrics数据：7天
2. 调整TTL（oap-server/config/application.yml）：
   core:
     default:
       enableDataKeeperExecutor: true
       dataKeeperExecuteInterval: 5  # 清理间隔（分钟）
       recordDataTTL: 3              # Trace数据保留天数
       metricsDataTTL: 7             # 指标数据保留天数
3. 或者使用ILM（Index Lifecycle Management）策略自动管理
```

### 3.5 TraceId与日志不关联

```text
❌ 错误现象：
日志中有TraceId但和SkyWalking中的TraceId不一致，或日志中根本没有TraceId

✅ 解决方案：
1. 检查logback-spring.xml中是否配置了%X{tid}（SkyWalking的MDC key是"tid"，不是"traceId"！）
2. SkyWalking 8.4.0+ 通过以下配置开启TraceId注入：
   - agent.correlation.auto_tag=true
   - 需要手动配置logback MDC（见章节5.3）
```

### 3.6 跨线程链路断裂

```text
❌ 错误现象：
异步线程（@Async、CompletableFuture）中的Span没有父Span，链路在异步处断裂

✅ 解决方案：
1. 对于Spring @Async，SkyWalking Agent自动处理（需开启cross_thread_span）
2. 手动传递TraceContext：
   TracingContext.INSTANCE.capture()      → 在父线程创建快照
   TracingContext.INSTANCE.continued(snapshot) → 在子线程恢复上下文
3. 使用SkyWalking提供的包装工具类：
   RunnableWrapper.of(runnable)  /  CallableWrapper.of(callable)
```

### 3.7 关键配置对比

| 配置项 | 开发环境 | 生产环境 |
|--------|---------|---------|
| 采样率 | `sample_n_per_3_secs: 1` | `sample_n_per_3_secs: 3` |
| Trace保留 | `recordDataTTL: 7` | `recordDataTTL: 3` |
| Metrics保留 | `metricsDataTTL: 30` | `metricsDataTTL: 7` |
| 告警沉默期 | `silence-period: 1` | `silence-period: 10` |

---

## 4. 随堂基础练习

### 4.1 概念理解题

**练习1**：将Trace中的Span信息填充完整。

已知一个请求经过 Gateway → Order-Service → User-Service，其中Order-Service调用了User-Service的HTTP接口，填充TraceId和SpanId：

```text
TraceId: (     ①      )
    ├── Span: spanId=null, parentSpanId=null, operationName="GET /api/order"       ← Gateway
    │   ├── Span: spanId=B, parentSpanId=(      ②     ), operationName="orderService.order()"  ← Order-Service
    │   │   ├── Span: spanId=D, parentSpanId=(      ③     ), operationName="/api/user"       ← HTTP调用
    │   │   └── Span: spanId=(      ④     ), parentSpanId=B, operationName="[SQL] SELECT * FROM order"  ← MySQL
```

> 💡 答案：① `abc123`（任意全局唯一值）、② `A`、③ `B`、④ `E`

### 4.2 配置题

**练习2**：以下是错误的Agent配置，请找出并修正：

```properties
agent.service_name=my-service
collector.backend_service=127.0.0.1:12800
agent.sample_n_per_3_secs=-1
agent.ignore_excluded_paths=/health,/actuator
```

> ⚠️ **错误点**：OAP gRPC端口是`11800`，不是`12800`。`12800`是HTTP/GraphQL端口（UI连接用）。修正：`collector.backend_service=127.0.0.1:11800`。

### 4.3 连线题

**练习3**：将以下端口与对应的功能连线：

| 端口 | 功能 |
|------|------|
| 11800 | A. UI控制台(HTTP) |
| 12800 | B. Agent与OAP(gRPC) |
| 8080 (Webapp) | C. OAP GraphQL查询接口 |

> 💡 答案：11800→B, 12800→C, 8080→A

---

## 5. 章节综合实操案例

> 🎯 **目标**：搭建一个完整的微服务Demo（order-service + user-service），接入SkyWalking实现链路追踪，并通过UI定位一个慢SQL查询。

### 5.1 项目结构

```
skywalking-demo/
├── docker-compose.yml           # SkyWalking OAP + UI + ES
├── order-service/
│   ├── src/main/java/...
│   │   ├── OrderApplication.java
│   │   ├── controller/OrderController.java
│   │   ├── service/OrderService.java
│   │   ├── mapper/OrderMapper.java
│   │   └── entity/Order.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── user-service/
│   ├── src/main/java/...
│   │   ├── UserApplication.java
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── mapper/UserMapper.java
│   │   └── entity/User.java
│   ├── src/main/resources/application.yml
│   └── pom.xml
└── agent/
    └── skywalking-agent/         # 解压后的SkyWalking Agent
```

### 5.2 部署基础设施（Docker Compose）

```yaml
# docker-compose.yml
# ⚠️ 启动命令：docker-compose up -d
# 前置依赖：Docker Engine 20.10+、Docker Compose 2.x+

version: '3.8'

services:
  # --- Elasticsearch 存储 ---
  elasticsearch:
    image: elasticsearch:7.17.15
    container_name: skywalking-es
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - bootstrap.memory_lock=true
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - es-data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    networks:
      - skywalking

  # --- SkyWalking OAP Server ---
  oap:
    image: apache/skywalking-oap-server:9.7.0
    container_name: skywalking-oap
    depends_on:
      - elasticsearch
    environment:
      - SW_STORAGE=elasticsearch
      - SW_STORAGE_ES_CLUSTER_NODES=elasticsearch:9200
      - SW_ES_HEALTH_CHECKER=elasticsearch:9200
      - SW_HEALTH_CHECKER=true
      - SW_TELEMETRY=prometheus
      - JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "11800:11800"   # gRPC端口（Agent连接）
      - "12800:12800"   # HTTP/GraphQL端口（UI连接）
    networks:
      - skywalking
    restart: unless-stopped

  # --- SkyWalking UI ---
  ui:
    image: apache/skywalking-ui:9.7.0
    container_name: skywalking-ui
    depends_on:
      - oap
    environment:
      - SW_OAP_ADDRESS=http://oap:12800
    ports:
      - "8080:8080"     # UI访问地址：http://localhost:8080
    networks:
      - skywalking
    restart: unless-stopped

volumes:
  es-data:
    driver: local

networks:
  skywalking:
    driver: bridge
```

> 💡 **启动验证**：`docker-compose ps` 确认三个服务状态为"Up"。访问 `http://localhost:8080` 确认SkyWalking UI正常加载。

### 5.3 微服务项目配置

#### 5.3.1 order-service 项目

**pom.xml**（核心依赖）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.4.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
    </parent>
    <groupId>com.demo</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.4</spring-cloud.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- MySQL + MyBatis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <finalName>order-service</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**application.yml**：

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:mysql://localhost:3306/skywalking_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

# Feign 配置
spring.cloud.openfeign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 5000

logging:
  level:
    com.demo: DEBUG
```

**OrderController.java**：

```java
package com.demo.order.controller;

import com.demo.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取订单详情（包含用户信息）
     * 此端点将被SkyWalking追踪
     */
    @GetMapping("/detail/{orderId}")
    public String getOrderDetail(@PathVariable Long orderId) {
        return orderService.getOrderDetailWithUser(orderId);
    }

    /**
     * 模拟慢查询端点
     */
    @GetMapping("/slow/{orderId}")
    public String getOrderSlow(@PathVariable Long orderId) throws InterruptedException {
        // 模拟业务处理耗时
        Thread.sleep(2000);
        return orderService.getOrderDetailWithUser(orderId);
    }
}
```

**OrderService.java**：

```java
package com.demo.order.service;

import com.demo.order.client.UserServiceClient;
import com.demo.order.entity.Order;
import com.demo.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserServiceClient userServiceClient;

    /**
     * 获取订单详情并关联用户信息
     * SkyWalking将自动追踪此方法内的所有调用
     */
    public String getOrderDetailWithUser(Long orderId) {
        // 1. 查询订单（数据库）
        Optional<Order> orderOpt = orderMapper.findById(orderId);
        if (orderOpt.isEmpty()) {
            return "订单不存在：" + orderId;
        }
        Order order = orderOpt.get();

        // 2. 调用User-Service获取用户信息（HTTP调用）
        String userInfo = userServiceClient.getUser(order.getUserId());

        // 3. 模拟处理耗时
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "订单: " + order.getOrderNo() + " | 用户: " + userInfo;
    }
}
```

**OrderMapper.java**：

```java
package com.demo.order.mapper;

import com.demo.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMapper extends JpaRepository<Order, Long> {
    // JPA自动实现CRUD
}
```

**UserServiceClient.java**（Feign客户端）：

```java
package com.demo.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserServiceClient {

    @GetMapping("/api/user/{userId}")
    String getUser(@PathVariable("userId") Long userId);
}
```

**Order.java**：

```java
package com.demo.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, nullable = false)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

#### 5.3.2 user-service 项目

**application.yml**：

```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/skywalking_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: root
    password: root123
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**UserController.java**：

```java
package com.demo.user.controller;

import com.demo.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 模拟一个慢查询方法（通过SQL睡眠模拟）
     */
    @GetMapping("/{userId}")
    public String getUser(@PathVariable Long userId) {
        // 模拟慢SQL：查询用户并加入延时
        return userService.findUserWithSlowQuery(userId);
    }

    /**
     * 正常查询
     */
    @GetMapping("/fast/{userId}")
    public String getUserFast(@PathVariable Long userId) {
        return userService.findUserFast(userId);
    }
}
```

**UserService.java**：

```java
package com.demo.user.service;

import com.demo.user.entity.User;
import com.demo.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 模拟慢查询 —— 通过延时制造慢SQL
     * SkyWalking将会标记此Span为慢查询
     */
    public String findUserWithSlowQuery(Long userId) {
        // 模拟慢SQL（实际慢SQL由数据库执行计划决定）
        Optional<User> userOpt = userMapper.findByIdWithSleep(userId);
        return userOpt.map(u -> u.getName() + "(" + u.getEmail() + ")")
                .orElse("用户不存在");
    }

    public String findUserFast(Long userId) {
        Optional<User> userOpt = userMapper.findById(userId);
        return userOpt.map(u -> u.getName() + "(" + u.getEmail() + ")")
                .orElse("用户不存在");
    }
}
```

**UserMapper.java**：

```java
package com.demo.user.mapper;

import com.demo.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMapper extends JpaRepository<User, Long> {

    /**
     * 模拟慢查询：使用 MySQL SLEEP 函数人为制造慢SQL
     * 这个慢查询在执行 SELECT 之前会先睡眠 1.5 秒
     */
    @Query(value = "SELECT * FROM t_user WHERE id = :userId AND SLEEP(1.5) = 0", nativeQuery = true)
    Optional<User> findByIdWithSleep(@Param("userId") Long userId);

    /**
     * 正常查询
     */
    @Query(value = "SELECT * FROM t_user WHERE id = :userId", nativeQuery = true)
    Optional<User> findById(@Param("userId") Long userId);
}
```

**User.java**：

```java
package com.demo.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
```

### 5.4 启动微服务并接入SkyWalking Agent

#### 5.4.1 下载并解压SkyWalking Agent

```bash
# 下载 SkyWalking 9.7.0（包含 Agent）
wget https://dlcdn.apache.org/skywalking/9.7.0/apache-skywalking-apm-9.7.0.tar.gz

# 解压后 agent 目录在 apache-skywalking-apm-bin/agent/
tar -xzf apache-skywalking-apm-9.7.0.tar.gz

# Agent目录结构
# agent/
# ├── activations/
# ├── config/
# │   └── agent.config          ← Agent配置文件
# ├── logs/                     ← Agent日志目录
# ├── optional-plugins/
# ├── plugins/                  ← 自动加载所有插件
# └── skywalking-agent.jar      ← Agent主程序
```

#### 5.4.2 使用Java Agent启动微服务

**启动order-service**：

```bash
# 注意：-javaagent 参数必须在 -jar 之前！
java -javaagent:/path/to/agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=order-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar order-service.jar
```

**启动user-service**：

```bash
java -javaagent:/path/to/agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=user-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar user-service.jar
```

> 💡 **IDEA开发环境配置**：在IDEA的 Run Configurations → VM options 中添加：
> `-javaagent:D:\path\to\agent\skywalking-agent.jar -Dskywalking.agent.service_name=order-service -Dskywalking.collector.backend_service=127.0.0.1:11800`

### 5.5 构造请求并观察Trace

#### 5.5.1 生成测数据

```sql
-- 初始化数据库
CREATE DATABASE IF NOT EXISTS skywalking_demo;
USE skywalking_demo;

-- 插入测试数据
INSERT INTO t_user (id, name, email, phone, create_time) VALUES
(1, '张三', 'zhangsan@example.com', '13800001111', NOW()),
(2, '李四', 'lisi@example.com', '13800002222', NOW());

INSERT INTO t_order (order_no, user_id, total_amount, status, create_time) VALUES
('ORD202407260001', 1, 99.50, 1, NOW()),
('ORD202407260002', 2, 199.00, 1, NOW());
```

#### 5.5.2 发起测试请求

```bash
# 1. 正常请求
curl http://localhost:8081/api/order/detail/1

# 2. 慢请求（会触发user-service的慢查询）
curl http://localhost:8081/api/order/slow/1

# 3. 制造错误请求（不存在的订单）
curl http://localhost:8081/api/order/detail/9999
```

#### 5.5.3 在SkyWalking UI中定位慢查询

**操作步骤**：

```text
1. 打开浏览器访问 http://localhost:8080
2. 点击左侧导航栏 → "Trace" (追踪)
3. 设置查询条件：
   - 时间范围：最近15分钟
   - 服务：order-service
   - 状态：全部
   - 最小耗时：输入 1000（只显示超过1秒的慢请求）
4. 点击查询 → 找到耗时最高的Trace（约2.3秒）
5. 点击Trace详情 → 观察Span树：
   ┌─────────────────────────────────────────────────┐
   │ Span 1: GET /api/order/slow/1        2.3s      │
   │ ├── Span 2: OrderService.slow()      2.2s      │
   │ │ ├── Span 3: [SQL] SELECT order     0.05s     │
   │ │ ├── Span 4: HTTP /api/user/1       1.7s      │  ← 瓶颈！
   │ │ │ └── Span 5: UserController       1.65s     │
   │ │ │     └── Span 6: [SQL] SLEEP(1.5) 1.5s     │  ← 慢查询！
   │ │ └── Span 7: Thread.sleep(100)      0.1s      │
   └─────────────────────────────────────────────────┘
6. 点击 Span 6 查看详情：
   - Tags: db.type=mysql, db.statement=SELECT * FROM t_user WHERE id=? AND SLEEP(1.5)=0
   - 问题定位：user-service使用了SLEEP(1.5)导致慢查询
```

### 5.6 TraceId集成到日志

> 🎯 **目标**：让日志打印中包含TraceId，实现"日志←→链路"双向关联。

**logback-spring.xml**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- SkyWalking的MDC key是 "tid"，不是 "traceId" -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="unknown"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <!-- 👇 核心：在pattern中加入 %tid 输出TraceId -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{tid}] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{tid}] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**日志输出示例**：

```text
# 无SkyWalking Agent时：TraceId部分为空
2026-07-26 14:30:45.123 [http-nio-8081-exec-1] [] INFO  c.d.o.service.OrderService - 开始查询订单：1

# 有SkyWalking Agent时：自动注入TraceId
2026-07-26 14:30:45.123 [http-nio-8081-exec-1] [abc123def456ghi] INFO  c.d.o.service.OrderService - 开始查询订单：1
2026-07-26 14:30:45.456 [http-nio-8081-exec-1] [abc123def456ghi] INFO  c.d.o.client.UserServiceClient - 调用用户服务获取用户信息
2026-07-26 14:30:47.123 [http-nio-8081-exec-1] [abc123def456ghi] WARN  c.d.o.service.OrderService - 用户服务响应耗时较长：1680ms
```

> ⚠️ **关键细节**：SkyWalking的MDC key是`tid`，不是`traceId`，也不是`trace_id`！在logback pattern中必须写`%X{tid}`。

### 5.7 AOP自定义Trace注解

除了Agent自动采集的Span，我们还可以通过AOP+注解的方式手动标记业务Span：

**@TracedMethod 注解**：

```java
package com.demo.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义链路追踪注解
 * 标记在方法上，SkyWalking会为此方法创建一个Span
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TracedMethod {
    /**
     * 操作名称，默认使用方法全路径
     */
    String operationName() default "";

    /**
     * 标签键值对，格式：{"key1=value1", "key2=value2"}
     */
    String[] tags() default {};
}
```

**AOP切面实现**：

```java
package com.demo.common.aspect;

import com.demo.common.annotation.TracedMethod;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * SkyWalking自定义Span切面
 * 功能：为标记了 @TracedMethod 的方法创建自定义Span
 */
@Aspect
@Component
public class SkywalkingTraceAspect {

    @Around("@annotation(tracedMethod)")
    public Object aroundTracedMethod(ProceedingJoinPoint joinPoint, TracedMethod tracedMethod) throws Throwable {
        // 获取操作名称
        String operationName = tracedMethod.operationName();
        if (operationName.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            operationName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        }

        // 创建自定义Span：SkyWalking Toolkit API
        ActiveSpan.tag("operation", operationName);
        ActiveSpan.tag("class", joinPoint.getTarget().getClass().getName());
        ActiveSpan.tag("method", joinPoint.getSignature().getName());
        ActiveSpan.tag("args", Arrays.toString(joinPoint.getArgs()));

        // 添加自定义标签
        for (String tag : tracedMethod.tags()) {
            String[] kv = tag.split("=", 2);
            if (kv.length == 2) {
                ActiveSpan.tag(kv[0], kv[1]);
            }
        }

        // 记录开始事件
        ActiveSpan.info("开始执行: " + operationName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;

            ActiveSpan.tag("cost_ms", String.valueOf(cost));
            ActiveSpan.info("执行完成: " + operationName + ", 耗时: " + cost + "ms");

            return result;
        } catch (Exception e) {
            ActiveSpan.tag("error", e.getMessage());
            ActiveSpan.error(e);
            throw e;
        }
    }
}
```

**使用示例**：

```java
@Service
public class PaymentService {

    @TracedMethod(operationName = "PaymentService.processPayment", tags = {"module=payment", "version=v2"})
    public String processPayment(Long orderId, BigDecimal amount) {
        // 此方法会在SkyWalking中创建独立Span
        // 可以在UI中看到 "PaymentService.processPayment" 的自定义操作
        // ... 业务逻辑
        return "支付处理完成";
    }
}
```

### 5.8 gRPC自定义Span上报

对于更复杂的场景，可以使用SkyWalking Toolkit直接创建并上报Span：

```java
package com.demo.common.trace;

import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;

/**
 * SkyWalking Toolkit API 使用示例
 * 通过注解方式创建Span，比AOP方案更轻量
 */
@Service
public class OrderWorkflowService {

    /**
     * @Trace 注解创建Span
     * 方法名作为operationName
     */
    @Trace
    public void createOrder(Long userId, Long productId) {
        // 此方法被追踪
        doValidate();
        doSave();
        doNotify();
    }

    /**
     * 自定义操作名称和标签
     */
    @Trace(operationName = "OrderWorkflow.sendNotification")
    @Tag(key = "channel", value = "sms")
    public void doNotify() {
        // 发送通知
    }

    /**
     * 多个标签
     */
    @Trace(operationName = "OrderWorkflow.validateOrder")
    @Tags({
        @Tag(key = "validateType", value = "full"),
        @Tag(key = "timeout", value = "5000")
    })
    public void doValidate() {
        // 校验逻辑
    }

    /**
     * 手动获取TraceId（在日志或响应头中返回）
     */
    public String getCurrentTraceId() {
        // TraceContext.traceId() 返回当前线程的TraceId
        return TraceContext.traceId();
    }

    /**
     * 获取当前SpanId
     */
    public String getCurrentSpanId() {
        return TraceContext.spanId();
    }
}
```

---

## 6. 分层综合习题

### 6.1 基础题（理解）

**题目1**：什么是TraceId？什么是SpanId？它们在链路追踪中分别起什么作用？

**题目2**：SkyWalking的三大核心组件是什么？分别用一句话描述它们的职责。

**题目3**：填空：
- Agent与OAP的通信端口是（ ① ），协议是（ ② ）
- UI与OAP的通信端口是（ ③ ），协议是（ ④ ）

**题目4**：判断题：SkyWalking Agent接入必须修改应用代码才能完成链路追踪。（ ）

**题目5**：以下哪个不是Span的必含属性？
A. traceId  B. spanId  C. operationName  D. dbStatement  E. startTime

### 6.2 进阶题（应用）

**题目6**：生产环境中，你发现某个API的P99耗时突然从200ms飙升到3s，请描述使用SkyWalking定位问题的完整排查流程。

**题目7**：配置题：请写出一份完整的Docker Compose配置，包含Elasticsearch 7.17、SkyWalking OAP 9.7.0、SkyWalking UI 9.7.0，要求：
- ES单节点模式
- OAP使用ES作为存储后端
- 开放正确的端口

**题目8**：日志集成：写出logback-spring.xml中输出TraceId的pattern配置，并说明为什么使用`%X{tid}`而不是`%X{traceId}`。

**题目9**：以下告警规则配置有误，请找出问题并修正：

```yaml
rules:
  endpoint_slow_rule:
    metrics-name: endpoint_p99
    threshold: 1000
    op: "<"
    period: 1
    count: 1
    silence-period: 5
    message: 端点 {name} P99耗时 {value}ms
```

> ⚠️ **提示**：`op: "<"`表示小于阈值时触发告警，而我们需要的是**大于**阈值时触发。应改为 `op: ">"`。

**题目10**：在SkyWalking UI中，如何通过Trace详情页快速判断一个请求的瓶颈在哪个环节？请写出至少3个判断维度。

### 6.3 高级题（分析）

**题目11**：你的微服务系统中，有一个服务使用了`CompletableFuture`进行异步调用，但在SkyWalking中发现异步子线程中的Span没有关联到父Trace，链路在异步处断裂。请分析原因并给出至少两种解决方案。

**题目12**：场景设计：某电商系统的库存服务在高峰期出现大量超时，通过SkyWalking定位到是数据库连接池耗尽导致，但数据库本身负载并不高。请分析可能的原因，并说明你会在SkyWalking中查看哪些指标来验证你的假设。

**题目13**：设计题：请设计一个SkyWalking告警体系，包含以下要求：
- 端点P99耗时超过800ms持续2分钟
- 服务成功率低于90%
- 服务实例GC超过每分钟50次
- 告警通知发送到企业微信
- 同一种告警10分钟内不重复发送

**题目14**：存储选型：某公司每天产生约500GB的Trace数据，要求在Trace历史中能查询最近7天的数据，查询响应时间不超过3秒。请分析Elasticsearch和BanyanDB的方案优劣，给出你的选型建议。

**题目15**：在多语言异构系统中（Java + Go + Python），你会如何规划链路追踪标准？结合OpenTelemetry和SkyWalking，设计一个兼容多语言的Trace方案。

---

## 7. 本章复盘速记清单

### 7.1 核心概念速查

| 概念 | 快速记忆 | 一句话解释 |
|------|---------|-----------|
| Trace | 一次请求的全景图 | 从请求发起到响应返回的完整路径 |
| Span | 一个调用单元的快照 | 每个服务/方法调用的耗时和状态 |
| TraceId | 请求身份证号 | 关联所有日志和Span的唯一标识 |
| ParentSpanId | 父级引用 | 构建Span树形结构的关键指针 |
| Agent | 不干活的数据采集员 | 挂在JVM上自动收集调用数据 |
| OAP | 数据分析处理中心 | 接收Agent上报、聚合、写入存储 |
| Topology | 服务关系地图 | 自动发现服务之间的调用依赖关系 |

### 7.2 常用命令速查

```bash
# 启动SkyWalking OAP + UI + ES
docker-compose up -d

# 查看SkyWalking日志
docker logs -f skywalking-oap

# 查看Agent日志
tail -f agent/logs/skywalking-agent.log

# 使用Agent启动Spring Boot应用
java -javaagent:/path/skywalking-agent.jar \
     -Dskywalking.agent.service_name=my-service \
     -Dskywalking.collector.backend_service=127.0.0.1:11800 \
     -jar app.jar

# 查询OAP健康状态
curl http://localhost:12800/graphql -H "Content-Type: application/json" -d '{"query":"{getHealth}"}'
```

### 7.3 端口速记

| 端口 | 组件 | 用途 |
|------|------|------|
| 11800 | OAP Server | gRPC：Agent上报数据 |
| 12800 | OAP Server | HTTP/GraphQL：UI查询接口 |
| 8080 | UI Webapp | 浏览器访问地址 |
| 9200 | Elasticsearch | RESTful API |
| 9300 | Elasticsearch | 集群内部通信 |

### 7.4 排查流程

```
定位慢请求 / 错误请求的标准流程：

1. SkyWalking UI → Trace 页面
   ↓
2. 按耗时排序 → 找到最慢的Trace
   ↓
3. 查看Span树 → 找耗时最长的Span
   ↓
4. 查看Span Tags → 区分是SQL/HTTP/Redis/业务逻辑
   ↓
5. 如果是SQL → Database页面查看具体SQL
   ↓
6. 如果是HTTP → 跳转到下游服务的Trace
   ↓
7. 复制TraceId → 到日志系统中搜索关联日志
   ↓
8. 定位到具体代码行 → 修复
```

### 7.5 常见配置速查

| 配置项 | 默认值 | 生产推荐值 | 说明 |
|--------|--------|-----------|------|
| `agent.sample_n_per_3_secs` | -1（全量） | 3-5 | 每3秒采样条数 |
| `agent.ignore_excluded_paths` | 空 | `/health,/actuator,/metrics` | 忽略健康检查等 |
| `agent.cross_thread_span` | false | true | 跨线程链路传播 |
| `recordDataTTL` | 3天 | 3-7天 | Trace数据保留时长 |
| `metricsDataTTL` | 7天 | 7-30天 | 指标数据保留时长 |
| `SW_STORAGE` | H2 | elasticsearch | 存储后端 |

---

## 8. 精通拓展补充-P2

### 8.1 自定义Agent插件开发

当SkyWalking内置插件不覆盖某些自定义框架时，可以开发自定义插件。

**插件开发核心步骤**：

```java
/**
 * 自定义SkyWalking插件
 * 拦截自定义RPC框架的调用
 */
public class MyCustomPlugin extends AbstractClassEnhancePluginDefine {

    @Override
    protected ClassMatch enhanceClass() {
        // 1. 指定要增强的类
        return NameMatch.byName("com.mycompany.rpc.client.RpcClient");
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    // 2. 指定要增强的方法
                    return named("call");
                }

                @Override
                public String getMethodsInterceptor() {
                    // 3. 指定拦截器实现类
                    return "com.mycompany.plugin.RpcClientInterceptor";
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
}
```

**插件注册文件**：在 `src/main/resources/skywalking-plugin.def` 中注册：

```properties
my-custom-plugin=com.mycompany.plugin.MyCustomPlugin
```

> 💡 **插件开发三要素**：① `enhanceClass()` 指定增强哪个类；② `getMethodsMatcher()` 指定增强哪个方法；③ 拦截器中创建Span。

### 8.2 BanyanDB原生存储

BanyanDB是SkyWalking社区专门为Observability数据设计的原生数据库，定位替代Elasticsearch。

**BanyanDB核心特性**：

| 特性 | 说明 | 优势 |
|------|------|------|
| **数据模型** | 专为Trace/Metrics/Log设计 | 避免ES的"宽表"浪费 |
| **写入性能** | LSM-Tree + 列式存储 | 比ES高3-5倍写入吞吐 |
| **存储压缩** | 时序数据专用压缩算法 | 存储成本降低60% |
| **查询** | 原生支持Trace树查询 | 复杂关联查询更快 |
| **运维** | 无外部依赖 | 比ES+ZK/Kafka简单 |

**BanyanDB + SkyWalking部署**：

```yaml
# docker-compose 使用 BanyanDB 替代 Elasticsearch
version: '3.8'
services:
  banyandb:
    image: apache/skywalking-banyandb:0.6.0
    container_name: skywalking-banyandb
    environment:
      - SW_STORAGE=banyandb
    ports:
      - "17912:17912"
      - "17913:17913"
    networks:
      - skywalking

  oap:
    image: apache/skywalking-oap-server:9.7.0
    environment:
      - SW_STORAGE=banyandb
      - SW_STORAGE_BANYANDB_HOST=banyandb
      - SW_STORAGE_BANYANDB_PORT=17912
    depends_on:
      - banyandb
    ports:
      - "11800:11800"
      - "12800:12800"
    networks:
      - skywalking

  ui:
    image: apache/skywalking-ui:9.7.0
    environment:
      - SW_OAP_ADDRESS=http://oap:12800
    ports:
      - "8080:8080"
    networks:
      - skywalking

networks:
  skywalking:
    driver: bridge
```

### 8.3 eBPF集成概览

SkyWalking 9.x引入了eBPF（Extended Berkeley Packet Filter）支持，将可观测性下沉到操作系统内核层面。

**eBPF + SkyWalking的应用场景**：

| 场景 | 说明 | 解决的问题 |
|------|------|-----------|
| **网络延迟分析** | 在内核层捕获网络包收发耗时 | 区分"网络延迟"和"应用处理延迟" |
| **内核级Span** | 系统调用级别的Span | 精确定位内核态瓶颈 |
| **Service Mesh兼容** | 与Istio/Envoy配合 | 无侵入sidecar数据采集 |
| **TCP丢包检测** | 监控TCP重传和RTT | 网络质量监控 |

> 💡 **eBPF定位**：eBPF解决的是"内核态"的可观测性，与SkyWalking Agent的"应用态"追踪互补。目前仍在快速发展中，适合对内核排障有高要求的团队。

### 8.4 SkyWalking与OpenTelemetry集成

OpenTelemetry是CNCF的可观测性标准，SkyWalking 9.x原生支持OTLP协议。

**集成架构**：

```text
应用 (Java/Go/Python)
    │
    ├── OpenTelemetry SDK ──→ OTLP Exporter ──→ SkyWalking OAP (OTLP Receiver)
    │                                  │
    │                           (也可使用SkyWalking Agent)
    │
    └── 统一的数据模型：OTel → SkyWalking 格式转换
```

**配置OAP接收OTLP数据**：

```yaml
# oap-server/config/application.yml
receiver:
  otlp:
    enabled: true
    gRPC:
      host: 0.0.0.0
      port: 11801   # OTLP gRPC 端口（与原生SkyWalking gRPC 11800不同）
    http:
      host: 0.0.0.0
      port: 12801   # OTLP HTTP 端口
```

### 8.5 大规模生产部署建议

| 关注点 | 建议 | 说明 |
|--------|------|------|
| **OAP集群化** | 至少2节点，K8s Deployment + Service | 高可用，避免单点故障 |
| **ES集群** | 3节点以上，ILM自动管理索引 | 防止磁盘写满导致OAP不可用 |
| **存储容量规划** | 每天Trace量 × 保留天数 × 压缩比(1.5) | 预留20%的Buffer空间 |
| **OAP JVM配置** | 至少8GB堆内存（-Xms8g -Xmx8g） | OAP是内存密集型应用 |
| **Agent批量上报** | `agent.buffer_size=20000` | 避免高并发时数据丢失 |
| **采样策略** | 日常3-5条/3秒，压测时临时全量 | 平衡数据完整性和性能开销 |
| **监控OAP本身** | 对OAP也部署SkyWalking Agent | 避免"谁来监控监控系统" |
| **日志级别** | OAP生产环境用WARN，Agent用INFO | Agent日志过多影响磁盘IO |

> 🎯 **终极建议**：在生产环境部署SkyWalking前，一定要先做压力测试 + 索引TTL验证，确保存储不会在高峰期打爆。

---

> **本章关联**：SkyWalking与后续的[15-分布式链路追踪-Zipkin]()（原理对比）、[18-Spring Cloud Alibaba生态]()（集成方案）共同构成微服务可观测性体系。在[04-实战项目]()中将落地SkyWalking + Prometheus + Grafana三件套。
