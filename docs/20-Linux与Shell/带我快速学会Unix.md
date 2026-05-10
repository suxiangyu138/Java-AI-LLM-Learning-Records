<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# 带我快速学会Unix

你已经有 Linux 命令基础，其实“快速学会 Unix”的核心就是两件事：
1）把 Unix 的概念体系补上；2）把你现在会的 Linux 命令迁移为对“类 Unix 系统一族”的通用能力。[^1_1][^1_2]

下面我给你一套专门适配你 Java 后端＋AI 方向的 Unix 快速学习路线，用 3～5 天就能把“该知道的 Unix”吃透，之后遇到 macOS、各类服务器都能举一反三。

***

## 1. 先搞清楚：Unix 到底是什么

- Unix 是 70 年代 Bell Labs 做的多用户、多任务操作系统家族，今天主要以各厂商的商用 Unix（AIX、HP-UX、Solaris 等）存在。[^1_3][^1_1]
- Linux 是“类 Unix”系统，它在设计上模仿 Unix，但代码完全重写、开源、免费，本质上是 Unix 思想的延续版。[^1_4][^1_5]

你要形成一个**家族树**的认知（方便面试说人话）：

- Unix 家族（商业）：AIX、HP-UX、Solaris、早期的 BSD 等。[^1_1][^1_3]
- Unix-like / POSIX 系统：Linux 各发行版、FreeBSD、macOS 等，这些都遵守 POSIX 标准，命令接口高度兼容。[^1_6][^1_7]

对你来说：
你已经会 Linux 命令 ≈ 已经掌握了大部分 Unix 命令的用法，剩下是标准、历史和少量差异的补齐。[^1_7][^1_1]

***

## 2. 一眼看懂的 Unix vs Linux 速记表

这张表你可以直接抄进笔记里，当成“面试八股速背版”。[^1_8][^1_5][^1_1]


| 维度 | Unix | Linux |
| :-- | :-- | :-- |
| 起源年代 | 1970s Bell Labs，C 语言重写 | 1990s Linus Torvalds 基于 Unix 思想重做内核 |
| 是否开源 | 大多是商业闭源，要买授权 | 开源，遵循 GPL，任何人可用可改 |
| 代表系统 | AIX、HP-UX、Solaris、早期 BSD | Ubuntu、CentOS、Debian、Arch 等 |
| 标准关系 | 是最早的“事实标准”实现之一 | 遵循 POSIX，兼容 Unix 风格 API 与命令行 |
| 内核形态 | 单体内核为主，厂商定制严重 | 单体＋模块化，可动态加载内核模块 |
| 典型场景 | 大型企业服务器、银行、电信、传统机房 | 云服务器、容器、桌面、嵌入式、手机等 |
| 命令生态 | 经典 Unix 工具集，版本较保守 | 核心命令类似，但大量 GNU 增强版工具 |

表里这些点全部是真·面试常问点，你以后如果被问“说说 Unix 和 Linux 的区别”，照这个维度展开就够用了。[^1_5][^1_8][^1_1]

***

## 3. Unix 要学的“核心三件套”：内核、Shell、工具

和 Linux 一样，Unix 也可以拆成三层来理解：[^1_2][^1_8][^1_7]

1. 内核（Kernel）
    - 负责进程调度、内存管理、文件系统、设备驱动等，是操作系统的核心。Unix 和 Linux 都采用单体内核结构，不过 Linux 加了模块化机制，能动态加载驱动和功能。[^1_5][^1_3]
2. Shell（命令行解释器）
    - Shell 是用户与内核之间的接口，你敲的命令由 Shell 解析后，再调用内核和工具程序。[^1_8][^1_2]
    - 典型 Unix 上早期是 Bourne Shell（sh）、后来又有 C Shell（csh）、Korn Shell（ksh），Linux 上则以 Bash 为主。[^1_2][^1_1]
3. 工具程序（命令行工具）
    - ls、cp、mv、ps、grep、awk、sed 这些“标准工具集”在 Unix 和 Linux 上几乎长一样，只是 GNU 版本往往功能更丰富、带更多参数。[^1_7][^1_2]

对你来说：
你已经练过 Linux 的命令、管道、重定向，现在要做的是：
“把这些能力理解为：我学的是 POSIX / Unix 风格命令行，而不仅仅是 Ubuntu 上这一套”。[^1_6][^1_7]

***

