# 快速学会 GitHub + Git 实操
定位：**从零上手、日常提交/托管/开源协作/备份代码**，只学刚需，拒绝废话。
> 前提：Git 是本地版本工具，GitHub 是远程代码仓库平台。

---

## 一、核心概念（3句话记牢）
1. **Git**：本地工具，管理代码版本、回退、对比、分支。
2. **GitHub**：远程云端仓库，存代码、备份、开源、协作。
3. 核心流程：**本地写代码 → Git提交本地 → 推送到GitHub远程**。

---

## 二、环境准备
### 1. 安装 Git
Windows 直接装 Git For Windows，一路默认下一步。
验证：
```bash
git --version
```

### 2. 全局配置（只配置一次）
```bash
git config --global user.name "你的用户名"
git config --global user.email "你的github邮箱"
```

### 3. 注册 GitHub
官网：github.com
你已有账号：**suxiangyu138**

---

## 三、四大核心指令（必背）
```bash
git init         # 初始化本地仓库
git add .        # 全部文件加入暂存区
git commit -m "提交描述"  # 提交到本地仓库
git push         # 推送到远程 GitHub
```

---

## 四、场景1：本地已有项目，上传到 GitHub（最常用）
1. 去 GitHub 新建仓库
   - 点 `New repository`
   - 填仓库名，**不要勾选初始化README**
   - 创建

2. 本地项目根目录，右键 `Git Bash` 执行：
```bash
# 1. 初始化git仓库
git init

# 2. 绑定远程仓库地址
git remote add origin https://github.com/suxiangyu138/仓库名.git

# 3. 添加所有文件
git add .

# 4. 本地提交
git commit -m "初次提交：项目初始化"

# 5. 推送到远程main分支
git push -u origin main
```

---

## 五、场景2：拉取别人代码 / 克隆仓库
```bash
git clone 仓库地址
```
示例：
```bash
git clone https://github.com/suxiangyu138/demo.git
```

---

## 六、日常更新代码标准流程（每天必用）
修改代码后，固定三步：
```bash
git add .
git commit -m "更新：修复bug/新增功能"
git push
```

---

## 七、拉取远程最新代码（多人协作必备）
```bash
git pull
```
作用：同步 GitHub 远端最新改动到本地。

---

## 八、分支基础（刚需，面试+开发必用）
```bash
# 查看所有分支
git branch

# 创建新分支
git branch dev

# 切换分支
git checkout dev

# 创建并直接切换
git checkout -b dev
```

- `main` / `master`：主分支，稳定代码
- 开发在 dev 分支，写完合并到主分支

---

## 九、忽略文件 .gitignore（超重要）
新建文件：`.gitignore`
作用：过滤不需要上传的文件（编译产物、缓存、配置、依赖）

Java/AI 通用模板，直接复制：
```
# 编译目录
target/
out/

# 系统文件
.DS_Store
Thumbs.db

# IDEA
.idea/
*.iml

# 日志
*.log

# 环境配置
.env
```
写入后，被匹配的文件**不会提交到 GitHub**。

---

## 十、SSH 免密推送（解决每次输密码）
1. 生成密钥
```bash
ssh-keygen -t ed25519
```
一路回车。
2. 复制公钥
3. GitHub → Settings → SSH keys 粘贴添加
4. 仓库地址换成 `ssh` 格式，后续 `git push` 无需账号密码。

---

## 十一、高频问题快速解决
### 1. 推送报错：本地与远程冲突
```bash
git pull --rebase origin main
```

### 2. 撤销本地修改（未commit）
```bash
git checkout .
```

### 3. 删除远程绑定
```bash
git remote remove origin
```

---

## 十二、适配你方向的高阶用法（后续要用）
1. 用 GitHub 托管：
   - Java 后端项目
   - Bash/Bat 脚本
   - Docker Compose 配置
   - 学习笔记、Markdown 文档
2. 搭建个人开源作品集，**求职加分**
3. 结合 BAT 脚本，写一键自动提交脚本，全自动备份代码

---

## 十三、最简学习路线
1. 掌握：`clone / add / commit / push / pull`
2. 学会：.gitignore 配置
3. 学会：分支切换
4. 配置 SSH 免密
5. 用 GitHub 长期托管你的代码

---
