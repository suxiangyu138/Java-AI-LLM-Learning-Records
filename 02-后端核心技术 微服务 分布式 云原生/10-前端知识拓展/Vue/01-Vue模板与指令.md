# 01 - Vue 模板与指令

> 定位：模板语法全解——插值、指令系统、事件与表单绑定、计算属性与侦听器

## 📚 目录

1. [插值与模板语法](#1-插值与模板语法)
2. [指令系统](#2-指令系统)
3. [事件绑定](#3-事件绑定)
4. [表单双向绑定](#4-表单双向绑定)
5. [计算属性与侦听器](#5-计算属性与侦听器)

---

## 1. 插值与模板语法

### 1.1 文本插值

```vue
<template>
    <!-- 插值：{{ }} 输出数据（自动转义 HTML） -->
    <p>{{ message }}</p>
    <p>{{ count + 1 }}</p>                    <!-- 表达式 -->
    <p>{{ user.name }}</p>                    <!-- 属性访问 -->
    <p>{{ isActive ? '是' : '否' }}</p>       <!-- 三元 -->

    <!-- v-text / v-html（⚠️ v-html 有 XSS 风险） -->
    <span v-text="message"></span>
    <span v-html="rawHtml"></span>            <!-- ⚠️ 不要用于用户输入 -->
</template>
```

### 1.2 属性绑定

```vue
<template>
    <!-- v-bind 简写 : -->
    <img :src="imageUrl" :alt="altText">
    <div :class="activeClass">动态类</div>
    <div :style="{ color: textColor, fontSize: size + 'px' }">动态样式</div>

    <!-- class 对象/数组语法 -->
    <div :class="{ active: isActive, 'text-danger': hasError }">对象语法</div>
    <div :class="[baseClass, isActive ? 'active' : '']">数组语法</div>

    <!-- 动态属性名 -->
    <div :[attributeName]="value"></div>
</template>
```

> 🎯 **要点**：`{{ }}` 自动转义（安全默认）；`v-html` 渲染原始 HTML（XSS 风险，用户输入禁用）；`:class` 对象/数组语法是高频写法。

---

## 2. 指令系统

### 2.1 条件渲染

```vue
<template>
    <!-- v-if：条件渲染（不满足不渲染） -->
    <p v-if="score >= 90">优秀</p>
    <p v-else-if="score >= 60">及格</p>
    <p v-else>不及格</p>

    <!-- v-show：切换 display（始终渲染） -->
    <p v-show="isVisible">显示/隐藏</p>

    <!-- ⚠️ v-if vs v-show：频繁切换用 v-show，条件少用 v-if -->
</template>
```

### 2.2 列表渲染

```vue
<template>
    <!-- v-for：列表渲染（⚠️ key 必须唯一稳定） -->
    <li v-for="(item, index) in items" :key="item.id">
        {{ index }} - {{ item.name }}
    </li>

    <!-- 对象遍历 -->
    <li v-for="(value, key) in user" :key="key">{{ key }}: {{ value }}</li>

    <!-- 数字遍历 -->
    <li v-for="n in 5" :key="n">{{ n }}</li>

    <!-- ⚠️ 不要用 index 作 key（列表排序/过滤时出 bug） -->
</template>
```

### 2.3 其他内置指令

| 指令 | 作用 | 示例 |
|------|------|------|
| v-model | 表单双向绑定 | `v-model="username"` |
| v-on（@） | 事件绑定 | `@click="handler"` |
| v-bind（:） | 属性绑定 | `:href="url"` |
| v-if / v-show | 条件渲染 | 见上 |
| v-for | 列表渲染 | 见上 |
| v-html / v-text | 内容输出 | ⚠️ v-html 慎用 |
| v-once | 只渲染一次 | 静态内容优化 |
| v-memo | 记忆渲染（Vue 3.2+） | 复杂列表性能优化 |

---

## 3. 事件绑定

### 3.1 基础用法

```vue
<template>
    <!-- 内联语句 -->
    <button @click="count++">加一</button>

    <!-- 方法调用 -->
    <button @click="handleClick">调用方法</button>
    <button @click="handleClick('参数', $event)">带参</button>

    <!-- 事件修饰符（高频） -->
    <form @submit.prevent="onSubmit">       <!-- .prevent 阻止默认 -->
    <button @click.stop="doThis">停止冒泡</button>
    <button @click.once="doOnce">只执行一次</button>
    <input @keyup.enter="search">           <!-- 按键修饰符 -->
    <div @click.self="onlySelf">只有自身点击</div>
</template>
```

### 3.2 按键与系统修饰符

```vue
<template>
    <!-- 按键修饰符 -->
    <input @keyup.enter="submit">
    <input @keyup.esc="cancel">
    <input @keyup.ctrl.enter="special">      <!-- 组合键 -->

    <!-- 系统修饰符 -->
    <div @click.ctrl="onCtrlClick">Ctrl+点击</div>

    <!-- 修饰符可组合 -->
    <button @click.prevent.stop="handle">阻止默认 + 停止冒泡</button>
</template>
```

---

## 4. 表单双向绑定

### 4.1 各控件用法

```vue
<template>
    <!-- 文本框 -->
    <input v-model="username" type="text">

    <!-- 文本域 -->
    <textarea v-model="bio"></textarea>

    <!-- 复选框（数组收集多选） -->
    <input type="checkbox" v-model="checked">
    <input type="checkbox" value="a" v-model="hobbies">
    <input type="checkbox" value="b" v-model="hobbies">

    <!-- 单选 -->
    <input type="radio" value="male" v-model="gender">
    <input type="radio" value="female" v-model="gender">

    <!-- 下拉 -->
    <select v-model="city">
        <option value="bj">北京</option>
        <option value="sh">上海</option>
    </select>
</template>
```

### 4.2 修饰符

```vue
<template>
    <!-- .number：自动转数字 -->
    <input v-model.number="age" type="number">

    <!-- .trim：去除首尾空格 -->
    <input v-model.trim="username">

    <!-- .lazy：失焦才同步（性能） -->
    <input v-model.lazy="keyword">
</template>
```

---

## 5. 计算属性与侦听器

### 5.1 computed（计算属性）

```vue
<script setup>
import { ref, computed } from 'vue';

const items = ref([{ price: 10, count: 2 }, { price: 5, count: 1 }]);

// ⚠️ 计算属性：依赖变化自动重算 + 缓存
const totalPrice = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.count, 0)
);

// 可写计算属性（少见）
const fullName = computed({
    get: () => `${firstName.value} ${lastName.value}`,
    set: (value) => { /* 拆分赋值 */ },
});
</script>

<template>
    <p>总价：{{ totalPrice }}</p>
</template>
```

> 🎯 **要点**：`computed` 有缓存（依赖不变不重算）——**模板里复杂表达式一律用 computed**；`watch` 适合副作用（异步/API 调用）。

### 5.2 watch（侦听器）

```vue
<script setup>
import { ref, watch } from 'vue';

const keyword = ref('');
const user = reactive({ name: '张三' });

// 基本侦听
watch(keyword, (newVal, oldVal) => {
    search(newVal);                    // 副作用：请求/日志
});

// 侦听对象属性（需要 getter）
watch(() => user.name, (newVal) => {
    console.log('名字变化：' + newVal);
});

// 深度侦听
watch(user, (newVal) => { }, { deep: true });

// 立即执行一次（初始化）
watch(keyword, handler, { immediate: true });

// 一次性
watch(keyword, handler, { once: true });
</script>
```

### 5.3 computed vs watch 选型

| 维度 | computed | watch |
|------|:---:|:---:|
| 用途 | 派生数据（返回新值） | 副作用（请求/操作） |
| 缓存 | ✅ 有 | ❌ |
| 模板使用 | ✅ 直接 {{ }} | ❌ |
| 触发 | 依赖变化自动 | 显式监听 |
| 选型口诀 | **能 computed 不 watch** | 需要副作用才 watch |

> 🎯 **核心要点**：模板体系 = **插值**（{{ }} 转义安全）+ **指令**（v-if/v-for/v-model/:class 高频五件套）+ **事件修饰符**（prevent/stop/enter）+ **computed**（缓存派生值）+ **watch**（副作用）。"能 computed 不 watch"是 Vue 最佳实践第一原则。

---

**返回总览**：[00-Vue总览与核心概念](00-Vue总览与核心概念.md) | **下一篇**：[02-Vue组件化开发](02-Vue组件化开发.md)
