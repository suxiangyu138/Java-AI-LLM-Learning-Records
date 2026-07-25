# React 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：React 中 useState 的更新是同步还是异步？如何获取最新状态？

**面试官意图：** 考察对 React 状态更新机制的理解深度，区分批量更新与即时更新。

**完美解答：**

在 React 中，`useState` 的更新行为取决于调用上下文：

**在事件处理函数和生命周期函数中**，状态更新是**异步**的。React 会进入批量更新模式（batching），将多个 `setState` 合并为一次重新渲染，以提高性能。例如连续调用三次 `setCount(count + 1)`，最终只会加 1 而不是加 3，因为每次读取的都是同一个闭包中的旧值。

**在 setTimeout、Promise、原生事件监听器、或 async 函数中**（React 18 之前），更新是**同步**的，每次 `setState` 都会触发一次重新渲染。React 18 通过 `createRoot` 实现了自动批量更新（Automatic Batching），即使在上述异步上下文中也会批量处理。

**获取最新状态的正确方式：**
- 使用函数式更新：`setCount(prev => prev + 1)` — 确保每次基于最新状态计算。
- 使用 `useEffect` 监听状态变化：`useEffect(() => { console.log(count); }, [count])`。
- 使用 `useRef` 保存最新值：`const countRef = useRef(count); countRef.current = count;`。

```jsx
// 错误示例：三次调用只加 1
const handleWrong = () => {
  setCount(count + 1);
  setCount(count + 1);
  setCount(count + 1); // 结果：count + 1
};

// 正确示例：函数式更新，三次调用加 3
const handleCorrect = () => {
  setCount(prev => prev + 1);
  setCount(prev => prev + 1);
  setCount(prev => prev + 1); // 结果：count + 3
};
```

**延伸追问应对：**

- **问："React 18 的自动批量更新如何关闭？"** 答：可以使用 `flushSync` 强制同步刷新，但官方不推荐关闭自动批量更新。`flushSync(() => { setCount(c => c + 1); })` 会立即提交更新并重新渲染。
- **问："类组件的 setState 和函数组件的 useState 有什么区别？"** 答：类组件的 `setState` 会自动合并对象状态，而 `useState` 需要手动合并（通常使用展开运算符或 `useReducer` 处理复杂状态）。

---

### Q2：useEffect 的依赖数组是如何进行比较的？为什么会出现无限循环？

**面试官意图：** 考察对副作用执行机制的理解，以及排查常见陷阱的能力。

**完美解答：**

React 使用 `Object.is`（类似于 `===`）对依赖数组中的每个值进行**浅比较**（shallow compare）。如果某个依赖项在上次渲染和本次渲染之间发生了变化，则该副作用会在渲染提交后重新执行。

**无限循环的常见原因：**

1. **依赖项是对象或数组（引用不变性破坏）：** 每次渲染都创建新对象，即使内容相同引用也不同，导致 `useEffect` 每次都会执行。

```jsx
// 错误：每次渲染创建新对象，导致无限循环
useEffect(() => {
  fetchData();
}, [{ id: 1 }]); // 每次都是新引用

// 正确：使用基本类型依赖
useEffect(() => {
  fetchData({ id: 1 });
}, []);
```

2. **在 useEffect 中修改了依赖项本身：**

```jsx
// 错误：修改了依赖项，触发重新渲染，再次执行
const [count, setCount] = useState(0);
useEffect(() => {
  setCount(count + 1); // 修改 count，而 count 在依赖中
}, [count]); // 导致无限循环
```

3. **函数作为依赖项未包裹 useCallback：**

```jsx
// 错误：每次渲染 handleClick 都是新函数
const handleClick = () => { setOpen(!open); };
useEffect(() => {
  window.addEventListener('click', handleClick);
  return () => window.removeEventListener('click', handleClick);
}, [handleClick]); // 每次渲染 handleClick 引用都变了
```

**排查技巧：**
- 使用 React DevTools Profiler 记录渲染原因。
- 检查组件的 Props 中是否有匿名对象/函数。
- 使用 `useRef` 保存不需要触发副作用的可变值。
- 合理使用 ESLint 的 `react-hooks/exhaustive-deps` 规则。

**延伸追问应对：**

- **问："Effect 的清理函数何时执行？"** 答：在组件卸载时，以及每次重新执行 Effect 之前（先执行上一次的清理函数，再执行新的 Effect）。

---

### Q3：React 中父组件重新渲染时，子组件一定会重新渲染吗？如何优化？

**面试官意图：** 考察对 React 渲染机制的理解和性能优化能力。

**完美解答：**

默认情况下，**当父组件重新渲染时，所有子组件都会重新渲染**，即使子组件的 Props 没有变化。这是因为 React 默认会递归渲染整个组件树。

**优化手段（按推荐优先级）：**

| 优化手段 | 适用场景 | 原理 |
|---------|---------|------|
| `React.memo` | 纯展示组件、Props 不变的子组件 | 对 Props 进行浅比较，无变化则跳过渲染 |
| `useMemo` | 计算量大的值 | 缓存计算结果，依赖不变时复用 |
| `useCallback` | 传递给子组件的回调函数 | 缓存函数引用，避免子组件因 Props 引用变化而重新渲染 |
| `useId` | 服务端渲染相关 | 生成唯一 ID，避免不必要渲染 |
| 状态下沉 | 只影响局部状态 | 将状态移动到只使用它的组件中 |
| 组件拆分 | 大组件拆分 | 将变化部分和不变部分拆分为独立组件 |

```jsx
// 使用 React.memo 包裹子组件
const ChildComponent = React.memo(({ data, onClick }) => {
  console.log('子组件渲染');
  return <div onClick={onClick}>{data.name}</div>;
});

// 父组件中使用 useCallback 和 useMemo
function Parent() {
  const [count, setCount] = useState(0);
  const [data, setData] = useState({ name: 'React' });

  // 缓存回调函数，防止每次渲染都创建新引用
  const handleClick = useCallback(() => {
    console.log('clicked');
  }, []);

  // 缓存计算结果
  const processedData = useMemo(() => ({
    ...data,
    formatted: data.name.toUpperCase()
  }), [data]);

  return (
    <>
      <ChildComponent data={processedData} onClick={handleClick} />
      <button onClick={() => setCount(c => c + 1)}>Count: {count}</button>
    </>
  );
}
```

> ⚠️ **注意：** 不要滥用 `React.memo` 和 `useMemo`。对于简单的渲染，浅比较的开销可能比重新渲染更大。只有在对性能有实际影响时才使用。

**延伸追问应对：**

- **问："`React.memo` 如何自定义比较逻辑？"** 答：`React.memo(Component, (prevProps, nextProps) => { /* 返回 true 表示相等，不重新渲染 */ })`。
- **问："useMemo 和 useCallback 的区别？"** 答：`useCallback(fn, deps)` 等价于 `useMemo(() => fn, deps)`，但 `useMemo` 缓存的是任意值，`useCallback` 专门缓存函数。

---

### Q4：React 中如何实现组件间通信？

**面试官意图：** 考察对组件通信机制的系统性掌握，是否了解多种方案及其适用场景。

**完美解答：**

React 组件间通信有 5 种主要方式，根据组件关系选择最合适的方案：

