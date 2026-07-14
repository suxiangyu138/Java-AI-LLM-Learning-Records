# GitHub 仓库重命名后的处理方法

GitHub 仓库改名后，最重要的处理动作是同步更新本地仓库的远程地址 `origin`，并检查所有依赖旧仓库地址的外部引用是否仍然有效。[cite:30][cite:44]

## 核心结论

GitHub 在仓库重命名后通常会对旧仓库链接提供重定向，因此访问旧网页地址以及针对旧地址执行部分 Git 操作通常仍可继续工作一段时间。[cite:30][cite:44] 但官方仍建议尽快把本地远程地址改成新的仓库 URL，避免后续维护、协作或自动化流程出现混乱。[cite:30]

## 本地仓库需要做什么

在本地项目根目录中，先查看当前远程地址，再把 `origin` 修改为新仓库地址，最后再次确认修改结果。[cite:30][cite:35]

```bash
git remote -v
git remote set-url origin https://github.com/用户名/新仓库名.git
git remote -v
```

如果本地仓库使用的是 SSH 方式连接 GitHub，则应把远程地址改成 SSH 地址。[cite:30]

```bash
git remote set-url origin git@github.com:用户名/新仓库名.git
```

更新完成后，可以执行一次 `git push` 或 `git fetch`，确认新的远程地址工作正常。[cite:30][cite:35]

## GitHub 会自动处理的内容

仓库改名后，GitHub 会保留旧地址到新地址的重定向，这意味着很多旧链接在一段时间内仍然可以访问到新的仓库页面。[cite:30][cite:44] 根据 GitHub 官方说明，仓库相关的 issues、stars、wiki 等内容会跟随仓库一起保留，而不是因为改名而丢失。[cite:30]

这类自动处理机制可以降低改名带来的影响，但它更像兼容措施，不应替代手动更新配置和文档。[cite:30][cite:37]

## 需要手动检查的地方

仓库名变更后，所有写死旧仓库地址的地方都应该检查一遍，尤其是 README、个人主页、博客、简历、项目文档和其他仓库中的引用链接。[cite:30][cite:37]

下面这些内容最值得重点核对：

- README 中的仓库链接、徽章链接、截图链接。[cite:30]
- 个人主页、博客、简历或笔记中写死的旧仓库地址。[cite:30][cite:37]
- GitHub Pages 相关地址，因为 Pages 不会像普通仓库页面那样自动保持同等重定向行为，改名后旧站点链接可能失效。[cite:30][cite:37]
- GitHub Actions 的外部引用；如果其他仓库把该仓库当作一个 Action 来调用，改名后这类引用可能无法自动重定向。[cite:30][cite:44]

## 不要踩的坑

仓库改名后，不建议立刻重新创建一个同名的旧仓库，因为旧地址的重定向可能会被新的同名仓库占用，从而导致原本指向新仓库的跳转失效。[cite:30][cite:31][cite:44]

如果项目已经被写进简历、博客或多个外部页面，改名前后最好保留一段检查窗口，确认新旧地址、页面访问和推送拉取都没有问题，再继续做其他命名调整。[cite:30][cite:37]

## 推荐处理顺序

1. 在 GitHub 上完成仓库改名，复制新的 HTTPS 或 SSH 地址。[cite:44]
2. 进入本地项目目录，执行 `git remote set-url origin 新地址`。[cite:30]
3. 使用 `git remote -v` 确认远程地址已经更新成功。[cite:35]
4. 执行一次 `git fetch` 或 `git push`，确认仓库连接正常。[cite:30]
5. 检查 README、个人主页、简历、博客、GitHub Pages 和其他自动化引用中的旧链接。[cite:30][cite:37]
6. 不要重新占用旧仓库名，以免破坏 GitHub 的重定向关系。[cite:30][cite:31]

## 适合直接执行的命令模板

### HTTPS

```bash
git remote -v
git remote set-url origin https://github.com/用户名/新仓库名.git
git remote -v
git fetch
git push
```

### SSH

```bash
git remote -v
git remote set-url origin git@github.com:用户名/新仓库名.git
git remote -v
git fetch
git push
```

## 使用建议

如果这是公开展示型仓库，改名后除了保证 Git 命令可用，更应该把 README 标题、项目介绍、徽章、目录说明和简历中的仓库链接一起统一更新，这样仓库展示会更专业，也能避免面试或分享时出现名称不一致的问题。[cite:30][cite:37]
