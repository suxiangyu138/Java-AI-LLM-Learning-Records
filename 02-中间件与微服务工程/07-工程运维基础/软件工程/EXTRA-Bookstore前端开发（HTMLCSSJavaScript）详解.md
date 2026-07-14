Bookstore前端开发（HTML/CSS/JavaScript）详解
一、HTML（超文本标记语言）—— 网页的骨架
1.1 核心定位
HTML是纯标记语言，无逻辑处理能力，核心作用是定义Bookstore前端页面的结构和内容，相当于搭建书店页面的“骨架”，所有可见元素（登录框、书籍列表、按钮等）都通过HTML标签组织，为后续CSS美化、JavaScript交互提供基础。
1.2 核心语法
标签分类：分为双标签（需成对闭合，如<div></div>、<p></p>）和单标签（自闭合，无需结束标签，如<img/>、<input/>），Bookstore开发中需规范书写自闭合标签的“/”，提升代码可读性。
标签嵌套：遵循“嵌套不交叉”原则，例如正确写法：<div><p>书籍名称</p></div>，错误写法：<div><p>书籍名称</div></p>。
属性补充：标签通过属性扩展功能，格式为“属性名="属性值"”，核心常用属性包括id（唯一标识）、class（样式类名）、src（图片/文件路径）、href（链接地址）等，是后续CSS和JS操作元素的关键。
1.3 Bookstore场景常用标签（重点扩展）
（1）结构与布局标签
<html></html>：根标签，包裹整个页面，需添加lang="zh-CN"属性，指定页面语言为中文。
<head></head>：页面头部，存放元信息，核心包含<meta charset="UTF-8">（避免中文乱码）、<title>书店登录页</title>（页面标题）、<link>（引入CSS文件）。
<body></body>：页面主体，所有可见内容均放在此标签内。
<header></header>：书店页面头部，用于放置logo、导航栏（首页、书籍列表、购物车、我的订单）。
<footer></footer>：页面底部，放置书店版权信息、联系方式、备案号等。
<nav></nav>：导航标签，专门包裹书店导航链接，如“首页 → 小说类 → 科幻小说 → 购物车”。
<div></div>：块级布局标签，用于划分页面区域（如登录表单区域、书籍列表区域、购物车卡片区域），是Bookstore布局最常用的标签。
<span></span>：行内标签，用于包裹小块文本（如书籍价格、折扣标签、购物车数量提示），不独占一行。
（2）内容标签
<h1>-<h6>：标题标签，h1用于页面主标题（如“XX书店 - 会员登录”），h2用于区域标题（如“热门书籍推荐”），h3用于书籍名称，层级清晰，提升页面可读性。
<p></p>：段落标签，用于展示书籍简介、书店公告、用户须知等文本内容。
<a></a>：链接标签，核心属性href，用于实现页面跳转（如从登录页跳转到首页、从书籍列表跳转到书籍详情页），搭配target="_blank"可在新窗口打开链接，Bookstore中常用作“查看详情”“立即购买”链接。
<img/>：图片标签，核心属性src（图片路径）、alt（图片加载失败提示），用于展示书店logo、书籍封面、活动海报，路径可使用相对路径（如"img/book1.jpg"）或绝对路径。
<ul><li></li></ul>：无序列表，用于展示书籍列表、分类导航（如“书籍分类：小说、科技、历史、少儿”），每个列表项对应一本书籍或一个分类。
（3）表单标签（Bookstore核心高频）
主要用于用户登录、注册、下单、搜索书籍等场景，核心标签为<form>，内部嵌套各类表单控件：
<form></form>：表单容器，核心属性action（提交后端接口地址）、method（提交方式，get/post），Bookstore中登录、下单表单常用method="post"，保障数据安全。
<input/>：表单控件，通过type属性切换类型，Bookstore常用类型：
type="text"：用户名、书籍搜索输入框；
type="password"：登录密码输入框，输入内容隐藏；
type="submit"：提交按钮（如“登录”“下单”）；
type="button"：普通按钮（如“清空购物车”“重置表单”）；
type="checkbox"：复选框（如“勾选同意用户协议”“批量选择书籍”）。
<label></label>：标签关联控件，点击label文本可聚焦对应输入框，提升用户体验（如<label for="username">用户名：</label>，搭配<input id="username">）。
<select><option></option></select>：下拉选择框，用于书籍分类筛选（如“选择分类：小说、科技、历史”）、收货地址选择等。
<textarea></textarea>：多行文本域，用于用户留言、订单备注等场景。
（4）表格标签（书籍列表展示专用）
用于规范展示书籍列表、订单列表，核心标签：<table>（表格容器）、<tr>（表格行）、<th>（表头单元格，如“书籍名称、价格、库存”）、<td>（普通单元格，存放具体数据），可通过border属性设置边框，提升表格清晰度。
1.4 Bookstore场景核心作用
构建核心页面结构：登录页、书籍列表页、书籍详情页、购物车页、订单页、用户中心页的基础骨架。
承载用户交互入口：通过表单收集用户登录信息、下单信息，通过链接和按钮提供页面跳转、操作入口。
展示核心数据：书籍名称、价格、封面、库存、订单信息等，为用户提供直观的浏览和操作对象。
二、CSS（层叠样式表）—— 网页的外观
2.1 核心定位
CSS用于控制HTML元素的样式和布局，相当于给Bookstore的“骨架”穿上“漂亮的衣服”，核心作用是美化页面，提升用户视觉体验，使页面布局合理、风格统一（如书店整体采用温馨、简约的风格）。
2.2 引入方式（按推荐度排序）
外部样式：创建独立的.css文件（如bookstore.css），通过<link rel="stylesheet" href="bookstore.css">引入HTML页面，是Bookstore开发的首选方式，便于样式复用和后期维护（修改一个CSS文件，所有引用该文件的页面都会同步更新）。
内部样式：在HTML的<head>标签内添加<style></style>，将样式写在标签内部，适用于单个页面的样式设置，无需单独创建CSS文件，适合简单页面或临时样式调整。
行内样式：直接在HTML标签内添加style属性（如<div style="color: red;">），优先级最高，但样式无法复用，且会导致HTML和CSS混淆，仅用于临时调试或单个元素的特殊样式设置，Bookstore开发中尽量避免大量使用。
2.3 核心语法
基础语法：选择器 { 属性: 值; 属性: 值; ... }
选择器：用于定位需要设置样式的HTML元素，Bookstore常用选择器：
标签选择器：直接使用HTML标签名（如div、p、input），用于统一设置某类元素的样式（如所有p标签的字体大小）。
id选择器：以“#”开头，对应HTML元素的id属性（如#loginBtn），用于设置单个元素的独特样式（id唯一，不可重复）。
类选择器：以“.”开头，对应HTML元素的class属性（如.form-item），可重复使用，用于设置多个元素的统一样式（如所有表单项的间距）。
后代选择器：通过空格连接多个选择器（如.nav li），用于设置某元素后代的样式（如导航栏下的所有列表项）。
样式优先级：行内样式 > id选择器 > 类选择器 > 标签选择器，优先级高的样式会覆盖优先级低的样式，若样式冲突，遵循“就近原则”（距离元素越近，优先级越高）。
2.4 Bookstore场景核心功能与常用属性
（1）核心功能
布局排版：使用flex（弹性布局）、grid（网格布局）实现书籍列表的整齐排列、页面分栏（如左侧分类导航、右侧书籍展示），替代传统float布局，更灵活、易维护。
美化元素：设置按钮、表单、书籍卡片的样式，提升视觉体验（如按钮圆角、卡片阴影、hover效果）。
响应式适配：通过媒体查询（@media）适配手机、平板、电脑等不同屏幕尺寸，确保Bookstore在移动端和PC端都能正常显示（如移动端书籍列表单列显示，PC端双列/三列显示）。
（2）常用属性（按功能分类）
布局属性：
width/height：设置元素宽度和高度（如书籍卡片宽度200px、表单宽度300px）；
margin：元素外部间距（如书籍卡片之间的间距、表单与页面顶部的间距）；
padding：元素内部间距（如表单输入框内的上下左右间距、书籍卡片内文字与边框的间距）；
display：控制元素显示方式（block：块级元素，独占一行；inline：行内元素，不独占一行；flex：弹性布局）；
justify-content/align-items：flex布局的核心属性，用于控制元素水平、垂直居中（如书籍列表水平均匀分布、按钮文字垂直居中）。
外观属性：
color：文本颜色（如书籍名称颜色#333、价格颜色#ff0000）；
background：背景（如按钮背景色、页面背景色、书籍卡片背景色），可设置纯色、图片；
font-size：字体大小（如书籍名称16px、简介14px）；
text-align：文本对齐方式（left：左对齐、center：居中、right：右对齐，如书籍名称居中、表单标签左对齐）；
border：边框（如书籍卡片边框、表单输入框边框），可设置边框宽度、颜色、样式；
border-radius：圆角（如按钮圆角、书籍卡片圆角，提升美观度）；
box-shadow：阴影（如书籍卡片添加阴影，增强立体感）；
hover：伪类，设置元素鼠标悬停时的样式（如按钮悬停时背景色变化、书籍卡片悬停时阴影加深）。
2.5 Bookstore场景样式示例（扩展）
/* 外部CSS文件：bookstore.css */
/* 页面整体样式 */
body {
    margin: 0;
    padding: 0;
    font-family: "微软雅黑", sans-serif; /* 统一字体，提升阅读体验 */
    background-color: #f5f5f5; /* 页面背景色，简约柔和 */
}
/* 头部导航样式 */
header {
    width: 100%;
    height: 60px;
    background-color: #fff;
    box-shadow: 0 2px 5px rgba(0,0,0,0.1); /* 轻微阴影，增强层次感 */
}
/* 导航栏样式 */
nav {
    height: 100%;
    display: flex;
    align-items: center;
    padding: 0 50px;
}
nav ul {
    list-style: none; /* 去掉列表默认圆点 */
    display: flex;
    gap: 30px; /* 列表项之间的间距 */
}
nav a {
    text-decoration: none; /* 去掉链接下划线 */
    color: #333;
    font-size: 16px;
}
nav a:hover {
    color: #0088ff; /* 导航链接悬停变色 */
}
/* 书籍卡片样式 */
.book-card {
    width: 220px;
    height: 350px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    padding: 15px;
    margin: 15px;
    transition: all 0.3s; /* 过渡效果，悬停时平滑变化 */
}
.book-card:hover {
    box-shadow: 0 5px 15px rgba(0,0,0,0.15); /* 悬停时阴影加深 */
    transform: translateY(-5px); /* 悬停时轻微上移，增强交互感 */
}
.book-card img {
    width: 100%;
    height: 200px;
    object-fit: cover; /* 图片自适应，不变形 */
    border-radius: 4px;
}
.book-card h3 {
    font-size: 16px;
    margin: 10px 0;
    white-space: nowrap; /* 书籍名称不换行 */
    overflow: hidden; /* 超出部分隐藏 */
    text-overflow: ellipsis; /* 超出部分显示省略号 */
}
.book-card .price {
    color: #ff0000;
    font-weight: bold;
    font-size: 18px;
}
/* 表单样式 */
.form-item {
    margin: 15px 0;
    display: flex;
    align-items: center;
    gap: 10px;
}
.form-item label {
    width: 80px;
    text-align: right;
    font-size: 14px;
    color: #666;
}
.input {
    width: 200px;
    height: 30px;
    padding: 0 5px;
    border: 1px solid #ddd;
    border-radius: 4px;
    outline: none; /* 去掉输入框聚焦时的默认边框 */
}
.input:focus {
    border-color: #0088ff; /* 输入框聚焦时边框变色 */
}

