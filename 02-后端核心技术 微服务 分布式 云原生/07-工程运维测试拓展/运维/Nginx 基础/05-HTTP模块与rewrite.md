# 05-HTTP 模块与 rewrite
> URL 重写、if 判断、return/redirect、try_files、跨域配置——"地址改写与请求路由的瑞士军刀"

## 📚 目录
1. [rewrite 语法与标志](#1-rewrite-语法与标志)
2. [return 与重定向](#2-return-与重定向)
3. [if 判断（谨慎使用）](#3-if-判断谨慎使用)
4. [变量系统](#4-变量系统)
5. [跨域（CORS）配置](#5-跨域cors配置)
6. [常见重写场景](#6-常见重写场景)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. rewrite 语法与标志

```nginx
# 语法：rewrite 正则 替换 [标志]
rewrite ^/old/(.*)$ /new/$1 permanent;      # 301 永久跳转
rewrite ^/user/(\d+)$ /user?id=$1 break;    # 内部重写
```

| 标志 | 行为 | 场景 |
|------|------|------|
| `last` | 重写后**重新匹配 location** | 内部路由变更 |
| `break` | 重写后**停止**（不重新匹配） | 仅改 URI |
| `redirect` | 302 临时重定向 | 临时地址变更 |
| `permanent` | 301 永久重定向 | 永久地址变更（SEO） |

```nginx
# last vs break 的区别（面试高频）：
# last：rewrite 后重新进入 location 匹配（可能匹配到新 location）
# break：rewrite 后停止匹配，直接用新 URI 处理

location /old/ {
    rewrite ^/old/(.*)$ /new/$1 last;      # 重新匹配 /new/ 的 location
}
location /old/ {
    rewrite ^/old/(.*)$ /new/$1 break;     # 不再匹配，直接用 /new/$1
}
```

> 🎯 **last vs break**：last 会"重新匹配 location"（新 URI 走新 location 的处理）；break 只改 URI 不重匹配——**路由级变更用 last，仅路径改写用 break**。

## 2. return 与重定向

```nginx
# return：直接返回（比重写更高效）
return 301 https://$host$request_uri;        # HTTP → HTTPS 跳转
return 404;                                  # 直接 404
return 200 "OK";                             # 直接返回内容（健康检查用）

# 临时/永久重定向
return 302 /new/path;
return 301 /new/path;
```

| 场景 | 配置 |
|------|------|
| HTTP → HTTPS | `return 301 https://$host$request_uri;` |
| 域名迁移 | `return 301 http://new-domain.com$request_uri;` |
| 接口下线 | `return 410;` |
| 健康检查 | `return 200 "OK";` |

> 💡 **return vs rewrite 的选择**：能 return 就 return（更简单高效）——rewrite 用于需要正则改写的场景，纯跳转用 return。

## 3. if 判断（谨慎使用）

```nginx
# if 语法（仅支持少数指令，勿滥用）
if ($request_method = POST) {
    return 405;
}
if ($http_user_agent ~* "bot") {
    return 403;
}
if ($request_uri ~* "\.(php)$") {
    return 403;
}
```

| if 中可用指令 | 说明 |
|---------------|------|
| `return` | 返回状态码（最常用） |
| `rewrite` | 重写 |
| `set` | 设置变量 |
| 其他 | **大部分指令在 if 中无效**（官方警告） |

> ⚠️ **if 是"邪恶的 if"**（Nginx 官方文档原话）：if 内多数指令行为不可预期——**只用于 return/rewrite/set 三件事**，复杂逻辑用 location 拆分或 Lua（OpenResty）。

## 4. 变量系统

| 变量 | 含义 |
|------|------|
| `$host` | 请求域名 |
| `$request_uri` | 完整请求 URI（含参数） |
| `$uri` | 规范化 URI（不含参数） |
| `$remote_addr` | 客户端 IP |
| `$http_user_agent` | User-Agent |
| `$scheme` | 协议（http/https） |
| `$request_method` | 请求方法 |
| `$upstream_status` | 上游响应状态（排障用） |
| `$request_time` | 请求处理耗时 |

```nginx
# 变量使用示例
location / {
    if ($request_method = OPTIONS) {
        return 204;                     # 预检请求处理
    }
    add_header X-Request-ID $request_id;  # 自定义头
}
```

## 5. 跨域（CORS）配置

```nginx
# 前后端分离场景：后端 API 跨域
location /api/ {
    # 允许的源（按需收紧，勿用 * 带凭证）
    add_header Access-Control-Allow-Origin $http_origin always;
    add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
    add_header Access-Control-Allow-Headers "Content-Type, Authorization, X-Requested-With";
    add_header Access-Control-Allow-Credentials true always;
    add_header Access-Control-Max-Age 3600;

    # 预检请求直接返回（不转发后端）
    if ($request_method = OPTIONS) {
        return 204;
    }

    proxy_pass http://backend;
}
```

| CORS 配置要点 | 说明 |
|---------------|------|
| `Allow-Origin` | 指定源（生产用具体域名，勿 `*` + 凭证） |
| 预检 OPTIONS | Nginx 直接 204（不落后端） |
| `always` 参数 | 错误响应也带 CORS 头（前端能读到错误信息） |
| 凭证 | `Allow-Credentials true` 时 Origin 不能是 `*` |

> 🎯 **Nginx 解决跨域的定位**：网关/接入层统一加 CORS 头——后端不用每接口处理跨域；**预检请求在 Nginx 层消化**（不浪费后端资源）。

## 6. 常见重写场景

```nginx
# 场景 1：去掉 index.php（伪静态）
location / {
    if (!-e $request_filename) {
        rewrite ^(.*)$ /index.php?s=$1 last;
    }
}

# 场景 2：URL 规范化（去掉尾部斜杠）
rewrite ^/(.*)/$ /$1 permanent;

# 场景 3：移动端跳转
if ($http_user_agent ~* "(Android|iPhone)") {
    rewrite ^/(.*)$ /m/$1 last;
}

# 场景 4：旧地址 301 到新地址（SEO）
location /old-page {
    return 301 /new-page;
}
```

| 场景 | 手段 |
|------|------|
| 伪静态 | `if (!-e $request_filename)` + rewrite |
| 地址变更 | return 301（SEO 友好） |
| 设备分流 | if User-Agent + rewrite |
| 参数改写 | rewrite 正则捕获 |

## 7. 核心要点

> 🎯 **核心要点**：
> - rewrite 四标志：last（重匹配）/break（停止）/redirect（302）/permanent（301）；
> - **last vs break 是面试高频**：重匹配 location vs 直接使用；
> - 能 return 就 return（跳转用 return 301，比 rewrite 高效）；
> - if 是"邪恶的 if"：只用 return/rewrite/set，复杂逻辑拆 location 或上 Lua；
> - CORS 在 Nginx 统一处理：具体 Origin + 预检 204 + `always` 错误也带头；
> - 变量系统是配置的灵魂（$host/$request_uri/$remote_addr...）。

## 8. 参考来源

- [Nginx rewrite 模块文档](https://nginx.org/en/docs/http/ngx_http_rewrite_module.html)
- [Nginx if 指令说明（官方警告）](https://nginx.org/en/docs/http/ngx_http_rewrite_module.html#if)
- [Nginx 变量与常用变量列表](https://nginx.org/en/docs/varindex.html)

---

**下一模块**：[06-HTTPS与证书](06-HTTPS与证书.md)　/　**返回总览**：[00-总览](00-Nginx知识体系总览.md)
