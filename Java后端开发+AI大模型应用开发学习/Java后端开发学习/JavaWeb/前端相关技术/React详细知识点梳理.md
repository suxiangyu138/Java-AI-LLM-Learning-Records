03.26 18:21
React详细知识点梳理
一、React 基础核心
1.1 React 简介
React 是由 Facebook（现 Meta）开发并维护的开源前端 JavaScript 库，用于构建用户界面（UI），尤其适合构建复杂、交互式的单页面应用（SPA）。其核心理念是组件化和声明式编程，替代了传统的命令式编程（频繁操作 DOM），大幅提升开发效率和代码可维护性。
React 的核心优势：
组件化：将 UI 拆分为独立、可复用的组件，组件内部封装逻辑和样式，降低耦合度。
声明式编程：开发者只需描述“UI 应该是什么样子”，React 自动处理“如何渲染 UI”，无需手动操作 DOM。
虚拟 DOM（Virtual DOM）：通过内存中的虚拟 DOM 树替代真实 DOM，减少 DOM 操作，提升渲染性能。
单向数据流：数据从父组件流向子组件，状态管理清晰，避免数据混乱。
跨平台：结合 React Native 可开发 iOS、Android 移动应用，实现“一次学习，多端开发”。
1.2 React 环境搭建
React 开发环境依赖 Node.js 和 npm（或 yarn），常用搭建方式有两种：
方式1：使用 Create React App（官方推荐，快速搭建）
# 全局安装 create-react-app（可选，新版本可直接用 npx）
npm install -g create-react-app
# 新建 React 项目
npx create-react-app my-react-app
# 进入项目目录
cd my-react-app
# 启动开发服务器（默认端口 3000）
npm start
Create React App 会自动配置 Webpack、Babel 等工具，无需手动配置，开箱即用。
方式2：手动配置（Webpack + Babel）
适合需要自定义配置的场景，核心依赖：
webpack：模块打包工具，将 React 代码打包为浏览器可识别的 JS 文件。
babel：转译工具，将 ES6+、JSX 代码转译为 ES5 代码，兼容低版本浏览器。
react、react-dom：React 核心库和 DOM 渲染库。
1.3 JSX 语法（核心）
JSX（JavaScript XML）是 React 独创的语法，允许在 JavaScript 中编写 HTML 样的代码，本质是 React.createElement() 方法的语法糖，最终会被 Babel 转译为 JavaScript 代码。
1.3.1 JSX 基本规则
标签必须闭合：单标签（如 img、input）需加 / 闭合，例：<img src="" />。
根节点唯一：一个 JSX 表达式只能有一个根节点，可使用 <div>、<Fragment>（空标签，无额外 DOM 节点）或 <></>（Fragment 简写）包裹。
类名用 className：HTML 中的 class 在 JSX 中需改为 className（因 class 是 JavaScript 关键字），例：<div className="box"></div>。
内联样式用对象：JSX 中内联样式需写为 style={{ key: value }}，key 采用驼峰命名（如 fontSize、backgroundColor），例：<div style={{ fontSize: '16px', color: 'red' }}></div>。
表达式嵌入：用 {} 嵌入 JavaScript 表达式（变量、运算、函数调用等），例：<div>{ 1 + 1 }</div>、<div>{ username }</div>。
注释写法：{/* 注释内容 */}（JSX 内部），// 注释仅能用于 JavaScript 代码部分。
1.3.2 JSX 与 HTML 的区别
HTML
JSX
class
className
style="font-size: 16px"
style={{ fontSize: '16px' }}
onclick
onClick（驼峰命名）

