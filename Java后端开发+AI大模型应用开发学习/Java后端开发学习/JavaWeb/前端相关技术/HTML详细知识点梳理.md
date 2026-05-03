03.26 18:18
HTML详细知识点梳理
一、HTML基础核心概念
1.1 什么是HTML
HTML（HyperText Markup Language，超文本标记语言）是用于创建网页结构和内容的标准标记语言，它不是编程语言（无逻辑、不执行计算），而是通过一系列标记（标签）来定义网页中各个元素的含义和布局。
核心作用：将文本、图片、视频、链接等内容组织成浏览器可识别、可渲染的网页结构，是网页开发的“骨架”，通常与CSS（样式）、JavaScript（交互）配合使用，构成完整网页。
1.2 HTML的基本结构
一个标准的HTML文档必须包含固定的基本结构，所有内容都嵌套在<html>标签内，核心分为<head>（头部）和<body>（主体）两部分，缺一不可。
<!DOCTYPE html>  <!-- 声明文档类型，告诉浏览器这是HTML5文档 -->
<html lang="zh-CN">  <!-- 根标签，lang属性指定页面语言（zh-CN为中文） -->
  <head>  <!-- 头部：存放页面元信息，不直接显示在页面上 -->
    <meta charset="UTF-8"> <!-- 指定字符编码，UTF-8可兼容所有中文和特殊字符 -->
    <title>页面标题</title>  <!-- 浏览器标签页显示的标题，必填 -->
    <meta name="description" content="页面描述">  <!-- 用于搜索引擎优化（SEO） -->
    <link rel="stylesheet" href="style.css">  <!-- 引入外部CSS样式文件 -->
  </head>
  <body>  <!-- 主体：页面所有可见内容（文本、图片、按钮等）都放在这里 -->
    <h1>这是页面主标题</h1>
    <p>这是一段文本内容</p>
  </body>
</html>
1.3 HTML标签的核心特性
标签语法：大多数标签成对出现，分为开始标签（如<h1>）和结束标签（如</h1>）；少数标签为自闭合标签（无需结束标签），如<img>、<input>、<meta>，自闭合标签需在末尾加/（HTML5可省略，但规范建议保留）。
标签属性：标签可以添加属性，用于补充标签的信息或功能，格式为“属性名="属性值"”，属性必须写在开始标签内，多个属性用空格分隔。例如<img src="img.jpg" alt="图片">，src和alt都是img标签的属性。
嵌套规则：标签可以嵌套，但不能交叉嵌套。例如正确嵌套：<div><p>文本</p></div>；错误嵌套：<div><p>文本</div></p>。
大小写不敏感：HTML标签和属性名不区分大小写（如<H1>和<h1>效果一致），但规范建议全部使用小写，提高可读性和统一性。
二、HTML核心标签（按功能分类）
2.1 文档结构标签（定义页面整体框架）
标签
说明
关键属性

文档类型声明，必须放在HTML文档最顶部，声明当前文档是HTML5格式（无结束标签）
无

根标签，包裹整个HTML文档的所有内容
lang：指定页面语言（zh-CN、en等）

头部标签，存放页面元信息、外部资源引入等，不显示在页面主体
无

页面标题，显示在浏览器标签页，影响SEO
无

主体标签，所有可见内容都放在此标签内
无
2.2 文本相关标签（定义页面文本内容）
2.2.1 标题标签（h1~h6）
用于定义不同层级的标题，h1层级最高（页面主标题，建议一个页面只写一个h1），h6层级最低，默认样式为加粗、换行，字号依次减小。
<h1>一级标题（主标题）</h1>
<h2>二级标题（章节标题）</h2>
<h3>三级标题（小节标题）</h3>
<h4>四级标题</h4>
<h5>五级标题</h5>
<h6>六级标题</h6>
2.2.2 段落与换行标签
<p>：段落标签，用于包裹一段文本，段落之间会自动换行并产生间距，不能嵌套块级元素（如div、h1）。
<br/>：换行标签，自闭合标签，用于在段落内强制换行（不产生段落间距），区别于<p>的段落换行。
<p>这是第一段文本，使用p标签包裹，会自动换行并与其他段落产生间距。</p>
<p>这是第二段文本，<br/>这里使用br标签强制换行，段落内换行无间距。</p>
2.2.3 文本样式标签（强调、修饰）
标签
说明
示例
﻿
强调文本，默认样式为加粗（语义强调，比更具语义性）
重点内容
﻿
斜体强调，默认样式为斜体（语义强调，比更具语义性）
强调内容
﻿
单纯加粗文本（无语义，仅样式）
加粗文本
﻿
单纯斜体文本（无语义，仅样式）
斜体文本