| 通信方式 | 适用场景 | 缺点 | 推荐度 |
|---------|---------|------|--------|
| **Props 父传子** | 父子直接通信 | 层级深时 Prop Drilling | ⭐⭐⭐⭐⭐ |
| **回调函数 子传父** | 子组件通知父组件 | 同样存在 Prop Drilling | ⭐⭐⭐⭐⭐ |
| **Context / Provider** | 跨层级共享全局数据 | 引起消费组件全部重渲染 | ⭐⭐⭐⭐ |
| **状态管理库（Zustand/Redux）** | 复杂应用全局状态 | 增加依赖体积 | ⭐⭐⭐⭐ |
| **ref + useImperativeHandle** | 父组件调用子组件方法 | 打破单向数据流 | ⭐⭐⭐ |

```jsx
// 方案 1：Props + 回调（最基本的父子通信）
function Parent() {
  const [message, setMessage] = useState('');
  return (
    <Child
      data={message}
      onChildMsg={(msg) => setMessage(msg)}
    />
  );
}

// 方案 2：Context 跨层级共享
const ThemeContext = React.createContext('light');
function App() {
  return (
    <ThemeContext.Provider value="dark">
      <DeepChild />
    </ThemeContext.Provider>
  );
}
function DeepChild() {
  const theme = useContext(ThemeContext); // 直接获取 "dark"
  return <div>Current theme: {theme}</div>;
}

// 方案 3：useImperativeHandle 父调子
const Child = forwardRef((props, ref) => {
  useImperativeHandle(ref, () => ({ focus: () => inputRef.current.focus() }));
  return <input ref={inputRef} />;
});
// 父组件中：childRef.current.focus()
```

> 💡 **面试提示：** 当被问到组件通信时，主动引出 Props Drilling 问题及其解决方案（Context + 组合模式），能展现你的工程化思考。

**延伸追问应对：**

- **问："Props Drilling 有哪些更好的替代方案？"** 答：除了 Context，还可以使用**组件组合**（children prop 或插槽模式），将深层组件需要的 Props 直接通过 JSX 传入，避免中间组件传递无关 Props。此外 Zustand 的 store 可以在任意组件中直接引用，完全绕过 Props 传递。

---

### Q5：React 事件系统和原生 DOM 事件有什么区别？

**面试官意图：** 考察对 React 底层事件处理机制的理解，这是区分初中级开发者的关键问题。

**完美解答：**

React 实现了一套**合成事件系统（SyntheticEvent）**，与原生 DOM 事件有三个核心区别：

**1. 事件委托机制：**
- **原生事件**：直接绑定到具体 DOM 元素上。
- **React 事件**：所有事件都委托到根容器（React 17 是 `document`，React 18 是 `createRoot` 创建的 root 节点），通过事件冒泡集中处理，减少内存占用。

**2. 事件对象差异：**
- React 的合成事件 `SyntheticEvent` 是对原生事件的跨浏览器封装，提供了统一的 API。
- 合成事件对象会被**池化重用**（事件回调结束后，属性会被清空），因此在异步代码中访问事件属性需要使用 `e.persist()`（React 17 之前）或直接读取需要的属性。

**3. 执行顺序：**

```jsx
// 执行顺序：原生捕获 -> React捕获 -> React冒泡 -> 原生冒泡
componentDidMount() {
  document.addEventListener('click', () => console.log('原生事件')); // 后执行
}
handleClick = () => console.log('React事件'); // 先执行
// 原因：React 事件委托在 root 节点处理，冒泡比 document 晚
```

> ⚠️ **警告：** 在 React 事件处理函数中调用 `e.stopPropagation()` 只能阻止 React 合成事件的冒泡，不能阻止原生事件的冒泡。如果需要同时阻止，需要调用 `e.nativeEvent.stopPropagation()`。

**延伸追问应对：**

- **问："React 17 和 React 18 的事件委托有什么变化？"** 答：React 17 将事件委托从 `document` 改为 root 节点，目的是为微前端和多版本 React 共存做准备。React 18 的 `createRoot` 延续了这一设计，并且支持 `flushSync` 等新特性。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节

### Q6：你在 TodoList 项目中如何实现 localStorage 持久化？考虑过哪些边界情况？

**面试官意图：** 考察对数据持久化、异常处理和自定义 Hooks 封装能力的实际理解。

**完美解答：**

在 TodoList 项目中，我使用自定义 Hook `useLocalStorage` 封装读写逻辑，核心考虑如下：

**实现方案：**

```jsx
function useLocalStorage(key, initialValue) {
  // 懒初始化：从 localStorage 读取，失败则使用默认值
  const [value, setValue] = useState(() => {
    try {
      const item = localStorage.getItem(key);
      // 处理 null 和 undefined
      return item !== null ? JSON.parse(item) : 
        typeof initialValue === 'function' ? initialValue() : initialValue;
    } catch (error) {
      // localStorage 可能被用户禁用或存储空间已满
      console.warn(`Error reading localStorage key "${key}":`, error);
      return typeof initialValue === 'function' ? initialValue() : initialValue;
    }
  });

  // 每次值变化时同步到 localStorage
  useEffect(() => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      // 存储空间已满等异常
      console.warn(`Error setting localStorage key "${key}":`, error);
    }
  }, [key, value]);

  // 提供清除方法
  const remove = useCallback(() => {
    try {
      localStorage.removeItem(key);
      setValue(typeof initialValue === 'function' ? initialValue() : initialValue);
    } catch (error) {
      console.warn(`Error removing localStorage key "${key}":`, error);
    }
  }, [key, initialValue]);

  return [value, setValue, remove];
}
```

**边界情况处理：**
1. **SSR 环境：** `localStorage` 不存在，使用 `typeof window !== 'undefined'` 判断。
2. **存储配额超限：** `localStorage` 通常限制 5MB，使用 `try/catch` 捕获 `QuotaExceededError`。
3. **数据格式异常：** 用户可能手动修改了存储值导致 `JSON.parse` 失败，使用 `try/catch` 兜底返回默认值。
4. **同 key 多 Tab 同步：** 使用 `window.addEventListener('storage', handleStorageChange)` 监听其他 Tab 的修改。

> 💡 **告诉面试官：** "我封装了 `useLocalStorage` 自定义 Hook，将持久化逻辑与 UI 层完全解耦，不仅在 TodoList 中使用，在暗黑模式主题持久化中也复用了同一 Hook。"

**延伸追问应对：**

- **问："localStorage 和 IndexedDB 的区别？"** 答：localStorage 同步、存储量小（5MB）、只能存字符串；IndexedDB 异步、可存大量结构化数据（>=250MB）、支持索引查询和事务。对于 TodoList 这种轻量数据，localStorage 足够；对于大型离线应用，IndexedDB 更合适。

---

### Q7：在你的后台管理系统中，路由守卫（Route Guard）是如何实现的？如何处理未登录重定向？

**面试官意图：** 考察对 React Router 的掌握程度以及权限控制的设计能力。

**完美解答：**

在后台管理系统中，我设计了三层路由守卫体系：**认证守卫**、**角色守卫**和**权限守卫**。

**核心实现：**

