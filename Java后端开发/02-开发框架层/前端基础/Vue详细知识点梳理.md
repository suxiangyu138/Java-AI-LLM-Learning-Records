# Vue 详细知识点梳理

> **文档定位**：Java 后端技术参考文档 | Vue.js 核心知识点全景  
> **核心说明**：Vue 是一套用于构建用户界面的渐进式 JavaScript 框架，基于 MVVM 架构模式，实现数据与视图的双向绑定  
> **版本说明**：本文同时覆盖 Vue 2 和 Vue 3，重点标注两者差异

---

## 目录

- [一、Vue 简介](#一vue-简介)
- [二、Vue 基础](#二vue-基础)
- [三、Vue 核心特性](#三vue-核心特性)
- [四、Vue 进阶特性](#四vue-进阶特性)
- [五、Vue2 与 Vue3 差异速查表](#五vue2-与-vue3-差异速查表)
- [六、总结](#六总结)

---

## 一、Vue 简介

### 1.1 什么是 Vue

Vue（读音 /vjuː/，类似于 view）是一套用于构建用户界面的**渐进式 JavaScript 框架**。与其他重量级框架不同，Vue 被设计为可以**自底向上逐层应用**——核心只关注视图层，易于上手，同时可以与现有项目整合，也能支持复杂的单页应用（SPA）开发。

**Vue 的核心特点**：易用性、灵活性、高效性，基于 **MVVM（Model-View-ViewModel）** 架构模式。

### 1.2 Vue 2 vs Vue 3

| 对比维度 | Vue 2 | Vue 3 |
|----------|-------|-------|
| **核心架构** | Options API（`data`、`methods`、`computed`） | Composition API（`setup`、响应式 API），兼容 Options API |
| **响应式原理** | `Object.defineProperty`（数组/对象新增属性无法响应） | `Proxy + Reflect`（完美支持所有操作） |
| **性能** | 虚拟 DOM 更新效率一般 | 虚拟 DOM 重写，Tree-Shaking 优化，打包体积更小 |
| **TypeScript** | 支持有限 | 原生支持 |
| **其他特性** | 生命周期钩子固定 | 新增 Teleport、Suspense、Fragment |

---

## 二、Vue 基础

### 2.1 环境搭建

#### CDN 引入（快速测试）

```html
<!-- Vue 2 -->
<script src="https://cdn.jsdelivr.net/npm/vue@2.7.14/dist/vue.js"></script>

<!-- Vue 3 -->
<script src="https://cdn.jsdelivr.net/npm/vue@3.4.21/dist/vue.global.js"></script>
```

#### Vue CLI（工程化开发）

```bash
# 安装 Vue CLI
npm install -g @vue/cli

# 创建 Vue 2 项目
vue create 项目名

# 创建 Vue 3 项目
vue create 项目名 -b vue@next

# 启动项目
cd 项目名 && npm run serve
```

#### Vite（Vue 3 推荐）

```bash
# 创建 Vue 3 项目
npm create vite@latest 项目名 -- --template vue

# 安装依赖并启动
cd 项目名 && npm install && npm run dev
```

### 2.2 实例与挂载

#### Vue 2 实例

```javascript
const vm = new Vue({
  el: '#app',           // 挂载点（CSS 选择器）
  data: {               // 数据中心（响应式）
    message: 'Hello Vue!'
  }
})
// 或手动挂载：vm.$mount('#app')
```

#### Vue 3 实例

```javascript
const { createApp } = Vue

const app = createApp({
  data() {              // Vue 3 中 data 必须是函数
    return {
      message: 'Hello Vue3!'
    }
  }
})

app.mount('#app')
```

### 2.3 模板语法

#### 插值表达式（Mustache 语法）

```html
<div id="app">
  <!-- 渲染文本 -->
  <p>{{ message }}</p>
  
  <!-- 简单表达式 -->
  <p>1 + 1 = {{ 1 + 1 }}</p>
  <p>{{ age > 18 ? '成年' : '未成年' }}</p>
</div>
```

#### 核心指令速查

| 指令 | 缩写 | 作用 | 示例 |
|------|------|------|------|
| `v-text` | — | 渲染文本 | `<p v-text="message"></p>` |
| `v-html` | — | 渲染 HTML（慎防 XSS） | `<div v-html="htmlContent"></div>` |
| `v-bind` | `:` | 动态绑定属性 | `<img :src="imgUrl">` |
| `v-on` | `@` | 事件监听 | `<button @click="handleClick">` |
| `v-model` | — | 双向数据绑定 | `<input v-model="message">` |
| `v-if` | — | 条件渲染（销毁/创建） | `<div v-if="isShow">显示</div>` |
| `v-show` | — | 条件渲染（隐藏/显示） | `<div v-show="isShow">显示</div>` |
| `v-for` | — | 列表渲染 | `<li v-for="item in list" :key="item.id">` |

#### v-bind 详解

```html
<!-- 基础绑定 -->
<img :src="imgUrl" alt="">

<!-- 绑定 class（对象语法） -->
<div :class="{ active: isActive, 'text-danger': hasError }"></div>

<!-- 绑定 class（数组语法） -->
<div :class="[activeClass, errorClass]"></div>

<!-- 绑定 style -->
<div :style="{ color: textColor, fontSize: fontSize + 'px' }"></div>
```

#### v-on 详解

```html
<!-- 基础用法 -->
<button @click="handleClick">点击</button>
<button @click="handleClick(123, $event)">传参</button>

<!-- 事件修饰符 -->
<button @click.stop="handleClick">阻止冒泡</button>
<button @click.prevent="handleSubmit">阻止默认行为</button>
<button @click.once="handleClick">只触发一次</button>

<!-- 按键修饰符 -->
<input @keyup.enter="handleEnter" placeholder="按回车提交">
```

#### v-if vs v-show

| 特性 | v-if | v-show |
|------|------|--------|
| **原理** | 销毁 / 创建 DOM | 隐藏 / 显示（`display: none`） |
| **初始渲染成本** | 低（条件 false 时不渲染） | 高（总是渲染） |
| **切换成本** | 高（销毁重建） | 低（仅切换 display） |
| **适用场景** | 条件很少改变 | 条件频繁切换 |

#### v-for 详解

```html
<!-- 循环数组 -->
<ul>
  <li v-for="(item, index) in list" :key="item.id">
    {{ index + 1 }}. {{ item.name }}
  </li>
</ul>

<!-- 循环对象 -->
<div v-for="(value, key, index) in user" :key="key">
  {{ index }}. {{ key }}: {{ value }}
</div>

<!-- 循环数字（1-10） -->
<option v-for="n in 10" :key="n">{{ n }}</option>
```

> **重要**：`v-for` 必须配合 `:key`，且 key 值必须唯一（推荐使用 id，避免使用 index）。

---

## 三、Vue 核心特性

### 3.1 响应式原理

#### Vue 2 响应式（Object.defineProperty）

```javascript
// 简化版原理
function defineReactive(obj, key, value) {
  Object.defineProperty(obj, key, {
    get() {
      // 收集依赖
      return value
    },
    set(newValue) {
      if (newValue !== value) {
        value = newValue
        // 通知视图更新
        updateView()
      }
    }
  })
}
```

**Vue 2 响应式的局限性**：

| 问题 | 示例 | 解决方案 |
|------|------|----------|
| 数组下标修改 | `arr[0] = 1` | 使用变异方法：`push/pop/shift/unshift/splice/sort/reverse` |
| 修改数组长度 | `arr.length = 0` | 使用 `splice` |
| 新增对象属性 | `obj.newKey = 'value'` | `Vue.set(obj, 'newKey', 'value')` |
| 删除对象属性 | `delete obj.key` | `Vue.delete(obj, 'key')` |

#### Vue 3 响应式（Proxy + Reflect）

```javascript
// 简化版原理
function reactive(obj) {
  return new Proxy(obj, {
    get(target, key, receiver) {
      const value = Reflect.get(target, key, receiver)
      track(target, key)  // 收集依赖
      return typeof value === 'object' && value !== null 
        ? reactive(value) : value  // 递归代理嵌套对象
    },
    set(target, key, newValue, receiver) {
      const oldValue = Reflect.get(target, key, receiver)
      if (newValue !== oldValue) {
        Reflect.set(target, key, newValue, receiver)
        trigger(target, key)  // 通知视图更新
      }
      return true
    },
    deleteProperty(target, key) {
      const hasKey = Reflect.has(target, key)
      const result = Reflect.deleteProperty(target, key)
      if (hasKey) { trigger(target, key) }
      return result
    }
  })
}
```

> **Vue 3 优势**：无需手动处理数组/对象操作，自动递归代理，性能更优，原生支持 TypeScript。

### 3.2 组件化开发

#### 组件注册

**Vue 2**：

```javascript
// 全局注册
Vue.component('MyComponent', {
  template: '<div>全局组件</div>',
  data() { return { count: 0 } }
})

// 局部注册
new Vue({
  el: '#app',
  components: {
    LocalComponent: { template: '<div>局部组件</div>' }
  }
})
```

**Vue 3**：

```javascript
const app = createApp({})

// 全局注册
app.component('MyComponent', {
  template: '<div>全局组件</div>',
  data() { return { count: 0 } }
})

// 局部注册
const LocalComponent = { template: '<div>局部组件</div>' }
app.component('Parent', {
  components: { LocalComponent }
})
```

#### 组件通信方式

| 通信方式 | 适用场景 | 方向 |
|----------|----------|------|
| `props` + `$emit` | 父子组件 | 父→子（props）、子→父（emit） |
| `provide` / `inject` | 祖孙组件（跨级） | 祖先→后代 |
| EventBus / mitt | 兄弟组件 | 双向 |
| Vuex / Pinia | 大型项目全局状态 | 双向 |
| `$refs` | 父访问子实例 | 父→子 |

### 3.3 生命周期

#### Vue 2 生命周期

| 钩子 | 时机 | 常用操作 |
|------|------|----------|
| `beforeCreate` | 实例创建前 | 无（data/methods 不可用） |
| `created` | 实例创建完成 | 数据初始化、请求接口 |
| `beforeMount` | 挂载前 | 最后一次修改数据 |
| `mounted` | 挂载完成 | DOM 操作、初始化第三方库 |
| `beforeUpdate` | 数据更新前 | 获取更新前的 DOM 状态 |
| `updated` | 数据更新后 | DOM 操作（谨慎修改数据） |
| `beforeDestroy` | 销毁前 | 清理定时器、解绑事件 |
| `destroyed` | 销毁完成 | 资源释放 |

#### Vue 3 生命周期（Composition API）

```javascript
import { 
  onBeforeMount, onMounted, 
  onBeforeUpdate, onUpdated,
  onBeforeUnmount, onUnmounted 
} from 'vue'

export default {
  setup() {
    // setup 相当于 beforeCreate + created
    onBeforeMount(() => { /* 挂载前 */ })
    onMounted(() => { /* DOM 已挂载 */ })
    onBeforeUpdate(() => { /* 更新前 */ })
    onUpdated(() => { /* 更新后 */ })
    onBeforeUnmount(() => { /* 销毁前 */ })
    onUnmounted(() => { /* 销毁后 */ })
  }
}
```

---

## 四、Vue 进阶特性

### 4.1 计算属性（computed）

**特点**：基于依赖缓存，依赖不变时不重新计算。

```javascript
// Vue 3 写法
import { ref, computed } from 'vue'

export default {
  setup() {
    const firstName = ref('张')
    const lastName = ref('三')
    
    // 只读
    const fullName = computed(() => firstName.value + lastName.value)
    
    // 可读写
    const fullName2 = computed({
      get: () => firstName.value + lastName.value,
      set: (newVal) => {
        const arr = newVal.split('')
        firstName.value = arr[0]
        lastName.value = arr.slice(1).join('')
      }
    })
    
    return { firstName, lastName, fullName }
  }
}
```

**computed vs methods**：

| 特性 | computed | methods |
|------|----------|---------|
| **缓存** | 有（依赖不变则取缓存） | 无 |
| **性能** | 高（避免重复计算） | 低（每次调用都计算） |
| **适用场景** | 依赖数据的计算 | 事件处理、无缓存需求 |

### 4.2 侦听器（watch）

```javascript
// Vue 3 写法
import { ref, reactive, watch } from 'vue'

export default {
  setup() {
    const message = ref('Hello')
    const user = reactive({ name: '张三', age: 20 })
    
    // 监听 ref
    watch(message, (newVal, oldVal) => {
      console.log('变化:', newVal, oldVal)
    })
    
    // 监听 reactive（自动深度监听）
    watch(user, (newVal) => {
      console.log('user 变化:', newVal)
    })
    
    // 监听对象属性
    watch(() => user.name, (newVal) => {
      console.log('name 变化:', newVal)
    })
    
    // 监听多个
    watch([message, () => user.age], ([newMsg, newAge]) => {
      console.log('message 或 age 变化')
    })
  }
}
```

#### watchEffect（Vue 3 新增）

```javascript
import { ref, watchEffect } from 'vue'

const message = ref('Hello')
const count = ref(0)

// 自动收集依赖，依赖变化时自动执行
const stop = watchEffect(() => {
  console.log('依赖变化:', message.value, count.value)
})
// 停止监听：stop()
```

### 4.3 插槽（Slot）

#### 匿名插槽

```html
<!-- 子组件 -->
<template>
  <div><slot>默认内容</slot></div>
</template>

<!-- 父组件 -->
<Child><p>插入到插槽的内容</p></Child>
```

#### 具名插槽

```html
<!-- 子组件 -->
<template>
  <slot name="header">默认头部</slot>
  <slot name="content">默认内容</slot>
  <slot name="footer">默认底部</slot>
</template>

<!-- 父组件 -->
<Child>
  <template v-slot:header>头部内容</template>
  <template #content>内容区域</template>     <!-- 缩写 -->
  <template #footer>底部内容</template>
</Child>
```

#### 作用域插槽

```html
<!-- 子组件（传递数据给父组件） -->
<template>
  <slot :user="user" :count="count"></slot>
</template>

<!-- 父组件 -->
<Child>
  <template v-slot:default="{ user, count }">
    <p>用户名：{{ user.name }}，数量：{{ count }}</p>
  </template>
</Child>
```

---

## 五、Vue2 与 Vue3 差异速查表

| 项目 | Vue 2 | Vue 3 |
|------|-------|-------|
| **data** | 对象或函数 | 必须是函数 |
| **响应式** | `Object.defineProperty` | `Proxy` |
| **API 风格** | Options API | Composition API（兼容 Options） |
| **TypeScript** | 支持有限 | 原生支持 |
| **生命周期** | `beforeDestroy` / `destroyed` | `beforeUnmount` / `unmounted` |
| **事件总线** | `new Vue()` | mitt 库 |
| **过滤器** | 支持 | 移除（用 computed/methods 替代） |
| **多根节点** | 不支持 | 支持（Fragment） |
| **Teleport** | 无 | 有（传送 DOM 节点） |
| **Suspense** | 无 | 有（异步组件） |
| **Tree-Shaking** | 较差 | 优秀 |

---

## 六、总结

| 核心知识点 | 要点 |
|------------|------|
| **Vue 本质** | 渐进式 JavaScript 框架，MVVM 架构 |
| **核心特性** | 响应式、组件化、双向绑定、虚拟 DOM |
| **模板语法** | 插值 `{{ }}`、指令 `v-*`、事件 `@`、属性 `:` |
| **响应式原理** | Vue 2：`Object.defineProperty`；Vue 3：`Proxy` |
| **生命周期** | 创建 → 挂载 → 更新 → 销毁 |
| **组件通信** | props/emit、provide/inject、EventBus、Vuex |
| **性能优化** | computed 缓存、key 属性、懒加载、keep-alive |
