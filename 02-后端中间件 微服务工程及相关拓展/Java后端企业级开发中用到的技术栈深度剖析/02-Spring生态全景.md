# 02 - Spring 生态全景

> 定位：IoC/AOP 核心、Spring 家族关系图、Spring Boot、Spring Cloud、常用组件——框架层全景（导览）

## 📚 目录

1. [Spring 家族关系图](#1-spring-家族关系图)
2. [IoC 控制反转](#2-ioc-控制反转)
3. [AOP 面向切面](#3-aop-面向切面)
4. [Spring Boot 定位](#4-spring-boot-定位)
5. [Spring Cloud 定位](#5-spring-cloud-定位)

---

## 1. Spring 家族关系图

```
Spring 生态全景：
  Spring Core（IoC/AOP 基础）
  Spring MVC（Web）
  Spring Boot（自动配置封装）✅ 开发主体
  Spring Data（数据访问：JPA/Redis/Mongo）
  Spring Security（安全）
  Spring Cloud（微服务）
  Spring AI（大模型接入，2025+）

关系：
  Boot 让 Core/MVC/Data/Security 开箱即用
  Cloud 建立在 Boot 之上（微服务）

⚠️ 面试必答：
"Spring 家族三层次——Core（地基）、
 Boot（开发效率）、Cloud（微服务）；
 现代开发 90% 在 Boot 生态内完成。"
```

---

## 2. IoC 控制反转

### 2.1 核心概念

```
IoC（控制反转）= 对象的创建与依赖交给容器管理
DI（依赖注入）= IoC 的实现方式（构造器/字段/Setter）

传统：new 对象（自己控制）
IoC：容器创建 + 注入（反转控制权）

⚠️ 面试必答：
"IoC = 控制权反转——对象创建交给容器、
 依赖自动注入；
 好处：解耦 + 易测试 + 生命周期管理。"
```

### 2.2 Bean 生命周期

```
Bean 生命周期：
  实例化 → 属性填充 → Aware 回调 → BeanPostProcessor
  → 初始化（@PostConstruct）→ 使用 → 销毁（@PreDestroy）

作用域：
  singleton（默认单例）/prototype（每次新建）
  request/session（Web）

⚠️ 面试必答：
"Bean 生命周期关键点——单例默认、
 初始化/销毁钩子、
 BeanPostProcessor（AOP 的植入点）。"
```

### 2.3 循环依赖

```
循环依赖：A 依赖 B、B 依赖 A

三级缓存解决（构造器注入除外）：
  一级：完成 Bean
  二级：早期 Bean（半成品）
  三级：ObjectFactory（代理工厂）

⚠️ 面试必答：
"Spring 三级缓存解决 setter 循环依赖——
 构造器注入无法解决（启动即报错）；
 工程建议：避免循环依赖（设计问题）。"
```

---

## 3. AOP 面向切面

### 3.1 核心概念

```
AOP = 横切逻辑抽取（日志/事务/鉴权）
  切面（Aspect）：横切逻辑集合
  通知（Advice）：Before/After/Around
  切点（Pointcut）：匹配哪些方法
  连接点（JoinPoint）：方法执行点

⚠️ 面试必答：
"AOP 五要素——切面/通知/切点/连接点/织入；
 典型应用：@Transactional、日志、鉴权。"
```

### 3.2 实现原理

```
AOP 两种实现：
  ① JDK 动态代理：接口代理（Proxy）
  ② CGLIB：类代理（继承，无接口时）

⚠️ 经典坑：同类调用不走代理（this 调用）
  → 事务失效/日志不生效

⚠️ 面试必答：
"AOP = 动态代理（JDK 接口 / CGLIB 类）——
 '同类调用失效'是最高频考点
 （this 调用绕过代理）。"
```

---

## 4. Spring Boot 定位

```
Spring Boot = 生态的开箱即用层
  自动配置：依赖即装配（条件注解）
  Starter：一键引入
  内嵌容器：jar 独立运行
  生产就绪：Actuator/监控

⚠️ 面试必答：
"Boot 解决'配置地狱'——自动配置 +
 Starter + 内嵌容器；
 面试核心 = 自动配置原理（imports +
 条件装配）。"
```

> 深入：[SpringBoot 体系](../06-Spring全家桶/SpringBoot/00-SpringBoot总览与核心概念.md)（本仓库 9 篇深度体系）

---

## 5. Spring Cloud 定位

### 5.1 微服务组件

```
Spring Cloud 核心组件（2026 生态）：
  注册发现：Nacos（国产主流）
  配置中心：Nacos
  网关：Spring Cloud Gateway
  负载均衡：Spring Cloud LoadBalancer
  熔断限流：Sentinel
  分布式事务：Seata

⚠️ 面试必答：
"Spring Cloud = 微服务全家桶——
 注册/配置/网关/负载/熔断/事务；
 国产主流组合：Nacos + Gateway + Sentinel + Seata。"
```

### 5.2 服务调用链

```
微服务请求链：
  客户端 → 网关（鉴权/路由）
  → 服务 A →（注册中心发现）→ 服务 B
  → 数据层 + 消息 + 缓存
  全链路：OTel 追踪

⚠️ 面试必答：
"微服务链路 = 网关入口 + 注册发现 +
 服务间调用 + 配置中心 + 链路追踪；
 分布式问题（事务/锁/一致性）是核心考点。"
```

---

> 🎯 **核心要点**：Spring 生态 = **家族关系**（Core/Boot/Cloud 三层）+ **IoC**（容器 + 生命周期 + 三级缓存）+ **AOP**（五要素 + 动态代理）+ **Boot**（自动配置，见深度体系）+ **Cloud**（Nacos/Gateway/Sentinel/Seata 组合）。"IoC + AOP 是地基、Boot 是效率、Cloud 是规模"。

---

**返回总览**：[00-Java后端技术栈总览](00-Java后端技术栈总览.md) | **上一篇**：[01-Java语言核心](01-Java语言核心.md) | **下一篇**：[03-数据访问技术栈](03-数据访问技术栈.md)
