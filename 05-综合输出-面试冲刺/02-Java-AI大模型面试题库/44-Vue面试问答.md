# Vue 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理
> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Vue 3 的响应式原理是什么？和 Vue 2 相比有什么改进？

**面试官意图：** 考察对响应式核心机制的理解深度，以及是否关注框架版本演进的关键变化。

**完美解答：**

Vue 3 的响应式核心基于 **Proxy** 实现，而 Vue 2 基于 **Object.defineProperty**。两者最本质的区别在于拦截能力的不同。

Vue 2 的 `Object.defineProperty` 只能劫持对象的**已有属性**的 getter/setter，存在三个致命局限：

- **无法检测属性的新增和删除**——必须使用 `Vue.set()` / `Vue.delete()` 来弥补。
- **无法直接拦截数组下标赋值和长度变化**——需要通过重写数组的 7 个变更方法（push、pop、splice 等）来 hack。
- **初始化时必须递归遍历所有属性**——对于深层嵌套的大对象，性能开销较大。

Vue 3 的 `Proxy` 直接代理整个对象，而不是某个属性：

```javascript
// Vue 3 响应式核心简化示意
const reactive = (target) => {
  return new Proxy(target, {
    get(target, key, receiver) {
      track(target, key)       // 依赖收集
      return Reflect.get(target, key, receiver)
    },
    set(target, key, value, receiver) {
      const result = Reflect.set(target, key, value, receiver)
      trigger(target, key)     // 触发更新
      return result
    },
    deleteProperty(target, key) {
      const result = Reflect.deleteProperty(target, key)
      trigger(target, key)     // 删除也能触发更新
      return result
    }
  })
}
```

`ref` 的本质是对基本类型做了一层包装——内部也是通过 `reactive` 实现，只不过用 `{ value: 原始值 }` 的形式包裹。

**改进总结：**

| 维度 | Vue 2 | Vue 3 |
|------|-------|-------|
| 底层 API | Object.defineProperty | Proxy |
| 新增/删除属性 | 无法自动检测，需 Vue.set/Vue.delete | 自动检测 |
| 数组拦截 | 重写数组方法，下标赋值不触发更新 | 原生拦截，下标赋值也触发 |
| 初始化性能 | 递归遍历所有属性，嵌套越深越慢 | 惰性代理，访问时才递归 |
| Tree-shaking | 响应式代码全部打包 | 按需引入，更小体积 |
| 多响应式副本 | 同一个数据源只能有一个响应式实例 | 每个组件调用独立，多实例无冲突 |

**延伸追问应对：**

如果问"Proxy 的兼容性怎么处理？"——Vue 3 不再支持 IE 11，Proxy 是 ES6 特性，在现代浏览器中全覆盖。如果项目必须兼容 IE，应该选 Vue 2 而不是试图给 Proxy 打 polyfill（Proxy 无法完美 polyfill）。

---

### Q2：组合式 API（Composition API）和选项式 API（Options API）该怎么选？

**面试官意图：** 考察是否真正在项目中用过组合式 API，而不只是看文档知道个概念。

**完美解答：**

**选项式 API** 把代码按 `data`、`methods`、`computed`、`watch` 等选项分开组织。优点是结构固定、上手门槛低，适合简单页面或新手入门。但**痛点非常明显**：当一个组件的逻辑变得复杂时，同一个功能的代码会被拆散到各个选项中，无法复用逻辑片段。

**组合式 API** 允许你按**逻辑关注点**组织代码，而不是按选项类型。同一个业务功能的所有代码（响应式状态、计算属性、方法、侦听器）可以写在一起，并且通过 `useXxx` 函数轻松抽离和复用。

```
// 选项式 API：同一功能的代码被分散
export default {
  data() { return { todos: [], filter: 'all' } },
  computed: { filteredTodos() { ... } },
  methods: { addTodo() { ... }, toggleTodo() { ... } },
  watch: { todos: { handler() { localStorage.setItem(...) }, deep: true } }
}

// 组合式 API：同一功能的代码聚在一起，还能抽出去复用
export default {
  setup() {
    const todos = ref([])
    const filter = ref('all')
    const filteredTodos = computed(() => ...)
    const addTodo = (text) => todos.value.push({ text, done: false })
    watch(todos, (val) => localStorage.setItem('todos', JSON.stringify(val)), { deep: true })
    return { todos, filter, filteredTodos, addTodo }
  }
}
```

**选择建议：**

| 场景 | 推荐 |
|------|------|
| 简单静态页面、表单展示 | 选项式 API 即可，简洁直观 |
| 组件逻辑复杂（多个关注点交织） | 组合式 API，按模块组织 |
| 需要跨组件复用逻辑 | 组合式 API + 自定义 Hook（`useAuth`、`usePagination`） |
| 团队新人对 Vue 不熟 | 先从选项式上手，逐步过渡到组合式 |
| TypeScript 项目 | 组合式 API 类型推导更友好 |
| 大型企业级项目 | 强制组合式 API，统一规范 |

