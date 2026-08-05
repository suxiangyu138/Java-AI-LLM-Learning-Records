# Nginx教程

> Nginx 是一款轻量级、高性能的 HTTP 和反向代理 Web 服务器，广泛应用于静态资源服务、反向代理、负载均衡、缓存加速及高可用集群等场景。

### 一、基础篇：初识Nginx

**模块1：Nginx简介**
- `001` Nginx课程内容介绍
- `002` Nginx背景介绍
- `003` 常见服务器的对比
- `004` Nginx的优点
- `005` Nginx的功能特性及常用功能
- `006` Nginx的官方简介

**模块2：环境准备**
- `007` Nginx系统环境准备
- `008` Nginx安装方式介绍及源码安装的准备工作
- `009` 通过Nginx源码简单安装
- `010` 通过yum安装Nginx上
- `011` 通过yum安装Nginx下
- `012` 简单源码安装和yum安装的区别
- `013` 通过Nginx源码复杂安装

**模块3：结构分析**
- `014` Nginx的目录结构分析
- `015` Nginx服务器启停方式介绍
- `016` Nginx服务的信号控制
- `017` Nginx服务的信号控制之USR2
- `018` Nginx服务的命令行控制
- `019` Nginx服务器版本升级需求分析
- `020` Nginx使用服务信号升级
- `021` Nginx使用make命令升级

**模块4：基础配置**
- `022` Nginx配置文件nginx.conf的文件结构
- `023` Nginx全局块的user指令
- `024` Nginx全局块的工作进程的两个指令
- `025` Nginx全局块的其他配置指令
- `026` Nginx的events块指令讲解
- `027` Nginx的events块指令的配置使用
- `028` Nginx的http块MIME-Type的使用
- `029` Nginx的http块自定义服务日志
- `030` Nginx的http块其他配置指令说明
- `031` Nginx的server块和location块的简单说明
- `032` Nginx基础配置实例需求分析
- `033` Nginx基础配置实例配置实现
- `034` Nginx基础配置实例测试
- `035` Nginx配置成系统服务
- `036` Nginx命令配置到系统环境

### 二、基础篇：静态资源服务
- `037` Nginx静态资源概述
- `038` Nginx配置指令之listen
- `039` server_name精确匹配配置
- `040` server_name通配符匹配配置
- `041` server_name正则表达式匹配配置
- `042` Nginx的server_name匹配执行顺序
- `043` Nginx的location指令
- `044` Nginx的root和alias指令
- `045` Nginx的index指令
- `046` Nginx的error_page指令
- `047` Nginx静态资源优化配置之sendfile
- `048` Nginx静态资源优化配置之tcp_nopush和tcp_nodelay
- `049` Nginx静态资源压缩实战内容介绍
- `050` Nginx的Gzip模块配置指令（一）
- `051` Nginx的Gzip模块配置指令（二）
- `052` Nginx的Gzip模块配置指令（三）
- `053` Nginx中Gzip压缩功能的实例配置
- `054` Nginx的Gzip和sendfile的共存问题
- `055` Nginx中添加gzip_static支持
- `056` Nginx中gzip_static使用测试
- `057` Nginx中浏览器缓存的相关概念
- `058` Nginx中浏览器缓存的执行流程
- `059` 浏览器强缓存和弱缓存的效果演示
- `060` Nginx关于浏览器缓存相关的配置指令
- `061` Nginx跨域问题的原因分析
- `062` Nginx跨域问题的案例演示
- `063` Nginx解决跨域问题的具体实现
- `064` Nginx静态资源防盗链的效果展示
- `065` Nginx防盗链的实现原理和实现步骤

### 三、进阶篇：Rewrite模块
- `066` Nginx的rewrite内容介绍
- `067` Nginx的rewrite之set指令
- `068` Nginx的rewrite之if指令（一）
- `069` Nginx的rewrite之if指令（二）
- `070` Nginx的rewrite之break指令
- `071` Nginx的rewrite之return指令
- `072` Nginx的rewrite之rewrite指令
- `073` Nginx的rewrite之rewrite_log指令
- `074` Nginx的rewrite案例之域名跳转
- `075` Nginx的rewrite案例之域名镜像
- `076` Nginx的rewrite案例之独立域名
- `077` Nginx的rewrite案例之目录自动添加斜杠
- `078` Nginx的rewrite案例之目录合并
- `079` Nginx的rewrite案例之防盗链

