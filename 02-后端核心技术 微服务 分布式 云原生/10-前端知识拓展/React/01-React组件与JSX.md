# 01 - React 组件与 JSX

> 定位：JSX 语法、函数组件、props 与 children、条件/列表渲染、表单受控组件——React 写界面的全部基础

## 📚 目录

1. [JSX 语法](#1-jsx-语法)
2. [函数组件与 props](#2-函数组件与-props)
3. [条件与列表渲染](#3-条件与列表渲染)
4. [表单与受控组件](#4-表单与受控组件)
5. [事件处理](#5-事件处理)

---

## 1. JSX 语法

### 1.1 JSX 是什么

```jsx
// JSX = JavaScript 的 XML 扩展（写界面的语法糖）
// 本质：createElement 调用的语法糖（编译后是 JS 函数调用）

const element = <h1 className="title">Hello</h1>;
// 编译为：React.createElement('h1', { className: 'title' }, 'Hello')
```

### 1.2 JSX 规则

```jsx
function App() {
    const name = '张三';
    const isActive = true;

    return (
        // ① 必须有单一根节点（或用 Fragment）
        <>
            {/* ② 表达式用 {} 包裹（不是字符串插值） */}
            <h1>你好，{name}</h1>

            {/* ③ 属性用驼峰（className/htmlFor 非 class/for） */}
            <div className="card" tabIndex={0}>
            </div>

            {/* ④ 内联样式是对象 */}
            <div style={{ color: 'red', fontSize: '16px' }}>
            </div>

            {/* ⑤ 注释写法 */}
            {/* 这是注释 */}

            {/* ⑥ 条件渲染用表达式 */}
            <span>{isActive ? '启用' : '禁用'}</span>
        </>
    );
}
```

> 🎯 **要点**：JSX 六规则——单根节点、{} 表达式、驼峰属性、样式对象、条件表达式、注释。JSX 本质是 JS 表达式（可赋值/传参/返回）。

---

## 2. 函数组件与 props

### 2.1 组件定义

```jsx
// 函数组件 = 接收 props 返回 JSX 的函数（首字母大写）
function Greeting({ name, age = 18 }) {       // ⚠️ 解构 + 默认值
    return <p>你好，{name}（{age} 岁）</p>;
}

// 箭头函数组件（现代写法）
const Greeting = ({ name }) => <p>你好，{name}</p>;

// 使用
function App() {
    return (
        <div>
            <Greeting name="张三" age={25} />
            <Greeting name="李四" />           {/* age 用默认 18 */}
        </div>
    );
}
```

### 2.2 props 的特性

```jsx
// ① props 只读（不可修改——单向数据流）
// ② children：嵌套内容的特殊 prop
function Card({ title, children }) {
    return (
        <div className="card">
            <h3>{title}</h3>
            {children}          {/* 组件嵌套内容 */}
        </div>
    );
}
<Card title="标题">
    <p>这是 children 内容</p>
</Card>

// ③ 展开传递
<Greeting {...user} />          {/* 展开对象为多个 props */}

// ④ 渲染函数 prop（render prop 模式）
<DataList renderItem={(item) => <li>{item.name}</li>} />
```

> 🎯 **要点**：props 单向只读 + children 嵌套 + 展开传递是组件复用的基础三式。**props 变化 → 组件重新渲染**是 React 更新的起点。

---

## 3. 条件与列表渲染

### 3.1 条件渲染

```jsx
function Status({ isLoggedIn, user }) {
    // ① 三元（最常用）
    return isLoggedIn ? <Profile user={user} /> : <Login />;

    // ② 逻辑与（真值才渲染）
    // {isLoggedIn && <Welcome />}

    // ③ 变量分支
    // let content = isLoggedIn ? <Profile/> : <Login/>;
    // return <div>{content}</div>;

    // ⚠️ 注意：数字 0 会被渲染（0 && x 输出 0）
    // 用 !!count 或 count > 0
    return <div>{count > 0 && `共 ${count} 条`}</div>;
}
```

### 3.2 列表渲染

```jsx
function UserList({ users }) {
    return (
        <ul>
            {/* ⚠️ key 必须（唯一稳定，不要用 index） */}
            {users.map((user) => (
                <li key={user.id}>
                    {user.name} - {user.age}
                </li>
            ))}
        </ul>
    );
}

// key 的用途：diff 时识别节点复用
// ⚠️ 列表排序/过滤时用 index 作 key 会导致状态错乱

// 分组渲染
{Object.entries(grouped).map(([city, list]) => (
    <section key={city}>
        <h2>{city}</h2>
        {list.map(user => <UserCard key={user.id} user={user} />)}
    </section>
))}
```

---

## 4. 表单与受控组件

### 4.1 受控组件

```jsx
// ⚠️ 受控组件：表单值由 state 控制（React 权威单一来源）
import { useState } from 'react';

function LoginForm() {
    const [form, setForm] = useState({
        username: '',
        password: '',
        agree: false,
    });

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setForm((prev) => ({
            ...prev,
            // ⚠️ checkbox 用 checked、其他用 value
            [name]: type === 'checkbox' ? checked : value,
        }));
    };

    const handleSubmit = (e) => {
        e.preventDefault();              // ⚠️ 阻止默认提交
        console.log(form);
    };

    return (
        <form onSubmit={handleSubmit}>
            <input name="username" value={form.username}
                   onChange={handleChange} />
            <input name="password" type="password"
                   value={form.password} onChange={handleChange} />
            <input name="agree" type="checkbox"
                   checked={form.agree} onChange={handleChange} />
            <button type="submit">登录</button>
        </form>
    );
}
```

### 4.2 非受控 vs 受控

| 维度 | 受控 | 非受控 |
|------|:---:|:---:|
| 值来源 | state | DOM（ref 读取） |
| 实时校验 | ✅ | ❌ |
| 即时联动 | ✅ | ❌ |
| 适用 | 表单校验/联动 | 简单一次性读取 |

```jsx
// 非受控（ref 读取，少见）
import { useRef } from 'react';
function Uncontrolled() {
    const inputRef = useRef(null);
    const submit = () => console.log(inputRef.current.value);
    return <input ref={inputRef} defaultValue="初始值" />;
}
```

> 🎯 **要点**：受控组件 = "state 是唯一数据源"（value + onChange 成对出现）。受控表单 + 校验是 React 表单的标准姿势。

---

## 5. 事件处理

```jsx
function EventDemo() {
    // 事件处理器（首字母大写驼峰：onClick 非 onclick）
    const handleClick = (e) => {
        e.preventDefault();              // 阻止默认
        e.stopPropagation();             // 停止冒泡
        console.log('clicked', e.target);
    };

    // 带参
    const handleDelete = (id) => (e) => {   // ⚠️ 柯里化：返回函数
        console.log('删除', id);
    };

    return (
        <div>
            <button onClick={handleClick}>点击</button>

            {/* ⚠️ 不要直接写调用（会立即执行） */}
            {/* ❌ onClick={handleDelete(1)} —— 渲染时就执行了 */}
            {/* ✅ onClick={() => handleDelete(1)} */}
            <button onClick={() => handleDelete(1)}>删除 1</button>

            {/* 事件对象 */}
            <input onChange={(e) => console.log(e.target.value)} />
        </div>
    );
}

// ⚠️ React 事件是合成事件（SyntheticEvent）
//   统一浏览器差异、事件委托到根节点
//   异步访问 e 需 e.persist()（旧版）或先取值
```

> 🎯 **要点**：事件三要点——柯里化传参（避免立即执行）、合成事件（统一差异）、受控 onChange。`onClick={() => fn(arg)}` 模式是传参的标准答案。

---

> 🎯 **核心要点**：组件与 JSX = **JSX 六规则**（单根/{}表达式/驼峰）+ **props 单向流**（只读 + children + 展开）+ **列表 key**（唯一稳定）+ **受控表单**（state 唯一源）+ **事件**（柯里化传参）。这些是 React 写界面的全部语法面——之后的所有概念（Hooks/状态/性能）都建立在这之上。

---

**返回总览**：[00-React总览与核心概念](00-React总览与核心概念.md) | **下一篇**：[02-ReactHooks全解](02-ReactHooks全解.md)
