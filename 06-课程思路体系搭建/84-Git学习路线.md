# Git学习路线

> Java后端入门标准Git实战课，覆盖本地Git操作 + 远程仓库 + IDEA集成Git，完全贴合企业开发流程。

### 一、课程整体定位

该课程面向Java后端入门开发者，系统讲解Git版本控制的核心知识与实战技能，包含本地Git操作、远程仓库（Gitee码云）使用、IDEA集成Git三大模块，可满足校招与实习日常开发需求。

### 二、14集逐节核心考点与学习重点

- 1 前言之版本控制 — 版本控制概念、解决的问题（文件多版本混乱、多人协作冲突）；集中式与分布式版本控制基础概念
- 2 Git和SVN的区别（面试高频简答） — SVN集中式（必须联网、中央仓库单点故障、本地无完整版本快照） vs Git分布式（本地完整仓库、断网可提交、分支轻量速度快）
- 3 聊聊Git的历史 — 诞生背景（Linux内核开发需求），科普内容
- 4 安装Git及环境配置 — Windows安装步骤、Git Bash终端使用，区分图形工具与命令行
- 5 常用的Linux命令 — Git内置Linux基础命令：cd、ls、mkdir、rm、pwd，Git终端操作必备前置知识
- 6 Git的必要配置（必操作） — 全局用户名、邮箱配置（提交代码身份标识），`git config --global user.name` / `git config --global user.email` / `git config --global --list`
- 7 Git的工作原理（重中之重，面试必考） — 四大区域：工作区、暂存区（stage/index）、本地仓库（.git）、远程仓库；流转流程：工作区 → add → 暂存区 → commit → 本地仓库 → push → 远程仓库
- 8 Git项目创建及克隆 — 本地新建仓库 `git init`；拉取远程项目 `git clone 仓库地址`
- 9 Git的基本操作命令（日常高频） — `git add .`、`git commit -m "提交备注"`、`git status`、`git log`、`git reset`
- 10 码云Gitee的注册和使用 — 国内替代GitHub，企业国内开发主流远程仓库；创建仓库、HTTPS地址拉取推送
- 11 配置SSH公钥及创建远程仓库 — 解决每次push重复输账号密码问题；生成ssh密钥 → 公钥复制到Gitee账号 → 用SSH地址关联仓库，`ssh-keygen -t rsa`
- 12 IDEA中集成Git操作（开发天天用） — 图形化操作：提交、推送、拉取、切换分支、解决冲突
- 13 Git分支说明（团队协作核心） — 主分支main/master（线上稳定代码）、开发分支dev/feature、热修复分支hotfix；基础命令：`git branch`、`git checkout`、`git merge`
- 14 Git后续操作说明 — 冲突解决、版本回退、暂存区撤销、多人协作规范（企业开发标准流程）

### 三、学习规划建议（速成2天吃透）

- 第一天：1~9集，吃透本地Git原理与全部基础命令，手动敲一遍所有指令
- 第二天：10~14集，搭建远程仓库、配置SSH、IDEA图形操作、分支协作实战

### 四、求职加分重点（面试必背）

- Git分布式与SVN集中式差异
- Git四大工作区流转逻辑
- SSH免密配置流程
- 分支管理规范、代码冲突解决步骤
- IDEA集成Git完整流程