> 💡 **面试加分：** 主动提到 Vue 3 的 `<script setup>` 语法糖。它让组合式 API 更加简洁——不需要 `return`，不需要显式写 `setup()` 函数，是 Vue 3 项目的推荐写法。

**延伸追问应对：**

如果问"组合式 API 和 React Hooks 有什么区别？"——核心区别在于响应式机制不同。React Hooks 每次渲染都会重新执行，依赖数组决定是否重新计算；Vue 的组合式 API 基于依赖追踪，`computed` 和 `watch` 自动收集依赖，不需要手动声明依赖数组，也没有闭包陷阱的问题。

---

### Q3：v-model 的原理是什么？Vue 3 中的 v-model 有哪些变化？

**面试官意图：** 考察对双向绑定的理解，以及是否熟悉 Vue 3 的重要破坏性变更。

**完美解答：**

`v-model` 的本质是**语法糖**，它背后是 `props` + `emit` 的组合。

**Vue 2 中的 v-model：**

```vue
<!-- 等价于 -->
<ChildComponent v-model="value" />

<!-- 实际上是 -->
<ChildComponent :value="value" @input="value = $event" />
```

默认接收名为 `value` 的 prop，触发名为 `input` 的事件。因此一个组件只能有一个 `v-model`。

**Vue 3 中的 v-model：**

```vue
<!-- 等价于 -->
<ChildComponent v-model="value" />

<!-- 实际上是 -->
<ChildComponent :modelValue="value" @update:modelValue="value = $event" />
```

- prop 名从 `value` 改为 `modelValue`。
- 事件名从 `input` 改为 `update:modelValue`。
- **支持多个 v-model 绑定**，这是最重要的变化。

```vue
<!-- 多个 v-model 绑定 -->
<ChildComponent 
  v-model:name="name" 
  v-model:age="age" 
/>

<!-- 等价于 -->
<ChildComponent 
  :name="name" 
  @update:name="name = $event"
  :age="age" 
  @update:age="age = $event" 
/>
```

**自定义组件实现 v-model：**

```vue
<script setup>
// ChildComponent.vue
const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const handleInput = (e) => {
  emit('update:modelValue', e.target.value)
}
</script>

<template>
  <input :value="modelValue" @input="handleInput" />
</template>
```

> 💡 **面试加分：** Vue 3 还新增了 `v-model` 修饰符的自定义支持。通过 `modelModifiers` prop 可以接收自定义修饰符，实现更灵活的双向绑定控制。

**延伸追问应对：**

如果问"v-model 和 .sync 修饰符的关系？"——Vue 2 中 `.sync` 用来实现多个双向绑定（`:foo.sync="val"` 等价于 `@update:foo`），Vue 3 中 `v-model` 直接支持多个绑定，所以 `.sync` 被废弃了，统一用 `v-model:propName`。

---

### Q4：computed 和 watch 有什么区别？分别在什么场景下使用？

**面试官意图：** 考察对响应式副作用的理解深度，以及是否能在实际场景中做出正确选择。

**完美解答：**

**computed（计算属性）** 是基于依赖的**派生值**。它有两个核心特征：

- **缓存性**：只有依赖发生变化时才重新计算，多次访问直接返回缓存结果。
- **纯函数性质**：不应该有副作用，只做计算逻辑。

**watch（侦听器）** 是响应式变化时的**副作用回调**。它注重"过程"而不是"返回值"：

- **不缓存**：每次变化都执行回调。
- **用于副作用**：异步操作、DOM 操作、状态同步等。
- **可以访问变化前后的值**：`(newVal, oldVal) => ...`。

```javascript
// computed - 派生状态
const filteredTodos = computed(() => {
  return todos.value.filter(t => t.done)
})

// watch - 执行副作用
watch(todos, (newTodos) => {
  localStorage.setItem('todos', JSON.stringify(newTodos))
}, { deep: true })

// watch 也能监听多个来源
watch([todos, filter], ([newTodos, newFilter]) => {
  console.log('todos 或 filter 发生了变化')
})
```

| 维度 | computed | watch |
|------|----------|-------|
| 返回值 | 有返回值，直接用于模板或逻辑 | 无返回值，执行回调 |
| 缓存 | 有缓存，依赖不变不重算 | 不缓存，每次变化都触发 |
| 是否用于模板 | 直接用在模板中 `{{ filtered }}` | 不直接在模板中使用 |
| 副作用 | 不应有副作用 | 专门处理副作用 |
| 新旧值 | 不提供 | 提供 `(newVal, oldVal)` |
| 适合场景 | 数据转换、筛选、求和、格式化 | 异步请求、本地存储、DOM 操作 |

