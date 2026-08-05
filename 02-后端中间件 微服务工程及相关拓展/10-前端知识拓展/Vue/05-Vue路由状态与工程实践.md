# 05 - Vue 路由、状态管理与工程实践

> 定位：Vue Router 4、Pinia 状态管理、工程化（Vite/项目结构）、Vue 面试题精选

## 📚 目录

1. [Vue Router 4](#1-vue-router-4)
2. [Pinia 状态管理](#2-pinia-状态管理)
3. [工程化与项目结构](#3-工程化与项目结构)
4. [Vue 面试题精选](#4-vue-面试题精选)

---

## 1. Vue Router 4

### 1.1 路由基础

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import Home from '@/views/Home.vue';

const router = createRouter({
    history: createWebHistory(),          // ⚠️ HTML5 history（vs hash）
    routes: [
        { path: '/', name: 'home', component: Home },
        { path: '/user/:id', name: 'user',
          component: () => import('@/views/User.vue') },   // ⚠️ 路由懒加载
        { path: '/:pathMatch(.*)*', redirect: '/' },       // 404 兜底
    ],
});

export default router;
```

```vue
<!-- 使用 -->
<template>
    <RouterLink to="/">首页</RouterLink>
    <RouterLink :to="{ name: 'user', params: { id: 1 } }">用户</RouterLink>

    <!-- ⚠️ 视图出口（必须） -->
    <RouterView />
    <!-- 命名视图 -->
    <RouterView name="sidebar" />
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
const route = useRoute();       // 当前路由信息（params/query）
const router = useRouter();     // 路由操作

// 编程式导航
router.push({ name: 'user', params: { id: 2 } });
router.replace('/home');        // 不保留历史
router.back();
</script>
```

### 1.2 路由守卫

```javascript
// 全局前置守卫（登录鉴权标准姿势）
router.beforeEach((to, from) => {
    const isLoggedIn = localStorage.getItem('token');
    if (to.meta.requiresAuth && !isLoggedIn) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }
    // ⚠️ 返回 false 阻止、返回路径重定向、返回 true/undefined 放行
});

// 路由级守卫
{
    path: '/admin',
    meta: { requiresAuth: true, role: 'admin' },
    beforeEnter: (to, from) => { /* 路由级校验 */ },
}

// 组件内守卫
onBeforeRouteLeave(() => { /* 离开确认 */ });
```

| 守卫 | 时机 |
|------|------|
| beforeEach | 全局（鉴权） |
| beforeResolve | 全局（数据获取后） |
| afterEach | 全局（埋点/标题） |
| beforeEnter | 路由级 |
| onBeforeRouteLeave/Update | 组件内 |

> 🎯 **要点**：路由三件套——懒加载（打包优化）、路由守卫（鉴权）、meta 元信息（权限标记）。SPA 权限控制 = beforeEach + meta.requiresAuth。

---

## 2. Pinia 状态管理

### 2.1 基础 store

```javascript
// stores/user.js
import { defineStore } from 'pinia';

// ⚠️ 选项式 store（类似 Vuex 风格）
export const useUserStore = defineStore('user', {
    state: () => ({ name: '张三', token: '' }),
    getters: {
        displayName: (state) => `用户：${state.name}`,
    },
    actions: {
        async login(username) {
            // 异步 action（请求 + 更新 state）
            this.token = 'mock-token';
            this.name = username;
        },
        logout() { this.$reset(); },      // 重置
    },
});

// 组合式 store（推荐，与 setup 一致）
export const useCounterStore = defineStore('counter', () => {
    const count = ref(0);
    const double = computed(() => count.value * 2);
    function increment() { count.value++; }
    return { count, double, increment };
});
```

### 2.2 使用与持久化

```vue
<script setup>
import { storeToRefs } from 'pinia';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

// ⚠️ 解构保响应：必须用 storeToRefs（直接解构会丢失响应）
const { name, token } = storeToRefs(userStore);
const { login, logout } = userStore;     // actions 直接解构

// 持久化：pinia-plugin-persistedstate（第三方）
// 或手动：watch(name, v => localStorage.setItem('name', v))
</script>
```

### 2.3 Pinia vs Vuex

| 维度 | Vuex 4 | Pinia |
|------|:---:|:---:|
| API | 选项式（state/getters/mutations/actions） | 组合式（简洁） |
| mutations | ✅ 必须（同步） | ❌ 去掉（直接改 state） |
| 类型推导 | 弱 | ✅ 强 |
| 模块 | modules 嵌套 | 独立 store（扁平） |
| DevTools | ✅ | ✅ |

> 🎯 **要点**：Pinia 是 Vue 官方推荐的状态管理（替代 Vuex）——"去掉 mutations、组合式 API、TypeScript 友好"。状态管理选型一句话：**全局共享用 Pinia、局部跨层用 provide/inject**。

---

## 3. 工程化与项目结构

### 3.1 标准项目结构

```
src/
├─ main.js              # 入口（createApp + plugins）
├─ App.vue              # 根组件
├─ router/              # 路由
├─ stores/              # Pinia
├─ views/               # 页面级组件
├─ components/          # 通用组件
├─ composables/         # 组合式函数
├─ directives/          # 自定义指令
├─ api/                 # 请求封装
├─ utils/               # 工具函数
├─ assets/              # 静态资源
└─ styles/              # 全局样式
```

```javascript
// main.js：应用入口
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount('#app');
```

### 3.2 Vite 工程要点

```javascript
// vite.config.js
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
    },
    server: {
        proxy: {                             // ⚠️ 开发代理（解决跨域）
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
    build: {
        chunkSizeWarningLimit: 500,
        rollupOptions: {
            output: { manualChunks: { vue: ['vue', 'vue-router', 'pinia'] } },
        },
    },
});
```

### 3.3 工程最佳实践清单

```
✅ 路由懒加载 + 异步组件（包体积）
✅ API 统一封装（拦截器/错误处理）
✅ 环境变量（.env.development/.production）
✅ ESLint + Prettier（代码规范）
✅ 组件命名 PascalCase、composable 用 use 前缀
✅ 样式 scoped（全局样式集中管理）
⚠️ 敏感信息不进前端（token 内存/httponly）
```

---

## 4. Vue 面试题精选

**Q1: Vue 3 响应式原理？**
```
Proxy 拦截（get 收集依赖/set 触发更新）+ 依赖管理
（target → key → effects 三层）+ 批量异步渲染。
对比 Vue 2 defineProperty：新增/删除/数组原生响应。
完整见 03 篇。
```

**Q2: 组件通信方式？**
```
props（父→子）、emit（子→父）、v-model（双向）、
provide/inject（跨层）、defineExpose（父调子方法）、
Pinia（全局）。优先 props/emit，跨层 provide/inject，
全局 Pinia。
```

**Q3: v-if 和 v-show 区别？**
```
v-if 不渲染（条件渲染，切换有创建/销毁开销）
v-show 始终渲染（display 切换）
频繁切换 v-show、条件少 v-if。v-if 有懒加载价值。
```

**Q4: computed 和 watch 区别？**
```
computed：派生数据 + 缓存（依赖不变不重算）
watch：副作用（请求/日志），显式监听
能 computed 不 watch。
```

**Q5: key 的作用？**
```
虚拟 DOM diff 的复用标识——key 唯一稳定时
可精确复用节点（避免误删重建）。
⚠️ 不要用 index 作 key（排序/过滤时 bug）。
```

**Q6: nextTick 原理？**
```
更新是异步批量的（微任务）——nextTick 在
DOM 更新后执行回调。本质 = Promise.then 微任务
（有降级方案）。
```

**Q7: v-model 的实现原理？**
```
语法糖：:value + @input（组件是 :model-value +
@update:model-value）。自定义组件实现 v-model
需 props.modelValue + emit('update:modelValue')。
```

**Q8: SPA 首屏优化？**
```
路由懒加载、异步组件、骨架屏、
静态资源 CDN + hash 缓存、代码分割（manualChunks）、
图片懒加载。
```

**Q9: 说说 Vue 3 相比 Vue 2 的改进？**
```
响应式（Proxy 完整性）、组合式 API（复用）、
TS 支持、性能（编译优化：静态提升/patchFlags）、
Fragment/Teleport/Suspense。
```

**Q10: 谈谈 Vue 的虚拟 DOM？**
```
render 生成 vnode 树 → diff 比较新旧 → 最小化 patch。
Vue 3 编译优化：静态节点提升、动态节点 patchFlags
（只对比动态部分）→ 比 Vue 2 全量对比更快。
```

---

> 🎯 **核心要点**：工程实践体系 = **Router**（懒加载 + 守卫鉴权）+ **Pinia**（组合式 store + storeToRefs）+ **工程化**（Vite 代理/别名/分包）+ **面试十问**（响应式/通信/nextTick/虚拟 DOM 是最高频）。Vue 面试主线：原理（响应式）→ 实践（通信）→ 优化（首屏）。

---

**返回总览**：[00-Vue总览与核心概念](00-Vue总览与核心概念.md) | **上一篇**：[04-Vue组合式API与进阶](04-Vue组合式API与进阶.md)
