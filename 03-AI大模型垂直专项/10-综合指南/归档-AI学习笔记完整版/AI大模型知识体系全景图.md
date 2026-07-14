# 🗺️ AI大模型：从小白到大神 — 完整知识体系

> **核心理念：** 不跳步、不贪快、层层递进。每个阶段的知识点都是下一层的基石。
> **学习原则：** 理论理解 → 代码实现 → 项目实战 → 优化迭代

---

## 📐 能力等级定义

| 等级 | 称号 | 能力标准 | 预计时间 |
|:--:|------|----------|:--:|
| Lv.0 | 小白 | 会Python基础，能调用API | 起点 |
| Lv.1 | 入门 | 理解Prompt/API/Embedding基本概念，能搭建简单AI应用 | 2-3月 |
| Lv.2 | 初级 | 掌握RAG全流程，能独立开发AI应用后端 | 4-6月 |
| Lv.3 | 中级 | 掌握Agent开发/微调/全栈，能交付企业级项目 | 7-12月 |
| Lv.4 | 高级 | 精通性能优化/架构设计/多模态，能设计复杂AI系统 | 1-2年 |
| Lv.5 | 大神 | 跟踪前沿/贡献开源/创新架构，能定义行业最佳实践 | 2年+ |

---

## 第一阶段：地基篇（Lv.0 → Lv.1 入门）

### 1.1 Python编程基础

```
知识点清单：
├── 基础语法
│   ├── 变量与数据类型：int, float, str, bool, None
│   ├── 容器类型：list, tuple, dict, set → 各自的时间复杂度
│   ├── 控制流：if/elif/else, for/while, break/continue
│   ├── 函数：def, 参数类型(位置/关键字/默认/可变), return, 作用域
│   ├── 类型提示：typing模块, Optional, Union, List, Dict, TypeVar
│   ├── 异常处理：try/except/else/finally, 自定义异常, 异常链
│   └── 上下文管理器：with语句, __enter__, __exit__, contextlib
│
├── 面向对象
│   ├── 类和对象：__init__, self, 实例方法/类方法/静态方法
│   ├── 继承与多态：super(), MRO(Method Resolution Order)
│   ├── 魔术方法：__str__, __repr__, __call__, __getitem__, __iter__
│   ├── 属性管理：@property, @setter, @deleter
│   ├── 数据类：@dataclass, field, 不可变对象
│   └── 抽象类：ABC, @abstractmethod
│
├── 函数式编程
│   ├── 高阶函数：map, filter, reduce, sorted(key=...)
│   ├── 匿名函数：lambda表达式, 使用场景与限制
│   ├── 推导式：列表/字典/集合/生成器推导式
│   ├── 闭包：外部变量引用, nonlocal
│   └── 装饰器：@语法糖, 带参数装饰器, 类装饰器, functools.wraps
│
├── 迭代器与生成器
│   ├── 可迭代对象 vs 迭代器：__iter__, __next__
│   ├── 生成器函数：yield, yield from
│   ├── 生成器表达式：(x for x in ...)
│   └── itertools模块：chain, cycle, groupby, combinations, product
│
├── 文件与IO
│   ├── 文本文件：open, read/write/append, 编码处理(utf-8/gbk)
│   ├── 二进制文件：struct, bytes, bytearray
│   ├── JSON：json.dumps/loads, 自定义编码器
│   ├── CSV：csv.reader/writer, DictReader/DictWriter
│   ├── Pickle：序列化与安全问题
│   ├── Path操作：pathlib.Path, 跨平台路径处理
│   └── 流式读取：大文件分块读取, mmap
│
├── 并发编程
│   ├── 线程：threading, Lock, Queue, 线程池(ThreadPoolExecutor), GIL
│   ├── 多进程：multiprocessing, Pool, 进程间通信
│   ├── 异步IO：async/await, asyncio, gather, create_task, Semaphore
│   ├── 协程：事件循环, Future, Task
│   └── 并发模式：生产者-消费者, 发布-订阅, 扇出-扇入
│
├── 包管理与环境
│   ├── pip：安装/卸载/冻结/依赖解析
│   ├── venv/virtualenv：虚拟环境创建与激活
│   ├── conda：环境管理, 包管理, 通道配置
│   ├── poetry/pdm：现代依赖管理, pyproject.toml
│   └── 打包发布：setup.py, wheel, twine, __init__.py导出控制
│
└── 工程规范
    ├── PEP 8：命名规范, 缩进, 行长度, 空行
    ├── 代码格式化：black, isort, autopep8
    ├── 静态检查：flake8, pylint, mypy(类型检查)
    ├── 文档字符串：Google风格, NumPy风格, Sphinx
    └── 项目结构：src-layout, flat-layout, __main__.py
```

### 1.2 NumPy数值计算

```
知识点清单：
├── 数组基础
│   ├── 创建数组：np.array, zeros, ones, empty, full, arange, linspace
│   ├── 数组属性：shape, dtype, ndim, size, itemsize, strides
│   ├── 数据类型：int8~int64, float16~float64, bool, complex, datetime
│   ├── 类型转换：astype, 隐式转换规则, 溢出处理
│   └── 内存布局：C-order(row-major), F-order(column-major), 连续数组
│
├── 索引与切片
│   ├── 基础索引：[i], [i:j], [i:j:k], 负索引
│   ├── 花式索引：[i, j, k], 整数数组索引
│   ├── 布尔索引：arr[arr > 0], 组合条件 & |
│   ├── 多维索引：arr[i, j], arr[:, :, k]
│   └── np.where：条件选择, 三元操作
│
├── 数组运算
│   ├── 逐元素运算：+ - * / // % **, 一元操作
│   ├── 广播机制：Broadcasting规则, 维度对齐, 显式扩展
│   ├── 归约操作：sum, mean, std, var, min, max, argmin, argmax
│   ├── 累积操作：cumsum, cumprod, 沿轴操作(axis参数)
│   └── 比较操作：>, <, ==, np.isclose, np.allclose
│
├── 线性代数
│   ├── 矩阵乘法：np.dot, @运算符, np.matmul
│   ├── 矩阵分解：SVD(np.linalg.svd), QR, Cholesky, 特征值
│   ├── 范数：L1/L2/Frobenius范数, np.linalg.norm
│   ├── 线性方程：np.linalg.solve, lstsq(最小二乘)
│   └── 行列式与逆：det, inv, pinv(伪逆)
│
├── 随机数
│   ├── 生成器：default_rng(新), RandomState(旧), 种子设置
│   ├── 分布：uniform, normal, randint, choice, shuffle, permutation
│   ├── 采样：不放回采样, 加权采样, 分层采样
│   └── 随机种子：可复现性, 全局种子 vs 局部种子
│
├── 数组操作
│   ├── 形状变换：reshape, resize, flatten, ravel, transpose, T
│   ├── 合并与分割：concatenate, stack, hstack/vstack, split, hsplit
│   ├── 插入与删除：insert, delete, append, unique
│   ├── 填充：pad(各种模式), tile, repeat
│   └── 排序搜索：sort, argsort, searchsorted, partition
│
├── AI特化场景
│   ├── Embedding操作：查表(embedding[token_ids]), 相似度计算
│   ├── Softmax：数值稳定版, temperature缩放, log_softmax
│   ├── Attention矩阵：Q@K.T/√d_k, Causal Mask, Padding Mask
│   ├── Token处理：one-hot编码, padding, truncation, mask创建
│   ├── 批量处理：Batch矩阵乘法, 批量归一化
│   └── 损失函数：MSE, Cross-Entropy(含softmax), BCE
│
└── 性能优化
    ├── 向量化：消除Python循环, 用NumPy原生操作
    ├── 内存视图：view vs copy, as_strided
    ├── 就地操作：out参数, += 等
    └── 内存预分配：避免循环中的concatenate
```

### 1.3 数据可视化

```
知识点清单：
├── Matplotlib核心
│   ├── 基础概念：Figure, Axes, Axis, Artist
│   ├── 线图：plot, 线型/颜色/标记, 多线, 图例
│   ├── 散点图：scatter, 气泡图(大小/颜色映射)
│   ├── 柱状图：bar, barh, 分组柱状图, 堆叠柱状图
│   ├── 直方图：hist, 分箱策略, 密度估计
│   ├── 饼图：pie, 环形图(donut)
│   ├── 箱线图：boxplot, 异常值检测
│   ├── 热力图：imshow, pcolormesh, 配色方案
│   ├── 等高线图：contour, contourf
│   └── 3D图：plot_surface, scatter3D
│
├── Seaborn统计可视化
│   ├── 分布图：distplot, kdeplot, ecdfplot
│   ├── 关系图：relplot, scatterplot, lineplot
│   ├── 分类图：catplot, boxplot, violinplot, stripplot
│   ├── 矩阵图：heatmap, clustermap
│   ├── 回归图：lmplot, regplot
│   └── 多面板：FacetGrid, PairGrid, JointGrid
│
├── AI特化可视化
│   ├── 训练曲线：Loss/Accuracy vs Epoch, 多模型对比
│   ├── 学习率曲线：LR Schedule可视化
│   ├── 混淆矩阵：confusion_matrix + heatmap
│   ├── ROC/AUC曲线：多分类, 微平均/宏平均
│   ├── Attention热力图：Token级别的Attention权重
│   ├── Embedding投影：PCA/t-SNE降维散点图
│   ├── Token分析：Token使用分布, 模型成本对比
│   ├── 模型结构图：层连接, 参数流向
│   └── 评估雷达图：多维度模型能力对比
│
└── 交互式可视化
    ├── Plotly：散点图/折线图/3D图, 仪表盘
    ├── Bokeh：大数据交互图, 服务端渲染
    ├── Streamlit：快速数据Dashboard
    └── Gradio：模型Demo界面
```

