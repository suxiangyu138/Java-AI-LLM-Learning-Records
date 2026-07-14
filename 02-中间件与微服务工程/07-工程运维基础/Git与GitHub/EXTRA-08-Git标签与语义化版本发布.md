# Git 标签与语义化版本发布

## 为什么需要标签？

分支是可移动的指针，但标签（tag）是**固定**的指针——永远指向某个 commit。它标记了"那个版本在这里"。

```text
分支: main → ●  ●  ●  ●  ● (随时在移动)
标签: v1.0.0 → ● (永远钉在这里)
```

---

## 1. 两种标签：轻量标签 vs 注解标签

| 特性 | 轻量标签 (Lightweight) | 注解标签 (Annotated) |
|------|----------------------|---------------------|
| 本质 | 只是一个 ref | 独立的 tag 对象 |
| 元数据 | 只有 commit SHA | tagger、日期、message、GPG签名 |
| 大小 | 0 字节 | ~200 字节 |
| 推荐 | 临时使用 | **发布版本必须用这个** |

### 创建标签

```bash
# 轻量标签（不推荐用于发布）
git tag v1.0.0

# 注解标签（⭐ 发布用这个）
git tag -a v1.0.0 -m "Release version 1.0.0"

# 给过去的 commit 打标签
git tag -a v0.9.0 -m "Beta release" a1b2c3d

# 查看标签信息
git show v1.0.0
```

### 操作标签

```bash
# 列出所有标签
git tag
git tag -l "v1.*"       # 过滤

# 查看标签详情
git show v1.0.0

# 推送标签到远程
git push origin v1.0.0          # 推送单个
git push origin --tags          # 推送所有标签

# 删除标签
git tag -d v1.0.0               # 删除本地
git push origin --delete v1.0.0 # 删除远程

# 检出标签（进入 detached HEAD 状态）
git checkout v1.0.0

# 基于标签创建分支
git checkout -b hotfix-from-v1.0.0 v1.0.0
```

---

## 2. 语义化版本（Semantic Versioning / SemVer）

### 格式：`MAJOR.MINOR.PATCH`

```text
v2.3.1
 │ │ │
 │ │ └── PATCH: 向后兼容的 bug 修复
 │ └──── MINOR: 向后兼容的新功能
 └────── MAJOR: 不兼容的 API 变更
```

### 版本号规则

| 变更类型 | 版本 | 示例 |
|---------|------|------|
| 修复一个 bug | PATCH +1 | `v1.2.3` → `v1.2.4` |
| 新增一个功能，旧的 API 仍可用 | MINOR +1, PATCH=0 | `v1.2.3` → `v1.3.0` |
| 重大变更，旧 API 不再兼容 | MAJOR +1, MINOR=0, PATCH=0 | `v1.2.3` → `v2.0.0` |

### 先行版本与构建元数据

```text
v1.0.0-alpha.1        # Alpha 版本
v1.0.0-beta.2         # Beta 版本  
v2.0.0-rc.1           # Release Candidate
v1.5.0+build.20250101 # 构建元数据（= 号前面的才是版本比较依据）
```

### 版本优先级

```text
1.0.0 < 2.0.0 < 2.1.0 < 2.1.1
1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0
```

---

## 3. GitHub Releases

GitHub Releases 将 Git tag 与发布说明、二进制文件捆绑在一起。

### 通过 GitHub Web 创建

1. 进入仓库 → Releases → Create a new release
2. 选择 tag（可以现场创建）
3. 填写 Release title 和描述
4. 可附加编译好的二进制文件（.exe, .dmg, .apk 等）
5. 勾选 "Set as pre-release"（alpha/beta/rc）
6. Publish release

### 通过 GitHub CLI 创建

```bash
# 创建 release
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --notes "## What's new
- feat: add dark mode support
- fix: resolve login timeout issue
- perf: improve image loading speed by 40%" \
  --target main

# 附带二进制文件
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  dist/app-v1.0.0.exe \
  dist/app-v1.0.0.dmg

# 标记为 pre-release
gh release create v2.0.0-beta.1 \
  --prerelease \
  --title "v2.0.0 Beta 1"
```