```jsx
// 1. 封装 AuthGuard 组件：认证守卫
function AuthGuard({ children, requiredRoles, requiredPermission }) {
  const { user, token } = useAuthStore();
  const location = useLocation();

  // 未登录 -> 重定向到登录页，携带回跳路径
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 角色校验（RBAC）
  if (requiredRoles && !requiredRoles.some(r => user.roles.includes(r))) {
    return <Navigate to="/403" replace />;
  }

  // 按钮级权限校验
  if (requiredPermission && !user.permissions.includes(requiredPermission)) {
    return <Navigate to="/403" replace />;
  }

  return children;
}

// 2. 路由配置（动态路由方案）
function AppRouter() {
  const { token, user } = useAuthStore();

  // 根据用户角色动态生成可访问的路由
  const accessibleRoutes = useMemo(() => {
    if (!token) return publicRoutes;
    return filterRoutesByPermission(allRoutes, user.permissions);
  }, [token, user.permissions]);

  return (
    <Routes>
      {/* 公开路由 */}
      <Route path="/login" element={<LoginPage />} />

      {/* 受保护路由 */}
      <Route element={<AuthGuard requiredRoles={['admin']} />}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/users" element={<UserManagement />} />
      </Route>

      {/* 动态路由 */}
      {accessibleRoutes.map(route => (
        <Route key={route.path} path={route.path} element={
          <AuthGuard requiredPermission={route.permission}>
            <route.component />
          </AuthGuard>
        } />
      ))}

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

// 3. Axios 拦截器：Token 过期自动跳转
request.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // Token 过期，清除登录态并跳转
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

**关键设计要点：**
- 使用 `Navigate` 组件 + `state.from` 实现登录后回跳。
- RBAC 权限模型分为：用户 -> 角色 -> 权限，三层映射关系。
- 按钮级权限使用自定义 Hook 或 HOC 实现，如 `const canEdit = usePermission('user:edit')`。
- 动态路由根据后端返回的权限树过滤生成，避免前端硬编码。

**延伸追问应对：**

- **问："菜单权限和路由权限如何联动？"** 答：后端返回的权限树同时包含菜单配置和路由配置，前端根据权限列表过滤菜单项和路由表，实现"无权限的菜单不显示，无权限的路由不可访问"的双重保障。

---

### Q8：你在 AI 对话项目中如何实现流式输出（SSE）？如何处理断连和重连？

**面试官意图：** 考察对 SSE/WebSocket 实时通信的理解，以及生产级异常处理能力。

**完美解答：**

在 AI 对话聊天 Web 端项目中，我使用 **EventSource API（SSE）** 实现流式输出，配合自动重连和状态管理。

**核心实现：**

```jsx
// 自定义 Hook：useSSE
function useSSE(url, options = {}) {
  const [data, setData] = useState('');
  const [status, setStatus] = useState('idle'); // idle | connecting | connected | error
  const eventSourceRef = useRef(null);
  const reconnectTimerRef = useRef(null);
  const { onMessage, onError, maxRetries = 3, retryDelay = 3000 } = options;
  const retryCountRef = useRef(0);

  const connect = useCallback(() => {
    // 关闭已有连接
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    setStatus('connecting');
    const es = new EventSource(url);
    eventSourceRef.current = es;

    // 超时检测
    const timeoutTimer = setTimeout(() => {
      if (es.readyState !== EventSource.OPEN) {
        es.close();
        handleReconnect();
      }
    }, 10000);

    es.onopen = () => {
      clearTimeout(timeoutTimer);
      setStatus('connected');
      retryCountRef.current = 0; // 连接成功后重置重试计数
    };

    // 接收流式数据
    es.onmessage = (event) => {
      try {
        // 服务端返回的 SSE 数据格式：data: {"content": "你好", "done": false}
        const parsed = JSON.parse(event.data);

        if (parsed.done) {
          // 流式输出结束
          setStatus('idle');
          es.close();
        } else {
          // 累积内容，实现打字动画效果
          setData(prev => prev + parsed.content);
        }

        onMessage?.(parsed);
      } catch (e) {
        // 非 JSON 格式（如心跳包）
        setData(prev => prev + event.data);
      }
    };

    es.onerror = (err) => {
      clearTimeout(timeoutTimer);
      setStatus('error');
      onError?.(err);
      es.close();
      // 非正常关闭时尝试重连
      handleReconnect();
    };
  }, [url]);

  // 断开重连逻辑（指数退避）
  const handleReconnect = useCallback(() => {
    if (retryCountRef.current >= maxRetries) {
      setStatus('error');
      return;
    }
    const delay = retryDelay * Math.pow(2, retryCountRef.current); // 指数退避
    retryCountRef.current += 1;
    reconnectTimerRef.current = setTimeout(() => {
      connect();
    }, delay);
  }, [connect, maxRetries, retryDelay]);

  // 清理
  useEffect(() => {
    return () => {
      eventSourceRef.current?.close();
      clearTimeout(reconnectTimerRef.current);
    };
  }, []);

  // 发送消息（通过 POST 触发 SSE 会话）
  const sendMessage = useCallback(async (message, conversationId) => {
    setData(''); // 清空上一次内容
    setStatus('connecting');
    // 先 POST 请求创建会话，服务端返回 SSE 端点 URL
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, conversationId })
    });
    const { sseUrl } = await response.json();
    connect(sseUrl);
  }, [connect]);

  return { data, status, sendMessage, close: () => eventSourceRef.current?.close() };
}
```

**打字动画与 Markdown 渲染：**

```jsx
function ChatMessage({ content }) {
  return (
    <div className="chat-bubble">
      <ReactMarkdown
        rehypePlugins={[rehypeHighlight]}
        remarkPlugins={[remarkGfm]}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
```

**自动滚动实现：**

```jsx
function ChatWindow({ messages }) {
  const bottomRef = useRef(null);
  const containerRef = useRef(null);
  const [isUserScrolling, setIsUserScrolling] = useState(false);

  // 自动滚动到底部（用户未手动滚动时）
  useEffect(() => {
    if (!isUserScrolling) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isUserScrolling]);

  // 检测用户是否手动滚动
  const handleScroll = useCallback(() => {
    const container = containerRef.current;
    if (!container) return;
    const isAtBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 100;
    setIsUserScrolling(!isAtBottom);
  }, []);

  return (
    <div ref={containerRef} onScroll={handleScroll} className="chat-container">
      {messages.map(msg => <ChatMessage key={msg.id} {...msg} />)}
      <div ref={bottomRef} />
    </div>
  );
}
```

**延伸追问应对：**

- **问："SSE 和 WebSocket 怎么选型？"** 答：SSE 基于 HTTP、自动重连、单向（服务端推送）、适合 AI 流式对话。WebSocket 全双工、低延迟、适合实时协作（如在线考试系统）。如果需要双向通信（如用户打断 AI 生成），可以用 WebSocket 携带 `abort` 信号。
- **问："如何在用户点击停止时中断 SSE？"** 答：调用 `eventSource.close()` 关闭连接，同时发送 POST 请求到 `/api/chat/abort` 通知服务端停止生成。

---

### Q9：在购物车项目中，你是如何处理全选/反选和总价计算的复杂状态？

**面试官意图：** 考察复杂数据状态管理能力，以及对状态提升和不可变性的理解。

**完美解答：**

购物车的核心难点在于**多个商品状态之间的联动**（选择状态变动 -> 影响总价 -> 影响全选状态）。我采用**状态提升 + useReducer** 的方案来管理：

```jsx
// 1. 定义 State 和 Action（reducer 集中管理）
const cartReducer = (state, action) => {
  switch (action.type) {
    case 'TOGGLE_ITEM':
      return {
        ...state,
        items: state.items.map(item =>
          item.id === action.payload
            ? { ...item, checked: !item.checked }
            : item
        )
      };

    case 'TOGGLE_ALL':
      // 全选/反选：检查是否全部已选中
      const allChecked = state.items.every(item => item.checked);
      return {
        ...state,
        items: state.items.map(item => ({ ...item, checked: !allChecked }))
      };

    case 'UPDATE_QUANTITY':
      return {
        ...state,
        items: state.items.map(item =>
          item.id === action.payload.id
            ? { ...item, quantity: Math.max(1, Math.min(action.payload.quantity, 99)) }
            : item
        )
      };

    case 'REMOVE_ITEM':
      return {
        ...state,
        items: state.items.filter(item => item.id !== action.payload)
      };

    default:
      return state;
  }
};

// 2. 使用 useMemo 计算派生状态
function CartPage() {
  const [state, dispatch] = useReducer(cartReducer, { items: [] });

  // 派生状态：全选状态
  const isAllChecked = useMemo(
    () => state.items.length > 0 && state.items.every(item => item.checked),
    [state.items]
  );

  // 派生状态：选中商品总价
  const totalPrice = useMemo(
    () => state.items
      .filter(item => item.checked)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
      .toFixed(2),
    [state.items]
  );

  // 派生状态：选中商品数量
  const selectedCount = useMemo(
    () => state.items.filter(item => item.checked).length,
    [state.items]
  );

  // 3. 数量限制
  const handleQuantityChange = (id, delta) => {
    const item = state.items.find(i => i.id === id);
    if (!item) return;
    const newQty = Math.max(1, Math.min(item.quantity + delta, 99));
    dispatch({ type: 'UPDATE_QUANTITY', payload: { id, quantity: newQty } });
  };
}
```

> 💡 **设计要点：** 总价、全选状态、选中数量都是通过 `useMemo` 从 `items` 派生计算出来的，而不是额外存储在 state 中。这遵循了"最小状态原则"——**任何能从现有状态计算出来的值，都不应该单独存储**，避免了状态不一致的问题。

**父子组件通信：**

```jsx
// 商品列表项组件
const CartItem = React.memo(({ item, onToggle, onQuantityChange, onRemove }) => {
  return (
    <div className="cart-item">
      <Checkbox checked={item.checked} onChange={() => onToggle(item.id)} />
      <span>{item.name}</span>
      <div className="quantity-control">
        <button onClick={() => onQuantityChange(item.id, -1)}>-</button>
        <span>{item.quantity}</span>
        <button onClick={() => onQuantityChange(item.id, 1)}>+</button>
      </div>
      <span>¥{(item.price * item.quantity).toFixed(2)}</span>
      <button onClick={() => onRemove(item.id)}>删除</button>
    </div>
  );
});
```

**延伸追问应对：**

- **问："为什么不用 Context 而用 useReducer？"** 答：购物车状态只在本页面使用，不需要跨页面共享。useReducer 的 reducer 模式使得状态变更逻辑集中可预测，便于测试和 debug。如果购物车需要在多个页面共享（如底部购物车浮窗），可以配合 Context 或 Zustand 使用。

---

### Q10：你在极简后台管理系统中如何进行 Axios 请求封装？如何处理 Token 刷新和并发请求？

**面试官意图：** 考察请求封装工程化能力，以及处理 Token 过期等生产级问题的经验。

**完美解答：**

我设计了多层 Axios 封装，包括：基础实例配置、请求拦截器、响应拦截器、以及 Token 透传和自动刷新机制。

```jsx
// request.js - 完整的 Axios 封装
import axios from 'axios';
import { useAuthStore } from '@/stores/auth';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
});

