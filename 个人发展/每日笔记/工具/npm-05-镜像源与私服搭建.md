# npm 镜像源与私服搭建

> 国内开发绕不开的必修课——换源、多源管理、企业私服。

---

## 一、为什么要换源

```
npm 官方源 registry.npmjs.org → 服务器在境外

国内直连体验：
├── 下载速度：几十 KB/s ~ 几百 KB/s
├── 大包下载：依赖多时可能卡 10 分钟+
├── 偶发性超时：npm ERR! network timeout
└── CI/CD 构建：慢 = 浪费机器时间和钱

切换到国内镜像 → 几 MB/s ~ 几十 MB/s
```

---

## 二、主流镜像源

### 2.1 国内可用镜像

| 镜像源 | URL | 说明 |
|--------|-----|------|
| **npmmirror（淘宝）** | `https://registry.npmmirror.com` | 原淘宝镜像，**最常用**，每 10 分钟同步 |
| 阿里云镜像 | `https://mirrors.aliyun.com/npm/` | 阿里云提供 |
| 腾讯云镜像 | `https://mirrors.cloud.tencent.com/npm/` | 腾讯云提供 |
| 华为云镜像 | `https://mirrors.huaweicloud.com/repository/npm/` | 华为云提供 |
| 清华大学 TUNA | `https://mirrors.tuna.tsinghua.edu.cn/npm/` | 高校镜像 |
| 中科大 USTC | `https://npmreg.proxy.ustclug.org/` | 高校镜像 |

### 2.2 淘宝镜像历史

```
时间线：

2014-2023：npm.taobao.org / registry.npm.taobao.org
    ↓
2023 年起：npmmirror.com（淘宝 npm 镜像新域名）
    ↓
新老域名关系：registry.npmmirror.com（新）替代旧域名
    旧域名已停止更新，必须切换到新域名！
```

---

## 三、换源方式

### 3.1 全局换源（最常用）

```bash
# 查看当前源
npm config get registry
# → https://registry.npmjs.org/

# 切换到淘宝镜像
npm config set registry https://registry.npmmirror.com

# 切回官方源
npm config set registry https://registry.npmjs.org
```

### 3.2 项目级换源（.npmrc）

项目根目录创建 `.npmrc`：

```
registry=https://registry.npmmirror.com
```

优点：不影响全局，项目独立配置，可提交到 Git。

### 3.3 单次临时换源

```bash
# 单次安装使用指定源
npm install axios --registry=https://registry.npmmirror.com
```

### 3.4 确认源是否生效

```bash
npm config list           # 查看所有配置
npm config get registry   # 只看 registry
npm info vue              # 查看 vue 包信息（会显示从哪个源拉取）
```

---

## 四、多源管理工具

### 4.1 nrm（npm registry manager）

```bash
# 安装
npm install -g nrm

# 列出所有可用源
nrm ls
#   npm ---------- https://registry.npmjs.org/
#   yarn --------- https://registry.yarnpkg.com/
# * tencent ------ https://mirrors.cloud.tencent.com/npm/
#   npmMirror ---- https://registry.npmmirror.com/

# 切换
nrm use taobao       # 某些版本还用 taobao 作为名称
nrm use npmMirror

# 测试各源速度
nrm test

# 添加自定义源
nrm add my-registry http://192.168.1.100:4873/

# 删除源
nrm del my-registry
```

### 4.2 yrm（yarn registry manager）

```bash
npm install -g yrm
yrm ls
yrm use taobao
```

---

## 五、.npmrc 配置文件详解

### 5.1 配置层级与优先级

```
项目级 .npmrc（项目根目录）          ← 优先级最高
    ↓ 覆盖
用户级 .npmrc（~/.npmrc）            ← 用户全局配置
    ↓ 覆盖
全局 .npmrc（$PREFIX/etc/npmrc）     ← npm 安装位置下
    ↓ 覆盖
内置配置（npm 内置的默认值）           ← 优先级最低
```

### 5.2 常用 .npmrc 配置

```
# 镜像源
registry=https://registry.npmmirror.com

# 认证相关
//registry.npmjs.org/:_authToken=xxx        # npm 官方 token
//registry.npmmirror.com/:_authToken=xxx    # 私有源 token
always-auth=true                             # 始终带认证

# 作用域指定源（企业常用）
@mycompany:registry=https://npm.mycompany.com

# 严格版本
engine-strict=true                           # 不满足 engines 直接报错
save-exact=true                              # 安装时默认 -E（精确版本）
package-lock=true                            # 生成 lock 文件（默认 true）

# 脚本相关
ignore-scripts=true                          # 禁掉 install 自动执行脚本（安全）

# Node 版本
use-node-version=20.10.0

# 其他
prefer-offline=true                          # 优先用缓存（离线模式）
fund=false                                   # 关闭 npm install 后的赞助广告
audit=false                                  # 关闭安全审计提示
progress=false                               # 关闭进度条（CI 中提速）
```