### 1.4 Pandas数据分析

```
知识点清单：
├── 数据结构
│   ├── Series：一维带标签数组, 索引对齐
│   └── DataFrame：二维表, 列操作/行操作, 多级索引
│
├── 数据读写
│   ├── 文件格式：CSV, Excel, JSON, Parquet, HDF5, SQL
│   ├── 读写参数：编码, 分隔符, 表头, 索引列, 分块读取(chunksize)
│   └── 数据库连接：SQLAlchemy集成, read_sql, to_sql
│
├── 数据清洗
│   ├── 缺失值：isna/notna, dropna, fillna, interpolate
│   ├── 重复值：duplicated, drop_duplicates
│   ├── 类型转换：astype, to_numeric, to_datetime
│   ├── 字符串处理：str访问器, 正则提取, 替换
│   └── 异常值：IQR方法, Z-Score, 截尾/缩尾
│
├── 数据变换
│   ├── 分组聚合：groupby, agg, transform, filter
│   ├── 透视表：pivot_table, melt, stack/unstack
│   ├── 合并：merge, join, concat, append
│   ├── 排序排名：sort_values, rank, nlargest/nsmallest
│   └── 窗口函数：rolling, expanding, ewm(指数加权)
│
├── 数据分析
│   ├── 描述统计：describe, value_counts, unique, nunique
│   ├── 相关性：corr, corrwith, 相关矩阵
│   ├── 分位数：quantile, cut(离散化), qcut(等频分箱)
│   └── 交叉表：crosstab, 列联表分析
│
└── 性能优化
    ├── 分块处理：chunksize, 迭代读取大文件
    ├── 类型优化：category类型, 小整数类型, 稀疏类型
    ├── 向量化操作：避免apply/iterrows, 优先使用内置方法
    └── 并行处理：swifter, dask, modin
```

---

## 第二阶段：机器学习基础篇（Lv.1 入门强化）

### 2.1 经典机器学习算法

```
知识点清单：
├── 监督学习
│   ├── 线性回归
│   │   ├── 数学原理：最小二乘法, 正规方程, 梯度下降推導
│   │   ├── 损失函数：MSE, MAE, Huber Loss
│   │   ├── 正则化：L1(Lasso), L2(Ridge), ElasticNet
│   │   ├── 多项式回归：特征变换, 过拟合风险
│   │   └── 评估指标：R², Adjusted R², RMSE, MAE, MAPE
│   │
│   ├── 逻辑回归
│   │   ├── Sigmoid函数, 决策边界, 概率解释
│   │   ├── 损失函数：Cross-Entropy, 梯度推导
│   │   ├── 多分类：OvR, Softmax回归
│   │   └── 评估：混淆矩阵, Accuracy/Precision/Recall/F1
│   │
│   ├── 决策树
│   │   ├── 分裂准则：信息增益(ID3), 增益率(C4.5), Gini(CART)
│   │   ├── 剪枝：预剪枝(max_depth/min_samples), 后剪枝
│   │   ├── 特征重要性：基于不纯度减少
│   │   └── 可视化：plot_tree, 规则提取
│   │
│   ├── 随机森林
│   │   ├── Bagging原理：Bootstrap采样, 并行训练
│   │   ├── 特征随机性：max_features, 降低树间相关性
│   │   ├── OOB评估：袋外样本验证
│   │   └── 优缺点：抗过拟合, 高维适应, 不可解释
│   │
│   ├── GBDT系列
│   │   ├── 提升原理：前向分步加法模型, 残差拟合
│   │   ├── XGBoost：正则化, 列采样, 稀疏感知, 并行化
│   │   ├── LightGBM：GOSS, EFB, 直方图算法, 叶子优先
│   │   ├── CatBoost：有序提升, 类别特征原生支持
│   │   └── 调参：学习率, 树数, 深度, 子采样, 早停
│   │
│   └── 支持向量机(SVM)
│       ├── 核心思想：最大间隔分类器, 支持向量
│       ├── 核技巧：线性核, RBF核, 多项式核, Sigmoid核
│       ├── 软间隔：C参数, 松弛变量
│       └── 适用场景：小样本高维数据, 文本分类
│
├── 无监督学习
│   ├── K-Means：肘部法则, K-Means++, Mini-Batch K-Means
│   ├── DBSCAN：密度聚类, eps/min_samples, 噪声检测
│   ├── 层次聚类：凝聚/分裂, 树状图(dendrogram)
│   ├── PCA：方差解释率, 特征向量/特征值, 白化
│   ├── t-SNE：困惑度参数, 可视化利器（不能用于特征提取）
│   └── UMAP：拓扑保留, 比t-SNE快
│
├── 模型评估与选择
│   ├── 数据划分：训练/验证/测试, 分层抽样, 时间序列划分
│   ├── 交叉验证：K-Fold, Stratified K-Fold, Group K-Fold
│   ├── 超参搜索：Grid Search, Random Search, Bayes Search
│   ├── 特征工程：归一化/标准化, 编码(OneHot/Label/Target), 分箱
│   ├── 特征选择：Filter(卡方/互信息), Wrapper(RFE), Embedded(L1)
│   └── 过拟合对策：正则化, Dropout, Early Stopping, Data Augmentation
│
└── 集成学习
    ├── Bagging：并行训练, 投票/平均, 降低方差
    ├── Boosting：串行训练, 关注错分样本, 降低偏差
    ├── Stacking：多层模型堆叠, 元学习器
    └── Blending：简化版Stacking, 防止过拟合
```

### 2.2 优化理论与损失函数

```
知识点清单：
├── 梯度下降
│   ├── 批量梯度下降(BGD)：全量数据, 稳定但慢
│   ├── 随机梯度下降(SGD)：单样本, 噪声大但快
│   ├── 小批量梯度下降：折中方案, GPU友好
│   ├── Momentum：动量, 加速收敛, 减少震荡
│   ├── Nesterov Accelerated Gradient(NAG)：前瞻更新
│   ├── Adagrad：自适应学习率, 稀疏特征友好
│   ├── RMSprop：解决Adagrad学习率衰减过快
│   ├── Adam：Momentum + RMSprop, 最常用
│   ├── AdamW：权重衰减解耦, LLM训练标准
│   └── 学习率调度：Step/Cosine/Warmup/ReduceLROnPlateau
│
├── 损失函数
│   ├── 回归：MSE, MAE, Huber, Log-Cosh, Quantile Loss
│   ├── 二分类：Binary Cross-Entropy, Hinge Loss, Focal Loss
│   ├── 多分类：Categorical Cross-Entropy, Sparse CCE
│   ├── 对比学习：Triplet Loss, InfoNCE, SimCLR Loss
│   ├── 语言模型：Cross-Entropy(下一Token预测), Perplexity
│   └── 强化学习：Policy Gradient, PPO Loss, DPO Loss
│
└── 优化技巧
    ├── 梯度裁剪：防止梯度爆炸, 最大范数约束
    ├── 权重初始化：Xavier/Glorot, Kaiming/He, 正交初始化
    ├── Batch Normalization：训练/推理模式差异, 小batch问题
    ├── Layer Normalization：Transformer标准, 不依赖batch
    ├── 混合精度训练：FP16+FP32, 动态loss scaling
    └── 梯度累积：模拟大batch, 小显存训练大模型
```

---

## 第三阶段：深度学习与NLP篇（Lv.1 → Lv.2）

### 3.1 神经网络基础

