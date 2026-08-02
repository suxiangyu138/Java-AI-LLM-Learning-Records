# 03 - Vue 响应式原理

> 定位：响应式系统的完整实现——Proxy 拦截、依赖收集与触发、ref/reactive 对比、更新流程、Vue 2 vs Vue 3

## 📚 目录

1. [响应式是什么](#1-响应式是什么)
2. [Proxy 拦截机制](#2-proxy-拦截机制)
3. [依赖收集与触发更新](#3-依赖收集与触发更新)
4. [ref / reactive / computed 实现](#4-ref--reactive--computed-实现)
5. [Vue 2 vs Vue 3 响应式](#5-vue-2-vs-vue-3-响应式)
6. [响应式更新流程](#6-响应式更新流程)

---

## 1. 响应式是什么

```
响应式 = 数据变化 → 依赖它的视图自动更新

核心机制（三要素）：
  ① 拦截：数据读写被拦截（Proxy）
  ② 收集：读取时记录"谁依赖了这个数据"（依赖收集）
  ③ 触发：写入时通知依赖更新（触发更新）

⚠️ 面试必答：
"响应式三要素——拦截（Proxy get/set）、
 收集（读取时记录 effect）、触发（写入时执行 effect）。
 数据驱动视图的本质是'读写拦截 + 依赖管理'。"
```

---

## 2. Proxy 拦截机制

### 2.1 Proxy 为什么优于 defineProperty

| 维度 | Vue 2 defineProperty | Vue 3 Proxy |
|------|:---:|:---:|
| 拦截对象 | 单个属性（需遍历） | 整个对象 |
| 新增属性 | ❌ 不响应（需 $set） | ✅ 自动响应 |
| 删除属性 | ❌ 不响应 | ✅ 自动响应 |
| 数组 | 需 hack 方法 | ✅ 原生支持 |
| 嵌套 | 递归一次性 | 惰性（访问时深层代理） |

```javascript
// Vue 3 的核心：new Proxy 拦截整个对象
function reactive(target) {
    return new Proxy(target, {
        get(target, key, receiver) {
            track(target, key);              // ① 读取 → 收集依赖
            return Reflect.get(target, key, receiver);
        },
        set(target, key, value, receiver) {
            const result = Reflect.set(target, key, value, receiver);
            trigger(target, key);            // ② 写入 → 触发更新
            return result;
        },
        deleteProperty(target, key) {
            const result = Reflect.deleteProperty(target, key);
            trigger(target, key);
            return result;
        },
    });
}
```

> 🎯 **要点**：Proxy 拦截"整个对象所有操作"（含新增/删除/数组）——Vue 3 响应式比 Vue 2 完整的根本原因。

---

## 3. 依赖收集与触发更新

### 3.1 依赖管理结构

```
数据结构：
  WeakMap<目标对象, Map<属性键, Set<effect>>>
    target → key → effects（依赖集合）

收集（track）：读取属性时，把当前正在执行的 effect 加入集合
触发（trigger）：写入属性时，执行该属性的全部 effect

⚠️ 面试必答：
"依赖结构 = target → key → effects 三层 Map——
 track 在 get 中收集、trigger 在 set 中触发；
 WeakMap 保证对象回收时依赖也回收。"
```

### 3.2 最小实现（面试手写）

```javascript
// ⚠️ 手写迷你响应式（Vue 3 原理的教学实现）
const targetMap = new WeakMap();            // 对象 → Map<键, Set<effect>>
let activeEffect = null;                    // 当前正在执行的 effect

function effect(fn) {
    activeEffect = fn;
    fn();                                   // 执行时触发 get → track
    activeEffect = null;
}

function track(target, key) {
    if (!activeEffect) return;
    let depsMap = targetMap.get(target);
    if (!depsMap) targetMap.set(target, depsMap = new Map());
    let deps = depsMap.get(key);
    if (!deps) depsMap.set(key, deps = new Set());
    deps.add(activeEffect);                 // 收集当前 effect
}

function trigger(target, key) {
    const depsMap = targetMap.get(target);
    if (!depsMap) return;
    const deps = depsMap.get(key);
    if (deps) deps.forEach(fn => fn());     // 执行全部依赖
}

function reactive(target) {
    return new Proxy(target, {
        get(t, key, r) { track(t, key); return Reflect.get(t, key, r); },
        set(t, key, v, r) {
            const result = Reflect.set(t, key, v, r);
            trigger(t, key);
            return result;
        },
    });
}

// 使用验证：
const state = reactive({ count: 0 });
effect(() => console.log('count =', state.count));  // 收集
state.count = 1;      // 触发 → count = 1
state.count = 2;      // 触发 → count = 2
```

> 🎯 **要点**：迷你响应式四步——effect（当前依赖）、track（get 收集）、trigger（set 触发）、reactive（Proxy 桥接）。这是 Vue 面试手写题的黄金答案。

---

## 4. ref / reactive / computed 实现

### 4.1 三者的区别

| API | 适用 | 访问 | 解包 |
|------|------|------|------|
| ref | 基础类型/单个值 | `.value` | 模板中自动解包 |
| reactive | 对象/数组 | 直接属性 | 无需 .value |
| computed | 派生值 | `.value` | 模板自动 |

```javascript
import { ref, reactive, computed } from 'vue';

// ref：基本类型响应式
const count = ref(0);
count.value++;                       // ⚠️ script 中必须 .value

// reactive：对象响应式（深层）
const user = reactive({ name: '张三', profile: { age: 25 } });
user.name = '李四';                  // 直接访问（深层也响应）

// ⚠️ 使用原则：
//   基础类型 → ref
//   对象/数组 → reactive（或 ref 包对象）
//   模板中：ref 自动解包（{{ count }} 不需要 .value）
//   script 中：ref 必须 .value（⚠️ 最易错点）

// 解包陷阱：
const obj = { count: ref(0) };
// ❌ 嵌套在对象中不会自动解包（reactive 对象内才会）
// ✅ const obj = reactive({ count: ref(0) })  reactive 会解包
```

### 4.2 ref 的实现本质

```javascript
// ref 本质 = 对象包装 + 值响应式
function ref(value) {
    return new RefImpl(value);
}

class RefImpl {
    constructor(value) {
        this._value = toReactive(value);    // 对象则 reactive
    }
    get value() {
        track(this, 'value');               // ⚠️ 读取收集
        return this._value;
    }
    set value(newValue) {
        this._value = toReactive(newValue);
        trigger(this, 'value');             // 写入触发
    }
}
```

---

## 5. Vue 2 vs Vue 3 响应式

### 5.1 核心差异

| 维度 | Vue 2 | Vue 3 |
|------|:---:|:---:|
| 拦截 | Object.defineProperty | Proxy |
| 新增/删除属性 | ❌ 需 $set/$delete | ✅ 自动 |
| 数组 | 方法 hack（7 个方法） | ✅ 原生 |
| 嵌套对象 | 递归一次全代理 | 惰性（访问时） |
| 性能 | 初始化遍历全属性 | 按需代理 |

### 5.2 Vue 2 的局限（面试常问）

```javascript
// Vue 2 响应式三大坑：
// ① 新增属性不响应
this.user.age = 25;                  // ❌ 视图不更新
Vue.set(this.user, 'age', 25);       // ✅ 必须 $set

// ② 数组下标修改不响应
this.items[0] = '新值';              // ❌
this.$set(this.items, 0, '新值');    // ✅

// ③ 数组长度修改
this.items.length = 0;               // ❌
this.items.splice(0);                // ✅

// Vue 3 全部原生支持（Proxy 拦截）：
user.age = 25;                       // ✅ 自动响应
items[0] = '新值';                   // ✅ 自动响应
```

> 🎯 **要点**：Vue 3 响应式完整性的核心卖点——"新增/删除/数组下标原生响应"，Vue 2 的 $set 时代结束。

---

## 6. 响应式更新流程

### 6.1 完整流程

```
数据变化 → 视图更新全过程：
  ① setter 拦截（Proxy set）
  ② trigger：执行依赖的 effect
  ③ 组件渲染 effect（render 函数）
  ④ 生成新虚拟 DOM
  ⑤ diff 对比旧虚拟 DOM
  ⑥ 最小化更新真实 DOM

⚠️ 面试必答：
"更新链路：set → trigger → 渲染 effect
 → 新 vdom → diff → patch 真实 DOM。
 响应式是'驱动'，虚拟 DOM 是'执行'。"
```

### 6.2 调度与批量更新

```javascript
// ⚠️ 同一轮多次修改 → 合并为一次渲染（微任务批量）
state.count++;
state.count++;
state.name = 'x';
// 三次 set 只触发一次渲染（nextTick 之前合并）

// nextTick：更新后的 DOM 访问
import { nextTick } from 'vue';
state.count++;
await nextTick();                 // DOM 已更新
console.log(el.textContent);
```

> 🎯 **要点**：Vue 的更新是**批量异步**的（微任务合并）——`nextTick` 等 DOM 更新后操作是高频考点。

---

> 🎯 **核心要点**：响应式体系 = **Proxy 拦截**（整个对象 vs defineProperty 单属性）+ **依赖收集**（target→key→effects 三层）+ **触发更新**（set → 渲染 effect → vdom diff）+ **ref/reactive 分工**（基础类型 ref、对象 reactive）+ **批量异步**（nextTick）。手写迷你响应式 + Vue2/3 差异是面试两大必考题。

---

**返回总览**：[00-Vue总览与核心概念](00-Vue总览与核心概念.md) | **上一篇**：[02-Vue组件化开发](02-Vue组件化开发.md) | **下一篇**：[04-Vue组合式API与进阶](04-Vue组合式API与进阶.md)
