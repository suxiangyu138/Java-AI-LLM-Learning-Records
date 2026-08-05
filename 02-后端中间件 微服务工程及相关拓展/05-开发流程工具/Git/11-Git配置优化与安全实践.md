# Git 配置优化与安全实践

## 三层配置体系

```bash
# System (所有用户) — 最低优先级
git config --system --list
# 文件位置: /etc/gitconfig

# Global (当前用户) — 中等优先级
git config --global --list
# 文件位置: ~/.gitconfig 或 ~/.config/git/config

# Local (当前仓库) — 最高优先级
git config --local --list
# 文件位置: .git/config

# 查看某个配置的来源
git config --show-origin user.name
```

---

## 1. 基础身份配置

```bash
# 必须设置
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 按仓库设置不同身份（工作用工作邮箱，个人用个人邮箱）
git config --local user.email "work@company.com"

# 设置默认分支名（Git 2.28+）
git config --global init.defaultBranch main

# 设置默认编辑器
git config --global core.editor "code --wait"     # VS Code
git config --global core.editor "vim"             # Vim
```

---

## 2. 命令行别名（提高效率）

### 2.1 常用别名

```bash
# 状态相关
git config --global alias.st "status"
git config --global alias.sts "status -s"        # 简短状态

# 日志相关
git config --global alias.lg "log --oneline --graph --all"
git config --global alias.lg10 "log --oneline --graph --all -10"
git config --global alias.ll "log --oneline --graph --all --decorate --pretty=format:'%C(yellow)%h%Creset %C(cyan)%ad%Creset %s %C(green)%d%Creset' --date=short"

# 提交相关
git config --global alias.ca "commit --amend"
git config --global alias.can "commit --amend --no-edit"
git config --global alias.cm "commit -m"

# 分支相关
git config --global alias.br "branch"
git config --global alias.co "checkout"
git config --global alias.sw "switch"
git config --global alias.cob "checkout -b"      # 创建并切换

# 撤销相关
git config --global alias.unstage "restore --staged ."
git config --global alias.discard "restore ."
git config --global alias.undo "reset --soft HEAD~1"

# Diff
git config --global alias.d "diff"
git config --global alias.ds "diff --staged"
git config --global alias.dc "diff --cached"     # 同 --staged

# Reflog
git config --global alias.rl "reflog"
```

### 2.2 高级别名（带 shell 命令）

```bash
# 查看每个贡献者的提交数
git config --global alias.rank "shortlog -sn --no-merges"

# 删除已合并的本地分支
git config --global alias.clean-branches '!git branch --merged | grep -v "\\*\\|main\\|master" | xargs -r git branch -d'

# 查看今天做了什么
git config --global alias.today '!git log --oneline --since="6am" --author="$(git config user.name)"'

# 查看贡献者排名
git config --global alias.who "shortlog -sn --no-merges"

# 撤销上一次commit（保留修改）
git config --global alias.undo-commit "reset --soft HEAD~1"
```

---

## 3. 差异与合并工具

```bash
# 使用 VS Code 作为 diff 工具
git config --global diff.tool vscode
git config --global difftool.vscode.cmd "code --wait --diff \$LOCAL \$REMOTE"

# 使用 VS Code 作为 merge 工具
git config --global merge.tool vscode
git config --global mergetool.vscode.cmd "code --wait \$MERGED"

# 更直观的 diff 输出
git config --global diff.colorMoved zebra     # 移动的代码用不同颜色
git config --global diff.colorMovedWS allow-indentation-change

# 启用 diff 算法改进（更易读的 diff）
git config --global diff.algorithm histogram
```

---

## 4. 性能优化

```bash
# 启用文件系统缓存（Windows 必开）
git config --global core.fscache true

# 增大缓冲区，加速大文件操作
git config --global http.postBuffer 524288000   # 500MB

# 并行 fetch（加速 clone 和 fetch）
git config --global fetch.parallel 8

# 启用 commit-graph（加速 log 和 blame）
git config --global gc.writeCommitGraph true

# 文件系统监控（大型仓库加速 git status）
git config --global core.untrackedCache true

# 启用多包索引（加速 object 查找）
git config --global core.multiPackIndex true
```

---

## 5. 安全实践

### 5.1 GPG 签名——证明"这段代码确实是我提交的"

