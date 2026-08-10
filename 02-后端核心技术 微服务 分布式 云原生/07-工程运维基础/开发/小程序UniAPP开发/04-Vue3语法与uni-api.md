# 04-Vue3语法与uni-api
> 定位：页面开发的两大基础——Vue3 组合式 API（响应式/计算/监听）与 uni 全局 API（请求/导航/交互/存储）；「模板语法是 Vue 的、能力 API 是 uni 的」。

## 📚 目录
1. [Vue3 组合式 API](#1-vue3-组合式-api)
2. [模板语法要点](#2-模板语法要点)
3. [uni 全局 API 全景](#3-uni-全局-api-全景)
4. [导航 API](#4-导航-api)
5. [交互与存储 API](#5-交互与存储-api)
6. [条件编译](#6-条件编译)
7. [常见坑](#7-常见坑)
8. [练习](#8-练习)

## 1. Vue3 组合式 API

uni-app 的页面开发用 Vue3 组合式 API（`<script setup>`——2026 标准写法）：**响应式**——`ref`（基础类型/对象——`ref(0)`）、`reactive`（对象——`reactive({})`）、`computed`（计算属性——派生状态）；**监听**——`watch`（监听变化——响应式数据的副作用）；**生命周期**——`onMounted`/`onUnmounted`（组件级——页面级生命周期在 03 篇）；**组合式**——`useXxx()` 自定义组合函数（逻辑复用）。

```vue
<script setup>
import { ref, computed, watch } from 'vue'
const count = ref(0)
const double = computed(() => count.value * 2)   // 计算属性
watch(count, (v) => console.log('count 变了', v)) // 监听
function add() { count.value++ }
</script>
```

**「组合式 API 的核心是逻辑组织」**——按功能组织（一个功能的响应式+计算+监听在一起）vs Options API 按类型分散——**「2026 新项目一律组合式」**（官方推荐——01 篇基线）。

## 2. 模板语法要点

模板语法的关键点（Vue 基础——[[../../10-前端知识拓展/Vue/00-Vue总览与核心概念|Vue 体系]] 的小程序版差异）：**插值**——`{{ message }}`；**指令**——`v-if`（条件）、`v-for`（列表——**`v-for` 要 `:key`**）、`v-bind`（`:属性`）、`v-on`（`@事件`）、`v-model`（双向绑定——表单）；**事件**——`@click="handler"`——**注意**：**小程序端的限制**——**不能用 `v-html`**（小程序无 DOM 渲染 HTML——富文本用 `rich-text` 组件）、**不能用内联复杂表达式**（模板表达式保持简单——复杂逻辑进 script）——**「模板差异是跨端开发的第一批坑」**。

## 3. uni 全局 API 全景

**uni API 是「平台差异封装」的统一 API**——`uni.xxx()` 形态（类似微信的 `wx.xxx()` 但跨端统一）——**「写一次 uni API，各平台自动映射」**是跨端能力的核心。API 全景按用途分类：**网络**——`uni.request`（HTTP——05 篇封装）；**导航**——`uni.navigateTo`/`redirectTo`/`switchTab`/`navigateBack`（下节）；**交互**——`uni.showToast`/`showLoading`/`showModal`（提示弹窗）；**存储**——`uni.setStorageSync`/`getStorageSync`（同步本地存储——小程序版 localStorage）；**路由参数**——`uni.navigateTo` 的 url 带参（下节）；**系统**——`uni.getSystemInfo`/`getLocation` 等。

**「先查 uni API 再查平台 API」**——跨端开发的第一动作是找 uni 封装（没有的再走条件编译 + 平台 API——08 篇）。

## 4. 导航 API

页面导航四件套（对应不同场景）：**navigateTo**——跳转新页面（**保留当前页**——可返回——「列表 → 详情」）；**redirectTo**——关闭当前页跳转（不可返回——「登录成功 → 首页」）；**switchTab**——跳 tabBar 页面（**只有它才能跳 tab 页**）；**navigateBack**——返回上一页（`delta` 参数返回层级——「详情 → 返回列表」）。

**传参姿势**：

```js
// 跳转带参（url 查询串）
uni.navigateTo({ url: '/pages/detail/detail?id=123' })
// 详情页接收（onLoad 的 options）
onLoad((options) => { console.log(options.id) })   // '123'
```

**注意**：**参数是字符串**（数字要 `Number()` 转换）、**参数别塞敏感/大对象**（url 长度限制——大数据用全局状态/存储——06 篇）——**「传参的姿势：小数据 url、大数据状态」**。

## 5. 交互与存储 API

**交互 API**（用户反馈）：`uni.showToast`（轻提示——`icon: 'success'/'error'`）、`uni.showLoading`（加载中——**要配 `uni.hideLoading`**——「showLoading 忘记 hide 是常见 bug」）、`uni.showModal`（确认弹窗——删除确认）、`uni.showActionSheet`（操作菜单）。**存储 API**：`uni.setStorageSync(key, value)`/`getStorageSync`（**同步版——业务代码直接读**）、`uni.setStorage`（异步版）——**用途**：**Token 存储、用户信息缓存、草稿**——**「存储的注意」**——**敏感凭证的存储策略**（05 篇——Token 放哪、什么时候清）。

## 6. 条件编译

**条件编译——「平台差异代码的隔离」**——一套代码里写各平台差异（08 篇详解，本篇入门）：

```js
// #ifdef MP-WEIXIN
console.log('只在微信小程序编译')
// #endif
// #ifndef H5
console.log('除 H5 外都编译')
// #endif
```

**条件编译的形式**——JS（`// #ifdef`）、模板（`<!-- #ifdef -->`）、样式（`/* #ifdef */`）都支持；**平台标识**——`MP-WEIXIN`（微信）、`MP-ALIPAY`、`H5`、`APP-PLUS`（App）、`APP-HARMONY`（鸿蒙）——**「条件编译是跨端开发的标配武器」**——平台差异代码用条件编译隔离，业务代码保持平台无关（01 篇基线）。

**组合式 API 的工程实践**（2026 标准的代码组织）：**逻辑按功能组织**——「一个功能的 ref/computed/watch/方法放一起」（组合式 API 的核心价值——替代 Options API 的按类型分散）；**自定义组合函数 useXxx**——「请求列表逻辑抽 `useOrderList()`、登录逻辑抽 `useLogin()`」——**「页面 = 组合函数的组装」**（跨页面复用逻辑的标配姿势——与后端 Service 抽样的思维同构）；**响应式的注意**——`ref` 取值要 `.value`（模板里自动解包）、`reactive` 的重新赋值要小心（直接赋值丢失响应——用 ref 或 reactive 对象内更新）、`watch` 的 deep（对象深层监听——性能注意）——**「组合式 API 写多了自然形成自己的 useXxx 库」**——页面代码瘦身的第一手段。

## 7. 常见坑

**坑一：用 v-html**。小程序无 DOM——富文本用 rich-text。

**坑二：模板写复杂逻辑**。长表达式/方法调用堆模板——逻辑进 script。

**坑三：switchTab 用 navigateTo**。tab 页跳转报错——switchTab。

**坑四：传参不转类型**。`options.id` 是字符串——Number() 转换。

**坑五：showLoading 不 hide**。加载圈永久转——配对 hide。

**坑六：用 wx. 不用 uni.**。平台 API 直调——先查 uni 封装（跨端失效）。

**坑七：v-for 无 key**。列表渲染告警/性能——:key。

**坑八：响应式丢失**。reactive 整体重新赋值（`obj = {...}`）丢响应——ref 或对象内更新。

**坑九：computed 里做副作用**。计算属性里发请求/改状态——computed 是纯函数——副作用放 watch/方法。

**坑十：条件编译漏平台**。新平台（鸿蒙）没覆盖——平台标识更新时检查条件编译分支。

**坑十一：uni API 回调忘处理 fail**。只写 success——失败静默——fail 统一处理（05 篇错误三层）。

**坑十二：storage 大对象频繁读写**。大对象每次 setStorageSync 全量序列化——页面卡——storage 存小数据、大数据走状态管理。

**坑十三：事件总线滥用**。uni.$emit/$on 到处用——事件难追踪——跨页共享用 store、跨页通信限制场景（03 篇通信姿势）。

**坑十四：模板里调用复杂方法**。`{{ formatPrice(item.price) }}` 每个渲染都执行——计算放 computed 或预处理数据——「模板保持简单表达式」。

**坑十五：v-model 与表单组件绑定错误**。picker/switch 的 v-model 行为与 input 不同——按组件文档绑定（07 篇表单组件）。

**坑十六：页面 onLoad 与组合式 onMounted 混用时机**。onLoad（uni 页面级——03 篇）与 onMounted（Vue 组件级）触发时机不同——数据初始化用 onLoad、DOM 相关用 onMounted——「生命周期按场景选」。

**坑十七：uni API 的 promise 化**。部分 uni API 支持 promise（`uni.request` 可 promise 化）——统一封装统一风格——「API 风格一致」。

**坑十八：条件编译的注释格式写错**。`// #ifdef` 的 `#` 前空格与 `//` 后空格是硬要求——格式错则编译不生效——「条件编译格式照抄规范」（04 篇示例）。

**坑十九：模板里 index 当 key**。`v-for` 用 index 作 key——列表插入/删除时复用错乱——用业务唯一 id（07 篇 key 纪律）。

## 8. 练习

1. 组合式 API 四件套（ref/reactive/computed/watch）？
2. 模板语法的两个小程序差异（v-html/复杂表达式）？
3. uni API 的五类用途？
4. 导航四件套与传参姿势？
5. 条件编译的三种形式与平台标识？

> 🎯 **核心要点**：组合式 API（script setup）是 2026 标准；模板语法注意小程序限制（无 v-html——rich-text 替代）；uni API 是跨端封装（先查 uni 再查平台）；导航四件套（navigateTo/redirectTo/switchTab/navigateBack——tab 页只能 switchTab）；传参小数据 url 大数据状态（字符串要转换）；showLoading 配对 hide；条件编译隔离平台差异。

---

**下一模块**：[05-网络请求与数据交互](05-网络请求与数据交互.md)｜**返回总览**：[00-小程序UniAPP开发总览](00-小程序UniAPP开发总览.md)
