03.26 18:27
Spring Boot + Vue 企业级前后端开发核心操作手册
前置条件：已安装 Node.js（≥16）、Java（≥17）、Maven（≥3.8）、Vue CLI/Vite，且掌握 HTML/CSS/JS、Java 基础。
一、Spring Boot 后端核心常用操作
1. 项目初始化（企业级规范）
企业开发中优先使用 Spring Initializr（https://start.spring.io/）初始化项目，无需从零编写基础代码，核心依赖选择如下：
必选依赖（核心必备）
Spring Web：用于Web接口开发，提供HTTP请求处理能力；
Spring Data JPA/MyBatis-Plus：简化数据库操作，替代手写SQL（推荐JPA，快速实现CRUD）；
MySQL Driver：MySQL数据库驱动，实现与数据库的连接；
Lombok：通过注解简化代码，自动生成get/set/toString/构造方法等；
Spring Boot Validation：用于接口参数校验，避免非法数据传入；
Spring Boot DevTools：热部署工具，修改代码后无需重启项目，提升开发效率。
可选依赖（按需选择）
Spring Security：用于接口权限控制（登录、令牌验证等）；
Redis：用于缓存热点数据，提升接口响应速度；
Knife4j：接口文档生成工具，自动生成接口文档，方便前后端联调。
初始化后核心配置（application.yml）
配置数据库连接、JPA规则、服务器参数，遵循企业级规范，统一接口前缀：
spring:
  # 数据库配置（企业级常用MySQL）
  datasource:
    url: jdbc:mysql://localhost:3306/vue_springboot_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root  # 数据库用户名（根据自身环境修改）
    password: 你的数据库密码  # 数据库密码（替换为自身密码）
    driver-class-name: com.mysql.cj.jdbc.Driver
  # JPA配置（简化CRUD，替代手写SQL）
  jpa:
    hibernate:
      ddl-auto: update  # 开发环境用update（自动更新表结构），生产环境用none（禁止自动更新）
    show-sql: true      # 开发环境打印SQL，方便调试，生产环境关闭
    properties:
      hibernate:
        format_sql: true # 格式化SQL，提升可读性
# 服务器配置
server:
  port: 8080  # 后端端口（默认8080，可按需修改）
  servlet:
    context-path: /api  # 统一接口前缀（企业级规范，避免接口路径冲突）
