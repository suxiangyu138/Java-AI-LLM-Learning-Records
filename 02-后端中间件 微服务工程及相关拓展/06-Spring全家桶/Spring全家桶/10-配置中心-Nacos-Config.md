# 配置中心 Nacos Config
> 微服务架构的"配置总开关"—— 集中管理分布式系统中的所有配置，实现配置的动态推送、版本管理与环境隔离。

## 目录
1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-P2)

---

## 1. 本章总览

### 1.1 知识定位

在传统单体应用中，配置通常写在 `application.yml` 文件中，修改配置需要重新打包和重启服务。在微服务架构中，服务实例数量可能达到数十甚至上百个，手动修改每个实例的配置不现实，且重启服务会影响可用性。

**配置中心** 解决的核心问题：
- 配置集中管理：所有服务的配置统一存储、分类管理
- 动态刷新：配置变更时实时推送到客户端，无需重启
- 环境隔离：开发/测试/生产环境的配置自动分离
- 版本管理：配置变更历史可追溯，支持回滚
- 权限控制：配置变更需要审批，防止误操作

Nacos Config 是 Nacos 内置的配置管理能力，与 Nacos Discovery 使用同一服务端，无需额外部署。

### 1.2 前置知识

| 知识领域 | 要求 | 说明 |
|---------|------|------|
| Nacos 服务注册 | 掌握 | 推荐先学前一章 Nacos 服务注册与发现 |
| Spring Boot 配置体系 | 掌握 | application.yml、bootstrap.yml、@ConfigurationProperties |
| Spring Cloud 基础 | 了解 | Spring Cloud 配置加载顺序 |
| Nacos Server 部署 | 掌握 | 了解 Nacos 控制台操作 |

### 1.3 学习目标

| 层级 | 目标 | 对应能力 |
|------|------|---------|
| **L1-应用** | 能在 Nacos 控制台创建配置，通过 Spring Boot 客户端读取和动态刷新 | 初级工程师 |
| **L2-原理** | 理解 Namespace/Group/DataId 三级模型，掌握配置共享与优先级规则 | 中级工程师 |
| **L3-架构** | 能设计多环境多租户配置架构、灰度发布方案，理解长轮询机制与监听原理 | 高级工程师 |

---

## 2. 分层理论讲解

### 2.1 Nacos Config 架构总览

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            Nacos Config Server                                │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                          Config Service                                 │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────────┐  │   │
│  │  │ Namespace│ │ Namespace│ │ Namespace│ │ Namespace│ │ Namespace       │  │   │
│  │  │   dev    │ │   test   │ │   staging│ │   prod   │ │   custom        │  │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬───────────┘  │   │
│  │       │           │           │           │           │               │   │
│  │       ▼           ▼           ▼           ▼           ▼               │   │
│  │  ┌───────────┐                                                   │   │   │
│  │  │   Group    │━━━ DataId: xxx.yml / xxx.properties                 │   │   │
│  │  └───────────┘                                                   │   │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                       核心能力                                            │   │
│  │  Long Polling(Nacos-Client)   ConfigChangeListener   ConfigHistory     │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
           ▲                              │
           │  pull / watch                 │  push / notify
           │                              ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Microservice Client                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                       │
│  │  user-service │  │ order-service│  │product-service│                      │
│  │  dev          │  │  prod        │  │  test         │                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                       │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Nacos Config 三级模型

Nacos Config 使用 **Namespace → Group → DataId** 三级结构管理配置。

```
Namespace（命名空间）    ── 最顶层隔离单元，用于隔离不同环境或租户
    │
    ├── Group（分组）    ── 同一命名空间下的逻辑分组，用于区分业务线或项目
    │     │
    │     └── DataId（配置 ID）─ 配置文件的唯一标识，通常以文件名 + 后缀形式存在
    │           │
    │           └── Config（配置内容）
```

#### 2.2.1 Namespace（命名空间）

| 特性 | 说明 |
|------|------|
| 用途 | 实现多环境（dev/test/staging/prod）或多租户隔离 |
| 默认值 | `public`（保留命名空间） |
| 创建方式 | Nacos 控制台 → 命名空间 → 新建 |
| 隔离级别 | **完全隔离**，不同 Namespace 之间的配置不可见 |
| 适用场景 | 环境隔离、部门隔离、租户隔离 |

```yaml
spring:
  cloud:
    nacos:
      config:
        namespace: dev     # 使用 dev 命名空间
        # namespace: c0a6d4a1-7b8c-4c9d-9e0f-1a2b3c4d5e6f  # 也可以使用 Namespace ID
```

> 💡 推荐使用 Namespace ID（UUID）而非 Namespace 名称来配置，避免名称冲突。

#### 2.2.2 Group（分组）

| 特性 | 说明 |
|------|------|
| 用途 | 同一命名空间下的逻辑分组 |
| 默认值 | `DEFAULT_GROUP` |
| 常见分组 | `DEFAULT_GROUP`, `ECOMMERCE_GROUP`, `PAY_GROUP` |
| 隔离级别 | **逻辑隔离**，同 Group 的配置具有更高聚合度 |

```yaml
spring:
  cloud:
    nacos:
      config:
        group: ECOMMERCE_GROUP   # 自定义分组
```

#### 2.2.3 DataId（配置 ID）

DataId 是配置的唯一标识，格式通常为：

```
${prefix}-${spring.profiles.active}.${file-extension}
```

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `prefix` | `${spring.application.name}` | 配置前缀，默认为服务名 |
| `spring.profiles.active` | 无 | 当前激活的环境 Profile |
| `file-extension` | `properties` | 配置文件的格式（properties / yaml / yml） |

```
# 示例：一个服务在不同环境下的 DataId
user-service-dev.yaml       # 开发环境
user-service-test.yaml      # 测试环境
user-service-prod.yaml      # 生产环境
user-service.yaml           # 公共配置（不指定 Profile）
```

```yaml
spring:
  cloud:
    nacos:
      config:
        file-extension: yaml        # 配置文件格式
        prefix: ${spring.application.name}  # 前缀
```

### 2.3 Nacos Config 客户端集成

#### 2.3.1 添加依赖

