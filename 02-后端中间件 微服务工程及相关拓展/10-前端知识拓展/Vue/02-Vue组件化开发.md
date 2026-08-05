# 02 - Vue 组件化开发

> 定位：组件通信全体系、插槽、生命周期、动态组件与异步组件——组件化是 Vue 复用的核心

## 📚 目录

1. [组件基础](#1-组件基础)
2. [组件通信全景](#2-组件通信全景)
3. [插槽 Slots](#3-插槽-slots)
4. [生命周期](#4-生命周期)
5. [动态组件与异步组件](#5-动态组件与异步组件)

---

## 1. 组件基础

```vue
<!-- Child.vue：单文件组件（SFC） -->
<template>
    <div class="child">
        <p>{{ title }}</p>
        <slot></slot>                    <!-- 插槽：父传入内容 -->
    </div>
</template>

<script setup>
// ⚠️ 声明 props（只读，不可修改）
const props = defineProps({
    title: { type: String, required: true },
    count: { type: Number, default: 0 },
});

// 声明事件（emit）
const emit = defineEmits(['update', 'delete']);
const handleClick = () => emit('update', '新数据');
</script>

<style scoped>
.child { border: 1px solid #ccc; }
</style>
```

```vue
<!-- Parent.vue：使用 -->
<template>
    <Child title="标题" :count="n" @update="onUpdate">
        这是插槽内容
    </Child>
</template>
```

> 🎯 **要点**：SFC 三块（template/script/style）+ props（父传子，只读）+ emit（子传父事件）。`<script setup>` 是 Vue 3 标准写法。

---

## 2. 组件通信全景

### 2.1 通信方式总览

| 方式 | 方向 | 场景 |
|------|:---:|------|
| props | 父 → 子 | 传递数据（主要） |
| emit | 子 → 父 | 触发事件（主要） |
| v-model | 双向 | 表单类组件 |
| provide/inject | 祖先 → 后代 | 跨层级（深层） |
| defineExpose | 子 → 父 | 父调用子方法 |
| Pinia | 任意 | 全局共享状态 |

### 2.2 props / emit 完整示例

```vue
<!-- Counter.vue：自定义 v-model 组件（双向绑定核心模式） -->
<script setup>
const props = defineProps({
    modelValue: { type: Number, default: 0 },   // ⚠️ v-model 约定名
});
const emit = defineEmits(['update:modelValue']);

const increment = () =>
    emit('update:modelValue', props.modelValue + 1);
</script>

<template>
    <button @click="increment">{{ modelValue }}</button>
</template>
```

```vue
<!-- 父组件使用：v-model 语法糖 -->
<Counter v-model="count" />
<!-- 等价于：:model-value="count" @update:model-value="count = $event" -->

<!-- 多参数 v-model（Vue 3.3+） -->
<UserForm v-model:name="name" v-model:age="age" />
```

### 2.3 provide / inject（跨层级）

```vue
<!-- 祖先组件：提供 -->
<script setup>
import { provide } from 'vue';
provide('theme', 'dark');                    // 键 + 值
provide('userStore', store);                 // 可传响应式
</script>

<!-- 后代组件：注入 -->
<script setup>
import { inject } from 'vue';
const theme = inject('theme', 'light');      // 默认值
const userStore = inject('userStore');
</script>

<!-- ⚠️ 适用：深层嵌套（3 层以上）避免 props 逐层传递 -->
<!-- 对比 Pinia：全局共享用 Pinia，局部跨层用 provide/inject -->
```

### 2.4 defineExpose（父调子方法）

```vue
<!-- 子组件 -->
<script setup>
const innerMethod = () => '子组件方法';
defineExpose({ innerMethod });               // ⚠️ 显式暴露
</script>

<!-- 父组件 -->
<script setup>
import { ref } from 'vue';
const childRef = ref(null);
// 调用：childRef.value?.innerMethod()
</script>
```

---

## 3. 插槽 Slots

### 3.1 基础插槽

```vue
<!-- Card.vue：默认插槽 -->
<template>
    <div class="card">
        <header><slot name="title">默认标题</slot></header>
        <main><slot></slot></main>               <!-- 默认插槽 -->
        <footer><slot name="footer"></slot></footer>
    </div>
</template>

<!-- 使用 -->
<Card>
    <template #title>我的卡片</template>         <!-- 具名插槽 -->
    <p>主体内容</p>                              <!-- 默认插槽 -->
    <template #footer>底部</template>
</Card>
```

### 3.2 作用域插槽（子传数据给插槽）

```vue
<!-- List.vue：作用域插槽（子传数据给父的插槽内容） -->
<template>
    <ul>
        <li v-for="item in items" :key="item.id">
            <slot name="item" :item="item" :index="index">
                {{ item.name }}                  <!-- 默认内容 -->
            </slot>
        </li>
    </ul>
</template>

<!-- 父组件：定制渲染 -->
<List :items="items">
    <template #item="{ item, index }">          <!-- ⚠️ 解构插槽 props -->
        <strong>{{ index + 1 }}. {{ item.name }}</strong>
    </template>
</List>
```

> 🎯 **要点**：插槽 = 组件的内容占位。具名插槽（#title）+ 作用域插槽（子传数据给父渲染）= 组件复用的高阶形态（表格/列表/布局组件必备）。

---

## 4. 生命周期

### 4.1 生命周期钩子（组合式 API）

```
创建阶段：
  setup()（本身）
  onBeforeMount → onMounted

更新阶段：
  onBeforeUpdate → onUpdated

卸载阶段：
  onBeforeUnmount → onUnmounted

调试/缓存：
  onActivated / onDeactivated（KeepAlive）
  onErrorCaptured（错误捕获）
```

```vue
<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue';

const timer = ref(null);

onMounted(() => {
    // ✅ 挂载后：DOM 可访问、发起请求、启动定时器
    timer.value = setInterval(() => { }, 1000);
});

onBeforeUnmount(() => {
    // ⚠️ 卸载前：清理定时器/事件监听/连接（防内存泄漏）
    clearInterval(timer.value);
});
</script>
```

### 4.2 父子生命周期顺序

```
创建：父 setup → 子 setup → 子 mounted → 父 mounted
更新：父 beforeUpdate → 子 beforeUpdate → 子 updated → 父 updated
卸载：父 beforeUnmount → 子 beforeUnmount → 子 unmounted → 父 unmounted

⚠️ 面试必答：
"子先挂载（mounted 内到子）、
 父后卸载（子先清理）——
 这是父组件等待子组件就绪的保证。"
```

---

## 5. 动态组件与异步组件

### 5.1 动态组件

```vue
<script setup>
import { ref } from 'vue';
import TabA from './TabA.vue';
import TabB from './TabB.vue';

const currentTab = ref('TabA');
const tabs = { TabA, TabB };
</script>

<template>
    <!-- ⚠️ :is 动态切换组件 -->
    <component :is="tabs[currentTab]"></component>

    <!-- KeepAlive：缓存组件状态（切换不销毁） -->
    <KeepAlive>
        <component :is="tabs[currentTab]"></component>
    </KeepAlive>
</template>
```

### 5.2 异步组件（按需加载）

```vue
<script setup>
import { defineAsyncComponent } from 'vue';

// ⚠️ 懒加载：首次渲染才请求（路由/大组件必用）
const HeavyChart = defineAsyncComponent(() =>
    import('./HeavyChart.vue')
);

// 加载状态定制
const Report = defineAsyncComponent({
    loader: () => import('./Report.vue'),
    loadingComponent: Spinner,           // 加载中
    delay: 200,
    errorComponent: ErrorBox,            // 加载失败
    timeout: 3000,
});
</script>

<template>
    <HeavyChart />
</template>
```

> 🎯 **要点**：动态组件（`:is`）+ KeepAlive（缓存状态）是 Tab 页签/多视图切换的标准方案；异步组件（defineAsyncComponent）是包体积优化的核心手段。

---

> 🎯 **核心要点**：组件化体系 = **通信**（props/emit 为主，provide/inject 跨层，v-model 双向，Pinia 全局）+ **插槽**（具名 + 作用域定制渲染）+ **生命周期**（挂载清理对称，防泄漏）+ **动态/异步组件**（切换缓存 + 按需加载）。组件通信方式是 Vue 面试第一大题。

---

**返回总览**：[00-Vue总览与核心概念](00-Vue总览与核心概念.md) | **上一篇**：[01-Vue模板与指令](01-Vue模板与指令.md) | **下一篇**：[03-Vue响应式原理](03-Vue响应式原理.md)
