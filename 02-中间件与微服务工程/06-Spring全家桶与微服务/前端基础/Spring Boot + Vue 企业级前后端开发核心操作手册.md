# Spring Boot + Vue 企业级前后端开发核心操作手册

> **文档定位**：Java 后端企业级技术文档 | Spring Boot + Vue 前后端开发实战手册  
> **前置条件**：Node.js（≥ 16）、Java（≥ 17）、Maven（≥ 3.8）、Vue CLI / Vite  
> **核心说明**：本文档覆盖 Spring Boot 后端核心操作 + Vue 前端核心操作 + 前后端联调最佳实践，适合快速搭建企业级前后端分离项目。

---

## 目录

- [一、Spring Boot 后端核心常用操作](#一spring-boot-后端核心常用操作)
- [二、Vue 前端核心常用操作](#二vue-前端核心常用操作)
- [三、前后端联调与企业级最佳实践](#三前后端联调与企业级最佳实践)

---

## 一、Spring Boot 后端核心常用操作

### 1. 项目初始化（企业级规范）

企业开发中优先使用 [Spring Initializr](https://start.spring.io/) 初始化项目。

#### 必选依赖

| 依赖 | 用途 |
|------|------|
| **Spring Web** | Web 接口开发，HTTP 请求处理 |
| **Spring Data JPA / MyBatis-Plus** | 简化数据库操作 |
| **MySQL Driver** | MySQL 数据库驱动 |
| **Lombok** | 简化代码，自动生成 get/set/toString |
| **Spring Boot Validation** | 接口参数校验 |
| **Spring Boot DevTools** | 热部署 |

#### 可选依赖

| 依赖 | 用途 |
|------|------|
| **Spring Security** | 接口权限控制 |
| **Redis** | 缓存热点数据 |
| **Knife4j** | 接口文档自动生成 |

#### 核心配置（application.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vue_springboot_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的数据库密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update       # 开发用 update，生产用 none
    show-sql: true           # 开发打印 SQL，生产关闭
    properties:
      hibernate:
        format_sql: true

server:
  port: 8080
  servlet:
    context-path: /api       # 统一接口前缀（企业级规范）
```

### 2. 核心业务开发（用户 CRUD 分层实现）

遵循 **Controller → Service → Repository** 分层开发模式。

#### 2.1 实体类（Entity）

```java
package com.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")  // 系统表前缀 sys_
public class User implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Length(min = 3, max = 20, message = "用户名长度必须在 3-20 之间")
    private String username;
    
    @Column(nullable = false)
    @Length(min = 6, message = "密码长度不能少于 6 位")
    private String password;
    
    private String nickname;
    private Integer age;
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
```

#### 2.2 数据访问层（Repository）

```java
package com.demo.repository;

import com.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 自定义查询（登录场景必备）
    Optional<User> findByUsername(String username);
}
```

#### 2.3 服务层（Service）

```java
package com.demo.service;

import com.demo.entity.User;
import com.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    // 分页查询
    public Page<User> getUserList(Integer pageNum, Integer pageSize) {
        return userRepository.findAll(PageRequest.of(pageNum - 1, pageSize));
    }
    
    // 新增用户（事务控制）
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    // 根据 ID 查询
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在：" + id));
    }
    
    // 修改用户（先查再改，避免覆盖）
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = getUserById(id);
        existingUser.setNickname(user.getNickname());
        existingUser.setAge(user.getAge());
        return userRepository.save(existingUser);
    }
    
    // 删除用户
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("用户不存在：" + id);
        }
        userRepository.deleteById(id);
    }
}
```

#### 2.4 控制器（Controller）

```java
package com.demo.controller;

import com.demo.entity.User;
import com.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