```xml
<!-- Nacos Config Starter -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    <version>2022.0.0.0</version>
</dependency>

<!-- Nacos Discovery（可选，同时做服务发现） -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    <version>2022.0.0.0</version>
</dependency>
```

#### 2.3.2 bootstrap.yml 配置

> ⚠️ Nacos Config 必须在 **bootstrap.yml**（或 bootstrap.properties）中配置，因为配置中心的信息需要在应用启动的早期阶段加载。Spring Cloud 2020.x 之后默认不再自动引入 bootstrap，需要手动添加依赖或使用不同方式。

```xml
<!-- 方式一：添加 bootstrap 依赖（推荐） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

```yaml
# bootstrap.yml —— 应用启动最早加载的配置文件
spring:
  application:
    name: user-service

  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848   # Nacos 服务端地址
        namespace: public              # 命名空间
        group: DEFAULT_GROUP           # 分组
        file-extension: yaml           # 配置格式
        prefix: ${spring.application.name}  # DataId 前缀
        refresh-enabled: true          # 开启动态刷新
        timeout: 3000                  # 获取配置的超时时间（ms）
        max-retry: 3                   # 最大重试次数
        enable-remote-sync-config: true  # 是否开启远程配置同步

      discovery:
        server-addr: 127.0.0.1:8848
```

#### 2.3.3 在 Nacos 控制台创建配置

在 Nacos 控制台 → 配置管理 → 配置列表 → 新建配置：

```
Data ID:    user-service.yaml
Group:      DEFAULT_GROUP
格式:       YAML
配置内容:
```

```yaml
# user-service.yaml
server:
  port: 8081

app:
  name: 用户服务
  version: v1.0.0

user:
  default-avatar: https://default-avatar.com/user.png
  max-login-attempts: 5
  login-lock-duration: 1800

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_db?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 2.3.4 读取配置代码

```java
package com.example.userservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RefreshScope  // 动态刷新：当配置变更时，自动刷新 Bean 的属性
public class ConfigController {

    @Value("${app.name:unknown}")
    private String appName;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Value("${user.default-avatar:}")
    private String defaultAvatar;

    @Value("${user.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("appName", appName);
        config.put("appVersion", appVersion);
        config.put("defaultAvatar", defaultAvatar);
        config.put("maxLoginAttempts", maxLoginAttempts);
        config.put("datasourceUrl", datasourceUrl);
        config.put("timestamp", System.currentTimeMillis());
        return config;
    }
}
```

### 2.4 @RefreshScope 动态刷新原理

#### 2.4.1 核心机制

```
Nacos Config Server                   Client Application
       │                                     │
       │  1. 管理员修改配置                     │
       │                                     │
       │  2. 发布新版本                        │
       │                                     │
       │  3. 推送变更通知（HTTP）               │
       │  ─────────────────────────────────→   │
       │                                     │
       │  4. 客户端收到通知                     │
       │     └─ 内部触发 refresh 流程          │
       │                                     │
       │  5. 客户端拉取最新配置                 │
       │  ←─────────────────────────────────   │
       │                                     │
       │  6. Spring Cloud RefreshBus          │
       │     └─ ContextRefresher.refresh()    │
       │        └─ 销毁所有 @RefreshScope Bean │
       │           └─ 重新创建 Bean（注入新值） │
       │                                     │
       │  7. 下次请求使用新 Bean               │
```

#### 2.4.2 @RefreshScope 源码分析

```java
// @RefreshScope 使用的 Scope 实现
public class RefreshScope extends GenericScope implements Scope {

    // 存储所有 @RefreshScope 的 Bean 缓存
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        // 从缓存获取，缓存未命中则创建
        return this.cache.computeIfAbsent(name, k -> objectFactory.getObject());
    }

    // 核心方法：刷新时清除所有缓存
    public void refreshAll() {
        super.destroy();  // 调用每个 Bean 的 destroy 方法
        this.cache.clear();
    }

    // 刷新单个 Bean
    public void refresh(String name) {
        this.cache.remove(name);
        this.destroy(name);  // 调用指定 Bean 的 destroy 方法
    }
}
```

```java
// ContextRefresher —— 触发刷新的入口
public class ContextRefresher implements ApplicationContextAware {

    private final ConfigurableApplicationContext context;

    public synchronized String[] refresh() {
        // 1. 创建新的 StandardEnvironment
        // 2. 重新获取所有配置源（PropertySource）
        // 3. 对比旧配置和新配置，找出变更项
        Map<String, Object> before = extract(context.getEnvironment().getPropertySources());
        addConfigFilesToEnvironment();
        Set<String> keys = changes(before, extract(context.getEnvironment().getPropertySources()));

        // 4. 刷新所有 @RefreshScope Bean
        this.scope.refreshAll();

        // 5. 发布 EnvironmentChangeEvent 事件
        this.context.publishEvent(new EnvironmentChangeEvent(context, keys));

        return keys.toArray(new String[0]);
    }
}
```

> 💡 `@RefreshScope` 的原理是自定义 Scope。Spring 容器在启动时创建 Bean 实例并缓存，刷新时销毁缓存，下次请求时重新创建实例并注入新值。注意：Bean 在第一次被调用时才创建（懒加载），刷新后第一个请求会有创建开销。

#### 2.4.3 @RefreshScope 的局限性

| 局限性 | 说明 | 解决方案 |
|--------|------|---------|
| 构造方法注入 | 刷新时 Bean 重建会重新执行构造方法 | 确保构造方法幂等 |
| @PostConstruct | 刷新时会再次执行初始化方法 | 慎用资源初始化逻辑 |
| 字段注入 | 普通字段不会自动刷新 | 必须使用 @Value 或 @ConfigurationProperties |
| 集群广播 | 单个实例刷新通知不传播 | 集成 Spring Cloud Bus |
| 持久化连接 | 数据库连接池等不会重建 | 使用 DataSource 代理或手动刷新 |

### 2.5 配置共享（多服务共用配置）

#### 2.5.1 共享配置的三种方式