// ---- 请求拦截器：自动携带 Token ----
request.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ---- 响应拦截器：统一错误处理 + Token 自动刷新 ----
// 存储正在刷新 Token 的 Promise，避免并发重复刷新
let refreshPromise = null;

request.interceptors.response.use(
  (response) => {
    // 直接返回 data，上层调用无需再解构
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config;

    // 401 未授权：Token 过期，尝试刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 如果是登录接口本身的 401，直接抛出
      if (originalRequest.url === '/auth/refresh') {
        useAuthStore.getState().logout();
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      // 防止并发刷新 Token：复用同一个 Promise
      if (!refreshPromise) {
        refreshPromise = request.post('/auth/refresh', {
          refreshToken: useAuthStore.getState().refreshToken
        })
        .then(({ accessToken, refreshToken }) => {
          // 更新 Store 中的 Token
          useAuthStore.getState().setTokens(accessToken, refreshToken);
          return accessToken;
        })
        .catch(() => {
          // 刷新失败，强制登出
          useAuthStore.getState().logout();
          window.location.href = '/login';
          return Promise.reject(error);
        })
        .finally(() => {
          refreshPromise = null;
        });
      }

      // 等待 Token 刷新完成，然后重试原始请求
      const newToken = await refreshPromise;
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return request(originalRequest);
    }

    // 统一错误提示
    const message = error.response?.data?.message || error.message || '网络错误';
    // 非 401 错误可以在这里统一弹 Toast
    // message.error(message);

    return Promise.reject(error);
  }
);

// ---- API 模块化导出 ----
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  logout: () => request.post('/auth/logout'),
  refresh: (refreshToken) => request.post('/auth/refresh', { refreshToken })
};

export const userApi = {
  list: (params) => request.get('/users', { params }),
  create: (data) => request.post('/users', data),
  update: (id, data) => request.put(`/users/${id}`, data),
  delete: (id) => request.delete(`/users/${id}`)
};

export default request;
```

> ⚠️ **并发请求 Token 刷新的关键设计：** 如果不做 `refreshPromise` 的共享，多个并发的 401 请求会同时触发多次 Token 刷新请求，导致后端的 refreshToken 被重复消费而失效。使用一个全局 Promise 变量，让所有等待刷新的请求共享同一个刷新结果，是生产环境的标准做法。

**延伸追问应对：**

- **问："如何实现请求取消（如页面切换时取消未完成的请求）？"** 答：使用 `AbortController` + `useEffect` 清理：`const controller = new AbortController(); axios.get('/api/data', { signal: controller.signal })`，在组件卸载时调用 `controller.abort()`。
- **问："大文件上传如何处理？"** 答：使用 `axios.post` 的 `onUploadProgress` 回调实现进度条，大文件使用分片上传 + 断点续传方案。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节

### Q11：如果让你设计一个 RBAC 权限管理系统，前端层面如何实现完整的权限控制方案？

**面试官意图：** 考察系统设计能力，是否对权限管理的前端落地有完整认知。

**完美解答：**

RBAC（Role-Based Access Control）在前端需要实现**路由权限**、**菜单权限**、**按钮权限**三层控制。

**整体架构设计：**

```
用户登录
  ├── 后端返回：用户信息 + 角色列表 + 权限树
  └── 前端处理：
        ├── 1. 路由层面：根据权限动态生成可访问路由表
        ├── 2. 菜单层面：根据权限过滤侧边栏/导航菜单
        ├── 3. 按钮层面：提供权限指令/Hook 控制按钮显隐
        └── 4. API 层面：拦截器处理 403 响应
