# 01 - SPA 单页应用与前端路由原理

> 🎯 SPA 是前后端分离的基础 — 理解"只有一个 HTML + JS 接管一切"和前端的 hash/history 路由模式，后端才知道 Nginx 为何要配 try_files

---

## 目录

1. [MPA vs SPA](#1-mpa-vs-spa)
2. [前端路由原理](#2-前端路由原理)
3. [Vue Router 基础](#3-vue-router-基础)
4. [后端配合要点](#4-后端配合要点)

---

## 1. MPA vs SPA

```text
MPA（Multi-Page — 传统 JSP/Thymeleaf）：
  浏览器 → GET /users → 服务端返回完整 HTML → 渲染
          → GET /orders → 服务端返回另一个完整 HTML → 重新渲染
  → 每次导航都重新加载整个页面

SPA（Single-Page — Vue/React）：
  浏览器 → GET / → 返回空壳 HTML (<div id="app"></div>)
                → JS 加载（Vue Router 接管）
          → 点击"用户管理"→ JS 局部渲染，不发请求
          → 需要数据时才调 /api/users
  → 只有一个 HTML，JS 控制一切
```

| 维度 | MPA | SPA |
|------|:---:|:---:|
| 页面切换 | 整页刷新 | 局部更新（快） |
| 服务端职责 | 渲染 HTML + 提供数据 | 仅提供数据 API |
| 首屏速度 | 快（服务端渲染） | 慢（需加载 JS） |
| SEO | ✅ 天然友好 | ❌ 需额外处理 |
| 前后端 | 耦合（同一项目） | ⭐ 分离 |

---

## 2. 前端路由原理

```text
前端路由 vs 后端路由：

后端路由：
  GET /users    → Controller → 返回 HTML
  GET /orders   → Controller → 返回 HTML
  → 请求发到服务端，服务端决定返回什么

前端路由：
  GET /users    → Vue Router → 渲染 UserList 组件（不发请求！）
  GET /orders   → Vue Router → 渲染 OrderList 组件
  → 请求被 JS 拦截，JS 决定渲染什么
```

### 两种路由模式

| 模式 | URL | 原理 | 刷新后 |
|------|-----|------|:---:|
| **Hash** | `/#/users` | `#` 后的内容不发送到服务端 | ✅ 正常 |
| **History** | `/users` | HTML5 History API (`pushState`) | ❌ 404！需要 Nginx 配置 |

```javascript
// Vue Router 配置
const router = createRouter({
  history: createWebHistory(),    // History 模式（URL 干净）
  // history: createWebHashHistory(),  // Hash 模式（兼容性好）
  routes: [
    { path: '/',        component: Home },
    { path: '/users',   component: UserList },
    { path: '/users/:id', component: UserDetail },
    { path: '/orders',  component: OrderList }
  ]
});
```

---

## 3. Vue Router 基础

```vue
<!-- App.vue — SPA 入口 -->
<template>
  <nav>
    <router-link to="/">首页</router-link>
    <router-link to="/users">用户管理</router-link>
  </nav>
  <router-view />    <!-- ⭐ 路由匹配的组件渲染在这里 -->
</template>
```

```javascript
// ⭐ 路由守卫 — 权限控制（类似后端 Filter）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken');
  if (to.meta.requiresAuth && !token) {
    next('/login');       // 未登录 → 跳转登录页
  } else {
    next();               // 放行
  }
});

// 路由定义 + 权限
const routes = [
  { path: '/login', component: Login },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, roles: ['ADMIN'] },  // ← 需要的权限
    children: [
      { path: 'users', component: UserManagement }
    ]
  }
];
```

---

## 4. 后端配合要点

### ⚠️ History 模式需要 Nginx try_files

```nginx
# ❌ 没有 try_files → 刷新 /users → Nginx 找 /users 文件 → 404
# ✅ 有 try_files → 所有路径返回 index.html → Vue Router 接管

server {
    listen 80;
    root /usr/share/nginx/html;

    location / {
        try_files $uri $uri/ /index.html;   # ⭐ 关键：找不到文件返回 index.html
    }

    location /api/ {
        proxy_pass http://backend:8080;      # API 代理到后端
    }
}
```

### SPA + 后端需注意的点

| 问题 | 后端关注 |
|------|----------|
| **404 只在 API 返回** | SPA 路由由前端控制，后端只返回 JSON 404 |
| **首屏加载慢** | 后端 API 要快（首屏会批量调接口），或用 SSR |
| **权限控制** | 前端路由守卫仅做 UI 控制，真正的权限校验在后端！ |
| **页面刷新** | 确保 Nginx try_files 配置正确 |

> 🎯 **后端记住**：SPA 只有一个 HTML，一切由 JS 接管。前端路由 ≠ 后端路由，History 模式必须配 Nginx `try_files`。前端路由守卫是 UI 层，真正的安全在后端。