**方式一：通过 shared-configs 共享**

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: common-database.yaml     # 公共数据库配置
            group: DEFAULT_GROUP
            refresh: true
          - data-id: common-redis.yaml        # 公共 Redis 配置
            group: DEFAULT_GROUP
            refresh: true
          - data-id: common-logging.yaml      # 公共日志配置
            group: DEFAULT_GROUP
            refresh: true
```

```yaml
# common-database.yaml（所有服务共享）
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**方式二：通过 extension-configs 扩展（Nacos 2.x 推荐）**

```yaml
spring:
  cloud:
    nacos:
      config:
        extension-configs:
          - data-id: redis-config.yaml
            group: SHARED_GROUP
            refresh: true
          - data-id: mq-config.yaml
            group: SHARED_GROUP
            refresh: true
```

**方式三：通过 @PropertySource 导入**

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
// 从 Nacos 配置中心获取共享配置
@PropertySource(value = "common-config.properties")
public class SharedConfig {
    // 这种方式的局限性：不支持动态刷新
}
```

#### 2.5.2 Namespace 级别的共享

利用 Namespace 做配置共享：

```
Namespace: common（公共命名空间）
├── common-database.yaml
├── common-redis.yaml
└── common-mq.yaml

Namespace: product-service（产品线命名空间）
├── product-service-dev.yaml
├── product-service-test.yaml
└── product-service-prod.yaml
```

```yaml
spring:
  cloud:
    nacos:
      config:
        # 从公共命名空间读取共享配置
        namespace: ${config.namespace:product-service}
        shared-configs:
          - data-id: common-database.yaml
            namespace: common     # 指向公共命名空间
            refresh: true
          - data-id: common-redis.yaml
            namespace: common
            refresh: true
```

### 2.6 配置优先级

#### 2.6.1 加载优先级排序

Nacos Config 配置优先级从高到低：

```
高优先级
    │
    ├── 1. ${spring.application.name}-${profile}.${file-extension}
    │     例：user-service-dev.yaml（带 profile 的专属配置，优先级最高）
    │
    ├── 2. ${spring.application.name}.${file-extension}
    │     例：user-service.yaml（不带 profile 的服务配置）
    │
    ├── 3. extension-configs（扩展配置，按列表顺序，后声明优先级低）
    │     例：第一个列出的优先级最高
    │
    ├── 4. shared-configs（共享配置，按列表顺序，后声明优先级低）
    │
    └── 5. application.yml（本地配置，优先级最低）
       │
低优先级
```

#### 2.6.2 优先级验证示例

```yaml
# ===== Nacos 配置：user-service-dev.yaml =====
app:
  title: Nacos配置-带Profile  # 优先级最高

# ===== Nacos 配置：user-service.yaml =====
app:
  title: Nacos配置-无Profile
  version: 1.0.0

# ===== Nacos 配置：extension-configs 中的 common.yaml =====
app:
  title: 扩展共享配置
  version: 0.0.1

# ===== 本地 application.yml =====
app:
  title: 本地配置
```

最终加载结果：
- `app.title` = "Nacos配置-带Profile"（优先级 1 最高）
- `app.version` = "1.0.0"（从优先级 2 获取，优先级 3 的值被覆盖）

#### 2.6.3 配置优先级完整表格

| 序号 | 配置来源 | 示例 | 优先级 |
|------|---------|------|--------|
| 1 | 命令行参数 | `--app.title=cmd` | 最高 |
| 2 | 带 Profile 的远程配置 | `user-service-dev.yaml` | |
| 3 | 无 Profile 的远程配置 | `user-service.yaml` | |
| 4 | Extension Configs | `extension-configs[0]` > `[1]` | |
| 5 | Shared Configs | `shared-configs[0]` > `[1]` | |
| 6 | 本地 bootstrap.yml | 本地配置 | 最低 |
| 7 | 本地 application.yml | 本地配置 | 最低 |

> 🎯 **关键原则**：带 Profile 的配置覆盖无 Profile 的配置；远程配置覆盖本地配置；专有配置覆盖共享配置。

### 2.7 灰度发布（Beta 发布）

#### 2.7.1 概念

灰度发布（Gray Release / Canary Release）是一种逐步推送配置变更的策略，先让小部分服务实例应用新配置，验证无问题后再推送到全量实例。

#### 2.7.2 Nacos Config 灰度发布

Nacos 控制台支持一键配置灰度发布：

```
操作路径：配置管理 → 配置列表 → 编辑 → 发布配置
    └─ 勾选 "Beta发布"
    └─ 填写 Beta IP 列表（以逗号分隔）
    └─ 点击 "发布 Beta"
```

#### 2.7.3 灰度发布的架构流程

```
                         Nacos Config Server
                               │
                     ┌─────────┴─────────┐
                     │                     │
              ┌──────┴──────┐       ┌──────┴──────┐
              │  Beta 分组    │       │  正式分组    │
              │ (灰度实例)    │       │ (正常实例)   │
              └──────┬──────┘       └──────┬──────┘
                     │                     │
                     ▼                     ▼
              ┌──────────────┐    ┌──────────────┐
              │ user-service  │    │ user-service  │
              │ 192.168.1.10  │    │ 192.168.1.11  │
              │ 192.168.1.12  │    │ 192.168.1.13  │
              │ (使用新配置)  │    │ (使用旧配置)  │
              └──────────────┘    └──────────────┘
```

#### 2.7.4 灰度发布操作流程

```yaml
# 1. 在 Nacos 控制台编辑配置
# 2. 勾选 "Beta发布"
# 3. 填写灰度 IP 列表，如：
#    192.168.1.10,192.168.1.12
# 4. 发布

# 灰度配置内容示例（调整数据库连接池）
spring:
  datasource:
    hikari:
      maximum-pool-size: 50           # 原值 20，灰度调整为 50
      minimum-idle: 20                # 原值 10，灰度调整为 20
      connection-timeout: 3000        # 原值 5000，灰度调整为 3000