```
知识点清单：
├── 前馈神经网络
│   ├── 感知机：线性分类, XOR问题, 多层解决非线性
│   ├── 激活函数：ReLU, GELU, Swish/SiLU, tanh, sigmoid, LeakyReLU
│   ├── 反向传播：链式法则, 计算图, 自动微分
│   ├── 全连接层：参数计算, Flatten, 表示能力
│   └── Dropout：训练时随机丢弃, 推理时缩放补偿
│
├── 卷积神经网络(CNN)
│   ├── 卷积层：kernel, stride, padding, dilation, 感受野
│   ├── 池化层：MaxPool, AvgPool, Global Pooling
│   ├── 经典架构：LeNet → AlexNet → VGG → ResNet → EfficientNet
│   ├── 残差连接：跳连接, 恒等映射, 解决梯度消失
│   └── AI应用：Embedding增强, 多模态视觉编码器
│
├── 循环神经网络(RNN)
│   ├── 基础RNN：隐藏状态, 时间展开, BPTT(随时间反向传播)
│   ├── LSTM：遗忘门, 输入门, 输出门, 细胞状态
│   ├── GRU：更新门, 重置门, 简化LSTM
│   ├── 双向RNN：Bi-LSTM, 前后文信息融合
│   └── Seq2Seq：Encoder-Decoder, Teacher Forcing
│
└── 深度学习框架
    ├── PyTorch
    │   ├── 张量操作：torch.Tensor, device管理, GPU加速
    │   ├── 自动微分：autograd, backward(), 计算图构建与释放
    │   ├── nn.Module：模型定义, forward方法, 参数管理
    │   ├── 数据加载：Dataset, DataLoader, 自定义collate_fn
    │   ├── 训练循环：optimizer.zero_grad → loss.backward → optimizer.step
    │   ├── 模型保存：state_dict vs 完整模型, checkpoint, jit.script
    │   ├── 分布式：DataParallel, DistributedDataParallel(DDP)
    │   └── 混合精度：torch.cuda.amp, autocast, GradScaler
    │
    └── PyTorch Lightning(可选)
        ├── 模块化训练, 自动日志, 多GPU支持
        └── 回调系统, 早停, checkpoint自动保存
```

### 3.2 自然语言处理(NLP)

```
知识点清单：
├── 文本预处理
│   ├── 分词
│   │   ├── 英文：空格分词, 词干提取(stemming), 词形还原(lemmatization)
│   │   ├── 中文：jieba分词(精确/全/搜索模式), pkuseg, HanLP
│   │   └── 子词分词：BPE, WordPiece, Unigram, SentencePiece
│   ├── 文本清洗
│   │   ├── 正则表达式：URL/邮箱/HTML标签去除
│   │   ├── Unicode规范：NFKC/NFKD标准化, 全角半角转换
│   │   ├── 大小写：lowercase, truecase(上下文恢复)
│   │   └── 特殊处理：emoji, 数字, 标点
│   ├── 停用词处理：中文停用词表, 去留决策
│   └── 文本增强：回译, 同义词替换, 随机删除/交换
│
├── Tokenization深入
│   ├── BPE(Byte-Pair Encoding)
│   │   ├── 原理：迭代合并最频繁的字节对
│   │   ├── 训练：构建vocab, merge规则
│   │   └── 优势：处理OOV(未登录词), 子词共享
│   ├── WordPiece
│   │   ├── 与BPE区别：基于似然而非频率选择合并
│   │   └── ##前缀标记：区分词首和词内子词
│   ├── Unigram Language Model
│   │   ├── 基于概率的子词选择
│   │   └── 可逆性：完全可逆的编码-解码
│   ├── SentencePiece
│   │   ├── 语言无关：直接在原始文本上训练
│   │   └── 空格处理：将空格视为普通字符
│   └── 实战工具
│       ├── tiktoken：OpenAI Tokenizer, 精确Token计数
│       ├── HuggingFace Tokenizers：高速BPE实现
│       └── 自定义Tokenizer训练：特定领域优化
│
├── 词向量(Word Embeddings)
│   ├── 静态词向量
│   │   ├── Word2Vec：CBOW, Skip-gram, 负采样, 层次Softmax
│   │   ├── GloVe：全局词共现矩阵, 加权最小二乘
│   │   ├── FastText：子词n-gram, 字符级信息
│   │   └── 评估：相似度, 类比推理(king-man+woman≈queen)
│   ├── 上下文词向量
│   │   ├── ELMo：双向LSTM, 深层特征组合
│   │   ├── BERT Embedding：多层融合, 动态表示
│   │   └── GPT Embedding：单向生成, 最后层输出
│   ├── 句子向量(Sentence Embeddings)
│   │   ├── 平均池化：简单但有效
│   │   ├── Sentence-BERT：孪生网络, 对比学习
│   │   ├── SimCSE：简单对比学习, Dropout增强
│   │   └── 现代Embedding模型：BGE, E5, Instructor, GTE
│   └── Embedding应用
│       ├── 语义搜索：余弦相似度, 近似最近邻搜索
│       ├── 聚类：话题发现, 文档组织
│       ├── 分类：Embedding作为特征
│       └── 去重：相似文本检测
│
├── 经典NLP任务
│   ├── 文本分类
│   │   ├── 特征提取：TF-IDF, CountVectorizer, N-gram
│   │   ├── 传统模型：朴素贝叶斯, SVM, FastText
│   │   └── 深度学习：TextCNN, RNN+Attention, BERT微调
│   ├── 命名实体识别(NER)
│   │   ├── 标注方案：BIO, BIOES
│   │   ├── 序列标注：CRF, BiLSTM-CRF, BERT+CRF
│   │   └── 实体类型：人名/地名/组织/时间/数量...
│   ├── 关系抽取：远程监督, 实体关系三元组
│   ├── 文本摘要：抽取式(TextRank), 生成式(Seq2Seq)
│   ├── 机器翻译：统计MT → 神经MT → Transformer MT
│   ├── 问答系统：抽取式QA, 生成式QA, 阅读理解
│   └── 对话系统：任务型(意图+槽位), 闲聊型, 知识型
│
└── NLP评估指标
    ├── 分类：Accuracy, Precision, Recall, F1, AUC-ROC
    ├── 生成：BLEU, ROUGE, METEOR, BERTScore
    ├── 翻译：chrF, COMET(神经网络评估)
    └── 人工评估：有用性, 流畅度, 忠实度, 一致性
```

### 3.3 Transformer架构深度

```
知识点清单：
├── Attention机制
│   ├── Scaled Dot-Product Attention
│   │   ├── 公式：softmax(QK^T/√d_k)V
│   │   ├── Q/K/V含义：Query查什么, Key有什么, Value实际内容
│   │   ├── √d_k缩放：防止大维度导致softmax梯度消失
│   │   └── 计算复杂度：O(n²d) — 序列长度二次方！
│   ├── Multi-Head Attention
│   │   ├── 多头原理：多个低维注意力并行, 关注不同语义关系
│   │   ├── 头数选择：8/16/32/64, 与d_model的关系
│   │   └── 输出拼接：Concat + Linear投影
│   ├── Self-Attention vs Cross-Attention
│   │   ├── Self-Attention：Q=K=V=同源输入
│   │   └── Cross-Attention：Q来自Decoder, K=V来自Encoder
│   └── 高效Attention变体
│       ├── Sparse Attention：局部窗口, 全局token, 带状稀疏
│       ├── Flash Attention：IO感知, 分块计算, 不存储完整矩阵
│       ├── Flash Attention 2：进一步优化, 并行序列维度
│       ├── PagedAttention(vLLM)：KV Cache分页管理
│       ├── Multi-Query Attention(MQA)：所有头共享K和V
│       ├── Grouped-Query Attention(GQA)：分组共享K和V
│       └── Sliding Window Attention：滑动窗口(Mistral)
│
├── Position Encoding(位置编码)
│   ├── 绝对位置编码
│   │   ├── Sinusoidal：正弦/余弦函数, 可外推(一定范围)
│   │   ├── Learned：可学习位置Embedding(GPT使用)
│   │   └── 局限性：无法处理超长序列
│   ├── 相对位置编码
│   │   ├── Relative Position Bias：T5中的偏置矩阵
│   │   └── ALiBi：Attention with Linear Biases
│   ├── 旋转位置编码(RoPE)
│   │   ├── 原理：复数旋转, 相对位置隐含在点积中
│   │   ├── 优势：外推性好, 相对位置自然编码
│   │   ├── LLaMA/Qwen/Mistral等主流模型使用
│   │   └── 扩展：NTK-aware插值, YaRN(PI+NTK)
│   └── 位置插值
│       ├── Linear PI：线性缩放位置 → 4K扩展到32K
│       ├── NTK-aware：不同维度不同缩放因子
│       └── Dynamic NTK：推理时动态调整
│
├── Feed-Forward Network(FFN)
│   ├── 标准FFN：Linear → ReLU/GELU → Linear
│   ├── SwiGLU/GeGLU：门控激活, LLaMA/PaLM使用
│   ├── FFN维度：通常d_ff = 4 × d_model
│   ├── FFN存储知识：模型2/3的参数在FFN中
│   └── MoE(Mixture of Experts)：稀疏FFN, 条件计算
│
├── Layer Normalization
│   ├── Post-Norm：原始Transformer, 残差后归一化
│   ├── Pre-Norm：归一化在前, GPT/LLaMA使用, 训练更稳定
│   ├── RMSNorm：去掉均值中心化, 计算更高效
│   └── DeepNorm：极深网络专用(1000层+)
│
├── Encoder(编码器)架构
│   ├── 双向Attention：看到所有位置
│   ├── BERT：MLM预训练, [CLS]表示, 12/24层
│   │   ├── BERT-base(110M), BERT-large(340M)
│   │   ├── 预训练任务：MLM(15%), NSP
│   │   └── 微调策略：分类/QA/NER
│   ├── RoBERTa：更大数据, 动态Mask, 无NSP
│   ├── DeBERTa：解耦Attention, 增强Mask解码器
│   └── 现代Encoder模型：BERT依然是Embedding主力
│
├── Decoder(解码器)架构
│   ├── Causal Self-Attention：只能看到之前的Token
│   ├── Causal Mask：下三角矩阵, 保证自回归性
│   ├── GPT系列演进
│   │   ├── GPT-1(117M)：预训练+微调范式
│   │   ├── GPT-2(1.5B)：Zero-shot能力, Prompt预测
│   │   ├── GPT-3(175B)：In-Context Learning, Few-shot
│   │   ├── GPT-3.5/InstructGPT：RLHF对齐
│   │   └── GPT-4：多模态, 更长上下文, 更强推理
│   └── 开源Decoder
│       ├── LLaMA系列：RoPE, SwiGLU, RMSNorm, Pre-Norm
│       ├── Qwen系列：中文优化, 多尺寸
│       ├── Mistral/Mixtral：滑动窗口, MoE
│       └── DeepSeek系列：MLA(多头潜在Attention)
│
└── Encoder-Decoder架构
    ├── 原始Transformer：机器翻译
    ├── T5：Text-to-Text统一框架
    ├── BART：去噪自编码器
    └── 现代应用：翻译/摘要/Seq2Seq生成
```

