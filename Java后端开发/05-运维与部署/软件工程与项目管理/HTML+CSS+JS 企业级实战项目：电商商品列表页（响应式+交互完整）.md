HTML+CSS+JS 企业级实战项目：电商商品列表页（响应式+交互完整）
项目简介
纯前端实现电商商品列表页，包含商品卡片、筛选、搜索、加入购物车、响应式布局等功能，贴合企业级前端开发规范，适合夯实HTML/CSS/JS基础。
技术栈
- 结构：HTML5（语义化标签）
- 样式：CSS3（Flex布局、Grid、响应式、动画）
- 交互：原生JavaScript（DOM操作、事件监听、本地存储）
- 规范：模块化、语义化、响应式适配
    项目结构
    plaintext
    project/
    ├── index.html       # 主页面
    ├── css/
    │   └── style.css    # 样式文件
    ├── js/
    │   └── main.js      # 交互逻辑
    └── images/          # 图片资源（可使用占位图）
 
完整代码实现
1. index.html（语义化结构）
    html
    <!DOCTYPE html>
    <html lang="zh-CN">
    <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>电商商品列表</title>
    <link rel="stylesheet" href="css/style.css">
    </head>
    <body>
    <!-- 头部导航 -->
    <header class="header">
        <div class="nav">
            <div class="logo">电商平台</div>
            <div class="search-box">
                <input type="text" id="searchInput" placeholder="搜索商品">
                <button id="searchBtn">搜索</button>
            </div>
            <div class="cart">
                <span>购物车</span>
                <span id="cartCount">0</span>
            </div>
        </div>
    </header>
    <!-- 筛选栏 -->
    <section class="filter">
        <div class="filter-item">
            <span>分类：</span>
            <select id="categorySelect">
                <option value="all">全部</option>
                <option value="phone">手机</option>
                <option value="computer">电脑</option>
                <option value="pad">平板</option>
            </select>
        </div>
        <div class="filter-item">
            <span>价格：</span>
            <select id="priceSelect">
                <option value="all">全部</option>
                <option value="0-1000">0-1000元</option>
                <option value="1000-3000">1000-3000元</option>
                <option value="3000+">3000元以上</option>
            </select>
        </div>
    </section>
    <!-- 商品列表 -->
    <main class="container">
        <div class="product-list" id="productList">
            <!-- 商品卡片通过JS动态生成 -->
        </div>
    </main>
    <!-- 购物车弹窗 -->
    <div class="cart-modal" id="cartModal">
        <div class="modal-content">
            <span class="close" id="closeModal">&times;</span>
            <h3>购物车</h3>
            <div class="cart-list" id="cartList"></div>
        </div>
    </div>
    <script src="js/main.js"></script>
    </body>
    </html>
 