# oginBtn {
    width: 212px;
    height: 35px;
    background: #0088ff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer; /* 鼠标悬停时显示手型 */
    transition: background 0.3s;
}

# oginBtn:hover {
    background: #0066cc; /* 悬停时背景色加深 */
}
/* 响应式适配（移动端） */
@media (max-width: 768px) {
    nav {
        padding: 0 20px;
    }
    nav ul {
        gap: 15px;
    }
    .book-card {
        width: 100%;
        margin: 10px 0;
    }
    .form-item {
        flex-direction: column;
        align-items: flex-start;
    }
    .form-item label {
        text-align: left;
        margin-bottom: 5px;
    }
}
三、JavaScript（脚本语言）—— 网页的行为
3.1 核心定位
JavaScript是解释型、弱类型脚本语言，核心作用是为Bookstore页面赋予交互能力和动态逻辑，相当于给页面“注入灵魂”，处理用户操作（点击、输入、提交），动态修改HTML/CSS，实现数据交互，是连接前端页面与后端接口的关键。
3.2 核心特性
解释型语言：无需编译，浏览器直接解析执行，开发效率高，便于调试。
弱类型语言：变量类型无需提前声明，可动态变化（如var a = 10; a = "abc"; 合法）。
基于对象和事件驱动：围绕“对象”（如HTML元素、数组、对象）展开，通过绑定事件（如点击、输入）触发逻辑执行。
跨平台：可在所有主流浏览器中执行，无需适配不同浏览器（少量兼容性问题可通过 polyfill 解决）。
3.3 核心语法（Bookstore场景重点）
（1）变量声明
推荐使用let和const（ES6新增），替代var（存在变量提升、作用域混乱问题）：
let：声明可修改的变量（如购物车数量、用户输入的用户名）；
const：声明不可修改的常量（如书籍单价、后端接口地址）。
（2）数据类型
基本数据类型：字符串（string，如书籍名称）、数字（number，如价格、库存）、布尔（boolean，如是否登录、是否选中书籍）、null（空值，如购物车为空）、undefined（未定义，如未赋值的变量）；
引用数据类型：数组（array，如书籍列表、购物车商品列表）、对象（object，如用户信息、书籍信息）。
（3）流程控制
用于实现逻辑判断和循环执行，Bookstore场景常用：
if-else：逻辑判断（如校验用户名密码是否为空、判断书籍库存是否充足）；
for循环：遍历书籍列表、购物车商品列表（如计算购物车总价、渲染书籍列表）；
switch：多条件判断（如根据书籍分类显示不同样式、根据订单状态显示不同提示）。
（4）函数
用于封装可复用的逻辑（如表单校验、计算购物车总价、添加商品到购物车），避免重复代码，便于维护。
// 示例：计算购物车总价函数
function calculateTotal(cartList) {
    let total = 0;
    // 遍历购物车列表，累加每本图书的价格（价格 * 数量）
    for (let i = 0; i < cartList.length; i++) {
        total += cartList[i].price * cartList[i].count;
    }
    return total; // 返回计算后的总价
}
（5）事件绑定
Bookstore场景核心交互事件，通过绑定事件触发逻辑：
onclick：点击事件（如点击“加入购物车”“登录”按钮）；
onsubmit：表单提交事件（如登录表单、下单表单提交时触发校验）；
onchange：值变化事件（如下拉框选择书籍分类、输入框输入内容时触发）；
onblur：失去焦点事件（如输入框失去焦点时校验内容格式）。
3.4 Bookstore场景核心功能（重点扩展）
（1）表单校验（登录/注册/下单）
核心逻辑：用户提交表单前，校验输入内容的合法性，提示错误信息，避免无效数据提交到后端，提升用户体验。
// 登录表单校验（扩展完善版）
const loginForm = document.getElementById('loginForm');
const username = document.getElementById('username');
const password = document.getElementById('password');
const errorTip = document.createElement('div'); // 动态创建错误提示元素
errorTip.style.color = '#ff0000';
errorTip.style.fontSize = '12px';
loginForm.appendChild(errorTip);
loginForm.onsubmit = function(e) {
    e.preventDefault(); // 阻止表单默认提交行为，先执行校验
    const userVal = username.value.trim(); // 去除输入内容前后空格
    const pwdVal = password.value.trim();
    // 校验逻辑（扩展完善）
    if (userVal === '') {
        errorTip.textContent = '请输入用户名（不能为空）';
        return; // 校验失败，终止执行
    }
    if (userVal.length < 3 || userVal.length > 15) {
        errorTip.textContent = '用户名长度需在3-15位之间';
        return;
    }
    if (pwdVal === '') {
        errorTip.textContent = '请输入密码（不能为空）';
        return;
    }
    if (pwdVal.length < 6) {
        errorTip.textContent = '密码长度不能少于6位';
        return;
    }
    // 校验通过，可添加额外逻辑（如记住密码、加密密码），再提交表单
    errorTip.textContent = ''; // 清空错误提示
    alert('登录校验通过，正在提交...');
    this.submit(); // 提交表单
}
（2）购物车交互（核心功能）
实现添加商品、删除商品、修改商品数量、计算总价等动态交互，示例代码：
// 模拟购物车数据（实际从后端获取）
let cartList = [
    { id: 1, name: 'JavaScript入门教程', price: 59.9, count: 1 },
    { id: 2, name: 'HTML/CSS实战', price: 49.9, count: 1 }
];
// 1. 添加商品到购物车
function addToCart(book) {
    // 判断商品是否已在购物车中
    const isExist = cartList.some(item => item.id === book.id);
    if (isExist) {
        // 已存在，数量+1
        cartList.forEach(item => {
            if (item.id === book.id) {
                item.count++;
            }
        });
    } else {
        // 不存在，添加到购物车
        cartList.push({ ...book, count: 1 });
    }
    // 更新购物车页面显示
    updateCart();
}
// 2. 删除购物车商品
function deleteCartItem(bookId) {
    cartList = cartList.filter(item => item.id !== bookId);
    updateCart();
}
// 3. 修改商品数量
function changeCount(bookId, type) {
    cartList.forEach(item => {
        if (item.id === bookId) {
            if (type === 'add') {
                item.count++;
            } else if (type === 'reduce' && item.count > 1) {
                item.count--; // 数量不能小于1
            }
        }
    });
    updateCart();
}
// 4. 更新购物车页面显示（渲染商品列表、计算总价）
function updateCart() {
    const cartContainer = document.getElementById('cartContainer');
    const totalElement = document.getElementById('cartTotal');
    let cartHtml = '';
    let total = 0;
    // 遍历购物车，渲染商品列表
    cartList.forEach(item => {
        const itemTotal = item.price * item.count;
        total += itemTotal;
        cartHtml += `
            <div class="cart-item">
                <span>${item.name}</span>
                <span>¥${item.price.toFixed(2)}</span>
                <button onclick="changeCount(${item.id}, 'reduce')">-</button>
                <span>${item.count}</span>
                <button onclick="changeCount(${item.id}, 'add')">+</button>
                <span>¥${itemTotal.toFixed(2)}</span>
                <button onclick="deleteCartItem(${item.id})" style="color: #ff0000;">删除</button>
            </div>
        `;
    });
    // 渲染到页面
    cartContainer.innerHTML = cartHtml || '<div>购物车为空，快去添加商品吧！</div>';
    totalElement.textContent = `购物车总价：¥${total.toFixed(2)}`;
}
// 页面加载完成后，初始化购物车显示
window.onload = function() {
    updateCart();
    // 给“加入购物车”按钮绑定点击事件（假设页面中有多个书籍添加按钮）
    const addButtons = document.querySelectorAll('.add-to-cart');
    addButtons.forEach(button => {
        button.onclick = function() {
            // 获取书籍信息（从按钮自定义属性中获取，实际开发中从后端数据渲染）
            const bookId = parseInt(this.dataset.bookId);
            const bookName = this.dataset.bookName;
            const bookPrice = parseFloat(this.dataset.bookPrice);
            // 调用添加购物车函数
            addToCart({ id: bookId, name: bookName, price: bookPrice });
        };
    });
}
（3）DOM操作（动态修改页面）
核心用于动态修改HTML元素的内容、样式和结构，Bookstore场景常用操作：
获取元素：document.getElementById()（通过id获取单个元素）、document.querySelectorAll()（通过选择器获取多个元素，如所有书籍卡片）；
修改内容：element.innerHTML（可解析HTML标签，如动态渲染书籍列表）、element.textContent（仅修改文本，不解析HTML）；
修改样式：element.style.属性 = 值（如element.style.color = '#ff0000'，动态修改文本颜色）；
添加/删除元素：document.createElement()（创建元素）、element.appendChild()（添加元素）、element.remove()（删除元素）。
（4）异步请求（AJAX）
Bookstore中用于调用后端接口，实现数据交互（如搜索书籍、加载书籍列表、提交订单、获取用户信息），核心是通过XMLHttpRequest或fetch API发送请求，无需刷新页面即可获取/提交数据。
// 示例：搜索书籍（fetch API，更简洁）
function searchBook(keyword) {
    // 调用后端搜索接口，传递搜索关键词
    fetch(`/api/book/search?keyword=${keyword}`)
        .then(response => response.json()) // 解析后端返回的JSON数据
        .then(data => {
            // 后端返回书籍列表，动态渲染到页面
            const bookContainer = document.getElementById('bookContainer');
            let bookHtml = '';
            if (data.length === 0) {
                bookHtml = '<div>未找到相关书籍，请更换关键词重试！</div>';
            } else {
                data.forEach(book => {
                    bookHtml += `
                        <div class="book-card">
                            <img src="${book.coverUrl}" alt="${book.name}">
                            <h3>${book.name}</h3>
                            <p class="price">¥${book.price.toFixed(2)}</p>
                            <button class="add-to-cart" 
                                    data-book-id="${book.id}" 
                                    data-book-name="${book.name}" 
                                    data-book-price="${book.price}">
                                加入购物车
                            </button>
                        </div>
                    `;
                });
            }
            bookContainer.innerHTML = bookHtml;
            // 重新绑定“加入购物车”按钮事件
            bindAddToCartEvent();
        })
        .catch(error => {
            // 捕获请求错误（如网络异常、接口报错）
            alert('搜索失败，请稍后重试！');
            console.log('请求错误：', error);
        });
}
四、HTML/CSS/JavaScript三者关系（Bookstore场景重点）
三者协同工作，缺一不可，核心关系可总结为：
HTML（骨架）：搭建Bookstore页面的基础结构，确定“有什么”（登录框、书籍列表、购物车按钮等），是页面的基础。
CSS（外观）：美化HTML骨架，确定“长什么样”（书籍卡片的样式、按钮的颜色、页面的布局），提升视觉体验，让书店页面更美观、更易浏览。
JavaScript（行为）：赋予页面交互能力，确定“能做什么”（校验登录信息、添加购物车、搜索书籍、计算总价），让页面“活”起来，实现用户与页面的互动。
举例：Bookstore登录页的协同逻辑——HTML搭建登录表单（用户名输入框、密码输入框、登录按钮），CSS设置表单的布局、颜色、按钮样式，JavaScript实现表单校验（判断用户名密码是否合法）和表单提交（调用后端接口完成登录）。
五、Bookstore前端开发完整示例（整合版）
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>XX书店 - 登录页</title>
    <style>
        /* 内部样式（实际开发建议用外部CSS） */
        body {
            margin: 0;
            padding: 0;
            font-family: "微软雅黑", sans-serif;
            background-color: #f5f5f5;
        }
        .login-container {
            width: 350px;
            height: 300px;
            background-color: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            margin: 100px auto;
            padding: 20px;
        }
        .login-container h2 {
            text-align: center;
            color: #333;
            margin-bottom: 30px;
        }
        .form-item {
            margin: 15px 0;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .form-item label {
            width: 80px;
            text-align: right;
            font-size: 14px;
            color: #666;
        }
        .input {
            width: 200px;
            height: 30px;
            padding: 0 5px;
            border: 1px solid #ddd;
            border-radius: 4px;
            outline: none;
        }
        .input:focus {
            border-color: #0088ff;
        }

        #errorTip {
            color: #ff0000;
            font-size: 12px;
            margin-left: 90px;
            margin-bottom: 10px;
        }

        #loginBtn {
            width: 212px;
            height: 35px;
            background: #0088ff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            margin-left: 90px;
            transition: background 0.3s;
        }

        #loginBtn:hover {
            background: #0066cc;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <h2>书店会员登录</h2>
        <form id="loginForm">
            <div id="errorTip"></div>
            <div class="form-item">
                <label for="username">用户名：</label>
                <input type="text" id="username" class="input" placeholder="请输入用户名">
            </div>
            <div class="form-item">
                <label for="password">密码：</label>
                <input type="password" id="password" class="input" placeholder="请输入密码">
            </div>
            <button type="submit" id="loginBtn">登录</button>
        </form>
    </div>
    <script>
        // JavaScript表单校验与交互
        const loginForm = document.getElementById('loginForm');
        const username = document.getElementById('username');
        const password = document.getElementById('password');
        const errorTip = document.getElementById('errorTip');
        loginForm.onsubmit = function(e) {
            e.preventDefault();
            const userVal = username.value.trim();
            const pwdVal = password.value.trim();
            // 校验逻辑
            if (userVal === '') {
                errorTip.textContent = '请输入用户名（不能为空）';
                return;
            }
            if (userVal.length < 3 || userVal.length > 15) {
                errorTip.textContent = '用户名长度需在3-15位之间';
                return;
            }
            if (pwdVal === '') {
                errorTip.textContent = '请输入密码（不能为空）';
                return;
            }
            if (pwdVal.length < 6) {
                errorTip.textContent = '密码长度不能少于6位';
                return;
            }
            // 校验通过，模拟登录请求
            errorTip.textContent = '';
            alert('登录校验通过，正在登录...');
            // 实际开发中，这里会调用后端登录接口
            // fetch('/api/login', {
            //     method: 'POST',
            //     body: JSON.stringify({ username: userVal, password: pwdVal }),
            //     headers: { 'Content-Type': 'application/json' }
            // }).then(response => response.json())
            //   .then(data => {
            //       if (data.success) {
            //           window.location.href = 'index.html'; // 登录成功跳转到首页
            //       } else {
            //           errorTip.textContent = data.msg; // 显示后端返回的错误信息
            //       }
            //   });
        }
    </script>
</body>
</html>
六、核心总结（Bookstore前端开发重点）
基础原则：HTML、CSS、JavaScript分离开发，结构（HTML）、样式（CSS）、交互（JS）独立，便于后期维护和复用，这是企业开发的核心规范。
场景重点：Bookstore开发中，HTML重点关注表单、书籍列表、导航等结构；CSS重点关注布局、卡片美化、响应式适配；JavaScript重点关注表单校验、购物车交互、后端接口请求。
进阶方向：实际企业开发中，会使用Vue、Element UI等框架简化开发（如用Vue的v-for渲染书籍列表、用Element UI组件快速搭建表单），但HTML、CSS、JavaScript是基础，必须熟练掌握，才能更好地使用框架。
核心目标：打造美观、易用、适配多端的Bookstore前端页面，提升用户浏览、购买体验，降低操作成本（如简化登录流程、清晰展示书籍信息、便捷的购物车操作）。