---

## 第四阶段：大模型应用篇（Lv.2 → Lv.3）

### 4.1 HuggingFace生态系统

```
知识点清单：
├── Transformers库
│   ├── 模型加载
│   │   ├── AutoModel系列：Tokenizer, Model, Config自动匹配
│   │   ├── from_pretrained：本地加载, Hub下载, 缓存管理
│   │   ├── save_pretrained：保存模型, push_to_hub发布
│   │   └── 设备管理：device_map="auto", CPU Offloading
│   ├── 模型推理
│   │   ├── forward方法：直接调用(需手动处理)
│   │   ├── generate方法：自回归生成, 多种解码策略
│   │   │   ├── Greedy Search：每次选最大概率(确定性)
│   │   │   ├── Beam Search：保留K条路径(翻译/摘要)
│   │   │   ├── Sampling：按概率采样(temperature控制)
│   │   │   ├── Top-K Sampling：只在Top-K中采样
│   │   │   ├── Top-P(Nucleus) Sampling：累积概率阈值
│   │   │   ├── Repetition Penalty：减少重复
│   │   │   └── Constrained Beam Search：约束生成
│   │   └── Pipeline：一行代码完成任务(prototype用)
│   ├── Tokenizer API
│   │   ├── encode/decode：文本↔Token IDs
│   │   ├── __call__：批量编码(padding/truncation/return_tensors)
│   │   ├── apply_chat_template：对话模板格式化
│   │   ├── 特殊Token：bos/eos/pad/unk/sep/cls/mask
│   │   ├── 快速分词器：tokenizers库后端(Rust实现)
│   │   └── 批处理：batched=True, pad_to_max_length
│   └── 模型配置
│       ├── Config类：模型超参数集中管理
│       ├── 修改配置：隐藏层大小, 头数, 词汇表大小
│       └── Trust Remote Code：自定义模型加载
│
├── Datasets库
│   ├── 数据加载
│   │   ├── load_dataset：Hub/本地/内存多种来源
│   │   ├── 格式支持：JSON/CSV/Parquet/Arrow/SQL/WebDataset
│   │   └── Streaming模式：流式处理TB级数据
│   ├── 数据处理
│   │   ├── map：批量应用函数, 多进程并行(num_proc)
│   │   ├── filter：按条件过滤数据
│   │   ├── select/shuffle/sort：数据子集选择
│   │   └── train_test_split：数据集划分
│   ├── 数据格式
│   │   ├── Arrow格式：列式存储, 内存映射, 零拷贝
│   │   └── 类型系统：Value, ClassLabel, Sequence, Image, Audio
│   └── 数据推送：push_to_hub发布自己的数据集
│
├── 模型Hub
│   ├── 模型搜索：按任务/语言/框架/许可过滤
│   ├── 模型卡(Model Card)：阅读和理解模型信息
│   │   ├── 模型描述, 使用限制, 许可证
│   │   ├── 训练数据, 评估结果, 偏差说明
│   │   └── 推荐参数, 使用示例
│   ├── 下载管理：snapshot_download, hf_hub_download
│   ├── 国内镜像：hf-mirror.com, modelscope
│   └── 模型发布：push_to_hub, 自动化CI部署
│
├── Accelerate库
│   ├── 分布式训练：多GPU/TPU/节点, 自动设备放置
│   ├── 混合精度：自动FP16/BF16
│   ├── 梯度累积：小batch模拟大batch
│   └── 零代码改动：notebook_launcher, accelerate launch
│
├── PEFT库
│   ├── LoRA：低秩适配器, 核心参数配置
│   ├── Prefix Tuning：前缀虚拟Token
│   ├── Prompt Tuning：软提示
│   ├── AdaLoRA：自适应秩分配
│   └── IA3：极少参数微调
│
└── 其他生态工具
    ├── TRL：RLHF训练(PPO/DPO), SFT训练器
    ├── Diffusers：图像生成(Stable Diffusion等)
    ├── Evaluate：标准评估指标
    ├── Gradio：快速Demo搭建
    ├── Text-Generation-Inference(TGI)：生产级推理服务
    └── Optimum：硬件优化推理(ONNX/OpenVINO/Neuron)
```

### 4.2 大模型API应用

```
知识点清单：
├── 主流API Provider
│   ├── OpenAI
│   │   ├── 模型：GPT-4o, GPT-4 Turbo, GPT-4o-mini, GPT-3.5 Turbo
│   │   ├── API类型：Chat Completions, Embeddings, Audio, Images
│   │   ├── 特殊功能：JSON Mode, Function Calling, Vision, Streaming
│   │   └── SDK：openai Python库, 异步支持
│   ├── Anthropic
│   │   ├── 模型：Claude Opus, Sonnet, Haiku
│   │   ├── 特色：超长上下文, Prompt Caching, Tool Use
│   │   └── 消息格式：system/messages/user/assistant角色
│   ├── Google
│   │   ├── 模型：Gemini 1.5 Pro/Flash, 原生多模态
│   │   ├── 特色：超长上下文(1M+), 视频理解
│   │   └── SDK：google-generativeai
│   ├── 国产API
│   │   ├── 通义千问(阿里)：qwen-max/plus/turbo, 中文强
│   │   ├── 文心一言(百度)：ERNIE系列, 国内合规
│   │   ├── DeepSeek：价格极低, 性能强
│   │   ├── 智谱(ChatGLM)：开源+API双模式
│   │   ├── 月之暗面(Kimi)：超长上下文
│   │   ├── 字节豆包(Doubao)：多模态, 低价
│   │   └── 百川(Baichuan)：中文优化
│   └── 开源兼容API
│       ├── vLLM：OpenAI兼容服务
│       ├── Ollama：本地运行, REST API
│       ├── LocalAI：OpenAI兼容, 本地推理
│       └── LiteLLM：多Provider统一接口
│
├── API调用基础
│   ├── 请求构造
│   │   ├── messages：system/user/assistant/tool角色
│   │   ├── 参数：temperature, top_p, max_tokens, stop, seed
│   │   ├── 多轮对话：维护messages数组, 上下文窗口管理
│   │   └── 并发控制：Rate Limit, 连接池, 信号量
│   ├── 响应处理
│   │   ├── choices：message.content, finish_reason
│   │   ├── usage：prompt_tokens, completion_tokens, total_tokens
│   │   └── 错误处理：401(Auth), 429(Rate), 500(Server), 超时
│   ├── 流式响应
│   │   ├── 服务端：stream=True, SSE格式/WebSocket
│   │   ├── 客户端：ReadableStream, 逐token更新UI
│   │   └── 中断生成：AbortController, 取消请求
│   └── 函数调用(Function Calling)
│       ├── 工具定义：JSON Schema, 描述, 参数
│       ├── tool_choice：auto/none/required/指定工具
│       ├── 并行调用：parallel_tool_calls
│       └── 结果回传：tool_call_id匹配
│
├── Token管理
│   ├── Token计数
│   │   ├── tiktoken：OpenAI官方Token计数器
│   │   ├── 不同模型不同Tokenizer：必须匹配
│   │   ├── 中英文Token差异：中文1字≈2-3Token
│   │   └── 特殊格式开销：JSON结构, Markdown标记
│   ├── 上下文窗口
│   │   ├── 窗口限制：4K/8K/32K/128K/1M
│   │   ├── 窗口管理：滑动窗口, 摘要压缩, Token预算
│   │   └── 超限处理：truncation策略, 分段处理
│   └── 成本估算
│       ├── 定价模式：按输入/输出Token分别计费
│       ├── 成本计算器：实时追踪, 预警
│       └── 优化策略：缓存, 模型分级, Prompt精简
│
├── API Key管理
│   ├── 本地开发：.env文件, python-dotenv
│   ├── 生产环境：环境变量, Secret Manager, Vault
│   ├── 安全实践：最小权限, 定期轮换, 使用审计
│   └── Key泄露应急：立即吊销, 检查使用记录
│
└── 多Provider统一层
    ├── 抽象接口：统一chat/completions/embeddings
    ├── 模型路由：按任务/成本/延迟选择最佳模型
    ├── 故障切换：主备切换, 降级策略
    └── 负载均衡：多个API Key轮换, Rate Limit分散
```

