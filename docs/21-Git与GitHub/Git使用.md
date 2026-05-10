# 第一次用 Git 必须做的操作（只做一次）

``` bash

\# 1. 初始化本地仓库（只在第一次用）

git init



\# 2. 关联远程仓库（只在第一次用）

git remote add origin https://github.com/suxiangyu138/你的仓库.git



\# 3. 建立本地分支与远程分支的追踪关系（只在第一次用）

git branch -M main

git pull origin main --allow-unrelated-histories

git push -u origin main

```
  
``` bash
 # 拉取远程分支并合并
  git pull origin main --allow-unrelated-histories
  # 解决可能的冲突后，再执行 push
  git push -u origin main
  ```



# 后面用 Git 的日常操作（每次都要做）



``` bash

运行

 1. 把修改的文件加入暂存区

git add .



2. 提交修改（两种方式）

 方式A：直接在命令行写提交说明（推荐，不弹编辑器）

git commit -m "feat: 新增功能"

 方式B：弹出你设置的记事本/VS Code写提交说明

git commit



 3. 拉取远程最新代码（避免推送失败）

git pull



\# 4. 推送到远程仓库

git push

```

### 🔍 问题分析
你遇到的核心问题是：**本地仓库创建后，没有提交任何代码，直接执行 `git push` 失败**，提示 `src refspec main does not match any`。
- `git init` 创建了本地仓库
- `git remote add origin ...` 关联了远程仓库
- `git branch -M main` 将默认分支改为 `main`
- 此时本地仓库**没有任何提交记录**，`main` 分支是空的，所以无法 push

---
### ✅ 完整解决方案（按顺序执行）
#### 1. 先提交代码到本地仓库
```bash
# 1. 把当前目录所有文件加入暂存区
git add .

# 2. 提交到本地仓库（必须有一次提交，main 分支才会被真正创建）
git commit -m "first commit: init doc"
```

#### 2. 重新设置上游分支并推送
```bash
# 3. 关联远程分支并推送
git push -u origin main
```

---
### 💡 补充说明
- 你执行 `git pull` 提示没有跟踪信息，是因为本地 `main` 分支还没有和远程 `origin/main` 建立关联，在完成上述 `git push -u` 命令后，关联会自动建立。
- 如果远程仓库已有文件（比如 GitHub 自动创建的 README），直接 push 会失败，此时可以：
  ```bash
  # 拉取远程分支并合并
  git pull origin main --allow-unrelated-histories
  # 解决可能的冲突后，再执行 push
  git push -u origin main
  ```

---


