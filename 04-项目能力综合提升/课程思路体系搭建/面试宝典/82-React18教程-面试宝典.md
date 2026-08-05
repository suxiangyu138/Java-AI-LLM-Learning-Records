# React18 面试宝典
> 基于React18核心与实战课程大纲，全面覆盖React18面试高频考点与核心原理

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1.1 React18相比React17有哪些核心变化？
| 特性 | React17 | React18 |
|------|---------|---------|
| 并发模式 | 不支持 | Concurrent Features |
| 自动批处理 | 仅合成事件中 | 所有场景（setTimeout/Promise/原生事件） |
| Suspense | 仅支持懒加载 | 支持服务端渲染和并发 |
| useTransition | 无 | 新增 |
| useDeferredValue | 无 | 新增 |
| useId | 无 | 新增 |
| 渲染API | ReactDOM.render | createRoot |

### 1.2 JSX的本质是什么？
> 🎯 JSX是 `React.createElement` 的语法糖，最终编译为虚拟DOM对象。

```jsx
// JSX写法
const element = <h1 className="title">Hello React</h1>
// 编译后
const element = React.createElement('h1', { className: 'title' }, 'Hello React')
// 返回虚拟DOM
// { type: 'h1', props: { className: 'title', children: 'Hello React' } }
```

### 1.3 useState的使用规则？
```jsx
const [count, setCount] = useState(0)
// 1. 只能在组件顶层调用，不能在循环/条件中
// 2. 更新时若新值相同，React会跳过子组件渲染
// 3. 函数式更新：setCount(prev => prev + 1)
// 4. 对象更新需展开不可变
setUser(prev => ({ ...prev, name: 'new' }))
```

### 1.4 useEffect的三种使用模式？
```jsx
// 1. 无依赖：每次渲染后执行（模拟componentDidUpdate + componentDidMount）
useEffect(() => { console.log('每次渲染后执行') })

// 2. 空数组：仅挂载时执行（模拟componentDidMount）
useEffect(() => { fetchData() }, [])

// 3. 有依赖：依赖变化时执行
useEffect(() => { console.log('count变化:', count) }, [count])
// 清除副作用
useEffect(() => {
  const timer = setInterval(() => setCount(c => c+1), 1000)
  return () => clearInterval(timer) // 组件卸载时清除
}, [])
```

### 1.5 useCallback和useMemo的区别？
| API | 返回值 | 用途 | 场景 |
|-----|--------|------|------|
| useCallback | 缓存的函数 | 避免函数每次重新创建 | 传给子组件的回调 |
| useMemo | 缓存的计算值 | 避免重复计算 | 复杂计算、派生数据 |

```jsx
// useCallback：缓存函数引用
const handleClick = useCallback(() => {
  setCount(c => c + 1)
}, []) // 空依赖，引用不变

// useMemo：缓存计算结果
const sortedList = useMemo(() => {
  return items.sort((a, b) => a.name.localeCompare(b.name))
}, [items])
```

### 1.6 React.memo的作用？
> 💡 React.memo是性能优化高阶组件，通过浅比较props决定是否重新渲染。

```jsx
const Child = React.memo(({ name }) => {
  console.log('Child渲染')
  return <div>{name}</div>
})
// 配合useCallback使用避免子组件无效渲染
function Parent() {
  const [count, setCount] = useState(0)
  const handleClick = useCallback(() => {}, [])
  return <Child onClick={handleClick} />
}
```

### 1.7 受控组件 vs 非受控组件？
| 特性 | 受控组件 | 非受控组件 |
|------|---------|-----------|
| 数据管理 | React state | DOM自身 |
| 获取值方式 | state | ref |
| 控制能力 | 完全控制 | 受限 |
| 适用场景 | 需要即时校验/联动 | 简单表单、文件上传 |

### 1.8 useContext使用场景？
> 🎯 用于跨层级传递数据，避免props层层传递（prop drilling）。