```

**具体实现：**

```jsx
// ---- 1. 权限数据结构定义（TypeScript 类型约束） ----
interface Permission {
  id: string;
  code: string;          // 权限编码，如 "user:create"
  name: string;          // 权限名称
  type: 'menu' | 'button' | 'api';
  parentId: string | null;
  children?: Permission[];
}

interface Role {
  id: string;
  name: string;
  code: string;          // 角色编码，如 "admin"
  permissionCodes: string[]; // 权限编码列表
}

// ---- 2. 自定义 Hook：usePermission ----
function usePermission() {
  const { user } = useAuthStore();

  // 检查是否拥有指定权限
  const hasPermission = useCallback((permissionCode: string) => {
    return user.permissions.includes(permissionCode);
  }, [user.permissions]);

  // 检查是否拥有任意一个权限（OR）
  const hasAnyPermission = useCallback((codes: string[]) => {
    return codes.some(code => user.permissions.includes(code));
  }, [user.permissions]);

  // 检查是否拥有全部权限（AND）
  const hasAllPermissions = useCallback((codes: string[]) => {
    return codes.every(code => user.permissions.includes(code));
  }, [user.permissions]);

  return { hasPermission, hasAnyPermission, hasAllPermissions };
}

// ---- 3. 按钮级权限组件封装 ----
// 方式一：组件方式
function PermissionBoundary({ code, fallback = null, children }) {
  const { hasPermission } = usePermission();
  return hasPermission(code) ? children : fallback;
}
// 使用：<PermissionBoundary code="user:delete"><DeleteButton /></PermissionBoundary>

// 方式二：自定义 Hook 条件渲染
function UserTable() {
  const { hasPermission } = usePermission();
  const columns = [
    { title: '用户名', dataIndex: 'name' },
    { title: '操作', render: (_, record) => (
      <>
        {hasPermission('user:edit') && <Button onClick={() => edit(record)}>编辑</Button>}
        {hasPermission('user:delete') && <Button danger onClick={() => del(record)}>删除</Button>}
      </>
    )}
  ];
  return <Table columns={columns} dataSource={data} />;
}

// ---- 4. 动态路由过滤（根据权限树） ----
function filterRoutesByPermission(routes, permissions) {
  return routes.filter(route => {
    // 如果路由没有配置权限，视为公开路由
    if (!route.permission) return true;
    // 检查当前用户是否有权访问
    const hasPermission = permissions.includes(route.permission);
    if (hasPermission && route.children) {
      // 递归过滤子路由
      route.children = filterRoutesByPermission(route.children, permissions);
    }
    return hasPermission;
  });
}
```

| 权限层级 | 实现方式 | 防护目标 |
|---------|---------|---------|
| 路由层 | 动态路由表 + AuthGuard | 未授权路径不可访问 |
| 菜单层 | 权限树过滤菜单配置 | 无权限菜单项不显示 |
| 按钮层 | PermissionBoundary / usePermission | 无权限按钮不可见 |
| API 层 | Axios 拦截器处理 403 | 即使绕过前端也无法调用接口 |

> 🎯 **面试加分点：** 主动提出——"真正的权限安全在后端，前端权限控制是为了更好的用户体验（隐藏不可操作的元素），不能单纯依赖前端做安全防护。"

**延伸追问应对：**

- **问："如果角色很多（几十个），权限树很大，如何优化性能？"** 答：将权限列表扁平化为 `Set<string>` 存储在 Store 中，查找时间复杂度 O(1)。动态路由过滤只在登录/角色切换时执行一次，使用 `useMemo` 缓存结果。
- **问："从权限管理后台如何动态更新前端权限？"** 答：用户重新登录或调用刷新权限接口，后端返回最新权限列表，前端更新 Store 并重新生成动态路由。

---

### Q12：React 项目中如何进行性能优化？从加载性能到运行时性能讲一下体系化方案。

**面试官意图：** 考察性能优化的系统化知识，是否掌握从网络到渲染的全链路优化。

**完美解答：**

React 性能优化分为**加载性能**和**运行时性能**两个维度，我从以下几个层面系统化处理：

```jsx
// ======= 一、加载性能优化 =======

// 1. 路由懒加载（Code Splitting）
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const UserManagement = lazy(() => import('@/pages/UserManagement'));

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/users" element={<UserManagement />} />
      </Routes>
    </Suspense>
  );
}

// 2. 组件懒加载（条件渲染时加载）
function AIChat() {
  const [showEditor, setShowEditor] = useState(false);
  return (
    <>
      <button onClick={() => setShowEditor(true)}>打开编辑器</button>
      {showEditor && <LazyMarkdownEditor />}
    </>
  );
}
const LazyMarkdownEditor = lazy(() => import('./MarkdownEditor'));

// 3. 图片懒加载
function LazyImage({ src, alt }) {
  const imgRef = useRef(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin: '200px' } // 提前加载 200px
    );
    observer.observe(imgRef.current);
    return () => observer.disconnect();
  }, []);

  return <div ref={imgRef}>{visible && <img src={src} alt={alt} />}</div>;
}

// ======= 二、运行时性能优化 =======

// 1. 虚拟列表（大量数据时）
function VirtualList({ items, itemHeight, containerHeight }) {
  const containerRef = useRef(null);
  const [scrollTop, setScrollTop] = useState(0);

  const visibleCount = Math.ceil(containerHeight / itemHeight);
  const startIndex = Math.floor(scrollTop / itemHeight);
  const endIndex = startIndex + visibleCount + 2; // 额外渲染 2 个缓冲

  const visibleItems = items.slice(startIndex, endIndex);

  return (
    <div
      ref={containerRef}
      style={{ height: containerHeight, overflow: 'auto' }}
      onScroll={(e) => setScrollTop(e.target.scrollTop)}
    >
      <div style={{ height: items.length * itemHeight, position: 'relative' }}>
        {visibleItems.map((item, index) => (
          <div
            key={item.id}
            style={{
              position: 'absolute',
              top: (startIndex + index) * itemHeight,
              height: itemHeight
            }}
          >
            {item.content}
          </div>
        ))}
      </div>
    </div>
  );
}

// 2. 防抖与节流
function useDebounce(value, delay = 300) {
  const [debouncedValue, setDebouncedValue] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debouncedValue;
}

