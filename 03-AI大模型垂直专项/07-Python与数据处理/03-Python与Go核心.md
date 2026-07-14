# Python 与 Go 核心

## 📌 课程定位
Python是AI/数据科学的第一语言——生态无敌。Go是云原生的基础设施语言——Docker/K8s都用Go写的。这两门语言代表了两条技术路线。

## 🎯 核心章节

### 第一部分：Python

### 1. Python 基础特性
- **动态类型 + 强类型**：变量无类型、对象有类型，`"1"+2` 报错（不隐式转换）
- **一切皆对象**：函数、类、模块都是对象——`type` 是元类
- **可变 vs 不可变**：list/dict/set可变，int/str/tuple不可变——影响参数传递和hashable
- **GIL（全局解释器锁）**：CPython的"命门"——同一时刻只有一个线程执行Python字节码
  - CPU密集型用多进程(multiprocessing)，IO密集型用多线程/协程(asyncio)

### 2. 核心数据结构
- **list**：动态数组，append=O(1)均摊——`[x for x in range(10)]` 列表推导式
- **tuple**：不可变列表，可哈希，可做dict的key
- **dict**：哈希表实现——Python 3.6+ 保持插入顺序(compact dict)
- **set**：哈希表实现的无序不重复集合——交集(`&`)、并集(`|`)、差集(`-`)
- **deque**：双端队列——`collections.deque`

### 3. 高级特性
- **生成器(Generator)**：`yield` 惰性求值，节省内存——适合处理大文件/无限序列
- **装饰器(Decorator)**：`@wrapper` 本质是语法糖，AOP利器——日志/权限/缓存
- **上下文管理器**：`with` 语句——`__enter__`/`__exit__`，确保资源释放
- **f-string**：`f"Hello {name}"`——Python 3.6+，简洁高效

### 4. AI/ML 生态（Python的核心优势）
- **NumPy**：多维数组+向量化运算——所有数值计算的基础
- **Pandas**：DataFrame——数据清洗、探索性分析的主力
- **Matplotlib/Seaborn**：数据可视化
- **Scikit-learn**：传统机器学习算法库——分类/回归/聚类/降维
- **PyTorch / TensorFlow**：深度学习框架——动态图(PyTorch) vs 静态图(TF)

### 第二部分：Go

### 5. Go 语言核心
- **并发模型（Goroutine + Channel）**：轻量级协程(2KB栈)+CSP通信模型
  - `go func()` 启动一个goroutine——百万级并发不是梦
  - Channel：有缓冲/无缓冲，`select` 多路复用
  - **不要通过共享内存来通信，而要通过通信来共享内存**
- **GMP调度模型**：G(Goroutine)→P(Processor,逻辑核)→M(Machine,系统线程)——work stealing
- **defer**：函数返回前执行，LIFO栈顺序——常用于释放锁/关闭文件
- **interface{}**：隐式实现——无需显式声明implements
- **错误处理**：`if err != nil`——Go的特色，不是异常

### 6. Go 在后端的优势
- 编译为静态二进制文件（无运行时依赖）——Docker镜像极小
- 高并发、低内存——适合微服务、API网关、消息队列
- 生态：Docker、Kubernetes、Prometheus、etcd、TiDB——云原生标准语言

## ✅ 学习建议
- Python：先搞定NumPy+Pandas，再学PyTorch——这是AI方向的标准路径
- Go：先写好goroutine+channel的模式，再理解GMP调度
- 两门语言都学——Python做数据分析/模型训练，Go做高性能服务