### 4.3 Prompt Engineering体系

```
知识点清单：
├── 基础技术
│   ├── Zero-shot Prompting
│   │   ├── 清晰指令：任务描述, 输出格式, 约束条件
│   │   ├── 角色设定：System Prompt, Persona定义
│   │   └── 分步指令：编号步骤, 明确检查点
│   ├── Few-shot Prompting
│   │   ├── 示例选择：覆盖多样性, 边界case
│   │   ├── 示例数量：3-5个最佳(质量>数量)
│   │   ├── 示例排序：最后一个影响最大(近因效应)
│   │   ├── 动态示例选择：基于Embedding相似度
│   │   └── 示例格式：严格一致性
│   └── Instruction Tuning感知
│       ├── 模型训练格式：了解模型在什么格式上训练
│       └── Chat Template：apply_chat_template的重要性
│
├── 推理增强技术
│   ├── Chain-of-Thought(CoT)
│   │   ├── Zero-shot CoT："Let's think step by step"
│   │   ├── Few-shot CoT：带推理过程的示例
│   │   ├── 自动CoT：Auto-CoT, 聚类选示例
│   │   └── 适用场景：数学/逻辑/多步推理
│   ├── Self-Consistency
│   │   ├── 多次采样：高温+多次运行
│   │   ├── 多数投票：取最一致的答案
│   │   └── 复杂度投票：选推理路径最完整的
│   ├── Tree-of-Thought(ToT)
│   │   ├── 多路径探索：生成多个候选思路
│   │   ├── 评估与剪枝：淘汰差的, 保留好的
│   │   └── BFS/DFS搜索：广度优先, 深度优先
│   ├── Graph-of-Thought(GoT)
│   │   ├── 图结构推理：合并/分支/循环
│   │   └── 复杂问题分解
│   └── ReAct(Reasoning + Acting)
│       ├── Thought-Action-Observation循环
│       ├── 工具调用决策
│       └── Agent实现基础
│
├── 高级技术
│   ├── In-Context Learning
│   │   ├── 上下文窗口内的学习
│   │   ├── 检索增强ICL：动态检索相关示例
│   │   └── 结构化ICL：表格格式示例
│   ├── Constitutional AI
│   │   ├── 原则约束：定义AI行为准则
│   │   └── 自我批评：模型审查自己的输出
│   ├── Meta-Prompting
│   │   ├── 用LLM生成Prompt
│   │   ├── Prompt优化迭代
│   │   └── 自动Prompt工程(APE)
│   ├── DSPy
│   │   ├── 声明式Prompt编程
│   │   ├── 自动Prompt优化(编译器)
│   │   └── 模块化：ChainOfThought, ReAct模块
│   └── Prompt Chaining
│       ├── 串行链：A→B→C, 逐步细化
│       ├── 并行链：A+B+C同时, 汇总
│       └── 条件链：if-else路由
│
├── Prompt架构模式
│   ├── CRISPE框架
│   │   └── Capacity-Role, Request, Insight, Statement, Personality, Experiment
│   ├── RACE框架
│   │   └── Role, Action, Context, Expectation
│   ├── 分层Prompt
│   │   ├── System Prompt：全局规则, 角色, 工具
│   │   ├── Context Prompt：背景知识, 文档, 历史
│   │   └── Task Prompt：具体任务, 格式要求
│   └── 动态Prompt
│       ├── 模板引擎：Jinja2, f-string
│       ├── 条件渲染：根据上下文选择Prompt片段
│       └── 运行时注入：实时数据, 检索结果
│
├── 安全与防护
│   ├── Prompt注入防御
│   │   ├── 输入过滤：检测注入模式
│   │   ├── 输出审查：敏感信息脱敏
│   │   ├── 角色强化：系统指令优先级>用户指令
│   │   └── 沙箱隔离：危险操作需确认
│   ├── 越狱(Jailbreak)防御
│   │   ├── 常见攻击：DAN, 角色扮演, 编码绕过
│   │   ├── 多层防护：输入→模型→输出
│   │   └── 持续更新：新型攻击模式跟踪
│   └── 内容安全
│       ├── 有害内容过滤
│       ├── 偏见检测
│       └── 合规检查
│
└── Prompt工程化
    ├── 版本管理：Git管理Prompt, 版本号约定
    ├── A/B测试：对比实验, 统计显著性
    ├── 评估体系：LLM-as-Judge, 人工评估, 自动化测试
    ├── Prompt注册表：集中管理, 灰度发布, 回滚
    └── 监控分析：效果追踪, 成本监控, 异常告警
```

---

## 第五阶段：AI工程架构篇（Lv.3 → Lv.4）

### 5.1 RAG(检索增强生成)

```
知识点清单：
├── RAG架构设计
│   ├── Naive RAG：检索→拼接→生成
│   ├── Advanced RAG：检索前/后优化
│   │   ├── 查询重写：Query2doc, HyDE(Hypothetical Document Embeddings)
│   │   ├── 多步检索：Iterative RAG, 逐步细化
│   │   ├── 多路召回：向量+关键词+结构化
│   │   └── Self-RAG：自我反思, 选择性检索
│   └── Modular RAG
│       ├── 可插拔组件：检索器/重排序器/生成器随意替换
│       ├── 流程编排：顺序/分支/循环
│       └── 适配性：不同场景不同Pipeline
│
├── 文档处理管线
│   ├── 文档加载
│   │   ├── 格式支持：PDF, Word, Markdown, HTML, CSV, JSON
│   │   ├── 结构感知：标题层级, 表格, 列表, 代码块
│   │   ├── 图片处理：OCR提取文字, Vision模型直接理解
│   │   └── 元数据提取：来源, 作者, 日期, 页码
│   ├── 文本分割(Chunking)
│   │   ├── 固定大小分割：按字符数/Token数
│   │   ├── 语义分割：按段落/句子/语义边界
│   │   ├── 结构分割：按标题/章节(Markdown感知)
│   │   ├── 重叠策略：10-20%重叠防信息断裂
│   │   ├── 小2大检索：小chunk检索→大chunk返回
│   │   └── 父子文档：检索子文档→返回父文档
│   └── 数据增强
│       ├── 元数据丰富：添加标题层级, 文档摘要
│       ├── 假设问题生成：为每个chunk生成可能的问题
│       └── 摘要生成：多级摘要(段落/页面/文档)
│
├── 检索系统
│   ├── 稀疏检索(BM25/TF-IDF)
│   │   ├── 关键词匹配：专有名词, 数字, 代码
│   │   └── 倒排索引：高效, 可解释
│   ├── 稠密检索(向量)
│   │   ├── Embedding模型选择：维度/领域/多语言
│   │   ├── ANN算法：HNSW, IVF, PQ
│   │   └── 向量数据库：实际存储和检索
│   ├── 混合检索
│   │   ├── 融合策略：RRF(Reciprocal Rank Fusion), 加权和
│   │   └── 互补性：语义理解+精确匹配
│   ├── 多阶段检索
│   │   ├── 粗排(Retrieval)：快速召回Top-N(N=50-200)
│   │   ├── 精排(Reranking)：Cross-Encoder重排序Top-K
│   │   └── 压缩(Compression)：LLM压缩/提取相关内容
│   └── 检索优化
│       ├── 查询扩展：同义词, 相关词, LLM生成变体
│       ├── 查询分解：复杂问题拆成子问题
│       └── 查询路由：不同问题走不同检索策略
│
├── 重排序(Reranking)
│   ├── Cross-Encoder：query+doc联合编码, 精度高
│   ├── Bi-Encoder重排：更快的近似方案
│   ├── LLM重排：用LLM判断相关性
│   ├── 多向量重排：ColBERT后期交互
│   └── 模型选择：BGE-Reranker, Cohere Rerank, RankLLM
│
├── 生成增强
│   ├── 上下文构建
│   │   ├── Prompt模板：文档+问题+指令的标准格式
│   │   ├── 引用标注：[来源X]格式, 可追溯
│   │   ├── 置信度：不确定时说明不确定
│   │   └── 文档去重：相似文档只保留一个
│   ├── 幻觉缓解
│   │   ├── 强制引用：要求每句话标注来源
│   │   ├── 自我检查：生成后自我审查
│   │   ├── NLI验证：用自然语言推理验证
│   │   └── 多轮确认：不确定时反问用户
│   └── 流式RAG
│       ├── 检索延迟优化：与生成并行
│       └── 增量检索：生成过程中继续检索
│
└── RAG评估
    ├── 检索指标：Recall@K, MRR, NDCG, Hit Rate
    ├── 生成指标：Faithfulness, Answer Relevance, Context Relevance
    ├── 端到端指标：用户满意度, 任务完成率
    └── 评估框架：RAGAS, TruLens, LangSmith
```