给文本添加下划线（注意：避免与链接混淆）
下划线文本

删除线文本，用于表示删除、废弃的内容
已删除内容

插入线文本，用于表示新增的内容，默认带下划线
新增内容
2.2.4 特殊文本标签
<span>：行内标签，用于包裹一小段文本（无默认样式），常与CSS配合使用，给部分文本设置样式（区别于div的块级标签）。
<pre>：预格式化标签，保留文本中的空格、换行等格式，默认使用等宽字体，常用于展示代码、诗歌等需要固定格式的内容。
<blockquote>：引用标签，用于引用一段外部内容，默认会有缩进效果，语义上表示“引用”。
<span style="color: red;">红色文本</span>
<pre>
  预格式化文本
  保留空格和换行
</pre>
<blockquote>这是一段引用的内容，会自动缩进。</blockquote>
2.3 链接标签（<a>）
链接标签是HTML中实现页面跳转、资源跳转的核心标签，又称锚点标签，成对出现，核心属性为href。
核心属性
href：指定链接跳转的目标地址（必填），值可以是：
外部链接：完整的URL地址，如<a href="https://www.baidu.com">百度</a>（需加http/https）。
内部链接：同一网站内的其他页面路径（相对路径），如<a href="about.html">关于我们</a>。
锚点链接：跳转到当前页面的指定位置，需先给目标元素设置id，如<a href="#top">回到顶部</a>，目标元素为<div id="top">顶部内容</div>。
邮件链接：href="mailto:邮箱地址"，如<a href="mailto:123@qq.com">联系我们</a>，点击会打开邮件客户端。
电话链接：href="tel:电话号码"，如<a href="tel:13800138000">拨打电话</a>，移动端点击会唤起拨号盘。
target：指定链接的打开方式，常用值：
_self：默认值，在当前窗口打开链接。
_blank：在新窗口（新标签页）打开链接，建议配合rel="noopener noreferrer"使用（安全优化）。
_parent：在父框架中打开链接（用于框架页面）。
_top：在整个窗口中打开链接，取消所有框架。
rel：指定当前页面与目标页面的关系，如rel="noopener noreferrer"（安全属性，防止新窗口劫持）、rel="nofollow"（告诉搜索引擎不追踪该链接）。
<!-- 外部链接，新窗口打开 -->
<a href="https://www.baidu.com" target="_blank" rel="noopener noreferrer">百度一下</a>
<!-- 内部链接 -->
<a href="news.html">新闻页面</a>
<!-- 锚点链接 -->
<a href="#section1">跳转到第一部分</a>
<div id="section1">第一部分内容</div>
<!-- 邮件链接 -->
<a href="mailto:123@qq.com">发送邮件</a>
2.4 图片标签（<img>）
图片标签用于在页面中插入图片，自闭合标签，核心属性为src和alt，必须添加（兼顾兼容性和SEO）。
核心属性
src：指定图片的路径（必填），值可以是：
相对路径：同一项目中的图片路径，如src="img/1.jpg"（img文件夹下的1.jpg）。
绝对路径：完整的URL地址，如src="https://xxx.com/img/1.jpg"。
base64编码：将图片转为base64字符串嵌入标签（无需请求图片资源，适合小图片，如图标）。
alt：图片加载失败时显示的替代文本（必填），同时用于搜索引擎识别图片内容（SEO优化），如果图片是装饰性的，alt可以设为空（alt=""）。
width/height：设置图片的宽度和高度，单位可以是px（像素）、%（相对于父元素），建议只设置一个属性（如只设width），另一个属性会自动按比例缩放，避免图片变形。
title：鼠标悬停在图片上时显示的提示文本。
loading：图片加载方式，值为lazy（懒加载，滚动到图片位置再加载，优化性能）、eager（默认，立即加载）。
<!-- 基础图片，带替代文本和懒加载 -->
<img src="img/1.jpg" alt="风景图" width="500" loading="lazy" title="美丽的风景">
<!-- 图片加载失败时显示alt文本 -->
<img src="img/error.jpg" alt="图片加载失败，请刷新重试">
2.5 列表标签（有序列表、无序列表、定义列表）
2.5.1 无序列表（<ul>）
用于展示无顺序的列表项，默认列表项前有圆点（-disc），可通过CSS修改样式，<ul>内只能嵌套<li>（列表项标签）。
<ul>
  <li>列表项1</li>
  <li>列表项2</li>
  <li>列表项3</li>
