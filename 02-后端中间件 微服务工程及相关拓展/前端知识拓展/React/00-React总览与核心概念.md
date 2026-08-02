# 00 - React 总览与核心概念

> 定位：React 知识体系入口——框架定位、核心思想、React 19 现状、与 Vue 对比、模块导航（2026-07 验证）

## 📚 目录

1. [React 是什么](#1-react-是什么)
2. [React 核心思想](#2-react-核心思想)
3. [React 19 与 2026 现状](#3-react-19-与-2026-现状)
4. [React vs Vue 对比](#4-react-vs-vue-对比)
5. [模块导航](#5-模块导航)

---

## 1. React 是什么

```
React = 用于构建用户界面的 JavaScript 库（Meta 出品）

定位：
  "库"而非"框架"——只负责视图层
  配合路由（React Router）、状态（Redux/Zustand）、
  构建（Vite/Next.js）组成完整方案

核心卖点：
  组件化（函数组件 + Hooks）
  声明式 UI（描述状态 → 界面自动一致）
  生态最大（50,000+ npm 包）

⚠️ 面试必答：
"React 是视图层库——组件 + 声明式
 + 单向数据流；全家桶需要自行组合
 （Router/状态管理/Next.js）。"
```

---

## 2. React 核心思想

### 2.1 三大核心

```
① 组件化：UI = f(state)
   函数组件接收 props → 返回 JSX

② 声明式：描述"界面应该是什么"
   状态变化 → React 自动更新 UI
   （对比命令式手动操作 DOM）

③ 单向数据流：
   父 → 子（props 向下）
   子 → 父（回调向上）
   状态提升（共享状态放共同父级）

⚠️ 面试必答：
"React 思想三句话——组件是 UI 的基本单元、
 声明式描述界面、单向数据流保证可预测性。"
```

### 2.2 声明式示例

```javascript
// 命令式（手动 DOM）
let count = 0;
btn.onclick = () => {
    count++;
    el.textContent = count;      // 手动同步
};

// React 声明式
function Counter() {
    const [count, setCount] = useState(0);
    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
    // ⚠️ 状态变化 → React 自动重新渲染
}
```

---

## 3. React 19 与 2026 现状

```
React 19（2024 年底发布，2026 主流）核心特性：
  ① React Compiler：编译期自动 memoization
     → 不再需要手动 useMemo/useCallback
  ② React Server Components（RSC）：组件服务端渲染
     → 客户端 bundle 减少 30-50%
  ③ Actions：表单处理与状态更新统一
  ④ use() Hook：读取 Promise/Context

2026 趋势（基准对比）：
  React 19 运行时 28.4 ops/s（vs Vue 4 的 31.2）
  Bundle ~72KB（最大，生态换来的代价）
  ⚠️ 生态与就业市场仍占主导

⚠️ 面试必答：
"React 19 的最大变化是 Compiler——
 自动记忆化（免手写 memo）；
 RSC 把组件移到服务端（包体积减半）；
 但运行时性能仍落后 Signals 架构框架。"
```

---

## 4. React vs Vue 对比

| 维度 | React 19 | Vue 3 |
|------|:---:|:---:|
| 渲染 | JSX（JS 内嵌） | 模板（HTML 友好） |
| 数据流 | 显式 setState | 自动响应式 |
| 更新机制 | 状态驱动 + Compiler memo | Proxy 细粒度追踪 |
| 组件语法 | 函数 + Hooks | SFC（template/script/style） |
| 状态管理 | Redux/Zustand（社区） | Pinia（官方） |
| 服务端 | RSC / Next.js | Nuxt.js |
| 学习曲线 | 中（Hooks 心智） | 低（模板接近 HTML） |

```
⚠️ 面试必答：
"React 显式状态（setState）vs Vue 自动响应式；
 JSX vs 模板是书写范式差异；
 React 生态最大、Vue 官方全家桶。
 两者本质都是'声明式组件 + 虚拟 DOM'。"
```

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-React总览与核心概念.md) | 核心思想、React 19、对比 | 入口 |
| 01 | [组件与JSX](01-React组件与JSX.md) | JSX、props、条件/列表渲染、表单 | 基础 |
| 02 | [Hooks 全解](02-ReactHooks全解.md) | useState/useEffect/useRef/useMemo/自定义 Hooks | 核心 |
| 03 | [状态管理与Context](03-React状态管理与Context.md) | 状态提升、Context、Reducer、Zustand/Redux | 核心 |
| 04 | [渲染与性能优化](04-React渲染与性能优化.md) | 渲染机制、memo、Compiler、虚拟列表 | 进阶 |
| 05 | [工程实践与面试题](05-React工程实践与面试题.md) | Next.js、React Router、工程化、面试题 | 实战 |

---

> 🎯 **本体系学习建议**：React 五条主线——**JSX 与组件**（写界面）、**Hooks**（状态与副作用，面试核心）、**状态管理**（Context/Zustand）、**性能**（渲染机制 + memo）、**工程**（Router/Next.js）。先吃透 Hooks 再谈其他。

---

**下一篇**：[01-React组件与JSX](01-React组件与JSX.md)