### 5.2 向量数据库

```
知识点清单：
├── 向量数据库选型
│   ├── 轻量级
│   │   ├── Chroma：Python原生, 零配置, 适合原型
│   │   ├── LanceDB：列式存储, 无需服务端
│   │   └── FAISS(Meta)：纯检索引擎, 不持久化
│   ├── 分布式
│   │   ├── Milvus：云原生, 十亿级, GPU加速
│   │   ├── Qdrant：Rust实现, 高性能, 过滤强
│   │   ├── Weaviate：GraphQL接口, 多模态
│   │   └── Elasticsearch：传统搜索+向量扩展
│   ├── 云服务
│   │   ├── Pinecone：全托管, 免运维
│   │   ├── Zilliz Cloud：Milvus托管版
│   │   ├── 阿里DashVector：国内可用
│   │   └── 腾讯VectorDB：国内可用
│   └── PG扩展
│       └── PGVector：PostgreSQL向量插件, 复用现有DB
│
├── 索引算法
│   ├── 精确检索：暴力搜索(O(n)), 小数据集
│   ├── 近似检索(ANN)
│   │   ├── 基于树：Annoy(Spotify), 随机投影森林
│   │   ├── 基于图：HNSW, NSG, DiskANN
│   │   ├── 基于量化：IVF, PQ(乘积量化), SQ(标量量化)
│   │   ├── 基于哈希：LSH(局部敏感哈希)
│   │   └── 混合索引：IVF+PQ, IVF+HNSW
│   └── GPU加速：RAFT, Faiss GPU, cuVS
│
├── 相似度度量
│   ├── 欧氏距离(L2)：√Σ(xi-yi)²
│   ├── 内积(IP)：Σxi·yi, 归一化后=余弦
│   ├── 余弦相似度：归一化内积, 方向敏感
│   ├── 汉明距离：二值向量
│   └── 选择指南：归一化向量→IP/余弦, 量级重要→L2
│
├── 数据管理
│   ├── CRUD操作：插入/查询/更新/删除
│   ├── 批量导入：分批处理, 并行加速
│   ├── 增量更新：变化检测, 增量索引
│   ├── 分区策略：按时间/类别/来源分区
│   ├── 数据一致性：最终一致性, 强一致性
│   └── 备份恢复：快照, 增量备份
│
└── 性能优化
    ├── 索引参数调优：nlist, nprobe, M, efConstruction
    ├── 查询批处理：合并多个查询一次执行
    ├── 连接池：复用数据库连接
    ├── 冷热分离：热数据内存, 冷数据磁盘
    └── 监控指标：QPS, 延迟(P50/P95/P99), 召回率
```

### 5.3 AI Agent架构

```
知识点清单：
├── Agent核心架构
│   ├── 基础组件
│   │   ├── LLM(Brain)：推理, 规划, 决策
│   │   ├── Tools(Hands)：执行具体操作
│   │   ├── Memory(Notebook)：短期/长期记忆
│   │   └── Planner(Strategy)：任务分解与调度
│   ├── 控制流程
│   │   ├── ReAct循环：Think→Act→Observe→Think...
│   │   ├── Plan-Execute：先规划再执行
│   │   ├── Plan-Execute-Reflect：增加了反思步骤
│   │   └── Observe-Plan-Act：先观察环境再行动
│   └── 终止条件
│       ├── 任务完成判断, 最大步数限制
│       ├── 置信度阈值, 死循环检测
│       └── 人工干预触发
│
├── 工具系统设计
│   ├── 工具定义
│   │   ├── Schema设计：name, description(最重要!), parameters
│   │   ├── 参数类型：string/number/boolean/enum/object/array
│   │   └── 参数约束：required, default, enum, 正则
│   ├── 工具类型
│   │   ├── 搜索类：网页搜索, 知识库搜索, 代码搜索
│   │   ├── 计算类：数学计算, 代码执行, 数据分析
│   │   ├── 数据类：数据库查询, API调用, 文件读写
│   │   ├── 通信类：邮件, 消息, 通知
│   │   └── 操作类：创建/修改/删除实体
│   ├── 工具权限
│   │   ├── 只读/读写/管理员分级
│   │   ├── 高风险确认机制
│   │   └── 频率限制, 用量封顶
│   └── 错误处理
│       ├── 工具超时, 重试策略
│       ├── 降级方案(替代工具)
│       └── 错误信息友好化(告诉Agent怎么修)
│
├── 规划与推理
│   ├── 任务分解
│   │   ├── 层次分解：目标→子目标→步骤
│   │   ├── 依赖分析：哪些步骤可以并行
│   │   └── 资源估算：每步需要什么工具
│   ├── 动态规划
│   │   ├── 失败重规划：某步失败后调整后续
│   │   ├── 机会主义执行：发现捷径时调整
│   │   └── 资源感知：根据可用工具调整策略
│   └── 元认知(反思)
│       ├── 进度回顾：已经完成了什么
│       ├── 策略评估：当前方法有效吗
│       └── 方向修正：是否需要改变策略
│
├── 记忆系统
│   ├── 工作记忆(Working)
│   │   ├── 对话上下文：当前会话的messages
│   │   ├── 任务状态：当前步骤, 中间结果
│   │   └── Token管理：滑动窗口, 摘要压缩
│   ├── 短期记忆(Short-term)
│   │   ├── 会话级信息：用户偏好, 本次任务上下文
│   │   └── 实现：键值存储, Python dict/Redis
│   ├── 长期记忆(Long-term)
│   │   ├── 历史任务：过去成功的执行记录
│   │   ├── 学到的规则：什么方法对什么问题有效
│   │   ├── 用户画像：长期偏好, 习惯
│   │   └── 实现：向量数据库+关系数据库
│   └── 记忆操作
│       ├── 记忆检索：语义搜索历史记录
│       ├── 记忆更新：新信息覆盖旧信息
│       ├── 记忆遗忘：不重要信息的衰减
│       └── 记忆整合：总结多个相关记忆
│
├── Multi-Agent系统
│   ├── 协作模式
│   │   ├── 顺序(Sequential)：流水线式传递
│   │   ├── 层级(Hierarchical)：Manager分配→Worker执行
│   │   ├── 辩论(Debate)：多Agent讨论达成共识
│   │   ├── 投票(Voting)：多个Agent独立处理→投票
│   │   └── 市场(Market)：Agent竞价获取子任务
│   ├── 角色设计
│   │   ├── 角色专业化：每个Agent有明确的专长
│   │   ├── 角色互补：覆盖不同的知识/能力
│   │   └── 角色冲突解决：出现矛盾时的仲裁机制
│   ├── 通信协议
│   │   ├── 消息格式：结构化传递(任务+结果+置信度)
│   │   ├── 广播/点对点/组播
│   │   └── 共享黑板(Blackboard)：公共信息池
│   └── 框架工具
│       ├── AutoGen(Microsoft)：多Agent对话框架
│       ├── CrewAI：角色化Agent协作
│       ├── MetaGPT：模拟软件公司SOP
│       └── LangGraph：图结构Agent编排
│
├── Agent安全
│   ├── 输入安全
│   │   ├── Prompt注入检测, 指令过滤
│   │   └── 用户意图验证
│   ├── 过程安全
│   │   ├── 工具使用白名单, 参数校验
│   │   ├── 沙箱执行(代码/命令)
│   │   └── 资源限制(CPU/内存/时间)
│   ├── 输出安全
│   │   ├── 内容审查, 敏感信息脱敏
│   │   └── 人类确认关键操作
│   └── 审计追踪
│       ├── 全链路日志, 操作回放
│       └── 异常行为告警
│
└── Agent框架对比
    ├── LangChain：最流行, 生态丰富, 抽象层次多
    ├── LlamaIndex：RAG专精, 数据连接器多
    ├── Semantic Kernel(Microsoft)：企业集成(C#/Python)
    ├── DSPy：声明式, 自动优化
    ├── Haystack：Pipeline架构, 生产就绪
    └── 自建框架：完全控制, 适合特殊需求
```

### 5.4 后端与API设计

