JavaScript详细知识点
一、JavaScript基础核心
1.1 语言概述
JavaScript（简称JS）是一门跨平台、面向对象的脚本语言，主要用于网页交互，可在浏览器和服务器端（Node.js）运行。其核心特性包括：弱类型、动态性、解释性（浏览器/Node.js内置解释器逐行执行代码，无需编译）、单线程（同一时间只能执行一个任务，避免DOM渲染冲突）。
JS的组成部分：ECMAScript（核心语法，规定变量、函数、数据类型等基础规则）、DOM（文档对象模型，操作网页元素）、BOM（浏览器对象模型，操作浏览器窗口）。
1.2 变量与数据类型
1.2.1 变量声明
JS中变量声明有3种方式，核心区别在于作用域和变量提升：
var：函数级作用域，存在变量提升（声明提前到当前作用域顶部，赋值不提前），可重复声明、重复赋值。
let：块级作用域（{}内有效），无变量提升，不可重复声明，可重复赋值，暂存死区（声明前不可使用）。
const：块级作用域，无变量提升，不可重复声明，不可重新赋值（但引用类型的属性可修改），声明时必须赋值。
变量提升示例：console.log(a); // undefined（var声明的变量提升，赋值未提升）; var a = 10;
暂存死区示例：console.log(b); // 报错（let声明的变量无提升，声明前使用报错）; let b = 20;
1.2.2 数据类型（7种，分基本类型和引用类型）
基本数据类型（值类型，存储在栈中，赋值时拷贝值）
Number：数字类型，包含整数、浮点数、NaN（非数字，typeof NaN === 'number'，NaN !== NaN）、Infinity（无穷大）。
String：字符串类型，单引号、双引号、反引号（``）包裹，反引号支持模板字符串（${变量/表达式}）和换行。
Boolean：布尔类型，值为true或false，0、''、null、undefined、NaN会被隐式转为false（ falsy值），其余为true（truthy值）。
Undefined：未定义类型，变量声明未赋值时默认值，typeof undefined === 'undefined'。
Null：空值类型，表示“空对象指针”，typeof null === 'object'（历史bug），常用来主动清空变量。
Symbol：ES6新增，唯一值类型，用于解决变量命名冲突，不能与其他类型运算，可作为对象属性（不可枚举）。
BigInt：ES6新增，用于表示超出Number范围（2^53 - 1）的整数，后缀加n（如10n），不能与Number直接运算。
引用数据类型（存储在堆中，栈中存储堆地址，赋值时拷贝地址）
Object：对象类型，核心是键值对（key: value），key默认是字符串（可省略引号），value可以是任意数据类型。
Array：数组类型，有序集合，索引从0开始，可存储任意数据类型，length属性表示数组长度（可手动修改）。
Function：函数类型，可执行代码块，本质是对象，有name、length等属性，可作为参数、返回值（高阶函数）。
Date：日期类型，用于处理时间和日期，需通过new Date()创建实例。
RegExp：正则表达式类型，用于匹配字符串，通过字面量（/pattern/flags）或new RegExp()创建。
1.2.3 类型转换
隐式转换（自动触发，常见于运算、判断中）
转Boolean：falsy值转false，其余转true。
转Number：字符串转数字（纯数字字符串转对应数字，非纯数字转NaN）；布尔值转数字（true→1，false→0）；null转0，undefined转NaN。
转String：所有类型转字符串，基本类型直接拼接，引用类型转“[object 类型]”（如{}→"[object Object]"）。
显式转换（手动触发）
转Number：Number()（通用）、parseInt()（转整数，忽略非数字开头）、parseFloat()（转浮点数）。
转String：String()（通用）、toString()（基本类型可用，null/undefined不可用）。
转Boolean：Boolean()（通用）。
1.3 运算符
1.3.1 算术运算符
+（加法/字符串拼接）、-（减法）、*（乘法）、/（除法）、%（取余）、++（自增）、--（自减）。
注意：+号两边有一个是字符串，就会触发字符串拼接（如10 + '20' → "1020"）；++/--前置（先运算后赋值）、后置（先赋值后运算）。
1.3.2 赋值运算符
=
1.3.3 比较运算符
==（松散相等，先隐式转换再比较）、===（严格相等，不转换，值和类型都相同才返回true）、!=、!==、>、<、>=、<=。
示例：10 == '10' → true（隐式转数字）；10 === '10' → false（类型不同）；null == undefined → true；null === undefined → false。
1.3.4 逻辑运算符
&&（逻辑与，短路运算：左边为false，右边不执行，返回左边值；左边为true，返回右边值）。
||（逻辑或，短路运算：左边为true，右边不执行，返回左边值；左边为false，返回右边值）。
!（逻辑非，取反，将值转为Boolean后再取反，!!可用于快速转Boolean）。
1.3.5 三元运算符
语法：条件 ? 表达式1 : 表达式2；条件为true，执行表达式1，否则执行表达式2（可嵌套，但不建议嵌套过深）。
1.3.6 其他运算符
typeof：检测数据类型，返回字符串（注意：typeof null === 'object'，typeof function === 'function'）。
instanceof：检测引用类型的原型链，判断A是否是B的实例（A instanceof B），只能检测引用类型。
delete：删除对象的属性（不能删除var声明的变量、函数声明），删除数组元素会留下空占位符（不改变length）。
1.4 流程控制语句
1.4.1 条件语句
if-else：单条件判断，if后条件为true执行代码块，否则执行else代码块；可嵌套if-else if-else处理多条件。
switch-case：多条件匹配，case后值与switch后表达式严格相等（===）时执行对应代码块，break用于终止case（否则穿透），default处理未匹配的情况。
1.4.2 循环语句
for循环：适合已知循环次数，语法：for(初始化变量; 循环条件; 变量更新) { 循环体 }
while循环：适合未知循环次数，语法：while(循环条件) { 循环体 }（条件为true时执行，注意避免死循环）。
do-while循环：先执行一次循环体，再判断条件，语法：do { 循环体 } while(循环条件)（至少执行一次）。
for-in循环：遍历对象的可枚举属性（包括原型上的属性），语法：for(var key in 对象) { 循环体 }（不建议用于数组，会遍历索引和原型属性）。
for-of循环：ES6新增，遍历可迭代对象（数组、字符串、Map、Set等），语法：for(var value of 可迭代对象) { 循环体 }（只遍历值，不遍历原型属性）。
1.4.3 跳转语句
break：终止当前循环或switch语句，跳出整个代码块。
continue：跳过当前循环的剩余代码，进入下一次循环。
return：用于函数中，返回函数值并终止函数执行（无return时，函数默认返回undefined）。
二、函数详解
2.1 函数的声明与调用
2.1.1 函数声明
函数声明式：function 函数名(参数列表) { 函数体 }（存在函数提升，可在声明前调用）。
函数表达式：var 函数名 = function(参数列表) { 函数体 }（无函数提升，必须在声明后调用）。
箭头函数（ES6新增）：(参数列表) => { 函数体 }（简洁语法，无this绑定，无arguments，不能作为构造函数）。
2.1.2 函数调用
普通调用：函数名(参数)（最常用，this指向全局对象window/global，严格模式下指向undefined）。
作为对象方法调用：对象.函数名(参数)（this指向当前对象）。
构造函数调用：new 函数名(参数)（this指向新创建的实例对象，函数内部需用this定义属性/方法）。
apply/call/bind调用：改变this指向，apply（参数以数组传递）、call（参数逐个传递）、bind（返回新函数，不立即执行）。
2.2 函数的参数与返回值
2.2.1 参数
JS函数参数是“形参”，调用时传递“实参”，形参和实参数量可不一致（实参少于形参，未传递的形参为undefined；实参多于形参，可通过arguments获取）。
ES6新增：默认参数（function fn(a = 10) {}）、剩余参数（function fn(...args) {}，接收剩余实参，返回数组）、解构参数（function fn({name, age}) {}，解构对象/数组作为参数）。
arguments：函数内部的伪数组，存储所有实参，只能在普通函数中使用（箭头函数无arguments）。
2.2.2 返回值
通过return语句返回函数执行结果，return后代码不再执行；无return时，函数默认返回undefined；return可返回任意数据类型（包括函数）。
2.3 函数的作用域与闭包
2.3.1 作用域
作用域：变量/函数的可访问范围，分为3类：
全局作用域：最外层作用域，变量/函数可在整个代码中访问（var声明的全局变量会挂载到window上）。
函数作用域：函数内部的作用域，变量/函数只能在函数内部访问（函数嵌套时，内层可访问外层，外层不可访问内层）。
块级作用域：ES6新增，由{}包裹（if、for、let/const声明的变量），变量只能在块内访问。
作用域链：内层作用域访问变量时，先找自身作用域，找不到则向上找外层作用域，直到全局作用域（找不到则报错）。
2.3.2 闭包
定义：函数嵌套时，内层函数引用了外层函数的变量/参数，且内层函数被外部引用，此时外层函数的作用域不会被销毁，这种“内层函数+外层函数的变量”的组合就是闭包。
核心作用：延长变量的生命周期、实现私有变量（外部无法直接访问外层函数的变量，只能通过内层函数访问）。
注意：闭包会导致内存泄漏（外层函数变量无法被垃圾回收），需合理使用（用完后手动置为null）。
2.4 函数的this指向
this的指向是“动态的”，取决于函数的调用方式（箭头函数除外，箭头函数的this指向定义时的外层this，而非调用时）：
普通调用：this → window（非严格模式）/ undefined（严格模式）。
对象方法调用：this → 调用方法的对象。
构造函数调用：this → 新创建的实例对象。
apply/call/bind调用：this → 第一个参数（指定的对象）。
箭头函数：this → 定义时的外层作用域的this（固定不变，不受调用方式影响）。
三、数组详解
3.1 数组的创建
字面量方式：var arr = [1, 2, 3];（最常用）。
构造函数方式：var arr = new Array(3);（创建长度为3的空数组）、var arr = new Array(1, 2, 3);（创建包含指定元素的数组）。
ES6新增：Array.of(1, 2, 3)（创建包含指定元素的数组，解决new Array的歧义）、Array.from(可迭代对象)（将类数组/可迭代对象转为数组，如Array.from(document.querySelectorAll('div'))）。
3.2 数组的核心方法
3.2.1 增删改查（改变原数组）
push()：向数组末尾添加一个/多个元素，返回新长度。
pop()：删除数组末尾元素，返回被删除的元素。
unshift()：向数组开头添加一个/多个元素，返回新长度。
shift()：删除数组开头元素，返回被删除的元素。
splice()：万能方法，可增、删、改；语法：splice(起始索引, 删除个数, 新增元素)，返回被删除的元素组成的数组。
fill()：用指定值填充数组，语法：fill(值, 起始索引, 结束索引)，改变原数组。
3.2.2 数组遍历（不改变原数组）
forEach()：遍历数组，对每个元素执行回调函数，无返回值（无法中断遍历）。
map()：遍历数组，对每个元素执行回调函数，返回新数组（新数组长度与原数组一致，元素为回调函数的返回值）。
filter()：遍历数组，筛选出满足回调函数条件的元素，返回新数组（新数组长度≤原数组）。
find()：遍历数组，返回第一个满足回调函数条件的元素（找不到返回undefined）。
findIndex()：遍历数组，返回第一个满足回调函数条件的元素的索引（找不到返回-1）。
every()：判断数组所有元素是否都满足回调函数条件，返回Boolean（全满足返回true，否则false）。
some()：判断数组是否有至少一个元素满足回调函数条件，返回Boolean（有一个满足返回true，否则false）。
reduce()：累加器，遍历数组，将数组元素逐步累加为一个值，返回最终累加结果；语法：reduce(回调函数(累计值, 当前值, 索引, 数组), 初始值)。
3.2.3 数组排序与反转（改变原数组）
sort()：对数组元素排序，默认按字符串Unicode编码排序（需传入回调函数实现数字排序：arr.sort((a, b) => a - b) 升序，b - a 降序）。
reverse()：反转数组元素顺序，改变原数组。
3.2.4 数组拼接与截取（不改变原数组）
concat()：拼接两个/多个数组，返回新数组（不改变原数组）。
slice()：截取数组片段，语法：slice(起始索引, 结束索引)（结束索引不包含），返回新数组（不改变原数组）；起始索引为负数表示从末尾开始（如slice(-2)截取最后2个元素）。
3.2.5 其他常用方法
join()：将数组元素转为字符串，用指定分隔符连接，返回字符串（不改变原数组）；默认用逗号分隔，join('')无分隔符。
indexOf()：查找指定元素在数组中的第一个索引（找不到返回-1）；lastIndexOf()：查找最后一个索引。
includes()：判断数组是否包含指定元素，返回Boolean（ES6新增）。
flat()：扁平化数组，语法：flat(层级)（如flat(1)扁平化1层嵌套数组，flat(Infinity)扁平化所有层级），返回新数组。
flatMap()：先map再flat(1)，返回新数组（ES6新增）。
四、对象详解
4.1 对象的创建
字面量方式：var obj = { name: '张三', age: 18 };（最常用）。
构造函数方式：var obj = new Object(); obj.name = '张三';（创建空对象，再添加属性）。
工厂模式：function createObj(name, age) { return { name, age }; }（批量创建对象，无原型关联）。
自定义构造函数：function Person(name, age) { this.name = name; this.age = age; } var obj = new Person('张三', 18);（有原型关联，可实现继承）。
ES6新增：class类（语法糖，本质是构造函数）：class Person { constructor(name, age) { this.name = name; this.age = age; } }。
4.2 对象的属性与方法
4.2.1 属性的访问与修改
访问：obj.name（点语法，key必须是合法标识符）、obj['name']（方括号语法，key可以是变量、特殊字符）。
修改：obj.name = '李四'（直接赋值）、obj['age'] = 20；新增属性：obj.gender = '男'；删除属性：delete obj.gender。
4.2.2 属性的特性（ES5新增）
通过Object.defineProperty(obj, '属性名', { 特性 }) 定义属性特性，常用特性：
value：属性值（默认undefined）。
writable：是否可修改（默认true，false则属性值不可修改）。
enumerable：是否可枚举（默认true，false则for-in、Object.keys()无法遍历）。
configurable：是否可删除、可修改特性（默认true，false则不可删除、不可修改特性）。
存取器属性（getter/setter）：通过get和set方法控制属性的访问和修改，如Object.defineProperty(obj, 'age', { get() { return this._age; }, set(val) { this._age = val; } })。
4.2.3 对象的方法
对象的方法就是属性值为函数的属性，如：var obj = { sayHi: function() { console.log('Hi'); } }; 调用：obj.sayHi()。
ES6简写：var obj = { sayHi() { console.log('Hi'); } }（省略function关键字）。
4.3 对象的常用方法
Object.keys(obj)：返回对象可枚举属性的key组成的数组。
Object.values(obj)：返回对象可枚举属性的value组成的数组（ES6新增）。
Object.entries(obj)：返回对象可枚举属性的[key, value]数组（ES6新增），可用于for-of遍历。
Object.assign(target, ...sources)：将多个源对象的属性复制到目标对象，返回目标对象（浅拷贝，只拷贝一层属性）。
Object.freeze(obj)：冻结对象，对象不可新增、删除、修改属性（浅冻结）。
Object.is(a, b)：判断两个值是否严格相等，解决===的bug（如Object.is(NaN, NaN) → true，Object.is(+0, -0) → false）。
4.4 原型与原型链
4.4.1 原型（prototype）
每个函数（构造函数）都有一个prototype属性，指向一个对象（原型对象）；原型对象上的属性和方法，会被该构造函数的所有实例继承。
实例对象通过__proto__属性（非标准，标准方法是Object.getPrototypeOf(实例)）指向其构造函数的prototype。
4.4.2 原型链
当访问实例对象的属性/方法时，先找自身属性/方法，找不到则通过__proto__找原型对象的属性/方法，再找不到则找原型对象的原型（Object.prototype），直到null（Object.prototype.__proto__ = null），这个链式结构就是原型链。
核心作用：实现继承（实例继承构造函数原型上的属性和方法）。
4.5 继承方式
原型链继承：将子类构造函数的prototype指向父类的实例（Child.prototype = new Parent()），缺点：子类实例共享父类原型的属性（引用类型会相互影响）。
构造函数继承：在子类构造函数中调用父类构造函数（Parent.call(this, 参数)），缺点：无法继承父类原型上的方法。
组合继承：结合原型链继承和构造函数继承（子类构造函数调用父类构造函数，子类prototype指向父类实例），缺点：父类构造函数被调用两次。
寄生组合继承：优化组合继承，通过Object.create(Parent.prototype)创建父类原型的副本，赋值给子类prototype，避免父类构造函数被调用两次（最推荐的原生继承方式）。
ES6类继承：通过class Child extends Parent {} 实现继承，constructor中用super()调用父类构造函数（语法糖，底层还是原型链）。
五、ES6及以上新增特性
5.1 变量与解构赋值
5.1.1 let/const（已在基础部分提及，补充细节）
let/const块级作用域，解决var的变量提升、作用域混乱问题；const声明的引用类型，仅保证地址不变，属性可修改（如const obj = {a:1}; obj.a = 2; 合法）。
5.1.2 解构赋值
快速从数组/对象中提取值，赋值给变量，简化代码：
数组解构：var [a, b, c] = [1, 2, 3];（a=1, b=2, c=3）；可设置默认值（var [a=0, b] = [undefined, 2];）；可跳过元素（var [a, , c] = [1, 2, 3];）。
对象解构：var {name, age} = {name: '张三', age: 18};（name='张三', age=18）；可重命名（var {name: myName} = {name: '张三'};）；可设置默认值（var {gender='男'} = {name: '张三'};）。
其他解构：字符串解构（var [a, b] = 'ab';）、函数参数解构（function fn({name, age}) {}）。
5.2 字符串新增方法
includes(str, start)：判断字符串是否包含指定子串，返回Boolean（start为起始索引，可选）。
startsWith(str, start)：判断字符串是否以指定子串开头，返回Boolean。
endsWith(str, length)：判断字符串是否以指定子串结尾，返回Boolean（length为字符串长度，可选）。
repeat(n)：将字符串重复n次，返回新字符串（n为非负整数）。
padStart(length, str)：在字符串开头补全指定字符，使字符串长度达到length，返回新字符串。
padEnd(length, str)：在字符串结尾补全指定字符，使字符串长度达到length，返回新字符串。
trimStart()/trimEnd()：去除字符串开头/结尾的空白字符，返回新字符串（trim()去除两端空白）。
5.3 函数新增特性
箭头函数（已提及，补充：无this、无arguments、无prototype，不能作为构造函数，适合简写回调函数）。
默认参数（function fn(a=10, b=20) {}，默认参数可引用其他参数）。
剩余参数（function fn(...args) {}，接收剩余实参，返回数组，替代arguments）。
函数参数默认值与解构结合（function fn({name='张三', age=18} = {}) {}，避免参数为undefined时报错）。
5.4 数组新增方法
已在数组部分提及（Array.of、Array.from、flat、flatMap、includes等），补充：Array.prototype.keys()（返回索引迭代器）、Array.prototype.values()（返回值迭代器）、Array.prototype.entries()（返回[key, value]迭代器）。
5.5 对象新增特性
对象字面量简写：属性简写（var name='张三'; var obj={name}; 等价于{name: name}）、方法简写（{sayHi() {}} 等价于{sayHi: function(){}}）。
计算属性名：var key='name'; var obj={[key]: '张三'};（属性名由表达式计算得出）。
Object.assign、Object.values、Object.entries、Object.is（已提及）。
Object.getOwnPropertyDescriptors(obj)：获取对象所有属性的特性描述符（ES6新增）。
5.6 类（class）与模块（module）
5.6.1 class类
ES6语法糖，本质是构造函数，简化对象创建和继承：
基本语法：class Person { constructor(name, age) { this.name = name; this.age = age; } sayHi() { console.log('Hi'); } }
继承：class Student extends Person { constructor(name, age, score) { super(name, age); this.score = score; } }
静态方法：用static关键字定义，只能通过类调用，不能通过实例调用（class Person { static fn() {} } → Person.fn()）。
私有属性/方法：用#开头定义，只能在类内部访问（class Person { #age = 18; #sayHi() {} }，ES2022新增）。
5.6.2 模块（module）
ES6新增模块系统，实现代码模块化（拆分代码，按需导入导出），解决全局变量污染问题：
导出（export）：export const name = '张三';（导出单个变量）、export default function() {}（默认导出，一个模块只能有一个默认导出）、export { name, age }（批量导出）。
导入（import）：import { name, age } from './module.js';（导入指定导出）、import fn from './module.js';（导入默认导出）、import * as module from './module.js';（导入所有导出，挂载到module对象上）。
5.7 其他新增特性
Set：集合类型，存储唯一值（不允许重复），常用方法：add()、delete()、has()、clear()、size（属性），可用于数组去重（[...new Set(arr)]）。
Map：映射类型，键值对集合（key可以是任意数据类型，而非仅字符串），常用方法：set()、get()、delete()、has()、clear()、size（属性）。
Promise：异步编程解决方案，解决回调地狱，有三种状态：pending（等待中）、fulfilled（成功）、rejected（失败），常用方法：then()（成功回调）、catch()（失败回调）、finally()（无论成功失败都执行）。
async/await：ES7新增，基于Promise的语法糖，使异步代码看起来像同步代码（async函数返回Promise，await只能在async函数中使用，等待Promise完成）。
Symbol（已提及）、BigInt（已提及）。
六、异步编程
6.1 异步的概念
JS是单线程，同步代码按顺序执行，异步代码（如定时器、AJAX、事件回调）不会阻塞主线程，会被放入“任务队列”，待主线程同步代码执行完毕后，再依次执行任务队列中的异步代码。
常见异步场景：setTimeout、setInterval、AJAX请求、DOM事件回调、Promise、async/await。
6.2 任务队列（宏任务与微任务）
异步任务分为宏任务和微任务，执行顺序：主线程同步代码 → 微任务队列全部执行 → 宏任务队列执行一个 → 微任务队列全部执行 → 循环。
6.2.1 宏任务（macrotask）
常见类型：setTimeout、setInterval、setImmediate（Node.js）、I/O操作、DOM渲染。
特点：执行优先级低，每次执行一个宏任务后，会先执行所有微任务。
6.2.2 微任务（microtask）
常见类型：Promise.then/catch/finally、process.nextTick（Node.js）、queueMicrotask()。
特点：执行优先级高，主线程同步代码执行完毕后，先执行所有微任务，再执行宏任务。
6.3 异步编程方案演进
回调函数：最基础的异步方案，如setTimeout(() => {}, 1000)，缺点：回调嵌套过深（回调地狱），代码可读性差。
Promise：解决回调地狱，将异步操作封装为Promise对象，通过then()链式调用，缺点：仍需链式调用，代码不够简洁。
async/await：ES7新增，基于Promise，使异步代码同步化，代码简洁、可读性高，是目前最推荐的异步方案。
6.4 Promise详解
6.4.1 Promise的创建
语法：new Promise((resolve, reject) => { 异步操作；成功时调用resolve(结果)；失败时调用reject(错误信息)； })。
Promise实例有三个状态，状态一旦改变，不可再变：
pending：初始状态，等待异步操作完成。
fulfilled：成功状态，调用resolve()后触发，执行then()回调。
rejected：失败状态，调用reject()后触发，执行catch()回调。
6.4.2 Promise的常用方法
then()：接收两个参数（成功回调、失败回调），返回新的Promise对象，可链式调用。
catch()：接收失败回调，等价于then(null, 失败回调)，捕获Promise的错误（包括then()中的错误）。
finally()：无论Promise成功还是失败，都会执行，返回新的Promise对象（不改变Promise的状态）。
Promise.resolve()：快速创建一个成功状态的Promise对象。
Promise.reject()：快速创建一个失败状态的Promise对象。
Promise.all()：接收一个Promise数组，所有Promise都成功，才返回成功结果数组；只要有一个失败，就返回第一个失败的错误信息（并行执行）。
Promise.race()：接收一个Promise数组，返回第一个完成的Promise的结果（无论成功还是失败，竞争机制）。
Promise.allSettled()：接收一个Promise数组，所有Promise都完成（无论成功还是失败），返回每个Promise的结果对象（包含status和value/reason）。
6.5 async/await详解
async关键字：修饰函数，使函数返回一个Promise对象；函数内部return的值，会成为Promise的成功结果；函数内部抛出的错误，会成为Promise的失败原因。
await关键字：只能在async函数内部使用，等待一个Promise对象完成；await后面跟Promise，会暂停async函数的执行，直到Promise完成（成功则返回结果，失败则抛出错误）。
错误处理：async/await的错误可以用try-catch捕获（try { await 异步操作; } catch (err) { 处理错误; }）。
七、DOM操作
7.1 DOM概述
DOM（文档对象模型）是浏览器将HTML文档解析成的树形结构（DOM树），每个HTML元素都是一个DOM节点（元素节点、文本节点、属性节点、注释节点等），JS通过DOM操作实现网页交互。
7.2 DOM节点的获取
getElementById(id)：通过id获取单个元素节点（唯一，id重复时返回第一个）。
getElementsByClassName(className)：通过类名获取元素节点集合（HTMLCollection，伪数组，实时更新）。
getElementsByTagName(tagName)：通过标签名获取元素节点集合（HTMLCollection，伪数组）。
querySelector(selector)：通过CSS选择器获取单个元素节点（返回第一个匹配的元素）。
querySelectorAll(selector)：通过CSS选择器获取元素节点集合（NodeList，伪数组，不实时更新）。
其他获取方式：document.body（body节点）、document.head（head节点）、document.documentElement（html节点）。
7.3 DOM节点的操作
7.3.1 节点的创建与插入
createElement(tagName)：创建元素节点（如document.createElement('div')）。
createTextNode(text)：创建文本节点（如document.createTextNode('Hello')）。
appendChild(child)：将子节点添加到父节点的末尾，返回子节点。
insertBefore(newNode, referenceNode)：将新节点插入到参考节点之前，返回新节点。
append()：将多个节点/文本添加到父节点末尾（可添加多个参数，支持文本）。
prepend()：将多个节点/文本添加到父节点开头（可添加多个参数，支持文本）。
7.3.2 节点的删除与替换
removeChild(child)：删除父节点的子节点，返回被删除的节点。
remove()：删除当前节点（无需父节点，直接调用）。
replaceChild(newNode, oldNode)：用新节点替换旧节点，返回被替换的旧节点。
7.3.3 节点的克隆
cloneNode(deep)：克隆节点，deep为true时，克隆节点及其所有子节点；deep为false时，只克隆当前节点（不克隆子节点）。
7.4 DOM属性与样式操作
7.4.1 属性操作
获取属性：element.getAttribute(attrName)（获取自定义属性和原生属性）、element.attrName（获取原生属性，如element.src）。
设置属性：element.setAttribute(attrName, attrValue)（设置自定义属性和原生属性）、element.attrName = attrValue（设置原生属性）。
删除属性：element.removeAttribute(attrName)。
自定义属性：HTML5中自定义属性需以data-开头（如data-id），可通过element.dataset.id获取（简化getAttribute('data-id')）。
7.4.2 样式操作
内联样式操作：element.style.cssProperty（如element.style.color = 'red'，注意CSS属性用驼峰命名，如backgroundColor）。
类样式操作：element.className = 'class1 class2'（覆盖原有类）、element.classList.add('class1')（添加类）、element.classList.remove('class1')（删除类）、element.classList.toggle('class1')（切换类，有则删，无则加）、element.classList.contains('class1')（判断是否包含类）。
获取计算样式：window.getComputedStyle(element)（获取元素最终渲染的样式，只读，不能修改）。
7.5 DOM事件
7.5.1 事件的绑定与解绑
行内绑定：<button onclick="fn()">点击</button>（不推荐，耦合度高）。
DOM0级绑定：element.onclick = function() {}（只能绑定一个回调函数，解绑：element.onclick = null）。
DOM2级绑定：element.addEventListener('click', fn, false)（可绑定多个回调函数，第三个参数false表示冒泡阶段触发，true表示捕获阶段触发）；解绑：element.removeEventListener('click', fn)（必须传入同一个函数引用）。
7.5.2 事件流
事件流分为三个阶段（从外到内再到外）：
捕获阶段：事件从最外层节点（html）向目标节点传播。
目标阶段：事件到达目标节点，执行目标节点的事件回调。
冒泡阶段：事件从目标节点向最外层节点传播（默认触发，可通过event.stopPropagation()阻止冒泡）。
7.5.3 事件对象（event）
事件回调函数的第一个参数为事件对象，包含事件相关信息：
event.target：触发事件的目标节点（最具体的节点）。
event.currentTarget：绑定事件的节点（当前执行回调的节点）。
event.stopPropagation()：阻止事件冒泡和捕获。
event.preventDefault()：阻止事件的默认行为（如a标签跳转、表单提交）。
event.type：事件类型（如'click'、'mouseover'）。
event.clientX/clientY：事件触发时，鼠标相对于浏览器可视区域的坐标。
7.5.4 常见事件类型
鼠标事件：click（点击）、dblclick（双击）、mouseover（鼠标移入）、mouseout（鼠标移出）、mousemove（鼠标移动）、mousedown（鼠标按下）、mouseup（鼠标松开）。
键盘事件：keydown（键盘按下）、keyup（键盘松开）、keypress（键盘按下并松开，不识别功能键）。
表单事件：input（输入框内容变化）、change（表单元素内容改变且失去焦点）、submit（表单提交）、blur（失去焦点）、focus（获得焦点）。
文档事件：load（页面完全加载完成）、DOMContentLoaded（DOM树加载完成，无需等待图片、样式等资源）。
八、BOM操作
8.1 BOM概述
BOM（浏览器对象模型）是浏览器提供的一套操作浏览器窗口的API，核心对象是window（全局对象，所有全局变量、函数、对象都挂载在window上）。
8.2 window对象的核心属性与方法
8.2.1 核心属性
window.innerWidth/window.innerHeight：浏览器可视区域的宽度/高度（不含滚动条）。
window.outerWidth/window.outerHeight：浏览器窗口的宽度/高度（含边框、滚动条）。
window.screen：屏幕对象，包含屏幕相关信息（screen.width/screen.height：屏幕分辨率）。
window.location：地址栏对象，控制浏览器地址。
window.history：历史记录对象，控制浏览器历史记录。
window.navigator：浏览器信息对象，获取浏览器相关信息（如navigator.userAgent：浏览器UA，用于判断浏览器类型）。