> ⚠️ **常见坑：** 在 `watch` 中修改监听的数据源本身可能导致死循环。善用 `{ deep: true }` 监听对象内部变化，但注意深度监听开销较大。对于明确知道要监听某个具体属性的场景，用函数返回值形式：`watch(() => obj.someProp, handler)` 更高效。

**延伸追问应对：**

如果问"watchEffect 和 watch 的区别？"——`watchEffect` 自动收集依赖，初始化立即执行，不需要指定监听源；`watch` 需要明确指定监听源，惰性执行（默认不立即执行）。`watchEffect` 适合副作用和依赖收集自动化的场景，`watch` 适合需要精确控制监听源、需要访问新旧值的场景。

---

## 2. 项目实战深度问答
> 💡 面试官会深挖你的项目细节

### Q5：你在 TodoList 项目中如何处理组件的合理拆分？状态应该放在哪一层？

**面试官意图：** 考察组件化设计能力，以及状态管理的基本原则——什么状态用 `ref`，什么状态用 `computed`，什么状态应该提升到父组件。

**完美解答：**

TodoList 虽然是入门项目，但组件拆分思路可以直接体现工程化水平。我的拆分方案是：

```
App.vue
├── TodoHeader.vue          # 输入框 + 添加按钮
├── TodoFilters.vue         # 全部 / 已完成 / 未完成 筛选按钮
├── TodoList.vue            # 列表渲染层
│   └── TodoItem.vue        # 单个待办项（展示 + 勾选 + 删除操作）
└── TodoFooter.vue          # 统计信息 + 清空已完成
```

**状态管理策略：**

```javascript
// 状态只放在 TodoList.vue（或者提升到 App.vue），而不是分散在各个子组件
// 采用"单向数据流"：父组件传 props 给子组件，子组件通过 emit 通知父组件

// 父组件（状态持有者）
const todos = ref([
  { id: 1, text: '学习 Vue 3 Composition API', done: false },
  { id: 2, text: '完成 TodoList 项目', done: true }
])
const filter = ref('all')

const filteredTodos = computed(() => {
  if (filter.value === 'done') return todos.value.filter(t => t.done)
  if (filter.value === 'pending') return todos.value.filter(t => !t.done)
  return todos.value
})

// 子组件只接收 props，不修改 props
// TodoItem.vue
const props = defineProps({ todo: Object })
const emit = defineEmits(['toggle', 'delete'])
```

**选择原则：**

| 状态类型 | 放置位置 | 理由 |
|----------|----------|------|
| 待办列表数据 | 父组件或 Pinia store | 多个子组件共享 |
| 筛选条件 | 父组件或 Pinia store | 影响列表渲染 |
| 输入框文本 | 子组件内部 ref | 仅输入框自身使用，不对外暴露 |
| 筛选后的列表 | computed 派生 | 不存储，依赖变化自动重算 |

> 💡 **面试加分：** 提到"状态提升"原则——当多个子组件需要共享同一份状态时，把状态提升到它们的共同父组件中管理。这是 Vue 和 React 都遵循的设计模式。

---

### Q6：购物车项目中，父子通信、兄弟组件通信、跨层级通信分别怎么处理？

**面试官意图：** 考察对 Vue 组件通信体系的完整掌握，不只是知道"一种方式"。

**完美解答：**

购物车项目是练熟组件通信的绝佳场景，因为天然涉及多种通信模式。

```
App.vue（购物车页面）
├── CartHeader.vue           # 显示商品总数
├── CartList.vue             # 商品列表
│   └── CartItem.vue         # 单个商品（数量调整、选中/取消、删除）
├── CartSummary.vue          # 汇总区域（选中商品总价、结算按钮）
└── CartCoupon.vue           # 优惠券选择（跨层级通信示例）
```

**分层通信方案：**

| 通信类型 | 技术方案 | 购物车场景 |
|----------|----------|-----------|
| 父 → 子 | `props` | App 传递商品列表给 CartList |
| 子 → 父 | `emit` | CartItem 通知 App 修改数量 |
| 兄弟组件 | 状态提升到共同父组件 | CartList 数量变化 → App 状态更新 → CartSummary 自动更新 |
| 跨层级 | `provide / inject` | App provide 当前购物车数据，CartCoupon inject 使用 |
| 深层跨组件 | Pinia（推荐） | 购物车状态统一管理，任意组件都可读写 |

