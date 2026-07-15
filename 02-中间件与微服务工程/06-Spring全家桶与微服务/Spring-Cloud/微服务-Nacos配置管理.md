# 微服务 - Nacos 配置管理

> **定位**：Nacos 统一配置管理 = 所有微服务配置集中存储 → 动态更新 → 环境隔离 → 敏感加密。

---

## 1. 传统本地配置痛点 vs Nacos 方案

| 痛点 | Nacos 方案 |
|------|-----------|
| 修改配置需逐服务重启 | 控制台一次修改，秒级动态更新 |
| 多环境配置易混淆 | 命名空间隔离（dev/test/prod） |
| 敏感配置泄露 | 支持加密存储 |
| 维护成本高 | 集中管理，降低 90% 维护成本 |

---

## 2. 核心原理

```text
① 配置发布：控制台创建 → 存到 Nacos DB（Derby/MySQL）→ 同步集群
② 配置拉取：微服务启动 → 读 bootstrap.yml → 拉取远程配置
③ 动态更新：长连接 + 主动推送 → 微服务自动刷新（无需重启）
```

> 优先级：Nacos 远程配置 > bootstrap.yml > application.yml

---

## 3. 实操步骤

### Step 1：Nacos 控制台创建配置

| 参数 | 值 | 说明 |
|------|-----|------|
| Data ID | `user-service-dev.yml` | 格式：`服务名-环境.yml` |
| Group | `DEFAULT_GROUP` | 默认分组 |
| 命名空间 | `dev` | 环境隔离 |

### Step 2：微服务集成

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

```yaml
# bootstrap.yml
spring:
  application:
    name: user-service
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: dev
        group: DEFAULT_GROUP
        file-extension: yml
        refresh-enabled: true
```

```java
@RefreshScope  // 配置动态刷新
@RestController
public class UserController {
    @Value("${spring.redis.host}")
    private String redisHost;
}
```

---

## 4. 进阶用法

| 功能 | 做法 |
|------|------|
| **公共配置复用** | `shared-configs` 配置 `common-dev.yml` |
| **敏感加密** | 明文 → AES 加密 → `cipher:密文` 存储 |
| **刷新范围控制** | `@RefreshScope` 局部刷新 / `refresh-exclude` 排除 |
| **多环境切换** | `--spring.profiles.active=dev/test/prod` |