## 4. 3～5 天 Unix 快速路线图（完全基于你已有的 Linux 基础）

### 第 1 天：概念＋标准，搞懂 POSIX 思维

目标：知道自己在学的不是单一系统，而是一整类系统的“共同约定”。[^1_9][^1_6]

建议任务：

- 用 Markdown 记一页笔记，回答三个问题：
1）什么是 Unix（历史＋特点）
2）什么是 Unix-like / POSIX 系统
3）Unix vs Linux 的主要区别
- 笔记内容可以参考：
    - Unix、Linux 比较文章中的定义和特性描述。[^1_1][^1_5]
    - POSIX 是如何定义“兼容层”的（API、Shell、命令集）。[^1_9][^1_6]

完成标准：

- 你能用自己的话，在 1 分钟内讲清楚：“为什么 macOS、Linux 都算 Unix-like？”[^1_3][^1_6]
- 你能画出一张简易家族图：Unix → POSIX → Unix-like（Linux、BSD、macOS 等）。[^1_6][^1_3]

***

### 第 2 天：把 Linux 命令上升到“Unix 通用命令”

目标：意识到：你掌握的是“POSIX 命令接口”，而不是“Ubuntu 专用技巧”。[^1_7][^1_6]

你可以做一个对照练习（写在 md 里）：

- 按功能分类列出命令，并标注“POSIX 标准工具”：
    - 文件与目录：ls、cp、mv、rm、mkdir、rmdir、ln。[^1_2][^1_7]
    - 文件内容：cat、head、tail、less、wc、sort、uniq。[^1_2][^1_7]
    - 搜索：grep、find。[^1_7][^1_2]
    - 进程：ps、kill。[^1_7]
    - 系统状态：df、du、top / prstat / vmstat 等（不同 Unix 可能略有差异）。[^1_7]
- 把你在 Linux 上常用的命令写出来，顺便写一句：
“如果换成 Solaris/AIX，只要符合 POSIX，命令名和核心参数基本不变，最多在高级选项上有点差异。”[^1_6][^1_7]

完成标准：

- 你能回答面试问题：“为什么学 Linux 对理解 Unix 有帮助？”并从 POSIX 标准和命令兼容性角度解释。[^1_1][^1_6][^1_7]

***

### 第 3 天：Unix 文件系统与权限模型（和 Linux 类似，但要以“Unix 通识”来理解）

Unix 的文件系统模型、本质上就是你在 Linux 上已经用过的那套：一棵单根目录树、设备和一切都抽象成文件、通过权限控制安全。[^1_2][^1_7]

必备要点（写在笔记里，顺便结合命令实验）：

- 根目录 /、家目录、典型系统目录（/bin、/sbin、/usr、/etc、/var 等）及其职责。[^1_2][^1_7]
- 权限三元组：所有者、所属组、其他人，对应读/写/执行位 r/w/x，在 Unix 上也是同样的表示方式。[^1_7]
- chmod、chown、chgrp 的语义，数字权限（755/644）与符号权限（u+rwx,g+rx,o+rx）的对应关系。[^1_7]

完成标准：

- 不看笔记，能解释 ls -l 行中每一段的信息含义（类型、权限、硬链接数、所有者、组、大小、时间、文件名）。[^1_7]
- 能写出两条命令：
    - 把某个目录改为 755 / 某个配置文件改为 640。
    - 把某个目录及子文件归属给指定用户和用户组（递归 chown）。[^1_7]

***

### 第 4～5 天：面向“真正的 Unix 环境”的迁移能力

你以后极大概率会遇到两类场景：

1）登录一台真实的商用 Unix（AIX/Solaris 等）的生产服务器；
2）在 macOS 上开发、运维（macOS 本身就是基于 BSD / XNU 内核的 Unix 系统）。[^1_3]

练习方式（如果暂时没有真机，就用“假设环境＋笔记模拟”的方式）：

- 制作一份“登录陌生 Unix 服务器后的自检 checklist”：
    - uname -a：看内核与系统信息。[^1_7]
    - echo \$SHELL：看默认 Shell 类型。
    - ps、top（或者厂商自带工具）看进程情况。[^1_7]
    - df -h 看磁盘、du -sh 看目录大小。