```javascript
// 使用 Pinia 管理购物车状态（生产环境推荐方案）
// stores/cart.js
export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  const totalPrice = computed(() =>
    items.value.filter(i => i.checked).reduce((sum, i) => sum + i.price * i.count, 0)
  )

  const selectedCount = computed(() =>
    items.value.filter(i => i.checked).reduce((sum, i) => sum + i.count, 0)
  )

  function addItem(product) { /* ... */ }
  function updateCount(id, delta) { /* ... */ }
  function toggleCheck(id) { /* ... */ }
  function removeItem(id) { /* ... */ }

  return { items, totalPrice, selectedCount, addItem, updateCount, toggleCheck, removeItem }
})
```

> 💡 **面试加分：** 提到"选择 `props` + `emit` 还是 Pinia"的权衡尺度。页面内简单通信用 `props/emit` 足够了；但如果购物车数据需要在多个页面间共享（比如订单页、商品详情页、购物车页），就应该用 Pinia。

---

### Q7：权限管理系统中的动态路由和 RBAC 是怎么实现的？

**面试官意图：** 考察企业级项目的核心难点——权限控制，这是全栈面试的高频考点。

**完美解答：**

动态路由和 RBAC 是权限管理系统的两个核心模块，它们配合实现"不同用户登录后看到不同的菜单和页面"。

**动态路由的实现流程：**

```
用户登录 → 后端返回角色和权限标识 → 前端根据权限动态添加路由 → 生成侧边栏菜单
```

```javascript
// router/index.js
import { createRouter, createWebHistory } from 'vue-router'

// 公开路由——所有人都能访问
const publicRoutes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/404', component: () => import('@/views/NotFound.vue') }
]

// 动态路由——需要权限才能访问
const asyncRoutes = [
  {
    path: '/system',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['admin', 'editor'], title: '系统管理', icon: 'Setting' },
    children: [
      { path: 'user', component: () => import('@/views/system/User.vue'), meta: { roles: ['admin'], title: '用户管理' } },
      { path: 'role', component: () => import('@/views/system/Role.vue'), meta: { roles: ['admin', 'editor'], title: '角色管理' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes: publicRoutes })

// 动态添加路由的核心方法
export function addDynamicRoutes(roles) {
  const accessibleRoutes = filterRoutes(asyncRoutes, roles)
  accessibleRoutes.forEach(route => {
    router.addRoute(route)   // 动态添加路由
  })
}

// 递归过滤：根据用户角色过滤可访问的路由
function filterRoutes(routes, roles) {
  return routes.filter(route => {
    if (route.meta?.roles && !route.meta.roles.some(r => roles.includes(r))) {
      return false
    }
    if (route.children) {
      route.children = filterRoutes(route.children, roles)
    }
    return true
  })
}
```

**RBAC（基于角色的访问控制）模型：**

```
用户 ↔ 角色 ↔ 权限

用户登录 → 获取角色列表 → 根据角色获取权限标识（权限码）
→ 路由级别过滤（动态路由）
→ 菜单级别过滤（侧边栏显隐）
→ 按钮级别过滤（增删改查按钮显隐）
```

**按钮级权限实现：**

```vue
<!-- 自定义权限指令 v-permission -->
<button v-permission="'user:create'">新增用户</button>
<button v-permission="'user:delete'">删除用户</button>

<!-- 权限指令实现 -->
<script setup>
const app = createApp(App)
app.directive('permission', {
  mounted(el, binding) {
    const permissions = usePermissionStore().permissions
    if (!permissions.includes(binding.value)) {
      el.parentNode?.removeChild(el)  // 无权限则移除元素
    }
  }
})
</script>
```

**路由守卫配合权限校验：**

```javascript
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') return next()

  // 未登录，跳转登录页
  if (!userStore.token) return next('/login')

  // 已登录但未加载权限，获取权限并动态添加路由
  if (!userStore.roles.length) {
    const roles = await userStore.getUserInfo()
    addDynamicRoutes(roles)
    next({ ...to, replace: true })  // 重新导航以应用新路由
  } else {
    next()
  }
})
```

> ⚠️ **注意：** 前端权限控制只是用户体验层面的限制，真正的安全校验必须在后端完成。前端可以通过修改代码绕过按钮隐藏，但后端的接口鉴权是无法绕过的。

---

### Q8：AI 对话 Web 端项目中，WebSocket 流式输出是怎么实现的？如何处理断线重连？

**面试官意图：** 考察实时通信和 AI 前端场景的实战能力，这是 Java+AI 方向面试的核心考点。

**完美解答：**

AI 对话的流式输出是最关键的交互体验——用户发出问题后，AI 的回答一个字一个字地显示出来，而不是等待全部生成完毕一次性展示。

**技术选型：** WebSocket + Server-Sent Events（SSE），后端采用 Spring AI + SSE 协议，前端使用 WebSocket 或 EventSource 接收流式数据。