// 搜索输入防抖
function SearchInput() {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 300);

  useEffect(() => {
    if (debouncedQuery) {
      searchApi(debouncedQuery);
    }
  }, [debouncedQuery]);

  return <input value={query} onChange={(e) => setQuery(e.target.value)} />;
}
```

**性能优化清单总结：**

| 维度 | 优化手段 | 效果 |
|------|---------|------|
| 加载 | 路由懒加载（React.lazy + Suspense） | 首屏 JS 减少 30%-70% |
| 加载 | 组件懒加载（条件渲染按需加载） | 减少非首屏代码体积 |
| 加载 | 图片懒加载（IntersectionObserver） | 减少首屏网络请求 |
| 加载 | 静态资源 CDN + 预加载 `<link rel="preload">` | 关键资源提前加载 |
| 运行时 | React.memo + useMemo + useCallback | 减少不必要的重新渲染 |
| 运行时 | 虚拟列表（react-window / 自实现） | 万级列表卡顿优化 |
| 运行时 | 防抖/节流（搜索、滚动、resize） | 减少高频触发计算 |
| 运行时 | 状态管理精细化（拆分 store、selector 精确订阅） | 减少全局状态引起的重渲染 |
| 构建 | Tree Shaking + 按需引入 Ant Design | 打包体积减少 50%+ |
| 构建 | 压缩 + Gzip | 传输体积减少 70%+ |

**延伸追问应对：**

- **问："React.memo 和 useMemo 是否带来了 CPU 开销？"** 答：是的。浅比较本身有开销，对于 Props 简单且渲染轻量的组件，不包裹 `React.memo` 反而更快。建议只在渲染成本高（大型列表、复杂图表）或 Props 结构稳定的组件上使用。

---

### Q13：React 项目中 Zustand 和 Redux 如何选型？Zustand 相对于 Redux 的核心优势是什么？

**面试官意图：** 考察对状态管理库的深度理解，是否具备技术选型判断力。

**完美解答：**

**Zustand 和 Redux 的核心对比：**

| 维度 | Zustand | Redux Toolkit |
|------|---------|--------------|
| 模板代码 | 极少（零样板代码） | 中等（createSlice + configureStore） |
| 学习曲线 | 低（类似 useState） | 中等（需理解 reducer、action、dispatch） |
| TypeScript 支持 | 天然友好（无额外类型定义） | 友好（createSlice 自动生成类型） |
| Bundle 大小 | ~1KB（极小） | ~12KB（含 Redux Toolkit） |
| 中间件 | 内置（persist、devtools、immer、subscribe） | 独立（redux-saga、redux-thunk） |
| 非 React 使用 | 支持（Store 定义独立于 React） | 支持 |
| 并发模式兼容 | 完美兼容 | 兼容 |
| 社区生态 | 较小但快速成长 | 极其成熟（DevTools、中间件生态） |

**Zustand 的核心优势：**

```jsx
// ---- Zustand：极简定义 ----
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// 直接在 store 中定义状态和操作（不需要 action type 常量）
const useStore = create(
  persist(
    (set, get) => ({
      // 状态
      user: null,
      token: null,
      theme: 'light',

      // 操作（直接修改状态，类 useState 风格）
      login: (userData, token) => set({ user: userData, token }),
      logout: () => set({ user: null, token: null }),
      toggleTheme: () => set(state => ({
        theme: state.theme === 'light' ? 'dark' : 'light'
      })),

      // 派生状态：通过 get 获取其他状态
      isLoggedIn: () => !!get().token,

      // 异步操作
      fetchUser: async (id) => {
        const userData = await api.getUser(id);
        set({ user: userData });
      }
    }),
    {
      name: 'app-storage',
      partialize: (state) => ({ token: state.token, theme: state.theme }) // 只持久化部分字段
    }
  )
);

// 组件中使用（精确订阅，避免不必要渲染）
function UserInfo() {
  // 只订阅 user，user 不变时组件不重新渲染
  const user = useStore(state => state.user);
  return <div>{user?.name}</div>;
}

// 在组件外使用（不需要 Provider）
const token = useStore.getState().token;
useStore.getState().logout();

// ---- Redux Toolkit 对比 ----
// Redux 需要：定义 slice -> 导出 actions/reducer -> 配置 store -> Provider 包裹
const counterSlice = createSlice({
  name: 'counter',
  initialState: { value: 0 },
  reducers: {
    increment: (state) => { state.value += 1; }
  }
});
// 需要 Provider 包裹整个应用
```

> 🎯 **面试建议：** 对于新项目或中小型项目，我推荐 Zustand，因为它零样板代码、TypeScript 开箱即用、且包体积极小。对于大型团队和大规模项目（如电商平台），Redux Toolkit 的规范约束和成熟生态更适合团队协作。

**延伸追问应对：**

- **问："Zustand 的 subscribeWithSelector 中间件有什么用？"** 答：它可以精确监听某个状态字段的变化，适用于需要监听状态变化并触发副作用的场景（如自动保存），比 React 的 useEffect 更灵活。
- **问："Zustand 如何实现 computed 属性？"** 答：可以直接在 Store 中定义 getter 函数，或者在组件中使用 `useMemo`。也可以使用第三方库 `zundo` 实现时间旅行调试。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q14：线上页面白屏了，如何排查和定位问题？

**面试官意图：** 考察前端故障排查的系统性思路和工具使用经验。

**完美解答：**

当遇到线上白屏问题时，我按照以下排查流程逐步定位：

```
白屏排查路线图：
1. 网络层面 -> 2. JS 执行 -> 3. 渲染层面 -> 4. 兼容性 -> 5. 路由
```

**第一步：网络层面排查**

```javascript
// 打开浏览器 DevTools -> Network 面板
// 1. 确认 HTML 是否正常返回（状态码 200）
// 2. 确认 JS/CSS 资源是否加载成功
//    - 404：检查构建产物是否上传完整，CDN 缓存是否过期
//    - 网络错误：检查 CDN 域名解析、CORS 配置、资源路径
// 3. 确认是否有资源被 AdBlock / 浏览器扩展拦截
```

**第二步：JS 执行错误排查（最常见原因）**

```
// 打开 DevTools -> Console 面板
// 常见错误：
// 1. "Uncaught TypeError: Cannot read properties of undefined"
//    -> 后端返回的数据结构不符合预期，某个字段为 undefined
//    -> 解决方案：加可选链 ?. 或默认值 || 兜底
//
// 2. "Uncaught SyntaxError: Unexpected token '<'"
//    -> 前端请求的 JS 文件被服务器返回了 HTML（如 index.html）
//    -> 解决方案：检查 webpack/vite 的 publicPath 配置，确保资源路径正确
//
// 3. "ChunkLoadError: Loading chunk xxx failed"
//    -> 路由懒加载的 chunk 加载失败
//    -> 解决方案：配置 retry 机制，或优化分包策略
```

**第三步：渲染层面排查**

```jsx
// 1. React 渲染错误（无 Error Boundary 捕获时白屏）
// 解决方案：全局 Error Boundary 兜底
import { ErrorBoundary } from 'react-error-boundary';

function ErrorFallback({ error, resetErrorBoundary }) {
  return (
    <div role="alert">
      <h2>页面出错了</h2>
      <pre>{error.message}</pre>
      <button onClick={resetErrorBoundary}>重试</button>
    </div>
  );
}

// 全局包裹
<ErrorBoundary FallbackComponent={ErrorFallback}>
  <App />
</ErrorBoundary>
```

**第四步：兼容性排查**

- 检查浏览器控制台是否有 API 不支持的报错（如 `Array.flat` 在旧浏览器不支持）
- 检查 CSS 兼容性（CSS Grid、CSS 变量等）
- 查看 Browserslist 配置是否覆盖了目标用户

**第五步：路由层面**

- 检查是否是非正常路由路径导致未匹配到任何路由
- SPA 应用需确认 Nginx 等服务器做了 `try_files` 配置，否则路由刷新会 404

> 🎯 **面试加分点：** 在项目中，我构建了"前端监控体系"（Sentry / 自建埋点），监控 JS Error、资源加载失败、API 异常，一旦出现白屏类问题能第一时间告警。同时配置了 Error Boundary 兜底展示"页面异常"提示，而不是直接白屏。

**延伸追问应对：**

- **问："如果是移动端 H5 白屏，排查有什么不同？"** 答：移动端需要额外关注：vConsole / Eruda 远程调试、WebView 版本兼容性（尤其是 Android 低版本 WebView 不支持某些 ES6 语法）、弱网环境超时、微信浏览器缓存等问题。

---

### Q15：如果用户反馈"页面数据没更新，刷新后才正常"，可能是什么原因？如何解决？

**面试官意图：** 考察对 React 状态更新机制和缓存策略的理解。

**完美解答：**

这个现象本质是**视图与数据不同步**。通过"刷新后才正常"可以判断后端数据本身是正确的，问题在前端。常见原因及解决方案如下：

**原因 1：React 状态更新未触发重渲染（闭包陷阱）**

```jsx
// 错误示例：setTimeout 中的闭包捕获了旧值
function Timer() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCount(count + 1); // 闭包中 count 始终是 0
    }, 1000);
    return () => clearInterval(timer);
  }, []); // 依赖数组为空，count 从未更新

  return <div>{count}</div>;
}

