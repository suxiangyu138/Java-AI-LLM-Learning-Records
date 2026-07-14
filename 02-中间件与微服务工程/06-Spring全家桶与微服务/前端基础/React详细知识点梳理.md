# React 详细知识点梳理

> **文档定位**：Java 后端技术参考文档 | React 核心知识点全景  
> **核心说明**：React 是 Facebook（Meta）开发的开源前端 JavaScript 库，用于构建用户界面（UI），核心理念是组件化和声明式编程，替代传统命令式操作 DOM  
> **核心优势**：组件化、声明式编程、虚拟 DOM、单向数据流、跨平台（React Native）

---

## 目录

- [一、React 基础核心](#一react-基础核心)
- [二、React 组件](#二react-组件)
- [三、React Hooks（React 16.8+ 核心）](#三react-hooksreact-168-核心)
- [四、React 事件处理](#四react-事件处理)
- [五、条件渲染与列表渲染](#五条件渲染与列表渲染)
- [六、组件生命周期](#六组件生命周期)
- [七、React 状态管理](#七react-状态管理)
- [八、React 路由（React Router）](#八react-路由react-router)
- [九、React 性能优化](#九react-性能优化)

---

## 一、React 基础核心

### 1.1 React 简介

React 是由 Facebook（现 Meta）开发并维护的开源前端 JavaScript 库，用于构建用户界面（UI），尤其适合构建复杂、交互式的单页面应用（SPA）。

| 核心优势 | 说明 |
|----------|------|
| **组件化** | UI 拆分为独立、可复用的组件 |
| **声明式编程** | 只需描述"UI 应该是什么样子" |
| **虚拟 DOM** | 内存中虚拟 DOM 树替代真实 DOM，减少 DOM 操作 |
| **单向数据流** | 数据从父组件流向子组件，状态管理清晰 |
| **跨平台** | 结合 React Native 开发 iOS、Android 应用 |

### 1.2 环境搭建

```bash
# 方式 1：Create React App（官方推荐，快速搭建）
npx create-react-app my-react-app
cd my-react-app
npm start  # 默认端口 3000

# 方式 2：手动配置（Webpack + Babel）
# 适合需要自定义配置的场景
```

### 1.3 JSX 语法

JSX 是 React 独创的语法，允许在 JavaScript 中编写 HTML 样的代码，本质是 `React.createElement()` 的语法糖。

#### JSX 基本规则

| 规则 | 说明 | 示例 |
|------|------|------|
| **标签闭合** | 单标签需加 `/` | `<img src="" />` |
| **根节点唯一** | 一个 JSX 表达式只能有一个根节点 | 使用 `<Fragment>` 或 `<>...</>` |
| **类名用 `className`** | HTML `class` → JSX `className` | `<div className="box">` |
| **内联样式用对象** | 驼峰命名 | `style={{ fontSize: '16px' }}` |
| **表达式嵌入 `{}`** | 嵌入 JS 表达式 | `<div>{1 + 1}</div>` |
| **注释** | `{/* 注释 */}` | — |

#### JSX 与 HTML 的区别

| HTML | JSX |
|------|-----|
| `class` | `className` |
| `style="font-size: 16px"` | `style={{ fontSize: '16px' }}` |
| `onclick` | `onClick`（驼峰命名） |

---

## 二、React 组件

### 2.1 函数组件（推荐）

```jsx
// 基础函数组件
function Hello(props) {
  return <h1>Hello, {props.name}</h1>;
}

// 箭头函数写法
const Hello = (props) => {
  const { name } = props;  // 解构 props
  return <h1>Hello, {name}</h1>;
};

// 使用
function App() {
  return <Hello name="React" />;
}
```

### 2.2 类组件（传统方式）

```jsx
import React from 'react';

class Hello extends React.Component {
  constructor(props) {
    super(props);
    this.state = { count: 0 };
    this.handleClick = this.handleClick.bind(this);
  }

  handleClick() {
    this.setState({ count: this.state.count + 1 });
  }

  render() {
    return (
      <div>
        <h1>Hello, {this.props.name}</h1>
        <p>计数：{this.state.count}</p>
        <button onClick={this.handleClick}>点击+1</button>
      </div>
    );
  }
}
```

### 2.3 Props（父子组件通信）

| 特点 | 说明 |
|------|------|
| **只读性** | 子组件不能修改 props，只能使用 |
| **可传递任意类型** | 数字、字符串、布尔、数组、对象、函数、组件 |
| **默认值** | `组件.defaultProps` |
| **类型校验** | `PropTypes`（需安装 `prop-types` 包） |

```jsx
import PropTypes from 'prop-types';

const Hello = ({ name, age, isStudent }) => (
  <div>
    <p>姓名：{name}</p>
    <p>年龄：{age}</p>
    <p>是否学生：{isStudent ? '是' : '否'}</p>
  </div>
);

Hello.defaultProps = { age: 18, isStudent: false };

Hello.propTypes = {
  name: PropTypes.string.isRequired,
  age: PropTypes.number,
  isStudent: PropTypes.bool
};
```

### 2.4 组件状态（State）

State 是组件内部的私有数据，用于管理动态变化。

| 特点 | 说明 |
|------|------|
| **私有性** | 只有组件自身能修改 |
| **不可直接修改** | 必须用 `setState()` 或 `useState` |
| **异步性** | `setState()` 是异步操作 |

#### 函数组件中使用 State（useState）

```jsx
import { useState } from 'react';

const Counter = () => {
  const [count, setCount] = useState(0);

  const increment = () => {
    setCount(prevCount => prevCount + 1);  // 推荐：传递函数
  };

  return (
    <div>
      <p>计数：{count}</p>
      <button onClick={increment}>+1</button>
    </div>
  );
}
```

---

## 三、React Hooks（React 16.8+ 核心）

Hooks 允许函数组件拥有状态和生命周期，无需编写类组件。

> **核心规则**：只能在函数组件顶层调用，不能在循环/条件/嵌套函数中调用。

### 3.1 useState — 管理组件状态

```jsx
const [user, setUser] = useState({ name: '小明', age: 18 });

// 修改对象/数组时，需返回新对象（不能直接修改原数据）
setUser({ ...user, age: 19 });  // 正确
```

### 3.2 useEffect — 处理副作用

替代类组件的生命周期方法（`componentDidMount`、`componentDidUpdate`、`componentWillUnmount`）。

```jsx
import { useState, useEffect } from 'react';

const Timer = () => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCount(prev => prev + 1);
    }, 1000);
    
    // 清理函数（组件卸载时执行）
    return () => clearInterval(timer);
  }, []);  // 空依赖 = 只执行一次

  return <p>定时器计数：{count}</p>;
}
```

| 依赖项 | 执行时机 |
|--------|----------|
| 不写 | 每次渲染都执行 |
| `[]`（空数组） | 仅在初始渲染时（`componentDidMount`） |
| `[count]` | count 变化时（`componentDidUpdate`） |
| 返回清理函数 | 卸载时（`componentWillUnmount`） |

### 3.3 useContext — 跨组件通信

解决 props 层层传递问题。

```jsx
import { createContext, useContext, useState } from 'react';

// 1. 创建 Context
const ThemeContext = createContext('light');

// 2. Provider 提供数据
function App() {
  const [theme, setTheme] = useState('light');
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <Header />
      <Content />
    </ThemeContext.Provider>
  );
}

// 3. 子组件使用
function Content() {
  const { theme, setTheme } = useContext(ThemeContext);
  return <button onClick={() => setTheme(prev => prev === 'light' ? 'dark' : 'light')}>
    当前主题：{theme}
  </button>;
}
```

### 3.4 useRef — 获取 DOM 或保存持久值

```jsx
import { useRef, useEffect } from 'react';

const RefDemo = () => {
  const inputRef = useRef(null);    // 获取 DOM
  const countRef = useRef(0);       // 保存持久值（修改不触发渲染）

  useEffect(() => {
    inputRef.current.focus();       // 自动聚焦
    countRef.current += 1;
  }, []);

  return <input ref={inputRef} type="text" />;
}
```

### 3.5 其他常用 Hooks

| Hook | 用途 |
|------|------|
| `useReducer` | 替代 useState，管理复杂状态 |
| `useMemo` | 缓存计算结果，避免重复计算 |
| `useCallback` | 缓存函数，避免重复创建 |
| `useLayoutEffect` | 与 useEffect 类似，但在浏览器绘制前执行 |

### 3.6 自定义 Hooks

```jsx
// 封装"获取窗口大小"
import { useState, useEffect } from 'react';

function useWindowSize() {
  const [size, setSize] = useState({
    width: window.innerWidth,
    height: window.innerHeight
  });

  useEffect(() => {
    const handleResize = () => {
      setSize({ width: window.innerWidth, height: window.innerHeight });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return size;
}

// 使用
const { width, height } = useWindowSize();
```

---

## 四、React 事件处理

| 差异 | 原生 DOM | React |
|------|----------|-------|
| 命名 | `onclick` | `onClick`（驼峰） |
| 传参 | 字符串 `"handleClick()"` | 函数引用 `{handleClick}` |
| 事件对象 | 原生事件 | 合成事件（SyntheticEvent） |

```jsx
const EventDemo = () => {
  const handleClick = () => alert('按钮被点击');
  
  // 带参数
  const handleDelete = (id) => alert(`删除 id=${id}`);
  
  // 输入框变化
  const handleChange = (e) => setValue(e.target.value);

  return (
    <div>
      <button onClick={handleClick}>点击</button>
      <button onClick={() => handleDelete(123)}>删除</button>
      <input value={value} onChange={handleChange} />
    </div>
  );
}
```

---

## 五、条件渲染与列表渲染

### 条件渲染

```jsx
// 三元运算符（二选一）
{isLogin ? <button>退出</button> : <button>登录</button>}

// 逻辑与（条件成立时渲染）
{isLogin && <p>已登录</p>}

// if-else（复杂条件）
let content;
if (role === 'admin') content = <p>管理员</p>;
else content = <p>普通用户</p>;
```

### 列表渲染

```jsx
const [todos, setTodos] = useState([
  { id: 1, text: '学习 React' },
  { id: 2, text: '掌握 Hooks' }
]);

return (
  <ul>
    {todos.map((todo) => (
      <li key={todo.id}>{todo.text}</li>  {/* key 必须唯一 */}
    ))}
  </ul>
);
```

> **重要**：每个列表项必须有唯一的 `key` 属性，推荐使用数据 `id`，避免使用 `index`。

---

## 六、组件生命周期

### 类组件生命周期

| 阶段 | 核心方法 | 说明 |
|------|----------|------|
| **挂载** | `constructor()` → `render()` → `componentDidMount()` | DOM 已渲染，可发网络请求 |
| **更新** | `shouldComponentUpdate()` → `render()` → `componentDidUpdate()` | 避免在此修改 state |
| **卸载** | `componentWillUnmount()` | 清理定时器、取消请求 |

### 函数组件"Hooks 替代"

| 类组件生命周期 | useEffect 等价写法 |
|----------------|-------------------|
| `componentDidMount` | `useEffect(() => {}, [])` |
| `componentDidUpdate` | `useEffect(() => {}, [依赖项])` |
| `componentWillUnmount` | `useEffect(() => { return 清理函数 }, [])` |

---

## 七、React 状态管理

### 方案对比

| 方案 | 适用规模 | 特点 |
|------|----------|------|
| **useContext + useReducer** | 中小型 | 无需第三方库，React 内置 |
| **Redux / Redux Toolkit** | 中大型 | 独立状态管理库，生态成熟 |
| **MobX** | 中小型 | 观察者模式，语法简洁 |
| **Zustand** | 中小型 | 轻量，无需 Provider 包裹 |
| **Recoil** | 复杂应用 | 原子化状态，专门为 React 设计 |

### Redux 核心概念

```
Store（全局状态容器）→ Action（描述变化）→ Reducer（修改状态）→ Dispatch（触发变化）
```

---

## 八、React 路由（React Router）

```bash
npm install react-router-dom
```

### 核心组件（v6）

| 组件 | 作用 |
|------|------|
| `<BrowserRouter>` | 路由容器 |
| `<Routes>` | 路由规则容器，替代 v5 的 `<Switch>` |
| `<Route path="/" element={<Comp />}>` | 路由规则 |
| `<Link to="/">` | 跳转链接（避免页面刷新） |
| `<NavLink>` | 带激活状态的 Link |
| `<Outlet>` | 嵌套路由占位符 |

### 基本使用

```jsx
import { BrowserRouter, Routes, Route, Link, useParams, useNavigate } from 'react-router-dom';

const Home = () => <h1>首页</h1>;
const About = () => <h1>关于我们</h1>;
const UserDetail = () => {
  const { id } = useParams();  // 获取动态参数
  return <h1>用户 ID：{id}</h1>;
};

function App() {
  const navigate = useNavigate();

  return (
    <BrowserRouter>
      <nav>
        <Link to="/">首页</Link>
        <Link to="/about">关于</Link>
        <Link to="/user/1">用户 1</Link>
      </nav>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/user/:id" element={<UserDetail />} />
        <Route path="*" element={<h1>404</h1>} />
      </Routes>
    </BrowserRouter>
  );
}
```

### 编程式导航

```jsx
const navigate = useNavigate();
navigate('/', { replace: true });  // 跳转
navigate(-1);                       // 返回上一页
```

---

## 九、React 性能优化

| 优化手段 | 说明 |
|----------|------|
| **React.memo** | 包装函数组件，props 未变化时不重新渲染 |
| **useMemo** | 缓存计算结果，依赖不变时不重新计算 |
| **useCallback** | 缓存函数，避免子组件接收新函数引用 |
| **避免渲染时创建对象/函数** | `style={{}}` / `onClick={() => {}}` 提取为常量 |
| **懒加载** | `React.lazy()` + `<Suspense>` 按需加载组件 |

```jsx
import { memo, useMemo, useCallback } from 'react';

// React.memo 包装子组件
const Child = memo(({ name, onClick }) => {
  console.log('子组件渲染');
  return <button onClick={onClick}>{name}</button>;
});

const Parent = () => {
  const [count, setCount] = useState(0);

  // useCallback 缓存函数
  const handleClick = useCallback(() => {
    console.log('点击');
  }, []);

  // useMemo 缓存计算结果
  const doubleCount = useMemo(() => count * 2, [count]);

  return (
    <div>
      <p>count: {count}, double: {doubleCount}</p>
      <button onClick={() => setCount(c => c + 1)}>+1</button>
      <Child name="按钮" onClick={handleClick} />
    </div>
  );
}
```

---

## 后端开发者 React 核心关注点

| 优先级 | 知识点 | 理由 |
|--------|--------|------|
| ⭐⭐⭐⭐⭐ | 组件 + Props + State | 理解前端组件交互模式 |
| ⭐⭐⭐⭐⭐ | Hooks（useState/useEffect） | 主流代码风格 |
| ⭐⭐⭐⭐ | 事件处理 | 理解前端交互逻辑 |
| ⭐⭐⭐⭐ | 路由 | 理解 SPA 路由与后端接口路由的区别 |
| ⭐⭐⭐ | 状态管理 | 理解全局状态与后端数据的对应 |
| ⭐⭐⭐ | 性能优化 | 理解前端渲染优化思路 |