```javascript
// 方式一：使用原生 WebSocket 封装
// utils/websocket.js
class ChatWebSocket {
  constructor(url) {
    this.url = url
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
  }

  connect(sessionId) {
    this.ws = new WebSocket(`${this.url}?sessionId=${sessionId}`)

    this.ws.onopen = () => {
      console.log('WebSocket 连接已建立')
      this.reconnectAttempts = 0
    }

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      switch (data.type) {
        case 'start':     // AI 开始生成
          this.onStart?.()
          break
        case 'delta':     // 流式增量内容（核心）
          this.onDelta?.(data.content)
          break
        case 'done':      // 生成完成
          this.onDone?.(data.fullContent)
          break
        case 'error':     // 错误
          this.onError?.(data.message)
          break
      }
    }

    this.ws.onclose = () => {
      this.handleReconnect(sessionId)
    }

    this.ws.onerror = () => {
      this.ws?.close()
    }
  }

  handleReconnect(sessionId) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      const delay = this.reconnectDelay * Math.pow(1.5, this.reconnectAttempts - 1)
      console.log(`尝试第 ${this.reconnectAttempts} 次重连，延迟 ${delay}ms`)
      setTimeout(() => this.connect(sessionId), delay)
    } else {
      this.onFailed?.('连接已断开，请刷新页面重试')
    }
  }

  send(message) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  close() {
    this.maxReconnectAttempts = 0  // 主动关闭不重连
    this.ws?.close()
  }
}
```

**前端对话组件使用流式输出：**

```vue
<script setup>
// views/chat/ChatRoom.vue
const messages = ref([])
const currentMessage = ref('')
const isLoading = ref(false)
const ws = ref(null)

async function sendMessage() {
  const userMsg = { role: 'user', content: currentMessage.value }
  messages.value.push(userMsg)

  // 插入 AI 占位消息
  const aiMsg = { role: 'assistant', content: '', loading: true }
  messages.value.push(aiMsg)

  isLoading.value = true
  ws.value = new ChatWebSocket('ws://localhost:8080/ai/chat')

  ws.value.onStart = () => {
    aiMsg.loading = false
  }

  ws.value.onDelta = (content) => {
    aiMsg.content += content  // 逐步追加内容
  }

  ws.value.onDone = () => {
    isLoading.value = false
    // 保存完整对话到历史
  }

  ws.value.onError = (msg) => {
    aiMsg.content = `出错了：${msg}`
    isLoading.value = false
  }

  ws.value.connect(sessionId)
  ws.value.send({ content: currentMessage.value })
  currentMessage.value = ''
}
</script>

<template>
  <div class="message-list">
    <div v-for="msg in messages" :key="msg.id" :class="msg.role">
      <!-- 代码高亮渲染 -->
      <MarkdownRenderer :content="msg.content" />
      <div v-if="msg.loading" class="typing-indicator">
        <span class="dot"></span><span class="dot"></span><span class="dot"></span>
      </div>
    </div>
  </div>
</template>
```

**断线重连策略的关键设计点：**

| 策略 | 实现方式 | 说明 |
|------|----------|------|
| 指数退避 | `delay = base * 1.5^(attempt-1)` | 避免频繁重连加重服务器压力 |
| 最大重试次数 | 限制 3-5 次 | 超过上限提示用户手动刷新 |
| 会话恢复 | 重连时携带 sessionId | 后端保留上下文，继续未完成的生成 |
| 心跳检测 | 每 30s 发送 ping | 及时发现连接断开 |
| 可见性检测 | `document.visibilitychange` | 用户切回页面时检查连接状态 |

> 💡 **面试加分：** 提到"打字机效果"的性能优化——频繁更新 DOM 可能导致卡顿。可以使用 `requestAnimationFrame` 批量更新，或者设定 50ms 的更新间隔，避免每次 `delta` 都触发重渲染。

---

## 3. 进阶与系统设计
> 💡 拉开差距的环节

### Q9：如果让你从零设计一个企业级权限管理系统的前端架构，你会怎么做？

**面试官意图：** 考察系统设计能力——不只是实现功能，而是要有架构思维、可扩展性思考和安全性考量。

**完美解答：**

企业级权限管理系统不是简单的"登录 + 路由守卫"，而是一个完整的授权体系。我会从四个层面来设计：

**第一层：用户认证（Authentication）**

```javascript
// stores/auth.js
// JWT Token 管理：access_token + refresh_token 双令牌机制
// access_token 短期有效（15分钟），refresh_token 长期有效（7天）
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken'))
  const refreshToken = ref(localStorage.getItem('refreshToken'))
  const user = ref(null)

  async function login(credentials) {
    const { accessToken: at, refreshToken: rt, user: u } = await loginApi(credentials)
    accessToken.value = at
    refreshToken.value = rt
    user.value = u
    // Token 持久化
    localStorage.setItem('accessToken', at)
    localStorage.setItem('refreshToken', rt)
  }

  // 自动刷新 Token
  async function refreshAccessToken() {
    const res = await refreshTokenApi(refreshToken.value)
    accessToken.value = res.accessToken
    localStorage.setItem('accessToken', res.accessToken)
  }
})
```

