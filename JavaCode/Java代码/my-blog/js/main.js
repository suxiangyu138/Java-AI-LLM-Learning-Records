// 等待HTML文档加载完成后再执行JS（避免获取不到元素）
document.addEventListener('DOMContentLoaded', function() {
    // 1. 导航栏：手机端折叠/展开功能
    const menuBtn = document.getElementById('menuBtn');
    const navMenu = document.querySelector('.nav-menu');
    const navLinks = document.querySelectorAll('.nav-link');

    // 点击菜单按钮，切换导航菜单显示/隐藏
    menuBtn.addEventListener('click', function() {
        navMenu.classList.toggle('active');
        // 菜单按钮图标切换（汉堡→叉号）
        const icon = menuBtn.querySelector('i');
        icon.classList.toggle('fa-bars');
        icon.classList.toggle('fa-times');
    });

    // 点击导航链接，关闭菜单（手机端）+ 平滑滚动到对应模块
    navLinks.forEach(link => {
        link.addEventListener('click', function() {
            navMenu.classList.remove('active');
            const icon = menuBtn.querySelector('i');
            icon.classList.replace('fa-times', 'fa-bars');
            // 平滑滚动
            const targetId = this.getAttribute('href');
            document.querySelector(targetId).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });

    // 2. 回到顶部按钮：滚动显示/隐藏 + 点击平滑回到顶部
    const backToTop = document.getElementById('backToTop');
    window.addEventListener('scroll', function() {
        // 滚动距离超过500px，显示按钮，否则隐藏
        if (window.scrollY > 500) {
            backToTop.classList.add('show');
        } else {
            backToTop.classList.remove('show');
        }
        // 滚动时高亮对应导航链接
        highlightNavLink();
    });

    // 点击回到顶部，平滑滚动到页面顶部
    backToTop.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    // 3. 留言区：发送留言 + 非空校验 + 动态展示留言
    const sendMsgBtn = document.getElementById('sendMsg');
    const msgName = document.getElementById('msg-name');
    const msgContent = document.getElementById('msg-content');
    const msgList = document.getElementById('msgList');

    sendMsgBtn.addEventListener('click', function() {
        // 获取输入内容，去掉首尾空格
        const name = msgName.value.trim();
        const content = msgContent.value.trim();

        // 非空校验
        if (name === '') {
            alert('请输入你的昵称！');
            msgName.focus();
            return;
        }
        if (content === '') {
            alert('请输入你的留言内容！');
            msgContent.focus();
            return;
        }

        // 获取当前时间，格式化（年-月-日 时:分:秒）
        const now = new Date();
        const timeStr = `${now.getFullYear()}-${formatNum(now.getMonth()+1)}-${formatNum(now.getDate())} ${formatNum(now.getHours())}:${formatNum(now.getMinutes())}:${formatNum(now.getSeconds())}`;

        // 动态创建留言项HTML
        const msgItem = document.createElement('div');
        msgItem.className = 'msg-item';
        msgItem.innerHTML = `
            <span class="msg-item-name">${name}</span>
            <span class="msg-item-time">${timeStr}</span>
            <p class="msg-item-content">${content}</p>
        `;

        // 将留言项添加到留言区（插入到最前面）
        msgList.insertBefore(msgItem, msgList.firstChild);

        // 清空输入框
        msgName.value = '';
        msgContent.value = '';
        alert('留言发送成功！');
    });

    // 拓展：留言区按回车键发送
    msgContent.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) { // 按Enter不按Shift，发送留言
            e.preventDefault(); // 阻止换行
            sendMsgBtn.click();
        }
    });

    // 工具函数：数字补0（比如1→01，9→09）
    function formatNum(num) {
        return num < 10 ? '0' + num : num;
    }

    // 工具函数：滚动时高亮对应导航链接
    function highlightNavLink() {
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            const sectionTop = section.offsetTop - 100;
            const sectionHeight = section.offsetHeight;
            const sectionId = section.getAttribute('id');
            if (window.scrollY >= sectionTop && window.scrollY < sectionTop + sectionHeight) {
                navLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === '#' + sectionId) {
                        link.classList.add('active');
                    }
                });
            }
        });
    }
});