# 02 - Nacos 配置中心高阶特性

> 配置管理不等于"把 application.yml 放到 Nacos 里"。灰度发布、配置加密、SPI 插件、长轮询优化——这些才是 Nacos Config 真正的生产级能力。

---

## 📚 目录

1. [配置管理的三层模型](#1-配置管理的三层模型)
2. [动态刷新：@RefreshScope 原理](#2-动态刷新refreshscope-原理)
3. [灰度发布：Beta 发布与 IP 灰度](#3-灰度发布beta-发布与-ip-灰度)
4. [配置加密方案](#4-配置加密方案)
5. [长轮询机制与性能优化](#5-长轮询机制与性能优化)
6. [共享配置与扩展配置](#6-共享配置与扩展配置)
7. [SPI 插件机制](#7-spi-插件机制)
8. [namespace 生产最佳实践](#8-namespace-生产最佳实践)

---

## 1. 配置管理的三层模型

### 1.1 核心概念

```yaml
# Nacos 配置的唯一标识 = namespace + group + dataId

Namespace（命名空间）
  ├── 作用：租户级隔离（生产/开发/测试完全隔离）
  ├── ID 示例：a1b2c3d4-e5f6-7890-abcd-ef1234567890
  └── 典型用法：每个环境一个 namespace

Group（分组）
  ├── 作用：同一 namespace 内的二次分组
  ├── 默认值：DEFAULT_GROUP
  └── 典型用法：按微服务或业务域分组

DataId（配置 ID）
  ├── 作用：单个配置文件标识
  ├── 格式：${prefix}-${spring.profiles.active}.${file-extension}
  └── 示例：order-service-dev.yml
```

### 1.2 DataId 命名约定

```text
标准格式：
  ${spring.application.name}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}

示例：
  order-service-dev.yml      → 订单服务开发环境
  order-service-prod.yml     → 订单服务生产环境
  user-service.yml            → 用户服务（通用配置）
  common-redis.yml            → 公共 Redis 配置（共享配置）
```

---

## 2. 动态刷新：@RefreshScope 原理

### 2.1 工作机制

```text
@RefreshScope 的执行流程：

1. Nacos Client 检测到配置变更
   → 长轮询返回 → 发现 MD5 变了

2. 发布 RefreshEvent
   → ContextRefresher.refresh()

3. 销毁 @RefreshScope Bean 的缓存
   → RefreshScope.refreshAll() → 清空 refreshCache

4. 下次访问时重新创建 Bean
   → @RefreshScope Bean 是 Lazy Proxy
   → 首次访问时才真正初始化，读取最新配置

关键：不是"热更新"已存在的 Bean，而是"下次访问时创建新 Bean"
```

### 2.2 源码核心

```java
// RefreshScope 内部实现（简化版）
@ManagedOperation(description = "refresh all beans in refresh scope")
public void refreshAll() {
    super.destroy();  // 清空缓存中的 bean 实例
    this.context.publishEvent(new RefreshScopeRefreshedEvent());
}

// RefreshScope Bean 是代理对象
// 每次方法调用都通过 scope.get() 获取真实 bean
// scope.get() → 如果缓存中有则返回，没有则创建
```

### 2.3 使用注意事项

```java
// ✅ 正确：@RefreshScope + @Value
@RefreshScope
@RestController
public class ConfigController {
    @Value("${app.timeout:3000}")
    private int timeout;

    @GetMapping("/timeout")
    public int get() { return timeout; }
}

// ✅ 正确：@ConfigurationProperties 天然支持
@ConfigurationProperties(prefix = "app.pay")
@Component
@Data
public class PayProperties {
    private int timeout;
    private String callback;
    // @ConfigurationProperties 的 Bean 自动支持刷新
}

// ❌ 错误：static 变量
@Value("${app.staticVal}")
private static String staticVal;  // 不会被刷新！

// ⚠️ 注意：@RefreshScope 的 Bean 是 Lazy 的
// 如果启动时就需要初始化，配合 @PostConstruct 使用
```

---

## 3. 灰度发布：Beta 发布与 IP 灰度

### 3.1 两种灰度方式

```text
方式 1：Beta 发布（Nacos 控制台操作）
  流程：
    1. 在控制台编辑配置
    2. 选择"Beta 发布"
    3. 输入 Beta 版 IP（如 10.0.1.5）
    4. 发布 → 只有指定 IP 的实例收到新配置
    5. 验证通过后 → "正式发布" → 全量生效
    6. 验证失败 → "停止 Beta" → 回滚

  原理：
    Nacos 在配置变更通知中增加了 betaIps 字段
    Client 收到通知后检查自己是否在 betaIps 列表中
    在列表中 → 读取 beta 版配置
    不在 → 继续用正式配置

方式 2：IP 灰度（API/代码控制）
  → 更灵活，可自定义灰度规则
```

### 3.2 灰度发布流程

```text
时间线：

t0: 正式配置 v1（全量生效）
  ↓
t1: 创建 Beta 配置 v2-beta
  → 指定 IP: 10.0.1.10（金丝雀实例）
  → 只有 10.0.1.10 读到 v2-beta
  → 其他所有实例仍读 v1
  ↓
t2: 观察 10.0.1.10 的日志/监控 30 分钟
  ↓
t3a: ✅ 一切正常 → 点击"正式发布"
  → v2-beta 变为 v2
  → 全量实例生效
  ↓
t3b: ❌ 出现问题 → 点击"停止 Beta"
  → v2-beta 被删除
  → 10.0.1.10 恢复读 v1
  → 零影响范围
```

### 3.3 灰度最佳实践

```text
✅ 生产配置变更必须走灰度流程
✅ 灰度验证至少 30 分钟
✅ 选择典型流量实例做金丝雀（不是低流量实例）
✅ 关注的不只是错误日志，还有业务指标（订单量、支付成功率）
✅ 黄金法则：能灰度的配置才配叫"动态配置"
```

---

## 4. 配置加密方案

### 4.1 为什么需要加密

```text
问题：数据库密码、API Key、密钥等敏感配置不能明文存储在 Nacos

Nacos 配置在 MySQL 中的存储：
  SELECT data_id, content FROM config_info;
  → content 列是明文 TEXT 字段
  → 有数据库访问权限 = 能看到所有敏感配置

解决方案层级：
  L1: Nacos 自带加密插件（基于 AES）
  L2: 阿里云 KMS 集成
  L3: 自定义 SPI 加密插件
```

### 4.2 自定义加密 SPI

```java
// 实现 Nacos 的配置加密 SPI
@NacosPlugin
public class AesConfigEncryption implements ConfigEncryptionPlugin {

    private static final String AES_KEY = System.getenv("NACOS_ENCRYPT_KEY");

    @Override
    public String encrypt(String content) {
        // 加密：明文 → 密文
        return AES.encrypt(content, AES_KEY);
    }

    @Override
    public String decrypt(String content) {
        // 解密：密文 → 明文
        return AES.decrypt(content, AES_KEY);
    }

    @Override
    public String getAlgorithmName() {
        return "aes-256-gcm";
    }
}

// 然后在 Nacos 配置中这样存：
// app.db.password = {cipher}aes-256-gcm:U2FsdGVkX1+xxx
```

### 4.3 分层加密策略

```text
运维层（K8s Secret / Vault）:
  → 加密密钥本身 → 注入到容器的环境变量
  → Nacos 加密密钥绝不能存在 Nacos 配置中！

配置层（Nacos）:
  → 密文存储数据库密码等敏感值
  → 使用 {cipher} 前缀标识加密字段

应用层（Spring Boot）:
  → 启动时使用密钥解密
  → 解密后的值注入到 @Value / @ConfigurationProperties
```

---

## 5. 长轮询机制与性能优化

### 5.1 长轮询 vs 短轮询

```text
短轮询（Nacos 1.x 早期）：
  Client: "配置变了吗？" (每 1s)
  Server: "没变"
  Client: "配置变了吗？" (每 1s)
  Server: "没变"
  → 99% 的请求是浪费

长轮询（Nacos 1.x 后期至今）：
  Client: "配置变了吗？如果有变化请立即告诉我" (timeout=30s)
  Server: (挂起连接，不做响应)
  
  情况 A：30s 内有变化 → Server 立即返回新配置
  情况 B：30s 内无变化 → Server 返回 "没变"，Client 立即发起下一个长轮询
```

### 5.2 长轮询参数调优

```yaml
spring:
  cloud:
    nacos:
      config:
        # 长轮询超时时间（ms）
        # 越大 → 实时性越好，但服务端连接数压力越大
        timeout: 30000  # 默认 30 秒

        # 长轮询间隔：收到响应后多久发起下一次
        config-long-poll-timeout: 30000
```

### 5.3 性能优化建议

```text
场景 1：配置少、实例多
  → 问题：每个实例独立长轮询 → Nacos 连接数 = 实例数
  → 优化：无需优化，这是正常场景

场景 2：配置多（500+）、实例多（1000+）
  → 问题：大量长轮询连接消耗 Nacos 内存和线程
  → 优化方案：
    a. 合并配置：减少 dataId 数量
    b. 共享配置：公共配置只监听一份
    c. Nacos 升级到 2.x（gRPC 替代 HTTP，连接复用）

场景 3：超大规模（10000+ 实例）
  → 优化：使用 Nacos-Sync 做多集群同步
  → 每个集群 ≤ 3000 实例
```

---

## 6. 共享配置与扩展配置

### 6.1 配置优先级

```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      config:
        server-addr: nacos:8848
        namespace: prod-ns-id
        file-extension: yml

        # ===== 共享配置（最低优先级）=====
        shared-configs:
          - data-id: common-redis.yml
            group: COMMON_GROUP
            refresh: true
          - data-id: common-mq.yml
            group: COMMON_GROUP

        # ===== 扩展配置 =====
        extension-configs:
          - data-id: order-datasource.yml
            group: ORDER_GROUP
            refresh: true
```

### 6.2 配置加载顺序（优先级从低到高）

```text
优先级（低 → 高）：

1. shared-configs（共享配置）
   common-redis.yml, common-mq.yml

2. extension-configs（扩展配置）
   order-datasource.yml

3. 应用专属配置（最高优先级）
   ${spring.application.name}-${profile}.yml
   order-service-prod.yml

4. 命令行参数 / 环境变量
   --app.timeout=5000

同优先级的配置：后面的覆盖前面的
```

---

## 7. SPI 插件机制

### 7.1 Nacos 的 SPI 扩展点

```text
Nacos 主要通过 Java SPI 机制暴露扩展点，
关键扩展接口：

1. 配置管理扩展
   ConfigFilter           → 配置过滤器（加解密、格式转换）
   ConfigPublishInterceptor → 配置发布拦截器

2. 服务发现扩展
   ServiceInstanceFilter  → 实例过滤器
   HealthCheckProcessor   → 自定义健康检查

3. 鉴权扩展
   AuthPluginService      → 自定义认证方式

4. 数据源扩展
   DataSourcePlugin       → 自定义存储（默认 Derby/MySQL）
```

### 7.2 配置过滤器示例

```java
// 自定义配置过滤器：自动脱敏日志中的敏感配置
@NacosPlugin
public class SensitiveConfigFilter implements ConfigFilter {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "secret", "api-key", "token"
    );

    @Override
    public void doFilter(ConfigRequest request, ConfigResponse response,
                         ConfigFilterChain chain) throws NacosException {
        // 继续链路
        chain.doFilter(request, response);

        // 后置处理：日志脱敏
        if (response != null && response.getContent() != null) {
            String masked = maskSensitive(response.getContent());
            // 不修改实际返回值，只在日志中脱敏
            log.debug("Config content (masked): {}", masked);
        }
    }

    private String maskSensitive(String content) {
        for (String key : SENSITIVE_KEYS) {
            content = content.replaceAll(
                key + "\\s*[:=]\\s*\\S+",
                key + ": ***");
        }
        return content;
    }

    @Override
    public int getOrder() { return 0; }
}
```

---

## 8. namespace 生产最佳实践

### 8.1 标准 namespace 规划

```text
推荐的四层 namespace 设计：

prod (生产环境)
  ├── 独立 Nacos 集群（物理隔离）
  ├── 独立 MySQL 集群
  └── 权限：只读账号给开发，读写给运维

staging (预发布环境)
  ├── 共享 Nacos 集群（可和 dev 共用）
  ├── 独立的 namespace
  └── 配置与 prod 保持同步（定期 diff）

dev (开发环境)
  ├── 共享 Nacos 集群
  ├── 每人/每组一个 namespace（可选）
  └── 配置自由修改

test (测试环境)
  ├── 共享 Nacos 集群
  └── 自动化测试专用 namespace
```

### 8.2 生产 Checklist

```text
□ namespace 必须分环境（prod/dev/test 至少三个）
□ 生产 Nacos 独立部署（不和开发共用）
□ 生产 Nacos 开启鉴权（nacos.core.auth.enabled=true）
□ 生产配置变更走灰度流程
□ 敏感配置加密存储
□ Nacos 集群 ≥ 3 节点
□ MySQL 做主从/集群
□ 配置变更日志保留 ≥ 90 天
□ Nacos 控制台不暴露到公网
□ 定期备份 MySQL nacos_config 库
```

---

> 🎯 **核心要点**：Nacos 配置中心的三个杀手特性——灰度发布（零风险变更）、配置加密（安全合规）、长轮询（实时性 + 低开销）。生产环境中，namespace 分层 + 灰度发布流程是"必须做"的基础设施规范，不是可选项。

---

**下一模块**：[03 - Sentinel 核心原理与扩展机制](./03-Sentinel核心原理与扩展机制.md)  
**返回总览**：[00 - 组件体系总览](./00-SpringCloudAlibaba组件体系总览.md)