（布尔值属性）
二、React 组件
组件是 React 应用的基本构建单元，每个组件负责渲染一个独立的 UI 部分，可分为函数组件和类组件（React 16.8 后，函数组件结合 Hooks 成为主流）。
2.1 函数组件（推荐）
函数组件是简单的 JavaScript 函数，接收 props 参数，返回 JSX 表达式，语法简洁，无状态（早期），结合 Hooks 后可拥有状态和生命周期。
// 基础函数组件
function Hello(props) {
  // props 是父组件传递过来的数据，只读不可修改
  return <h1>Hello, {props.name}</h1>;
}
// 箭头函数写法（更简洁）
const Hello = (props) => {
  const { name } = props; // 解构 props
  return <h1>Hello, {name}</h1>;
};
// 使用组件
function App() {
  return <Hello name="React" />;
}
2.2 类组件（传统方式）
类组件需要继承 React.Component，必须实现 render() 方法，render() 方法返回 JSX 表达式，可拥有状态（state）和生命周期方法。
import React from 'react';
class Hello extends React.Component {
  // 构造函数，初始化 state 和绑定事件（可选）
  constructor(props) {
    super(props); // 必须调用 super(props)，才能使用 this.props
    this.state = { count: 0 }; // 初始化状态
    // 绑定事件（避免 this 指向问题）
    this.handleClick = this.handleClick.bind(this);
  }
  // 事件处理方法
  handleClick() {
    // 修改状态必须用 this.setState()，不能直接赋值
    this.setState({ count: this.state.count + 1 });
  }
  // 渲染 UI
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
// 使用组件
class App extends React.Component {
  render() {
    return <Hello name="React" />;
  }
}
2.3 组件props（父子组件通信）
props（properties）是父组件传递给子组件的数据，是组件间通信的主要方式，特点：
只读性：子组件不能修改 props 的值，只能使用（单向数据流）。
可传递任意类型：数字、字符串、布尔值、数组、对象、函数、甚至组件。
默认值：可通过组件.defaultProps 设置 props 默认值。
类型校验：可通过 PropTypes 校验 props 的类型（避免传错数据），React 15.5 后需单独安装 prop-types 包。
import PropTypes from 'prop-types';
const Hello = (props) => {
  const { name, age, isStudent } = props;
  return (
    <div>
      <p>姓名：{name}</p>
      <p>年龄：{age}</p>
      <p>是否学生：{isStudent ? '是' : '否'}</p>
    </div>
  );
};
// 设置 props 默认值
Hello.defaultProps = {
  age: 18,
  isStudent: false
};
// 校验 props 类型
Hello.propTypes = {
  name: PropTypes.string.isRequired, // 字符串类型，必传
  age: PropTypes.number, // 数字类型
  isStudent: PropTypes.bool // 布尔类型
};
// 使用组件（不传 age 和 isStudent，会使用默认值）
function App() {
  return <Hello name="小明" />;
}
2.4 组件状态（State）
State 是组件内部的私有数据，用于管理组件自身的动态变化（如输入框内容、按钮点击计数），特点：
私有性：只有组件自身能修改 state，父组件无法直接修改子组件的 state。
不可直接修改：修改 state 必须使用 setState() 方法（类组件）或 useState Hook（函数组件），直接赋值不会触发组件重新渲染。
异步性：setState() 是异步操作，若需在修改后获取最新 state，可在 setState() 的第二个参数（回调函数）中操作。
类组件中使用 State
class Counter extends React.Component {
  constructor(props) {
    super(props);
    this.state = { count: 0 };
  }
  increment() {
    // 方式1：直接传递对象
    this.setState({ count: this.state.count + 1 });
    // 方式2：传递函数（推荐，当新 state 依赖旧 state 时）
    this.setState((prevState) => ({
      count: prevState.count + 1
    }), () => {
      // 回调函数，state 修改完成后执行
      console.log('最新计数：', this.state.count);
    });
  }
  render() {
    return (
      <div>
        <p>计数：{this.state.count}</p>
        <button onClick={() => this.increment()}>+1</button>
      </div>
    );
  }
}
函数组件中使用 State（useState Hook）
import { useState } from 'react';
const Counter = () => {
  // useState(初始值)：返回 [当前state, 修改state的函数]
  const [count, setCount] = useState(0);
  const increment = () => {
    // 直接传递新值
    setCount(count + 1);
    // 新 state 依赖旧 state 时，传递函数
    setCount(prevCount => prevCount + 1);
  };
  return (
    <div>
      <p>计数：{count}</p>
      <button onClick={increment}>+1</button>
    </div>
  );
}
三、React Hooks（React 16.8+ 核心）
Hooks 是 React 16.8 新增的特性，允许函数组件拥有状态（state）、生命周期和其他 React 特性，无需编写类组件，解决了类组件中“状态逻辑复用困难”“生命周期混乱”等问题。
核心规则：
只能在函数组件的顶层调用 Hooks，不能在循环、条件、嵌套函数中调用。
只能在 React 函数组件或自定义 Hooks 中调用 Hooks。
3.1 常用 Hooks
3.1.1 useState：管理组件状态
作用：为函数组件添加状态，用法如上文“函数组件中使用 State”所示。
补充：useState 可接收任意类型的初始值（数字、字符串、对象、数组等），修改对象/数组时，需返回新的对象/数组（不能直接修改原数据）。
import { useState } from 'react';
const User = () => {
  // 初始值为对象
  const [user, setUser] = useState({ name: '小明', age: 18 });
  const updateAge = () => {
    // 错误：直接修改原对象，不会触发重新渲染
    // user.age = 19;
    // setUser(user);
    // 正确：返回新对象
    setUser({ ...user, age: 19 });
  };
  return (
    <div>
      <p>姓名：{user.name}</p>
      <p>年龄：{user.age}</p>
      <button onClick={updateAge}>修改年龄</button>
    </div>
  );
}
3.1.2 useEffect：处理副作用
作用：替代类组件的生命周期方法（componentDidMount、componentDidUpdate、componentWillUnmount），处理组件中的“副作用”（如网络请求、DOM 操作、定时器、订阅等）。
语法：useEffect(() => { 副作用逻辑 }, [依赖项]);
第一个参数：副作用函数，组件渲染后执行。
第二个参数：依赖项数组（可选），控制副作用函数的执行时机：
不写依赖项：组件每次渲染（初始渲染 + 每次更新）都会执行。
空数组 []：只在组件初始渲染（componentDidMount）时执行一次。
有具体依赖项（如 [count, user]）：只有当依赖项的值发生变化时，才会执行。
清理函数：副作用函数可返回一个函数，在组件卸载（componentWillUnmount）或副作用重新执行前触发，用于清理资源（如清除定时器、取消订阅）。
import { useState, useEffect } from 'react';
const Timer = () => {
  const [count, setCount] = useState(0);
  // 副作用：定时器
  useEffect(() => {
    const timer = setInterval(() => {
      setCount(prev => prev + 1);
    }, 1000);
    // 清理函数：组件卸载时清除定时器
    return () => {
      clearInterval(timer);
    };
  }, []); // 空依赖，只执行一次（初始渲染）
  return <p>定时器计数：{count}</p>;
}
3.1.3 useContext：跨组件通信
作用：解决“props drilling”（ props 层层传递）问题，实现跨层级组件间的状态共享，无需手动传递 props。
使用步骤：
使用 React.createContext() 创建 Context 对象，可设置默认值。
使用 Context.Provider 组件包裹需要共享状态的组件树，通过 value 属性传递共享数据。
在需要使用共享数据的子组件中，使用 useContext(Context) 获取共享数据。
import { createContext, useContext, useState } from 'react';
// 1. 创建 Context
const ThemeContext = createContext('light'); // 默认值为 'light'
// 2. 父组件（Provider）
function App() {
  const [theme, setTheme] = useState('light');
  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };
  return (
    // 3. 提供共享数据
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      <Header />
      <Content />
    </ThemeContext.Provider>
  );
}
// 子组件（使用共享数据）
function Header() {
  const { theme } = useContext(ThemeContext);
  return <h1 style={{ color: theme === 'light' ? 'black' : 'white', backgroundColor: theme === 'light' ? 'white' : 'black' }}>Header</h1>;
}
// 孙组件（直接使用共享数据，无需通过 props 传递）
function Content() {
  const { theme, toggleTheme } = useContext(ThemeContext);
  return (
    <div style={{ color: theme === 'light' ? 'black' : 'white', backgroundColor: theme === 'light' ? 'white' : 'black', padding: '20px' }}>
      <p>当前主题：{theme}</p>
      <button onClick={toggleTheme}>切换主题</button>
    </div>
  );
}
3.1.4 useRef：获取 DOM 元素或保存持久值
作用：有两个核心用途：
获取 DOM 元素：通过 ref 绑定 DOM 元素，可直接操作 DOM（如获取输入框值、聚焦输入框）。
保存持久值：在组件多次渲染中，保存一个不变的值（不会因组件渲染而重置），且修改该值不会触发组件重新渲染。
import { useRef, useEffect } from 'react';
const RefDemo = () => {
  // 1. 获取 DOM 元素
  const inputRef = useRef(null);
  // 2. 保存持久值（组件渲染时不会重置）
  const countRef = useRef(0);
  useEffect(() => {
    // 聚焦输入框
    inputRef.current.focus();
    // 修改持久值，不会触发组件重新渲染
    countRef.current += 1;
    console.log('组件渲染次数：', countRef.current);
  }, []);
  const getInputValue = () => {
    // 获取输入框值
    console.log('输入框值：', inputRef.current.value);
  };
  return (
    <div>
      <input ref={inputRef} type="text" placeholder="请输入内容" />
      <button onClick={getInputValue}>获取输入值</button>
    </div>
  );
}
3.1.5 其他常用 Hooks
useReducer：替代 useState，用于管理复杂状态（如状态逻辑较多、状态依赖于前一个状态），类似 Redux 的思想。
useMemo：缓存计算结果，避免组件每次渲染时重复执行耗时计算，优化性能（依赖项变化时才重新计算）。
useCallback：缓存函数，避免组件每次渲染时重新创建函数，优化子组件性能（尤其当子组件接收函数 props 时）。
useLayoutEffect：与 useEffect 类似，但执行时机不同（useLayoutEffect 在 DOM 渲染完成后、浏览器绘制前执行，useEffect 在浏览器绘制后执行），适合需要操作 DOM 并立即看到效果的场景。
3.2 自定义 Hooks
自定义 Hooks 是基于 React 内置 Hooks 封装的可复用函数，命名必须以“use”开头，用于抽取组件中的可复用状态逻辑，实现逻辑复用。
示例：封装一个“获取窗口大小”的自定义 Hook
import { useState, useEffect } from 'react';
// 自定义 Hook，命名以 use 开头
function useWindowSize() {
  // 状态：窗口宽高
  const [windowSize, setWindowSize] = useState({
    width: window.innerWidth,
    height: window.innerHeight
  });
  // 副作用：监听窗口 resize 事件
  useEffect(() => {
    const handleResize = () => {
      setWindowSize({
        width: window.innerWidth,
        height: window.innerHeight
      });
    };
    // 绑定事件
    window.addEventListener('resize', handleResize);
    // 清理函数：移除事件监听
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);
  // 返回窗口大小
  return windowSize;
}
// 使用自定义 Hook
const WindowSizeDemo = () => {
  const { width, height } = useWindowSize();
  return (
    <div>
      <p>窗口宽度：{width}px</p>
      <p>窗口高度：{height}px</p>
    </div>
  );
}
四、React 事件处理
React 事件处理与原生 DOM 事件类似，但有一些语法差异，核心是“事件委托”（React 会将所有事件绑定到根节点，通过事件冒泡处理，提升性能）。
4.1 事件绑定语法
React 事件采用驼峰命名（如 onClick、onChange、onBlur，区别于原生的 onclick、onchange）。
事件处理函数需传递“函数引用”，而非字符串（如 onClick={handleClick}，而非 onClick="handleClick()"）。
import { useState } from 'react';
const EventDemo = () => {
  const [inputValue, setInputValue] = useState('');
  // 1. 普通事件处理函数
  const handleClick = () => {
    alert('按钮被点击了');
  };
  // 2. 带参数的事件处理函数（使用箭头函数包裹）
  const handleDelete = (id) => {
    alert(`删除id为${id}的数据`);
  };
  // 3. 输入框变化事件
  const handleInputChange = (e) => {
    // e 是 React 合成事件（SyntheticEvent），类似原生事件，但有兼容性处理
    setInputValue(e.target.value);
  };
  return (
    <div>
      <button onClick={handleClick}>点击我</button>
      <button onClick={() => handleDelete(123)}>删除数据</button>
      <input type="text" value={inputValue} onChange={handleInputChange} placeholder="请输入" />
    </div>
  );
}
4.2 合成事件（SyntheticEvent）
React 中的事件不是原生 DOM 事件，而是 React 封装的合成事件，目的是实现跨浏览器兼容性，特点：
合成事件与原生事件用法类似（如 e.target、e.preventDefault()、e.stopPropagation()）。
合成事件会被 React 统一管理，事件处理完成后会被回收（若需在异步操作中使用 e，需调用 e.persist() 保存）。
可通过 e.nativeEvent 获取原生 DOM 事件。
4.3 事件冒泡与阻止冒泡
React 事件支持冒泡，可通过 e.stopPropagation() 阻止事件冒泡，与原生 DOM 一致。
const BubbleDemo = () => {
  const handleParentClick = () => {
    alert('父元素被点击');
  };
  const handleChildClick = (e) => {
    e.stopPropagation(); // 阻止事件冒泡
    alert('子元素被点击');
  };
  return (
    <div onClick={handleParentClick} style={{ padding: '20px', backgroundColor: 'lightblue' }}>
      父元素
      <button onClick={handleChildClick} style={{ margin: '10px' }}>子元素按钮</button>
    </div>
  );
}
五、React 条件渲染与列表渲染
5.1 条件渲染
React 中可通过 JavaScript 条件语句（if-else、三元运算符、逻辑与/或）实现条件渲染，根据不同状态渲染不同的 UI。
import { useState } from 'react';
const ConditionDemo = () => {
  const [isLogin, setIsLogin] = useState(false);
  const [role, setRole] = useState('user');
  // 方式1：if-else（适合复杂条件）
  let userInfo;
  if (isLogin) {
    if (role === 'admin') {
      userInfo = <p>管理员，欢迎您！</p>;
    } else {
      userInfo = <p>普通用户，欢迎您！</p>;
    }
  } else {
    userInfo = <p>请先登录！</p>;
  }
  return (
    <div>
      {/* 方式2：三元运算符（适合简单二选一） */}
      {isLogin ? (
        <button onClick={() => setIsLogin(false)}>退出登录</button>
      ) : (
        <button onClick={() => setIsLogin(true)}>登录</button>
      )}
      {/* 方式3：逻辑与（适合“条件成立时渲染”） */}
      {isLogin && <p>登录成功，可访问个人中心</p>}
      {/* 方式1的渲染结果 */}
      {userInfo}
    </div>
  );
}
5.2 列表渲染
React 中使用 map() 方法遍历数组，生成列表组件，核心要求：每个列表项必须有唯一的 key 属性。
key 的作用：帮助 React 识别列表项的变化（新增、删除、排序），优化渲染性能，避免不必要的 DOM 操作。key 需是唯一且稳定的值（如数据的 id，避免使用 index 作为 key，除非列表不会变化）。
import { useState } from 'react';
const ListDemo = () => {
  const [todos, setTodos] = useState([
    { id: 1, text: '学习 React' },
    { id: 2, text: '掌握 Hooks' },
    { id: 3, text: '开发项目' }
  ]);
  // 添加新任务
  const addTodo = () => {
    const newTodo = { id: Date.now(), text: '新任务' };
    setTodos([...todos, newTodo]);
  };
  return (
    <div>
      <button onClick={addTodo}>添加任务</button>
      <ul>
        {/* 列表渲染：map 遍历数组，每个项添加 key */}
        {todos.map((todo) => (
          <li key={todo.id} style={{ margin: '5px 0' }}>
            {todo.text}
          </li>
        ))}
      </ul>
    </div>
  );
}
六、React 组件生命周期（类组件）
生命周期是类组件特有的概念，指组件从“创建”到“挂载”“更新”“卸载”的整个过程，每个阶段会触发对应的生命周期方法，用于执行特定逻辑。React 16.3 后，生命周期方法有所调整，推荐使用新的生命周期方法。
6.1 生命周期三个阶段
阶段1：挂载阶段（组件创建并渲染到 DOM）
执行顺序：constructor() → static getDerivedStateFromProps() → render() → componentDidMount()
constructor()：组件初始化，初始化 state、绑定事件，必须调用 super(props)。
static getDerivedStateFromProps(props, state)：静态方法，根据 props 更新 state（很少用），返回新的 state 或 null。
render()：渲染 UI，返回 JSX 表达式，不能执行副作用（如网络请求、DOM 操作）。
componentDidMount()：组件挂载完成（DOM 已渲染），可执行副作用（如网络请求、DOM 操作、定时器），只执行一次。
阶段2：更新阶段（组件状态或 props 变化）
执行顺序：static getDerivedStateFromProps() → shouldComponentUpdate() → render() → getSnapshotBeforeUpdate() → componentDidUpdate()
static getDerivedStateFromProps()：同挂载阶段，每次更新都会执行。
shouldComponentUpdate(nextProps, nextState)：判断组件是否需要重新渲染，返回 true（默认）则渲染，返回 false 则不渲染，可用于优化性能。
render()：重新渲染 UI。
getSnapshotBeforeUpdate(prevProps, prevState)：在 DOM 更新前执行，返回一个快照值，传递给 componentDidUpdate() 的第三个参数。
componentDidUpdate(prevProps, prevState, snapshot)：组件更新完成后执行，可执行副作用（如根据新 props 发起网络请求），注意避免在此处修改 state（会导致无限更新）。
阶段3：卸载阶段（组件从 DOM 中移除）
执行方法：componentWillUnmount()，组件卸载前执行，用于清理资源（如清除定时器、取消网络请求、移除事件监听），只执行一次。
6.2 函数组件的“生命周期”（Hooks 替代）
函数组件无生命周期方法，可通过 useEffect Hook 替代：
componentDidMount：useEffect(() => {}, [])
componentDidUpdate：useEffect(() => {}, [依赖项])
componentWillUnmount：useEffect(() => { return 清理函数 }, [])
七、React 状态管理
状态管理用于解决组件间状态共享的问题，根据应用规模选择不同的方案：
7.1 小型应用：useContext + useReducer
适合中小型应用，无需引入第三方库，通过 React 内置 Hooks 实现跨组件状态共享，如上文“useContext”示例，结合 useReducer 可管理复杂状态。
import { createContext, useContext, useReducer } from 'react';
// 1. 创建 Context
const CounterContext = createContext();
// 2. 定义 reducer（处理状态逻辑）
const counterReducer = (state, action) => {
  switch (action.type) {
    case 'INCREMENT':
      return { count: state.count + 1 };
    case 'DECREMENT':
      return { count: state.count - 1 };
    default:
      return state;
  }
};
// 3. Provider 组件
function CounterProvider({ children }) {
  // useReducer(reducer, 初始状态)：返回 [state, dispatch]
  const [state, dispatch] = useReducer(counterReducer, { count: 0 });
  // 封装操作方法（可选，简化子组件调用）
  const increment = () => dispatch({ type: 'INCREMENT' });
  const decrement = () => dispatch({ type: 'DECREMENT' });
  return (
    <CounterContext.Provider value={{ count: state.count, increment, decrement }}>
      {children}
    </CounterContext.Provider>
  );
}
// 4. 子组件使用
function Counter() {
  const { count, increment, decrement } = useContext(CounterContext);
  return (
    <div>
      <button onClick={decrement}>-1</button>
      <span style={{ margin: '0 10px' }}>{count}</span>
      <button onClick={increment}>+1</button>
    </div>
  );
}
// 5. 根组件
function App() {
  return (
    <CounterProvider>
      <Counter />
    </CounterProvider>
  );
}
7.2 中大型应用：Redux / Redux Toolkit
Redux 是一个独立的状态管理库，并非 React 专属，可与 React、Vue 等框架配合使用，适合大型应用（状态复杂、组件层级多）。
核心概念：
Store：存储全局状态的容器，整个应用只有一个 Store。
Action：描述状态变化的“动作”，是一个包含 type（动作类型）和 payload（数据）的对象。
Reducer：纯函数，根据 Action 类型修改 State，接收 (state, action)，返回新的 state（不可直接修改原 state）。
Dispatch：发送 Action 的方法，通过 store.dispatch(action) 触发状态变化。
Redux Toolkit（RTK）：是 Redux 官方推荐的工具集，简化了 Redux 的配置和代码编写（如无需手动写 reducer、action，内置中间件），是目前开发 Redux 应用的首选。
7.3 其他状态管理方案
MobX：基于观察者模式，语法简洁，适合中小型应用，学习成本低于 Redux。
Zustand：轻量级状态管理库，API 简洁，无需 Provider 包裹，适合中小型应用。
Recoil：Facebook 开发的状态管理库，专门为 React 设计，支持原子化状态，适合复杂 React 应用。
八、React 路由（React Router）
React 本身不提供路由功能，需使用第三方库 React Router（目前最新版本为 React Router v6），用于实现单页面应用（SPA）的页面跳转和路由管理。
8.1 安装 React Router
npm install react-router-dom
8.2 核心组件（React Router v6）
<BrowserRouter>：路由容器，包裹整个应用，用于管理路由历史记录，通常放在根组件。
<Routes>：路由容器，替代 v5 中的 <Switch>，用于包裹 <Route> 组件，确保每次只匹配一个路由。
<Route>：路由规则，path 属性指定路由路径，element 属性指定路由对应的组件。
<Link>：路由跳转组件，替代 a 标签，避免页面刷新，to 属性指定跳转路径。
<NavLink>：与 <Link> 类似，可设置当前激活路由的样式（通过 className 或 style）。
<Outlet>：嵌套路由的占位符，用于渲染子路由组件。
8.3 基本使用示例
import { BrowserRouter as Router, Routes, Route, Link, NavLink } from 'react-router-dom';
// 页面组件
const Home = () => <h1>首页</h1>;
const About = () => <h1>关于我们</h1>;
const Contact = () => <h1>联系我们</h1>;
// 404 页面
const NotFound = () => <h1>404 Not Found</h1>;
function App() {
  return (
    <Router>
      {/* 导航栏 */}
      <nav style={{ margin: '10px 0' }}>
        {/* Link 跳转 */}
        <Link to="/" style={{ marginRight: '10px' }}>首页</Link>
        {/* NavLink 跳转，激活时添加样式 */}
        <NavLink 
          to="/about" 
          style={{ marginRight: '10px' }}
          className={({ isActive }) => isActive ? 'active' : ''}
        >
          关于我们
        </NavLink>
        <Link to="/contact">联系我们</Link>
      </nav>
      {/* 路由规则 */}
      <Routes>
        {/* 首页路由，path="/" 可设置 index 为默认路由 */}
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/contact" element={<Contact />} />
        {/* 404 路由，path="*" 匹配所有未匹配的路由 */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}
8.4 动态路由与参数传递
动态路由用于匹配不确定的路由路径（如详情页），通过 :param 定义动态参数，使用 useParams() Hook 获取参数。
import { Routes, Route, Link, useParams } from 'react-router-dom';
// 列表页
const UserList = () => (
  <div>
    <h1>用户列表</h1>
    <Link to="/user/1">用户1</Link><br />
    <Link to="/user/2">用户2</Link>
  </div>
);
// 详情页（动态路由）
const UserDetail = () => {
  // 获取动态参数
  const { id } = useParams();
  return <h1>用户详情页，用户ID：{id}</h1>;
};
function App() {
  return (
    <Router>
      <Routes>
        <Route path="/user" element={<UserList />} />
        {/* 动态路由：:id 是动态参数 */}
        <Route path="/user/:id" element={<UserDetail />} />
      </Routes>
    </Router>
  );
}
8.5 编程式导航
除了使用 <Link> 组件跳转，还可通过 useNavigate() Hook 实现编程式导航（如点击按钮跳转、登录后跳转）。
import { useNavigate } from 'react-router-dom';
const Login = () => {
  const navigate = useNavigate();
  const handleLogin = () => {
    // 模拟登录成功
    const isLogin = true;
    if (isLogin) {
      // 跳转至首页，replace: true 表示替换当前历史记录（无法返回登录页）
      navigate('/', { replace: true });
    }
  };
  return (
    <div>
      <h1>登录页</h1>
      <button onClick={handleLogin}>登录</button>
      <button onClick={() => navigate(-1)}>返回上一页</button>
    </div>
  );
}
九、React 性能优化
React 本身已做了很多性能优化（如虚拟 DOM、批处理更新），但在复杂应用中，仍需手动优化，核心思路是“减少不必要的组件渲染”。
9.1 避免不必要的渲染
使用 React.memo：包装函数组件，实现组件的浅比较，当 props 未变化时，避免组件重新渲染。
使用 useMemo：缓存计算结果，避免每次渲染重复执行耗时计算。
使用 useCallback：缓存函数，避免每次渲染重新创建函数，尤其当子组件接收函数 props 时。
避免在渲染时创建函数/对象：如 onClick={() => handleClick(1)}、style={{ color: 'red' }}，可提取为常量或使用 useCallback/useMemo。
import { useState, memo, useMemo, useCallback } from 'react';
// 使用 React.memo 包装子组件，浅比较 props
const Child = memo(({ name, onClick }) => {
  console.log('子组件渲染');
  return <button onClick={onClick}>{name}</button>;
});
const Parent = () => {
  const [count, setCount] = useState(0);
  const [name, setName] = useState('按钮');
  // 使用 useCallback 缓存函数，避免每次渲染重新创建
  const handleClick = useCallback(() => {
    console.log('点击按钮');
  }, []); // 空依赖，函数不会变化
  // 使用 useMemo 缓存计算结果
  const doubleCount = useMemo(() => {
    return count * 2;
  }, [count]); // 只有 count 变化时，才重新计算
  return (
    <div>
      <p>count：{count}，doubleCount：{doubleCount}</p>
      <button onClick={() => setCount(prev => prev + 1)}>+1</button>
      <Child name={name}

