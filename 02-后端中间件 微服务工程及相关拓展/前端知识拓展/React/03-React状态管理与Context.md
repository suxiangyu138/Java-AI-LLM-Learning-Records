# 03 - React 状态管理与 Context

> 定位：状态提升、Context 跨层传递、useReducer、Zustand/Redux——React 状态管理的完整层次

## 📚 目录

1. [状态管理的层次](#1-状态管理的层次)
2. [状态提升](#2-状态提升)
3. [Context 跨层共享](#3-context-跨层共享)
4. [useReducer](#4-usereducer)
5. [Zustand 与 Redux](#5-zustand-与-redux)

---

## 1. 状态管理的层次

```
React 状态管理的递进层次：
  ① 组件内 state：局部状态（useState）
  ② 状态提升：兄弟组件共享（放共同父级）
  ③ Context：跨层传递（主题/用户/语言）
  ④ useReducer：复杂状态逻辑（多操作/联动）
  ⑤ 外部库：Zustand/Redux（全局共享 + 中间件）

⚠️ 面试必答：
"状态管理从简到繁——先 useState，
 共享提升，跨层 Context，复杂逻辑
 useReducer，全局才上 Zustand/Redux。"
```

---

## 2. 状态提升

```jsx
// ⚠️ 状态提升：兄弟组件共享状态 → 放到共同父组件

// 子组件：受控（状态由父管理）
function Child({ value, onChange }) {
    return <input value={value} onChange={onChange} />;
}

// 父组件：持有共享状态
function Parent() {
    const [value, setValue] = useState('');

    return (
        <div>
            {/* 两个输入框共享同一状态 */}
            <Child value={value} onChange={(e) => setValue(e.target.value)} />
            <Child value={value} onChange={(e) => setValue(e.target.value)} />
            <p>当前值：{value}</p>
        </div>
    );
}
// ⚠️ 单向数据流：状态在父、数据向下、回调向上
```

---

## 3. Context 跨层共享

### 3.1 基础用法

```jsx
// ThemeContext.js
import { createContext, useContext } from 'react';

// ① 创建 Context（含默认值）
export const ThemeContext = createContext('light');

// ② 提供者：包裹子树提供值
export function ThemeProvider({ children }) {
    const [theme, setTheme] = useState('light');
    return (
        <ThemeContext.Provider value={{ theme, setTheme }}>
            {children}
        </ThemeContext.Provider>
    );
}

// ③ 消费：任意后代组件使用（免逐层传 props）
function Button() {
    const { theme, setTheme } = useContext(ThemeContext);
    return (
        <button
            className={theme}
            onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
        >
            切换主题
        </button>
    );
}
```

### 3.2 Context 的注意事项

```jsx
// ⚠️ 性能陷阱：value 每次渲染都是新对象 → 全部消费者重渲染
// 解决 1：useMemo 稳定 value
const value = useMemo(() => ({ theme, setTheme }), [theme, setTheme]);

// 解决 2：Context 拆分（细分避免大范围重渲染）
export const ThemeContext = createContext('light');
export const UserContext = createContext(null);

// ⚠️ Context vs props：
//   Context：跨层（3+ 层）省去逐层传递
//   props：显式清晰（1-2 层优先 props）
// ⚠️ Context vs 外部状态库：
//   Context 变化 → 全部消费者重渲染（无选择性）
//   Zustand 可订阅特定 slice（性能更好）
```

> 🎯 **要点**：Context 三件套——createContext（默认值）、Provider（提供）、useContext（消费）。**useMemo 稳定 value** 是性能关键；大范围状态用 Zustand 更精细。

---

## 4. useReducer

### 4.1 基础用法

```jsx
import { useReducer } from 'react';

// ① 定义 reducer：纯函数（旧状态 + action → 新状态）
function counterReducer(state, action) {
    switch (action.type) {
        case 'increment':
            return { count: state.count + 1 };
        case 'decrement':
            return { count: state.count - 1 };
        case 'reset':
            return { count: 0 };
        default:
            return state;
    }
}

function Counter() {
    // ② useReducer(reducer, 初始值)
    const [state, dispatch] = useReducer(counterReducer, { count: 0 });

    return (
        <div>
            <p>{state.count}</p>
            <button onClick={() => dispatch({ type: 'increment' })}>+1</button>
            <button onClick={() => dispatch({ type: 'reset' })}>重置</button>
        </div>
    );
}
```

### 4.2 useReducer vs useState

| 维度 | useState | useReducer |
|------|:---:|:---:|
| 适用 | 简单状态 | 复杂联动逻辑 |
| 更新 | 直接 set | dispatch action |
| 可测试 | 较弱 | ✅ reducer 纯函数 |
| 可预测 | 一般 | ✅ 集中管理 |
| 选型 | 默认 | 多个互相关联的状态 |

```jsx
// ⚠️ 适用场景：多个状态联动（表单多字段/购物车）
// 例：购物车 add/remove/clear/update 四个 action
function cartReducer(state, action) {
    switch (action.type) {
        case 'add':
            return { ...state, items: [...state.items, action.item] };
        case 'remove':
            return {
                ...state,
                items: state.items.filter(i => i.id !== action.id),
            };
        case 'clear':
            return { items: [] };
        default:
            return state;
    }
}
```

---

## 5. Zustand 与 Redux

### 5.1 Zustand（现代推荐）

```jsx
// store.js —— Zustand（轻量 + 选择订阅）
import { create } from 'zustand';

export const useStore = create((set) => ({
    user: null,
    cart: [],

    // actions 直接定义（免 action type 样板）
    login: (user) => set({ user }),
    logout: () => set({ user: null }),
    addToCart: (item) =>
        set((state) => ({ cart: [...state.cart, item] })),
}));

// 组件使用
function CartButton() {
    // ⚠️ 选择订阅：只订阅 cart → cart 变化才重渲染
    const cart = useStore((state) => state.cart);
    const addToCart = useStore((state) => state.addToCart);
    return <button onClick={() => addToCart({ id: 1 })}>{cart.length}</button>;
}
```

### 5.2 Redux Toolkit（大型工程）

```jsx
// slices/userSlice.js —— Redux Toolkit（官方推荐写法）
import { createSlice } from '@reduxjs/toolkit';

export const userSlice = createSlice({
    name: 'user',
    initialState: { name: '', token: null },
    reducers: {
        setUser: (state, action) => {
            state.name = action.payload.name;   // ⚠️ Immer 直接修改
            state.token = action.payload.token;
        },
        logout: (state) => {
            state.name = '';
            state.token = null;
        },
    },
});
export const { setUser, logout } = userSlice.actions;

// 组件使用（React-Redux Hooks）
import { useSelector, useDispatch } from 'react-redux';
const user = useSelector((s) => s.user);
const dispatch = useDispatch();
dispatch(setUser({ name: '张三', token: 'x' }));
```

### 5.3 状态库选型

| 库 | 特点 | 适用 |
|------|------|------|
| Zustand | 轻量 + 选择订阅 + 无样板 | **现代首选** |
| Redux Toolkit | 功能全 + DevTools + 中间件 | 大型复杂工程 |
| Jotai | 原子状态（细粒度） | 中等 |
| Context | 内置 | 跨层小范围 |

> 🎯 **要点**：2026 状态管理选型——**小范围跨层用 Context、全局共享用 Zustand**（轻量）、大型复杂工程 Redux Toolkit。Zustand 的选择订阅（只订阅需要的状态）是性能优势核心。

---

> 🎯 **核心要点**：状态管理体系 = **状态提升**（兄弟共享 → 共同父级）+ **Context**（跨层 + useMemo 稳定 value）+ **useReducer**（复杂联动逻辑）+ **Zustand/Redux**（全局共享 + 选择订阅）。选型阶梯："useState → 提升 → Context → Reducer → 外部库"由简到繁。

---

**返回总览**：[00-React总览与核心概念](00-React总览与核心概念.md) | **上一篇**：[02-ReactHooks全解](02-ReactHooks全解.md) | **下一篇**：[04-React渲染与性能优化](04-React渲染与性能优化.md)