### 四、进阶篇：代理服务
- `080` Nginx代理概述及环境准备
- `081` Nginx正向代理实现
- `082` Nginx反向代理之proxy_pass指令
- `083` Nginx反向代理之proxy_set_header指令
- `084` Nginx反向代理之proxy_redirect指令
- `085` Nginx反向代理的实战案例
- `086` Nginx的安全控制及SSL加密介绍
- `087` Nginx添加SSL的支持
- `088` Nginx的SSL相关指令
- `089` Nginx如何通过阿里云购买SSL证书简介
- `090` Nginx使用openssl生成证书文件
- `091` Nginx开启SSL支持实例配置
- `092` Nginx反向代理的系统优化

### 五、进阶篇：负载均衡
- `093` 负载均衡概念
- `094` 负载均衡的原理及流程分析
- `095` 负载均衡实现之用户手动选择与DNS轮询
- `096` 负载均衡实现之四层与七层负载
- `097` Nginx七层负载均衡配置
- `098` Nginx负载均衡状态介绍
- `099` Nginx负载均衡状态之down
- `100` Nginx负载均衡状态之backup
- `101` Nginx负载均衡状态值max_fails和fail_timeout
- `102` Nginx负载均衡策略介绍
- `103` Nginx负载均衡策略之轮询与加权轮询
- `104` Nginx负载均衡策略之ip_hash
- `105` Nginx负载均衡策略之least_conn
- `106` Nginx负载均衡策略之url_hash
- `107` Nginx负载均衡策略之fair介绍
- `108` Nginx负载均衡策略之fair的模块添加
- `109` Nginx七层负载均衡的案例
- `110` Nginx四层负载均衡模块添加
- `111` Nginx四层负载均衡案例需求分析
- `112` Nginx四层负载均衡案例实现(一)
- `113` Nginx四层负载均衡案例实现(二)
- `114` Nginx四层负载均衡案例实现(三)
- `115` Nginx四层负载均衡案例实现(四)

### 六、进阶篇：缓存集成
- `116` Nginx缓存集成中缓存的概念
- `117` Nginx缓存的实现流程与原理分析
- `118` Nginx缓存的实现指令之proxy_cache_path
- `119` Nginx缓存的实现指令之其他指令
- `120` Nginx缓存案例的需求分析
- `121` Nginx缓存案例实现(一)
- `122` Nginx缓存案例实现(二)
- `123` Nginx缓存案例实现(三)
- `124` Nginx清除缓存的实现方式
- `125` Nginx设置资源不缓存的指令介绍
- `126` Nginx设置资源不缓存的条件变量介绍
- `127` Nginx设置资源不缓存的案例实现

### 七、架构篇：高可用集群
- `128` Nginx实现服务端集群搭建的需求分析
- `129` Nginx实现服务端集群搭建的Tomcat环境准备
- `130` Nginx实现服务端集群搭建的Nginx环境准备
- `131` Nginx动静分离的概念介绍
- `132` Nginx动静分离的实现
- `133` Nginx部署tomcat集群环境的实现
- `134` Keepalived之VRRP原理分析
- `135` Keepalived之环境准备
- `136` Keepalived之配置文件
- `137` Keepalived之效果测试
- `138` Keepalived之自动切换脚本实现
- `139` Nginx制作下载站点
- `140` Nginx用户认证模块的使用

### 八、模块篇：OpenResty & Lua开发
- `141` Lua的概念介绍
- `142` Lua的安装
- `143` Lua的第一个案例交互式和脚本式使用
- `144` Lua的单行和多行注释
- `145` Lua的其他语法
- `146` Lua的数据类型上
- `147` Lua的数据类型中
- `148` Lua的数据类型下
- `149` Lua的条件判断
- `150` Lua的循环之while循环
- `151` Lua的循环之repeat循环
- `152` Lua的循环之for循环
- `153` ngx_lua的环境准备之OpenResty搭建
- `154` ngx_lua的指令介绍
- `155` ngx_lua操作Redis的实现
- `156` ngx_lua操作Mysql实现查询
- `157` lua_cjson的使用
- `158` ngx_lua操作Mysql实现增删改
- `159` ngx_lua综合案例