```jsx
const ThemeContext = createContext('light')
function App() {
  return (
    <ThemeContext.Provider value="dark">
      <Child />
    </ThemeContext.Provider>
  )
}
function Child() {
  const theme = useContext(ThemeContext) // 'dark'
  return <div>{theme}</div>
}
```

### 1.9 React中的事件机制？
> 💡 React事件是合成事件（SyntheticEvent），所有事件绑定在root节点上（事件委托）。

```jsx
// 合成事件与原生事件区别
// 合成事件：onClick={handleClick}，事件池复用
// 原生事件：addEventListener，无池化
// React18中移除了事件池，e.persist()不再需要
```

### 1.10 React Router v6核心API？
```jsx
<BrowserRouter>
  <Routes>
    <Route path="/" element={<Home />} />
    <Route path="/user/:id" element={<User />} />
    <Route path="/dashboard" element={<Dashboard />}>
      <Route index element={<Overview />} />
      <Route path="settings" element={<Settings />} />
    </Route>
    <Route path="*" element={<NotFound />} />
  </Routes>
</BrowserRouter>
// 导航
const navigate = useNavigate()
navigate('/user/1')
navigate(-1) // 后退
// 获取参数
const { id } = useParams()
const [searchParams] = useSearchParams()
```

## 二、深度原理剖析

### 2.1 React18自动批处理（Automatic Batching）？
> 🎯 React18中所有状态更新自动批处理，无论它们在哪里触发。

```jsx
// React17：setTimeout中不会批处理，导致两次渲染
setTimeout(() => {
  setCount(c => c + 1) // 触发一次渲染
  setFlag(f => !f)     // 触发一次渲染（共2次）
}, 100)

// React18：所有场景都批处理
setTimeout(() => {
  setCount(c => c + 1)
  setFlag(f => !f)
}, 100) // 只触发1次渲染

// 需要强制同步退出批处理
flushSync(() => {
  setCount(c => c + 1) // 立即触发渲染
})
```

### 2.2 Fiber架构的核心原理？
> 💡 Fiber是React16引入的新的协调引擎，React18的并发模式基于Fiber。

**Fiber核心特点：**
1. **可中断渲染**：将渲染拆分为小任务单元，可被优先级更高的任务中断
2. **优先级调度**：根据任务优先级（如用户交互 > 数据请求）决定执行顺序
3. **双缓存机制**：current Fiber树 + workInProgress Fiber树

```javascript
// Fiber节点结构（简化）
{
  type: 'div',        // 节点类型
  key: 'key1',        // 唯一标识
  stateNode: div,     // 真实DOM
  child: Fiber,       // 第一个子节点
  sibling: Fiber,     // 下一个兄弟节点
  return: Fiber,      // 父节点
  pendingProps: {},   // 新props
  memoizedProps: {},  // 旧props
  memoizedState: {},  // 状态
  lanes: 1,           // 优先级
  alternate: Fiber    // 对应缓存树的节点
}
```

### 2.3 Virtual DOM和Diff算法？
> 🎯 React的Diff算法复杂度为O(n)，基于三个策略：

1. **Tree Diff**：跨层级移动节点较少，只比较同层级节点
2. **Component Diff**：同类组件则递归比较，不同类直接替换
3. **Element Diff**：通过key优化节点复用

```jsx
// diff更新流程
// 1. type不同 → 直接替换整个子树
// 2. type相同，props不同 → 更新props
// 3. key相同 → 移动节点
// 4. key不同 → 删除/新增
```

### 2.4 Concurrent Mode（并发模式）工作原理？
> 💡 并发模式让React可以同时准备多个版本的UI，根据优先级切换。