// 正确方式：使用函数式更新
const timer = setInterval(() => {
  setCount(prev => prev + 1); // 每次都从最新值计算
}, 1000);
```

**原因 2：useEffect 依赖缺失**

```jsx
function UserProfile({ userId }) {
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetchUser(userId).then(setUser);
    // 漏掉了 userId 依赖！当从用户 A 切换到用户 B 时，
    // 仍然显示用户 A 的数据
  }, []);

  // 正确：依赖数组中包含 userId
  useEffect(() => {
    fetchUser(userId).then(setUser);
  }, [userId]);
}
```

**原因 3：浏览器缓存（Service Worker / HTTP 缓存）**

- Service Worker 缓存了旧的 API 响应
- HTTP 强缓存（Cache-Control: max-age）导致浏览器未请求最新数据
- **解决方案：** API 请求添加时间戳或版本号，或配置合适的缓存策略

```jsx
// 添加时间戳防止 GET 请求缓存
const fetchData = () => {
  axios.get('/api/data', {
    params: { _t: Date.now() }
  });
};
```

**原因 4：组件未正确卸载和重新挂载**

```jsx
// 使用 key 强制重新挂载组件
function App() {
  const [page, setPage] = useState('home');

  return (
    <div>
      <button onClick={() => setPage('home')}>首页</button>
      <button onClick={() => setPage('settings')}>设置</button>
      {/* key 变化时强制重新挂载，重新走初始化流程 */}
      {page === 'home' ? <HomePage key="home" /> : <SettingsPage key="settings" />}
    </div>
  );
}
```

**原因 5：React Query / SWR 等数据请求库的缓存策略**

- 配置了 `staleTime` 但未正确处理缓存失效
- 修改操作后未触发对应查询的失效（invalidation）
- **解决方案：** 在 mutate 操作后手动调用 `queryClient.invalidateQueries()`

> 💡 **排查工具：** 使用 React DevTools 的 Profiler 录制操作过程，直观看到哪些组件重新渲染了、哪些没有，20 秒内定位"数据变了但视图没更新"的原因。

**延伸追问应对：**

- **问："React Query 的 staleTime 和 cacheTime 有什么区别？"** 答：`staleTime` 是数据从"新鲜"变为"过期"的时间（过期后会在后台重新获取），`cacheTime` 是数据从内存中移除的时间。合理设置 `staleTime`（如 30 秒）可以减少不必要的请求，同时保证数据及时更新。

---

### Q16：你的 AI 对话项目中，如何管理多轮对话的上下文？历史消息量太大怎么办？

**面试官意图：** 考察对 AI 对话系统的工程化理解，以及对 LLM 上下文窗口限制的处理策略。

**完美解答：**

多轮对话的上下文管理需要同时处理好**前端展示**和**API 请求**两个层面的问题。

**前端架构设计：**

```jsx
// ---- 数据结构设计 ----
interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
  // 用于引用溯源（RAG 场景）
  sources?: Source[];
  // 用于流式输出标识
  isStreaming?: boolean;
}

interface Conversation {
  id: string;
  title: string;
  messages: Message[];
  createdAt: number;
  updatedAt: number;
  model?: string; // 当前对话使用的模型
}

// ---- 使用 Zustand 管理对话 ----
const useChatStore = create((set, get) => ({
  conversations: [],
  currentConversationId: null,

  // 当前对话
  currentConversation: () => {
    const { conversations, currentConversationId } = get();
    return conversations.find(c => c.id === currentConversationId);
  },

  // 添加消息
  addMessage: (message) => set(state => ({
    conversations: state.conversations.map(c =>
      c.id === state.currentConversationId
        ? { ...c, messages: [...c.messages, message], updatedAt: Date.now() }
        : c
    )
  })),

  // 更新流式输出的最后一条消息（追加内容）
  updateLastMessage: (content) => set(state => ({
    conversations: state.conversations.map(c => {
      if (c.id !== state.currentConversationId) return c;
      const messages = [...c.messages];
      const lastMsg = { ...messages[messages.length - 1] };
      lastMsg.content += content;
      messages[messages.length - 1] = lastMsg;
      return { ...c, messages };
    })
  })),

  // 创建新对话
  createConversation: () => {
    const id = crypto.randomUUID();
    set(state => ({
      conversations: [{
        id,
        title: '新对话',
        messages: [],
        createdAt: Date.now(),
        updatedAt: Date.now()
      }, ...state.conversations],
      currentConversationId: id
    }));
  }
}));
```

**上下文窗口管理策略（关键）：**

```jsx
// ---- 向后端发送消息时，裁剪上下文 ----
function buildPrompt(conversation, maxTokens = 4000) {
  const { messages } = conversation;

  // 策略 1：系统消息始终保留
  const systemMessages = messages.filter(m => m.role === 'system');
  const chatMessages = messages.filter(m => m.role !== 'system');

  // 策略 2：滑动窗口——只保留最近的 N 轮对话
  const MAX_ROUNDS = 10;
  const recentMessages = chatMessages.slice(-MAX_ROUNDS * 2); // user + assistant

  // 策略 3：Token 计数裁剪（精确控制）
  let tokenCount = 0;
  const includedMessages = [];

  // 从最近的开始，反向选取，直到超过 maxTokens
  for (const msg of [...recentMessages].reverse()) {
    const msgTokens = estimateTokens(msg.content);
    if (tokenCount + msgTokens > maxTokens) break;
    tokenCount += msgTokens;
    includedMessages.unshift(msg);
  }

  return [...systemMessages, ...includedMessages];
}