</ul>
2.5.2 有序列表（<ol>）
用于展示有顺序的列表项，默认列表项前有数字（1、2、3...），可通过属性修改序号样式，<ol>内只能嵌套<li>。
核心属性
type：指定序号类型，值为1（数字，默认）、a（小写字母）、A（大写字母）、i（小写罗马数字）、I（大写罗马数字）。
start：指定序号的起始值，如start="3"，则从3开始计数。
reversed：倒序排列，值为reversed（如<ol reversed>，序号从大到小）。
<!-- 从3开始的小写字母有序列表 -->
<ol type="a" start="3">
  <li>列表项1</li>
  <li>列表项2</li>
  <li>列表项3</li>
</ol>
2.5.3 定义列表（<dl>）
用于展示“术语-解释”的列表，由<dl>（定义列表）、<dt>（定义术语）、<dd>（定义解释）组成，<dl>内只能嵌套<dt>和<dd>。
<dl>
  <dt>HTML</dt>
  <dd>超文本标记语言，用于创建网页结构。</dd>
  <dt>CSS</dt>
  <dd>层叠样式表，用于美化网页样式。</dd>
</dl>
2.6 表格标签（<table>）
用于展示结构化数据（如表格、报表），由多个标签配合组成，核心标签包括<table>（表格容器）、<tr>（表格行）、<th>（表头单元格）、<td>（普通单元格）。
核心组成标签
<table>：表格容器，所有表格内容都嵌套在其中。
<thead>：表格头部，用于包裹表头行（<tr>），语义上区分表头和表体。
<tbody>：表格主体，用于包裹表格内容行（<tr>），是表格的核心内容区域。
<tfoot>：表格底部，用于包裹表格总结行（<tr>），如合计、备注等。
<tr>：表格行，每一行对应一个<tr>标签，内部嵌套<th>或<td>。
<th>：表头单元格，用于表格标题，默认样式为加粗、居中。
<td>：普通单元格，用于表格内容，默认左对齐。
核心属性（表格合并）
rowspan：单元格跨行合并，值为合并的行数（如rowspan="2"，表示当前单元格占2行）。
colspan：单元格跨列合并，值为合并的列数（如colspan="3"，表示当前单元格占3列）。
<table border="1"&gt;  <!-- border属性设置表格边框（仅用于演示，实际用CSS） -->
  <thead>
    <tr>
      <th colspan="2">学生信息表&lt;/th&gt;  <!-- 跨2列 -->
    </tr>
    <tr>
      <th>姓名</th>
      <th>年龄</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>张三</td>
      <td>18</td>
    </tr>
    <tr>
      <td>李四</td>
      <td rowspan="2"&gt;19&lt;/td&gt;  <!-- 跨2行 -->
    </tr>
    <tr>
      <td>王五</td>
    </tr>
  </tbody>
</table>
2.7 表单标签（<form>）
表单用于收集用户输入的信息（如登录、注册、搜索），并将信息提交到服务器，核心标签为<form>，内部嵌套各种表单控件（如输入框、按钮、下拉框）。
2.7.1 <form>标签核心属性
action：指定表单提交的目标地址（服务器接口地址），如action="login.php"。
method：指定表单提交的方式，常用值为get（默认，提交数据会显示在URL中，适合简单查询，数据量小）和post（提交数据不显示在URL中，安全，适合登录、注册，数据量大）。
enctype：指定表单数据的编码方式，默认值为application/x-www-form-urlencoded（普通表单）；上传文件时需设为multipart/form-data。
target：指定表单提交后响应的打开方式（同<a>标签的target属性）。
2.7.2 常用表单控件
控件标签
说明
核心属性
示例

