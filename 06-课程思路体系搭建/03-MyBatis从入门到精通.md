# MyBatis(2026)从入门到精通

> 从零掌握 MyBatis 框架核心，涵盖框架理解、ORM 映射、CRUD 操作、核心配置、动态代理、参数处理、动态 SQL、高级映射与延迟加载、缓存机制、分页、逆向工程、PageHelper 插件以及注解式开发，贯穿完整实战案例。

## 目录

- [第一章：MyBatis 概述与简介](#第一章mybatis-概述与简介)
- [第二章：MyBatis 入门程序](#第二章mybatis-入门程序)
- [第三章：使用 MyBatis 完成 CRUD](#第三章使用-mybatis-完成-crud)
- [第四章：MyBatis 核心配置文件详解](#第四章mybatis-核心配置文件详解)
- [第五章：在 WEB 中应用 MyBatis](#第五章在-web-中应用-mybatis)
- [第六章：接口代理机制及使用](#第六章接口代理机制及使用)
- [第七章：MyBatis 的缓存](#第七章mybatis-的缓存)
- [第八章：MyBatis 参数处理](#第八章mybatis-参数处理)
- [第九章：MyBatis 查询语句专题](#第九章mybatis-查询语句专题)
- [第十章：动态 SQL](#第十章动态-sql)
- [第十一章：高级映射及延迟加载](#第十一章高级映射及延迟加载)
- [第十二章：MyBatis 分页](#第十二章mybatis-分页)
- [第十三章：MyBatis 的逆向工程](#第十三章mybatis-的逆向工程)
- [第十四章：MyBatis 使用 PageHelper](#第十四章mybatis-使用-pagehelper)
- [第十五章：MyBatis 的注解式开发](#第十五章mybatis-的注解式开发)

---

### 第一章：MyBatis 概述与简介

- `001` MyBatis 的简介
- `002` 对 ORM 的理解

### 第二章：MyBatis 入门程序

- `003` 第一个 MyBatis 程序的准备工作
- `004` 编写 MyBatis 的配置文件
- `005` 完成第一个 MyBatis 程序
- `006` 最终版本的第一个 MyBatis 程序
- `007` MyBatis 集成 Junit 单元测试
- `008` 开启 MyBatis 的标准日志
- `009` 封装 SqlSessionUtil
- `010` IDEA 配置文件模板

### 第三章：使用 MyBatis 完成 CRUD

- `011` 基于 Map 实现保存
- `012` 基于 Entity 实现保存（一）
- `013` 基于 Entity 实现保存（二）
- `014` 根据 id 删除数据
- `015` 根据 id 修改数据
- `016` 查询一条记录
- `017` 查询多条记录
- `018` SqlMapper 中 namespace 的作用

### 第四章：MyBatis 核心配置文件详解

- `019` 核心配置文件中的 environment
- `020` 核心配置文件中的事务管理器
- `021` 核心配置文件中的数据源配置
- `022` 核心配置文件中连接池的属性配置
- `023` 核心配置文件引入外部属性配置文件
- `024` 核心配置文件中 mapper 的配置

### 第五章：在 WEB 中应用 MyBatis

- `025` 将账户转账的案例升级为 MyBatis 版本
- `026` 添加 ThreadLocal 保证事务

### 第六章：接口代理机制及使用

- `027` MyBatis 使用动态代理生成接口实现
- `028` SQL 语句的传值和拼接 SQL
- `029` MyBatis 的模糊查询两种方式
- `030` MyBatis 中的起别名
- `031` MyBatis 中的 mapper 的终极写法
- `032` 插入数据时自动获取生成的主键值

### 第七章：MyBatis 的缓存

- `051` 缓存概述
- `052` MyBatis 的一级缓存
- `053` MyBatis 的二级缓存

### 第八章：MyBatis 参数处理

- `033` MyBatis 对单个简单类型参数的默认处理
- `034` MyBatis 多参数处理及命名参数

### 第九章：MyBatis 查询语句专题

- `035` MyBatis 查询语句专题
- `036` resultMap 标签的使用
- `037` 开启下划线转驼峰自动映射
- `038` 查询总记录条数

### 第十章：动态 SQL

- `039` 动态 SQL 之 if 标签
- `040` 动态 SQL 之 where 标签
- `041` 动态 SQL 之 trim 标签
- `042` 动态 SQL 之 set 标签
- `043` 动态 SQL 之 choose when otherwise
- `044` 动态 SQL 之 foreach
- `045` sql 片段声明与包含

### 第十一章：高级映射及延迟加载

- `046` 高级映射-多对一-级联属性
- `047` 高级映射-多对一-association
- `048` 高级映射-多对一-分步查询-延迟加载
- `049` 高级映射-一对多-collection 方式
- `050` 高级映射-一对多-分步查询-延迟加载

### 第十二章：MyBatis 分页

- `055` MyBatis 实现最基本的分页查询

### 第十三章：MyBatis 的逆向工程

- `054` 使用逆向工程插件

### 第十四章：MyBatis 使用 PageHelper

- `056` 使用 PageHelper 分页插件

### 第十五章：MyBatis 的注解式开发

- `057` MyBatis 的注解式开发