- 列出 5 条你在 Linux 上排查问题常用的命令（比如 tail -f 日志、grep 关键字、ps aux | grep 进程名 等），然后标注：
    - 在 Unix 上这些命令是否存在（一般存在）；
    - 有哪些可能差异（比如 ps 的参数不完全一样，top 换成 prstat 等）。[^1_1][^1_7]

完成标准：

- 你能写出一段话，描述“我登陆一台从没见过的 Unix 服务器，前 5 分钟会做哪些检查和确认”，并且不依赖特定发行版细节。[^1_1][^1_7]

***

## 5. 给你一个“Unix 入门 md 笔记骨架”

你可以新建一个 `unix-quickstart.md`，结构建议这样搭，之后可以填充内容甚至传到 GitHub：

```markdown
# Unix 快速入门笔记（面向 Java/AI 开发）

## 1. 什么是 Unix
- Unix 的历史和设计目标
- Unix 家族 vs Unix-like vs POSIX
- Unix vs Linux：核心差异点

## 2. Unix 系统架构
- 内核 / Shell / 工具
- 典型 Unix 系统示意图
- 与 Linux 的对应关系

## 3. Unix 文件系统与权限
- 目录结构与关键目录
- 权限模型（用户/组/其他）
- 常用命令：ls/chmod/chown/chgrp

## 4. POSIX 标准与命令兼容
- POSIX 是什么、解决什么问题
- Unix、Linux、macOS 为何能通用命令
- 我学过的 Linux 命令中哪些是 POSIX 标准

## 5. 登录陌生 Unix 服务器的操作习惯
- 登录后自检 checklist
- 查看系统信息、进程、磁盘、日志
- 需要注意的差异点（ps/top 等）

## 6. 面试向问答整理
- Unix 和 Linux 的区别
- 为什么学 Linux 等于掌握类 Unix 能力
- 什么是 POSIX，举几个 POSIX 系统例子
```

这个骨架基本覆盖了主流教程里的 Unix 初级内容，同时又紧贴你以后求职要说得清楚的点。[^1_2][^1_7]

***

如果我帮你把这个 `unix-quickstart.md` 直接按上面路线写满一版“可直接放到 GitHub 的版本”，你是更想要偏“面试八股风”还是偏“教学讲解风”？
<span style="display:none">[^1_10]</span>

<div align="center">⁂</div>

[^1_1]: https://www.geeksforgeeks.org/linux-unix/linux-vs-unix/

[^1_2]: https://github.com/dinanathsj29/unix-tutorial-for-beginners

[^1_3]: https://www.redhat.com/en/blog/unix-linux-history

[^1_4]: https://ithelp.ithome.com.tw/articles/10210448

[^1_5]: https://www.linuxjournal.com/content/unix-vs-linux-what-is-the-difference

[^1_6]: https://en.wikipedia.org/wiki/POSIX

[^1_7]: https://www.doc.ic.ac.uk/~wjk/UnixIntro/

[^1_8]: https://www.coursera.org/articles/unix-vs-linux

[^1_9]: https://machaddr.substack.com/p/posix-standards-historic-background

[^1_10]: https://www.reddit.com/r/explainlikeimfive/comments/1ctu2kn/eli5_whats_the_difference_between_unix_and_linux/


---

# 两种都要，先八股后讲解版

先给你一份“Unix 八股速记版”，可以直接背；后面是“教学讲解＋例子版”，适合你当 GitHub 笔记模板用。[^2_1][^2_2][^2_3]

***

## 一、Unix 八股速记版（适合面试快速背）

### 1. 什么是 Unix？

- Unix 是 1970 年代 Bell Labs 设计的多用户、多任务操作系统家族，主要面向服务器和大型机场景。[^2_4][^2_1]
- 现代 Unix 常指一系列通过标准认证的商业系统，如 AIX、HP-UX、Solaris 等。[^2_1][^2_4]

***

### 2. Unix 和 Linux 的关系与区别

- Unix 是最早的商用操作系统家族，多为闭源、收费，由厂商维护和发行。[^2_4][^2_1]
- Linux 是 1990 年代基于 Unix 思想重写的开源内核，属于 Unix-like 系统，实现了类似接口与命令行环境。[^2_1][^2_4]
- 两者都采用单体内核、支持多用户多任务，并且大部分命令和系统调用接口相似。[^2_4][^2_1]
- Unix 更常见于传统金融、电信、机房场景，强调稳定和厂商支持；Linux 更流行于互联网服务器、云计算、容器、桌面和嵌入式设备。[^2_5][^2_1][^2_4]