**第二层：路由权限（Route Guard）**

```
多层路由守卫体系：
┌─────────────────────┐
│ 全局前置守卫         │ → 检查登录态，无 → /login
│ router.beforeEach   │
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ 角色路由过滤         │ → 已登录但无角色信息 → 获取用户角色
│ 动态添加路由         │ → 根据角色过滤 asyncRoutes
└─────────┬───────────┘
          ▼
┌─────────────────────┐
│ 路由 meta 校验       │ → meta.roles 包含当前角色才放行
│ 按钮级权限           │ → 自定义指令 v-permission 控制显隐
└─────────────────────┘
```

**第三层：数据权限（Data Level）**

- 用户只能看到自己部门的数据
- 数据查询时携带部门 ID 参数
- 后端鉴权，前端只做展示适配

**第四层：可视化的权限管理后台**

```
用户管理      → 给用户分配角色
角色管理      → 定义角色，给角色分配权限
菜单管理      → 维护动态路由树，配置菜单名称、图标、排序
权限标识管理  → 维护权限码字典（user:create, user:edit, user:delete）
```

```vue
<!-- 角色-权限分配的树形组件 -->
<template>
  <el-tree
    :data="permissionTree"
    show-checkbox
    node-key="id"
    :default-checked-keys="role.permissions"
    @check="handlePermissionChange"
    :props="{ label: 'label', children: 'children' }"
  />
</template>
```

**架构图的核心设计要点：**

| 设计维度 | 方案 | 理由 |
|----------|------|------|
| 状态管理 | Pinia 模块化拆分 | authStore、permissionStore、appStore 各司其职 |
| 请求封装 | Axios 拦截器统一处理 Token | 请求拦截器注入 Token，响应拦截器处理 401 自动刷新 |
| 路由设计 | 静态路由 + 动态路由分离 | 登录页、404 等固定路由不走权限过滤 |
| 菜单生成 | 根据动态路由树递归生成 | 路由配置即菜单配置，保持单一数据源 |
| 权限粒度 | 路由级 + 按钮级 + 数据级 | 逐层递进，满足企业级需求 |

---

### Q10：Axios 二次封装你是怎么设计的？拦截器一般处理哪些逻辑？

**面试官意图：** 考察工程化能力和代码组织能力——90% 的 Vue 项目都会做 Axios 封装，但每个人的封装质量差异很大。

**完美解答：**

Axios 二次封装是 Vue 项目的标配，我在后台管理系统中的封装层级如下：

```
utils/
└── request/
    ├── index.js          # 导出封装好的 request 实例
    ├── interceptors.js   # 请求/响应拦截器逻辑
    └── statusCode.js     # HTTP 状态码枚举和错误提示
```

**核心封装代码：**

```javascript
// utils/request/index.js
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,  // 从环境变量读取
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端自定义业务状态码处理
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data  // 直接返回业务数据，调用方无需再 .data
  },
  async (error) => {
    if (error.response) {
      const { status } = error.response
      const authStore = useAuthStore()

      switch (status) {
        case 401:  // Token 过期或无效
          // 尝试刷新 Token
          try {
            await authStore.refreshAccessToken()
            // 刷新成功，重新发起原始请求
            error.config.headers.Authorization = `Bearer ${authStore.accessToken}`
            return request(error.config)
          } catch {
            // 刷新失败，强制退出登录
            authStore.logout()
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
          }
          break

        case 403:
          ElMessage.warning('没有操作权限')
          break

        case 404:
          ElMessage.error('请求的资源不存在')
          break

        case 500:
          ElMessage.error('服务器内部错误')
          break

        default:
          ElMessage.error(`请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
