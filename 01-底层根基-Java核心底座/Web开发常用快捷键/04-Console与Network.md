# 04 - Console 与 Network 快捷键

> **核心摘要**：Console 是「信息中枢」（报错/调试输出/命令执行）、Network 是「接口透视镜」（请求详情/时序/过滤）。本文覆盖 Console 操作快捷键与 $0/$_ 快捷引用、Network 面板过滤与请求排查。

> **前置阅读**：[[01-Chrome DevTools快捷键]]

---

## 📚 目录

1. [Console 面板快捷键](#1-console-面板快捷键)
2. [Console 快捷引用（$0 等）](#2-console-快捷引用0-等)
3. [Console 调试技巧](#3-console-调试技巧)
4. [Network 面板快捷键](#4-network-面板快捷键)
5. [Network 请求排查](#5-network-请求排查)
6. [接口排查实战流程](#6-接口排查实战流程)
7. [核心要点](#7-核心要点)

---

## 1. Console 面板快捷键

> **背景**：Console 是 DevTools 的信息中枢——报错、日志、命令执行都在这里。
> **目的**：快速操作控制台（历史/多行/清空/搜索）。
> **适用范围**：前端调试的信息获取与命令执行。
> **不适用场景**：逻辑跟踪（断点在 Sources）。

| 快捷键 | 功能 |
|:---:|------|
| `Ctrl+Shift+J` | 直接打开 Console（从页面） |
| `↑` / `↓` | 命令历史 |
| `Shift+Enter` | **多行输入**（不执行） |
| `Enter` | 执行命令 |
| `Ctrl+L` | 清空控制台 |
| `Ctrl+F` | 搜索控制台输出 |
| `Esc` | 关闭/打开底部抽屉（Console 常驻） |
| `Ctrl+[` / `]` | 面板切换 |

```text
Console 高频操作
├── ① 看报错：红色错误 → 点击定位源码
├── ② 执行命令：直接输入 JS 表达式（Enter）
├── ③ 多行脚本：Shift+Enter 换行（不执行）
├── ④ 历史复用：↑ 找回上一条命令
├── ⑤ 清屏：Ctrl+L（输出太多时）
└── ⑥ 搜索：Ctrl+F（找历史输出）
```

---

## 2. Console 快捷引用（$0 等）

### 2.1 快捷引用变量（2026 核心技巧）

```text
Console 的快捷引用
├── $0：当前在 Elements 选中的 DOM 节点
├── $1~$4：最近选中的 4 个节点（历史）
├── $_：上一次表达式的结果
├── $()：document.querySelector 的简写（$('#id')）
├── $$()：querySelectorAll 的简写
└── $x()：XPath 查询

实战（无需打开 Elements）
├── $0.style.color = 'red'       ← 改选中元素样式！
├── $0.textContent               ← 读选中元素内容
├── $_                           ← 上一个结果继续用
├── $$('div.card').length        ← 统计元素数量
└── $x('//button[text()="保存"]') ← XPath 找按钮
```

### 2.2 $0 的调试价值

```text
$0 的经典用法（Elements + Console 联动）
├── ① Elements 选中元素 → Console 用 $0 操作
├── ② $0 检查属性/内容（无需写选择器）
├── ③ $0 触发事件：$0.click()
├── ④ 修改样式：$0.style.padding = '20px'
└── ⑤ 获取数据：$0.dataset / $0.value
→ 金句：$0 = 「当前选中元素的快捷手柄」
```

---

## 3. Console 调试技巧

### 3.1 输出方法全家

```javascript
// Console 输出方法
console.log('普通日志');          // 最常用
console.info('信息');
console.warn('警告');             // 黄色
console.error('错误');            // 红色（带堆栈）
console.debug('调试');

// 进阶输出
console.table(array);            // 表格展示数组/对象（神器！）
console.group('分组');            // 分组折叠
console.time('耗时');             // 计时开始
console.timeEnd('耗时');          // 计时结束
console.assert(条件, '失败信息');  // 条件断言
console.trace();                  // 打印调用栈
```

### 3.2 调试辅助技巧

```text
Console 调试技巧
├── ① console.table：数组/对象表格化（可读性暴增）
├── ② console.time：测量代码耗时（性能快速判断）
├── ③ 保留日志：勾选 Preserve log（刷新后不丢）
├── ④ 分组折叠：console.group（输出结构清晰）
├── ⑤ 条件输出：console.assert（只输出失败）
└── ⑥ 生产调试：日志断点（见 03 篇——零污染）

Preserve log 的场景（重要）
├── ① 页面跳转/刷新后想保留旧日志
├── ② 查看跳转前的报错
├── ③ 表单提交后页面刷新（验证请求）
└── 设置：Console 右上角齿轮 → Preserve log
```

---

## 4. Network 面板快捷键

> **背景**：Network 面板展示所有网络请求——接口调试的透视镜。
> **目的**：快速录制/过滤/定位请求，排查接口问题。

| 快捷键 | 功能 |
|:---:|------|
| `Ctrl+E` | **开始/停止录制**请求 |
| `Ctrl+F` | 搜索请求（URL/响应内容） |
| `/` | 快速过滤请求列表（支持正则） |
| `Ctrl+R` | 重新加载并录制 |
| `Esc` | 打开/关闭底部抽屉 |
| `Ctrl+[` / `]` | 面板切换 |

```text
Network 面板高频操作
├── ① 开始录制：Ctrl+E（或刷新页面自动录制）
├── ② 过滤类型：All/JS/CSS/Img/XHR/Fetch（顶部）
├── ③ URL 过滤：/ 输入关键字（实时过滤）
├── ④ 搜索：Ctrl+F 全局搜（请求/响应内容）
├── ⑤ 详情查看：点击请求 → Headers/Preview/Response
├── ⑥ 请求回放：右键 → Replay XHR（重发请求！）
└── ⑦ 保存：右键 → Save all as HAR（导出分析）
```

---

## 5. Network 请求排查

### 5.1 请求详情看什么

```text
接口排查四步（点击请求后）
├── ① Headers：URL/方法/状态码/请求头
│   └── 关键：请求参数、Content-Type、Token
├── ② Payload/Request：请求体（POST 参数）
├── ③ Response：响应体（数据对不对）
├── ④ Timing：耗时分解（哪一段慢）
└── ⑤ Preview：响应预览（格式化）

状态码速查（接口排查）
├── 200 成功 / 201 创建 / 204 无内容
├── 301/302 重定向（可能接口路径变了）
├── 400 参数错误（请求体问题）
├── 401 未认证（Token 问题）
├── 403 无权限（权限问题）
├── 404 路径错误（URL 拼错！）
├── 500 服务器错误（后端问题）
└── 429 限流（请求太频繁）
```

### 5.2 网络模拟与重放

```text
Network 进阶能力（2026）
├── ① 模拟网络：Network conditions → 离线/慢速 3G
├── ② 请求重放：右键请求 → Replay XHR（不刷新页面）
├── ③ 修改重放：右键 → Copy as fetch → Console 修改后执行
├── ④ 复制请求头：Copy as cURL（终端复现请求）
├── ⑤ 阻断请求：右键 → Block request URL（测试容错）
├── ⑥ 保存 HAR：导出完整请求记录（提 Bug 给后端）
└── ⑦ 瀑布图：Timing 面板看请求并行/阻塞

Copy as cURL 的场景
├── ① 后端复现请求（给后端看完整请求）
├── ② 终端测试接口（curl 直接跑）
└── ③ 修改参数测试（curl 改数据）
```

---

## 6. 接口排查实战流程

### 6.1 完整流程（键盘流）

```text
接口 Bug 排查键盘流
┌──────────────────────────────────────────┐
│ ① F12 → Network 面板                      │
│ ② Ctrl+R 刷新触发请求                      │
│ ③ 找到目标请求（/ 过滤 URL 关键字）        │
│ ④ 看状态码：                            │
│    ├── 4xx → 看请求参数/URL/Token        │
│    ├── 5xx → 后端问题（复制 cURL 给后端）  │
│    └── 200 但数据不对 → 看 Response       │
│ ⑤ Headers 确认请求头（Content-Type/Token）│
│ ⑥ Payload 确认请求体（参数正确性）        │
│ ⑦ Response 看返回（数据/错误信息）        │
│ ⑧ 修复 → Ctrl+R 验证                      │
└──────────────────────────────────────────┘
```

### 6.2 常见问题

```text
Network 排查常见问题
├── ⚠️ 请求没出现在列表 → 检查 Preserve log/录制状态
├── ⚠️ 请求被缓存 → 勾选 Disable cache（Network 设置）
├── ⚠️ CORS 错误 → 看 Response Headers 的 Access-Control-*
├── ⚠️ 请求 pending 很久 → Timing 看哪段耗时
├── ⚠️ 找不到接口 → 全局搜索 Ctrl+Shift+F 搜 URL 关键字
└── ⚠️ 线上接口排查 → Overrides 本地覆盖 + 日志断点
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. Console 操作：Ctrl+Shift+J 直达 / Shift+Enter 多行 / Ctrl+L 清屏 / Ctrl+F 搜索
> 2. **快捷引用**：$0（选中元素）/$_（上次结果）/$()/$$()/$x()——Console 与 Elements 联动
> 3. 输出进阶：console.table（表格）/console.time（计时）/console.group（分组）——可读性暴增
> 4. **Preserve log**：刷新不丢日志——跳转后排查必备
> 5. Network 四键：Ctrl+E 录制 / / 过滤 / Ctrl+F 搜索 / Replay XHR 重放
> 6. 接口排查四步：状态码 → Headers → Payload → Response——4xx 查前端、5xx 查后端
> 7. Copy as cURL 给后端复现请求——前后端协作排障标配

---

**下一模块**：[05-前端框架与工作流](05-前端框架与工作流.md) | **返回总览**：[00-Web开发常用快捷键知识体系总览](00-Web开发常用快捷键知识体系总览.md)