```

灰度发布验证流程：

```bash
# 验证灰度实例是否应用了新配置
curl http://192.168.1.10:8081/config   # 预期显示 pool-size=50
curl http://192.168.1.11:8081/config   # 预期显示 pool-size=20（未灰度）
```

> ⚠️ 灰度发布生效后，如果不正式发布，Beta 配置会覆盖正式配置只在灰度 IP 上生效。**灰度配置重启后是否保留取决于是否已正式发布**，建议灰度验证通过后立即正式发布。

### 2.8 配置监听与自定义扩展

#### 2.8.1 监听配置变更

```java
package com.example.userservice.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class NacosConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigListener.class);

    private final NacosConfigManager nacosConfigManager;

    public NacosConfigListener(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @PostConstruct
    public void registerListener() throws NacosException {
        String dataId = "user-service.yaml";
        String group = "DEFAULT_GROUP";

        nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;  // null 表示使用 Nacos 默认的线程池执行
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                // 配置发生变更时的回调
                log.info("配置已变更，新配置内容：\n{}", configInfo);
                // 在这里执行自定义逻辑，如重新初始化连接池、刷新缓存等
                handleConfigChange(configInfo);
            }
        });
    }

    private void handleConfigChange(String configInfo) {
        // 自定义配置变更处理逻辑
        log.info("执行配置变更后的自定义处理...");
    }
}
```

#### 2.8.2 获取配置历史版本

Nacos 控制台支持查看配置的历史版本和回滚操作：

```
操作路径：配置管理 → 配置列表 → 点击配置 → "历史版本"
    └─ 查看每次修改的详细 Diff
    └─ 点击 "回滚" 恢复到指定版本
```

#### 2.8.3 Nacos Config 与 Apollo 对比

| 对比维度 | Nacos Config | Apollo (携程) |
|---------|-------------|---------------|
| **部署复杂度** | 低，与 Nacos 注册中心共用服务端 | 中，需要部署 ConfigService、AdminService、Portal 多个组件 |
| **配置管理模型** | Namespace / Group / DataId 三级模型 | 应用 / 环境 / 集群 / Namespace 四级模型 |
| **多环境支持** | 通过 Namespace 实现 | 原生多环境、多集群支持 |
| **实时推送** | 基于长轮询 (Long Polling) | 基于长连接 (WebSocket) |
| **动态刷新** | @RefreshScope | @ApolloConfigChangeListener |
| **灰度发布** | 支持（Beta 发布） | 支持（灰度发布 + 全量发布） |
| **配置继承** | 通过 shared-configs | 支持 Namespace 继承 |
| **权限管理** | 基础 RBAC（开源版） | 完善的权限管理体系 |
| **审计日志** | 历史版本 + 简单回滚 | 完整的审计追踪 |
| **开放 API** | 丰富 | 丰富 |
| **配置格式** | properties / yaml / yml / txt / json / xml / html | properties / xml / yml / yaml / json / txt / htl |
| **监听机制** | Listener 接口 | @ApolloConfigChangeListener |
| **配置导出/导入** | 支持 | 支持 |
| **性能** | 高（阿里双十一验证） | 高（携程验证） |
| **Spring Cloud 集成度** | 原生 Alibaba 生态，深度集成 | 需额外适配 |
| **社区活跃度** | 非常活跃 | 活跃 |
| **学习成本** | 低 | 中 |

> 🎯 **选型建议**：已经使用 Nacos 做注册中心的项目，自然使用 Nacos Config 避免维护两套系统；配置管理场景复杂、需要细粒度权限管理和审计追踪的大型团队，Apollo 的管理功能更完善。

---

## 3. 高频踩坑与误区

### 3.1 bootstrap.yml 不生效

**现象**：将 Nacos Config 配置写在 `application.yml` 中，启动后无法从 Nacos 拉取配置。

**根源**：Spring Boot 2.4+ / Spring Cloud 2020.x 默认禁用了 bootstrap 机制，`application.yml` 加载顺序晚于 `bootstrap.yml`，导致 Nacos Config 配置未被正确加载。

```xml
<!-- ❌ 错误：缺少 bootstrap 依赖 -->
<!-- 未引入 spring-cloud-starter-bootstrap -->

<!-- ✅ 方案一：添加 bootstrap 依赖 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>

<!-- ✅ 方案二：不使用 bootstrap，直接使用 Spring Cloud 新机制 -->
<!-- application.yml -->
spring:
  application:
    name: user-service
  config:
    import: nacos:user-service.yaml  # 直接导入 Nacos 配置
```

### 3.2 配置动态刷新不生效

**现象**：在 Nacos 控制台修改配置，客户端调用接口获取到的值未更新。

**根源**：Bean 未标注 `@RefreshScope`，或者配置属性未通过 `@Value` / `@ConfigurationProperties` 注入。

```java
// ❌ 错误：未加 @RefreshScope
@Component
public class AppConfig {
    @Value("${app.version}")
    private String version;
    // 修改 Nacos 配置后，version 不会更新
}

// ✅ 正确：添加 @RefreshScope
@Component
@RefreshScope
public class AppConfig {
    @Value("${app.version}")
    private String version;
    // 修改 Nacos 配置后，version 自动更新
}
```

```java
// ❌ 错误：硬编码默认值，覆盖了 Nacos 配置
@Value("${app.version:1.0.0}")  // 默认值 1.0.0 可能覆盖 Nacos 配置
private String version;

// ✅ 正确：避免使用默认值，或确保 Nacos 配置有值
@Value("${app.version}")
private String version;
```

### 3.3 配置文件格式不一致

**现象**：Nacos 控制台配置的 DataId 为 `user-service.properties`，但客户端设置的 `file-extension` 为 `yaml`，导致配置无法解析。

```yaml
# ❌ 错误：DataId 后缀和 file-extension 不一致
# Nacos 创建的 DataId：user-service.yaml
# 客户端配置：
spring:
  cloud:
    nacos:
      config:
        file-extension: properties  # 应该为 yaml

# ✅ 正确：保持一致
spring:
  cloud:
    nacos:
      config:
        file-extension: yaml  # 与 DataId 后缀一致
```

> 💡 Nacos Config 客户端拼接 DataId 的规则是：`${prefix}-${profile}.${file-extension}`，三个部分任何一个不匹配都会导致获取不到配置。

### 3.4 Namespace ID 与名称混淆

**现象**：配置了 `namespace: dev`，但客户端报了 `Namespace not found` 错误。

**根源**：`namespace` 属性既可以填 Namespace ID（UUID），也可以填 Namespace 名称，但**如果名称冲突或不存在**，会导致找不到命名空间。

```yaml
# ❌ 可能的问题：namespace 名称包含不可见字符或空格
spring:
  cloud:
    nacos:
      config:
        namespace: dev  # 控制台创建时名称可能误写了 "dev " 带空格