```

**拦截器职责清单：**

| 拦截器 | 职责 | 典型处理 |
|--------|------|----------|
| 请求拦截器 | 统一注入鉴权信息 | 添加 Authorization header |
| 请求拦截器 | 统一 Content-Type | 表单、JSON、文件上传自动适配 |
| 请求拦截器 | 请求时间戳/防重复 | 加时间戳防止缓存、loading 状态管理 |
| 响应拦截器 | 统一错误处理 | HTTP 状态码分类提示 |
| 响应拦截器 | Token 自动刷新 | 401 时无感刷新，重放请求队列 |
| 响应拦截器 | 数据解构 | 提取 `response.data.data`，调用方只需关注业务数据 |
| 响应拦截器 | 登录态失效处理 | 清除用户信息，跳转登录页 |

> 💡 **面试加分：** 提到"请求竞态处理"——当用户快速切换页面导致多个请求交错返回时，可以用 AbortController 取消上一个请求，避免数据覆盖。

---

## 4. 场景题与故障排查
> 💡 考察实际解决问题的能力

### Q11：页面渲染大量数据（如长列表、表格万级数据）时卡顿严重，你会怎么优化？

**面试官意图：** 考察性能优化的实战经验，而不是空谈理论。

**完美解答：**

性能优化不是一上来就上虚拟滚动，而是**分层排查、按需优化**。

**排查流程：**

```
卡顿 → 打开 Performance 面板录制 → 分析瓶颈：
  ├─ 渲染卡顿（大量 DOM 节点）→ 虚拟滚动
  ├─ 计算卡顿（computed 复杂计算）→ 缓存 / Web Worker
  ├─ 网络卡顿（请求过多）→ 分页 / 防抖节流
  └─ 内存泄漏（持续增长）→ 排查未清理的定时器和事件
```

**分层优化方案：**

| 优化层级 | 具体方案 | 适用场景 | 优化效果 |
|----------|----------|----------|----------|
| 数据层 | 后端分页，前端只渲染当前页 | 表格数据 | 最有效，从根源减少数据量 |
| 渲染层 | 虚拟滚动（vue-virtual-scroller） | 长列表、聊天记录 | DOM 节点控制在可视区域数量 |
| 计算层 | computed 缓存 + 避免深度 watch | 复杂筛选、聚合 | 避免无效重算 |
| 更新层 | `v-once` 静态内容 + `v-memo` 条件缓存 | 列表项中包含大量静态内容 | 减少 diff 开销 |
| 组件层 | 异步组件 + 懒加载 | 弹窗、详情面板 | 按需加载，减少首屏体积 |

**虚拟滚动核心实现示意：**

```vue
<!-- 虚拟滚动的基本原理：只渲染可视区域内的元素 -->
<script setup>
// 简单的虚拟列表实现
const props = defineProps({
  items: Array,
  itemHeight: { type: Number, default: 50 },
  containerHeight: { type: Number, default: 500 }
})

const scrollTop = ref(0)

const visibleData = computed(() => {
  const start = Math.floor(scrollTop.value / props.itemHeight)
  const visibleCount = Math.ceil(props.containerHeight / props.itemHeight) + 2
  return props.items.slice(start, start + visibleCount)
})

const paddingTop = computed(() => {
  return Math.floor(scrollTop.value / props.itemHeight) * props.itemHeight
})
</script>

<template>
  <div class="virtual-list" :style="{ height: containerHeight + 'px', overflowY: 'auto' }"
       @scroll="scrollTop = $event.target.scrollTop">
    <div :style="{ height: items.length * itemHeight + 'px' }">
      <div :style="{ transform: `translateY(${paddingTop}px)` }">
        <div v-for="item in visibleData" :key="item.id"
             :style="{ height: itemHeight + 'px' }">
          {{ item.name }}
        </div>
      </div>
    </div>
  </div>
</template>
```

> 💡 **面试加分：** 提到"优化要有数据支撑"——用 Chrome Performance 面板记录优化前后的 FPS、DOM 节点数、JS Heap 占用，用数据证明优化效果，而不是"感觉变快了"。

---

### Q12：项目中遇到"数据变了但视图没更新"，你是怎么排查和解决的？

**面试官意图：** 考察对 Vue 3 响应式系统的理解深度，以及排查问题的能力。

**完美解答：**

Vue 3 使用 Proxy 已经解决了 Vue 2 的大部分"视图不更新"问题，但依然有一些典型场景会导致视图不更新。

**常见原因排查及解决方案：**

| 场景 | 原因 | 解决方案 |
|------|------|----------|
| 给 `ref` 或 `reactive` 对象直接赋值新属性 | 对象被替换为普通对象，失去了响应式 | 不要直接替换整个对象，修改属性而不是替换引用 |
| 修改了 `reactive` 对象的深层属性 | Proxy 可以深度拦截，但仍需确保对象始终是响应式的 | 初始化时声明完整的深层结构 |
| `ref` 在模板中直接当做对象使用 | 模板中 `ref` 会自动解包，但在函数中需要 `.value` | 检查是否遗漏了 `.value` |
| `v-for` 遍历的数组项直接修改 | 数组项是对象时修改属性会被 Proxy 捕获 | 可以正常修改，但如果替换整个数组项需要确认响应式 |
| `async/await` 后响应式丢失 | `await` 之后的变量如果被解构赋值，会丢失响应式 | 保持引用不要解构，或者用 `ref` 包裹 |

**典型场景示例：**

```javascript
// ❌ 场景1：reactive 对象被整个替换
let data = reactive({ list: [] })
data = { list: [1, 2, 3] }       // data 变成了普通对象
// ✅ 正确做法
data.list = [1, 2, 3]

