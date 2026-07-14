# npm 依赖管理与 SemVer 语义化版本

> 版本号不是随便写的数字，它是一套精密的"契约系统"。

---

## 一、SemVer 语义化版本规范

### 1.1 版本号格式

```
主版本 . 次版本 . 补丁版本 - 预发布标签 + 构建元数据
 MAJOR   MINOR   PATCH    Pre-release     Build

示例：2.1.3-beta.1+build.20240101
      │ │ │   │            │
      │ │ │   预发布标识    构建元数据（npm 忽略此部分）
      │ │ └─ 补丁：bug 修复，完全向下兼容
      │ └─ 次版本：新功能，向下兼容
      └─ 主版本：破坏性变更，不向下兼容
```

### 1.2 版本变更规则

| 你要做的事情 | 更新哪一位 | 举例 |
|-------------|-----------|------|
| 修复一个 bug | PATCH（补丁）| `1.2.3` → `1.2.4` |
| 新增一个向下兼容的功能 | MINOR（次版本）| `1.2.3` → `1.3.0` |
| 做了不兼容的修改 | MAJOR（主版本）| `1.2.3` → `2.0.0` |

**SemVer 2.0.0 的官方定义**：
> Given a version number MAJOR.MINOR.PATCH, increment the:
> - MAJOR version when you make incompatible API changes
> - MINOR version when you add functionality in a backward compatible manner
> - PATCH version when you make backward compatible bug fixes

### 1.3 特殊版本号规则

| 情况 | 规则 |
|------|------|
| `0.x.y`（开发初期） | 不稳定阶段，MINOR 递增可以包含破坏性变更 |
| `1.0.0` | 第一个稳定版，API 被认定为公有 API |
| 预发布 | `1.0.0-alpha.1` → `1.0.0-beta.1` → `1.0.0-rc.1` → `1.0.0` |

**预发布版本优先级**（从低到高）：
```
1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-rc.1 < 1.0.0
```

---

## 二、版本范围符号

### 2.1 三大基础符号

| 符号 | 含义 | 示例 | 实际允许范围 |
|------|------|------|-------------|
| `^` (Caret) | 锁定主版本，更新次+补丁 | `^2.1.3` | `>=2.1.3 <3.0.0` |
| `~` (Tilde) | 锁定主+次版本，只更新补丁 | `~2.1.3` | `>=2.1.3 <2.2.0` |
| 无符号 | 精确锁定 | `2.1.3` | 只有 `2.1.3` |

### 2.2 特殊规则

```
^0.2.3  →  >=0.2.3 <0.3.0    （0.x 版本 ^ 退化为 ~ 行为）
^0.0.3  →  >=0.0.3 <0.0.4    （0.0.x 版本 ^ 退化为精确匹配）

~1.2    →  >=1.2.0 <1.3.0     （~ 可以省略补丁版本）
^1      →  >=1.0.0 <2.0.0     （^ 可以省略次要版本）
```

### 2.3 其他范围表达式

```json
{
  "dependencies": {
    "vue": ">=3.0.0 <4.0.0",           // 比较表达式
    "react": "18.x",                     // 通配符：18.任何版本
    "lodash": "*",                       // 任意版本（不推荐！）
    "axios": "1.2.3 || >=2.0.0",        // 或条件
    "express": "latest",                 // 始终取最新（会漂移，不推荐）
    "svelte": "file:../local-package",   // 本地文件路径
    "my-lib": "git+https://github.com   // Git 仓库
                /user/repo.git#v1.0.0"
  }
}
```

### 2.4 版本符号选择建议

| 开发阶段 | 推荐符号 | 理由 |
|----------|----------|------|
| 日常开发 | `^` | 享受新功能和 bug 修复，同时避免破坏性更新 |
| CI/CD 环境 | 用 `npm ci`（读 lock 文件） | 保证可复现，不依赖 `^` 范围 |
| 关键项目 | `~` 或 `-E` | 减少意外升级风险 |
| 安全敏感 | `npm audit fix` | 主动修复已知漏洞 |
| npm 包发布者 | `>=`（宽松）| 让使用者减少重复依赖 |

---

## 三、package-lock.json 深入

### 3.1 为什么需要 lock 文件

```
场景：你的 package.json 写了 "vue": "^3.0.0"

你开发时（2024年1月）：npm install → 安装 vue 3.4.0
同事装了（2024年6月）：npm install → 安装 vue 3.5.0
                            ↑ vue 发布了新版本！

package-lock.json 的作用：
将 3.4.0 这个精确版本锁定，同事安装时也得到 3.4.0
```

### 3.2 lock 文件版本 lockfileVersion

| lockfileVersion | npm 版本 | 格式特点 |
|-----------------|----------|----------|
| v1 | npm 5 | 只有 dependencies 树 |
| v2 | npm 7+ | 增加了 `packages` 字段（扁平化映射），完全描述 node_modules |
| v3 | npm 9+ | 进一步精简，性能优化 |

