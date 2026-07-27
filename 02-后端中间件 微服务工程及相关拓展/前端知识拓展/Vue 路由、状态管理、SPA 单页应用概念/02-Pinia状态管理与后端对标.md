# 02 - Pinia 状态管理与后端对标

> 🎯 前端状态管理 = 后端的"全局变量 + 缓存" — Pinia 是 Vue 3 的官方状态管理库。理解 Store 如何存用户信息、Token、配置等全局状态

---

## 目录

1. [什么是状态管理](#1-什么是状态管理)
2. [Pinia 核心用法](#2-pinia-核心用法)
3. [与后端交互的典型 Store](#3-与后端交互的典型-store)

---

## 1. 什么是状态管理

```text
前端状态 = 需要跨组件共享的数据

问题：组件 A 的数据怎么传给组件 B？
  → 如果不在父子关系 → Props 传递繁琐
  → 状态管理库 = 全局数据仓库

对标 Java 后端：
  Pinia Store  ≈  Redis 缓存    — 全局可访问
  Pinia State  ≈  HashMap      — 键值对存储
  Pinia Getter ≈  getter 方法   — 计算属性
  Pinia Action ≈  Service 方法  — 异步逻辑
```

```text
什么时候需要状态管理？
  ✅ 用户信息（路由守卫、权限判断 → 全局）
  ✅ Token（拦截器需要 → 全局）
  ✅ 购物车（多个页面共享）
  ✅ 主题/语言设置（全局）
  ❌ 表单输入（仅当前页 → 不需要）
```

---

## 2. Pinia 核心用法

```javascript
// stores/user.js — 用户 Store
import { defineStore } from 'pinia';

export const useUserStore = defineStore('user', {
  // ═══ State — 数据（对标 Java 的字段） ═══
  state: () => ({
    user: null,
    token: localStorage.getItem('accessToken'),
    roles: []
  }),

  // ═══ Getters — 计算属性（对标 Java 的 getter） ═══
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.roles.includes('ADMIN'),
    userName: (state) => state.user?.name ?? '未登录'
  },

  // ═══ Actions — 异步方法（对标 Java 的 Service） ═══
  actions: {
    async login(username, password) {
      const { data } = await api.post('/auth/login', { username, password });
      this.token = data.accessToken;
      localStorage.setItem('accessToken', data.accessToken);
      await this.fetchUser();            // 登录后自动获取用户信息
    },

    async fetchUser() {
      const { data } = await api.get('/users/me');
      this.user = data;
      this.roles = data.roles;
    },

    logout() {
      this.user = null;
      this.token = null;
      this.roles = [];
      localStorage.removeItem('accessToken');
    }
  }
});
```

```vue
<!-- 在组件中使用 Store -->
<script setup>
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

// 直接读写 state
console.log(userStore.user?.name);

// 调用 action（异步）
await userStore.login('admin', 'password');

// 读取 getter
if (userStore.isLoggedIn) { /* ... */ }
</script>
```

---

## 3. 与后端交互的典型 Store

```javascript
// ⭐ 购物车 Store — 数据在服务端
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [],
    totalAmount: 0
  }),

  actions: {
    async fetchCart() {
      const { data } = await api.get('/cart');
      this.items = data;
      this.calculateTotal();
    },

    async addItem(productId, quantity) {
      await api.post('/cart/items', { productId, quantity });
      await this.fetchCart();    // 重新获取（保证数据一致性）
    },

    calculateTotal() {
      this.totalAmount = this.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
    }
  }
});
```

### 前端 Store 的数据策略

| 策略 | 适用 | 说明 |
|------|------|------|
| 服务端为准 | 购物车/订单 | Action 调 API 获取 → 更新 State |
| 本地为主 | UI 状态/主题 | 仅存本地，不上传 |
| 混合 | 用户信息 | 登录后缓存，关键操作重新获取 |

### 对标概念速查

```text
Pinia Store   ←→  Spring Service + Redis Cache
  state       ←→  Redis 中的缓存数据
  getters     ←→  Service 的 getter 方法
  actions     ←→  Service 的 public 方法（含 API 调用）

Pinia 的 actions 是异步的 ←→ Java 的 @Async 方法
Pinia 的 getters 有缓存 ←→ Spring Cache @Cacheable
```

> 🎯 **后端对标**：Store 是前端的"全局变量仓库"兼"Redis 缓存"。Action 负责调 API 取数据，State 存数据，Getters 做层转换。后端 90% 的逻辑在 Service 层，前端 90% 的逻辑在 Store 的 Action 里。