---

## 4. 自动生成 Changelog

### 方案1：基于 Conventional Commits

```bash
# 安装 standard-version
npm install --save-dev standard-version

# package.json
{
  "scripts": {
    "release": "standard-version",
    "release:minor": "standard-version --release-as minor",
    "release:major": "standard-version --release-as major",
    "release:patch": "standard-version --release-as patch"
  }
}

# 发布流程
npm run release
# 自动：
# 1. bump 版本号（package.json）
# 2. 更新 CHANGELOG.md
# 3. 创建 commit: "chore(release): 1.2.0"
# 4. 创建 tag: v1.2.0
```

生成的 CHANGELOG.md 示例：

```markdown
## [1.2.0](https://github.com/user/repo/compare/v1.1.0...v1.2.0) (2025-01-15)

### Features
* **auth:** add OAuth2 social login ([a1b2c3d](https://github.com/user/repo/commit/a1b2c3d))
* **ui:** add dark mode support ([e4f5g6h](https://github.com/user/repo/commit/e4f5g6h))

### Bug Fixes
* **api:** handle null response from payment gateway ([i7j8k9l](https://github.com/user/repo/commit/i7j8k9l))
```

### 方案2：GitHub Actions 自动发布

```yaml
# .github/workflows/release.yml
name: Auto Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Generate Changelog
        id: changelog
        run: |
          PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")
          if [ -z "$PREV_TAG" ]; then
            LOG=$(git log --oneline --no-decorate)
          else
            LOG=$(git log --oneline --no-decorate ${PREV_TAG}..HEAD)
          fi
          echo "CHANGES<<EOF" >> $GITHUB_OUTPUT
          echo "$LOG" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          body: ${{ steps.changelog.outputs.CHANGES }}
          generate_release_notes: true
```

---

## 5. 发布工作流实战

### 标准发布流程

```bash
# 1. 确保在 main 分支且是最新代码
git checkout main
git pull origin main

# 2. 确定版本号
# 看了 CHANGELOG 或 commit 历史后决定 bump 到什么版本

# 3. 更新版本号（手动）
# 编辑 package.json / Cargo.toml / setup.py 等

# 4. 提交版本号变更
git add . && git commit -m "chore: bump version to 1.2.0"

# 5. 创建标签
git tag -a v1.2.0 -m "Release v1.2.0"
```

### Release 分支发布（Git Flow）

```bash
# 1. 创建 release 分支
git checkout -b release/1.2.0 develop

# 2. 在 release 分支上做最终调整
# 更新版本号、更新文档、修复小bug
echo "1.2.0" > VERSION
git add VERSION && git commit -m "chore: bump version to 1.2.0"

# 3. 合并到 main
git checkout main
git merge --no-ff release/1.2.0
git tag -a v1.2.0 -m "Release v1.2.0"

# 4. 合并回 develop（release 分支的修复也要回到开发线）
git checkout develop
git merge --no-ff release/1.2.0

# 5. 删除 release 分支
git branch -d release/1.2.0

# 6. 推送
git push origin main develop --tags
```

---

## 6. 标签速查

| 操作 | 命令 |
|------|------|
| 创建轻量标签 | `git tag v1.0.0` |
| 创建注解标签 | `git tag -a v1.0.0 -m "..."` |
| 给旧 commit 打标签 | `git tag -a v0.9.0 a1b2c3d -m "..."` |
| 列出标签 | `git tag -l "v1.*"` |
| 查看标签详情 | `git show v1.0.0` |
| 推送标签 | `git push origin v1.0.0` |
| 推送所有标签 | `git push origin --tags` |
| 删除本地标签 | `git tag -d v1.0.0` |
| 删除远程标签 | `git push origin --delete v1.0.0` |
| 基于标签创建分支 | `git checkout -b branch-name v1.0.0` |
| 创建 GitHub Release | `gh release create v1.0.0 --title "..." --notes "..."` |

---

> 上一篇：[07-Git钩子与自动化集成](07-Git钩子与自动化集成.md)
> 下一篇：[09-Git子模块与大仓管理](09-Git子模块与大仓管理.md)
