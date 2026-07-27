# 00 - MyBatis 知识体系总览

> 🎯 MyBatis 是 Java 持久层的事实标准——半自动 ORM、动态 SQL、插件拦截器、一级/二级缓存。理解 MyBatis 源码是成为 Java 高级工程师的必经之路

> 🎯 共 **12 篇**，从架构到源码、从 XML 到插件、从 MyBatis-Plus 到 AI 场景

---

## 1. 知识全景

```
MyBatis 体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-核心架构与工作流程.md
│   ├── 02-XML映射文件深度解析.md
│   └── 03-动态SQL实战.md
│
├── 🔧 进阶篇（04-06）
│   ├── 04-MyBatis缓存机制.md
│   ├── 05-插件与拦截器.md
│   └── 06-Spring Boot整合实战.md
│
├── 🚀 深入篇（07-09）
│   ├── 07-MyBatis-Plus快速开发.md
│   ├── 08-MyBatis性能优化.md
│   └── 09-源码导读.md
│
├── 📋 拓展篇（10）
│   └── 10-MyBatis与AI场景结合.md
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | 核心架构 | SqlSession/Executor/MappedStatement | ⭐⭐⭐⭐⭐ |
| 02 | XML映射 | resultMap/association/collection | ⭐⭐⭐⭐⭐ |
| 03 | 动态SQL | if/foreach/choose/where/trim | ⭐⭐⭐⭐⭐ |
| 04 | 缓存机制 | 一级(本地)/二级(全局)/自定义缓存 | ⭐⭐⭐⭐ |
| 05 | 插件拦截器 | Interceptor/分页插件/审计 | ⭐⭐⭐⭐ |
| 06 | Spring Boot整合 | Starter/自动配置/多数据源 | ⭐⭐⭐⭐⭐ |
| 07 | MyBatis-Plus | 代码生成/条件构造器/逻辑删除 | ⭐⭐⭐⭐⭐ |
| 08 | 性能优化 | 批量/延迟加载/N+1问题 | ⭐⭐⭐⭐ |
| 09 | 源码导读 | SqlSessionFactory/Executor/Plugin | ⭐⭐⭐ |
| 10 | AI场景 | 动态SQL生成/LLM数据映射 | ⭐⭐⭐ |
| 11 | 面试考点 | 高频题+源码理解 | ⭐⭐⭐⭐⭐ |

## 3. 核心架构速查

```
MyBatis 核心组件：
  SqlSessionFactory → 创建 SqlSession（类似 JDBC Connection）
  SqlSession → 执行 SQL / 获取 Mapper
  Executor → 真正执行 SQL（Simple/Reuse/Batch）
  MappedStatement → SQL + 映射规则的封装
  TypeHandler → Java 类型 ↔ JDBC 类型 转换
  Interceptor → 拦截 Executor/StatementHandler 等
```

## 4. MyBatis vs JPA/Hibernate

| 维度 | MyBatis | JPA/Hibernate |
|------|:---:|:---:|
| SQL 控制力 | ⭐⭐⭐⭐⭐ 手写 SQL | ⭐⭐ 自动生成 |
| 学习曲线 | ⭐⭐ 简单 | ⭐⭐⭐⭐ 复杂 |
| 复杂查询 | ✅ 擅长 | ❌ 吃力 |
| 自动映射 | 半自动 | 全自动 |
| 中国市场占比 | **70%+** | 30% |
| AI 场景 | SQL 可预测 | SQL 不可预测 |