### 5.3 企业 .npmrc 典型配置

```
# 公司私有源
registry=https://npm.mycompany.com

# 某些公开包走公网镜像（更快）
@types:registry=https://registry.npmmirror.com

# 认证（CI 中通过环境变量注入）
//npm.mycompany.com/:_authToken=${NPM_TOKEN}

# 作用域关联
@mycompany:registry=https://npm.mycompany.com
```

---

## 六、私有 npm Registry 搭建

### 6.1 为什么企业需要私服

```
1. 内网包：公司内部包不对外发布
2. 安全合规：代码不出内网
3. 加速安装：局域网下载极快
4. 缓存代理：缓存外网包，减少外部请求
5. 权限控制：细粒度控制谁能访问什么包
```

### 6.2 Verdaccio（最流行的开源方案）

```bash
# 安装
npm install -g verdaccio

# 启动（默认跑在 http://localhost:4873）
verdaccio

# 配置文件位置
~/.config/verdaccio/config.yaml
```

**核心配置 config.yaml**：
```yaml
storage: ./storage
plugins: ./plugins

# 监听
listen:
  - 0.0.0.0:4873

# 上游代理（本地没有的包向上游请求并缓存）
uplinks:
  npmjs:
    url: https://registry.npmjs.org/
  taobao:
    url: https://registry.npmmirror.com/

# 包访问规则
packages:
  '@mycompany/*':
    access: $authenticated     # 需要登录
    publish: $authenticated
    
  '**':
    access: $all              # 所有人都能拉
    publish: $authenticated   # 登录才能发
    proxy: taobao npmjs       # 优先淘宝，回退 npmjs

# 监听
server:
  keepAliveTimeout: 60

# 日志
logs: { type: stdout, format: pretty, level: http }
```

**使用**：
```bash
# 添加用户
npm adduser --registry http://localhost:4873

# 发布到私服
npm publish --registry http://localhost:4873

# 客户端设为该源
npm config set registry http://localhost:4873
```

### 6.3 其他方案

| 方案 | 说明 |
|------|------|
| **Verdaccio** | 轻量级，零配置，**中小企业首选** |
| **Nexus Repository** | Java 生态常用，支持多种包类型（Maven + npm + Docker）|
| **JFrog Artifactory** | 企业级，功能最强，价格高 |
| **GitHub Packages** | GitHub 原生支持，与代码库集成 |
| **GitLab Package Registry** | 自部署 GitLab 自带 |
| **Cloudflare npm Registry** | Cloudflare 提供的镜像代理 |
| **cnpmjs.org** | 淘宝开源的企业级 npm 方案，自身即用此搭建 |

### 6.4 Docker 部署 Verdaccio

```dockerfile
# docker-compose.yml
version: '3'
services:
  verdaccio:
    image: verdaccio/verdaccio
    ports:
      - "4873:4873"
    volumes:
      - ./storage:/verdaccio/storage
      - ./config:/verdaccio/conf
    restart: always
```

---

## 七、常见源问题排查

### 7.1 确认当前生效源

```bash
npm config get registry        # 明确配置
npm info vue --json | grep registry   # 看实际从哪取的
```

### 7.2 淘宝源 404 / 证书错误

```bash
# 旧域名 registry.npm.taobao.org 已停止服务，更换
npm config set registry https://registry.npmmirror.com

# HTTPS 证书问题（少见于内网）
npm config set strict-ssl false   # 不推荐，仅临时排查用
```

### 7.3 某些包同步延迟

```bash
# 手动触发淘宝源同步
curl -X PUT https://registry.npmmirror.com/<package-name>/sync

# 比如
curl -X PUT https://registry.npmmirror.com/vue/sync
```

### 7.4 混合源（公开+私有）

```
# .npmrc : 同一个项目同时从公开源和私有源拉取
registry=https://registry.npmmirror.com
@mycompany:registry=https://npm.mycompany.com
```

---

> **核心要点**：国内开发第一时间换淘宝镜像（npmmirror.com），用 nrm 管理多源切换。企业场景用 Verdaccio 搭私服 + .npmrc 的作用域配置实现公开/私有混合拉取。