2. 核心业务开发（用户CRUD，企业级分层实现）
遵循“Controller → Service → Repository”分层开发模式，职责清晰，便于维护和扩展，以用户模块CRUD为例。
（1）实体类（Entity）
对应数据库表，通过JPA注解关联表结构，配合Lombok简化代码，添加参数校验：
package com.demo.entity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import java.io.Serializable;
import java.time.LocalDateTime;
@Data  // Lombok注解：自动生成get/set/toString/构造方法等
@Entity
@Table(name = "sys_user")  // 对应数据库表名（企业级规范：系统表前缀sys_）
public class User implements Serializable {
    @Id  // 主键标识
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增主键（MySQL常用）
    private Long id;
    @Column(unique = true, nullable = false)  // 数据库字段：唯一、非空
    @Length(min = 3, max = 20, message = "用户名长度必须在3-20之间")  // 参数校验规则
    private String username;
    @Column(nullable = false)  // 数据库字段：非空
    @Length(min = 6, message = "密码长度不能少于6位")  // 参数校验规则
    private String password;
    private String nickname;  // 昵称（非必填）
    private Integer age;      // 年龄（非必填）
    private LocalDateTime createTime;  // 创建时间
    // 企业级优化：新增用户时自动填充创建时间（无需手动设置）
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
（2）数据访问层（Repository/DAO）
继承Spring Data JPA的JpaRepository，内置CRUD、分页、排序等方法，无需手写SQL，仅需自定义特殊查询：
package com.demo.repository;
import com.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
// JpaRepository<实体类, 主键类型>：提供基础CRUD方法
@Repository  // 标识为数据访问层组件，交给Spring管理
public interface UserRepository extends JpaRepository<User, Long> {
    // 自定义查询：根据用户名查询用户（企业级登录场景必备）
    Optional<User> findByUsername(String username);
}
（3）服务层（Service）
处理核心业务逻辑，添加事务控制，确保数据操作的原子性，避免直接暴露数据访问层：
package com.demo.service;
import com.demo.entity.User;
import com.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service  // 标识为服务层组件，交给Spring管理
@RequiredArgsConstructor  // Lombok注解：自动注入依赖（替代@Autowired，更简洁）
public class UserService {
    // 注入数据访问层对象（Spring自动管理，无需手动创建）
    private final UserRepository userRepository;
    // 分页查询用户（企业级列表页必备，支持分页、排序）
    public Page<User> getUserList(Integer pageNum, Integer pageSize) {
        // JPA分页从0开始，所以pageNum需减1
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        return userRepository.findAll(pageable);
    }
    // 新增用户（添加事务控制，确保操作失败时回滚）
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }
    // 根据ID查询用户（不存在则抛出异常，统一由全局异常处理）
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在：" + id));
    }
    // 修改用户（事务控制，先查询再修改，避免直接覆盖）
    @Transactional
    public User updateUser(Long id, User user) {
        // 先查询原有用户，确保用户存在
        User existingUser = getUserById(id);
        // 仅修改允许修改的字段（避免修改用户名、密码等敏感字段）
        existingUser.setNickname(user.getNickname());
        existingUser.setAge(user.getAge());
        return userRepository.save(existingUser);
    }
    // 删除用户（事务控制，先判断用户是否存在）
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("用户不存在：" + id);
        }
        userRepository.deleteById(id);
    }
}
（4）控制器（Controller）
接收前端请求，调用服务层方法，返回统一响应体，触发参数校验，遵循RESTful接口规范：
package com.demo.controller;
import com.demo.entity.User;
import com.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
// 统一响应体（企业级必备，所有接口返回格式一致，便于前端统一处理）
class Result<T> {
    private int code;    // 响应码：200成功，500失败
    private String msg;  // 响应信息
    private T data;      // 响应数据（可选）
    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }
    // 失败响应（带错误信息）
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
    // 省略getter/setter方法（Lombok的@Data也可用于此类）
}
@RestController  // 标识为REST接口控制器，返回JSON格式数据
@RequestMapping("/users")  // 接口路径：/api/users（配合server.context-path）
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    // 分页查询用户：GET /api/users?pageNum=1&pageSize=10
    @GetMapping
    public Result<Page<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,  // 默认页码1
            @RequestParam(defaultValue = "10") Integer pageSize) {  // 默认每页10条
        return Result.success(userService.getUserList(pageNum, pageSize));
    }
    // 新增用户：POST /api/users
    @PostMapping
    public Result<User> createUser(@Valid @RequestBody User user) {  // @Valid：触发参数校验
        return Result.success(userService.createUser(user));
    }
    // 根据ID查询用户：GET /api/users/{id}
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {  // @PathVariable：获取路径中的id
        return Result.success(userService.getUserById(id));
    }
    // 修改用户：PUT /api/users/{id}
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        return Result.success(userService.updateUser(id, user));
    }
    // 删除用户：DELETE /api/users/{id}
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success(null);
    }
}
3. 企业级必备：全局异常处理
统一捕获接口抛出的异常，返回规范的错误响应，避免接口抛裸异常，提升用户体验和代码可维护性：
package com.demo.exception;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
// @RestControllerAdvice：全局异常处理，作用于所有@RestController
public class GlobalExceptionHandler {
    // 处理参数校验异常（如用户名长度不符合要求）
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        // 获取校验失败的错误信息
        String msg = e.getFieldError().getDefaultMessage();
        return Result.error(msg);
    }
    // 处理业务异常（如用户不存在、数据重复等）
    @ExceptionHandler(EntityNotFoundException.class)
    public Result<String> handleEntityNotFoundException(EntityNotFoundException e) {
        return Result.error(e.getMessage());
    }
    // 处理所有未捕获的异常（兜底处理）
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();  // 开发环境打印异常堆栈，生产环境替换为日志记录
        return Result.error("服务器内部错误");
    }
}
二、Vue 前端核心常用操作
1. 项目初始化（企业级主流用Vite）
Vite比Vue CLI启动更快、构建效率更高，是目前企业级Vue项目的首选，执行以下命令初始化项目并安装核心依赖：
# 创建Vite + Vue项目（template vue指定Vue模板）
npm create vite@latest vue-demo -- --template vue
# 进入项目目录
cd vue-demo
# 安装项目依赖
npm install
# 安装企业级必备依赖
npm install axios pinia element-plus vue-router@4
# 说明：axios（请求工具）、pinia（状态管理）、element-plus（UI组件库）、vue-router@4（路由）
2. 核心配置（vite.config.js）
配置路径别名、跨域代理，解决前端开发中的路径繁琐、跨域问题，符合企业级规范：
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
export default defineConfig({
  plugins: [vue()],  // 引入Vue插件
  // 路径别名（企业级规范：@指向src目录，简化路径书写）
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  // 代理跨域（解决前端本地调试时的跨域问题，对接后端接口）
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 后端接口地址（与后端server.port一致）
        changeOrigin: true,  // 允许跨域
        rewrite: (path) => path.replace(/^\/api/, '')  // 替换路径，实际请求后端接口时去掉/api前缀
      }
    }
  }
})
3. 基础封装（企业级必备，解耦、复用）
封装Axios请求、API接口，避免代码冗余，便于维护和扩展。
（1）Axios 封装（src/utils/request.js）
统一处理请求拦截（添加token）、响应拦截（统一处理响应和错误），简化请求写法：
import axios from 'axios'
import { ElMessage } from 'element-plus'  // 引入Element Plus提示组件
// 创建axios实例，配置基础路径和超时时间
const service = axios.create({
  baseURL: '/api',  // 配合代理，实际请求路径为http://localhost:8080/api
  timeout: 5000     // 超时时间（5秒），避免请求长时间无响应
})
// 请求拦截器（发送请求前执行）
service.interceptors.request.use(
  (config) => {
    // 企业级场景：添加token（登录后存储在localStorage，用于权限验证）
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`  // 按后端要求的格式添加token
    }
    return config
  },
  (error) => {
    // 请求失败（如网络错误）
    return Promise.reject(error)
  }
)
// 响应拦截器（接收响应后执行）
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回码：200表示成功，其他表示失败
    if (res.code !== 200) {
      // 提示错误信息（Element Plus的ElMessage组件）
      ElMessage.error(res.msg || '请求失败，请稍后重试')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    // 成功则返回响应数据
    return res
  },
  (error) => {
    // 响应失败（如后端报错、超时）
    ElMessage.error(error.message || '服务器错误，请稍后重试')
    return Promise.reject(error)
  }
)
export default service
（2）API 封装（src/api/user.js）
将用户相关接口封装到单独文件，与页面逻辑解耦，便于复用和维护：
import request from '@/utils/request'  // 引入封装好的axios实例
// 分页查询用户（传递pageNum、pageSize参数）
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params  // 等价于params: params，GET请求用params传递参数
  })
}
// 新增用户（传递用户表单数据）
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data  // POST请求用data传递参数（JSON格式）
  })
}
// 根据ID查询用户（路径中传递id）
export function getUserById(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}
// 修改用户（路径中传递id，请求体传递修改后的数据）
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}
// 删除用户（路径中传递id）
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}
4. 页面开发（用户列表页，企业级实战）
结合Element Plus组件，实现用户列表、分页、新增/编辑/删除弹窗，适配后端接口，符合企业级UI规范。
（1）路由配置（src/router/index.js）
配置页面路由，实现页面跳转，遵循Vue Router 4的语法：
import { createRouter, createWebHistory } from 'vue-router'
import UserList from '@/views/UserList.vue'  // 引入用户列表页组件
// 路由规则
const routes = [
  {
    path: '/',
    redirect: '/users'  // 默认跳转到用户列表页
  },
  {
    path: '/users',
    name: 'UserList',
    component: UserList  // 路径对应的页面组件
  }
]
// 创建路由实例
const router = createRouter({
  history: createWebHistory(),  // HTML5历史模式（无#）
  routes  // 注入路由规则
})
export default router
（2）用户列表页（src/views/UserList.vue）
实现列表展示、分页、新增/编辑/删除功能，结合表单校验，与后端接口联动：
<!-- 新增用户按钮 -->
    <el-button type="primary" @click="handleAdd">新增用户</el-button>
    <!-- 用户列表表格（Element Plus组件） -->
    <el-table :data="userList" border style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" align="center" /><el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="age" label="年龄" width="80" align="center" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="200" align="center">
        <template #<!-- 编辑按钮 -->
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <!-- 删除按钮 -->
          <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
      </el-table-column>
    </el-table>
    <!-- 分页组件（Element Plus组件，适配后端JPA分页） -->
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper, ->, total"
      style="margin-top: 20px; text-align: right"
      @size-change="loadUserList"  // 每页条数变化时刷新列表
      @current-change="loadUserList"  // 页码变化时刷新列表
    />
    <!-- 新增/编辑弹窗（复用一个弹窗，通过isEdit区分） -->
    <el-dialog v-model="dialogVisible" title="用户表单" width="500px">
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <!-- 编辑时不回显密码，仅新增时显示密码输入框 -->
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
      <template #<el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </el-dialog>
5. 入口文件配置（src/main.js）
引入路由、Element Plus等核心依赖，挂载到Vue实例，确保全局可用：
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'  // 引入路由
import ElementPlus from 'element-plus'  // 引入Element Plus
import 'element-plus/dist/index.css'  // 引入Element Plus样式
// 创建Vue实例
const app = createApp(App)
// 挂载路由
app.use(router)
// 挂载Element Plus
app.use(ElementPlus)
// 挂载到页面#app元素
app.mount('#app')
三、前后端联调与企业级最佳实践
1. 前后端联调步骤（快速启动并测试）
启动MySQL数据库，手动创建数据库vue_springboot_demo（无需创建表，JPA会自动根据实体类生成表结构）；
启动Spring Boot后端：直接运行项目的DemoApplication.java（主启动类），确保后端服务正常启动（默认端口8080）；
启动Vue前端：在前端项目目录执行 npm run dev，启动成功后访问 http://localhost:5173（Vite默认端口），即可看到用户列表页；
测试功能：点击“新增用户”添加测试数据，测试编辑、删除功能，验证前后端数据交互是否正常。
2. 企业级最佳实践总结
Spring Boot 后端最佳实践
分层开发：严格遵循 Controller（接收请求）→ Service（业务逻辑）→ Repository（数据访问）分层，职责清晰，便于维护和扩展；
统一响应体：所有接口返回 Result 格式，前端无需单独处理不同返回格式，降低前后端联调成本；
全局异常处理：通过 @RestControllerAdvice 统一捕获异常，避免接口抛裸异常，提升用户体验和代码可维护性；
参数校验：前端+后端双重校验，后端校验是最后一道防线，确保传入数据合法；
事务控制：修改、删除等涉及数据变更的操作，添加 @Transactional 注解，确保数据一致性（操作失败时回滚）；
规范命名：数据库表名加前缀（如sys_），接口路径统一前缀（如/api），提升代码可读性和规范性。
Vue 前端最佳实践
封装Axios：统一处理请求/响应、跨域、token，简化请求写法，便于全局维护；
API分层：将接口封装到单独的api目录，与页面逻辑解耦，避免页面代码冗余，便于接口复用和修改；
表单校验：前端先进行表单校验，减少无效的后端请求，提升用户体验；
组件复用：复用Element Plus的表格、分页、弹窗等组件，快速开发页面，保持UI风格统一；
路径别名：用 @ 替代 ../ ，简化路径书写，避免路径错误，提升开发效率；
用户交互：添加确认弹窗（如删除）、操作提示（如新增成功），提升用户体验。
总结
Spring Boot 核心：通过分层开发实现用户CRUD，配合Spring Data JPA简化数据库操作，全局异常处理和统一响应体是企业级项目的必备组件，确保接口规范、稳定；
Vue 核心：封装Axios解决跨域和请求统一处理，API分层解耦，结合Element Plus快速实现企业级页面（列表、表单、分页等），提升开发效率和代码可维护性；
联调关键：后端配置 server.servlet.context-path 统一接口前缀，前端配置Vite代理，确保前后端接口路径匹配、数据格式统一，即可实现顺畅联调。