最常用的表单控件，通过type属性控制控件类型（自闭合标签）
type、name、value、id、placeholder、required等


多行文本输入框，用于输入大量文本（如备注、评论）
name、rows（行数）、cols（列数）、placeholder


下拉选择框，用于从多个选项中选择一个或多个
name、multiple（允许多选）、size（显示行数）
北京

下拉选择框的选项，嵌套在
value、selected（默认选中）
上海

按钮控件，用于提交表单、重置表单或触发事件
type（submit/reset/button）、name、value


标签关联控件，点击label文本会聚焦到对应的控件（提升用户体验）
for（与控件id对应）
用户名：
2.7.3 <input>控件的type属性常用值
text：单行文本输入框（默认），用于输入普通文本（如用户名、手机号）。
password：密码输入框，输入的内容会被隐藏（显示为圆点或星号）。
number：数字输入框，只能输入数字，会显示上下箭头调节数值。
email：邮箱输入框，会自动验证输入内容是否符合邮箱格式。
tel：电话输入框，移动端点击会唤起拨号盘，不强制验证格式。
date：日期选择框，会显示日历控件，用于选择日期（年/月/日）。
file：文件上传控件，用于上传本地文件，需配合form标签的enctype="multipart/form-data"。
checkbox：复选框，用于选择多个选项（需给多个checkbox设置相同的name属性）。
radio：单选框，用于选择一个选项（需给多个radio设置相同的name属性）。
submit：提交按钮，点击会提交表单数据到action指定的地址。
reset：重置按钮，点击会清空表单内所有输入内容，恢复默认状态。
button：普通按钮，无默认功能，需配合JavaScript实现交互（如弹窗、切换内容）。
<form action="login.php" method="post">
  <label for="username">用户名：</label>
  <input type="text" id="username" name="username" placeholder="请输入用户名" required><br/>
  <label for="password">密码：</label>
  <input type="password" id="password" name="password" placeholder="请输入密码" required><br/>
  <label>性别：</label>
  <input type="radio" name="gender" value="male" id="male"><label for="male">男</label>
  <input type="radio" name="gender" value="female" id="female"><label for="female">女</label><br/>
  <label>兴趣：</label>
  <input type="checkbox" name="hobby" value="game" id="game"><label for="game">游戏</label>
  <input type="checkbox" name="hobby" value="music" id="music"><label for="music">音乐</label><br/>
  <label for="city">城市：</label>
  <select id="city" name="city">
    <option value="">请选择城市</option>
    <option value="beijing">北京</option>
    <option value="shanghai" selected>上海</option>
  </select><br/>
  <label for="remark">备注：</label><br/>
  <textarea id="remark" name="remark" rows="3" cols="30" placeholder="请输入备注信息"></textarea><br/>
  <button type="submit">登录</button>
  <button type="reset">重置</button>
  <button type="button">取消</button>
</form>
三、HTML5新增标签与特性
3.1 新增语义化标签（提升页面结构可读性和SEO）
HTML5之前，页面结构主要靠<div>标签划分，语义不明确；HTML5新增了一系列语义化标签，明确标签的用途，便于浏览器和开发者理解。
标签
说明
使用场景

页面头部，用于包裹页面标题、导航栏、logo等
页面顶部、章节顶部

导航栏，用于包裹页面的导航链接（如首页、关于我们、联系我们）
页面顶部导航、侧边导航

页面主体内容，用于包裹页面的核心内容（一个页面只能有一个
页面核心内容区域

章节、区块，用于划分页面的不同部分（有明确的主题）
新闻列表、产品展示、章节内容

独立的文章内容，用于包裹可独立存在的内容（如新闻、博客、评论）
单篇新闻、博客文章、用户评论

侧边栏，用于包裹与主体内容相关的辅助内容（如侧边导航、广告、作者信息）
页面右侧侧边栏、文章作者信息

页面底部，用于包裹页面版权信息、联系方式、备案信息等
页面最底部

媒体容器，用于包裹图片、图表、代码等，配合添加说明
带说明的图片、代码片段