```jsx
// useTransition：标记低优先级更新
function SearchPage() {
  const [query, setQuery] = useState('')
  const [isPending, startTransition] = useTransition()
  
  const handleChange = (e) => {
    // 高优先级：立即更新输入框
    setQuery(e.target.value)
    // 低优先级：搜索结果延迟更新
    startTransition(() => {
      setSearchResults(filterResults(e.target.value))
    })
  }
  return (
    <div>
      <input value={query} onChange={handleChange} />
      {isPending ? <Spinner /> : <Results list={searchResults} />}
    </div>
  )
}

// useDeferredValue：延迟更新值的版本
const deferredQuery = useDeferredValue(query)
const suggestions = useMemo(() => 
  filterItems(deferredQuery),
  [deferredQuery]
)
```

## 三、实战场景题

### 3.1 实现自定义Hook：useWindowSize
```jsx
function useWindowSize() {
  const [size, setSize] = useState({ width: window.innerWidth, height: window.innerHeight })
  useEffect(() => {
    const handleResize = () => setSize({ width: window.innerWidth, height: window.innerHeight })
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])
  return size
}
```

### 3.2 实现自定义Hook：useDebounce
```jsx
function useDebounce(value, delay = 300) {
  const [debouncedValue, setDebouncedValue] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debouncedValue
}
```

### 3.3 实现Error Boundary
```jsx
class ErrorBoundary extends React.Component {
  state = { hasError: false, error: null }
  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }
  componentDidCatch(error, info) {
    console.error(error, info.componentStack)
  }
  render() {
    if (this.state.hasError) {
      return this.props.fallback || <h1>Something went wrong</h1>
    }
    return this.props.children
  }
}
```

### 3.4 实现组件懒加载
```jsx
import { lazy, Suspense } from 'react'
const HeavyComponent = lazy(() => import('./HeavyComponent'))
function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <HeavyComponent />
    </Suspense>
  )
}
```

## 四、手写代码题

### 4.1 手写useState（简易版）
```jsx
let state = []
let index = 0
function useState(initial) {
  const currentIndex = index
  state[currentIndex] = state[currentIndex] ?? initial
  const setState = (newVal) => {
    state[currentIndex] = typeof newVal === 'function'
      ? newVal(state[currentIndex])
      : newVal
    render() // 触发重新渲染
  }
  index++
  return [state[currentIndex], setState]
}
```

### 4.2 手写useEffect（简易版）
```jsx
let deps = []
function useEffect(callback, depArray) {
  const hasNoDeps = !depArray
  const hasChanged = !deps || depArray.some((dep, i) => !Object.is(dep, deps[i]))
  if (hasNoDeps || hasChanged) {
    callback()
    deps = depArray
  }
}
```

### 4.3 手写React.memo
```jsx
function memo(Component, areEqual) {
  return React.memo
    ? React.memo(Component, areEqual)
    : class extends PureComponent {
        render() {
          return <Component {...this.props} />
        }
      }
}
```

### 4.4 手写Redux的createStore
```jsx
function createStore(reducer) {
  let state
  let listeners = []
  const getState = () => state
  const dispatch = (action) => {
    state = reducer(state, action)
    listeners.forEach(l => l())
  }
  const subscribe = (listener) => {
    listeners.push(listener)
    return () => { listeners = listeners.filter(l => l !== listener) }
  }
  dispatch({ type: '@@INIT' })
  return { getState, dispatch, subscribe }
}
```

## 五、系统设计题

### 5.1 React大型项目状态管理选型？
| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| Redux Toolkit | 大型复杂应用 | 生态完善，中间件丰富 | 样板代码多 |
| Zustand | 中小型应用 | 轻量（1KB），简单易用 | 生态较小 |
| Jotai | 原子状态 | 无Provider，细粒度更新 | 学习成本 |
| React Context | 低频更新全局状态 | 内置，无需额外库 | 消费都会重渲染 |

### 5.2 React应用性能优化策略？
```jsx
// 1. React.memo + useCallback + useMemo
// 2. 虚拟列表（react-window）
// 3. 代码分割（lazy + Suspense）
// 4. 图片懒加载
// 5. useDeferredValue处理大量数据
// 6. 避免不必要的重新渲染
// 7. Immutable数据（Immer）
```

## 六、常见坑点与最佳实践

