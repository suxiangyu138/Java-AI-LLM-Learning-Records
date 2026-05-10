
## 技术主线
你的 Vue 学习和项目选择，建议固定在这套主线：Vue 3 + Vite + TypeScript + Vue Router + Pinia + Axios + Element Plus，因为 Vue 官方推荐 Vue 3 + TS 迁移到 Vite，Pinia 是 Vue 生态的主流状态管理方案，而 Element Plus 是成熟的 Vue 3 组件库。 [vuejs](https://vuejs.org/guide/typescript/overview)
这条路线的价值不只是“会写页面”，而是能直接承接后台管理系统、前后端分离项目，以及 AI 对话/RAG 前端，和你之前关注的 SpringBoot + Vue 权限系统、Java+AI 项目形成闭环。
## 必做项目
如果你只做 6 个，我建议优先做下面这组，覆盖基础、就业、全栈、AI 融合四层能力。

| 项目 | 技术重点 | 简历价值 |
|---|---|---|
| TodoList 待办管理 | `ref/reactive`、计算属性、侦听器、组件拆分 | 夯实 Vue3 响应式基础 |
| 商品购物车 | `v-model`、父子通信、复杂状态同步 | 练熟组件协作与业务交互 |
| Vue3 极简后台管理系统 | Vue Router、Pinia、Axios、Element Plus、表单校验 | 最接近校招前端/全栈基础项目  [pinia.vuejs](https://pinia.vuejs.org) |
| Vue3 + TS 权限管理系统 | 动态路由、RBAC、按钮级权限、请求拦截 | 直接对接 Java 后端权限系统  |
| AI 对话聊天 Web 端 | WebSocket 流式输出、Markdown 渲染、会话管理 | 贴合你的 Java + AI 方向  |
| RAG 知识库问答前端 | 文件上传、知识库管理、引用溯源、分页表格 | 和 Spring AI / RAG 后端强绑定  |
## 分阶段路线
第一阶段先做 TodoList + 购物车，目标不是项目多炫，而是把组合式 API、组件通信、状态更新、列表渲染彻底练熟。
第二阶段做 Vue3 后台管理系统，重点练路由、状态管理、请求封装、表单与表格，这是最典型的企业开发形态；Pinia 作为 Vue 官方生态中的直观状态管理方案，也很适合这一步上手。 [element-plus](https://element-plus.org)

第三阶段上 TypeScript 权限系统，把路由权限、按钮权限、菜单权限、用户角色管理全部补齐，这一步能明显拉开你和“只会写页面”的同学差距。
第四阶段直接做 AI 对话 Web 端和 RAG 前端，把 Vue 前端能力和你的 SpringBoot / Spring AI / Java 后端能力绑定在一起，形成真正有差异化的项目组合。
## 每层项目清单
### 1. 入门级
- TodoList 待办管理：增删改查、状态筛选、本地存储、批量操作；核心练 `ref/reactive`、计算属性、侦听器、组件拆分。  
- 计数器 + 暗黑模式：练自定义指令、CSS 变量、主题切换。  
- 商品购物车：练父子通信、`v-model`、总价计算、复杂列表联动。
### 2. 进阶级
- Vue3 后台管理系统：登录页、侧边栏、面包屑、表格 CRUD、分页、表单校验；技术栈固定为 Vue3 + Vite + Vue Router + Pinia + Element Plus + Axios。 [vite](https://vite.dev/guide/)
- 个人博客前台：文章列表、详情、分类标签、评论、搜索、暗黑模式。  
- 音乐播放器：播放列表、进度条、歌词滚动、音量控制，练组件封装和全局状态。
### 3. 高阶级
- Vue3 + TS 企业级权限管理系统：动态路由、RBAC、按钮权限、菜单管理、请求拦截。  
- 数据可视化大屏：ECharts、多图表联动、自适应缩放、主题切换。  
- 仿 B 站短视频页面：无限滚动、视频懒加载、下拉刷新、性能优化。
### 4. 全栈融合
- 前后端分离电商系统：首页、商品详情、购物车、订单、地址、支付页；对接 SpringBoot + JWT + Redis。
- 在线考试系统前端：登录、题库、在线答题、自动交卷、成绩展示；适合对接 Java 后端与 WebSocket 实时计时。
### 5. AI 融合
- AI 对话聊天 Web 端：对话气泡、历史会话、代码高亮、流式输出；对接 Spring AI 后端最合适。
- RAG 知识库问答系统前端：文档上传、知识库管理、问答交互、引用溯源、文件预览。  
- AI 绘图生成页面：提示词输入、风格选择、生成历史、图片下载。
## 简历标准
你做 Vue 项目时，别按“练习小 demo”交付，而要按企业项目标准来：统一目录结构、接口层封装、错误提示统一、权限控制清晰、README 完整。 [perplexity](https://www.perplexity.ai/search/4baf1a85-9adc-4517-bff3-80bd3f0ff066)
建议每个项目至少做到这些：

- 技术栈固定：Vue3 + Vite + TS。
- 目录规范：`api`、`views`、`components`、`stores`、`router`、`utils`、`types`。
- 工程规范：ESLint + Prettier + `.env` 多环境配置。
- 网络规范：Axios 二次封装、请求/响应拦截器、统一错误处理。
- 权限规范：路由守卫、登录态校验、按钮级权限。
- 展示规范：GitHub + 标准 README + 项目截图 + 部署说明。 [perplexity](https://www.perplexity.ai/search/4baf1a85-9adc-4517-bff3-80bd3f0ff066)
## 最适合你的顺序
最适合你的不是“把所有 Vue 项目都做一遍”，而是下面这条最省时、最能出成果的路线：

1. TodoList  
2. 商品购物车  
3. Vue3 极简后台管理系统  
4. Vue3 + TS 权限管理系统  
5. AI 对话聊天 Web 端  
6. RAG 知识库问答系统前端  
