# 04 - React 渲染与性能优化

> 定位：渲染机制、React.memo、虚拟 DOM 与 diff、React Compiler、长列表与懒加载——React 性能优化全体系

## 📚 目录

1. [渲染机制](#1-渲染机制)
2. [React.memo 与引用稳定](#2-reactmemo-与引用稳定)
3. [虚拟 DOM 与 diff](#3-虚拟-dom-与-diff)
4. [React Compiler 自动优化](#4-react-compiler-自动优化)
5. [列表与加载优化](#5-列表与加载优化)

---

## 1. 渲染机制

### 1.1 什么时候重新渲染

```
触发重渲染的三个原因：
  ① state 变化（setState）
  ② props 变化（父传子）
  ③ 父组件重渲染（默认子组件跟着渲染）

⚠️ 面试必答：
"渲染触发三原因——state、props、父渲染。
 默认：父渲染 → 所有子组件都重渲染
 （即使 props 没变）→ memo 解决。"
```

### 1.2 渲染流程

```
setState → 调度 → render（生成新 vdom）
→ diff（对比旧 vdom）→ commit（更新真实 DOM）

⚠️ 注意：
  render 阶段 = 执行组件函数（可能被跳过）
  commit 阶段 = 更新 DOM（真正生效）
  React 19 并发特性：render 可中断（startTransition）
```

---

## 2. React.memo 与引用稳定

### 2.1 React.memo

```jsx
import { memo } from 'react';

// ⚠️ memo：props 不变则跳过重渲染（浅比较）
const ExpensiveChild = memo(function ExpensiveChild({ data }) {
    console.log('渲染了');
    return <div>{data.name}</div>;
});

// 父组件
function Parent({ items }) {
    const [count, setCount] = useState(0);
    return (
        <div>
            <button onClick={() => setCount(c => c + 1)}>{count}</button>
            {/* ⚠️ count 变化 → Parent 重渲染 → memo 子组件 props 没变 → 跳过 */}
            <ExpensiveChild data={items} />
        </div>
    );
}
```

### 2.2 引用稳定的陷阱

```jsx
// ⚠️ memo 失效场景：每次渲染创建新引用

function Parent() {
    // ❌ 对象/函数每次渲染都是新引用 → memo 永远失效
    // <ExpensiveChild data={{ name: 'x' }} />
    // <ExpensiveChild onClick={() => {}} />

    // ✅ 必须 useMemo/useCallback 稳定引用
    const data = useMemo(() => ({ name: 'x' }), []);
    const onClick = useCallback(() => { }, []);
    return <ExpensiveChild data={data} onClick={onClick} />;
}
```

> 🎯 **要点**：memo + useMemo + useCallback 三件套配合才有效——**memo 做浅比较、useMemo/useCallback 提供稳定引用**。React 19 Compiler 自动做这件事。

---

## 3. 虚拟 DOM 与 diff

### 3.1 虚拟 DOM 是什么

```
虚拟 DOM（vdom）= 真实 DOM 的内存描述（JS 对象树）

为什么需要：
  直接操作 DOM 慢（重排/重绘）
  vdom diff → 最小化更新

⚠️ 面试必答：
"虚拟 DOM 的价值 = 批量最小化更新——
 JS 对象树对比（内存快）→ 只 patch
 变化部分（DOM 操作少而集中）。"
```

### 3.2 diff 算法要点

```
React diff 三策略：
  ① 同层比较（不跨层）
  ② 类型不同 → 重建（div → span 整树重建）
  ③ key 识别列表（key 相同复用）

⚠️ 面试必答：
"diff 三策略——同层、类型异则重建、
 key 复用列表节点。key 是列表性能关键
 （唯一稳定，勿用 index）。"
```

```jsx
// 类型不同的代价
// ❌ 条件切换不同类型 → 整树重建
// {isLoading ? <Spinner /> : <Content />}

// ✅ 保持类型稳定（内部条件渲染）
// <div>{isLoading ? <Spinner /> : <Content />}</div>
```

---

## 4. React Compiler 自动优化

### 4.1 React 19 Compiler

```
React Compiler（前身 React Forget）：
  编译期自动插入 memoization
  → 不再需要手动 useMemo/useCallback
  → 自动跳过未变化的组件重渲染

⚠️ 面试必答：
"React 19 Compiler 在编译时自动记忆化——
 手写 memo/useMemo/useCallback 的需求
 大幅下降；原理是自动依赖分析 +
 缓存不变的子表达式。"
```

### 4.2 手动优化 vs 编译器

| 维度 | 手动优化 | React Compiler |
|------|:---:|:---:|
| memo/useMemo | 手写 | 自动生成 |
| 出错风险 | 依赖漏写 | 编译器分析 |
| 心智负担 | 高 | 低 |
| 适用 | 无编译器环境 | React 19 默认 |

```
⚠️ 工程建议：
  理解手动优化原理（面试必问）
  React 19 生产依赖 Compiler
  （编译器默认关闭时手动兜底）
```

---

## 5. 列表与加载优化

### 5.1 列表优化

```jsx
// ① 大列表：虚拟列表（只渲染可视区域）
// 方案：react-window / react-virtualized
import { FixedSizeList } from 'react-window';

function BigList({ items }) {
    return (
        <FixedSizeList
            height={400}
            itemCount={items.length}
            itemSize={40}
            itemData={items}
        >
            {({ index, style, data }) => (
                <div style={style}>{data[index].name}</div>
            )}
        </FixedSizeList>
    );
}
// ⚠️ 10 万条数据只渲染可视 ~10 条

// ② 列表项 memo（每项独立 memo）
const Row = memo(function Row({ item, onSelect }) {
    return <li onClick={() => onSelect(item.id)}>{item.name}</li>;
});
```

### 5.2 代码分割与懒加载

```jsx
// ⚠️ React.lazy：组件级代码分割（按需加载）
import { lazy, Suspense } from 'react';

const HeavyChart = lazy(() => import('./HeavyChart'));

function Dashboard() {
    return (
        <Suspense fallback={<Spinner />}>
            <HeavyChart />          {/* 首次渲染才加载 */}
        </Suspense>
    );
}

// 路由级懒加载（标准实践）
// const UserPage = lazy(() => import('./pages/UserPage'));
// <Route element={<Suspense fallback={<Loading/>}><UserPage/></Suspense>} />
```

### 5.3 其他优化手段

| 优化 | 手段 |
|------|------|
| 包体积 | 路由懒加载 + 按需引入 UI 库 + Tree-shaking |
| 首屏 | 骨架屏 + 图片懒加载（loading="lazy"） |
| 渲染 | memo + 引用稳定 + 虚拟列表 |
| 交互 | 防抖/节流 + startTransition（非紧急更新） |
| 网络 | 缓存 + 请求合并 + SWR/React Query |

```jsx
// startTransition：非紧急更新标记（React 19 并发特性）
import { startTransition, useState } from 'react';

const [tab, setTab] = useState('all');
startTransition(() => setTab('all'));   // 不阻塞输入（低优先级）
// ⚠️ 适用：大列表过滤/切换（用户输入保持流畅）
```

> 🎯 **要点**：性能优化四层——**渲染层**（memo + 稳定引用 + 虚拟列表）、**加载层**（lazy + 路由分割）、**交互层**（防抖节流 + startTransition）、**网络层**（React Query 缓存）。面试按四层回答即完整。

---

> 🎯 **核心要点**：渲染与性能 = **渲染机制**（state/props/父渲染三触发）+ **memo 三件套**（memo + useMemo + useCallback 配合）+ **diff 三策略**（同层/类型/key）+ **React Compiler**（自动 memo，19 核心）+ **四层优化**（渲染/加载/交互/网络）。"memo 为什么失效"与"diff 怎么工作"是两大必考题。

---

**返回总览**：[00-React总览与核心概念](00-React总览与核心概念.md) | **上一篇**：[03-React状态管理与Context](03-React状态管理与Context.md) | **下一篇**：[05-React工程实践与面试题](05-React工程实践与面试题.md)
