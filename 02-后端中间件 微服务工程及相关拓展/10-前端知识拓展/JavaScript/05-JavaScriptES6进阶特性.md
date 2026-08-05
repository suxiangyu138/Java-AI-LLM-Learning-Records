# 05 - JavaScript ES6+ 进阶特性

> 定位：模块系统、Symbol、迭代器与生成器、Proxy 与 Reflect、Map/Set/WeakMap、新数据结构——ES6 之后的工程级特性

## 📚 目录

1. [ES Modules 模块系统](#1-es-modules-模块系统)
2. [Symbol 与唯一键](#2-symbol-与唯一键)
3. [迭代器与生成器](#3-迭代器与生成器)
4. [Proxy 与 Reflect](#4-proxy-与-reflect)
5. [Map / Set / WeakMap](#5-map--set--weakmap)
6. [其他高频特性速查](#6-其他高频特性速查)

---

## 1. ES Modules 模块系统

### 1.1 导入导出

```javascript
// export.js —— 导出
export const name = '模块';
export function greet() { return 'hi'; }
export default class Main {}        // 默认导出（每模块一个）

// import.js —— 导入
import Main, { name, greet } from './export.js';   // 默认 + 具名
import * as all from './export.js';                // 全部
import { greet as hi } from './export.js';         // 重命名

// 动态导入（ES2020，按需加载）
const module = await import('./heavy.js');
```

### 1.2 模块特性

| 特性 | 说明 |
|------|------|
| 严格模式 | 模块内自动严格模式 |
| 静态分析 | 导入导出可静态解析（Tree-shaking 基础） |
| 循环依赖 | 支持（有 TDZ 风险） |
| 延迟执行 | 依赖模块先执行（深度优先） |
| 顶层 await | 模块支持（ES2022） |

> 🎯 **要点**：ES Modules 是前端标准模块系统（VS CommonJS 的 `require`）。`export default` + 具名导出组合是组件库惯例。

---

## 2. Symbol 与唯一键

```javascript
// Symbol = 唯一标识（每次创建都不同）
const s1 = Symbol('desc');
const s2 = Symbol('desc');
s1 === s2;                 // false（描述相同也唯一）

// 用途一：对象私有/元数据键
const ITERATOR = Symbol.iterator;
const obj = {};
obj[ITERATOR] = function* () { yield 1; };

// 用途二：防属性冲突（第三方库）
const KEY = Symbol('key');
obj[KEY] = '安全属性';     // 不会与其他库冲突

// 用途三：内置 Symbol（JS 内部机制）
obj[Symbol.iterator];      // 迭代协议
obj[Symbol.toPrimitive];   // 类型转换
obj[Symbol.dispose];       // ⚠️ using 声明（ES2026）资源清理钩子

// ⚠️ 注意：for...in / Object.keys 不含 Symbol 键
// 取 Symbol 键：Object.getOwnPropertySymbols(obj)
```

---

## 3. 迭代器与生成器

### 3.1 迭代协议

```javascript
// 可迭代对象（有 Symbol.iterator）：数组/字符串/Map/Set/类数组
const arr = [1, 2, 3];
const iterator = arr[Symbol.iterator]();

iterator.next();    // {value: 1, done: false}
iterator.next();    // {value: 2, done: false}
iterator.next();    // {value: 3, done: false}
iterator.next();    // {value: undefined, done: true}

// 自定义可迭代（面试手写）
const range = {
    *[Symbol.iterator]() {
        for (let i = 1; i <= 3; i++) yield i;
    },
};
for (const x of range) console.log(x);   // 1 2 3
```

### 3.2 生成器（Generator）

```javascript
// 生成器 = 可暂停的函数（yield 暂停/恢复）
function* fibonacci() {
    let a = 0, b = 1;
    while (true) {
        yield a;                 // 暂停并返回值
        [a, b] = [b, a + b];
    }
}
const fib = fibonacci();
fib.next().value;    // 0
fib.next().value;    // 1
fib.next().value;    // 1
fib.next().value;    // 2

// 生成器与迭代器：生成器天然可迭代
// ⚠️ 用途：无限序列、惰性计算、异步流程控制（async 的底层）
```

### 3.3 ES2025 迭代器增强

```javascript
// Iterator Helpers（ES2025）：迭代器链式操作（惰性！）
function* naturalNumbers() {
    let n = 1;
    while (true) yield n++;
}

naturalNumbers()
    .filter(n => n % 2 === 0)       // 偶数
    .map(n => n * n)                // 平方
    .take(5)                        // 取前 5 个（惰性终止）
    .toArray();                     // [4, 16, 36, 64, 100]

// 其他：.drop()、.flatMap()、.reduce()、.forEach()、Iterator.from()
```

> 🎯 **要点**：生成器 = 可暂停函数（yield）；ES2025 的 Iterator Helpers 让无限序列处理像数组一样优雅（且惰性）。

---

## 4. Proxy 与 Reflect

### 4.1 Proxy 拦截

```javascript
// Proxy = 拦截对象操作（元编程）
const target = { name: '张三' };
const proxy = new Proxy(target, {
    get(obj, key) {
        if (key === 'name') return `【${obj[key]}】`;
        return obj[key];
    },
    set(obj, key, value) {
        if (key === 'age' && value < 0) throw new Error('年龄非法');
        obj[key] = value;
        return true;
    },
    has(obj, key) { return key in obj; },
    deleteProperty(obj, key) { delete obj[key]; return true; },
});

proxy.name;        // 【张三】
proxy.age = -1;    // ❌ 抛错（set 拦截校验）
```

| 拦截方法 | 触发场景 |
|---------|---------|
| get/set | 属性读写 |
| has | `in` 操作 |
| deleteProperty | delete |
| apply/construct | 函数调用/new |
| ownKeys | Object.keys |

### 4.2 Proxy 的应用

```javascript
// ① Vue 3 响应式原理（reactive 的底层）
function reactive(obj) {
    return new Proxy(obj, {
        get(target, key, receiver) {
            track(target, key);              // 依赖收集
            return Reflect.get(target, key, receiver);
        },
        set(target, key, value, receiver) {
            const result = Reflect.set(target, key, value, receiver);
            trigger(target, key);            // 触发更新
            return result;
        },
    });
}

// ② 数据校验 / 日志 / 缓存
// ③ 私有属性模拟

// Reflect：与 Proxy 配套的静态方法集（语义化操作）
Reflect.get(obj, 'name');        // obj.name
Reflect.set(obj, 'age', 25);     // 返回布尔
Reflect.has(obj, 'name');        // in 操作
```

> 🎯 **要点**：Proxy + Reflect 是 Vue 3 响应式的基础（get 收集依赖、set 触发更新）。Proxy 比 Object.defineProperty 强大（可拦截所有操作、数组友好）。

---

## 5. Map / Set / WeakMap

### 5.1 Map vs Object

```javascript
const map = new Map();
map.set('key', 'value');
map.get('key');
map.has('key');
map.delete('key');
map.size;

// Map 的优势（vs 普通对象）：
// ① 任意键类型（对象/函数作键）
// ② 有序迭代（插入顺序）
// ③ size 直接可得
// ④ 无原型链污染

// 对象 vs Map 选型：
//   键为字符串且简单 → 对象（字面量方便）
//   任意键/频繁增删/需要有序 → Map

// ES2026：Map.getOrInsert（查找或插入）
map.getOrInsert('key', 'default');
```

### 5.2 Set 与集合运算

```javascript
const set = new Set([1, 2, 3, 3, 3]);   // 去重 → {1,2,3}
set.add(4); set.has(2); set.delete(1);

// 数组去重（经典）
const unique = [...new Set(arr)];

// ⚠️ ES2025 Set 新方法（集合运算）：
const a = new Set([1, 2, 3]);
const b = new Set([2, 3, 4]);

a.union(b);              // {1,2,3,4} 并集
a.intersection(b);       // {2,3} 交集
a.difference(b);         // {1} 差集
a.isSubsetOf(b);         // false 子集判断
a.isDisjointFrom(b);     // false 不相交
```

### 5.3 WeakMap / WeakSet

```javascript
// WeakMap：键必须是对象，弱引用（不阻止 GC）
const wm = new WeakMap();
const obj = {};
wm.set(obj, '数据');
obj = null;              // ⚠️ 键被回收 → 条目自动消失

// 用途：私有数据、缓存（键生命周期随对象）、
//       Vue 3 的 targetMap（响应式对象 → 依赖）

// 与 Map 区别：
//   ① 键只能是对象
//   ② 弱引用（防内存泄漏）
//   ③ 不可迭代（无 size/keys）
```

---

## 6. 其他高频特性速查

| 特性 | 版本 | 示例 |
|------|:---:|------|
| 可选链 ?. | ES2020 | `user?.address?.city` |
| 空值合并 ?? | ES2020 | `x ?? '默认'` |
| BigInt | ES2020 | `12345678901234567890n` |
| globalThis | ES2020 | 跨环境全局对象 |
| 逻辑赋值 | ES2021 | `x ??= y` |
| replaceAll | ES2021 | `'a-b'.replaceAll('-','_')` |
| at() | ES2022 | `arr.at(-1)` |
| 结构化克隆 | ES2022 | `structuredClone(obj)` |
| Array.findLast | ES2023 | `arr.findLast(x => x > 0)` |
| toSorted/toReversed | ES2023 | 不可变排序（返回新数组） |
| 数组分组 | ES2024 | `Object.groupBy(items, cb)` |

```javascript
// ES2024 数组分组（替代手写 reduce）
const byCity = Object.groupBy(users, u => u.city);
// Map.groupBy 同样可用

// ES2023 不可变数组方法（⚠️ sort 原地 vs toSorted 新数组）
arr.toSorted((a, b) => a - b);    // 返回新数组（原数组不变）
arr.toReversed();                 // 反转副本
```

---

> 🎯 **核心要点**：ES6+ 进阶 = **模块**（import/export）+ **Symbol**（唯一键 + 内置协议）+ **生成器**（yield 暂停 + 惰性）+ **Proxy/Reflect**（元编程 + Vue 3 响应式底层）+ **Map/Set/WeakMap**（集合体系）+ **年度新特性**（?. ?? structuredClone 分组）。ES2025 的 Iterator Helpers 与 Set 集合运算是最新亮点。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[04-JavaScript异步编程](04-JavaScript异步编程.md) | **下一篇**：[06-JavaScriptDOM与浏览器API](06-JavaScriptDOM与浏览器API.md)
