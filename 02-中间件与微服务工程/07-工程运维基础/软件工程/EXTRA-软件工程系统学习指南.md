# 软件工程 系统学习指南

> **优先级：★★★★☆ | 求职权重：中高 | 推荐耗时：实战项目中边做边学**
>
> 软件工程是"怎么写好代码"的学问——设计模式、UML、敏捷开发、CI/CD 都是企业级开发的必备能力。本文档从 Java 后端实战出发，梳理软件工程核心知识。

---

## 目录

1. [软件生命周期](#1-软件生命周期)
2. [设计模式](#2-设计模式)
3. [UML 建模](#3-uml-建模)
4. [敏捷开发](#4-敏捷开发)
5. [代码质量与重构](#5-代码质量与重构)
6. [CI/CD 与 DevOps](#6-cicd-与-devops)
7. [后端开发关联](#7-后端开发关联)
8. [推荐资源](#8-推荐资源)

---

## 1. 软件生命周期

### 1.1 经典模型

```
需求分析 → 系统设计 → 编码实现 → 测试验证 → 部署维护
```

### 1.2 开发模型对比

| 模型 | 特点 | 适用场景 |
|------|------|----------|
| **瀑布模型** | 阶段线性推进，不可逆 | 需求明确、变更少的项目 |
| **V 模型** | 开发与测试并行设计 | 对质量要求极高的系统 |
| **迭代模型** | 分轮迭代，逐步完善 | 需求不完全明确 |
| **螺旋模型** | 加入风险分析 | 大型高风险项目 |
| **敏捷（Agile）** | 短周期、适应变化 | **互联网/创业型项目（主流）** |

### 1.3 软件开发阶段产出物

| 阶段 | 产出物 | 角色 |
|------|--------|------|
| 需求分析 | 需求规格说明书（SRS） | 产品经理 / BA |
| 系统设计 | 架构设计文档、接口文档、数据库 ER 图 | 架构师 / 高级开发 |
| 编码实现 | 源代码、单元测试 | 开发工程师 |
| 测试 | 测试用例、测试报告、缺陷报告 | QA / 测试工程师 |
| 部署 | 部署文档、运维手册 | DevOps / SRE |
| 维护 | 变更记录、运维日志 | 开发工程师 / SRE |

---

## 2. 设计模式

### 2.1 设计模式分类（GoF 23 种）

| 类型 | 模式 | 一句话说明 |
|------|------|-----------|
| **创建型** | 单例（Singleton） | 全局唯一实例 |
| | 工厂方法（Factory Method） | 子类决定创建什么 |
| | 抽象工厂（Abstract Factory） | 创建一系列相关对象 |
| | 建造者（Builder） | 分步构建复杂对象 |
| | 原型（Prototype） | 克隆已有对象 |
| **结构型** | 适配器（Adapter） | 不兼容接口协同工作 |
| | 装饰器（Decorator） | 动态添加功能 |
| | 代理（Proxy） | 控制访问 |
| | 外观（Facade） | 简化复杂子系统接口 |
| | 桥接（Bridge） | 分离抽象与实现 |
| | 组合（Composite） | 树形结构的统一处理 |
| | 享元（Flyweight） | 共享减少内存 |
| **行为型** | 观察者（Observer） | 一对多通知（发布-订阅） |
| | 策略（Strategy） | 算法族，可互换 |
| | 模板方法（Template Method） | 定义算法骨架，子类补细节 |
| | 责任链（Chain of Responsibility） | 请求沿链传递直到处理 |
| | 状态（State） | 行为随状态变化 |
| | 命令（Command） | 请求封装为对象 |

### 2.2 Java / Spring 中设计模式实例

| 设计模式 | Spring 中的体现 |
|----------|----------------|
| 单例 | Spring Bean 默认作用域（`@Scope("singleton")`） |
| 工厂方法 | `BeanFactory`、`FactoryBean` 接口 |
| 代理 | AOP 动态代理（JDK 代理 / CGLIB） |
| 模板方法 | `JdbcTemplate`、`RestTemplate` |
| 观察者 | `ApplicationEvent` + `@EventListener` |
| 策略 | `PlatformTransactionManager` 的不同实现 |
| 责任链 | `FilterChain`（Servlet） |
| 适配器 | `HandlerAdapter`（Spring MVC） |

**单例模式最佳实践（双重检查锁）：**
```java
public class Singleton {
    // volatile 防止指令重排（JDK 5+）
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null)
                    instance = new Singleton();
            }
        }
        return instance;
    }
}

// 现代推荐：枚举单例（线程安全 + 防反射 + 防序列化）
public enum Singleton {
    INSTANCE;
    public void doSomething() { /* ... */ }
}
```

**策略模式示例：**
```java
// 定义策略接口
interface PayStrategy {
    void pay(BigDecimal amount);
}
// 具体策略
class AliPayStrategy implements PayStrategy { /* ... */ }
class WechatPayStrategy implements PayStrategy { /* ... */ }

// Spring 中注入所有策略实现
@Service
public class PayService {
    private final Map<String, PayStrategy> strategyMap;

    public PayService(List<PayStrategy> strategies) {
        // Spring 会自动注入所有 PayStrategy 的实现
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(
                s -> s.getClass().getSimpleName(),
                Function.identity()
            ));
    }
}
```

---

## 3. UML 建模

### 3.1 常用图

| 图类型 | 用途 | 会画指数 |
|--------|------|----------|
| **类图** | 描述类之间的关系 | ★★★★★ |
| **时序图** | 描述对象间的交互顺序 | ★★★★★ |
| 用例图 | 描述系统功能 | ★★★☆☆ |
| 活动图 | 描述业务流程/算法流 | ★★★☆☆ |
| 状态图 | 描述对象生命周期 | ★★★☆☆ |
| 组件图 | 描述系统组件依赖 | ★★★☆☆ |

### 3.2 类图关系

```
┌─────────────────────────────────────────────────────┐
│  关系强度：依赖 —◁ 关联 —◁ 聚合 —◁ 组合 —◁ 继承    │
│  虚线 —→ —→ 实线 ——→ 空心◇——→ 实心◆——→ 空心△——→    │
└─────────────────────────────────────────────────────┘
```

| 关系 | 表示 | 说明 | Java 对应 |
|------|------|------|-----------|
| 泛化（继承） | —▷ | is-a | `extends` |
| 实现 | - - ▷ | 实现接口 | `implements` |
| 关联 | —→ | 类之间有关联 | 成员变量 |
| 聚合 | ◇—→ | has-a（整体-部分，部分可独立存在） | 成员（List/Set） |
| 组合 | ◆—→ | contains-a（部分不可独立存在） | 成员（生命周期绑定） |
| 依赖 | - - → | 方法参数/局部变量中使用 | 方法参数 |

### 3.3 时序图示例（用户登录）

```
  Client          AuthController      AuthService       UserRepository    Database
   │                    │                  │                  │               │
   │── POST /login ───►│                  │                  │               │
   │                    │── login(req) ───►│                  │               │
   │                    │                  │── findByPhone ──►│               │
   │                    │                  │                  │── SELECT ────►│
   │                    │                  │                  │◄── User ──────│
   │                    │                  │◄── User ────────│               │
   │                    │                  │── 校验密码+生成Token              │
   │                    │◄── Token ───────│                  │               │
   │◄── 200 {token} ───│                  │                  │               │
```

### 3.4 画图工具

| 工具 | 类型 | 推荐度 |
|------|------|--------|
| **PlantUML** | 文本生成图（代码化，Git 友好） | ★★★★★ |
| **draw.io** | 在线免费，拖拽式 | ★★★★☆ |
| Mermaid | 文本生成图，GitHub 原生支持 | ★★★★☆ |
| StarUML | 桌面应用，功能更全 | ★★★☆☆ |

**PlantUML 时序图代码：**
```
@startuml
Client -> AuthController: POST /login
AuthController -> AuthService: login(req)
AuthService -> UserRepository: findByPhone(phone)
UserRepository -> Database: SELECT ...
Database --> UserRepository: User
UserRepository --> AuthService: User
AuthService -> AuthService: 校验 + 生成Token
AuthService --> AuthController: Token
AuthController --> Client: 200 {token}
@enduml
```

---

## 4. 敏捷开发

### 4.1 Scrum 框架

```
┌──────────────── Sprint (1-4 周) ──────────────────┐
│                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │ Product   │  │  Sprint  │  │  Sprint   │         │
│  │ Backlog   │→ │  Backlog │→ │  执行     │→ 可交付增量│
│  └──────────┘  └──────────┘  └──────────┘         │
│                                │                   │
│                        每日站会 ◄┘                   │
│                                                    │
│  结束后：Sprint Review（演示） + Retrospective（回顾） │
└────────────────────────────────────────────────────┘
```

### 4.2 关键角色

| 角色 | 职责 |
|------|------|
| **Product Owner** | 定义需求，排列优先级，对 ROI 负责 |
| **Scrum Master** | 维护 Scrum 流程，清除障碍，教练角色 |
| **开发团队** | 自组织，5-9 人，交付可工作的软件 |

### 4.3 看板（Kanban）

**核心原则：** 可视化工作流、限制 WIP（在制品数量）、度量并优化流动。

**看板列：**
```
Backlog → TODO → In Progress → Code Review → Testing → Done
```

### 4.4 敏捷 vs 传统开发

| | 敏捷 | 传统（瀑布） |
|------|------|-------------|
| 需求变化 | 欢迎变化 | 变化成本高 |
| 交付方式 | 持续交付，小步快跑 | 项目结束时一次性交付 |
| 文档 | 够用就好（可工作的软件 > 文档） | 详尽的文档 |
| 计划 | 自适应（滚动规划） | 前期详细计划 |
| 团队 | 自组织跨职能团队 | 角色分明（开发/测试/运维分离） |

---

## 5. 代码质量与重构

### 5.1 SOLID 原则

| 原则 | 说明 |
|------|------|
| **S**单一职责 | 一个类只有一个改变的理由 |
| **O**开闭原则 | 对扩展开放，对修改关闭 |
| **L**里氏替换 | 子类必须能完全替换父类 |
| **I**接口隔离 | 不应强迫类实现不需要的接口 |
| **D**依赖倒置 | 依赖抽象而非具体实现 |

```java
// ❌ 违反 DIP：直接依赖具体实现
public class OrderService {
    private MySQLOrderRepository repo = new MySQLOrderRepository();
}

// ✅ 遵循 DIP：依赖接口
public class OrderService {
    private final OrderRepository repo;
    public OrderService(OrderRepository repo) {  // 构造器注入
        this.repo = repo;
    }
}
```

### 5.2 重构时机

- **三次法则（Rule of Three）：** 第三次做类似的事时重构
- **添加功能困难时** —— 先重构使结构更好扩展
- **Code Review 发现代码异味（Code Smell）时**
- **修复 Bug 时** —— "童子军规"：让营地比来时更干净

### 5.3 常见代码异味与重构手法

| 代码异味 | 重构手法 |
|----------|----------|
| 长方法 | 提取方法（Extract Method） |
| 大类 | 提取类（Extract Class） |
| 过长参数列表 | 引入参数对象（Introduce Parameter Object） |
| 重复代码 | 提取方法/提取到父类 |
| Switch 语句 | 多态替换（配合策略模式） |
| 特性依恋（跨类访问） | 移动方法/移动字段 |

---

## 6. CI/CD 与 DevOps

### 6.1 概念区分

| 缩写 | 全称 | 含义 |
|------|------|------|
| **CI** | 持续集成 | 代码频繁合并到主干，自动化构建和测试 |
| **CD** | 持续交付 | 代码随时可部署到生产环境（需手动确认） |
| **CD** | 持续部署 | 通过测试后自动部署到生产环境（无需人工干预） |

### 6.2 典型 CI/CD 流水线

```
Push 代码 → 触发构建
  → 编译 + 单元测试
  → 代码质量扫描（SonarQube）
  → 构建镜像（Docker build）
  → 推送镜像仓库（Docker Registry / Harbor）
  → 部署到测试环境 + 集成测试
  → 部署到预发环境 + 冒烟测试
  → 人工审批（生产部署卡点）
  → 部署到生产环境（滚动更新 / 蓝绿部署）
```

### 6.3 GitHub Actions 示例（Java 后端 CI）

```yaml
name: Java CI with Maven

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn verify -B
      - name: SonarQube Scan
        run: mvn sonar:sonar -Dsonar.host.url=${{ secrets.SONAR_URL }}
      - name: Build Docker Image
        run: docker build -t myapp:${{ github.sha }} .
      - name: Push to Registry
        run: docker push myapp:${{ github.sha }}
```

### 6.4 部署策略

| 策略 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| 滚动更新（Rolling） | 逐个替换实例 | 零停机 | 回滚慢 |
| 蓝绿部署（Blue-Green） | 新旧两个完整环境切换 | 秒级回滚 | 资源双倍 |
| 金丝雀发布（Canary） | 仅少量实例升级 | 风险最可控 | 实现复杂 |

---

## 7. 后端开发关联

### 7.1 Git 分支策略

**Git Flow：**
```
main ──────●────────────●───────── 生产版本
            \          /
develop ────●──●─────●──●───────── 开发主线
              \  \   /
feature/A      ●──●──●            功能分支
feature/B           ●──●          功能分支
release/1.0            ●──●      发布分支
hotfix/1.0.1                ●    热修复分支
```

**GitHub Flow（简化版）：**
- `main` 分支始终可部署
- 从 `main` 创建功能分支 → PR → Code Review → 合并回 `main` → 自动部署

### 7.2 API 版本管理

```java
// URL 版本化（最常见）
@RestController
@RequestMapping("/api/v1/users")   // v1
@RequestMapping("/api/v2/users")   // v2

// Header 版本化
@GetMapping("/users")
public ResponseEntity<?> getUsers(
    @RequestHeader("API-Version") String version) { ... }
```

### 7.3 技术债务管理

**技术债务分类：**
- 代码债务：命名不规范、重复代码、过长方法
- 架构债务：模块耦合过紧、缺少抽象层
- 测试债务：测试覆盖率低、重要路径缺少测试
- 文档债务：API 文档过期、架构文档缺失

**清偿策略：**
- 每个 Sprint 预留 20% 时间处理技术债务
- 重构与功能开发绑定（修改旧代码时顺手重构）
- 使用 SonarQube 等量化追踪技术债务

---

## 8. 推荐资源

### 书籍
- 《设计模式：可复用面向对象软件的基础》（GoF）——设计模式圣经
- 《重构：改善既有代码的设计》（Martin Fowler）——重构必读
- 《代码整洁之道》（Clean Code）——代码质量入门经典
- 《人月神话》——软件工程管理经典
- 《持续交付》（Jez Humble）——CI/CD 实践指南

### 在线资源
- [Refactoring.Guru](https://refactoring.guru/) —— 设计模式 + 重构图文教程
- [PlantUML](https://plantuml.com/zh/) —— 代码化 UML 图表绘制
- [SonarQube](https://www.sonarsource.com/) —— 代码质量检测

### 工具链
- **项目管理：** Jira、Trello、Linear
- **代码托管：** GitHub、GitLab
- **CI/CD：** GitHub Actions、Jenkins、GitLab CI
- **代码质量：** SonarQube、Checkstyle、SpotBugs

---

> **关联文档：**
> - [[Git结合Java后端企业级开发流程实操]]
> - [[软件交付与CICD全流程详解]]
> - [[Java设计模式期末复习项目——在线图书管理系统]]
> - [[代码整洁之道]]
> - [[从Java后端开发角度深度剖析软件工程 架构驱动的软件开发——软件工程简介]]
