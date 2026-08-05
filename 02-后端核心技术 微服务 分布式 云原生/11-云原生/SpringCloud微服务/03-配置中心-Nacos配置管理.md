# 03 - 配置中心：Nacos 配置管理

> 🎯 配置中心让应用"不用重启就能改配置" — Nacos Config 比 Spring Cloud Config + Bus 更简洁：无需 MQ、天然支持动态刷新、控制台可视化编辑

---

## 目录

1. [Nacos 配置中心概述](#1-nacos-配置中心概述)
2. [基础配置与动态刷新](#2-基础配置与动态刷新)
3. [多环境与共享配置](#3-多环境与共享配置)
4. [配置优先级](#4-配置优先级)
5. [灰度发布与版本回滚](#5-灰度发布与版本回滚)

---

## 1. Nacos 配置中心概述

### 1.1 依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<!-- Spring Boot 3.x 需额外引入 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

### 1.2 为什么要独立配置文件

```yaml
# bootstrap.yml — 在 application.yml 之前加载，用于配置中心连接
spring:
  application:
    name: user-service
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: dev
        group: DEFAULT_GROUP
        file-extension: yaml
```

---

## 2. 基础配置与动态刷新

### 2.1 Data ID 命名规则

```text
Data ID = ${prefix}-${spring.profiles.active}.${file-extension}

默认 prefix = spring.application.name

示例：
  user-service.yaml              ← 无 profile 时
  user-service-dev.yaml          ← profile=dev
  user-service-prod.yaml         ← profile=prod
```

### 2.2 Nacos 控制台配置

```
配置管理 → 配置列表 → 新建配置
  Data ID: user-service-dev.yaml
  Group:   DEFAULT_GROUP
  配置内容:
    app:
      name: 用户服务
    db:
      url: jdbc:mysql://localhost:3306/user_db
      max-pool: 50
```

### 2.3 动态刷新

```java
@RestController
@RefreshScope   // ⭐ 动态刷新 — Nacos 配置变更后自动更新 Bean
public class ConfigController {

    @Value("${db.max-pool}")
    private int maxPool;

    @GetMapping("/config/max-pool")
    public int getMaxPool() {
        return maxPool;      // 修改 Nacos 配置后，值自动更新
    }
}
```

```text
动态刷新原理：
  Nacos 配置变更 → 推送通知 → RefreshEventListener 收到
  → ContextRefresher.refresh() → 重建 @RefreshScope Bean
  → 新的 @Value 值生效
```

---

## 3. 多环境与共享配置

### 3.1 环境隔离

```yaml
# bootstrap-dev.yml — 开发环境
spring:
  cloud:
    nacos:
      config:
        namespace: dev          # 开发命名空间
        group: DEFAULT_GROUP

# bootstrap-prod.yml — 生产环境
spring:
  cloud:
    nacos:
      config:
        namespace: prod         # 生产命名空间
        group: DEFAULT_GROUP
```

### 3.2 共享配置（shared-configs）

```yaml
spring:
  cloud:
    nacos:
      config:
        # 所有环境共享的配置
        shared-configs:
          - data-id: common-config.yaml
            group: DEFAULT_GROUP
            refresh: true        # 是否动态刷新
          - data-id: redis-config.yaml
            group: DEFAULT_GROUP
            refresh: true

        # 扩展配置（优先级高于 shared-configs）
        extension-configs:
          - data-id: datasource.yaml
            group: DEFAULT_GROUP
            refresh: true
```

| 配置类型 | 用途 | 示例 |
|----------|------|------|
| **主配置** | 应用自身的配置 | `user-service-dev.yaml` |
| **shared-configs** | 多服务共享的公共配置 | `common-config.yaml`、`redis-config.yaml` |
| **extension-configs** | 扩展配置，优先级高于 shared | `datasource.yaml`（覆盖共享配置） |

---

## 4. 配置优先级

```text
配置加载优先级（从高到低）：

1. 命令行参数（--db.url=xxx）
2. Nacos extension-configs（后加载的覆盖前面的）
3. Nacos shared-configs
4. Nacos 主配置（${spring.application.name}-${profile}.yaml）
5. application.yml（本地）
6. bootstrap.yml（本地）

示例：如果 Nacos 配置和本地 application.yml 都定义了 db.url
  → Nacos 的值会覆盖本地（远程优先）
```

---

## 5. 灰度发布与版本回滚

### 5.1 灰度发布

```text
场景：修改配置 → 先让部分实例生效 → 验证通过 → 全量发布

Nacos 灰度配置流程：
  1. 创建灰度配置（Beta 发布） → 选择目标 IP
  2. 仅灰度 IP 的实例收到新配置
  3. 验证通过 → 正式发布 → 全量生效
  4. 验证失败 → 停止 Beta → 回滚到原配置
```

### 5.2 版本回滚

```
Nacos 控制台 → 配置管理 → 历史版本
  → 查看历史版本列表
  → 回滚到指定版本（一键恢复）
```

> 🎯 **最佳实践**：生产环境 namespace 隔离、shared-configs 提取公共配置、@RefreshScope 标注需要动态刷新的 Bean、修改配置后先在灰度实例验证。