***

### 3. 什么是 POSIX？为什么重要？

- POSIX 是基于 Unix 的一组标准接口，全称 Portable Operating System Interface，用来规范系统调用、Shell 和常用工具。[^2_2][^2_6]
- 只要操作系统遵守 POSIX 标准，应用和脚本就可以在不同系统之间以较小改动进行移植。[^2_2]
- 常见的 POSIX 系统包括多数 Unix 发行版、Linux 发行版、macOS 等。[^2_7][^2_2]

***

### 4. Unix 的系统架构（面试版）

- Unix 采用分层结构，大致分为内核层、系统调用接口层、Shell 与工具层、应用程序层。[^2_3][^2_4]
- 内核负责进程调度、内存管理、文件系统、设备管理等，是整个系统的核心。[^2_3][^2_4]
- Shell 是命令行解释器，接收用户输入并调用内核和工具程序，如 sh、csh、ksh、后来的 bash 等。[^2_3][^2_1]
- 工具层提供大量小而精的命令行工具（如 ls、cp、grep、awk、sed），通过管道组合完成复杂任务。[^2_8][^2_3]

***

### 5. Unix 文件系统和权限模型

- Unix 使用单根目录结构，以 / 作为根目录，所有文件和设备都挂在这棵树上。[^2_8][^2_3]
- 典型目录包括 /bin、/sbin、/usr、/etc、/var、/home 等，各司其职。[^2_8][^2_3]
- 权限模型基于“拥有者/用户组/其他人”三类主体和“读/写/执行”三种权限，通过 rwx 与数字表示（如 755、644）来控制访问。[^2_8]
- 常用权限相关命令包括 ls、chmod、chown、chgrp 等。[^2_8]

***

### 6. 学 Unix 对后端开发的价值

- Unix 和 Linux 命令高度兼容，掌握 Linux 即基本掌握类 Unix 系统的命令行环境。[^2_1][^2_8]
- 大部分服务器、云平台和容器环境都是基于 Unix-like 系统，熟悉 Unix 思想有利于排查问题、部署应用、编写脚本。[^2_5][^2_1]

***

### 7. 面试常见问法速答模板

你可以直接按下面结构临场组织语言：

1. “什么是 Unix？”
    - “Unix 是 70 年代在 Bell Labs 诞生的多用户、多任务操作系统家族，主要用于服务器和大型机，后来衍生出很多商业系统，比如 AIX、HP-UX 和 Solaris 等。”[^2_4][^2_1]
2. “Unix 和 Linux 有什么区别？”
    - “Unix 是早期的商业操作系统家族，多为闭源、收费，由厂商统一发布；Linux 是 90 年代重写的开源内核，属于 Unix-like 系统，接口和命令与 Unix 类似，现在广泛用于服务器和云计算。”[^2_1][^2_4]
3. “什么是 POSIX？为什么要遵守 POSIX？”
    - “POSIX 是一组基于 Unix 的标准接口，规范了系统调用、Shell 和常用工具，目的是让应用可以在不同 Unix-like 系统之间方便移植。Linux、macOS 等都实现了很大一部分 POSIX 标准。”[^2_7][^2_2]

***

## 二、Unix 教学讲解版（适合写到 `unix-quickstart.md`）

这一部分我直接按你写文档的风格来，适合放 GitHub，当成“Unix 入门＋面试向”的综合笔记。[^2_2][^2_3][^2_8]

***

### 1. Unix 是什么：从“一个系统”到“一个家族”

Unix 最早是 1970 年代 AT\&T Bell Labs 的工程师用 C 语言重写的操作系统，目标是轻量、可移植、适合多人同时使用。[^2_9][^2_1]
一开始 Unix 跑在大型机和小型机上，逐步在高校和企业流行起来，形成了大量变种和分支，这些变种构成了今天所谓的“Unix 家族”。[^2_9][^2_4]

现在我们说 Unix，通常有两层含义：[^2_4][^2_1]

- 狭义：通过正式标准认证（如 Single UNIX Specification）的系统，比如 AIX、HP-UX、Solaris 等商业版本。
- 广义：遵循 Unix 思想、接口和工具集的一系列类 Unix 系统，包含各种 Unix 变种以及 Unix-like 系统。