# ✅ 推荐：使用 Namespace ID（UUID），避免歧义
spring:
  cloud:
    nacos:
      config:
        namespace: c0a6d4a1-7b8c-4c9d-9e0f-1a2b3c4d5e6f
```

### 3.5 配置项被不当覆盖

**现象**：Nacos 配置的值和 application.yml 中的值不同，但最终生效的是 application.yml 的值。

**根源**：对配置优先级规则理解有误。**远程配置的优先级高于本地配置**，但如果本地配置中指定了特殊的 Active Profile，情况会更复杂。

```yaml
# ❌ 误区：以为 application.yml 会覆盖 Nacos 配置
# 实际上 Nacos 配置优先级高于本地 application.yml

# ✅ 正确理解优先级：
# 1. 带 Profile 的远程配置（优先级最高）
#    user-service-dev.yaml
# 2. 不带 Profile 的远程配置
#    user-service.yaml
# 3. 本地 application.yml（优先级最低）
```

### 3.6 DataId 命名冲突

**现象**：两个不同服务的配置相互覆盖，修改 A 服务的配置后 B 服务的配置也变了。

**根源**：使用了相同的 DataId，例如两个服务都使用 `application.yaml` 作为 DataId，导致配置冲突。

```yaml
# ❌ 错误：两个服务使用相同的 DataId
# user-service 的 DataId: application.yaml
# order-service 的 DataId: application.yaml
# 修改任意一个，另一个也会收到变更

# ✅ 正确：每个服务使用唯一 DataId
# user-service 的 DataId: user-service.yaml
# order-service 的 DataId: order-service.yaml
```

---

## 4. 随堂基础练习

### 练习 1：Nacos 控制台配置创建

在 Nacos 控制台中完成以下操作：

1. 创建一个新的命名空间 `training`
2. 在该命名空间下创建一个 Group `TRAINING_GROUP`
3. 创建 DataId 为 `demo-service.yaml` 的配置，内容包含：
   ```yaml
   demo:
     message: Hello Nacos Config
     timeout: 3000
     retry-count: 3
   ```
4. 在控制台查看该配置的详情

### 练习 2：基础配置读取

创建一个 Spring Boot 项目，实现：

1. 添加 Nacos Config 依赖
2. 配置 bootstrap.yml 读取练习 1 中创建的配置
3. 在 Controller 中使用 `@Value` 注入 `demo.message` 和 `demo.timeout`
4. 编写 `/config` 接口返回这些配置值

### 练习 3：动态刷新验证

在练习 2 的基础上：

1. 给 Controller 添加 `@RefreshScope` 注解
2. 在 Nacos 控制台将 `demo.message` 的值从 `Hello Nacos Config` 改为 `Hello Dynamic Config`
3. 调用接口观察返回值的实时变化
4. 移除 `@RefreshScope` 注解，再次修改配置，观察是否还会自动刷新

### 练习 4：多 Profile 配置

实现多环境配置切换：

1. 在 Nacos 创建两个配置：
   - `demo-service-dev.yaml`：`demo.env=development`
   - `demo-service-prod.yaml`：`demo.env=production`
2. 启动时通过 `--spring.profiles.active=dev` 或 `=prod` 切换环境
3. 验证不同 Profile 下读取到不同的配置值

---

## 5. 章节综合实操案例

### 5.1 案例背景

实现一个 **微服务配置管理中心**，包含多环境配置隔离、公共配置共享、动态刷新和配置变更监听。使用 `config-demo-parent` 多模块项目。

### 5.2 项目结构

```
config-demo/
├── pom.xml                          # 父模块
└── config-client/
    ├── pom.xml
    └── src/main/
        ├── java/com/example/config/
        │   ├── ConfigClientApplication.java
        │   ├── controller/
        │   │   └── ConfigController.java
        │   ├── config/
        │   │   ├── OrderConfig.java
        │   │   └── DataSourceConfig.java
        │   └── listener/
        │       └── NacosConfigListener.java
        └── resources/
            ├── bootstrap.yml
            └── application.yml
```

### 5.3 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>config-demo</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>config-client</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2022.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2022.0.0.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**config-client/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>config-demo</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>config-client</artifactId>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Nacos Config -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- Nacos Discovery -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Bootstrap 支持 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-bootstrap</artifactId>
        </dependency>

        <!-- Actuator（健康检查） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 5.4 Nacos 配置准备

在 Nacos 控制台创建以下配置：

**配置 1：config-client-dev.yaml（带 Profile 的专属配置）**

```yaml
# DataId: config-client-dev.yaml
# Group: DEFAULT_GROUP
app:
  name: 配置中心客户端
  env: development

order:
  timeout: 5000
  max-retry: 3
  payment-timeout: 30000

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/config_dev?useSSL=false
    username: dev_user
    password: dev_password
```

**配置 2：config-client.yaml（无 Profile 的基础配置）**

```yaml
# DataId: config-client.yaml
# Group: DEFAULT_GROUP
app:
  name: 配置中心客户端
  version: 1.0.0

server:
  port: 8081

management:
  endpoints:
    web:
      exposure:
        include: '*'
```

**配置 3：common-redis.yaml（共享配置）**

```yaml
# DataId: common-redis.yaml
# Group: SHARED_GROUP
spring:
  data:
    redis:
      host: 192.168.1.100
      port: 6379
      database: 0
      timeout: 3000
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
```

**配置 4：common-logging.yaml（共享日志配置）**

```yaml
# DataId: common-logging.yaml
# Group: SHARED_GROUP
logging:
  level:
    root: INFO
    com.example: DEBUG
  file:
    path: /var/log/apps
    name: ${spring.application.name}.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.5 bootstrap.yml

