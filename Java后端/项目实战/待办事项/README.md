# 待办事项 (Todo List)

> Java 后端待办事项管理系统，Spring Boot + RESTful API

## 项目概述

经典的待办事项（Todo List）管理系统，用于实践 Java Web 后端开发全流程。支持任务的增删改查、状态标记、分类筛选等核心功能。项目采用 Spring Boot + MyBatis + MySQL 标准三层架构。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 17+ |
| Spring Boot | 应用框架 |
| Spring MVC | RESTful API |
| MyBatis | ORM 数据库映射 |
| MySQL | 数据持久化 |
| Lombok | 代码简化 |
| Maven | 项目构建 |

## 功能特性

- **任务 CRUD**：创建、编辑、删除、查询待办事项
- **状态切换**：待办 ↔ 进行中 ↔ 已完成
- **优先级**：高/中/低三级优先级
- **分类标签**：工作、学习、生活等分类
- **截止日期**：任务到期提醒
- **排序筛选**：按优先级、日期、状态排序

## 项目结构

```
待办事项/
├── src/main/java/com/todo/
│   ├── TodoApplication.java          # Spring Boot 启动类
│   ├── controller/
│   │   └── TodoController.java       # REST API 接口
│   ├── service/
│   │   ├── TodoService.java          # 业务接口
│   │   └── impl/TodoServiceImpl.java # 业务实现
│   ├── mapper/
│   │   └── TodoMapper.java           # MyBatis 映射
│   ├── model/
│   │   ├── Todo.java                 # 实体类
│   │   └── TodoDTO.java              # 数据传输对象
│   └── config/
│       └── WebConfig.java            # CORS 等配置
├── src/main/resources/
│   ├── application.yml               # 应用配置
│   └── mappers/
│       └── TodoMapper.xml            # SQL 映射文件
├── sql/
│   └── schema.sql                    # 数据库建表脚本
├── pom.xml
└── README.md
```

## 数据库设计

```sql
CREATE TABLE todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority ENUM('HIGH', 'MEDIUM', 'LOW') DEFAULT 'MEDIUM',
    status ENUM('TODO', 'IN_PROGRESS', 'DONE') DEFAULT 'TODO',
    category VARCHAR(50),
    due_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## API 设计

| Method | Path | 说明 |
|--------|------|------|
| GET | /api/todos | 获取全部任务（支持筛选排序） |
| GET | /api/todos/{id} | 获取单个任务 |
| POST | /api/todos | 创建任务 |
| PUT | /api/todos/{id} | 更新任务 |
| PATCH | /api/todos/{id}/status | 切换任务状态 |
| DELETE | /api/todos/{id} | 删除任务 |

## 快速开始

```bash
# 初始化数据库
mysql -u root -p < sql/schema.sql

# 修改 application.yml 中的数据源配置

# 启动应用
mvn spring-boot:run

# 测试 API
curl http://localhost:8080/api/todos
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| Spring Boot 三层架构 | Controller → Service → Mapper 分层 |
| RESTful API 设计 | 资源命名、HTTP 方法语义 |
| MyBatis CRUD | 注解 + XML 混合映射 |
| 全局异常处理 | @ControllerAdvice 统一异常响应 |
| 参数校验 | @Valid + BindingResult |
| 跨域配置 | CORS 全局配置 |
