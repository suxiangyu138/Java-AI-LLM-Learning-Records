# 02 - React Hooks 全解

> 定位：Hooks 规则、useState/useEffect/useRef/useMemo/useCallback/useContext、自定义 Hooks——React 面试第一核心域

## 📚 目录

1. [Hooks 是什么与规则](#1-hooks-是什么与规则)
2. [useState 状态管理](#2-usestate-状态管理)
3. [useEffect 副作用](#3-useeffect-副作用)
4. [useRef 与 DOM](#4-useref-与-dom)
5. [useMemo 与 useCallback](#5-usememo-与-usecallback)
6. [自定义 Hooks](#6-自定义-hooks)

---

## 1. Hooks 是什么与规则

```
Hooks = 函数组件中使用状态与生命周期的函数（React 16.8+）

⚠️ 两条铁律（面试必答）：
  ① 只能在函数组件/自定义 Hook 顶层调用
     （不能在 if/for/嵌套函数中——Hooks 靠调用顺序识别）
  ② 只能用于函数组件（不能在 class 组件中）

⚠️ 面试必答：
"Hooks 的两条规则——顶层调用（顺序即身份）、
 函数组件专用。违反会报错或状态错乱。"
```

```javascript
// ❌ 错误：条件中调用（顺序不稳定）
if (condition) {
    useState(0);      // 破坏调用顺序
}

// ✅ 正确：顶层调用
const [state, setState] = useState(0);
```

---

## 2. useState 状态管理

### 2.1 基础用法

```jsx
import { useState } from 'react';

function Counter() {
    const [count, setCount] = useState(0);

    // 更新方式一：直接值
    setCount(count + 1);

    // 更新方式二：函数式（基于上一次）
    setCount((prev) => prev + 1);       // ✅ 推荐（多次更新安全）

    // ⚠️ 批处理：同一事件多次 set 合并
    // setCount(c => c + 1);
    // setCount(c => c + 1);   → 最终 +2（函数式正确）
    // setCount(count + 1);
    // setCount(count + 1);   → 最终 +1（count 是旧值！）

    return <button onClick={() => setCount(c => c + 1)}>{count}</button>;
}
```

### 2.2 对象与数组状态

```jsx
// ⚠️ 状态不可变更新（返回新对象，不能直接改）
const [user, setUser] = useState({ name: '张三', age: 25 });

// ✅ 正确：展开 + 覆盖（不可变更新）
setUser((prev) => ({ ...prev, age: 26 }));

// ❌ 错误：直接修改（不触发渲染）
// user.age = 26;

// 数组更新
const [items, setItems] = useState([1, 2, 3]);
setItems((prev) => [...prev, 4]);           // 添加
setItems((prev) => prev.filter(x => x !== 2)); // 删除
setItems((prev) => prev.map(x => x * 2));   // 映射
```

> 🎯 **要点**：useState 三要点——函数式更新（防闭包旧值）、不可变更新（新对象）、批处理（同事件合并）。对象/数组必须展开创建新引用。

---

## 3. useEffect 副作用

### 3.1 依赖数组

```jsx
import { useEffect, useState } from 'react';

// ① 无依赖：每次渲染后执行（⚠️ 少用，会频繁执行）
useEffect(() => {
    console.log('每次渲染');
});

// ② 空数组：挂载后执行一次（请求/初始化）
useEffect(() => {
    fetchData();
}, []);                                   // ⚠️ 挂载时执行一次

// ③ 有依赖：依赖变化才执行
useEffect(() => {
    search(keyword);
}, [keyword]);                            // keyword 变化时执行

// ④ 清理函数：卸载前/下次执行前（防泄漏）
useEffect(() => {
    const timer = setInterval(() => { }, 1000);
    return () => clearInterval(timer);    // ⚠️ 清理（必写）
}, []);
```

### 3.2 useEffect 常见陷阱

```jsx
// 陷阱一：依赖数组忘了引起无限循环
useEffect(() => {
    setCount(count + 1);        // ❌ setState → 渲染 → effect → setState...
}, []);                         // 空依赖但内部 setState → 无限循环

// 陷阱二：闭包旧值
useEffect(() => {
    const id = setInterval(() => {
        console.log(count);     // ⚠️ 闭包捕获首次渲染的 count（旧值）
    }, 1000);
    return () => clearInterval(id);
}, []);                         // count 永远是最初的 0
// 解决：依赖加 count（重建定时器）或 useRef 存最新值

// 陷阱三：函数依赖
useEffect(() => {
    fetchData(userId);
}, [fetchData]);                // ⚠️ 函数每次渲染都是新引用 → 无限循环
// 解决：useCallback 稳定函数引用（见第 5 节）
```

> 🎯 **要点**：useEffect 三依赖形态（每次/一次/条件）+ 清理函数（定时器/监听必清）。**依赖数组是 useEffect 的灵魂**——漏依赖/多依赖都会出 bug。

---

## 4. useRef 与 DOM

```jsx
import { useRef, useEffect } from 'react';

function FocusInput() {
    // ① DOM 引用（最常用）
    const inputRef = useRef(null);

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    // ② 存"最新值"（闭包陷阱解法）
    const countRef = useRef(0);
    // countRef.current = count;     // 每次渲染同步最新值
    // 定时器内读 countRef.current → 永远最新

    // ③ 存可变值（不触发渲染的"状态"）
    const timerRef = useRef(null);

    return <input ref={inputRef} />;
}

// ⚠️ useRef vs useState：
//   ref 变化不触发渲染；state 变化触发渲染
//   ref 适合：DOM 引用、定时器句柄、最新值缓存
```

---

## 5. useMemo 与 useCallback

### 5.1 useMemo（缓存计算值）

```jsx
import { useMemo } from 'react';

function ExpensiveList({ items, filter }) {
    // ⚠️ 依赖不变 → 跳过重算（性能）
    const filtered = useMemo(
        () => items.filter(x => x.name.includes(filter)),
        [items, filter]                   // 依赖数组
    );
    return filtered.map(x => <li key={x.id}>{x.name}</li>);
}

// 对比：无 useMemo 每次渲染都重算过滤
// ⚠️ 注意：useMemo 不是"免费"的——依赖比较也有成本
//   小计算不需要；大数组/重计算才用
```

### 5.2 useCallback（缓存函数）

```jsx
import { useCallback } from 'react';

function Parent() {
    // ⚠️ 稳定函数引用（防止子组件 memo 失效）
    const handleClick = useCallback(() => {
        console.log('点击');
    }, []);                                // 空依赖：永远同一引用

    return <Child onClick={handleClick} />;
}

// 为什么需要：
//   函数每次渲染都是新引用 → 子组件 memo 失效（重渲染）
//   useCallback 稳定引用 → memo 生效

// React 19 变化：React Compiler 自动 memo
//   → 手写 useMemo/useCallback 需求大幅下降
//   （仍要理解原理，编译器默认关闭时手动）
```

> 🎯 **要点**：useMemo 缓存值、useCallback 缓存函数（引用稳定）——两者都配依赖数组。**配合 React.memo 使用才有意义**（见 04 渲染优化）。

---

## 6. 自定义 Hooks

### 6.1 什么是自定义 Hook

```jsx
// ⚠️ 自定义 Hook = 以 use 开头的函数（内部可调用其他 Hooks）
// hooks/useFetch.js
import { useState, useEffect } from 'react';

export function useFetch(url) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;             // ⚠️ 竞态保护
        setLoading(true);
        fetch(url)
            .then(r => r.json())
            .then(d => { if (!cancelled) setData(d); })
            .catch(e => { if (!cancelled) setError(e); })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };  // 清理
    }, [url]);                              // ⚠️ url 变化重新请求

    return { data, loading, error };
}
```

### 6.2 使用与组合

```jsx
// 使用：任意组件复用
function UserProfile({ userId }) {
    const { data, loading, error } = useFetch(`/api/users/${userId}`);
    if (loading) return <Spinner />;
    if (error) return <ErrorBox error={error} />;
    return <div>{data?.name}</div>;
}

// 组合：Hook 调用 Hook（逻辑分层）
export function useDebouncedFetch(url, delay) {
    const [debouncedUrl, setDebouncedUrl] = useState(url);
    useDebounceEffect(() => setDebouncedUrl(url), delay);  // 复用防抖 Hook
    return useFetch(debouncedUrl);
}
```

### 6.3 自定义 Hook 规范

```
✅ use 前缀（识别 + lint 检查）
✅ 内部只调用 Hooks（顶层）
✅ 返回数组/对象（命名清晰）
✅ 清理函数必写（防泄漏）
✅ 竞态保护（cancelled 标志）
⚠️ 命名：useFetch/useLocalStorage/useDebounce/useClickOutside
```

> 🎯 **要点**：自定义 Hook = 逻辑复用（替代 HOC/render props）+ 竞态保护 + 清理对称。**useFetch/useDebounce/useLocalStorage** 是三大样板。

---

> 🎯 **核心要点**：Hooks 体系 = **useState**（函数式 + 不可变更新）+ **useEffect**（依赖数组 + 清理 + 闭包陷阱）+ **useRef**（DOM/最新值/不触发渲染）+ **useMemo/useCallback**（缓存 + 引用稳定）+ **自定义 Hook**（复用 + 竞态保护）。面试主线：闭包陷阱、依赖数组、无限循环三问必考。

---

**返回总览**：[00-React总览与核心概念](00-React总览与核心概念.md) | **上一篇**：[01-React组件与JSX](01-React组件与JSX.md) | **下一篇**：[03-React状态管理与Context](03-React状态管理与Context.md)