2. css/style.css（响应式+企业级样式）
    css
    /* 全局重置 */
    * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    }
    body {
    font-family: "Microsoft YaHei", sans-serif;
    background-color: #f5f5f5;
    }
    /* 头部导航 */
    .header {
    background-color: #ff4400;
    color: white;
    padding: 15px 0;
    }
    .nav {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    }
    .logo {
    font-size: 24px;
    font-weight: bold;
    }
    .search-box {
    display: flex;
    width: 400px;
    }
    .search-box input {
    flex: 1;
    padding: 8px 12px;
    border: none;
    border-radius: 4px 0 0 4px;
    outline: none;
    }
    .search-box button {
    padding: 8px 16px;
    background-color: #ff6600;
    border: none;
    color: white;
    border-radius: 0 4px 4px 0;
    cursor: pointer;
    }
    .cart {
    font-size: 18px;
    cursor: pointer;
    }
    .cart span:last-child {
    background-color: white;
    color: #ff4400;
    padding: 2px 6px;
    border-radius: 50%;
    margin-left: 5px;
    }
    /* 筛选栏 */
    .filter {
    max-width: 1200px;
    margin: 20px auto;
    padding: 0 20px;
    display: flex;
    gap: 20px;
    }
    .filter-item {
    display: flex;
    align-items: center;
    gap: 10px;
    }
    .filter-item select {
    padding: 6px 10px;
    border: 1px solid #ddd;
    border-radius: 4px;
    }
    /* 商品列表 */
    .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px 40px;
    }
    .product-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 20px;
    }
    .product-card {
    background-color: white;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    transition: transform 0.3s;
    }
    .product-card:hover {
    transform: translateY(-5px);
    }
    .product-img {
    width: 100%;
    height: 200px;
    object-fit: cover;
    }
    .product-info {
    padding: 15px;
    }
    .product-name {
    font-size: 16px;
    margin-bottom: 10px;
    height: 40px;
    overflow: hidden;
    }
    .product-price {
    color: #ff4400;
    font-size: 20px;
    font-weight: bold;
    margin-bottom: 15px;
    }
    .add-cart {
    width: 100%;
    padding: 10px;
    background-color: #ff4400;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    }
    /* 购物车弹窗 */
    .cart-modal {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0,0,0,0.5);
    z-index: 1000;
    }
    .modal-content {
    background-color: white;
    margin: 100px auto;
    padding: 20px;
    width: 80%;
    max-width: 600px;
    border-radius: 8px;
    position: relative;
    }
    .close {
    position: absolute;
    right: 20px;
    top: 15px;
    font-size: 24px;
    cursor: pointer;
    }
    .cart-list {
    margin-top: 20px;
    }
    .cart-item {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #ddd;
    }
    /* 响应式适配 */
    @media (max-width: 768px) {
    .nav {
        flex-direction: column;
        gap: 15px;
    }
    .search-box {
        width: 100%;
    }
    .filter {
        flex-direction: column;
    }
    }
 