对你这种以后要连服务器、跑 Java/AI 服务的开发者来说，更重要的是把 Unix 理解为“一整类系统的设计哲学和约定”，而不是单一品牌的操作系统。[^2_9][^2_1]

***

### 2. Unix vs Linux：为什么你学 Linux 就等于半只脚踏进 Unix 世界

Linux 出现在 1990 年代，是 Linus Torvalds 在个人电脑上重写的一个内核，它借鉴了 Unix 的很多设计，比如多用户、多任务、分层架构和命令行环境。[^2_1][^2_4]
但 Linux 完全独立开发，不包含 Unix 源码，所以在法律意义上不是“Unix”，而是“Unix-like”。[^2_4][^2_1]

两者的共同点和差异可以这样理解：[^2_5][^2_1][^2_4]

- 共同点
    - 架构类似：都有内核、系统调用接口、Shell、工具程序和应用层。
    - 使用多用户、多任务模型，通过权限和进程隔离保障安全。
    - 命令行体验高度相似，ls、cp、grep、awk、sed 等经典工具基本通用。
- 差异
    - 授权模式：Unix 多为厂商闭源、商业授权；Linux 遵循 GPL，完全开源。
    - 发行方式：Unix 常由特定厂商配套硬件一体交付；Linux 有大量发行版，社区和商业公司共同维护。
    - 使用场景：Unix 仍活跃在传统企业关键系统中；Linux 几乎统治了互联网服务器、云平台和容器环境。

你已经在 WSL/Ubuntu 里练了一堆命令，其实学习的就是“类 Unix 命令行操作”。
换句话说，你已经掌握了在大多数 Unix-like 系统上通用的一套技能。[^2_8][^2_1]

***

### 3. POSIX：把整个 Unix 家族“捆在一起”的标准

由于早期 Unix 分裂出很多版本，应用在不同系统间移植很麻烦，于是业界提出了 POSIX 标准。[^2_2]
POSIX（Portable Operating System Interface）是一组基于 Unix 的接口规范，包含：系统调用、Shell 行为、常用命令与工具的标准定义。[^2_6][^2_2]

它解决的核心问题是：
只要操作系统实现了 POSIX，应用程序和脚本就可以在不同厂商的 Unix、Linux、macOS 等系统之间较为平滑地迁移。[^2_2]

对你来说，记住两点就够用了：[^2_7][^2_2]

- 你在 Linux 上用的很多命令和系统调用，其实是在用 POSIX 定义的东西。
- 你写的符合 POSIX 规范的 Shell 脚本，在大多数 Unix 服务器上都能跑，只需要微调极少部分。

***

### 4. Unix 系统架构：从内核到 Shell 再到工具

可以把 Unix 想象成一个分层的洋葱，从里到外是内核、系统调用接口、Shell/工具、中间件和应用。[^2_3][^2_4]

1. 内核（Kernel）
    - 负责管理 CPU、内存、磁盘和设备，调度进程、处理中断，是整个系统的运行核心。[^2_3][^2_4]
    - Unix 和 Linux 都采用单体内核设计，但 Linux 做了模块化扩展，可以动态加载驱动等模块。[^2_4]
2. 系统调用接口（System Call Interface）
    - 提供 read、write、open、fork、exec 等基础操作，供应用通过库调用。[^2_2][^2_3]
    - POSIX 标准对这些接口做了统一，帮助应用在不同系统上编译和运行。[^2_2]
3. Shell 与工具程序
    - Shell 是交互层，比如 sh、csh、ksh，后来有 Bash 等；它负责解析命令并调用内核和工具程序。[^2_3][^2_1][^2_8]
    - 工具程序是 Unix 的精髓：小而专一的命令，通过管道组合成强大的处理链条（ls、grep、awk、sed、sort 等）。[^2_8][^2_3]
4. 应用与服务
    - 数据库、Web 服务器、中间件等都运行在上述层次之上，面向最终用户和业务系统提供服务。[^2_4]

你已经在 Linux 上大量使用 Shell 和工具层，接下来只需要把这些理解上升到“Unix 通用架构”层面即可。[^2_3][^2_8]

***

### 5. Unix 文件系统与权限：和你熟悉的 Linux 那套是一脉相承的

Unix 的文件系统设计强调“一切皆文件”，包括普通文件、目录、设备、管道等都用统一的抽象来表示。[^2_8][^2_3]

关键点有三：[^2_3][^2_8]

