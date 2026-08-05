# 01 - JavaScript 基础语法与流程控制

> 定位：变量声明、运算符、条件与循环、解构赋值、字符串与数组方法——JS 语法地基

## 📚 目录

1. [变量声明：var / let / const](#1-变量声明var--let--const)
2. [运算符](#2-运算符)
3. [条件与循环](#3-条件与循环)
4. [解构赋值](#4-解构赋值)
5. [字符串与模板](#5-字符串与模板)
6. [数组方法体系](#6-数组方法体系)

---

## 1. 变量声明：var / let / const

### 1.1 三兄弟对比

| 维度 | var | let | const |
|------|:---:|:---:|:---:|
| 作用域 | 函数级 | 块级 | 块级 |
| 变量提升 | ✅（undefined） | ⚠️ TDZ | ⚠️ TDZ |
| 重复声明 | ✅ | ❌ | ❌ |
| 重新赋值 | ✅ | ✅ | ❌（引用不可改） |
| 全局挂载 | window.x | 不挂 | 不挂 |

```javascript
// 变量提升（hoisting）
console.log(a);      // undefined（var 提升）
var a = 1;

console.log(b);      // ❌ ReferenceError（TDZ 暂时性死区）
let b = 2;

// const 的正确语义：引用不可变（对象内容可变）
const obj = { x: 1 };
obj.x = 2;           // ✅ 允许（改内容）
obj = {};            // ❌ 不允许（改引用）
```

> 🎯 **要点**：默认 `const`、需要改时 `let`、永远不用 `var`（现代规范）。TDZ 与块级作用域是面试高频点。

---

## 2. 运算符

### 2.1 算术与比较

```javascript
// 算术
+ - * / % **        // ** 幂运算（ES2016）

// 比较（永远用 === / !==）
1 === '1'    // false（类型也检查）
1 == '1'     // true（隐式转换，避免）

// 现代空值处理（ES2020）
const name = user?.name ?? '匿名';
// ?. 可选链：user 为 null/undefined 时不报错
// ?? 空值合并：null/undefined 时取默认（不是 0/false！）
```

### 2.2 逻辑与赋值

```javascript
// 逻辑运算符（短路求值）
const isAdmin = user && user.role === 'admin';   // 短路
const color = theme || 'light';                  // 取默认（⚠️ 0/'' 也会被替换）

// 逻辑赋值（ES2021）
a ||= b;        // a = a || b
a &&= b;        // a = a && b
a ??= b;        // a = a ?? b（空值才赋值）

// 一元运算符
++i / i++       // 前/后自增
!x              // 取反
typeof x        // 类型
```

---

## 3. 条件与循环

### 3.1 条件语句

```javascript
// if / else if / else
if (score >= 90) {
    grade = 'A';
} else if (score >= 80) {
    grade = 'B';
} else {
    grade = 'C';
}

// switch（现代写法：return 代替 break）
switch (day) {
    case 'mon': return '周一';
    case 'tue': return '周二';
    default: return '其他';
}

// 三元（简洁）
const status = isLogin ? '已登录' : '未登录';
```

### 3.2 循环家族

```javascript
// for 传统
for (let i = 0; i < 10; i++) { }

// for...of（值迭代，推荐 ✅）
for (const item of items) { }

// for...in（键迭代，⚠️ 遍历对象属性，数组别用）
for (const key in obj) { }

// while / do-while
while (condition) { }

// ⚠️ 数组遍历方法论：
items.forEach(x => ...);              // 遍历
items.map(x => x * 2);                // 映射（返回新数组）
items.filter(x => x > 0);             // 过滤
items.reduce((sum, x) => sum + x, 0); // 聚合
items.find(x => x.id === 1);          // 找第一个
items.some(x => x > 10);              // 任一满足
items.every(x => x > 0);              // 全部满足
```

---

## 4. 解构赋值

### 4.1 对象解构

```javascript
const user = { name: '张三', age: 25, address: { city: '北京' } };

// 基本解构 + 默认值
const { name, age = 18 } = user;

// 重命名
const { name: userName } = user;

// 嵌套解构
const { address: { city } } = user;

// 剩余收集
const { name, ...rest } = user;    // rest = {age, address}

// 函数参数解构（常用 ✅）
function greet({ name, age }) {
    return `你好，${name}（${age} 岁）`;
}
```

### 4.2 数组解构

```javascript
const [first, second] = [1, 2, 3];

// 跳过 + 剩余
const [a, , c, ...rest] = [1, 2, 3, 4, 5];   // a=1, c=3, rest=[4,5]

// 交换变量（经典技巧）
[a, b] = [b, a];

// 默认值
const [x = 0] = [undefined];
```

---

## 5. 字符串与模板

### 5.1 模板字符串（ES6）

```javascript
const name = '张三';
const age = 25;

// 插值
const msg = `你好，${name}，今年 ${age} 岁`;

// 多行
const html = `
    <div>
        <p>${name}</p>
    </div>
`;

// 标签模板（进阶）
const tagged = (strs, ...values) => strs.join('|') + values.join('+');
```

### 5.2 字符串方法速查

```javascript
'Hello'.toUpperCase();          // HELLO
'  hello  '.trim();             // 'hello'
'hello world'.includes('world'); // true
'hello'.startsWith('he');        // true
'hello'.padStart(8, '0');        // '000hello'
'1,2,3'.split(',');              // ['1','2','3']
'abc'.repeat(3);                 // 'abcabcabc'
'hello'.replaceAll('l', 'L');    // 'heLLo'（ES2021，替换全部）
'hello'.at(-1);                  // 'o'（ES2022，支持负索引）
```

---

## 6. 数组方法体系

### 6.1 数组方法分类

| 类别 | 方法 | 特点 |
|------|------|------|
| 遍历 | forEach | 无返回值 |
| 映射 | map | 返回新数组（长度不变） |
| 过滤 | filter | 返回新数组（长度变） |
| 聚合 | reduce | 任意返回值 |
| 查找 | find / findIndex / includes | 单值 |
| 判断 | some / every | 布尔 |
| 排序 | sort / reverse | 原地修改 ⚠️ |
| 增删 | push/pop/shift/unshift/splice | 原地修改 |
| 切片 | slice / concat | 返回新数组 |
| 展平 | flat / flatMap（ES2019） | 降维 |

### 6.2 链式调用

```javascript
// 经典链式：过滤 → 映射 → 排序 → 取前 3
const result = users
    .filter(u => u.age >= 18)
    .map(u => ({ name: u.name, salary: u.salary }))
    .sort((a, b) => b.salary - a.salary)
    .slice(0, 3);

// reduce 的进阶用法：分组
const byCity = users.reduce((acc, u) => {
    (acc[u.city] ||= []).push(u);          // 逻辑赋值
    return acc;
}, {});

// 扁平化
[[1, 2], [3, [4]]].flat(2);    // [1,2,3,4]
[1, 2, 3].flatMap(x => [x, x * 2]);  // [1,2,2,4,3,6]
```

> 🎯 **要点**：map/filter 返回新数组（不可变）、sort/splice 原地修改（注意副本）；链式调用 + reduce 分组是数据处理的标准姿势。

---

> 🎯 **核心要点**：基础语法 = **const/let 声明**（默认 const + TDZ）+ **严格相等**（=== 永远）+ **解构**（参数/交换/默认值）+ **模板字符串** + **数组方法链**。这些是 JS 日常编码的 90% 语法面。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **下一篇**：[02-JavaScript函数与作用域](02-JavaScript函数与作用域.md)