```yaml
spring:
  application:
    name: config-client

  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true
        timeout: 5000

        # 共享配置
        shared-configs:
          - data-id: common-redis.yaml
            group: SHARED_GROUP
            refresh: true
          - data-id: common-logging.yaml
            group: SHARED_GROUP
            refresh: true

      discovery:
        server-addr: 127.0.0.1:8848
        namespace: public
```

### 5.6 启动类

```java
package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ConfigClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigClientApplication.class, args);
    }
}
```

### 5.7 @ConfigurationProperties 配置类

```java
package com.example.config.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "order")
public class OrderConfig {

    private Integer timeout;
    private Integer maxRetry;
    private Integer paymentTimeout;
    private Map<String, String> rules;

    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }

    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }

    public Integer getPaymentTimeout() { return paymentTimeout; }
    public void setPaymentTimeout(Integer paymentTimeout) { this.paymentTimeout = paymentTimeout; }

    public Map<String, String> getRules() { return rules; }
    public void setRules(Map<String, String> rules) { this.rules = rules; }

    @Override
    public String toString() {
        return "OrderConfig{timeout=" + timeout +
                ", maxRetry=" + maxRetry +
                ", paymentTimeout=" + paymentTimeout +
                ", rules=" + rules + "}";
    }
}
```

```java
package com.example.config.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceConfig {

    private String url;
    private String username;
    private String password;
    private String driverClassName;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }

    @Override
    public String toString() {
        return "DataSourceConfig{url='" + url + "', username='" + username + "'}";
    }
}
```

### 5.8 Controller

```java
package com.example.config.controller;

import com.example.config.config.DataSourceConfig;
import com.example.config.config.OrderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RefreshScope
public class ConfigController {

    @Value("${app.name:unknown}")
    private String appName;

    @Value("${app.env:unknown}")
    private String appEnv;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Value("${spring.data.redis.host:unknown}")
    private String redisHost;

    @Value("${spring.data.redis.port:0}")
    private Integer redisPort;

    @Value("${logging.level.com.example:unknown}")
    private String logLevel;

    private final OrderConfig orderConfig;
    private final DataSourceConfig dataSourceConfig;

    public ConfigController(OrderConfig orderConfig, DataSourceConfig dataSourceConfig) {
        this.orderConfig = orderConfig;
        this.dataSourceConfig = dataSourceConfig;
    }

    /**
     * 获取所有动态配置
     */
    @GetMapping("/config/all")
    public Map<String, Object> getAllConfig() {
        Map<String, Object> config = new HashMap<>();

        // 应用基础信息
        config.put("app.name", appName);
        config.put("app.env", appEnv);
        config.put("app.version", appVersion);

        // Redis 配置（来自共享配置）
        config.put("redis.host", redisHost);
        config.put("redis.port", redisPort);

        // 日志配置（来自共享配置）
        config.put("log.level.com.example", logLevel);

        // 订单配置（来自 @ConfigurationProperties）
        config.put("orderConfig", orderConfig.toString());

        // 数据源配置（来自 @ConfigurationProperties）
        config.put("datasource", dataSourceConfig.toString());

        // 时间戳（用于验证实时刷新）
        config.put("timestamp", System.currentTimeMillis());

        return config;
    }

    /**
     * 获取单个配置项
     */
    @GetMapping("/config/{key}")
    public Map<String, Object> getConfigItem(@org.springframework.web.bind.annotation.PathVariable String key) {
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("timestamp", System.currentTimeMillis());

        switch (key) {
            case "app.name" -> result.put("value", appName);
            case "app.env" -> result.put("value", appEnv);
            case "orderConfig" -> result.put("value", orderConfig.toString());
            case "datasource" -> result.put("value", dataSourceConfig.toString());
            default -> result.put("value", "unknown key");
        }
        return result;
    }
}
```

### 5.9 配置变更监听器

```java
package com.example.config.listener;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class NacosConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigListener.class);

    private final NacosConfigManager nacosConfigManager;

    public NacosConfigListener(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @PostConstruct
    public void init() {
        // 监听专属配置
        registerListener("config-client-dev.yaml", "DEFAULT_GROUP");
        // 监听共享配置
        registerListener("common-redis.yaml", "SHARED_GROUP");
    }

    private void registerListener(String dataId, String group) {
        try {
            nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("配置变更检测 - dataId: {}, group: {}", dataId, group);
                    log.info("新配置内容:\n{}", configInfo);
                }
            });
            log.info("已注册配置监听器 - dataId: {}, group: {}", dataId, group);
        } catch (NacosException e) {
            log.error("注册配置监听器失败 - dataId: {}, group: {}", dataId, group, e);
        }
    }
}
```

### 5.10 验证步骤

```bash
# 1. 准备阶段：确认 Nacos 已启动
# 2. 在 Nacos 控制台创建上述 4 个配置

# 3. 以 dev profile 启动客户端
java -jar config-client/target/config-client-1.0.0.jar --spring.profiles.active=dev

# 4. 验证配置加载
curl http://localhost:8081/config/all
# 预期返回所有配置项（包含 dev 环境专属配置）

# 5. 测试动态刷新
# 在 Nacos 控制台修改 config-client-dev.yaml 中 order.timeout = 8000
# 等待约 5 秒（长轮询间隔）
curl http://localhost:8081/config/orderConfig
# 预期返回 timeout=8000

# 6. 验证共享配置
# 修改 common-redis.yaml 中的 redis.host
curl http://localhost:8081/config/all
# 预期返回更新后的 redis.host
```

### 5.11 配置优先级实验

```bash
# 实验一：同时修改多个配置源的值，观察最终生效的值
# 1. config-client-dev.yaml 中设置 app.version=dev-version
# 2. config-client.yaml 中设置 app.version=base-version
# 3. 调用 /config/all 观察 app.version 的值 = dev-version（profile 配置优先级更高）

# 实验二：切换 Profile
# 停止当前应用，以 prod profile 启动：
java -jar config-client-1.0.0.jar --spring.profiles.active=prod
# 此时应加载 config-client-prod.yaml（如果存在），app.env = production

# 实验三：移除本地 application.yml 中的默认值，验证远程配置完全接管
```

---

## 6. 分层综合习题

### 6.1 基础题

