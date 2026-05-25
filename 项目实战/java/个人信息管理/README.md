# 个人信息管理 (Personal Info Management)

> Java 后端个人信息管理系统，用户注册登录 + 信息 CRUD + 文件上传

## 项目概述

个人信息管理系统，实现用户注册登录、个人资料管理、头像上传等功能。采用 Spring Boot + Spring Security 或自定义 Session 机制，是练习用户认证和文件处理的综合项目。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 17+ |
| Spring Boot | 应用框架 |
| Spring MVC | RESTful API |
| MyBatis | ORM 映射 |
| MySQL | 数据库 |
| Spring Security | 认证授权（可选，学习时可用 Session） |
| 文件上传 | MultipartFile + 本地/OSS 存储 |
| Lombok | 代码简化 |
| Maven | 项目构建 |

## 功能特性

- **用户注册**：用户名、密码（加密存储）、邮箱
- **用户登录**：Session / JWT Token 认证
- **个人资料**：查看和编辑个人信息
- **头像上传**：图片上传与裁剪
- **密码修改**：旧密码验证 + 新密码更新
- **账户安全**：登录拦截、Session 过期

## 项目结构

```
个人信息管理/
├── src/main/java/com/pim/
│   ├── PimApplication.java           # 启动类
│   ├── controller/
│   │   ├── UserController.java       # 用户接口
│   │   └── ProfileController.java    # 个人资料接口
│   ├── service/
│   │   ├── UserService.java
│   │   └── FileService.java          # 文件上传服务
│   ├── mapper/
│   │   └── UserMapper.java
│   ├── model/
│   │   ├── User.java
│   │   └── UserProfileDTO.java
│   ├── config/
│   │   ├── WebConfig.java
│   │   └── SecurityConfig.java       # 安全配置
│   └── interceptor/
│       └── LoginInterceptor.java     # 登录拦截器
├── src/main/resources/
│   ├── application.yml
│   └── mappers/UserMapper.xml
├── sql/schema.sql
├── uploads/                          # 上传文件目录
├── pom.xml
└── README.md
```

## 数据库设计

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    phone VARCHAR(20),
    bio TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## API 设计

| Method | Path | 说明 |
|--------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/logout | 退出登录 |
| GET | /api/profile | 获取当前用户资料 |
| PUT | /api/profile | 更新个人资料 |
| POST | /api/profile/avatar | 上传头像 |
| PUT | /api/auth/password | 修改密码 |

## 快速开始

```bash
# 初始化数据库
mysql -u root -p < sql/schema.sql

# 启动
mvn spring-boot:run

# 测试注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","email":"test@example.com"}'
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| 密码加密 | BCrypt / SHA-256 + Salt |
| Session 管理 | HttpSession + 拦截器 |
| JWT 认证 | Token 生成、验证、刷新 |
| 文件上传 | MultipartFile → 本地存储路径 |
| 登录拦截 | HandlerInterceptor 实现 |
| 数据脱敏 | 返回数据时过滤敏感字段 |
