# 04 - Vue 组合式 API 与进阶

> 定位：组合式 API 全解（setup/computed/watch 进阶）、自定义指令、Teleport、Suspense、组合式函数（Composables）

## 📚 目录

1. [组合式 API vs 选项式 API](#1-组合式-api-vs-选项式-api)
2. [组合式函数 Composables](#2-组合式函数-composables)
3. [watch 与 computed 进阶](#3-watch-与-computed-进阶)
4. [自定义指令](#4-自定义指令)
5. [Teleport 与 Suspense](#5-teleport-与-suspense)
6. [性能优化进阶](#6-性能优化进阶)

---

## 1. 组合式 API vs 选项式 API

### 1.1 对比

```javascript
// 选项式（Vue 2 风格，Vue 3 仍支持）
export default {
    data() { return { count: 0 }; },
    computed: { double() { return this.count * 2; } },
    methods: { increment() { this.count++; } },
    watch: { count(n) { console.log(n); } },
    mounted() { console.log('mounted'); },
};

// 组合式（Vue 3 推荐）
<script setup>
import { ref, computed, watch, onMounted } from 'vue';
const count = ref(0);
const double = computed(() => count.value * 2);
const increment = () => count.value++;
watch(count, (n) => console.log(n));
onMounted(() => console.log('mounted'));
</script>
```

### 1.2 组合式的优势

| 优势 | 说明 |
|------|------|
| 逻辑复用 | 抽取为 composables（替代 mixin） |
| 类型推导 | TS 友好 |
| 按需组织 | 相关逻辑集中（vs 选项散落） |
| 更好的 tree-shaking | 按需导入 API |

> 🎯 **要点**：组合式 = "按逻辑组织"而非"按选项组织"——同一功能的 data/computed/methods 聚合在一起，可整体抽取复用。

---

## 2. 组合式函数 Composables

### 2.1 什么是 Composables

```javascript
// ⚠️ Composables = 以 use 开头的组合式函数（Vue 3 复用之王）
// useMouse.js
import { ref, onMounted, onBeforeUnmount } from 'vue';

export function useMouse() {
    const x = ref(0);
    const y = ref(0);

    function update(e) {
        x.value = e.clientX;
        y.value = e.clientY;
    }
    onMounted(() => window.addEventListener('mousemove', update));
    onBeforeUnmount(() => window.removeEventListener('mousemove', update));

    return { x, y };                       // ⚠️ 返回响应式状态
}
```

```vue
<!-- 使用：任意组件复用 -->
<script setup>
import { useMouse } from './useMouse';
import { useDebounce } from './useDebounce';

const { x, y } = useMouse();               // 复用鼠标追踪
const keyword = useDebounce('搜索词', 300); // 复用防抖逻辑
</script>
```

### 2.2 Composables vs Mixins

| 维度 | Mixins（Vue 2） | Composables |
|------|:---:|:---:|
| 命名冲突 | ❌ 属性合并冲突 | ✅ 解构重命名 |
| 数据来源 | 隐式（this.xxx） | 显式（返回值） |
| 类型推导 | ❌ 弱 | ✅ 强 |
| 组合嵌套 | ❌ | ✅（composable 调 composable） |

> 🎯 **要点**：Composables 是 Vue 3 的复用标准（替代 mixin）——"use 前缀 + 返回响应式状态 + 内部管理生命周期"是黄金模板。

---

## 3. watch 与 computed 进阶

### 3.1 watchEffect 与 watchPostFlush

```javascript
import { ref, watchEffect, watch } from 'vue';

const id = ref(1);
const data = ref(null);

// watchEffect：自动收集依赖（无需显式指定）
watchEffect(async () => {
    // ⚠️ 内部读取的响应式数据变化都会重新执行
    data.value = await fetchData(id.value);
});
// 对比 watch：watch 需要显式指定监听源

// 选项：flush（执行时机）
watchEffect(callback, { flush: 'post' });       // DOM 更新后（默认 pre）
watchEffect(callback, { flush: 'sync' });       // 同步（慎用）
```

### 3.2 watch 高级选项

```javascript
const state = reactive({ user: { profile: { age: 25 } } });

// 深层侦听（对象内部变化）
watch(state, cb, { deep: true });

// 多个源
watch([count, name], ([newC, newN], [oldC, oldN]) => { });

// getter 形式（只侦听属性路径）
watch(() => state.user.profile.age, cb);

// 清理副作用（防竞态：快速切换时旧请求作废）
watch(id, async (newId, oldId, onCleanup) => {
    let cancelled = false;
    onCleanup(() => cancelled = true);      // ⚠️ 下次触发前执行
    const result = await fetchData(newId);
    if (!cancelled) data.value = result;    // 竞态保护
});
```

---

## 4. 自定义指令

```javascript
// ⚠️ 自定义指令：复用 DOM 操作（v-focus/v-click-outside 等）
// directives/focus.js
export const vFocus = {
    mounted(el) {                            // 挂载时
        el.focus();
    },
};

// directives/clickOutside.js
export const vClickOutside = {
    mounted(el, binding) {
        el._clickOutside = (e) => {
            if (!el.contains(e.target)) {
                binding.value(e);            // 点击外部回调
            }
        };
        document.addEventListener('click', el._clickOutside);
    },
    unmounted(el) {
        document.removeEventListener('click', el._clickOutside);  // ⚠️ 清理
    },
};
```

```vue
<script setup>
import { vFocus, vClickOutside } from './directives';
import { ref } from 'vue';

const open = ref(true);
</script>

<template>
    <input v-focus />                                  <!-- 自动聚焦 -->
    <div v-click-outside="() => open = false">弹层</div>
</template>
```

> 🎯 **要点**：自定义指令钩子（mounted/updated/unmounted）+ 卸载清理是标准模板。指令适合"纯 DOM 操作"（聚焦/滚动/点击外部），逻辑复用用 composables。

---

## 5. Teleport 与 Suspense

### 5.1 Teleport（传送门）

```vue
<!-- ⚠️ Teleport：把内容渲染到指定 DOM 位置（脱离组件树） -->
<!-- 解决：弹窗/遮罩被父组件 overflow/层级遮挡的问题 -->
<template>
    <Teleport to="body">
        <div class="modal">
            弹窗内容——渲染到 body 下
        </div>
    </Teleport>

    <!-- 条件传送（移动端/桌面端不同容器） -->
    <Teleport :to="isMobile ? '#mobile-root' : '#desktop-root'">
        <Sidebar />
    </Teleport>
</template>
```

### 5.2 Suspense（异步组件等待）

```vue
<!-- ⚠️ Suspense：异步组件/异步 setup 的等待占位 -->
<template>
    <Suspense>
        <template #default>
            <AsyncComponent />            <!-- 异步组件 -->
        </template>
        <template #fallback>
            <Loading />                   <!-- 等待时的占位 -->
        </template>
    </Suspense>
</template>
```

| 特性 | 作用 |
|------|------|
| Teleport | 渲染位置传送（弹窗/悬浮层） |
| Suspense | 异步依赖的等待界面（嵌套异步组件统一 loading） |
| KeepAlive | 组件状态缓存（前面已讲） |

---

## 6. 性能优化进阶

### 6.1 渲染优化

```vue
<script setup>
import { shallowRef, markRaw } from 'vue';

// ① 浅响应（大数据不需要深层响应时）
const bigList = shallowRef([]);          // ⚠️ 数组内部不响应（替换整体才更新）

// ② markRaw：跳过响应式（静态大对象/第三方实例）
const chartInstance = markRaw(new Chart());   // 不代理（省开销）

// ③ v-memo：记忆渲染（复杂列表）
// <li v-for="item in list" v-memo="[item.id, item.checked]">
//   只有 id/checked 变化才重新渲染该节点

// ④ 虚拟列表（长列表）：只渲染可视区域（第三方 vue-virtual-scroller）
</script>
```

### 6.2 工程优化清单

| 优化项 | 手段 |
|--------|------|
| 包体积 | 异步组件 + 路由懒加载 + 按需引入 UI 库 |
| 首屏 | 骨架屏 + 懒加载图片 |
| 长列表 | 虚拟列表 |
| 频繁更新 | 防抖/节流 + shallowRef |
| 静态内容 | v-once + v-memo |
| 网络 | 缓存 + 请求合并 |

> 🎯 **要点**：性能优化分层——**渲染层**（shallowRef/markRaw/v-memo）、**加载层**（异步组件/路由懒加载）、**交互层**（防抖节流）。面试答"从这三层优化"即完整。

---

> 🎯 **核心要点**：组合式进阶 = **Composables**（use 函数复用，替代 mixin）+ **watch 进阶**（watchEffect/竞态清理）+ **自定义指令**（DOM 操作 + 卸载清理）+ **Teleport/Suspense**（传送门/异步等待）+ **性能优化**（shallowRef/异步组件/v-memo）。这五块是 Vue 3 从"会用"到"进阶"的分水岭。

---

**返回总览**：[00-Vue总览与核心概念](00-Vue总览与核心概念.md) | **上一篇**：[03-Vue响应式原理](03-Vue响应式原理.md) | **下一篇**：[05-Vue路由状态与工程实践](05-Vue路由状态与工程实践.md)