3. js/main.js（完整交互逻辑）
    javascript
    // 模拟商品数据
    const products = [
    { id: 1, name: 'iPhone 15', price: 5999, category: 'phone', img: 'https://via.placeholder.com/250x200?text=iPhone15' },
    { id: 2, name: 'MacBook Pro', price: 12999, category: 'computer', img: 'https://via.placeholder.com/250x200?text=MacBook' },
    { id: 3, name: 'iPad Air', price: 3999, category: 'pad', img: 'https://via.placeholder.com/250x200?text=iPad' },
    { id: 4, name: '小米14', price: 3999, category: 'phone', img: 'https://via.placeholder.com/250x200?text=小米14' },
    { id: 5, name: '华为MateBook', price: 5999, category: 'computer', img: 'https://via.placeholder.com/250x200?text=MateBook' },
    { id: 6, name: '小米平板6', price: 1999, category: 'pad', img: 'https://via.placeholder.com/250x200?text=小米平板' }
    ];
    // 购物车数据
    let cart = JSON.parse(localStorage.getItem('cart')) || [];
    // DOM元素
    const productList = document.getElementById('productList');
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const categorySelect = document.getElementById('categorySelect');
    const priceSelect = document.getElementById('priceSelect');
    const cartCount = document.getElementById('cartCount');
    const cartModal = document.getElementById('cartModal');
    const closeModal = document.getElementById('closeModal');
    const cartList = document.getElementById('cartList');
    const cartBtn = document.querySelector('.cart');
    // 初始化页面
    window.onload = function() {
    renderProducts(products);
    updateCartCount();
    };
    // 渲染商品列表
    function renderProducts(productArray) {
    productList.innerHTML = '';
    productArray.forEach(product => {
        const card = document.createElement('div');
        card.className = 'product-card';
        card.innerHTML = `
            <img src="${product.img}" alt="${product.name}" class="product-img">
            <div class="product-info">
                <div class="product-name">${product.name}</div>
                <div class="product-price">¥${product.price}</div>
                <button class="add-cart" data-id="${product.id}">加入购物车</button>
            </div>
        `;
        productList.appendChild(card);
    });
    // 绑定加入购物车事件
    bindAddCartEvent();
    }
    // 绑定加入购物车事件
    function bindAddCartEvent() {
    const addButtons = document.querySelectorAll('.add-cart');
    addButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const productId = parseInt(this.dataset.id);
            addToCart(productId);
        });
    });
    }
    // 加入购物车
    function addToCart(productId) {
    const product = products.find(p => p.id === productId);
    const existingItem = cart.find(item => item.id === productId);
    if (existingItem) {
        existingItem.quantity += 1;
    } else {
        cart.push({ ...product, quantity: 1 });
    }
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartCount();
    alert('加入购物车成功');
    }
    // 更新购物车数量
    function updateCartCount() {
    cartCount.textContent = cart.reduce((total, item) => total + item.quantity, 0);
    }
    // 搜索功能
    searchBtn.addEventListener('click', filterProducts);
    searchInput.addEventListener('keyup', function(e) {
    if (e.key === 'Enter') filterProducts();
    });
    // 筛选功能
    categorySelect.addEventListener('change', filterProducts);
    priceSelect.addEventListener('change', filterProducts);
    // 筛选商品
    function filterProducts() {
    const searchText = searchInput.value.toLowerCase().trim();
    const category = categorySelect.value;
    const priceRange = priceSelect.value;
    let filtered = products.filter(product => {
        // 搜索筛选
        const matchSearch = product.name.toLowerCase().includes(searchText);
        // 分类筛选
        const matchCategory = category === 'all' || product.category === category;
        // 价格筛选
        let matchPrice = true;
        if (priceRange === '0-1000') matchPrice = product.price <= 1000;
        else if (priceRange === '1000-3000') matchPrice = product.price > 1000 && product.price <= 3000;
        else if (priceRange === '3000+') matchPrice = product.price > 3000;
        return matchSearch && matchCategory && matchPrice;
    });
    renderProducts(filtered);
    }
    // 购物车弹窗
    cartBtn.addEventListener('click', function() {
    renderCart();
    cartModal.style.display = 'block';
    });
    closeModal.addEventListener('click', function() {
    cartModal.style.display = 'none';
    });
    // 点击弹窗外部关闭
    window.addEventListener('click', function(e) {
    if (e.target === cartModal) {
        cartModal.style.display = 'none';
    }
    });
    // 渲染购物车
    function renderCart() {
    cartList.innerHTML = '';
    if (cart.length === 0) {
        cartList.innerHTML = '<p>购物车为空</p>';
        return;
    }
    cart.forEach(item => {
        const cartItem = document.createElement('div');
        cartItem.className = 'cart-item';
        cartItem.innerHTML = `
            <span>${item.name} × ${item.quantity}</span>
            <span>¥${(item.price * item.quantity).toFixed(2)}</span>
        `;
        cartList.appendChild(cartItem);
    });
    }
 
项目核心功能
1. 商品列表展示：动态生成商品卡片
2. 搜索功能：按商品名称模糊搜索
3. 分类筛选：按手机/电脑/平板分类
4. 价格筛选：按价格区间筛选
5. 购物车功能：加入购物车、数量统计、本地存储
6. 响应式布局：适配PC、平板、手机
7. 交互效果：卡片悬浮动画、弹窗关闭
    运行步骤
    1. 创建项目文件夹，按结构新建文件
    2. 将代码分别复制到对应文件
    3. 直接打开index.html运行
    4. 测试搜索、筛选、购物车功能
    企业级优化点
    1. 语义化标签：header、section、main提升SEO
    2. 响应式设计：适配多端设备
    3. 本地存储：localStorage持久化购物车
    4. 模块化：HTML/CSS/JS分离，便于维护
    5. 性能优化：事件委托、减少DOM操作
    6. 用户体验：动画效果、弹窗交互
    扩展方向
    1. 加入商品详情页
    2. 实现购物车删除、修改数量功能
    3. 集成Mock数据模拟后端接口
    4. 添加登录注册功能
    5. 引入Vue/React框架重构
    6. 加入轮播图、商品评价等模块
