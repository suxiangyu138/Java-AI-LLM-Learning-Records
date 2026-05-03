03.26 18:18
CSS详细知识点梳理
一、CSS基础概念
1.1 什么是CSS
CSS（Cascading Style Sheets，层叠样式表）是用于描述HTML（或XML）文档呈现样式的语言，核心作用是分离文档结构（HTML）和表现形式（样式），让页面布局更灵活、样式更统一，降低维护成本。
CSS的核心特性：层叠性、继承性、优先级，这三大特性决定了样式的最终渲染效果。
1.2 CSS的引入方式（优先级从高到低）
（1）内联样式（行内样式）
直接在HTML标签的style属性中编写CSS，仅作用于当前标签，优先级最高，但可维护性差，不推荐大量使用。
示例：<p style="color: red; font-size: 16px;">这是内联样式</p>
（2）内部样式表
在HTML文档的<head>标签内，通过<style>标签编写CSS，作用于当前整个HTML文档，优先级低于内联样式，高于外部样式表。
示例：
<head>
<style>
p { color: blue; font-size: 14px; }
</style>
</head>
（3）外部样式表（推荐使用）
创建独立的.css文件，通过<link>标签引入HTML文档，可被多个HTML页面复用，维护性最强，优先级最低（若未加!important）。
示例：<link rel="stylesheet" href="style.css">
补充：还可通过@import引入外部CSS（如在style标签内写@import url("style.css");），但优先级低于外部样式表，且加载顺序滞后，不推荐使用。
1.3 CSS基本语法
CSS语法由“选择器”和“声明块”组成，声明块内包含多个“属性-值对”，格式如下：
选择器 { 属性1: 值1; 属性2: 值2; ... }
说明：
选择器：指定要样式化的HTML元素（如p、div、.class、#id等）；
声明块：用大括号{}包裹，每个声明由“属性”和“值”组成，中间用冒号:连接，多个声明用分号;分隔；
注释：/* 注释内容 */，注释不会被浏览器渲染，用于代码说明。
二、CSS选择器（核心重点）
选择器的作用是精准定位HTML页面中的元素，以便为其应用样式，分为基础选择器和复合选择器，优先级遵循“ specificity（ specificity值越高，优先级越高）”原则。
2.1 基础选择器（ specificity：1）
（1）元素选择器（标签选择器）
以HTML标签名作为选择器，作用于页面中所有该标签的元素，优先级最低。
示例：div { width: 100px; height: 100px; }
（2）类选择器（.class）
以“.”开头，后面跟类名，作用于所有class属性值为该类名的元素，可重复使用（一个元素可添加多个类，用空格分隔），优先级高于元素选择器。
示例：.box { background: #f0f0f0; } 对应HTML：<div class="box"></div>
（3）ID选择器（#id）
以“#”开头，后面跟ID名，作用于ID属性值为该名称的唯一元素（页面中ID不可重复），优先级高于类选择器。
示例：#header { height: 60px; } 对应HTML：<div id="header"></div>
（4）通配符选择器（*）
以“*”表示，作用于页面中所有元素，常用于重置默认样式（如清除margin、padding），优先级最低。
示例：* { margin: 0; padding: 0; box-sizing: border-box; }
2.2 复合选择器
（1）后代选择器（空格分隔）
选择某元素的所有后代元素（包括子元素、孙元素等），格式：父选择器 后代选择器。
示例：.box p { color: #333; } （选择class为box的元素内所有p标签）
（2）子选择器（>分隔）
选择某元素的直接子元素（仅一代，不包含孙元素），格式：父选择器 > 子选择器。
示例：.box > p { font-weight: bold; } （仅选择box的直接子元素p）
（3）相邻兄弟选择器（+分隔）
选择某元素的“下一个相邻”兄弟元素（必须是同级，且紧挨着），格式：元素1 + 元素2。
示例：p + div { margin-top: 10px; } （选择p标签后面紧挨着的div标签）
（4）通用兄弟选择器（~分隔）
选择某元素的“所有同级后续”兄弟元素（无需紧挨着），格式：元素1 ~ 元素2。
示例：p ~ div { color: #666; } （选择p标签后面所有同级的div标签）
（5）交集选择器（无分隔，紧连）
选择同时满足多个选择器条件的元素，格式：选择器1选择器2（如元素选择器+类选择器、元素选择器+ID选择器）。
示例：p.box { font-size: 18px; } （选择class为box的p标签）
（6）并集选择器（,分隔）
选择多个选择器对应的元素，统一应用样式，格式：选择器1, 选择器2, 选择器3...。
示例：p, div, .box { margin: 10px; } （选择p、div、class为box的所有元素）
2.3 伪类选择器（重点）
伪类选择器用于选择“处于特定状态”的元素，以“:”开头，常见分类如下：
（1）链接伪类（针对a标签）
a:link：未访问过的链接；
a:visited：已访问过的链接（仅能修改color、background-color等少数属性）；
a:hover：鼠标悬浮在链接上的状态（最常用）；
a:active：鼠标点击链接瞬间的状态。
注意：链接伪类的书写顺序必须是：link → visited → hover → active（记忆口诀：LVHA），否则样式会失效。
（2）结构伪类（根据元素在DOM中的位置选择）
:first-child：选择父元素的第一个子元素；
:last-child：选择父元素的最后一个子元素；
:nth-child(n)：选择父元素的第n个子元素（n可以是数字、odd（奇数）、even（偶数）、2n+1等）；
:nth-last-child(n)：选择父元素的倒数第n个子元素；
:first-of-type：选择父元素中同类型标签的第一个；
:last-of-type：选择父元素中同类型标签的最后一个；
:nth-of-type(n)：选择父元素中同类型标签的第n个（与nth-child的区别：nth-child不区分标签类型，nth-of-type区分）。
（3）状态伪类（针对元素的交互状态）
:focus：元素获得焦点时（如input输入框被点击时）；
:hover：鼠标悬浮时（可用于所有元素，不止a标签）；
:active：元素被激活时（如按钮被点击时）；
:checked：表单元素（如checkbox、radio）被选中时；
:disabled：元素被禁用时（如input disabled）。
（4）否定伪类（:not(选择器)）
选择“不满足括号内选择器条件”的元素，常用于排除特定元素。
示例：.box:not(.active) { opacity: 0.5; } （选择class为box但不包含active类的元素）
2.4 伪元素选择器（重点）
伪元素选择器用于创建“虚拟元素”（不在DOM中存在），以“::”开头（CSS3规范，CSS2用:，兼容写法可混用），常用如下：
::before：在元素内容的前面插入虚拟元素（必须配合content属性使用，content可设为""空值）；
::after：在元素内容的后面插入虚拟元素（同样需配合content属性）；
::first-letter：选择文本的第一个字符（仅适用于块级元素）；
::first-line：选择文本的第一行（仅适用于块级元素）；
::selection：选择用户选中的文本（可修改选中后的颜色、背景色）。
示例：.box::before { content: "★"; color: red; } （在box元素内容前添加红色五角星）
2.5 选择器优先级（核心）
优先级决定了多个样式冲突时，哪个样式会被浏览器渲染，优先级从高到低排序：
!important：强制提升样式优先级（最高，慎用，会破坏正常优先级，仅在特殊场景使用）；
内联样式（style属性）：specificity值为1000；
ID选择器（#id）：specificity值为100；
类选择器（.class）、伪类选择器（:hover）、属性选择器（[attr]）：specificity值为10；
元素选择器（div、p）、伪元素选择器（::before）：specificity值为1；
通配符选择器（*）：specificity值为0。
补充规则：
优先级计算：将选择器中各类型的specificity值相加，值越高，优先级越高；
优先级相同：后面编写的样式会覆盖前面的样式（层叠性）；
继承的样式：优先级最低，即使是元素选择器，也能覆盖继承的样式。
三、CSS核心特性（层叠、继承、优先级）
3.1 层叠性
当多个CSS规则作用于同一个元素，且样式不冲突时，所有规则会叠加生效；若样式冲突，则按优先级决定生效的样式，优先级相同则后面的样式覆盖前面的。
示例：p { color: red; } p { font-size: 16px; } 最终p标签会同时拥有红色文本和16px字体大小。
3.2 继承性
子元素会自动继承父元素的某些CSS属性，无需单独为子元素设置，减少代码冗余。
可继承的属性（常见）：
文本相关：color、font-size、font-family、font-weight、text-align、line-height等；
列表相关：list-style、list-style-type等；
其他：visibility（可见性）等。
不可继承的属性（常见）：width、height、margin、padding、border、background、position等（布局相关属性通常不可继承）。
补充：可通过inherit关键字强制子元素继承父元素的某属性，如子元素 { color: inherit; }。
3.3 优先级（已在选择器部分详细说明，此处补充核心要点）
优先级的核心是“specificity计算”，记住：!important > 内联 > ID > 类/伪类/属性 > 元素/伪元素 > 通配符，继承样式优先级最低。
注意：!important只能提升单个属性的优先级，不能提升整个选择器的优先级；且尽量不用，否则后续修改样式会非常繁琐。
四、CSS样式属性（重点模块）
4.1 文本样式（核心）
（1）字体相关属性
font-family：设置字体（可指定多个字体，用逗号分隔，浏览器会依次查找可用字体）；
示例：font-family: "Microsoft YaHei", Arial, sans-serif;（优先微软雅黑，其次Arial，最后默认无衬线字体）
font-size：设置字体大小（单位：px、em、rem，最常用px）；
font-weight：设置字体粗细（normal（400）、bold（700）、bolder、lighter，或直接写数字100-900）；
font-style：设置字体样式（normal、italic（斜体）、oblique（倾斜））；
font：复合属性，可简写字体样式（顺序：font-style font-weight font-size font-family，必须包含font-size和font-family）；
示例：font: italic bold 16px "Microsoft YaHei";
（2）文本相关属性
color：设置文本颜色（取值：十六进制#fff、rgb(255,255,255)、rgba(255,255,255,0.5)、英文单词red等）；
text-align：设置文本水平对齐方式（left（默认）、center、right、justify（两端对齐））；
text-decoration：设置文本装饰（none（取消下划线，常用于a标签）、underline（下划线）、overline（上划线）、line-through（删除线））；
text-indent：设置文本首行缩进（单位：px、em，常用2em表示首行缩进2个字符）；
line-height：设置行高（单位：px、em、%，或无单位（相对于font-size），常用于垂直居中）；
text-shadow：设置文本阴影（语法：text-shadow: 水平偏移 垂直偏移 模糊度 阴影颜色）；
示例：text-shadow: 2px 2px 3px rgba(0,0,0,0.3);
white-space：设置文本换行方式（normal（默认，自动换行）、nowrap（不换行）、pre（保留空格和换行））；
4.2 背景样式（核心）
background-color：设置背景颜色（取值同color属性）；
background-image：设置背景图片（语法：background-image: url("图片路径");）；
background-repeat：设置背景图片重复方式（repeat（默认，平铺）、no-repeat（不重复）、repeat-x（水平平铺）、repeat-y（垂直平铺））；
background-position：设置背景图片位置（语法：background-position: 水平位置 垂直位置；取值：left/center/right、top/center/bottom，或具体px、%）；
示例：background-position: center center;（图片居中显示）
background-size：设置背景图片大小（语法：background-size: 宽度 高度；取值：px、%、cover（覆盖容器，可能裁剪）、contain（完全显示，可能留空白））；
background-attachment：设置背景图片固定方式（scroll（默认，随页面滚动）、fixed（固定在视口，不随页面滚动））；
background：复合属性，可简写所有背景属性（顺序无严格要求，但建议：color image repeat position size attachment）；
示例：background: #f0f0f0 url("bg.jpg") no-repeat center center / cover fixed;（/用于分隔position和size）
background-origin：设置背景图片的起始位置（content-box、padding-box（默认）、border-box）；
background-clip：设置背景图片的裁剪范围（content-box、padding-box、border-box（默认）、text（裁剪到文本，需配合-webkit-前缀兼容））；
4.3 盒子模型（核心中的核心）
CSS中所有元素都可以看作一个“盒子”，盒子模型由4部分组成（从内到外）：内容区（content）、内边距（padding）、边框（border）、外边距（margin）。
（1）盒子模型的两种模式
标准盒子模型（W3C标准）：盒子总宽度 = content宽度 + padding + border + margin；
怪异盒子模型（IE浏览器）：盒子总宽度 = content宽度（包含padding和border） + margin；
切换模式：box-sizing: content-box;（标准模式，默认）、box-sizing: border-box;（怪异模式，推荐使用，方便布局）。
（2）盒子模型各部分属性
① 内容区（content）
设置元素的内容大小，常用属性：width（宽度）、height（高度），取值：px、%、em、rem，或auto（默认，自动适应）。
注意：行内元素（如span、a）默认无法设置width和height，需将其转换为块级元素或行内块元素。
② 内边距（padding）
内容区与边框之间的距离，会影响盒子的实际大小（标准模式下），属性：
padding-top：上内边距；
padding-right：右内边距；
padding-bottom：下内边距；
padding-left：左内边距；
简写：padding: 上 右 下 左;（顺时针），可省略部分值（如padding: 10px; 表示四个方向都是10px；padding: 10px 20px; 表示上下10px、左右20px）。
③ 边框（border）
盒子的边框，由边框宽度、边框样式、边框颜色组成，属性：
border-width：边框宽度（px、thin、medium、thick）；
border-style：边框样式（none（默认，无边框）、solid（实线）、dashed（虚线）、dotted（点线）、double（双线））；
border-color：边框颜色（取值同color属性）；
简写：border: 宽度 样式 颜色;（如border: 1px solid #ccc;）；
单独设置某一边边框：border-top、border-right、border-bottom、border-left（如border-left: 2px dashed red;）；
边框圆角：border-radius（语法：border-radius: 水平半径 垂直半径; 可简写为border-radius: 10px; 表示四个角都是10px圆角；也可单独设置每个角：border-top-left-radius等）；
边框阴影：box-shadow（语法：box-shadow: 水平偏移 垂直偏移 模糊度 扩散度 阴影颜色 内阴影（inset）；示例：box-shadow: 0 2px 5px rgba(0,0,0,0.2);）。
④ 外边距（margin）
盒子与其他盒子之间的距离，不影响盒子自身大小，属性与padding类似：
margin-top：上外边距；
margin-right：右外边距；
margin-bottom：下外边距；
margin-left：左外边距；
简写：margin: 上 右 下 左;（规则同padding）；
重点：margin塌陷（margin collapse）
场景1：相邻两个块级元素，上面元素的margin-bottom和下面元素的margin-top会合并，取两者中的较大值；
场景2：父元素的margin-top与子元素的margin-top会合并，取两者中的较大值；
解决方法：给父元素添加border或padding，或给父元素设置overflow: hidden;，或使用浮动、定位脱离文档流。
4.4 元素显示模式（核心）
CSS中元素分为3种基本显示模式，可通过display属性切换，不同模式的元素具有不同的特性。
（1）块级元素（block）
特性：
独占一行，宽度默认是父元素的100%；
可设置width、height、margin、padding、border；
常见块级元素：div、p、h1-h6、ul、ol、li、dl、dt、dd、header、footer、section等。
（2）行内元素（inline）
特性：
不独占一行，与其他行内元素并排显示；
不可设置width、height（设置无效）；
可设置margin-left、margin-right、padding-left、padding-right，但margin-top、margin-bottom无效；
常见行内元素：span、a、strong、em、i、u、s等。
（3）行内块元素（inline-block）
特性（结合块级和行内元素的优点）：
不独占一行，与其他行内/行内块元素并排显示；
可设置width、height、margin、padding、border；
常见行内块元素：img、input、button、select等；
注意：行内块元素之间会有默认的空隙（由HTML中的空格、换行导致），可通过给父元素设置font-size: 0; 解决。
（4）display属性的常用值
block：转换为块级元素；
inline：转换为行内元素；
inline-block：转换为行内块元素；
none：隐藏元素（元素不显示，且不占据页面空间，与visibility: hidden; 区别：visibility隐藏后仍占据空间）；
flex：转换为弹性盒容器（后续详细说明）；
grid：转换为网格盒容器（后续详细说明）。
4.5 浮动（float，经典布局方式）
浮动的核心作用是让元素“脱离文档流”，实现左右排列（如文字环绕图片、导航栏横向排列），但会导致“父元素塌陷”问题，需重点掌握清除浮动的方法。
（1）float属性的取值
none：默认值，不浮动；
left：向左浮动；
right：向右浮动。
（2）浮动的特性
元素浮动后，脱离正常文档流，不再占据原来的位置，会“浮”在其他元素上方；
浮动元素会尽量靠近父元素的边缘，或其他浮动元素的边缘；
行内元素浮动后，会自动转换为行内块元素（可设置width、height）；
父元素若没有设置高度，且所有子元素都浮动，父元素会塌陷（高度为0）。
（3）清除浮动（解决父元素塌陷）
清除浮动的核心是“让父元素感知到浮动子元素的存在”，常用方法如下：
方法1：额外标签法（最简单，不推荐）
在所有浮动子元素的最后，添加一个空的块级元素，设置clear: both;（clear: left; 清除左浮动，clear: right; 清除右浮动，clear: both; 清除所有浮动）；
示例：<div style="clear: both;"></div>
方法2：父元素设置overflow: hidden;（常用，简单）
给父元素添加overflow: hidden;，会触发BFC（块级格式化上下文），让父元素包裹住浮动子元素，从而避免塌陷；
注意：若父元素内有超出部分，会被隐藏，需根据需求使用。
方法3：父元素添加伪元素清除法（推荐，无额外标签，语义化好）
给父元素添加::after伪元素，设置如下样式：
.father::after { content: ""; display: block; clear: both; height: 0; visibility: hidden; }
简化写法：.father::after { content: ""; display: table; clear: both; }
方法4：父元素也设置浮动（不推荐，会导致父元素也脱离文档流，影响后续布局）。
4.6 定位（position，核心布局方式）
定位用于精确控制元素在页面中的位置，通过position属性设置定位模式，配合top、right、bottom、left属性（偏移量）确定元素位置，定位元素会脱离文档流（static除外）。
（1）position属性的取值（5种）
① static（静态定位，默认）
元素遵循正常文档流，top、right、bottom、left、z-index属性无效，无法设置偏移量，是默认的定位模式。
② relative（相对定位）
特性：
元素不脱离正常文档流，仍占据原来的位置；
偏移量（top、right、bottom、left）相对于元素自身原来的位置计算；
常用场景：作为绝对定位元素的“参考容器”（父相子绝）。
③ absolute（绝对定位）
特性（重点）：
元素脱离正常文档流，不再占据原来的位置；
偏移量相对于“最近的已定位祖先元素”（position为relative、absolute、fixed的祖先）计算；若没有已定位祖先元素，则相对于浏览器视口（body）计算；
常用场景：精确控制元素位置（如导航栏下拉菜单、弹窗、图标定位），必须配合“父相子绝”使用（父元素设为relative，子元素设为absolute），避免相对于视口定位。
④ fixed（固定定位）
特性：
元素脱离正常文档流，不再占据原来的位置；
偏移量相对于浏览器视口（viewport）计算，无论页面滚动，元素始终固定在指定位置；
常用场景：固定导航栏、回到顶部按钮、弹窗遮罩。
⑤ sticky（粘性定位，CSS3新增）
特性（结合relative和fixed）：
元素在滚动到指定偏移量前，表现为relative（遵循文档流）；滚动到指定偏移量后，表现为fixed（固定在视口）；
必须设置top、right、bottom、left中的至少一个，否则无效；
常用场景：滚动时固定的标题、导航栏。
（2）定位相关属性
top/right/bottom/left：偏移量，用于确定定位元素的位置（单位：px、%）；
z-index：设置定位元素的层级（数值越大，层级越高，越靠上显示）；
注意：z-index仅对已定位元素（relative、absolute、fixed、sticky）有效；若层级相同，后面的元素会覆盖前面的元素；父元素的z-index无论多大，都无法覆盖子元素的z-index（子元素层级相对父元素独立）。
4.7 弹性盒布局（Flex，CSS3核心布局，推荐使用）
Flex（Flexible Box，弹性盒）是CSS3新增的布局方式，用于快速实现元素的对齐、分布、自适应，兼容性好（IE11+支持），比浮动、定位更灵活，是目前主流的布局方式。
核心概念：将元素设置为flex容器（display: flex;），容器内的子元素称为flex项目（flex item），容器通过主轴（默认水平方向）和交叉轴（默认垂直方向）控制项目的排列。
（1）flex容器的属性（作用于父元素）
display: flex;：将父元素转换为flex容器，子元素自动成为flex项目；
flex-direction：设置主轴方向（控制项目排列方向）；
取值：row（默认，主轴水平，从左到右）、row-reverse（主轴水平，从右到左）、column（主轴垂直，从上到下）、column-reverse（主轴垂直，从下到上）；
flex-wrap：设置项目是否换行；
取值：nowrap（默认，不换行，项目会被压缩）、wrap（换行，超出容器宽度时换行）、wrap-reverse（反向换行）；
flex-flow：复合属性，简写flex-direction和flex-wrap（如flex-flow: row wrap;）；
justify-content：设置项目在主轴上的对齐方式；
取值：flex-start（默认，主轴起点对齐）、flex-end（主轴终点对齐）、center（主轴居中对齐）、space-between（两端对齐，项目之间间距相等）、space-around（项目两侧间距相等，整体间距是项目间间距的2倍）、space-evenly（所有间距相等）；
align-items：设置项目在交叉轴上的对齐方式（单行项目）；
取值：stretch（默认，项目拉伸至与容器交叉轴高度一致）、flex-start（交叉轴起点对齐）、flex-end（交叉轴终点对齐）、center（交叉轴居中对齐）、baseline（项目基线对齐）；
align-content：设置多行项目在交叉轴上的对齐方式（仅当项目换行时有效）；
取值：stretch（默认）、flex-start、flex-end、center、space-between、space-around、space-evenly。
（2）flex项目的属性（作用于子元素）
flex-grow：设置项目的放大比例（默认0，不放大；数值越大，放大比例越大，占据剩余空间越多）；
示例：三个项目的flex-grow分别为1、2、3，剩余空间会按1:2:3的比例分配给三个项目；
flex-shrink：设置项目的缩小比例（默认1，当容器空间不足时，项目会缩小；设为0，项目不缩小）；
flex-basis：设置项目在主轴上的初始宽度（默认auto，即项目自身宽度；可设为px、%）；
flex：复合属性，简写flex-grow、flex-shrink、flex-basis（常用写法：flex: 1; 等价于flex: 1 1 auto;，表示项目可放大、可缩小，初始宽度自动）；
align-self：单独设置某个项目在交叉轴上的对齐方式，覆盖容器的align-items属性（取值同align-items）；
order：设置项目的排列顺序（默认0，数值越小，排列越靠前；可设为负数）。
4.8 网格布局（Grid，CSS3新增，高级布局）
Grid（网格）布局是比Flex更强大的布局方式，用于实现二维布局（行和列同时控制），适合复杂的页面布局（如仪表盘、卡片布局），兼容性略差（IE11部分支持）。
核心概念：将父元素设置为grid容器（display: grid;），容器内的子元素称为grid项目，容器通过行（grid row）和列（grid column）划分网格，控制项目的位置和大小。
（1）grid容器的核心属性
display: grid;：将父元素转换为grid容器；
grid-template-columns：设置网格的列数和每列宽度；
示例：grid-template-columns: 100px 200px auto;（3列，宽度分别为100px、200px、自适应）；
常用单位：px（固定宽度）、%（相对宽度）、fr（比例单位，分配剩余空间，如grid-template-columns: 1fr 2fr 1fr; 表示3列，比例1:2:1）；
grid-template-rows：设置网格的行数和每行高度（用法同grid-template-columns）；
grid-gap（CSS3旧写法）/ gap（CSS3新写法）：设置网格项目之间的间距（gap: 行间距 列间距; 简写gap: 10px; 表示行和列间距都是10px）；
grid-template-areas：给网格区域命名，方便项目定位（配合项目的grid-area属性使用）；
justify-items：设置项目在网格单元格内的水平对齐方式（stretch、start、end、center）；
align-items：设置项目在网格单元格内的垂直对齐方式（stretch、start、end、center）；
place-items：复合属性，简写justify-items和align-items（如place-items: center center;）。
（2）grid项目的核心属性
grid-column-start / grid-column-end：设置项目占据的列范围（如grid-column-start: 1; grid-column-end: 3; 表示项目占据第1列到第3列，即跨2列）；
简写：grid-column: 1 / 3;（等价于上面的写法）；
grid-row-start / grid-row-end：设置项目占据的行范围（用法同列）；
简写：grid-row: 1 / 2;（项目占据第1行到第2行，即跨1行）；
grid-area：设置项目所在的网格区域（可配合容器的grid-template-areas使用，或直接简写grid-row和grid-column，如grid-area: 1 / 1 / 2 / 3;）；
justify-self：单独设置某个项目在单元格内的水平对齐方式，覆盖容器的justify-items；
align-self：单独设置某个项目在单元格内的垂直对齐方式，覆盖容器的align-items；
place-self：复合属性，简写justify-self和align-self。
4.9 其他常用样式属性
（1）visibility：控制元素可见性
visible（默认）：元素可见；
hidden：元素隐藏，但仍占据页面空间（与display: none; 区别：display: none; 不占据空间）；
collapse：仅用于表格元素，隐藏表格行或列，且不占据空间。
（2）opacity：控制元素透明度
取值范围：0~1（0完全透明，1完全不透明），会影响元素及其所有子元素的透明度（如opacity: 0.5; 表示元素半透明）。
（3）cursor：设置鼠标指针样式
常用取值：default（默认箭头）、pointer（手型，常用于可点击元素）、text（文本指针，常用于输入框）、move（移动指针）、not-allowed（禁止指针）等。
（4）overflow：控制元素内容溢出时的处理方式
visible（默认）：内容溢出时显示在元素外部；
hidden：内容溢出时隐藏；
scroll：无论内容是否溢出，都显示滚动条；
auto：内容溢出时显示滚动条，不溢出时不显示。
（5）transition：过渡效果（CSS3，用于实现平滑动画）
语法：transition: 过渡属性 过渡时间 过渡速度 延迟时间；
示例：transition: all 0.3s ease 0s;（所有属性变化都有0.3秒的平滑过渡，速度为ease，无延迟）；
常用过渡属性：width、height、color、background-color、opacity、transform等。
（6）transform：变形效果（CSS3，用于旋转、缩放、平移、倾斜）
translate(x, y)：平移（x为水平方向，y为垂直方向，单位px、%）；
示例：transform: translate(50px, 30px);（水平向右平移50px，垂直向下平移30px）；
scale(x, y)：缩放（x为水平缩放比例，y为垂直缩放比例，1为原大小，大于1放大，小于1缩小）；
示例：transform: scale(1.2);（整体放大1.2倍）；
rotate(deg)：旋转（单位deg，正数顺时针，负数逆时针）