### 3.3 不要手动编辑 lock 文件

```bash
# 正确做法：
npm install axios@1.6.0    # 自动更新 lock 文件

# 错误做法：
# 手动改 lock 文件里的版本号 → 破坏完整性校验
```

### 3.4 lock 文件冲突解决

多人并行修改依赖时，`package-lock.json` 容易产生 Git 冲突：

```bash
# 最佳实践：解决 package.json 冲突后，重新生成
git checkout --theirs package-lock.json   # 或 --ours
npm install                                # 重新生成干净的 lock 文件
```

---

## 四、依赖解析机制

### 4.1 版本选择算法

```
npm install axios@^1.6.0
         │
         ▼
1. 向 Registry 请求 axios 的所有版本列表
2. 过滤出符合 ^1.6.0 的版本：1.6.0, 1.6.1, 1.6.2, ...
3. 检查当前 node_modules 是否已有满足的版本
   ├─ 有 → 复用（不重新下载）
   └─ 没有 → 取最新的满足版本
4. 下载并安装
5. 写入 package-lock.json
```

### 4.2 多版本共存

```
项目依赖：
├── package-a 依赖 axios@^0.21.0
└── package-b 依赖 axios@^1.6.0

node_modules/
├── axios@1.6.4     ← 顶层（package-b 用的版本）
├── package-a/
│   └── node_modules/
│       └── axios@0.21.4  ← 嵌套（package-a 需要的版本）
│                           ↑ npm 3 起尽量不嵌套，但版本冲突时有嵌套
```

### 4.3 overrides / resolutions（强制覆写）

```json
// npm 8.3+ 支持 overrides
{
  "overrides": {
    "foo": "1.0.0",              // 覆写 foo 包为 1.0.0
    "bar": {                     // 覆写 bar 包的依赖
      "baz": "2.0.0"
    }
  }
}

// yarn 等价物
{
  "resolutions": {
    "foo": "1.0.0"
  }
}
```

**使用场景**：某个深层依赖有安全漏洞，但上游包还没更新，可以用 overrides 临时强制替换。

---

## 五、包分发标签（dist-tag）

### 5.1 默认标签

```bash
npm publish              # 默认打上 latest 标签
npm publish --tag beta   # 打上 beta 标签
npm publish --tag next   # 打上 next 标签
```

```bash
npm install vue          # 相当于 vue@latest
npm install vue@beta     # 安装 beta 标签指向的版本
npm install vue@next     # 安装 next 标签指向的版本
```

### 5.2 标签管理

```bash
npm dist-tag ls vue            # 查看 vue 的所有标签
npm dist-tag add vue@3.4.0 latest  # 将标签指向指定版本
npm dist-tag rm vue@beta       # 删除标签
```

### 5.3 常见标签策略

```
react 的标签策略：
  latest → 18.2.0    （稳定版，大多数人用的）
  next   → 19.0.0-beta  （下一个大版本的测试版）
  canary → 19.0.0-alpha （每日构建，最新但可能不稳定）

Vue 的标签策略：
  latest → 3.4.0
  next   → （无，最新稳定版就是 latest）
  v2-latest → 2.7.0  （Vue 2 最后的维护版本）
```

---

## 六、别名安装（alias）

```bash
# 给包起别名（同时安装两个版本/解决命名冲突）
npm install new-lodash@npm:lodash@latest
npm install legacy-react@npm:react@17
```

```json
{
  "dependencies": {
    "new-lodash": "npm:lodash@^4.17.21",
    "legacy-react": "npm:react@^17.0.2"
  }
}
```

**使用场景**：
- 同时使用两个大版本的同一包
- 包名与其他冲突时重命名

---

## 七、实战版本管理策略

### 7.1 日常项目

```bash
# 有 lock 文件的项目
npm ci                       # 首次安装
npm outdated                 # 定期查看更新
npm update                   # 安全地更新（遵守 ^ 范围）
npm audit fix                # 安全修复
```

### 7.2 发布 npm 包

```json
// 包作者应尽量宽松声明，让使用者减少重复依赖
{
  "peerDependencies": {
    "react": ">=16.8.0"     // 而不是 "^18.0.0"
  }
}
```

### 7.3 版本发布命令

```bash
npm version patch           # 1.2.3 → 1.2.4  (Git tag + commit)
npm version minor           # 1.2.3 → 1.3.0
npm version major           # 1.2.3 → 2.0.0

npm version prepatch        # 1.2.3 → 1.2.4-0
npm version preminor        # 1.2.3 → 1.3.0-0
npm version premajor        # 1.2.3 → 2.0.0-0
npm version prerelease      # 1.2.4-0 → 1.2.4-1
```

---

> **核心记忆**：`^` 锁主版本（最常用），`~` 锁主+次版本（更保守），无符号精确锁定。lock 文件是"环境的锚"，保证所有人安装的依赖完全一致。SemVer 是包作者和使用者之间的信任契约。