媒体说明，用于给
图片下方的说明文字
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <title>HTML5语义化页面</title>
  </head>
  <body>
    <header>  <!-- 页面头部 -->
      <img src="logo.png" alt="网站logo">
      <nav>  <!-- 导航栏 -->
        <ul>
          <li><a href="#">首页</a></li>
          <li><a href="#">新闻</a></li>
          <li><a href="#">关于我们</a></li>
        </ul>
      </nav>
    </header>
    <main>  <!-- 页面主体 -->
      <section>  <!-- 新闻章节 -->
        <h2>最新新闻</h2>
        <article>  <!-- 单篇新闻 -->
          <h3>HTML5语义化标签详解</h3>
          <p>HTML5新增了header、nav、main等语义化标签...</p>
          <figure>
            <img src="news.jpg" alt="新闻图片">
            <figcaption>HTML5语义化页面示例</figcaption>
          </figure>
        </article>
      </section>
      <aside>  <!-- 侧边栏 -->
        <h3>热门推荐</h3>
        <ul>
          <li><a href="#">CSS3新特性</a></li>
          <li><a href="#">JavaScript基础</a></li>
        </ul>
      </aside>
    </main>
    <footer>  <!-- 页面底部 -->
      <p>版权所有 © 2026 网站名称 备案号：xxx</p>
    </footer>
  </body>
</html>
3.2 HTML5新增表单控件与属性
3.2.1 新增input类型
color：颜色选择器，点击会弹出颜色面板，用于选择颜色，value值为十六进制颜色（如#ff0000）。
range：滑块控件，用于选择一个范围内的数值（如音量、亮度），可通过min（最小值）、max（最大值）、step（步长）属性控制。
search：搜索输入框，默认会显示清除按钮（输入内容后），语义上表示搜索功能。
url：URL输入框，会验证输入内容是否符合URL格式（如必须包含http/https）。
time：时间选择框，用于选择小时和分钟（24小时制）。
month：月份选择框，用于选择年和月。
week：周选择框，用于选择年和周。
3.2.2 新增表单属性
placeholder：输入提示文本，显示在输入框内，用户输入内容后自动消失（前面已提及，HTML5新增）。
required：必填属性，设置后该控件必须输入内容，否则表单无法提交，会提示“请填写此字段”。
autofocus：自动聚焦属性，页面加载完成后，该控件自动获得焦点（无需用户点击）。
autocomplete：自动完成属性，值为on（默认，自动提示历史输入内容）、off（关闭自动提示）。
min/max/step：用于number、range等类型，控制输入值的范围和步长（如min="0" max="100" step="5"）。
pattern：正则表达式验证，用于验证输入内容是否符合指定的正则规则（如验证手机号：pattern="^1[3-9]\d{9}$"）。
<!-- HTML5新增表单控件示例 -->
<input type="color" name="color" value="#ff0000"&gt;  <!-- 颜色选择器 -->
<input type="range" name="volume" min="0" max="100" step="5" value="50"&gt;  <!-- 滑块 -->
<input type="search" name="search" placeholder="请输入搜索内容"&gt;  <!-- 搜索框 -->
<input type="url" name="url" placeholder="请输入网址" required&gt;  <!-- URL输入框，必填 -->
<input type="time" name="time"&gt;  <!-- 时间选择框 -->
<input type="month" name="month"&gt;  <!-- 月份选择框 -->
<input type="tel" name="phone" pattern="^1[3-9]\d{9}$" placeholder="请输入手机号" required&gt;  <!-- 手机号验证 -->
3.3 HTML5新增媒体标签
3.3.1 视频标签（<video>）
用于在页面中插入视频，无需依赖第三方插件（如Flash），支持多种视频格式（MP4、WebM、Ogg），成对出现。
核心属性
src：指定视频文件的路径（必填）。
controls：显示视频控制栏（播放、暂停、音量、进度条等），值为controls（无需赋值）。
autoplay：自动播放视频，值为autoplay（注意：部分浏览器要求静音才能自动播放）。
muted：静音播放，值为muted。
loop：循环播放，值为loop。
width/height：设置视频的宽度和高度。
poster：视频加载前显示的封面图片，src属性指定图片路径。
<!-- 视频标签示例，兼容多种格式 -->
<video controls width="600" poster="video.jpg">
  <source src="video.mp4" type="video/mp4"&gt;  <!-- MP4格式（最兼容） -->
  <source src="video.webm" type="video/webm"&gt;  <!-- WebM格式 -->
  您的浏览器不支持视频播放，请升级浏览器。  <!-- 浏览器不支持时显示的文本 -->
