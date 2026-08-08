# 02-Git 基础操作与入门
> Git 是 Java 后端工程师的第二个 IDE——安装配置、.gitignore、常用命令全流程，从第一个仓库开始

## 📚 目录
1. [Git 概述：分布式版本控制系统](#1-git-概述分布式版本控制系统)
2. [安装与首次配置](#2-安装与首次配置)
3. [.gitignore：Java 项目忽略规则](#3-gitignorejava-项目忽略规则)
4. [初始化 / 克隆 / 状态 / 提交 / 差异](#4-初始化--克隆--状态--提交--差异)
5. [分支基础与撤销简版](#5-分支基础与撤销简版)
6. [SpringBoot 实战：从 init 到版本管理](#6-springboot-实战从-init-到版本管理)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. Git 概述：分布式版本控制系统

Git 由 Linus Torvalds 于 2005 年为管理 Linux 内核开发而创建，是 Java 后端、微服务、DevOps 生态的事实标准工具。

### 1.1 分布式 vs 集中式（SVN）

| 维度 | Git（分布式） | SVN（集中式） |
|------|-------------|-------------|
| **仓库位置** | 每个开发者本地有完整仓库 | 仅中央服务器有完整仓库 |
| **离线工作** | 完全支持，提交/日志/对比均离线 | 不支持，几乎全部操作需联网 |
| **分支成本** | O(1)，创建即指针，切换秒级 | O(n)，创建即复制整个目录 |
| **性能** | 本地操作，毫秒级响应 | 远程操作，受网络延迟影响 |
| **安全性** | 每个节点都是完整备份 | 中央服务器是单点故障 |
| **存储方式** | 内容寻址（基于 SHA-1 的键值存储） | 增量存储（基于文件变更列表） |

> 💡 核心区别一句话：SVN 是一份代码多人看，Git 是每人一份完整代码再同步。**离线可提交、分支即指针、仓库即备份**是 Git 碾压 SVN 的三个根本优势。

### 1.2 Git 核心优势

| 优势 | 说明 |
|------|------|
| 极致的分支与合并能力 | 合并算法成熟稳定 |
| 数据完整性保障 | SHA-1 校验，文件损坏或篡改立即发现 |
| 暂存区设计 | 三态模型，提交前精细控制文件颗粒度 |
| 社区生态 | GitHub/GitLab/Gitee 完整开源协作生态 |

## 2. 安装与首次配置

### 2.1 各平台安装

| 平台 | 安装方式 |
|------|---------|
| Windows | https://git-scm.com 下载安装包（选 Git Bash + "Checkout as-is, commit Unix-style line endings"） |
| macOS | `brew install git` |
| Ubuntu/Debian | `sudo apt install git -y` |
| CentOS/RHEL | `sudo yum install git -y` |
| 验证 | `git --version` → `git version 2.42.0` |

### 2.2 用户身份配置（必须）

```bash
git config --global user.name "Su Xiangyu"
git config --global user.email "xiangyu.su@example.com"
```

> ⚠️ 务必与远程托管平台（GitHub/GitLab）的账号邮箱一致，否则提交不会被关联到账号，贡献统计无法正确显示。

### 2.3 编辑器与行尾配置

```bash
git config --global core.editor "vim"
# Windows 也可配置为 VS Code：
git config --global core.editor "code --wait"

# Windows 行尾配置（解决 CRLF / LF 战争）
git config --global core.autocrlf true     # Windows（推荐）：提交 CRLF→LF，检出 LF→CRLF
git config --global core.autocrlf input    # macOS/Linux：提交 CRLF→LF，检出不转换
# core.autocrlf false：不做任何转换（不推荐，跨平台极易行尾混乱）
```

### 2.4 常用别名（9 条起步）

```bash
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.cm "commit -m"
git config --global alias.lg "log --oneline --graph --all --decorate"
git config --global alias.df diff
git config --global alias.dfc "diff --cached"
git config --global alias.unstage "restore --staged"
git config --global alias.last "log -1 HEAD"
```

### 2.5 配置层级与验证

| 层级 | 文件 | 优先级 |
|------|------|:---:|
| 系统级 | `/etc/gitconfig`（`--system`） | 低 |
| 全局级 | `~/.gitconfig` 或 `~/.config/git/config`（`--global`） | 中 |
| 项目级 | `.git/config`（当前仓库） | **高** |

```bash
git config --list                 # 列出所有配置
git config --list --global        # 仅全局配置
git config user.name              # 查单个配置项
```

## 3. .gitignore：Java 项目忽略规则

### 3.1 完整模板

```gitignore
# ============ Maven / Gradle 构建产物 ============
target/
build/
*.class
*.jar
*.war
*.ear

# ============ IDE 配置文件 ============
.idea/
*.iml
*.iws
*.ipr
.vscode/
.settings/
.project
.classpath
*.swp
*.swo
*~

# ============ 本地配置与敏感信息 ============
application-local.yml
application-dev.yml
application-local.properties
bootstrap-local.yml
*.env
.env.local

# ============ 日志文件 ============
logs/
*.log
log/
**/logs/

# ============ 依赖缓存 ============
.m2/
gradle/
.gradle/
node_modules/
__pycache__/

# ============ 操作系统文件 ============
.DS_Store
Thumbs.db
Desktop.ini

# ============ 临时文件 ============
*.tmp
*.temp
*.bak
*.orig
*.pid
*.seed
*.pid.lock

# ============ 系统与测试报告 ============
test-output/
*.html.report
coverage/
surefire-reports/
```

### 3.2 忽略规则原理

| 模式 | 含义 | 示例 |
|------|------|------|
| `target/` | 忽略整个目录（任意层级） | 匹配所有 `target/` |
| `/target/` | 仅忽略根目录下的 | 不匹配 `sub/target/` |
| `*.log` | 忽略所有 .log 文件 | 匹配任意目录 |
| `!important.log` | 排除（不忽略）该文件 | 与上一行配合 |
| `**/logs/` | 忽略任意深度的 logs 目录 | 匹配 `a/b/logs/` |

> 💡 匹配规则基于文件路径模式：`*` 匹配任意字符串（不含 `/`），`**` 匹配任意层级目录。

### 3.3 已追踪文件的忽略处理

```bash
git rm --cached application-dev.yml     # 移除追踪，保留本地文件
git rm application-dev.yml              # 移除追踪并删除文件
git rm -r --cached target/              # 批量移除目录追踪
```

> ⚠️ `git rm --cached` 只移除追踪不删文件；提交后 `.gitignore` 才对新克隆生效。但**历史版本中仍存在该文件**，彻底抹除需 `git filter-branch` 或 BFG（见 [11](11-Git配置安全与最佳实践.md)）。

## 4. 初始化 / 克隆 / 状态 / 提交 / 差异

### 4.1 初始化与克隆

```bash
git init                                            # 初始化仓库
git clone https://github.com/user/spring-boot-demo.git   # 标准克隆
git clone -b develop https://github.com/user/xxx.git     # 指定分支
git clone --depth 1 https://github.com/user/xxx.git      # 浅克隆（大仓库加速）
git clone https://github.com/user/xxx.git my-app         # 指定本地目录名
```

### 4.2 git status 与文件标记

```bash
git status          # 详细状态
git status -s       # 简洁状态（单行）
#  M README.md        ← 空格+M: modified, 未暂存
# A  src/main/App.java ← A: added, 已暂存
# ?? .env              ← ?? : 未追踪
```

| 标记 | 含义 | 标记 | 含义 |
|------|------|------|------|
| `??` | 未追踪 | `A ` | 新增已暂存 |
| ` M` | 已修改未暂存 | ` D` | 已删除未暂存 |
| `M ` | 已暂存 | `D ` | 已删除已暂存 |
| `MM` | 已暂存且又修改 | | |

> 💡 两列含义：**第一列** = 暂存区状态（相对上次提交）；**第二列** = 工作区状态（相对暂存区）。

### 4.3 添加与提交

```bash
git add README.md          # 添加单个文件
git add -A                 # 添加所有变更（含新增/修改/删除）
git add .                  # 同上（简写）
git add -p                 # 交互式添加（选择部分变更，见 07）
git commit -m "feat: 初始化SpringBoot项目"
git commit -am "fix: 修复空指针异常"   # 跳过暂存区直接提交（新增文件仍需 add）
git commit --amend -m "..."           # 修改最近一次提交（未推送时）
```

> ⚠️ `git commit --amend` 会生成新的 commit hash 替换原提交；已推送则需 `--force-with-lease`。

### 4.4 查看差异（9 种用法）

```bash
git diff                       # 工作区 vs 暂存区（未暂存的修改）
git diff --cached              # 暂存区 vs 版本库（已暂存未提交）
git diff --staged              # 同上
git diff HEAD                  # 工作区 vs 版本库（所有未提交修改）
git diff commitA..commitB      # 两个提交之间
git diff main..develop         # 两个分支之间
git diff --name-only           # 仅显示文件名
git diff --name-status         # 文件名 + 状态（M/A/D）
git diff --stat                # 统计变更行数
```

## 5. 分支基础与撤销简版

### 5.1 分支管理

```bash
git branch <name>          # 创建分支
git checkout -b <name>     # 创建并切换（等价 switch -c）
git merge <branch>         # 合并分支
git branch -d <name>       # 删除分支（已合并）
git branch -D <name>       # 强制删除
```

- **合并策略**：Fast-Forward（目标分支无新提交 → 直接移动指针）；Three-way Merge（两个分支都有新提交 → 生成合并提交）。
- **合并冲突**：同一文件同一位置被不同分支修改 → 手动解决 → `git add` → `git commit`（完整冲突指南见 [04](04-Git合并与变基.md)）。

### 5.2 撤销简版（完整版见 [05](05-Git撤销与历史重写.md)）

| 场景 | 命令 |
|------|------|
| 撤销工作区修改（未 add） | `git restore <file>`（旧版 `git checkout -- <file>`） |
| 取消暂存（已 add） | `git restore --staged <file>`（旧版 `git reset HEAD <file>`） |
| 撤销 commit 保留修改 | `git reset --soft HEAD~1` |
| 撤销 commit + 暂存 | `git reset HEAD~1`（--mixed 默认） |
| 彻底丢弃 | `git reset --hard HEAD~1`（⚠️ 危险，reflog 可救） |
| 撤销已推送的提交 | `git revert <commit>`（安全，新增反向提交） |

> 🎯 **核心原则**：**未推送** → `git reset` 随意使用；**已推送到公共分支** → 必须 `git revert`；**已推送到个人分支** → 谨慎 `git reset --hard` + `git push --force-with-lease`。

### 5.3 远程协作简版（完整版见 [06](06-Git远程协作与托管平台.md)）

```bash
git remote add origin <url>    # 添加远程仓库
git push origin main           # 推送本地分支到远程
git pull = git fetch + git merge   # 拉取并合并
git fetch origin               # 只拉取不合并（安全）
```

GitHub/GitLab 协作 4 步：① Fork 主仓库 → Clone 自己的 Fork；② 创建 feature 分支 → 开发 → commit；③ Push 到自己的 Fork → 发起 Pull Request；④ Code Review → 修改 → Merge。

## 6. SpringBoot 实战：从 init 到版本管理

### 6.1 第一阶段：环境准备

```bash
mkdir spring-boot-demo && cd spring-boot-demo
git init

# 创建 .gitignore（精简版，完整模板见第 3 节）
cat > .gitignore << 'EOF'
target/
*.class
*.jar
*.war
.idea/
*.iml
*.log
logs/
application-local.yml
.env
.DS_Store
EOF

echo "# Spring Boot Demo Project" > README.md
git add .
git commit -m "chore: 初始化SpringBoot项目结构

- 初始化Git仓库
- 添加Java项目.gitignore
- 添加项目README"
```

### 6.2 第二阶段：分步提交（每步一个原子提交）

```bash
mkdir -p src/main/java/com/demo/controller src/main/java/com/demo/service src/main/resources
```

```java
// src/main/java/com/demo/DemoApplication.java
package com.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

```yaml
# src/main/resources/application.yml
server:
  port: 8080
spring:
  application:
    name: spring-boot-demo
```

```bash
git add pom.xml
git commit -m "chore: 添加Maven构建配置

- 配置Spring Boot Starter Parent
- 添加spring-boot-starter-web依赖"

git add src/main/java/com/demo/DemoApplication.java
git add src/main/resources/application.yml
git commit -m "feat: 初始化SpringBoot主应用入口

- 创建DemoApplication启动类
- 配置应用端口和名称"
```

### 6.3 第三阶段：迭代提交

```java
// src/main/java/com/demo/controller/GreetingController.java
package com.demo.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    @PostMapping("/greet")
    public Map<String, String> greet(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "World");
        return Map.of("message", "Hello, " + name + "!");
    }
}
```

```bash
git add src/main/java/com/demo/controller/GreetingController.java
git commit -m "feat: 新增GreetingAPI

- POST /api/greet 接收name参数返回个性化问候
- 默认name为World"

# 修复 HelloController 返回格式为 JSON 对象（Map 封装）
git add src/main/java/com/demo/controller/HelloController.java
git commit -m "fix: HelloController返回格式改为JSON对象

- 原String类型改为Map封装，保持API响应格式统一"

git lg   # 查看最终提交历史（需先配置 lg 别名，见 2.4 节）
```

### 6.4 查看成果

```bash
git log --oneline
# 2d1e3a4 fix: HelloController返回格式改为JSON对象
# 8f4b9c1 feat: 新增GreetingAPI
# a1b2c3d feat: 初始化SpringBoot主应用入口
# e5f6g7h chore: 添加Maven构建配置
# 9i0j1k2 chore: 初始化SpringBoot项目结构

git log --stat                      # 查看文件变更
git show 8f4b9c1                    # 查看某次提交的具体变更
```

**实战要点**：① 先写 .gitignore 再开发，避免编译产物被追踪；② 一个功能一次 commit，message 用 Conventional Commits 规范；③ 频繁提交、尽早提交，每次提交都是可恢复的备份点；④ 提交前 `git status` + `git diff` 确认内容；⑤ 合理使用分支，main 始终保持可发布状态。

## 7. 核心要点

> 🎯 **核心要点**：
> - Git 三优势：离线可提交、分支即指针、仓库即备份；
> - 三件套配置：身份（user.name/email）、行尾（core.autocrlf）、别名（st/co/lg）；
> - .gitignore 先于开发写好，Java 模板按"构建产物/IDE/本地配置/日志/OS 文件"五类组织；
> - 状态标记两列读法：第一列暂存区、第二列工作区；
> - 提交纪律：一个逻辑单元一次提交 + Conventional Commits 消息。

## 8. 参考来源

- [Pro Git Book（第 1-3 章）](https://git-scm.com/book/zh/v2)
- [Git 官方文档](https://git-scm.com/docs)
- [Conventional Commits 规范](https://www.conventionalcommits.org/zh-hans/)

---

**下一模块**：[03-Git分支模型与团队工作流](03-Git分支模型与团队工作流.md)　/　**返回总览**：[00-总览](00-Git知识体系总览.md)