1. 单根目录结构
    - 以 / 作为根目录，整个系统是一棵树。
    - 典型目录职责：
        - /bin：基本命令
        - /sbin：系统管理命令
        - /usr：用户空间应用和库
        - /etc：配置文件
        - /var：日志等可变数据
        - /home：用户家目录
2. 权限模型
    - 每个文件都有拥有者（user）、所属组（group）、其他人（others）三个角色。
    - 每个角色有读（r）、写（w）、执行（x）三种权限，组合成 rwx，数值表示为 4/2/1。
    - 例如 755 表示拥有者 rwx，组和其他人 rx；644 表示拥有者 rw，组和其他人 r。
3. 常用命令
    - ls -l 查看权限与拥有者信息。
    - chmod 修改权限，支持符号和数字两种方式。
    - chown / chgrp 修改文件所属用户和用户组。

这些概念你在 Linux 里全部用得到，在 Unix 服务器上也是同一套逻辑，只是具体目录布局和工具版本可能略有差异。[^2_8][^2_3]

***

### 6. 登录陌生 Unix 服务器时，你应该怎么干？

假设你 SSH 登录了一台从没见过的 Unix 服务器（比如 AIX/Solaris），前几分钟可以按以下顺序来摸底环境：[^2_1][^2_8]

1. 识别系统类型和版本
    - uname -a：查看内核和系统信息。
    - echo \$SHELL：查看默认 Shell。
2. 熟悉文件系统和基本工具
    - pwd、ls、cd 等确认当前目录和结构。
    - which 或 type 查看常用命令所在路径。
3. 检查资源与状态
    - df -h：看磁盘空间。
    - du -sh 某些关键目录：看目录体积。
    - ps 和厂商特定工具（如 prstat 等）看进程和 CPU 使用情况。
4. 观察日志和配置
    - 查看 /var/log、应用日志目录等，确认日志策略。
    - 检查重要服务的配置文件（通常在 /etc 或应用自己的目录下）。

这些操作在大多数 Unix-like 系统上都成立，只是在命令的参数和工具名称上可能略有差别，你凭经验和帮助文档（man、--help）就能快速适配。[^2_1][^2_2][^2_8]

***

### 7. 如何把“Unix + Linux + POSIX”融进你简历和项目叙述？

结合你 Java 后端＋AI 的方向，可以自然地在简历/项目里这样表达：

- “熟练使用基于 Unix 的类 Linux 服务器环境，掌握 POSIX 风格命令行、Shell 脚本和常用工具链，用于部署和运维 Java / AI 服务。”[^2_2][^2_1]
- “能够在不同 Unix-like 系统（Linux/macOS 等）上完成环境搭建、日志分析、资源监控和故障排查。”[^2_1][^2_8]

这样既显得你知道底层背景（Unix/POSIX），又紧贴你真实的 Linux 实操经验，逻辑上是完整的。

***

如果我现在帮你把 `unix-quickstart.md` 直接生成一份成稿：上半部分是刚才的八股浓缩版，下半部分是教学讲解＋命令示例，你打算是用在 GitHub 公开仓库，还是先放在本地 Obsidian/VS Code 笔记里？
<span style="display:none">[^2_10][^2_11][^2_12]</span>

<div align="center">⁂</div>

[^2_1]: https://www.geeksforgeeks.org/linux-unix/linux-vs-unix/

[^2_2]: https://www.techtarget.com/whatis/definition/POSIX-Portable-Operating-System-Interface

[^2_3]: https://github.com/dinanathsj29/unix-tutorial-for-beginners

[^2_4]: https://www.theknowledgeacademy.com/blog/unix-vs-linux/

[^2_5]: https://www.coursera.org/articles/unix-vs-linux

[^2_6]: https://en.wiktionary.org/wiki/POSIX

[^2_7]: https://en.wikipedia.org/wiki/POSIX

[^2_8]: https://info-ee.surrey.ac.uk/Teaching/Unix/

[^2_9]: https://www.redhat.com/en/blog/unix-linux-history

[^2_10]: https://www.linkedin.com/posts/praveen-singampalli_linux-interview-questions-basic-level-activity-7307971527495364609-b2-_

[^2_11]: https://www.scribd.com/doc/5635990/UnixLinux-Basic-interview-question

[^2_12]: https://www.reddit.com/r/unix/comments/1dz3szh/top_30_unix_command_interview_questions_asked_in/