```
知识点清单：
├── FastAPI深度
│   ├── 路由设计：RESTful约定, 版本化, 路径参数/查询参数
│   ├── 请求/响应模型：Pydantic, 嵌套模型, 字段验证
│   ├── 依赖注入：Depends, 数据库会话, 认证, 配置
│   ├── 中间件：CORS, 日志, 限流, 压缩, 请求ID
│   ├── 异常处理：全局handler, 自定义异常, 错误码规范
│   ├── 后台任务：BackgroundTasks, Celery集成
│   ├── WebSocket：实时通信, 连接管理
│   └── 性能优化：异步视图, 连接池, 响应缓存
│
├── 数据库集成
│   ├── ORM：SQLAlchemy声明式模型, 关系映射
│   ├── 迁移：Alembic, 版本控制
│   ├── 连接池：pool_size, max_overflow, 超时配置
│   ├── 查询优化：N+1问题, eager loading, 索引策略
│   └── 异步驱动：asyncpg, databases, SQLAlchemy async
│
├── 缓存策略
│   ├── Redis：缓存, 会话, 分布式锁, 消息队列
│   ├── 多级缓存：L1本地→L2 Redis→L3 数据库
│   ├── 缓存模式：Cache-Aside, Read-Through, Write-Through
│   ├── 缓存失效：TTL, 主动失效, 版本号
│   └── AI特化：Prompt缓存, Embedding缓存, 语义缓存
│
├── 异步任务
│   ├── Celery：分布式任务队列, 定时任务(Beat)
│   ├── ARQ：基于Redis的轻量任务队列
│   ├── Dramatiq：简单可靠的任务队列
│   └── 任务设计：幂等性, 重试, 超时, 进度追踪
│
├── 认证与授权
│   ├── API Key：简单直接, SHA256存储
│   ├── JWT：无状态Token, 过期刷新
│   ├── OAuth2：第三方登录, 授权码流程
│   └── RBAC：角色权限控制, 资源级授权
│
├── 限流与防护
│   ├── 令牌桶/漏桶算法
│   ├── 分布式限流：Redis实现
│   ├── IP/用户/API Key多维度限流
│   └── DDoS防护, WAF
│
└── 可观测性
    ├── 日志：结构化日志(JSON), 级别, 上下文传递
    ├── 指标：Prometheus, 请求量/延迟/错误率
    ├── 追踪：OpenTelemetry, 分布式调用链
    └── 告警：Grafana, 阈值, 趋势, 异常检测
```

### 5.5 全栈AI应用

```
知识点清单：
├── 前端方案
│   ├── Streamlit：数据Dashboard, 快速原型
│   ├── Gradio：模型Demo, HuggingFace集成
│   ├── Chainlit：对话界面, 专为LLM设计
│   ├── Next.js/React：生产级前端, 完全定制
│   └── Vue/Nuxt：同样优秀的替代方案
│
├── Chat UI实现
│   ├── 消息渲染：Markdown, 代码高亮, LaTeX公式
│   ├── 流式输出：SSE/WebSocket/ReadableStream
│   ├── 停止生成：AbortController
│   ├── 消息操作：编辑, 删除, 重新生成, 复制
│   ├── 多模态展示：图片, 音频, 视频, 文件
│   └── 响应式设计：移动端适配
│
├── 状态管理
│   ├── 对话列表：多会话切换
│   ├── 对话历史：持久化, 搜索, 导出
│   ├── 模型选择：切换模型/参数
│   └── 用户设置：主题, 语言, 偏好
│
└── 性能与体验
    ├── 首Token时间(TTFT)优化
    ├── 乐观更新(Optimistic UI)
    ├── 骨架屏/加载动画
    └── 错误边界与降级
```

---

## 第六阶段：进阶专精篇（Lv.4 → Lv.5）

### 6.1 模型微调(Fin-tuning)

```
知识点清单：
├── 微调基础
│   ├── 微调 vs 预训练：只做少量更新 vs 从头训练
│   ├── 微调 vs RAG：改变行为 vs 注入知识
│   ├── 微调 vs Prompt Engineering：训练 vs 提示
│   ├── 微调适用场景
│   │   ├── ✅ 领域风格/格式(法律文书, 医学报告)
│   │   ├── ✅ 特定输出格式(固定JSON Schema)
│   │   ├── ✅ 垂直领域术语(金融, 医药)
│   │   ├── ✅ 小型化(蒸馏/让大模型教小模型)
│   │   └── ❌ 知识更新(RAG更合适)
│   └── 微调前提
│       ├── 高质量数据(质量>数量)
│       ├── GPU资源(QLoRA降低门槛)
│       └── 评估方案(怎么判断微调成功)
│
├── 全量微调(Full Fine-tuning)
│   ├── 原理：所有参数参与梯度更新
│   ├── 计算需求：7B模型≈112GB+显存(含优化器状态)
│   ├── 过拟合风险：小数据集容易过拟合
│   └── 灾难性遗忘：新知识挤掉旧知识
│
├── PEFT(参数高效微调)
│   ├── LoRA(Low-Rank Adaptation)
│   │   ├── 核心思想：权重更新矩阵ΔW是低秩的
│   │   ├── 公式：h = Wx + BAx, B∈R^(d×r), A∈R^(r×k)
│   │   ├── 超参数：
│   │   │   ├── r(秩): 4=简单, 8=标准, 16=复杂, 64=高性能
│   │   │   ├── alpha：缩放因子, 通常=2r
│   │   │   ├── target_modules：q_proj, k_proj, v_proj...
│   │   │   └── dropout：0.05-0.1
│   │   ├── 推理时合并：BA加到W中, 零额外延迟
│   │   ├── 多Adapter：切换不同LoRA适配不同任务
│   │   └── 变体：LoRA+(学习率分离), DoRA(方向分解)
│   ├── QLoRA
│   │   ├── 4-bit量化+LoRA
│   │   ├── NF4数据类型：适配正态分布权重量化
│   │   ├── 双重量化：量化常数也量化
│   │   └── 显存节省：33B模型→12GB显存可训
│   ├── Adapter
│   │   ├── 在层间插入小型网络
│   │   └── 推理有额外开销
│   ├── Prefix Tuning
│   │   ├── 在每层前加可学习的虚拟Token
│   │   └── 参数极少(0.01%)
│   ├── Prompt Tuning
│   │   ├── 只在输入层加软提示
│   │   └── 参数最少(0.001%)
│   ├── IA3
│   │   ├── 三个可学习向量：key, value, FFN
│   │   └── 参数极少但效果不错
│   └── 选型指南
│       ├── 通用场景→QLoRA(性价比最高)
│       ├── 多任务切换→LoRA(支持多Adapter)
│       └── 极致参数效率→IA3/Prompt Tuning
│
├── 指令微调(Instruction Tuning)
│   ├── 数据格式：instruction-input-output
│   ├── 数据质量
│   │   ├── 格式一致性, 示例多样性
│   │   ├── 覆盖长尾场景, 包含负面示例
│   │   └── 人工验证+自动检查
│   ├── 训练技巧
│   │   ├── Loss Masking：只在回复部分计算Loss
│   │   ├── 多任务混合：不同任务的数据混合训练
│   │   └── 数据配比：不同任务数据的比例影响行为
│   └── 评估方法
│       ├── 自动评估：基准测试(MMLU, HumanEval...)
│       ├── LLM-as-Judge：GPT-4评估
│       └── 人工评估：Win Rate, Elo Rating
│
├── RLHF与对齐
│   ├── RLHF(Reinforcement Learning from Human Feedback)
│   │   ├── 阶段1：SFT(监督微调)
│   │   ├── 阶段2：训练奖励模型(Reward Model)
│   │   ├── 阶段3：PPO强化学习优化
│   │   └── 工具：TRL库
│   ├── DPO(Direct Preference Optimization)
│   │   ├── 直接从偏好对学习, 不需要奖励模型
│   │   ├── 比RLHF简单稳定
│   │   └── 变体：IPO, KTO, ORPO
│   └── 对齐目标
│       ├── Helpful(有用), Harmless(无害), Honest(诚实)
│       └── 减少幻觉, 遵循指令, 拒绝有害请求
│
└── 微调实战流程
    ├── 数据准备：收集→清洗→格式化→验证
    ├── 环境搭建：GPU选型, 显存估算, 依赖安装
    ├── 训练监控：Loss曲线, 评估指标, WandB/Tensorboard
    ├── 超参调优：学习率, epoch数, LoRA参数
    ├── 过拟合检测：训练/验证Loss divergence
    ├── 模型保存：合并权重, 推送到Hub
    └── 部署推理：vLLM/TGI服务, 性能测试
```

### 6.2 模型部署与推理优化