```bash
# 1. 生成 GPG 密钥
gpg --full-generate-key
# 选择 RSA and RSA, 4096 bits, 设置过期时间

# 2. 列出密钥
gpg --list-secret-keys --keyid-format LONG
# sec   rsa4096/3AA5C34371567BD2 2025-01-01 [SC]
# 上面 3AA5C34371567BD2 就是 GPG key ID

# 3. 配置 Git 使用 GPG
git config --global user.signingkey 3AA5C34371567BD2
git config --global commit.gpgsign true    # 所有commit自动签名
git config --global tag.gpgsign true       # 所有tag自动签名

# 4. 导出公钥
gpg --armor --export 3AA5C34371567BD2
# 粘贴到 GitHub → Settings → SSH and GPG keys → New GPG key

# 5. 验证签名
git log --show-signature
git verify-commit HEAD
git verify-tag v1.0.0
```

### 5.2 SSH 签名（Git 2.34+ 替代 GPG）

```bash
# 使用 SSH key 签名
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true

# 添加 allowed signers 文件
echo "$(git config user.email) $(cat ~/.ssh/id_ed25519.pub)" >> ~/.ssh/allowed_signers
git config --global gpg.ssh.allowedSignersFile ~/.ssh/allowed_signers
```

### 5.3 保护敏感信息

```bash
# 防止意外提交 .env / 密钥 等文件
# 1. 在 .gitignore 中忽略
echo ".env" >> .gitignore
echo "*.pem" >> .gitignore
echo "credentials.json" >> .gitignore

# 2. 如果已经提交了敏感信息，从历史中彻底删除
# ⚠️ 这会改写历史！确保团队知晓
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all

# 更现代的方式（推荐）
git filter-repo --path .env --invert-paths

# 3. 使用 git-secrets 扫描
# https://github.com/awslabs/git-secrets
git secrets --scan

# 4. 使用 .gitattributes 标记需要 diff 忽略的文件
# .gitattributes
secrets.json filter=git-crypt diff=git-crypt
```

### 5.4 分支保护

```bash
# 在 GitHub/GitLab 上设置（非 Git 本身的功能）
# - 要求 PR review 通过才能 merge
# - 要求 CI 通过
# - 禁止 force push main 分支
# - 要求 commit 签名
# - 要求 linear history (禁止 merge commit)
```

---

## 6. 推荐的 .gitconfig

一个完整的个人配置示例：

```ini
[user]
    name = Your Name
    email = your.email@example.com
    signingkey = 3AA5C34371567BD2

[init]
    defaultBranch = main

[core]
    editor = code --wait
    fscache = true
    autocrlf = input              # Windows 上设为 true
    untrackedCache = true

[commit]
    gpgsign = true

[tag]
    gpgsign = true

[pull]
    rebase = true                 # git pull 默认 rebase

[fetch]
    parallel = 8
    prune = true                  # fetch 时自动清理远程已删分支

[rebase]
    autoStash = true              # rebase 前自动 stash

[diff]
    algorithm = histogram
    colorMoved = zebra
    tool = vscode

[merge]
    tool = vscode
    conflictstyle = diff3         # 显示共同祖先版本

[rerere]
    enabled = true                # 记住冲突解决方案

[gc]
    writeCommitGraph = true

[alias]
    st = status
    lg = log --oneline --graph --all -20
    co = checkout
    sw = switch
    unstage = restore --staged .
    discard = restore .
    undo = reset --soft HEAD~1
    amend = commit --amend --no-edit
    clean-branches = !git branch --merged | grep -v "\\*\\|main\\|master" | xargs -r git branch -d
```

---

## 7. 条件配置（多身份自动切换）

```ini
# ~/.gitconfig
[includeIf "gitdir:~/work/"]
    path = ~/.gitconfig-work

[includeIf "gitdir:~/personal/"]
    path = ~/.gitconfig-personal
```

```ini
# ~/.gitconfig-work
[user]
    name = Work Name
    email = work@company.com
```

```ini
# ~/.gitconfig-personal
[user]
    name = Personal Name
    email = personal@gmail.com
```

`~/work/` 下的所有仓库自动用工作身份，`~/personal/` 下的自动用个人身份。

---

## 8. 配置速查

| 配置 | 效果 |
|------|------|
| `pull.rebase true` | `git pull` 默认用 rebase |
| `fetch.prune true` | fetch 时自动清理远程已删分支 |
| `rebase.autoStash true` | rebase 前自动 stash |
| `merge.conflictstyle diff3` | 冲突时显示三栏（当前/祖先/对方） |
| `rerere.enabled true` | 自动记住冲突解决 |
| `gc.writeCommitGraph true` | 加速 log/blame 命令 |
| `commit.gpgsign true` | 所有 commit 自动签名 |
| `diff.algorithm histogram` | 更易读的 diff |
| `core.autocrlf input` | 统一换行符处理 |

---

> 上一篇：[10-Git高级调试与取证工具](10-Git高级调试与取证工具.md)
> 下一篇：[12-Git常见陷阱与排错指南](12-Git常见陷阱与排错指南.md)
