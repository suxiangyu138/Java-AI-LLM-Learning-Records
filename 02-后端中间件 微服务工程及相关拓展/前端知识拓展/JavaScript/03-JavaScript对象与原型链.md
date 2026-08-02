# 03 - JavaScript 对象与原型链

> 定位：对象机制、原型链、class 语法、继承方案、Object 方法体系——"JS 的面向对象到底怎么工作"

## 📚 目录

1. [对象基础](#1-对象基础)
2. [原型与原型链](#2-原型与原型链)
3. [原型链查找与继承](#3-原型链查找与继承)
4. [class 语法](#4-class-语法)
5. [继承方案对比](#5-继承方案对比)
6. [Object 方法体系](#6-object-方法体系)

---

## 1. 对象基础

### 1.1 创建与属性

```javascript
// 对象字面量（最常用）
const user = {
    name: '张三',
    age: 25,
    greet() { return `你好，${this.name}`; },   // 方法简写
};

// 动态属性名（ES6）
const key = 'dynamic';
const obj = { [key]: 'value' };       // {dynamic: 'value'}

// 属性操作
user.email = 'a@b.com';               // 添加
delete user.age;                      // 删除
'name' in user;                       // 存在性检查（含原型）
Object.hasOwn(user, 'name');          // ⚠️ 自有属性检查（ES2022）
```

### 1.2 属性特性

```javascript
// 属性描述符（高级）
Object.defineProperty(user, 'id', {
    value: 10086,
    writable: false,       // 可写
    enumerable: false,     // 可枚举（for...in 跳过）
    configurable: false,   // 可删除/改描述符
});

// 遍历方式选择：
Object.keys(obj)      // 自有可枚举键
Object.values(obj)    // 自有可枚举值
Object.entries(obj)   // [键, 值] 对数组
for (const key in obj) // ⚠️ 含原型链上的可枚举属性
```

---

## 2. 原型与原型链

### 2.1 原型是什么

```
每个函数都有 prototype 属性（原型对象）
每个对象都有 __proto__（内部 [[Prototype]]）指向其原型
  ⚠️ 现代写法：Object.getPrototypeOf(obj)

函数的关系（面试必画）：
  function Person() {}
  Person.prototype          → 原型对象
  new Person()              → 实例
  实例.__proto__ === Person.prototype
  Person.prototype.constructor === Person
```

```javascript
function Person(name) {
    this.name = name;
}
Person.prototype.sayHello = function () {
    return `你好，${this.name}`;
};

const p = new Person('张三');
p.sayHello();        // 方法来自原型（不在实例上）
p.hasOwnProperty('name');      // true（实例自有）
p.hasOwnProperty('sayHello');  // false（来自原型）
```

### 2.2 原型链

```
原型链 = 对象的"继承链"：
  p → Person.prototype → Object.prototype → null

查找规则：属性访问时沿链向上找，找到即停

⚠️ 面试必答：
"原型链是 JS 的继承机制——
 实例属性 > 原型链逐层查找；
 方法放原型（共享）、状态放实例（独立）。"
```

---

## 3. 原型链查找与继承

### 3.1 继承的实现

```javascript
// 原型链继承（经典方式）
function Animal(name) {
    this.name = name;
}
Animal.prototype.eat = function () { return `${this.name} 在吃`; };

// 组合继承（构造函数 + 原型链）
function Dog(name, breed) {
    Animal.call(this, name);      // ① 继承实例属性
    this.breed = breed;
}
Dog.prototype = Object.create(Animal.prototype);   // ② 继承原型方法
Dog.prototype.constructor = Dog;                   // ③ 修复 constructor
Dog.prototype.bark = function () { return '汪汪'; };

const dog = new Dog('旺财', '金毛');
dog.eat();    // 来自 Animal.prototype
dog.bark();   // 来自 Dog.prototype
```

### 3.2 instanceof 原理

```javascript
// instanceof = 沿原型链找 prototype
dog instanceof Dog;      // true
dog instanceof Animal;   // true（原型链上）
dog instanceof Object;   // true
dog instanceof Array;    // false

// 手写 instanceof（面试题）
function myInstanceof(obj, Ctor) {
    let proto = Object.getPrototypeOf(obj);
    while (proto) {
        if (proto === Ctor.prototype) return true;
        proto = Object.getPrototypeOf(proto);
    }
    return false;
}
```

---

## 4. class 语法

### 4.1 基本 class

```javascript
// class 是语法糖（底层还是原型链）
class Animal {
    // 实例字段（ES2022 直接声明）
    type = 'animal';

    // 构造器
    constructor(name) {
        this.name = name;
    }

    // 实例方法（原型上）
    eat() { return `${this.name} 在吃`; }

    // 静态方法（类上，实例不可调）
    static create(name) { return new Animal(name); }

    // 私有字段（ES2022 #）
    #secret = '私密';
    getSecret() { return this.#secret; }
}
```

### 4.2 class 继承

```javascript
class Dog extends Animal {
    constructor(name, breed) {
        super(name);            // ⚠️ 必须先调 super（this 之前）
        this.breed = breed;
    }
    bark() { return '汪汪'; }
    eat() {                    // 方法重写
        return `${super.eat()}，真香`;    // super 调用父方法
    }
}

// 静态继承
class Cat extends Animal {}
Cat.create('咪咪');    // ✅ 静态方法也继承
```

| 特性 | 说明 |
|------|------|
| super | 必须先于 this 调用（构造器） |
| 私有 # | 真私有（ES2022，外部不可访问） |
| 静态 | 类级方法/属性（继承可用） |
| getter/setter | `get name() {}` |

---

## 5. 继承方案对比

### 5.1 继承方式演进

| 方式 | 原理 | 问题 |
|------|------|------|
| 原型链继承 | Child.prototype = Parent.prototype | 引用属性共享 |
| 构造函数继承 | Parent.call(this) | 方法不共享 |
| 组合继承 | 两者结合 | 父构造调用两次 |
| 寄生组合继承 | Object.create 桥接 | ✅ 最优（class 底层） |
| class extends | 语法糖 | ✅ 现代标准 |

> 🎯 **要点**：面试从"手写继承"问起——最优是寄生组合继承（Object.create 桥接避免父构造重复调用）；class extends 是现代答案。

### 5.2 组合 vs 继承（设计层面）

```
优先组合：has-a（对象组合）
慎用继承：is-a（层级明确才用）

⚠️ 面试必答：
"'组合优于继承'——继承层级深了
 难以维护（脆弱的基类问题）；
 现代组件设计多用组合。"
```

---

## 6. Object 方法体系

### 6.1 静态方法速查

```javascript
Object.keys(obj)          // 键数组（可枚举自有）
Object.values(obj)        // 值数组
Object.entries(obj)       // [k,v] 数组
Object.fromEntries(arr)   // 反向构造（ES2019）

Object.assign(target, src)   // 合并（浅拷贝）
Object.create(proto)         // 指定原型创建
Object.getPrototypeOf(obj)   // 取原型
Object.setPrototypeOf(obj, p) // 设原型
Object.defineProperty(obj, k, desc)   // 定义属性
Object.hasOwn(obj, key)      // 自有属性（ES2022）
Object.is(a, b)              // 精确相等（NaN 相等）
```

### 6.2 拷贝的深浅

```javascript
// 浅拷贝：只复制第一层
const copy = { ...original };              // 展开（现代首选）
const copy2 = Object.assign({}, original);

// 深拷贝
const deep = structuredClone(original);    // ⚠️ 现代原生深拷贝（ES2022）
// 旧方案：JSON.parse(JSON.stringify(obj))（有函数/undefined 丢失问题）

// 手写浅拷贝（面试题）
function shallowCopy(obj) {
    return Object.keys(obj).reduce((acc, k) => {
        acc[k] = obj[k];
        return acc;
    }, {});
}
```

> 🎯 **要点**：展开运算符 `{...obj}` 是浅拷贝首选；`structuredClone` 是原生深拷贝（支持循环引用）；JSON 方案有坑（函数/Date/undefined）。

---

> 🎯 **核心要点**：对象体系 = **原型链**（继承机制，方法放原型/状态放实例）+ **class 语法糖**（extends/super/私有 #）+ **继承演进**（寄生组合最优）+ **Object 方法**（keys/entries/assign/clone）。"原型链 + this + 闭包"是 JS 三大底层机制。

---

**返回总览**：[00-JavaScript总览与核心概念](00-JavaScript总览与核心概念.md) | **上一篇**：[02-JavaScript函数与作用域](02-JavaScript函数与作用域.md) | **下一篇**：[04-JavaScript异步编程](04-JavaScript异步编程.md)
