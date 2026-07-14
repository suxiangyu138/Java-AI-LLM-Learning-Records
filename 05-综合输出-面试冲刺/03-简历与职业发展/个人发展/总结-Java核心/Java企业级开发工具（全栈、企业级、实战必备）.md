# Java企业级开发工具（全栈、企业级、实战必备）

## 一、开发环境（IDE）

### 1. IntelliJ IDEA（企业首选）

- 优势：智能提示、重构、调试、性能分析、Spring生态深度支持
- 版本：Ultimate（企业级，付费）、Community（免费）
- 必备插件：Lombok、MyBatisX、Spring Boot、Docker、GitToolBox

### 2. Eclipse（传统企业）

- 优势：轻量、插件丰富、开源免费
- 适用：老项目维护、Eclipse生态项目

### 3. VS Code（轻量开发）

- 优势：启动快、插件多、跨平台
- 适用：微服务、脚本、前端+后端混合开发

## 二、版本控制（Git生态）

### 1. Git（核心）

- 命令行工具，分布式版本控制
- 企业必备：分支管理、代码合并、冲突解决

### 2. GitLab/GitHub/Gitee（代码托管）

- GitLab：企业私有仓库、CI/CD、权限管理
- GitHub：开源社区、开源项目托管
- Gitee：国内加速、私有仓库

### 3. 图形化工具

- SourceTree、GitKraken、IDEA内置Git

## 三、构建工具（Maven/Gradle）

### 1. Maven（企业主流）

- 核心：依赖管理、项目构建、生命周期
- 配置：pom.xml、镜像仓库（阿里云）
- 插件：spring-boot-maven-plugin、mybatis-generator

### 2. Gradle（新一代）

- 优势：灵活、性能高、DSL语法
- 适用：Spring Boot、微服务、大型项目

## 四、接口调试与文档

### 1. Postman（接口调试）

- 功能：HTTP/HTTPS请求、自动化测试、Mock服务
- 企业级：集合管理、环境变量、协作

### 2. Apifox（一站式）

- 功能：接口调试、文档生成、Mock、自动化测试
- 优势：国产、全链路、团队协作

### 3. Swagger/Knife4j（接口文档）

- 自动生成API文档、在线调试
- 集成Spring Boot，零配置

## 五、数据库工具

### 1. Navicat（企业首选）

- 支持：MySQL、Oracle、PostgreSQL、Redis
- 功能：可视化、数据同步、备份、查询优化

### 2. DBeaver（开源免费）

- 跨平台、支持所有主流数据库
- 优势：免费、插件丰富、性能好

### 3. DataGrip（JetBrains）

- 智能提示、重构、调试、性能分析
- 适合复杂SQL开发

## 六、容器与云原生（Docker/K8s）

### 1. Docker（容器化）

- 核心：镜像、容器、Dockerfile、Compose
- 企业级：容器编排、微服务部署

### 2. Kubernetes（K8s）

- 容器编排、自动扩缩容、高可用
- 工具：kubectl、K9s、Rancher

### 3. Docker Desktop

- 本地Docker环境、K8s集成

## 七、微服务与中间件工具

### 1. Nacos（服务注册/配置）

- 控制台：服务管理、配置管理、动态刷新

### 2. Sentinel（熔断限流）

- 控制台：限流规则、监控、降级策略

### 3. Seata（分布式事务）

- 控制台：事务管理、监控、回查

### 4. Redis Desktop Manager

- Redis可视化、数据操作、监控

## 八、监控与运维

### 1. Arthas（阿里Java诊断）

- 功能：性能监控、线程分析、热更新、问题排查
- 企业必备：线上问题快速定位

### 2. Prometheus + Grafana

- 监控指标、可视化面板、告警

### 3. ELK（日志管理）

- Elasticsearch、Logstash、Kibana
- 日志收集、分析、检索

## 九、测试工具

### 1. JUnit 5（单元测试）

- Spring Boot默认测试框架

### 2. Mockito（Mock测试）

- 模拟依赖、隔离测试

### 3. JMeter（性能测试）

- 接口压测、并发测试、性能分析

## 十、协作与项目管理

### 1. Jira（任务管理）

- 敏捷开发、Bug跟踪、迭代管理

### 2. Confluence（文档协作）

- 知识库、设计文档、接口文档

### 3. 飞书/钉钉/企业微信

- 团队沟通、会议、文件协作

## 十一、代码质量

### 1. SonarQube

- 代码扫描、Bug检测、坏味道、覆盖率

### 2. Alibaba Java Coding Guidelines

- 阿里编码规范插件、自动检查

## 十二、企业级工具链（推荐组合）

- IDE：IntelliJ IDEA Ultimate
- 版本控制：Git + GitLab
- 构建：Maven
- 接口：Apifox + Knife4j
- 数据库：Navicat
- 容器：Docker + Docker Compose
- 监控：Arthas + Prometheus
- 协作：Jira + Confluence + 飞书