// 简易 Token 估算（生产环境使用 tiktoken 等库）
function estimateTokens(text) {
  // 英文 ~4 字符/token，中文 ~1.5 字符/token
  return Math.ceil(text.length / 2);
}
```

**多级存储策略：**

| 存储层级 | 存储内容 | 容量 | 用途 |
|---------|---------|------|------|
| Zustand Store | 当前对话完整消息 | 内存 | 即时展示 |
| localStorage | 最近 50 条对话元数据 + 消息摘要 | 5MB 内 | 会话恢复 |
| IndexedDB | 全部对话完整历史 | >=250MB | 历史查询、检索 |
| 后端数据库 | 全量数据持久化 | 无限制 | 多端同步、长期存储 |

> 🎯 **面试加分点：** 当历史消息过多时，我会对旧消息做**总结摘要**（使用 LLM 对每轮对话生成摘要），代替完整消息发送给后端，在保留上下文信息的同时大幅降低 Token 消耗。

**延伸追问应对：**

- **问："RAG 知识库问答中，如何实现引用溯源（展示答案来自哪些文档片段）？"** 答：后端在 SSE 流式响应的末帧中携带 `sources` 字段，包含引用的文档 ID、片段内容和来源链接。前端在答案下方渲染引用信息，支持点击跳转到原文。
- **问："如何实现对话搜索功能？"** 答：使用 Fuse.js 在前端进行模糊搜索，匹配对话标题和消息内容。大量数据时使用后端 Elasticsearch 或向量检索。

---

### Q17：React + TypeScript 项目中，如何处理好类型约束？有什么最佳实践？

**面试官意图：** 考察 TypeScript 在 React 项目中的实际使用水平，是否掌握常见类型模式。

**完美解答：**

TypeScript 类型约束在 React 项目中不仅是"写类型"，更是"用类型约束来减少 bug"。以下是我总结的最佳实践：

**1. Props 类型定义（严禁使用 any）**

```tsx
// 推荐：interface + 清晰的类型定义
interface ButtonProps {
  variant: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';   // 可选
  disabled?: boolean;           // 可选
  loading?: boolean;
  children: React.ReactNode;    // 支持任意可渲染内容
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  style?: React.CSSProperties;  // 支持 style Prop
}

// 使用联合类型约束枚举值，而非 string
// ❌ 错误：type ButtonProps = { variant: string };
// ✅ 正确：type ButtonProps = { variant: 'primary' | 'secondary' };

function Button({ variant, size = 'md', children, ...rest }: ButtonProps) {
  return <button className={`btn btn-${variant} btn-${size}`} {...rest}>{children}</button>;
}
```

**2. Hooks 的泛型使用**

```tsx
// useState 泛型自动推断，复杂类型显式标注
const [user, setUser] = useState<User | null>(null);
const [items, setItems] = useState<Item[]>([]);

// useReducer 强类型
type Action =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_DATA'; payload: DataType }
  | { type: 'SET_ERROR'; payload: string };

const reducer = (state: State, action: Action): State => {
  switch (action.type) {
    case 'SET_LOADING': return { ...state, loading: action.payload };
    case 'SET_DATA': return { ...state, data: action.payload, loading: false };
    case 'SET_ERROR': return { ...state, error: action.payload, loading: false };
    default: return state;
  }
};

// useRef 的三种用法
const inputRef = useRef<HTMLInputElement>(null);                    // DOM 引用
const countRef = useRef<number>(0);                                 // 可变值
const callbackRef = useRef<((data: Data) => void) | null>(null);     // 回调存储
```

**3. API 响应类型规范化**

```tsx
// 统一 API 响应结构
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 分页结构
interface PaginatedData<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

// 类型安全的 API 封装
interface User {
  id: string;
  name: string;
  email: string;
  role: 'admin' | 'user';
  createdAt: string;
}

const userApi = {
  list: (params: ListParams) =>
    request.get<ApiResponse<PaginatedData<User>>>('/users', { params }),

  create: (data: CreateUserDto) =>
    request.post<ApiResponse<User>>('/users', data),

  // 通过泛型约束入参类型
  update: <T extends Partial<User>>(id: string, data: T) =>
    request.put<ApiResponse<User>>(`/users/${id}`, data),
};
```

**4. 自定义 Hook 类型导出**

```tsx
// 返回值需要显式标注类型，方便使用者查看
interface UseAsyncResult<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

function useAsync<T>(fn: () => Promise<T>, deps: any[] = []): UseAsyncResult<T> {
  // ...实现
  return { data, loading, error, refresh };
}
```

**TypeScript 类型约束 Checklist：**

| 场景 | 类型方案 | 避坑点 |
|------|---------|--------|
| Props | interface / type + `React.FC` 弃用 | 不要用 `any`，优先 `interface` |
| State | 自动推断 / 显式 `<Type>` | 联合类型优于 `null` 联合 |
| Event | `React.ChangeEvent<HTMLInputElement>` | 不要用原生 `Event` |
| Ref | `useRef<HTMLDivElement>(null)` | 泛型参数 + 初始值 `null` |
| API | 泛型 `ApiResponse<T>` | 响应类型需要在后端维护一致 |
| Context | `createContext<Type \| null>` | 必须处理 `null` 情况 |
| Reducer | discriminated union Action 类型 | Action 的 type 用字面量联合 |
| 组件 props 透传 | `ComponentPropsWithoutRef<'button'>` | 用 `forwardRef` 时用 `ComponentPropsWithRef` |

**延伸追问应对：**

- **问："type 和 interface 在 React 项目中怎么选？"** 答：官方推荐 props 用 `interface`（可合并声明），联合类型用 `type`。在 React 项目中我遵循：Props 定义用 `interface`，函数/组件类型用 `type`。两者一般混用，但团队需要统一规范。
- **问："泛型组件的场景？"** 答：比如 `Table<T>` 组件，传入不同数据类型复用同一表格逻辑：`function Table<T>({ data, columns }: TableProps<T>)`。

---

## 💎 面试加分金句

- "React 的核心思想是 **UI = f(state)** —— 视图是状态的纯函数。理解了这个公式，就能理解为什么 React 强调不可变数据和单向数据流。"
- "性能优化的第一原则是**不要过早优化**，先用 Profiler 定位真正的瓶颈，再针对性优化。90% 的性能问题都来自 10% 的代码。"
- "前端权限控制的本质是**用户体验设计**，真正的安全防线在后端。前端控制菜单和按钮的显隐，是为了让用户不被不允许的操作打扰。"
- "AI 对话前端最核心的三个体验点：**流式输出（不卡等待）**、**打字动画（有反馈感）**、**自动滚动（不丢失上下文）**。"
- "TypeScript 在 React 项目中的价值不是'写类型'，而是**让非法状态在编译期就被发现**。一个精心设计的类型系统能消除一整类运行时错误。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|---------|
| React 18 新特性 | 掌握 Automatic Batching、Transitions、Suspense、useId、useDeferredValue |
| Fiber 架构原理 | 理解虚拟 DOM -> Fiber 树 -> 双缓存 -> 调和过程，从 requestIdleCallback 切入 |
| 微前端方案 | 了解 qiankun（基于 single-spa）、Module Federation 的核心区别和实践 |
| React Server Component | 理解 RSC 是"只在服务端运行的组件"，解决客户端包体积问题 |
| 测试方案 | Jest + React Testing Library 的行为测试 vs Storybook 的可视化测试 |
| 构建工具对比 | Vite（开发服务器 esbuild 预构建 + 生产 Rollup）vs Webpack（全部打包） |
| React Native | 了解 React Native 的桥接层（Bridge -> JSI）和新架构（Fabric、TurboModules） |

## 🔗 关联知识点

- **状态管理：** Zustand / Redux Toolkit / Jotai / React Query
- **路由：** React Router v6（loader、action、defer 新特性）
- **UI 框架：** Ant Design 5.x（CSS-in-JS）、Tailwind CSS、Material-UI
- **构建工具：** Vite / Webpack / Turbopack
- **测试：** Vitest / Jest + React Testing Library / Playwright
- **类型：** TypeScript 4.9+（satisfies 操作符）
- **后端对接：** Axios / React Query / SWR
- **AI 集成：** LangChain4j 对接 / SSE / WebSocket
- **性能：** Lighthouse / React DevTools Profiler / Web Vitals