| 坑点 | 问题描述 | 解决方案 |
|------|---------|---------|
| useEffect闭包陷阱 | 回调中读取的state是旧值 | 使用函数式更新或useRef |
| useState异步更新 | set后立即读取拿不到新值 | useEffect监听 |
| React.memo无效 | props是对象/函数引用变化 | useCallback/useMemo |
| 无限循环渲染 | useEffect依赖导致死循环 | 检查依赖项是否正确 |
| 状态更新合并 | 对象状态直接修改 | 不可变更新（展开运算符） |
| key使用索引 | 列表顺序变化导致混乱 | 使用稳定唯一ID |
| Context导致重渲染 | 所有Consumer都重渲染 | 拆分Context，使用useMemo |

### 闭包陷阱示例
```jsx
function Counter() {
  const [count, setCount] = useState(0)
  useEffect(() => {
    const timer = setInterval(() => {
      console.log(count) // 始终为0（闭包陷阱）
      setCount(count + 1) // 始终设置为1
    }, 1000)
    return () => clearInterval(timer)
  }, []) // ✗ 错误

  // ✅ 使用函数式更新
  useEffect(() => {
    const timer = setInterval(() => {
      setCount(c => c + 1)
    }, 1000)
    return () => clearInterval(timer)
  }, [])
}
```

## 七、面试回答模板

### Q1: 什么是Fiber？解决了什么问题？
> Fiber是React16推出的新的协调引擎，将渲染过程拆分为可中断的小任务单元。核心解决了React15中递归更新无法中断的问题（导致UI卡顿）。Fiber通过requestIdleCallback在浏览器空闲时执行任务，高优先级任务（如用户输入）可以中断低优先级任务（如列表渲染），保证交互的流畅性。

### Q2: useEffect和useLayoutEffect的区别？
> useEffect是异步执行（在浏览器绘制后执行），不会阻塞视图更新。useLayoutEffect是同步执行（在DOM更新后、浏览器绘制前执行），会阻塞视图更新。大多数场景用useEffect即可，需要测量DOM或同步操作DOM时用useLayoutEffect，避免白屏闪烁。

### Q3: React18的并发模式有什么实际价值？
> 核心价值是让React能"同时准备多个版本的UI"。通过useTransition标记低优先级更新，用户可以立即看到输入响应（高优），而搜索结果（低优）延迟渲染，界面不卡顿。useDeferredValue类似，但不需要包裹更新逻辑，适用于从props获取值的场景。实际项目中，搜索框、大数据列表、页面切换等场景能显著提升用户体验。

### Q4: useState和useReducer怎么选？
> state是独立值（数字/布尔）用useState；state是复杂对象，更新逻辑涉及多个子值，或存在依赖关系时用useReducer。useReducer更适合状态逻辑复杂的场景，如表单状态管理，能集中管理状态变更逻辑，方便测试。

### Q5: React中key的作用是什么？
> key帮助React识别哪些元素被修改、添加或删除。在同级列表中使用key，React能精准复用已有DOM节点，而不是全部重建。key应该是稳定、唯一、可预测的，优先使用数据ID。不要使用数组索引作为key（顺序变化会导致性能问题或状态错误）。

## 八、快速查漏补缺Checklist

- [ ] JSX本质和编译过程
- [ ] useState/useEffect/useRef/useContext使用
- [ ] useCallback/useMemo/memo性能优化
- [ ] useReducer/Zustand状态管理
- [ ] React Router v6路由配置
- [ ] Concurrent Mode / useTransition
- [ ] Fiber架构原理
- [ ] Virtual DOM Diff算法
- [ ] 自动批处理（React18）
- [ ] Error Boundary异常处理
- [ ] 受控/非受控组件
- [ ] 组件通信方式
- [ ] 自定义Hook封装
- [ ] 懒加载和代码分割
- [ ] Redux Toolkit使用
- [ ] TypeScript + React集成
- [ ] 常见性能优化策略
