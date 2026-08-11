# 09 Demo 常见问题与调试速查

> Level1 的故障手册：把 8 个 Demo 里最高频的报错按「环境 / 网络 / 依赖 / 编码 / 模型」五类归档，每个问题给出现象、排查步骤与解决。Debug 是工程师的第一技能，本手册就是这份技能的最小训练集。

## 📚 目录

1. [调试方法论：先环境后代码](#1-调试方法论先环境后代码)
2. [环境类问题](#2-环境类问题)
3. [网络类问题](#3-网络类问题)
4. [依赖类问题](#4-依赖类问题)
5. [编码与乱码类问题](#5-编码与乱码类问题)
6. [模型与 AI 链路问题](#6-模型与-ai-链路问题)
7. [通用排障三板斧](#7-通用排障三板斧)
8. [核心要点](#8-核心要点)

---

## 1. 调试方法论：先环境后代码

Demo 期的报错 60% 来自环境、30% 来自配置、10% 才是代码逻辑——所以排查顺序固定为**先环境、再配置、后代码**。环境问题（端口、版本、服务没起）用几条命令 30 秒定位；配置问题（依赖缺失、参数写错）看报错信息多半能对上；代码问题才需要断点与日志。判断"这是哪类问题"的快捷方式：**报错在启动时出现**多半是环境/配置（启动都过不去，代码根本没机会跑）；**报错在请求时出现**才是代码逻辑。另一个固定动作：**看报错要读完整**——多数人只看第一行，真正的根因（Caused by 部分）在最后几行。

| 问题类别 | 典型报错 | 定位手段 |
|---|---|---|
| 环境 | 端口占用、服务连不上、命令找不到 | 命令行验证：netstat / ping / java -version |
| 网络 | 超时、连接被拒、SSL 错误 | curl 验证，换网络分段 |
| 依赖 | ClassNotFound、版本冲突、NoSuchBean | 看依赖树、查 pom、清缓存 |
| 编码 | 乱码、JSON 解析失败 | 检查 UTF-8、Content-Type、序列化器 |
| 模型 | 401/404/429、流式无输出 | curl 验证 Key 与模型名，看响应体 |

## 2. 环境类问题

**端口被占用**：启动报 `Port 8080 was already in use`。排查：`netstat -ano | findstr 8080` 看占用进程的 PID，任务管理器结束该进程，或改 `server.port`。**数据库连不上**：报 `Communications link failure` 或 `Connection refused`。排查顺序：MySQL 服务启动了吗（服务管理器/`mysqladmin ping`）→ 端口对吗（3306）→ 密码对吗（报 Access denied 就是密码问题）→ 服务在 Docker 里吗（容器起来了没、端口映射对不对）。**redis-cli 连不上**：Redis 服务没起或端口不对，`redis-cli ping` 验证。**命令找不到**（`java` 不是内部命令）：PATH 没配，回 01 篇重配环境变量。

环境类问题的共同规律：**先确认"服务存在且可达"，再查代码**。一个命令能验证的事不要猜——`ping` 验证网络、`netstat` 验证端口、`redis-cli ping` 验证 Redis，每个工具都有它的"探针命令"，先把探针跑一遍。

## 3. 网络类问题

**Maven 依赖下载失败**：报 SSL 错误或下载超时。先换阿里云镜像（01 篇），再不行换网络（校园网/手机热点），最后清缓存 `mvn dependency:purge-local-repository`（慎用，全量重下）。**大模型 API 超时**：curl 验证接口可达性（`curl https://api.deepseek.com` 返回 404 或 400 都说明网络通，通到平台了）；公司网络或代理环境经常拦外网 API——换个人网络测试是最高效的排查。**Docker 拉镜像失败**：国内网络拉 Docker Hub 镜像不稳定，配镜像加速器（阿里云容器镜像服务里有免费加速地址），或换源。

网络问题的核心思路：**分段定位**——先验证"本机到目标服务器"的通路（curl/ping），通了再查"应用层"（Key、参数）；不通就是网络本身（代理、防火墙、DNS）。Windows 上注意 **PowerShell 的 curl 是 Invoke-WebRequest 的别名**，参数写法完全不同——要用真 curl 写 `curl.exe`，或用 IDEA 的 HTTP Client（新建 .http 文件，比命令行友好得多，强烈推荐）。

## 4. 依赖类问题

**ClassNotFoundException / NoClassDefFoundError**：某个类找不到——依赖没加、或版本冲突导致类被排除。查 pom 是否加了对应 starter，IDEA 右侧 Maven → Dependencies 看依赖树里有没有。**NoSuchBeanDefinitionException**（`Consider defining a bean of type`）：组件没被扫描到——类没加 @Service/@Repository/@Component 注解，或类不在主类包及其子包内（主类在 com.example.demo，你的类必须在 com.example.demo 下面）。**版本冲突**：`Could not resolve dependencies` 或运行期方法签名错误。先看完整报错里的版本信息，用 Maven Helper 插件（IDEA 插件市场可装）看冲突——Demo 阶段最省事的解法是把 pom 里手动声明的版本都删掉，让 Spring Boot 的依赖管理统一决定版本（Boot 的 dependencyManagement 会锁定兼容版本）。

**MyBatis-Plus 与 Boot 4 的兼容**：3.5.x 的 boot3 分支对 Boot 4 的支持要看 release 说明，集成报错时换 MyBatis 官方 `mybatis-spring-boot-starter` 3.0.x——**框架选型容错也是能力**，Demo 的目标是链路跑通，不在一棵树上吊死。

## 5. 编码与乱码类问题

**控制台中文乱码**：IDEA 终端或 Run 窗口显示中文变问号。Settings → Editor → File Encodings 三处（Global/Project/Default）设 UTF-8；Run 配置的 VM options 加 `-Dfile.encoding=UTF-8`。**数据库中文乱码**：建库没指定 utf8mb4、或连接 url 缺 characterEncoding=utf8。建库语句与连接串两处一起检查。**请求/响应乱码**：接口返回中文正常但前端显示乱码——检查响应头 Content-Type 是否含 charset=utf-8（Spring Boot 默认有，除非手动改过）；HTTP 请求体乱码——确认客户端发送的 Content-Type 与编码。

**JSON 解析失败**（`Cannot deserialize` / `Unrecognized field`）：请求 JSON 字段与实体不匹配——字段名大小写、拼写、类型（字符串传成了数字）；实体缺无参构造器或 setter（record 类用作请求体时，Jackson 2.15+ 支持 record 反序列化，但注意字段名一致）。报错信息会指出哪个字段出问题，**先读报错再改代码**。

## 6. 模型与 AI 链路问题

**401 Unauthorized**：API Key 错误/过期/格式不对。先用 curl 直接验证 Key（06 篇的命令），Key 问题先排除，再查代码里 Key 的读取方式（环境变量名拼写、配置文件没生效）。**404 或模型不存在**：模型名写错——2026-08 用 `deepseek-v4-flash`/`deepseek-v4-pro`，旧名 `deepseek-chat` 已弃用（2026-07-24 起）。模型名问题在配置中心查，代码里的模型名是配置不是硬编码。**429 限流**：请求太频繁或余额不足。看响应体里的错误信息（限流 vs 欠费），限流加延时重试，欠费去充值。**流式无输出**：SSE 格式问题——接口 produces 没设 TEXT_EVENT_STREAM_VALUE，或前端 EventSource 没收到数据。先用浏览器直接访问流式接口地址验证后端，再查前端。**回答与知识库无关**：检索环节的问题（07/08 篇的排查顺序），不是模型问题——先确认检索 Top 结果相关，再怀疑生成环节。

AI 链路排错的固定顺序：**curl 验证 Key → 检查模型名 → 看响应体错误信息 → 检查注入提示词 → 检查检索结果**。每一步都有明确的验证手段，不要跳过直接怀疑"模型不行"——绝大多数情况模型是好的。

## 7. 通用排障三板斧

三板斧适用于任何问题：**第一斧：完整读报错**——把控制台报错从头看到尾，重点看 Caused by 与 StackTrace 最深处，多数根因藏在最后；**第二斧：最小复现**——把问题缩小到最小范围：单个接口、单条数据、单行代码，用 System.out.println 或断点逐段确认（Demo 阶段 println 最快，不要上来就搞日志框架）；**第三斧：对比法**——"它之前是好的"就对比改动前后的差异（git diff 或记忆）；"教程里是好的"就逐行对比教程代码；"别人是好的"就对比环境差异。三板斧用完还定位不到，**把报错原文与上下文整理清楚再问人**——能完整描述问题的提问，本身就是调试能力的体现，也最容易得到有效答案。

## 8. 核心要点

1. 排查顺序固定：先环境（命令验证）→ 再配置（读报错）→ 后代码（断点/println）。
2. 报错读完整：根因在 Caused by，不在第一行。
3. 网络问题分段定位：curl 验证通路，再查应用层（Key/参数）。
4. 模型链路排错固定序：curl 验 Key → 模型名 → 响应体 → 提示词 → 检索。
5. 三板斧：完整读报错、最小复现、对比法——用完再问人，提问要带完整上下文。

> 🎯 **核心要点**：本手册的目的不是背答案，而是**训练"报错 → 分类 → 定位"的反射**。每类问题都有固定探针（端口用 netstat、Key 用 curl、依赖看树），探针跑一遍问题域就缩小一半。这个反射是 Level2 独立开发的第一生产力。

---

**下一模块**：[10 验收与进入 Level2](./10-验收与进入%20Level2.md) | **返回总览**：[Level1 总览](./00-Level1%20基础%20Demo%20总览.md)
