# JavaScript 详细知识点

> **文档定位**：Java 后端技术参考文档 | JavaScript 核心知识点全景  
> **核心说明**：JavaScript（JS）是一门跨平台、面向对象的脚本语言，主要用于网页交互，可在浏览器和服务器端（Node.js）运行  
> **核心特性**：弱类型、动态性、解释性、单线程  
> **组成部分**：ECMAScript（核心语法）+ DOM（文档对象模型）+ BOM（浏览器对象模型）

---

## 目录

- [一、JavaScript 基础核心](#一javascript-基础核心)
- [二、函数详解](#二函数详解)
- [三、数组详解](#三数组详解)
- [四、对象详解](#四对象详解)
- [五、ES6+ 新增特性](#五es6-新增特性)
- [六、异步编程](#六异步编程)
- [七、DOM 操作](#七dom-操作)
- [八、BOM 操作](#八bom-操作)

---

## 一、JavaScript 基础核心

### 1.1 变量声明

| 关键字 | 作用域 | 变量提升 | 重复声明 | 重新赋值 | 暂存死区 |
|--------|--------|----------|----------|----------|----------|
| `var` | 函数级 | ✅ | ✅ | ✅ | ❌ |
| `let` | 块级 | ❌ | ❌ | ✅ | ✅ |
| `const` | 块级 | ❌ | ❌ | ❌ | ✅ |

```javascript
// var 变量提升
console.log(a); // undefined
var a = 10;

// let 暂存死区
console.log(b); // 报错
let b = 20;

// const 声明时必须赋值，引用类型属性可修改
const obj = { a: 1 };
obj.a = 2; // 合法
```

### 1.2 数据类型

#### 基本数据类型（值类型，存储在栈中）

| 类型 | 说明 | 示例 |
|------|------|------|
| `Number` | 数字（含 NaN、Infinity） | `10`、`NaN`（`NaN !== NaN`） |
| `String` | 字符串，支持模板字符串 | `'hello'`、`` `值: ${x}` `` |
| `Boolean` | `true` / `false` | 6 个 falsy 值：`0`、`''`、`null`、`undefined`、`NaN`、`false` |
| `Undefined` | 变量声明未赋值 | `typeof undefined === 'undefined'` |
| `Null` | 空对象指针 | `typeof null === 'object'`（历史 bug） |
| `Symbol` | ES6 唯一值 | 对象属性（不可枚举） |
| `BigInt` | ES6 大整数 | `10n`，不能与 Number 直接运算 |

#### 引用数据类型（存储在堆中）

| 类型 | 说明 |
|------|------|
| `Object` | 键值对集合 |
| `Array` | 有序集合，索引从 0 开始 |
| `Function` | 可执行代码块，本质是对象 |
| `Date` | 日期时间 |
| `RegExp` | 正则表达式 |

#### 类型转换

| 转换方向 | 隐式转换 | 显式转换 |
|----------|----------|----------|
| → Boolean | falsy → false，其余 → true | `Boolean()` |
| → Number | 纯数字字符串 → 数字，`null` → 0，`undefined` → NaN | `Number()`、`parseInt()`、`parseFloat()` |
| → String | 基本类型直接拼接，引用类型 → `"[object 类型]"` | `String()`、`toString()` |

### 1.3 运算符

| 类别 | 运算符 | 注意 |
|------|--------|------|
| **算术** | `+` `-` `*` `/` `%` `++` `--` | `+` 号遇字符串触发拼接 |
| **比较** | `==` `===` `!=` `!==` `>` `<` | `10 == '10'` → true；`10 === '10'` → false |
| **逻辑** | `&&` `\|\|` `!` | 短路运算：`&&` 左边 false 则不执行右边 |
| **三元** | `条件 ? 表达式1 : 表达式2` | 可嵌套，但建议不超过一层 |
| **其他** | `typeof`、`instanceof`、`delete` | `typeof null === 'object'` |

### 1.4 流程控制语句

| 语句 | 用途 | 语法 |
|------|------|------|
| **if-else** | 条件判断 | `if (条件) {} else {}` |
| **switch-case** | 多条件匹配 | `switch(x) { case v: ...; break; default: }` |
| **for** | 已知次数循环 | `for(初始化; 条件; 更新) {}` |
| **while** | 未知次数循环 | `while(条件) {}` |
| **do-while** | 至少执行一次 | `do {} while(条件)` |
| **for-in** | 遍历对象可枚举属性 | `for(var key in obj) {}` |
| **for-of** | 遍历可迭代对象 | `for(var val of arr) {}` |
| **break** | 终止循环/switch | — |
| **continue** | 跳过当前循环 | — |
| **return** | 函数返回值 | — |

---

## 二、函数详解

### 2.1 函数的声明与调用

```javascript
// 函数声明（存在提升）
function fn(a, b) { return a + b; }

// 函数表达式（无提升）
const fn = function(a, b) { return a + b; };

// 箭头函数（ES6，无 this、无 arguments）
const fn = (a, b) => a + b;
```

| 调用方式 | this 指向 |
|----------|----------|
| 普通调用 `fn()` | `window` / `undefined`（严格模式） |
| 对象方法 `obj.fn()` | 调用方法的对象 |
| 构造函数 `new Fn()` | 新创建的实例 |
| apply/call/bind | 第一个参数指定的对象 |
| 箭头函数 | 定义时的外层 this（固定不变） |

### 2.2 作用域与闭包

| 作用域类型 | 范围 |
|-----------|------|
| **全局作用域** | 最外层，`var` 声明的全局变量挂载到 `window` |
| **函数作用域** | 函数内部，外层不可访问内层 |
| **块级作用域** | `{}` 包裹（ES6），`let`/`const` 声明的变量 |

> **闭包**：内层函数引用外层函数的变量，且内层函数被外部引用时，外层函数的作用域不会被销毁。

```javascript
function outer() {
  let count = 0;  // 私有变量
  return function() {
    return ++count;
  };
}
const counter = outer();
counter(); // 1
counter(); // 2
```

---

## 三、数组详解

### 3.1 数组创建

```javascript
// 字面量（最常用）
const arr = [1, 2, 3];

// ES6 新增
Array.of(1, 2, 3);         // 解决 new Array 歧义
Array.from(可迭代对象);      // 类数组 → 数组
```

### 3.2 核心方法速查

| 方法 | 说明 | 改变原数组 |
|------|------|-----------|
| `push()` / `pop()` | 末尾添加 / 删除 | ✅ |
| `unshift()` / `shift()` | 开头添加 / 删除 | ✅ |
| `splice(start, n, ...items)` | 增删改万能方法 | ✅ |
| `forEach()` | 遍历，无返回值 | ❌ |
| `map()` | 遍历 + 映射，返回新数组 | ❌ |
| `filter()` | 筛选，返回新数组 | ❌ |
| `find()` / `findIndex()` | 第一个匹配元素 / 索引 | ❌ |
| `every()` / `some()` | 全满足 / 有一个满足 | ❌ |
| `reduce(fn, init)` | 累加器，归并为一个值 | ❌ |
| `sort()` / `reverse()` | 排序 / 反转 | ✅ |
| `concat()` / `slice()` | 拼接 / 截取 | ❌ |
| `join(sep)` | 数组 → 字符串 | ❌ |
| `includes()` | 是否包含 | ❌ |
| `flat(n)` | 扁平化 n 层 | ❌ |

---

## 四、对象详解

### 4.1 对象创建

```javascript
// 字面量（最常用）
const obj = { name: '张三', age: 18 };

// ES6 class（语法糖）
class Person {
  constructor(name, age) { this.name = name; this.age = age; }
}
```

### 4.2 属性操作

```javascript
// 访问
obj.name       // 点语法（key 必须是合法标识符）
obj['name']    // 方括号（key 可以是变量、特殊字符）

// 增删改
obj.name = '李四';       // 修改
obj.gender = '男';       // 新增
delete obj.gender;       // 删除

// ES6 简写
const name = '张三';
const obj = { name };    // 等价 { name: name }
const obj = { sayHi() {} };  // 方法简写
```

### 4.3 常用对象方法

| 方法 | 说明 |
|------|------|
| `Object.keys(obj)` | 返回可枚举属性的 key 数组 |
| `Object.values(obj)` | 返回可枚举属性的 value 数组 |
| `Object.entries(obj)` | 返回 `[key, value]` 数组 |
| `Object.assign(target, ...sources)` | 浅拷贝 |
| `Object.freeze(obj)` | 冻结对象（浅冻结） |
| `Object.is(a, b)` | 严格相等判断 |

### 4.4 原型与继承

```
实例.__proto__ → 构造函数.prototype → Object.prototype → null
```

| 继承方式 | 推荐度 | 说明 |
|----------|--------|------|
| 原型链继承 | ⭐⭐ | 引用类型共享问题 |
| 构造函数继承 | ⭐⭐ | 无法继承原型方法 |
| 组合继承 | ⭐⭐⭐ | 父类构造函数调用两次 |
| 寄生组合继承 | ⭐⭐⭐⭐⭐ | 最推荐（`Object.create`） |
| ES6 class extends | ⭐⭐⭐⭐⭐ | 语法糖，底层仍是原型链 |

---

## 五、ES6+ 新增特性

### 5.1 解构赋值

```javascript
// 数组解构
const [a, b, c] = [1, 2, 3];
const [a = 0, , c] = [1, 2, 3];  // 默认值 + 跳过

// 对象解构
const { name, age } = { name: '张三', age: 18 };
const { name: myName } = { name: '张三' };  // 重命名
```

### 5.2 字符串新增方法

| 方法 | 说明 |
|------|------|
| `includes(str)` | 是否包含子串 |
| `startsWith(str)` / `endsWith(str)` | 开头/结尾匹配 |
| `repeat(n)` | 重复 n 次 |
| `padStart(len, str)` / `padEnd(len, str)` | 补全长度 |
| `trimStart()` / `trimEnd()` | 去除开头/结尾空白 |

### 5.3 新增数据结构

| 类型 | 说明 | 常用方法 |
|------|------|----------|
| **Set** | 唯一值集合 | `add()`、`delete()`、`has()`、`size` |
| **Map** | 任意类型键值对 | `set()`、`get()`、`delete()`、`has()`、`size` |

```javascript
// 数组去重
const unique = [...new Set(arr)];
```

### 5.4 模块（Module）

```javascript
// 导出
export const name = '张三';
export default function() {};
export { name, age };

// 导入
import { name } from './module.js';
import fn from './module.js';       // 默认导入
import * as m from './module.js';   // 全部导入
```

---

## 六、异步编程

### 6.1 异步概念

JS 是单线程，异步代码（定时器、AJAX、事件回调）不阻塞主线程，放入"任务队列"等待执行。

### 6.2 任务队列

```
同步代码 → 微任务队列（全部） → 宏任务（一个） → 微任务（全部） → 循环
```

| 类型 | 常见 | 优先级 |
|------|------|--------|
| **宏任务** | `setTimeout`、`setInterval`、I/O | 低 |
| **微任务** | `Promise.then/catch/finally`、`queueMicrotask()` | 高 |

### 6.3 Promise

```javascript
// 创建
new Promise((resolve, reject) => {
  // 异步操作
  if (成功) resolve(结果);
  else reject(错误);
});

// 链式调用
promise
  .then(res => {})     // 成功回调
  .catch(err => {})    // 失败回调
  .finally(() => {});  // 无论成功失败都执行
```

| 静态方法 | 说明 |
|----------|------|
| `Promise.resolve()` | 快速创建成功 Promise |
| `Promise.reject()` | 快速创建失败 Promise |
| `Promise.all([])` | 全部成功才成功 |
| `Promise.race([])` | 第一个完成即返回 |
| `Promise.allSettled([])` | 全部完成后返回每项结果 |

### 6.4 async / await

```javascript
async function fetchData() {
  try {
    const result = await promise;  // 等待 Promise 完成
    return result;
  } catch (err) {
    // 错误处理
  }
}
```

---

## 七、DOM 操作

### 7.1 节点获取

```javascript
document.getElementById('id')
document.getElementsByClassName('class')  // HTMLCollection（实时）
document.getElementsByTagName('div')
document.querySelector('.class')          // 单个
document.querySelectorAll('.class')       // NodeList（静态）
```

### 7.2 节点操作

```javascript
// 创建
document.createElement('div')

// 插入
parent.appendChild(child)
parent.insertBefore(newNode, refNode)
parent.append(...nodes)       // 末尾，支持文本
parent.prepend(...nodes)      // 开头

// 删除
parent.removeChild(child)
element.remove()              // 直接删除自身

// 克隆
element.cloneNode(true)       // true = 深克隆
```

### 7.3 属性与样式

```javascript
// 属性
element.getAttribute('attr')
element.setAttribute('attr', 'value')
element.dataset.id              // data-id 属性

// 类样式
element.classList.add('class')
element.classList.remove('class')
element.classList.toggle('class')
element.classList.contains('class')

// 内联样式
element.style.color = 'red'
element.style.backgroundColor = '#fff'  // 驼峰命名
```

### 7.4 DOM 事件

```javascript
// 绑定
element.addEventListener('click', fn, false)  // false = 冒泡阶段
element.removeEventListener('click', fn)

// 事件对象
event.target           // 触发事件的目标节点
event.stopPropagation() // 阻止冒泡
event.preventDefault()  // 阻止默认行为
```

| 常见事件 | 类型 |
|----------|------|
| **鼠标** | `click`、`dblclick`、`mouseover`、`mouseout` |
| **键盘** | `keydown`、`keyup` |
| **表单** | `input`、`change`、`submit`、`focus`、`blur` |
| **文档** | `load`、`DOMContentLoaded` |

---

## 八、BOM 操作

BOM（浏览器对象模型）的核心对象是 `window`。

| 属性/对象 | 说明 |
|-----------|------|
| `window.innerWidth / innerHeight` | 可视区域宽高 |
| `window.location` | 地址栏对象 |
| `window.history` | 历史记录对象 |
| `window.navigator` | 浏览器信息对象 |
| `window.screen` | 屏幕对象 |
