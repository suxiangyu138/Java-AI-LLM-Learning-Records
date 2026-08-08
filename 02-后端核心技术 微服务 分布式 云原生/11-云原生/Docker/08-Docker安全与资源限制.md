# 08-Docker 安全与资源限制
> Docker 隔离是"双刃剑"——核心认知是"隔离不是绝对安全"；从资源配置、网络、文件系统、权限四维度规范使用，遵循"最小权限原则、资源匹配原则、隔离隔离原则"

## 📚 目录
1. [Docker 隔离的本质](#1-docker-隔离的本质)
2. [四大核心危险（Java 后端高频场景）](#2-四大核心危险java-后端高频场景)
3. [规避方案：资源匹配](#3-规避方案资源匹配)
4. [规避方案：网络与文件系统](#4-规避方案网络与文件系统)
5. [规避方案：最小权限](#5-规避方案最小权限)
6. [排查技巧](#6-排查技巧)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. Docker 隔离的本质

Docker 隔离基于 Linux 内核三大核心技术，属"**基于 Linux 内核的有限隔离**"（共享内核、所有资源最终依赖宿主机），而非虚拟机级别的完全隔离：

| 技术 | 作用 | Java 后端适配要点 |
|------|------|-------------------|
| **Namespace** | 隔离"资源可见性"（PID/网络/挂载点/用户/IPC） | 避免 PID/端口冲突；但**只隔离可见性，不隔离资源占用** |
| **Cgroups** | "限制资源的使用上限"（CPU/内存/磁盘 IO） | 控制 JVM 内存的关键；但限制是"软限制"，可能被绕过 |
| **UnionFS** | "隔离文件系统" | 避免配置/日志互相干扰；**一旦挂载宿主机目录（数据卷）则文件隔离被打破** |

> 🎯 **关键结论**：隔离核心是"限制资源可见性和使用上限"，而非"完全隔离资源"——既是优势（简化部署、资源复用）也暗藏隐患，隔离边界被突破会威胁 Java 应用与宿主机安全。

## 2. 四大核心危险（Java 后端高频场景）

### 危险 1：资源隔离失效——资源抢占导致宿主机及其他容器崩溃

- **成因**：① JVM 配置与 Docker 资源限制不匹配（**Java 8 及以下 JVM 无法识别 Docker Cgroups 限制，默认使用宿主机内存**）；② Cgroups 限制绕过（软限制，死循环持续占用）；③ 资源配置不合理（核心与非核心服务分配相同资源）。
- **表现**：宿主机 CPU 飙升至 100%；容器内 Java OOM；其他容器响应超时被 kill；极端情况宿主机崩溃、业务中断。

### 危险 2：网络隔离失效——容器逃逸与网络攻击

- **成因**：① 开发环境用 host 网络模式（隔离完全失效）；② 自定义网络未限制容器网络权限（容器间随意通信）；③ 端口映射过度开放（映射到公网 IP 且不限制访问 IP）。
- **表现**：容器内应用可访问宿主机敏感端口（22、3306）；外部攻击者经公网 IP+映射端口窃取数据；极端情况容器逃逸获取宿主机 root 权限。

### 危险 3：文件系统隔离失效——数据泄露与恶意篡改

- **成因**：① 数据卷挂载权限过高（默认读写，被控容器可读写宿主机敏感文件）；② 挂载宿主机敏感目录（`/`、`/root` 挂进容器）；③ 容器间共享数据卷（一个被攻，经共享卷篡改其他容器）。
- **表现**：宿主机敏感文件被篡改删除；Java 配置被恶意修改；业务数据被窃取。

### 危险 4：权限隔离失效——容器权限过高被恶意利用

- **成因**：① 滥用 `--privileged=true`（赋予宿主机 root 权限，可实现容器逃逸）；② 随意添加 Linux 能力 `--cap-add=ALL`；③ 容器内 Java 应用以 root 用户运行。
- **表现**：攻击者获取宿主机 root 权限；Java 容器被利用为挖矿程序、病毒载体。

## 3. 规避方案：资源匹配

**JVM 与 Cgroups 资源匹配（Java 后端核心）**：

```bash
# 容器限制内存 2G，JVM 最大内存不超过容器内存的 80%
docker run -m 2g --cpus 1 my-java-app
# JVM 配置：-Xms512m -Xmx1.6g
```

| 规则 | 说明 |
|------|------|
| `-m` 限制容器内存 | 与 JVM 的 `-Xms`/`-Xmx` 严格匹配 |
| Xmx ≤ 容器内存 80% | 容器限制 2G → JVM 配 `-Xmx1.6g` |
| Java 8 及以下 | 添加 `-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap` 让 JVM 识别 Docker 资源限制 |
| 合理分配 | 核心服务 `--cpus 2`、非核心 `--cpus 0.5` |
| 监控 | `docker stats`、Prometheus+Grafana，配置内存/CPU 告警 |

## 4. 规避方案：网络与文件系统

### 4.1 网络隔离

| 措施 | 说明 |
|------|------|
| 开发慎用 host、生产**绝对禁止** | 用 bridge/自定义网络 |
| 不同项目不同环境用不同自定义网络 | 网络级隔离 |
| 生产端口映射仅绑内网 IP | 公网访问走 Nginx 反向代理并限制访问 IP |
| 应用层防护 | Java 应用开 HTTPS、防 SQL 注入/远程代码执行、加接口权限控制 |

### 4.2 文件系统隔离

| 措施 | 说明 |
|------|------|
| 挂载按需设权限 | 只读 `:ro`、读写 `:rw` |
| **绝对禁止挂载敏感目录** | `/`、`/root`、`/etc` 不能挂进容器 |
| Java 配置/日志挂载专用目录 | 如 `/host/java/app/config`，仅授权必要用户 |
| 共享数据优先用 Docker 管理卷 | 限制共享卷权限并定期检查 |
| 核心数据定期备份 | 敏感数据用环境变量注入 + 加密存储 |

## 5. 规避方案：最小权限

**1. 禁止 `--privileged=true`**：必须用时严格控制访问权限且仅必要容器。

**2. 按需添加 Linux 能力，避免 `--cap-add=ALL`**：

```bash
# 示例：Java 应用需绑定小于 1024 的端口
docker run -d -p 80:8080 --cap-add=NET_BIND_SERVICE springboot-app:1.0.0
```

**3. 容器内非 root 用户运行 Java 应用**：

```dockerfile
FROM openjdk:11-jre-slim
# 创建非 root 用户
RUN addgroup --system java-group && adduser --system --group java-user
# 切换到非 root 用户
USER java-user
WORKDIR /app
COPY target/app.jar /app/app.jar
ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-jar", "/app/app.jar"]
```

**4. 限制系统调用**：

```bash
docker run --security-opt seccomp=xxx.json my-java-app
# 禁止容器执行敏感系统调用（修改内核配置、挂载磁盘）
```

## 6. 排查技巧

| 排查方向 | 命令 |
|----------|------|
| 资源隔离 | `docker stats`（CPU/内存对比 Cgroups 限制）；`jstat`、`jmap`（JVM 内存，避免 OOM） |
| 网络隔离 | `docker network inspect 网络名`；`docker exec -it 容器名 ping 目标IP`、`telnet 目标IP 端口` |
| 文件系统隔离 | `docker inspect 容器名`（查看挂载情况、权限是否合理）；查看宿主机挂载目录权限 |
| 权限隔离 | `docker inspect 容器名`（查看是否有 `--privileged`、`--cap-add`）；`docker exec -it 容器名 id`（查看运行用户） |
| 日志 | `docker logs 容器名` + Java 应用日志（异常访问、权限不足、资源溢出） |

## 7. 核心要点

> 🎯 **核心要点**：
> - 隔离三技术：Namespace（可见性）、Cgroups（上限）、UnionFS（文件系统）——都是"有限隔离"；
> - 四大危险：资源抢占、网络逃逸、文件篡改、权限滥用；
> - Java 核心纪律：**Xmx ≤ 容器内存 80%**，Java 8 加 CGroup 参数；
> - 最小权限三件套：禁 privileged、按需 cap-add、非 root 用户运行；
> - 敬畏边界：隔离不是绝对安全——"最小权限原则、资源匹配原则、隔离隔离原则"。

## 8. 参考来源

- [Docker 安全最佳实践官方文档](https://docs.docker.com/engine/security/)
- [Docker 资源限制（runtime options）](https://docs.docker.com/config/containers/resource_constraints/)
- [Docker seccomp 安全配置文件](https://docs.docker.com/engine/security/seccomp/)
- [Docker 容器安全性（run 参考）](https://docs.docker.com/reference/cli/docker/container/run/)

---

**下一模块**：[09-多主机与Swarm集群](09-多主机与Swarm集群.md)　/　**返回总览**：[00-总览](00-Docker知识体系总览.md)