1. **Nacos Config 的三级模型是什么？请用实际例子说明 dev 环境下 DataId 的拼接规则。**

2. **写出 Spring Boot 集成 Nacos Config 并开启动态刷新所需的最小配置（bootstrap.yml）。**

3. **`@RefreshScope` 的作用是什么？不加这个注解时，修改 Nacos 配置后会发生什么？**

4. **什么是 shared-configs？它与服务专有配置有什么关系？**

### 6.2 进阶题

1. **Nacos 配置的完整优先级顺序是怎样的？从最高优先级到最低优先级列出所有级别。**

2. **长轮询（Long Polling）是如何工作的？与短轮询相比有什么优势？写一段伪代码说明长轮询的工作原理。**

3. **配置灰度发布的适用场景有哪些？灰度配置验证通过后的标准操作流程是什么？**

4. **Nacos Config 中 Namespace 和 Group 的设计差异是什么？** 如果一个项目有 3 个环境（dev/test/prod）和 2 个业务线（电商/支付），应该如何设计 Namespace 和 Group 的结构？

### 6.3 精通题

1. **设计一个多租户的 Nacos Config 架构方案，要求：**
   - 租户 A 和租户 B 之间的配置完全隔离
   - 每个租户有 dev/prod 环境
   - 公共配置（如日志、监控）在租户间共享
   - 写出 Namespace 设计、配置分组方案和客户端配置

2. **从源码角度分析 Nacos Config 长轮询机制的实现。** 当客户端发起长轮询请求后，服务端在什么情况下会立即返回？超时机制是如何实现的？

3. **如果你负责将公司从 Apollo 迁移到 Nacos Config，请列出迁移清单并评估风险。** 哪些 Apollo 特性是 Nacos 没有的？需要如何补偿？

4. **Nacos Config 的 `configListenExecutor` 线程池如何配置？当配置变更频繁时，如何防止监听处理逻辑成为性能瓶颈？**

---

## 7. 本章复盘速记清单

### 7.1 核心注解

| 注解 | 作用 | 使用位置 |
|------|------|---------|
| `@RefreshScope` | 标记 Bean 支持配置动态刷新 | 需要动态刷新配置的类 |
| `@ConfigurationProperties` | 批量绑定配置到 Java 对象 | 配置类 |
| `@Value` | 注入单个配置项 | 字段 / 方法参数 |
| `@PropertySource` | 导入额外配置源 | 配置类 |

### 7.2 核心配置项

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `spring.cloud.nacos.config.server-addr` | Nacos 服务端地址 | `127.0.0.1:8848` |
| `spring.cloud.nacos.config.namespace` | 命名空间 | `public` |
| `spring.cloud.nacos.config.group` | 分组 | `DEFAULT_GROUP` |
| `spring.cloud.nacos.config.file-extension` | 配置文件格式 | `yaml` |
| `spring.cloud.nacos.config.prefix` | DataId 前缀 | `${spring.application.name}` |
| `spring.cloud.nacos.config.refresh-enabled` | 是否开启动态刷新 | `true` |
| `spring.cloud.nacos.config.shared-configs` | 共享配置列表 | `[{dataId, group, refresh}]` |
| `spring.cloud.nacos.config.extension-configs` | 扩展配置列表 | `[{dataId, group, refresh}]` |

### 7.3 配置优先级速查

| 优先级 | 配置源 | 示例 |
|--------|--------|------|
| 最高 | 命令行参数 | `--app.timeout=5000` |
| | `{app}-{profile}.yml` | `user-service-dev.yml` |
| | `{app}.yml` | `user-service.yml` |
| | extension-configs | 按列表顺序 |
| | shared-configs | 按列表顺序 |
| 最低 | 本地 application.yml | |

### 7.4 三级模型速查

| 层级 | 用途 | 默认值 | 隔离级别 |
|------|------|--------|---------|
| Namespace | 环境/租户隔离 | `public` | 完全隔离 |
| Group | 业务线/项目分组 | `DEFAULT_GROUP` | 逻辑隔离 |
| DataId | 配置唯一标识 | `{app}-{profile}.{ext}` | 唯一标识 |

### 7.5 长轮询 vs 短轮询

| 对比维度 | 长轮询 (Long Polling) | 短轮询 (Polling) |
|---------|----------------------|-----------------|
| 请求频率 | 30s 一次（超时后立即再发） | 比如 3s 一次 |
| 实时性 | 高（秒级感知） | 取决于轮询间隔 |
| 服务端压力 | 低（连接保持，减少请求数） | 高（频繁创建连接） |
| 网络开销 | 低 | 高 |
| 实现复杂度 | 中（需要连接超时管理） | 低 |

---

## 8. 精通拓展补充-P2

### 8.1 长轮询机制源码分析

Nacos Config 客户端通过 `ClientWorker` 类实现长轮询。

```java
// ClientWorker 核心源码片段
public class ClientWorker implements Closeable {

    // 长轮询线程池
    private final ScheduledExecutorService executor;

    // 配置变更监听器管理
    private final ConfigChangeBatchListenTask configChangeListenTask;

    public ClientWorker(final ConfigHttpClientManager configHttpClientManager) {
        // 创建定时调度线程池
        this.executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors(), threadFactory);

        // 核心：启动长轮询定时任务
        executor.scheduleWithFixedDelay(() -> {
            try {
                // 检查本地配置是否有变更
                checkLocalConfig();

                // 执行长轮询请求
                List<String> changedGroupKeys = checkUpdateDataIds();

                // 如果有变更，触发监听器
                if (!CollectionUtils.isEmpty(changedGroupKeys)) {
                    for (String groupKey : changedGroupKeys) {
                        String[] keys = groupKey.split("\\+");
                        String dataId = keys[0];
                        String group = keys[1];
                        // 获取最新配置
                        String content = getConfigInner(dataId, group, timeout, false);
                        // 通知监听器
                        configListeners.forEach(listener ->
                                listener.receiveConfigInfo(content));
                    }
                }
            } catch (Exception e) {
                log.error("长轮询执行异常", e);
            }
        }, 1L, 30L, TimeUnit.SECONDS);  // 每 30s 执行一次
    }

    // 核心：检查服务端配置是否有变更
    private List<String> checkUpdateDataIds() throws Exception {
        // 构建监听请求：携带所有监听中的 DataId+Group 和它们的 MD5 值
        Map<String, String> listenContext = buildListenContext();

        // 发送长轮询请求
        // POST /nacos/v1/cs/listener?Listening-Configs={...}
        // 服务端会 hold 住请求最多 30s
        // 期间如果有任何配置变更，立即返回变更的 key
        // 如果没有变更，30s 超时后返回空列表
        HttpResult result = configHttpClientManager.post(
                url, headers, params, timeout);
        return parseListenResult(result.content);
    }
}
```

