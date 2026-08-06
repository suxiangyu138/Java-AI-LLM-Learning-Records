# 01 - CLI 概述与设计哲学

> 🎯 CLI = Command Line Interface — 后端工程师的"母语界面"。理解 Unix 管道哲学和 CLI 设计原则，才能写出好用的命令行工具

---

## 1. CLI vs GUI 本质区别

| 维度 | CLI | GUI |
|------|:---:|:---:|
| 操作方式 | 文本命令 | 鼠标点击 |
| 学习成本 | 陡峭（需记忆命令） | 低（直觉操作） |
| 操作效率 | ⭐⭐⭐ 批量/脚本/管道 | ⭐ 逐个手动 |
| 自动化 | ✅ 天然可脚本化 | ❌ 难以自动化 |
| 远程操作 | ✅ SSH 无缝 | ❌ 需 VNC/RDP |
| 资源消耗 | 极低 | 高 |
| 适用 | 服务器/开发/运维 | 普通用户/办公 |

```text
Unix 哲学（Doug McIlroy）：
  1. 每个程序只做一件事，并把它做好
  2. 程序之间通过文本流（管道）协作
  3. 程序的输出可以成为另一个程序的输入

示例：找出访问量 Top 10 的 IP
  cat access.log | awk '{print $1}' | sort | uniq -c | sort -rn | head -10
  ↑ 每个命令只做一件事，通过 | 串联
```

---

## 2. CLI 程序的设计原则

| 原则 | 说明 | 示例 |
|------|------|------|
| `--help` | 必须提供帮助信息 | `java --help` |
| 标准输入 | 支持管道输入 | `echo "data" \| myapp` |
| 标准输出 | 可重定向到文件 | `myapp > result.txt` |
| 退出码 | 0=成功 非0=失败 | `echo $?` |
| 静默模式 | 不输出非必要内容 | `--quiet` / `-q` |
| 幂等性 | 多次执行结果一致 | — |

```bash
# ✓ 好的 CLI 设计
git commit -m "fix: bug"
git branch --delete feature/old
docker ps --format "table {{.Names}}\t{{.Status}}"

# ✗ 坏的 CLI 设计
myapp --do-something-and-then-output-to-file  # 一次做太多事
myapp -xyz  # 参数含义不清晰
```

---

## 3. 后端常用的 CLI 分类

| 分类 | 工具示例 | 后端场景 |
|------|----------|----------|
| 文本处理 | grep/awk/sed/jq | 日志分析、配置提取 |
| 系统监控 | top/htop/ss/df | CPU/内存/端口排查 |
| 网络调试 | curl/nc/ping | API测试/端口检测 |
| 文件操作 | find/tar/rsync | 文件清理/备份 |
| 进程管理 | ps/kill/jps/jstack | Java进程管理 |
| 构建工具 | mvn/gradle/npm | CI/CD脚本 |
| 版本控制 | git | 代码管理 |
| 开发辅助 | jq/yq/httpie | JSON/YAML处理 |

---

## 4. Java CLI 程序基本结构

```java
// 最简单的 CLI
public class HelloCli {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java HelloCli <name>");
            System.exit(1);       // 非0退出码
        }
        System.out.println("Hello, " + args[0] + "!");
    }
}

// 使用 System.console()（支持密码输入）
Console console = System.console();
String username = console.readLine("Username: ");
char[] password = console.readPassword("Password: ");
```

| 需求 | 基础方案 | 专业方案 |
|------|----------|----------|
| 参数解析 | `args[]` 手动解析 | Picocli / JCommander |
| 交互式 | `System.console()` | JLine / Spring Shell |
| 进度条 | 手写 `\r` 覆盖 | Meeny |
| 彩色输出 | ANSI 转义码 | Jansi |

> 🎯 **CLI 设计心法**：做好一件事 + 支持管道 + 提供 --help + 正确退出码。好的 CLI 工具是"搭积木"式的，通过管道无限组合。