// ❌ 场景2：解构 reactive 丢失响应式
const state = reactive({ count: 0, name: 'vue' })
const { count, name } = state     // count 和 name 是普通值
// ✅ 正确做法：用 toRefs 保持响应式
const { count, name } = toRefs(state)
// 或者直接使用 state.count

// ❌ 场景3：ref 忘记 .value
const count = ref(0)
console.log(count)                // RefImpl 对象，不是 0
// ✅ 正确做法
console.log(count.value)

// ⚠️ 场景4：未被初始化的属性不响应
const obj = reactive({ name: 'Vue' })
obj.age = 3                       // Vue 3 Proxy 可以检测新增属性 ✅
// 但如果在模板中使用了 obj.hobby?.name，而 hobby 未初始化，不会报错但也不会更新
// ✅ 最好初始化完整结构
```

**排查步骤：**

1. **打开 Vue Devtools** —— 查看数据是否真的变了。变了 → 模板问题；没变 → 代码问题。
2. **检查控制台** —— Vue 3 在开发模式下会 warn 响应式相关的错误。
3. **确认是否使用了 `reactive` 或 `ref`** —— 如果用普通变量声明状态，当然不会响应。
4. **检查是否在 `setup` / `<script setup>` 中正确返回了变量** —— 没 return 模板拿不到。
5. **使用 `watch` 或 `watchEffect` 打印最新值** —— 确认数据变化事件是否触发了。
6. **检查模板引用方式** —— `{{ obj[动态key] }}` 这种写法确保 key 也是响应式的。

> 💡 **面试加分：** 提到"先看 Devtools 再猜原因"的排查思路——很多时候开发者靠感觉猜问题，但 Vue Devtools 直接显示响应式数据当前值和来源，是最快的排查工具。

---

## 💎 面试加分金句

- **"前端权限控制只是体验层面的，真正的安全需要在后端做"** —— 这句话体现了你的安全意识，面试官很看重这一点。
- **"组件化不是拆得越细越好，而是看复用边界和职责清晰度"** —— 体现工程化和架构思维，不是只会照搬设计模式。
- **"优化要有数据支撑，用 Performance 面板的 FPS 和 JS Heap 数据说话"** —— 不是凭感觉优化，而是数据驱动。
- **"Pinia 是 Vue 3 的状态管理首选，不仅因为它是官方出品，更因为它天然支持 TypeScript 和组合式 API"** —— 展示你的技术选型能力。
- **"流式输出 + 打字机效果的用户体验优化，核心在于更新频率的控制和保持 60fps 的流畅滚动"** —— AI 方向面试的专属加分项。

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| **你做过哪些性能优化？具体数据是什么？** | 举一个具体项目案例，说出优化前后的 FPS、加载时间、DOM 节点数对比 |
| **你的组件设计原则是什么？** | 单一职责、高内聚低耦合、props 尽量简单、不直接修改 props |
| **Vue 3 和 React 你怎么选？** | 看团队和技术栈；Vue 上手快、模板语法更接近 HTML；React 生态更灵活、Hooks 模式更流行 |
| **权限系统前后端是怎么对账的？** | 后端是权限的最终裁决者，前端只做体验层过滤；前后端权限码保持一致 |
| **你遇到最难的前端 Bug 是什么？** | 提前准备一个真实案例，讲清楚现象→排查过程→根因→修复方案→后续预防 |
| **Axios 二次封装遇到过什么问题？** | Token 并发刷新竞态——使用 pending 队列避免同时发起多个刷新请求 |
| **如果让你带一个前端新人，你怎么培养？** | 从小到大：先 TodoList 练基础，再后台管理系统练工程化，最后权限系统练体系化思维 |

## 🔗 关联知识点

- **Vue 官方文档：** [vuejs.org](https://vuejs.org/) —— 组合式 API 和 `<script setup>` 是必读内容
- **Pinia 文档：** [pinia.vuejs.org](https://pinia.vuejs.org/) —— 理解 actions/getters/state 三要素
- **Vite 文档：** [vitejs.dev](https://vitejs.dev/) —— 环境变量配置、代理配置
- **Element Plus：** [element-plus.org](https://element-plus.org/) —— 表单校验、表格分页、权限树组件
- **VueUse：** [vueuse.org](https://vueuse.org/) —— 常用组合式工具库，面试提到用过其中几个 hook 加分
- **项目技术主线：** Vue 3 + Vite + TypeScript + Vue Router + Pinia + Axios + Element Plus
- **简历标准规范：** 目录规范（api/views/components/stores/router/utils/types）+ ESLint/Prettier + 多环境配置 + GitHub 标准 README + 项目截图 + 部署说明
