03.31 02:02
Vue企业级项目实战：电商后台管理系统（Vue3 + Vite + Element Plus）
项目简介
基于Vue3 + Vite + Element Plus构建的电商后台管理系统，包含登录、商品管理、订单管理、用户管理等核心模块，覆盖Vue3核心语法、组件化、路由、状态管理、网络请求等企业级开发技能。
技术栈
- 核心框架：Vue3、Vite
- UI组件库：Element Plus
- 路由：Vue Router 4
- 状态管理：Pinia
- 网络请求：Axios
- 规范：组件化、模块化、响应式、路由守卫
项目结构
plaintext
vue-admin/
├── public/
├── src/
│   ├── api/          # 接口请求
│   ├── assets/       # 静态资源
│   ├── components/   # 公共组件
│   ├── router/       # 路由配置
│   ├── store/        # Pinia状态管理
│   ├── utils/        # 工具函数
│   ├── views/        # 页面组件
│   ├── App.vue       # 根组件
│   └── main.js       # 入口文件
├── index.html
├── package.json
└── vite.config.js
 
完整代码实现
1. 初始化项目
bash
# 创建项目
npm create vite@latest vue-admin -- --template vue
# 进入项目
cd vue-admin
# 安装依赖
npm install
# 安装Element Plus、Axios、Vue Router、Pinia
npm install element-plus axios vue-router@4 pinia
 
2. 配置文件
package.json
json
{
  "name": "vue-admin",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.6.0",
    "element-plus": "^2.4.0",
    "pinia": "^2.1.0",
    "vue": "^3.3.0",
    "vue-router": "^4.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.2.0",
    "vite": "^4.4.0"
  }
}
 
vite.config.js
javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000
  }
})
 
3. 入口文件（main.js）
javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
 
4. 路由配置（router/index.js）
javascript
import { createRouter, createWebHistory } from 'vue-router'
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/Product.vue')
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/Order.vue')
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/User.vue')
      }
    ]
  }
]
const router = createRouter({
  history: createWebHistory(),
  routes
})
// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})
export default router
 
5. 状态管理（store/user.js）
javascript
import { defineStore } from 'pinia'
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: ''
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    logout() {
      this.token = ''
      this.username = ''
      localStorage.removeItem('token')
    }
  }
})
 
6. 网络请求（api/request.js）
javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})
// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)
// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)
export default request
 
7. 页面组件
Login.vue
vue
<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2>电商后台管理系统</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const form = ref({
  username: '',
  password: ''
})
const rules = ref({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})
const login = async () => {
  await formRef.value.validate()
  // 模拟登录
  if (form.value.username === 'admin' && form.value.password === '123456') {
    userStore.setToken('admin-token')
    ElMessage.success('登录成功')
    router.push('/home')
  } else {
    ElMessage.error('用户名或密码错误')
  }
}
</script>
<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}
.login-box {
  width: 400px;
  padding: 20px;
}
h2 {
  text-align: center;
  margin-bottom: 20px;
}
</style>
 
Layout.vue
vue
<template>
  <el-container>
    <el-aside width="200px">
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/product">商品管理</el-menu-item>
        <el-menu-item index="/order">订单管理</el-menu-item>
        <el-menu-item index="/user">用户管理</el-menu-item>
        <el-menu-item @click="logout">退出登录</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view></router-view>
    </el-main>
  </el-container>
</template>
<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
const router = useRouter()
const userStore = useUserStore()
const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
<style>
.el-aside {
  background-color: #304156;
  color: white;
}
.el-menu {
  border-right: none;
}
</style>
 
Home.vue
vue
<template>
  <div>
    <h2>欢迎使用电商后台管理系统</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>商品总数：100</el-card>
      </el-col>
      <el-col :span="6">
        <el-card>订单总数：500</el-card>
      </el-col>
      <el-col :span="6">
        <el-card>用户总数：2000</el-card>
      </el-col>
      <el-col :span="6">
        <el-card>今日销售额：¥10000</el-card>
      </el-col>
    </el-row>
  </div>
</template>
 
Product.vue
vue
<template>
  <div>
    <el-button type="primary" @click="addProduct">新增商品</el-button>
    <el-table :data="productList" border>
      <el-table-column prop="id" label="ID"></el-table-column>
      <el-table-column prop="name" label="商品名称"></el-table-column>
      <el-table-column prop="price" label="价格"></el-table-column>
      <el-table-column prop="stock" label="库存"></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" size="small" @click="editProduct(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteProduct(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
const productList = ref([
  { id: 1, name: 'iPhone 15', price: 5999, stock: 100 },
  { id: 2, name: 'MacBook Pro', price: 12999, stock: 50 }
])
const addProduct = () => {
  ElMessage.success('新增商品')
}
const editProduct = (row) => {
  ElMessage.success(`编辑商品：${row.name}`)
}
const deleteProduct = (id) => {
  productList.value = productList.value.filter(item => item.id !== id)
  ElMessage.success('删除成功')
}
</script>
 
Order.vue
vue
<template>
  <div>
    <el-table :data="orderList" border>
      <el-table-column prop="id" label="订单ID"></el-table-column>
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="totalPrice" label="总价"></el-table-column>
      <el-table-column prop="status" label="状态"></el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
const orderList = ref([
  { id: 1, username: '张三', totalPrice: 5999, status: '已支付' },
  { id: 2, username: '李四', totalPrice: 12999, status: '待支付' }
])
</script>
 
User.vue
vue
<template>
  <div>
    <el-table :data="userList" border>
      <el-table-column prop="id" label="ID"></el-table-column>
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="phone" label="手机号"></el-table-column>
      <el-table-column prop="status" label="状态"></el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
const userList = ref([
  { id: 1, username: '张三', phone: '13800138000', status: '正常' },
  { id: 2, username: '李四', phone: '13900139000', status: '禁用' }
])
</script>
 
8. 根组件（App.vue）
vue
<template>
  <router-view></router-view>
</template>
 
项目核心功能
1. 登录认证：路由守卫、Token存储
2. 布局系统：侧边栏菜单、路由跳转
3. 商品管理：增删改查、表格展示
4. 订单管理：订单列表展示
5. 用户管理：用户信息展示
6. 状态管理：Pinia存储用户信息
7. 响应式布局：Element Plus组件适配
运行步骤
1. 初始化项目并安装依赖
2. 复制代码到对应文件
3. 启动项目：npm run dev
4. 访问http://localhost:3000
5. 登录账号：admin/123456
企业级优化点
1. 组件化：公共组件拆分、代码复用
2. 路由管理：路由守卫、懒加载
3. 状态管理：Pinia统一管理状态
4. 网络请求：拦截器统一处理请求响应
5. 模块化：目录结构清晰、职责分离
6. UI规范：Element Plus统一组件风格
扩展方向
1. 集成Mock数据模拟接口
2. 实现权限管理（角色、菜单）
3. 加入数据可视化（ECharts）
4. 集成富文本编辑器
5. 实现文件上传功能
6. 配置环境变量（开发/生产）
7. 加入单元测试（Jest）

