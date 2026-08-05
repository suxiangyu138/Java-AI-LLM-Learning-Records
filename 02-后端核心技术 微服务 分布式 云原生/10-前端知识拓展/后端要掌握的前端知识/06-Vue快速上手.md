# 06-Vue快速上手
> 🎯 后端为什么学Vue？因为国内90%的前端项目用Vue — 能看懂前端代码、能写简单页面验证自己的API，是后端协作能力的质变

---

## 目录
1. [Vue 3核心概念](#1-vue-3核心概念)
2. [单文件组件(SFC)](#2-单文件组件sfc)
3. [响应式数据与计算属性](#3-响应式数据与计算属性)
4. [常用指令速查](#4-常用指令速查)
5. [调用后端API](#5-调用后端api)
6. [Vue Router路由](#6-vue-router路由)
7. [实战：用Vue写一个调用自己API的页面](#7-实战用vue写一个调用自己api的页面)

---

## 1. Vue 3核心概念

### 1.1 Vue是什么

> Vue是一个渐进式JavaScript框架 — 可以只在一个页面中用，也可以构建整个SPA应用。

```text
后端类比：
  Spring Boot = Vue  （框架）
  @Controller = <script setup>
  Thymeleaf模板语法 = Vue模板语法 {{ }}
```

### 1.2 最简Vue应用

```html
<!DOCTYPE html>
<html>
<body>
  <div id="app">
    <h1>{{ message }}</h1>          <!-- 数据绑定 -->
    <button @click="count++">        <!-- 事件处理 -->
      点击 {{ count }} 次
    </button>
  </div>

  <script type="module">
    import { createApp, ref } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'
    
    createApp({
      setup() {
        const message = ref('Hello Vue!')
        const count = ref(0)
        return { message, count }
      }
    }).mount('#app')
  </script>
</body>
</html>
```

---

## 2. 单文件组件(SFC)

> `.vue`文件 = 模板 + 逻辑 + 样式，三者合一。后端可类比为JSP但更现代化。

```vue
<!-- UserList.vue — 用户列表组件 -->
<template>
  <div class="user-list">
    <h2>用户列表</h2>
    <ul>
      <li v-for="user in users" :key="user.id">
        {{ user.name }} — {{ user.email }}
      </li>
    </ul>
    <button @click="loadUsers">刷新</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

// 响应式数据
const users = ref([])

// 加载用户
const loadUsers = async () => {
  const { data } = await axios.get('/api/users')
  users.value = data.data  // ref 需要 .value 访问
}

// 组件挂载时加载
onMounted(() => loadUsers())
</script>

<style scoped>
.user-list { padding: 20px; }
</style>
```

| Vue概念 | 后端类比 |
|---------|---------|
| `<template>` | JSP/Thymeleaf HTML模板 |
| `<script setup>` | Controller方法（逻辑层） |
| `ref()` | 可变的成员变量 |
| `onMounted()` | `@PostConstruct` |
| `v-for` | `th:each` / `forEach` |
| `{{ }}` | `th:text` / EL表达式 |
| `@click` | `onclick` 事件 |

---

## 3. 响应式数据与计算属性

### 3.1 ref vs reactive

```javascript
import { ref, reactive } from 'vue'

// ref：基本类型和单值（推荐）
const count = ref(0)
count.value++  // 修改需要 .value
console.log(count.value)

// reactive：对象（不推荐用reactive包裹基本类型）
const user = reactive({ name: '张三', age: 25 })
user.name = '李四'  // 直接修改，不需要 .value
```

### 3.2 computed 计算属性

```javascript
import { ref, computed } from 'vue'

const firstName = ref('张')
const lastName = ref('三')

// 计算属性：依赖其他数据自动计算
const fullName = computed(() => firstName.value + lastName.value)

console.log(fullName.value) // "张三"
firstName.value = '李'
console.log(fullName.value) // "李三" — 自动更新！
```

---

## 4. 常用指令速查

| 指令 | 语法 | 说明 | 后端对应概念 |
|------|------|------|-------------|
| `v-if` | `<div v-if="show">` | 条件渲染(DOM移除) | `if` |
| `v-show` | `<div v-show="show">` | 条件显示(display:none) | 切换CSS display |
| `v-for` | `<li v-for="u in users" :key="u.id">` | 列表渲染 | `forEach` |
| `v-model` | `<input v-model="name">` | 双向绑定 | `@RequestParam` |
| `v-bind` / `:` | `<img :src="url">` | 属性绑定 | 动态设置属性 |
| `v-on` / `@` | `<button @click="submit">` | 事件绑定 | `click`事件 |
| `{{ }}` | `<span>{{ name }}</span>` | 文本插值 | `th:text` |

```html
<!-- 指令综合示例 -->
<template>
  <!-- 条件渲染 -->
  <div v-if="loading">加载中...</div>
  <div v-else-if="error">加载失败: {{ error }}</div>
  
  <!-- 列表渲染 -->
  <ul v-else>
    <li v-for="user in users" :key="user.id">
      <!-- 双向绑定 -->
      <input v-model="user.name">
      <!-- 事件绑定 -->
      <button @click="deleteUser(user.id)">删除</button>
    </li>
  </ul>
</template>
```

---

## 5. 调用后端API

### 5.1 Axios封装（后端友好写法）

```javascript
// api.js — 统一API调用
import axios from 'axios'

const api = axios.create({
    baseURL: '/api',          // 所有请求自动加/api前缀
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：自动加Token
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// 响应拦截器：统一错误处理
api.interceptors.response.use(
    response => response.data,        // 自动提取data
    error => {
        if (error.response?.status === 401) {
            // Token过期 → 跳转登录
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

// 导出API方法
export const userApi = {
    list: (params) => api.get('/users', { params }),
    getById: (id) => api.get(`/users/${id}`),
    create: (data) => api.post('/users', data),
    update: (id, data) => api.put(`/users/${id}`, data),
    delete: (id) => api.delete(`/users/${id}`),
}
```

---

## 6. Vue Router路由

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    { path: '/', redirect: '/users' },
    { path: '/users', component: () => import('@/views/UserList.vue') },
    { path: '/users/:id', component: () => import('@/views/UserDetail.vue') },
]

export default createRouter({
    history: createWebHistory(),
    routes
})
```

```text
后端类比：
  Vue Router = Spring MVC @RequestMapping
  /users    → GET /api/users
  /users/1  → GET /api/users/1
  路由切换  → 不刷新页面（SPA单页应用）
```

---

## 7. 实战：用Vue写一个调用自己API的页面

```vue
<!-- UserManager.vue — 完整的用户管理页面 -->
<template>
  <div>
    <h2>用户管理</h2>
    
    <!-- 搜索 -->
    <input v-model="keyword" placeholder="搜索用户">
    <button @click="search">搜索</button>
    
    <!-- 新增表单 -->
    <div>
      <input v-model="newUser.name" placeholder="姓名">
      <input v-model="newUser.email" placeholder="邮箱">
      <button @click="create">新增</button>
    </div>
    
    <!-- 列表 -->
    <table>
      <tr v-for="user in users" :key="user.id">
        <td>{{ user.id }}</td>
        <td>{{ user.name }}</td>
        <td>{{ user.email }}</td>
        <td><button @click="remove(user.id)">删除</button></td>
      </tr>
    </table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: '/api' })
const users = ref([])
const keyword = ref('')
const newUser = ref({ name: '', email: '' })

const search = async () => {
  const { data } = await api.get('/users', { params: { keyword: keyword.value } })
  users.value = data.data
}

const create = async () => {
  await api.post('/users', newUser.value)
  newUser.value = { name: '', email: '' }
  search() // 刷新列表
}

const remove = async (id) => {
  await api.delete(`/users/${id}`)
  search()
}
</script>
```

---

> 🎯 **后端学Vue的最小集合**：能看懂`.vue`文件的`<template>`+`<script setup>`结构 → 理解`v-for`/`v-if`/`v-model`/`@click` → 能自己写一个简单页面测试API。
