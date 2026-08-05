# 05 - React 工程实践与面试题

> 定位：React Router、Next.js 与 RSC、工程化规范、React 面试题精选——从"会写组件"到"能上生产"

## 📚 目录

1. [React Router](#1-react-router)
2. [Next.js 与 RSC](#2-nextjs-与-rsc)
3. [工程化最佳实践](#3-工程化最佳实践)
4. [React 面试题精选](#4-react-面试题精选)

---

## 1. React Router

### 1.1 基础用法（v6/v7）

```jsx
// router.jsx
import { createBrowserRouter, RouterProvider } from 'react-router-dom';

const router = createBrowserRouter([
    {
        path: '/',
        element: <Layout />,                     // 布局（Outlet）
        children: [
            { index: true, element: <Home /> },
            { path: 'users/:id', element: <UserDetail /> },
            { path: '*', element: <NotFound /> },
        ],
    },
]);

// main.jsx
root.render(<RouterProvider router={router} />);
```

```jsx
// 使用
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

function UserDetail() {
    const { id } = useParams();                  // 路径参数
    const [searchParams] = useSearchParams();    // 查询参数
    const navigate = useNavigate();

    return (
        <div>
            <Link to={`/users/${id}`}>用户 {id}</Link>
            <button onClick={() => navigate(-1)}>返回</button>
        </div>
    );
}
```

### 1.2 路由守卫与懒加载

```jsx
// 懒加载 + 守卫
import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';

const UserPage = lazy(() => import('./pages/UserPage'));

// ⚠️ 鉴权守卫组件（包一层）
function RequireAuth({ children }) {
    const isLoggedIn = Boolean(localStorage.getItem('token'));
    return isLoggedIn ? children : <Navigate to="/login" replace />;
}

// 路由配置中使用
{
    path: '/user',
    element: (
        <RequireAuth>
            <Suspense fallback={<Loading />}>
                <UserPage />
            </Suspense>
        </RequireAuth>
    ),
}
```

---

## 2. Next.js 与 RSC

### 2.1 Next.js 是什么

```
Next.js = React 全栈框架（服务端渲染/静态生成/API）
  2026 主流版本：Next.js 15+（App Router）

核心能力：
  ① RSC（React Server Components）
  ② SSR/SSG/ISR（服务端渲染/静态/增量）
  ③ API Routes（内置后端）
  ④ 文件式路由（app/ 目录）

⚠️ 面试必答：
"Next.js = React 的全栈框架——
 App Router（文件即路由）+ RSC（服务端组件）
 + SSR/SSG；适合 SEO 与首屏性能场景。"
```

### 2.2 RSC 核心概念

```jsx
// ⚠️ 服务端组件（默认，不打包进客户端 JS）
// app/page.jsx —— 默认 Server Component
export default async function Page() {
    const data = await fetchData();      // 服务端直接请求
    return <ServerContent data={data} />;
}

// "use client" —— 客户端组件（交互/状态）
'use client';
import { useState } from 'react';

export default function Counter() {
    const [count, setCount] = useState(0);
    return <button onClick={() => setCount(c => c + 1)}>{count}</button>;
}

// 收益：客户端 bundle 减少 30-50%（RSC 核心价值）
// ⚠️ 规则：服务端组件不能使用 state/effect（无交互）
//   客户端组件可以导入服务端组件？❌ 不能（方向限制）
```

### 2.3 数据获取模式

```jsx
// app/page.jsx —— 服务端数据获取（推荐）
export default async function Home() {
    const posts = await fetch('https://api.example.com/posts', {
        next: { revalidate: 60 },        // ISR：60 秒增量重验证
    }).then(r => r.json());

    return posts.map(p => <PostCard key={p.id} post={p} />);
}
// ⚠️ 对比 CSR：数据在服务端获取 → 首屏含数据 → SEO 友好
```

---

## 3. 工程化最佳实践

### 3.1 项目结构

```
src/
├─ main.jsx            # 入口
├─ App.jsx             # 根组件
├─ router/             # 路由配置
├─ pages/              # 页面级组件
├─ components/         # 通用组件
├─ hooks/              # 自定义 Hooks
├─ stores/             # 状态管理
├─ services/           # API 请求层
├─ utils/              # 工具函数
└─ assets/             # 静态资源
```

### 3.2 工程规范清单

```
✅ 函数组件 + Hooks（无 class 组件）
✅ 组件命名 PascalCase、Hook 用 use 前缀
✅ props 解构 + 默认值
✅ 事件处理器 handleXxx 命名
✅ 条件渲染用三元/&&（非 if 语句）
✅ 列表 key 唯一稳定
✅ 状态提升原则（就近放置）
✅ 请求封装统一（错误处理/loading）
✅ ESLint（react-hooks 插件检查依赖数组）
✅ 环境变量区分（.env.development/production）
```

### 3.3 请求与错误处理模式

```jsx
// hooks/useRequest.js —— 统一请求封装
export function useRequest(fetcher, deps = []) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [refresh, setRefresh] = useState(0);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        fetcher()
            .then(d => !cancelled && setData(d))
            .catch(e => !cancelled && setError(e))
            .finally(() => !cancelled && setLoading(false));
        return () => { cancelled = true; };
    }, [...deps, refresh]);                // ⚠️ 依赖数组（eslint 检查）

    return {
        data, loading, error,
        retry: () => setRefresh(r => r + 1),
    };
}
```

---

## 4. React 面试题精选

**Q1: React 的渲染流程？**
```
setState → 调度 → render（vdom）→ diff → commit（DOM）。
React 19 并发：render 可中断（startTransition 标记低优先级）。
```

**Q2: 为什么需要 key？**
```
diff 时识别列表节点复用——key 唯一稳定避免
误删重建（状态丢失）。⚠️ 勿用 index（排序/过滤错乱）。
```

**Q3: useEffect 依赖数组的作用？**
```
依赖变化才执行；空数组 = 挂载一次；
漏依赖 = 闭包旧值；函数依赖需 useCallback 稳定。
无限循环排查：setState 在 effect 内 + 依赖不当。
```

**Q4: useMemo 和 useCallback 区别？**
```
useMemo 缓存计算值（依赖不变跳过重算）
useCallback 缓存函数引用（防 memo 失效）
React 19 Compiler 自动完成（原理必懂）。
```

**Q5: 组件通信方式？**
```
props（父→子）、回调（子→父）、
Context（跨层）、状态提升（兄弟）、
Zustand/Redux（全局）。
```

**Q6: React.memo 为什么失效？**
```
浅比较 props——对象/函数每次渲染新引用 → 失效。
配合 useMemo/useCallback 稳定引用才有效。
```

**Q7: 虚拟 DOM 的优势？**
```
批量最小化 DOM 更新（重排重绘少）；
跨平台（React Native）；
diff 三策略：同层/类型重建/key 复用。
```

**Q8: 什么是受控组件？**
```
表单值由 state 控制（value + onChange 成对）——
React 是唯一数据源，可实时校验/联动。
```

**Q9: 闭包陷阱怎么解决？**
```
effect 闭包捕获旧值 → 依赖加变量 / useRef 存最新值 /
函数式更新 setState(prev => ...)。
```

**Q10: 首屏性能优化？**
```
路由懒加载 + Suspense、RSC（Next.js）、
骨架屏、图片懒加载、代码分割、
memo + 虚拟列表、startTransition。
```

**Q11: class 组件 vs 函数组件？**
```
函数组件 + Hooks 是标准（无 this、逻辑复用、
TS 友好）；class 是历史（生命周期三件套）。
```

**Q12: Hooks 规则？**
```
顶层调用（顺序即身份）、函数组件专用。
不能在条件/循环/嵌套函数中调用。
```

---

> 🎯 **核心要点**：React 工程 = **Router**（懒加载 + RequireAuth 守卫）+ **Next.js**（RSC 服务端组件 + SSR/ISR）+ **规范**（Hooks + 命名 + eslint）+ **面试十二问**（渲染流程/依赖数组/memo 失效/受控组件是最高频）。React 面试主线：原理（渲染/diff）→ Hooks（依赖/闭包）→ 优化（memo/懒加载）。

---

**返回总览**：[00-React总览与核心概念](00-React总览与核心概念.md) | **上一篇**：[04-React渲染与性能优化](04-React渲染与性能优化.md)