服务端 `ConfigServletInner` 处理长轮询请求：

```java
// 服务端长轮询处理
public String doPollingConfig(HttpServletRequest request, HttpServletResponse response,
        Map<String, Object> clientMd5Map, int probeRequestSize)
        throws Exception {

    // 1. 对比客户端传过来的 MD5 和当前配置的 MD5
    //    如果不同，说明配置已变更，立即返回
    List<String> changedKeys = checkMd5Diff(clientMd5Map);

    if (!changedKeys.isEmpty()) {
        // 有变更，立即返回
        return generateResponse(changedKeys);
    }

    // 2. 无变更，将请求加入长轮询队列
    //    添加一个调度任务：在 29.5s 后超时返回
    String clientIp = RequestUtil.getRemoteIp(request);
    allSubsService.addLongPollingClient(clientIp, request, response, 29500);

    return null;  // 返回值为空，后续通过异步方式返回
}
```

### 8.2 Nacos Config 事件驱动模型

```
配置变更事件流：

1. NacosConfigService.publishConfig(dataId, group, content)
       │
       ▼
2. ConfigChangePublisher.notifyConfigChange(configChangeEvent)
       │
       ▼
3. AsyncNotifyService（异步通知所有客户端）
       │
       ├── HTTP 方式：逐个通知客户端
       └── gRPC 方式：批量推送（Nacos 2.x）

4. 客户端收到通知
       │
       ├── 触发长轮询立即返回
       └── 获取最新配置 → 刷新 Bean
```

### 8.3 Nacos Config 客户端配置体系源码

```java
// NacosConfigProperties —— 所有配置的绑定入口
@ConfigurationProperties(NacosConfigProperties.PREFIX)
public class NacosConfigProperties {

    public static final String PREFIX = "spring.cloud.nacos.config";

    // 服务端地址
    private String serverAddr;

    // 命名空间
    private String namespace;

    // 分组
    private String group = "DEFAULT_GROUP";

    // 配置格式
    private String fileExtension = "properties";

    // 超时时间
    private int timeout = 3000;

    // 最大重试次数
    private int maxRetry = 3;

    // 动态刷新开关
    private boolean refreshEnabled = true;

    // 共享配置列表
    private List<Config> sharedConfigs;

    // 扩展配置列表
    private List<Config> extensionConfigs;

    // 内部配置类
    public static class Config {
        private String dataId;
        private String group = "DEFAULT_GROUP";
        private boolean refresh = false;

        // getters and setters ...
    }

    // getters and setters ...
}
```

### 8.4 Nacos Config 与 Spring Environment 的集成

Nacos Config 通过实现 Spring 的 `PropertySourceLocator` 接口，将 Nacos 中的配置注入到 Spring Environment。

```java
// NacosPropertySourceLocator —— 核心集成类
public class NacosPropertySourceLocator implements PropertySourceLocator {

    private final NacosConfigProperties nacosConfigProperties;

    @Override
    public PropertySource<?> locate(Environment env) {
        // 1. 构建复合 PropertySource
        CompositePropertySource composite = new CompositePropertySource("NACOS");

        // 2. 加载共享配置（低优先级）
        loadSharedConfigurations(composite, env);

        // 3. 加载扩展配置
        loadExtensionConfigurations(composite, env);

        // 4. 加载服务专有配置（高优先级）
        //    先加载不带 Profile 的配置
        loadApplicationData(composite, env, null);
        //    再加载带 Profile 的配置（覆盖前面的值）
        loadApplicationData(composite, env, env.getActiveProfiles());

        // 5. 返回合并后的配置源
        return composite;
    }
}
```

### 8.5 设计模式运用

| 设计模式 | 应用位置 | 说明 |
|---------|---------|------|
| 工厂模式 | `NacosConfigService` | 创建不同类型的 ConfigService 实现 |
| 观察者模式 | `ConfigChangeListener` | 配置变更时通知所有监听器 |
| 装饰器模式 | `NacosPropertySource` | 对 PropertySource 进行装饰增强 |
| 策略模式 | 长轮询 vs gRPC 推送 | 不同的配置获取策略 |
| 模板方法模式 | `AbstractConfigService` | 定义配置管理的标准流程 |
| 适配器模式 | `PropertySourceLocator` | 适配 Spring Environment 接口 |

### 8.6 生产环境配置建议

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| `spring.cloud.nacos.config.timeout` | 5000 | 配置获取超时时间，避免网络抖动导致启动失败 |
| `nacos.config.retry.time` | 3 | 客户端拉取配置的重试次数 |
| `nacos.config.long-polling.timeout` | 30000 | 长轮询超时时间（ms），不建议修改 |
| `nacos.config.max-age` | 0 | 配置缓存的最大存活时间（ms），0 表示每次都拉取 |
| `nacos.config.enable-remote-sync-config` | true | 启用远程配置同步，确保一致性 |
| `spring.cloud.nacos.config.refresh-enabled` | true | 开启动态刷新，否则配置变更不会自动生效 |

> 🎯 本章学习了 Nacos Config 的完整配置管理体系：三级模型（Namespace/Group/DataId）、动态刷新原理（@RefreshScope + 长轮询）、配置共享与优先级规则、灰度发布流程。掌握这些知识后，你可以在微服务项目中构建一套完整的集中配置管理方案。

---

> 本文档属于 `02-中间件与微服务工程/06-Spring全家桶与微服务` 模块，P1 就业必备层级。下一章：[11-服务调用-OpenFeign与负载均衡](./11-服务调用-OpenFeign与负载均衡.md)。