```
知识点清单：
├── 推理引擎
│   ├── vLLM
│   │   ├── PagedAttention：KV Cache分页, 近乎零浪费
│   │   ├── Continuous Batching：动态批处理
│   │   ├── 量化支持：AWQ, GPTQ, FP8
│   │   ├── 张量并行：多GPU并行推理
│   │   └── OpenAI兼容API：无缝替换
│   ├── TensorRT-LLM(NVIDIA)
│   │   ├── 图优化：算子融合, 内核自动调优
│   │   ├── In-flight Batching：更激进的批处理
│   │   └── 最佳性能(需要NVIDIA GPU)
│   ├── Text-Generation-Inference(HuggingFace)
│   │   ├── 生产环境推荐
│   │   └── 水冷(Watermarking), 安全过滤
│   ├── Llama.cpp
│   │   ├── CPU推理：GGUF量化格式
│   │   ├── GPU Offloading：部分层放GPU
│   │   └── 移动端：Android/iOS支持
│   ├── Ollama
│   │   ├── 一键部署, Modelfile配置
│   │   └── REST API
│   └── 引擎选型
│       ├── 生产环境→vLLM/TGI/TensorRT-LLM
│       ├── 本地开发→Ollama
│       └── CPU/边缘→Llama.cpp
│
├── 量化技术
│   ├── 训练后量化(PTQ)
│   │   ├── GPTQ：基于OBQ, 逐层量化, 需校准数据
│   │   ├── AWQ：激活感知, 保护重要通道
│   │   ├── GGUF(GGML)：Llama.cpp格式, CPU友好
│   │   └── bitsandbytes：简单易用, QLoRA基础
│   ├── 量化感知训练(QAT)
│   │   └── 训练时模拟量化, 精度最高
│   ├── 量化精度
│   │   ├── FP16/BF16：半精度, 无损
│   │   ├── INT8：8bit, 轻微损失
│   │   ├── INT4：4bit, 明显损失但可接受
│   │   └── INT3/INT2：极限压缩, 损失大
│   └── 选择建议
│       ├── 最佳质量→BF16/FP16
│       ├── 平衡→4bit GPTQ/AWQ
│       └── CPU/边缘→GGUF Q4_K_M
│
├── 服务架构
│   ├── 模型预热：首次请求前预加载模型
│   ├── 负载均衡：多实例, Round-Robin/Least-Connection
│   ├── 弹性伸缩：基于QPS/GPU利用率的自动扩缩
│   ├── 蓝绿部署：零停机更新模型
│   ├── 金丝雀发布：新模型逐步放量
│   └── A/B测试：新老模型流量分流对比
│
├── 性能指标
│   ├── TTFT(首Token延迟)：<500ms优秀, <2s可接受
│   ├── TPOT(每Token输出时间)：10-50ms/token
│   ├── 吞吐量：Requests/s, Tokens/s
│   ├── GPU利用率：SM利用率, 显存带宽利用率
│   ├── 排队延迟：请求在队列中等待的时间
│   └── 端到端延迟：从用户发送到收到完整回答
│
└── 成本优化
    ├── Prompt Caching：Anthropic支持, 重复前缀只算一次
    ├── Speculative Decoding：小模型生成, 大模型验证
    ├── KV Cache复用：相同前缀共享KV Cache
    ├── 模型量化：降低显存需求
    ├── Spot/Preemptible实例：节省60-90%
    ├── 混合模型策略：简单任务用小模型
    └── 批处理优化：凑batch提升GPU利用率
```

### 6.3 多模态AI

```
知识点清单：
├── 视觉理解(Vision)
│   ├── GPT-4V/GPT-4o
│   │   ├── 能力：描述, OCR, 问答, 推理, 对比
│   │   ├── 图像格式：base64编码, URL, 本地文件
│   │   └── detail参数：low(512×512), high(多crop拼接)
│   ├── Claude Vision
│   │   ├── 能力：长文档OCR, 图表理解, 多图对比
│   │   └── 适用：文档处理, 合规审查
│   └── 开源方案
│       ├── LLaVA：视觉指令微调
│       ├── Qwen-VL：中文视觉理解
│       └── CogVLM：深层视觉-语言融合
│
├── 语音处理
│   ├── 语音识别(ASR)
│   │   ├── Whisper(OpenAI)：多语言, 鲁棒性强
│   │   ├── Faster-Whisper：CTranslate2加速, 4x速度
│   │   ├── WhisperX：词级时间戳, 说话人分离
│   │   └── 实时ASR：流式处理, VAD
│   ├── 语音合成(TTS)
│   │   ├── OpenAI TTS：6种音色, 多格式
│   │   ├── ElevenLabs：最自然, 声音克隆
│   │   ├── Edge-TTS：免费, 多语言
│   │   └── ChatTTS/Bark：开源
│   └── 全双工语音对话
│       ├── 打断机制：用户可中途打断AI
│       ├── 端点检测：判断用户是否说完
│       └── 低延迟：<500ms响应
│
├── 视频理解
│   ├── 关键帧提取：均匀采样/场景变化检测
│   ├── 多帧分析：帧序列+时间信息
│   ├── Gemini 1.5：原生视频理解, 1M context
│   └── 视频摘要：长视频压缩为文字
│
├── 图像生成
│   ├── DALL-E 3：文本prompt→图像, 风格控制
│   ├── Stable Diffusion：开源, 社区模型丰富
│   ├── Midjourney：艺术风格强
│   ├── ControlNet：精确控制(姿态/深度/边缘)
│   └── IP-Adapter：图像提示(参考图引导)
│
└── 多模态架构模式
    ├── 早期融合：不同模态在输入层拼接
    ├── 中期融合：各模态独立编码, 中间层交叉Attention
    ├── 后期融合：各自处理完再融合
    └── 统一架构：单一Transformer处理所有模态(GPT-4o)
```

---

## 第七阶段：大神之路（Lv.5）

### 7.1 前沿技术跟踪

```
知识点清单：
├── 模型架构前沿
│   ├── Mamba(状态空间模型)：线性复杂度替代Attention
│   ├── RWKV：RNN+Transformer混合, 线性复杂度
│   ├── Retentive Network(RetNet)：训练并行+推理高效
│   ├── Griffin/ Hawk：门控线性RNN
│   └── MoE(Mixture of Experts)：稀疏激活, 条件计算
│
├── 对齐与安全
│   ├── Constitutional AI(Anthropic)
│   ├── RLHF改进：RRHF, ReST, RAFT
│   ├── Red Teaming：对抗测试方法论
│   ├── Unlearning：遗忘特定知识
│   └── 可解释性(Mechanistic Interpretability)
│
├── Agent前沿
│   ├── 代码Agent：SWE-Agent, Devin, OpenDevin
│   ├── 多模态Agent：视觉+操作(GUI Agent)
│   ├── 长期自主Agent：数小时/数天自主运行
│   └── Agent Swarms：大规模Agent协作
│
└── 评估基准
    ├── MMLU/MMLU-Pro：多任务语言理解
    ├── HumanEval/MBPP：代码生成
    ├── GSM8K/MATH：数学推理
    ├── HellaSwag/ARC：常识推理
    ├── AlpacaEval/MT-Bench：指令跟随
    ├── Needle-in-a-Haystack：长文本检索
    └── SWE-bench：真实GitHub Issue修复
```

### 7.2 论文阅读与复现能力

```
核心能力：
├── 论文搜索：arXiv, Papers With Code, Semantic Scholar
├── 论文阅读方法：三遍法, 关注方法>结果>图表
├── 论文复现：从伪代码到可运行代码
├── 论文批判：找局限, 想改进, 提新问题
└── 论文写作：清晰表达, 严谨实验, 诚实报告
```

### 7.3 开源贡献

```
参与方式：
├── 提Issue：报告Bug, 功能建议
├── 提PR：修Bug, 加Feature, 改文档
├── 模型贡献：HuggingFace发布微调模型
├── 数据集贡献：发布高质量数据集
├── 写博客/教程：知识分享
└── 创建项目：解决实际问题的开源项目
```

---

## 🎯 学习里程碑检查清单

### Lv.1 入门 ✅
- [ ] Python：熟练使用类型提示、装饰器、异步编程
- [ ] NumPy：能写Attention矩阵计算、Batch处理
- [ ] 能调用3个以上Provider的API
- [ ] 会设计Zero-shot/Few-shot/CoT Prompt
- [ ] 能搭建Streamlit AI Demo

### Lv.2 初级 ✅
- [ ] 理解Transformer每层的计算过程
- [ ] 能从零实现Self-Attention
- [ ] 能搭建完整RAG系统(加载→分割→索引→检索→生成)
- [ ] 能使用Chroma/Milvus管理向量数据
- [ ] 能用FastAPI搭建AI API服务

### Lv.3 中级 ✅
- [ ] 能实现ReAct Agent(工具调用+推理循环)
- [ ] 能设计Multi-Agent协作系统
- [ ] 能完成LoRA/QLoRA微调
- [ ] 能用WebSocket实现流式对话
- [ ] 能设计完整的企业级AI应用架构
- [ ] 能处理多模态输入(图+文+音)

### Lv.4 高级 ✅
- [ ] 能用vLLM部署高性能推理服务
- [ ] 能进行专业的性能分析和优化
- [ ] 能建立完整的监控告警体系
- [ ] 能独立交付端到端AI项目
- [ ] 能进行模型量化并评估质量损失
- [ ] 深入理解至少一个模型的源码

### Lv.5 大神 ✅
- [ ] 能阅读并复现最新论文
- [ ] 有至少一个开源项目有社区认可
- [ ] 能改进现有架构提出创新
- [ ] 能指导团队完成AI项目
- [ ] 有技术博客/分享影响力

---

> 📌 **本知识体系持续更新，跟上AI发展速度 = 每天学习+每周实践+每月总结**