</video>
3.3.2 音频标签（<audio>）
用于在页面中插入音频，支持多种音频格式（MP3、Wav、Ogg），成对出现，核心属性与<video>标签类似。
<!-- 音频标签示例（扩展功能：兼容多格式、添加控制优化、错误提示完善） -->
<audio controls muted autoplay loop preload="auto" id="audioPlayer">
  <!-- 优先MP3格式（最兼容所有浏览器） -->
  <source src="audio.mp3" type="audio/mpeg">
  <!-- 备用Wav格式（无损音质，兼容现代浏览器） -->
  <source src="audio.wav" type="audio/wav">
  <!-- 备用Ogg格式（开源格式，兼容Firefox、Chrome） -->
  <source src="audio.ogg" type="audio/ogg">
  <!-- 浏览器不支持音频播放时，显示更友好的提示，并提供下载备选方案 -->
  您的浏览器不支持音频播放，请升级浏览器；或点击<a href="audio.mp3" download="音频文件.mp3">下载音频</a>收听。
</audio>
<!-- 扩展功能：添加简单JS控制（播放/暂停、音量调节） -->
<script>
  // 获取音频元素
  const audio = document.getElementById('audioPlayer');
  // 播放/暂停切换按钮
  function togglePlay() {
    if (audio.paused) {
      audio.play();
    } else {
      audio.pause();
    }
  }
  // 音量调节函数（0-1之间，0为静音，1为最大音量）
  function adjustVolume(volume) {
    audio.volume = volume;
  }
</script>
<!-- 配套控制按钮（提升用户体验） -->
<button onclick="togglePlay()">播放/暂停</button>
<button onclick="adjustVolume(0.5)">音量50%</button>
<button onclick="adjustVolume(1)">音量最大</button>
<button onclick="adjustVolume(0)">静音</button>
四、HTML常用属性与规范
4.1 全局属性（所有标签都可使用的属性）
id：给标签设置唯一标识（页面中id不能重复），用于锚点链接、JavaScript获取元素、CSS定位。
class：给标签设置类名（页面中class可以重复），用于CSS批量设置样式，多个类名用空格分隔。
style：行内样式属性，用于给标签设置临时样式（优先级最高，覆盖外部CSS），格式为style="属性名:属性值;"。
title：鼠标悬停时显示的提示文本（适用于所有标签）。
hidden：隐藏标签，值为hidden，设置后标签不显示在页面上（但仍存在于HTML结构中）。
lang：指定标签内文本的语言（如lang="zh-CN"），用于搜索引擎和屏幕阅读器。
<!-- 全局属性示例 -->
<div id="box" class="container red" style="color: red; font-size: 16px;" title="这是一个容器">
  文本内容
</div>
<p hidden>这段文本会被隐藏</p>
4.2 HTML编码规范（推荐遵循）
文档声明：必须在HTML文档最顶部添加<!DOCTYPE html>，声明为HTML5文档。
字符编码：必须在<head>标签内添加<meta charset="UTF-8">，避免中文乱码。
标签小写：所有标签和属性名全部使用小写（规范写法，提高可读性）。
属性引号：所有属性值必须用双引号（""）包裹（单引号也可，但规范建议双引号）。
自闭合标签：自闭合标签（如<img>、<input>）建议添加/闭合（HTML5可省略，但规范建议保留）。
语义优先：优先使用语义化标签（如header、nav、main），避免过度使用<div>标签。
缩进规范：标签嵌套时使用缩进（一般2个或4个空格），提高代码可读性。
注释规范：使用<!-- 注释内容 -->添加注释，注释内容要简洁明了，避免过多注释。
五、HTML常见问题与注意事项
中文乱码问题：未添加<meta charset="UTF-8">或编码格式不匹配，解决方法：确保<head>内添加正确的字符编码标签。
图片加载失败：src路径错误（相对路径、绝对路径错误），解决方法：检查图片路径是否正确，确保图片文件存在。
表单无法提交：缺少name属性（表单控件必须设置name属性，否则数据无法提交）、必填项未填写、method或action属性错误。
标签嵌套错误：块级标签嵌套行内标签（正确），行内标签嵌套块级标签