// 统一响应体（企业级必备）
class Result<T> {
    private int code;
    private String msg;
    private T data;
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }
    
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
    
    // 构造方法 + getter/setter（Lombok @Data 可用）
}

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // GET /api/users?pageNum=1&pageSize=10
    @GetMapping
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(userService.getUserList(pageNum, pageSize));
    }
    
    // POST /api/users
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody User user) {
        return Result.success(userService.createUser(user));
    }
    
    // GET /api/users/{id}
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
    
    // PUT /api/users/{id}
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return Result.success(userService.updateUser(id, user));
    }
    
    // DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }
}
```

### 3. 企业级必备：全局异常处理

```java
package com.demo.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 参数校验异常
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        String msg = e.getFieldError().getDefaultMessage();
        return Result.error(msg);
    }
    
    // 业务异常
    @ExceptionHandler(EntityNotFoundException.class)
    public Result<String> handleEntityNotFoundException(EntityNotFoundException e) {
        return Result.error(e.getMessage());
    }
    
    // 兜底处理
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();  // 开发环境，生产换日志记录
        return Result.error("服务器内部错误");
    }
}
```

---

## 二、Vue 前端核心常用操作

### 1. 项目初始化（Vite 首选）

```bash
# 创建 Vite + Vue 项目
npm create vite@latest vue-demo -- --template vue

# 进入目录并安装依赖
cd vue-demo && npm install

# 安装企业级必备依赖
npm install axios pinia element-plus vue-router@4
```

### 2. 核心配置（vite.config.js）

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')  // 路径别名
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',   // 后端接口地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

### 3. Axios 封装（src/utils/request.js）

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 5000
})

// 请求拦截器：添加 Token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '服务器错误')
    return Promise.reject(error)
  }
)

export default service
```

### 4. API 封装（src/api/user.js）

```javascript
import request from '@/utils/request'

export function getUserList(params) {
  return request({ url: '/users', method: 'get', params })
}

export function createUser(data) {
  return request({ url: '/users', method: 'post', data })
}

export function getUserById(id) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function updateUser(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}
```

### 5. 路由配置（src/router/index.js）

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import UserList from '@/views/UserList.vue'

const routes = [
  { path: '/', redirect: '/users' },
  { path: '/users', name: 'UserList', component: UserList }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

### 6. 用户列表页核心结构（src/views/UserList.vue）

```html
<template>
  <div>
    <!-- 新增按钮 -->
    <el-button type="primary" @click="handleAdd">新增用户</el-button>
    
    <!-- 用户表格 -->
    <el-table :data="userList" border style="margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="age" label="年龄" width="80" align="center" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper, ->, total"
      style="margin-top: 20px; text-align: right"
      @size-change="loadUserList"
      @current-change="loadUserList"
    />
    
    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input v-model="userForm.age" type="number" placeholder="请输入年龄" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

### 7. 入口文件（src/main.js）

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

---

## 三、前后端联调与企业级最佳实践

### 1. 前后端联调步骤

| 步骤 | 操作 |
|------|------|
| **1. 启动数据库** | 创建数据库 `vue_springboot_demo`（JPA 自动生成表结构） |
| **2. 启动后端** | 运行 `DemoApplication.java`（默认端口 8080） |
| **3. 启动前端** | `npm run dev`，访问 `http://localhost:5173` |
| **4. 测试功能** | 新增、编辑、删除、分页，验证前后端数据交互 |

### 2. 企业级最佳实践总结

#### Spring Boot 后端

| 实践 | 说明 |
|------|------|
| **分层开发** | Controller（接收）→ Service（逻辑）→ Repository（数据） |
| **统一响应体** | 所有接口返回 `Result` 格式 `{code, msg, data}` |
| **全局异常处理** | `@RestControllerAdvice` 统一捕获异常 |
| **双重校验** | 前端 + 后端双重校验，后端校验是最后防线 |
| **事务控制** | 数据变更操作加 `@Transactional` |
| **规范命名** | 表名前缀 `sys_`，接口前缀 `/api` |

#### Vue 前端

| 实践 | 说明 |
|------|------|
| **封装 Axios** | 统一处理请求/响应、Token、跨域 |
| **API 分层** | 接口封装到 `api/` 目录，与页面解耦 |
| **表单校验** | 前端先校验，减少无效后端请求 |
| **组件复用** | 复用 Element Plus 表格、分页、弹窗 |
| **路径别名** | `@` 替代 `../`，简化路径 |
| **用户交互** | 删除确认弹窗 + 操作成功提示 |

#### 联调关键

```
后端: server.servlet.context-path=/api  →  统一接口前缀
前端: Vite proxy  /api → http://localhost:8080  →  代理跨域
结果: 前后端接口路径匹配、数据格式统一
```
