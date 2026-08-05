# GPU编程模型与CUDA
> 软件如何映射到硬件：CUDA/ROCm 的编程范式、内存空间模型、线程层级与同步，以及"三个性能金律"的软件表达。

---

## 📚 目录

1. [CUDA 编程范式](#1-cuda-编程范式)
2. [内存空间模型](#2-内存空间模型)
3. [线程层级与索引](#3-线程层级与索引)
4. [同步与原子操作](#4-同步与原子操作)
5. [三大性能金律的软件表达](#5-三大性能金律的软件表达)
6. [ROCm 与 CUDA 的对照](#6-rocm-与-cuda-的对照)

---

## 1. CUDA 编程范式

### 1.1 基本流程

```c
// ① 分配显存
float *d_a, *d_b;
cudaMalloc(&d_a, N * sizeof(float));
cudaMemcpy(d_a, h_a, N * sizeof(float), cudaMemcpyHostToDevice);

// ② 启动 kernel（<<<网格, 块>>> 语法）
vecAdd<<<blocks, threads>>>(d_a, d_b, d_c);

// ③ 同步并取回结果
cudaDeviceSynchronize();
cudaMemcpy(h_c, d_c, N * sizeof(float), cudaMemcpyDeviceToHost);
```

```c
// kernel 本体：每个线程处理一个元素
__global__ void vecAdd(float *a, float *b, float *c) {
    int i = blockIdx.x * blockDim.x + threadIdx.x;  // 全局线程号
    if (i < N) c[i] = a[i] + b[i];                  // 越界保护
}
```

### 1.2 关键概念：Host / Device / Kernel

| 概念 | 含义 | 特点 |
|------|------|------|
| Host（主机） | CPU 侧 | 控制流、内存管理 |
| Device（设备） | GPU 侧 | 并行执行 kernel |
| Kernel | GPU 上运行的函数 | `__global__` 修饰，由网格启动 |

```
执行模型：Host 串行启动 kernel → GPU 并行执行 → 隐式同步点
数据流：显式拷贝（cudaMemcpy）—— GPU 显存与主机内存分离
```

---

## 2. 内存空间模型

### 2.1 五级内存空间

| 空间 | 作用域 | 生命周期 | 速度 | 用法 |
|------|:---:|:---:|:---:|------|
| 寄存器 | 线程 | kernel | 最快 | 局部变量 |
| 共享内存 | 块 | kernel | TB/s 级 | 块内协作 |
| 全局内存 | 网格 | 程序级 | 数百 TB/s | 主数据区 |
| 常量内存 | 网格（只读） | 程序级 | 广播优化 | 权重/常量表 |
| 纹理/表面 | 网格（只读） | 程序级 | 缓存优化 | 图像/插值 |

### 2.2 变量声明示例

```c
__global__ void example(float *g) {
    float reg = g[threadIdx.x];      // 寄存器：局部变量
    __shared__ float sm[1024];        // 共享内存：块内
    // g 是全局内存参数
    sm[threadIdx.x] = reg;
    __syncthreads();                  // 确保块内写完
    g[threadIdx.x] = sm[blockDim.x - 1 - threadIdx.x];  // 块内反转
}
```

### 2.3 内存模型的三条铁律

```
① 显式：数据在全局内存 ↔ 共享内存之间的搬运是程序员写的
② 分层：能放共享内存就不碰全局，能放寄存器就不碰共享
③ 可见性：跨块通信必须经过全局内存 + 原子/栅栏
```

> 🎯 与 CPU 对照：CPU 程序员祈祷缓存命中；GPU 程序员**亲手指定**数据住在哪层——这就是"显式 > 隐式"的 GPU 哲学。

---

## 3. 线程层级与索引

### 3.1 一维到三维索引

```c
// 线程号推导（一维）
int i = blockIdx.x * blockDim.x + threadIdx.x;

// 二维网格（矩阵场景）—— 用 (x, y) 坐标自然表达
int col = blockIdx.x * blockDim.x + threadIdx.x;  // 列
int row = blockIdx.y * blockDim.y + threadIdx.y;  // 行
int idx = row * width + col;

// 三维：体素/张量场景
```

| 内置变量 | 含义 | 范围 |
|---------|------|------|
| threadIdx | 块内线程号 | 0 ~ blockDim-1 |
| blockIdx | 块在网格中的号 | 0 ~ gridDim-1 |
| blockDim | 块尺寸 | ≤1024 |
| gridDim | 网格尺寸 | 可达百万级 |

### 3.2 块/网格尺寸选择的经验

```
① 块大小：128-256 线程最常见（warp 整数倍：64/128/256）
   32 的倍数 → 无浪费 warp
② 网格大小：≥ SM 数 × 每 SM 驻留块数（保证满载）
③ 大块 vs 小块：
   大块 → 共享内存协作强，但驻留块数少
   小块 → 调度灵活，负载均衡好
```

### 3.3 Kernel 启动的软件栈

```text
CUDA 源码 → NVCC 编译
  → 主机代码（CPU 侧，常规编译）
  → 设备代码（GPU 侧：PTX 中间语言 → SASS 机器码）
  → 运行时库（cudaMalloc/cudaMemcpy/启动器）

2025+ 生态：CUDA 之上还有
  cuBLAS/cuDNN（库）、TensorRT（推理）、Triton（编译器）、
  PyTorch/DeepSeek 等框架（自动调度）—— 应用层无需手写 kernel
```

---

## 4. 同步与原子操作

### 4.1 同步原语

```c
__syncthreads();              // 块内屏障：所有线程到此汇合
__threadfence_block();        // 块内内存栅栏（可见性，非汇合）
atomicAdd(&x, 1);             // L2 原子操作（跨块安全）
```

### 4.2 经典模式：并行规约（Reduction）

```c
// 每块内部分阶段规约，最后原子归并
__global__ void sumReduce(float *g, float *result) {
    __shared__ float sdata[256];
    int i = blockIdx.x * blockDim.x + threadIdx.x;
    sdata[threadIdx.x] = g[i];
    __syncthreads();
    for (int s = 128; s > 0; s >>= 1) {   // 树形规约
        if (threadIdx.x < s)
            sdata[threadIdx.x] += sdata[threadIdx.x + s];
        __syncthreads();
    }
    if (threadIdx.x == 0) atomicAdd(result, sdata[0]);
}
```

```
性能要点（教科书级）：
  ① 每块只发 1 次原子操作（块内树形规约后）
  ② 连续访问共享内存（无 bank 冲突）
  ③ 块数 = 数据量/块大小 —— 覆盖全部数据
```

### 4.3 原子操作的瓶颈

```
原子操作在 L2 串行化：同一地址并发原子 → 吞吐 = L2 原子吞吐
  64 个 SM 同时 atomicAdd 同一变量 → 全部排队！
缓解：分级规约（块内先合并）、分区计数（per-SM 桶）、
  更高级：warp 内 shuffle 指令（__shfl_down）免共享内存
```

---

## 5. 三大性能金律的软件表达

| 金律 | 硬件依据（前几模块） | 软件写法 |
|------|-------------------|---------|
| **合并访问** | L2/HBM 按 128B 段 | 连续下标、无间接索引 |
| **用共享内存** | SM 内 TB/s vs 全局数百 GB/s | 分块（tiling）+ 手动搬运 |
| **保证占用率** | 寄存器/共享内存限制驻留 | `__launch_bounds__`、减寄存器 |

### 5.1 矩阵乘法分块（Tiling 经典）

```c
// 朴素：每个线程直接读全局内存 → 每个元素重复读 A/B 行×列次
// 分块：子块读入共享内存，块内复用
__global__ void matmul_tiled(float *A, float *B, float *C, int N) {
    __shared__ float As[16][16], Bs[16][16];
    int tx = threadIdx.x, ty = threadIdx.y;
    int row = blockIdx.y * 16 + ty, col = blockIdx.x * 16 + tx;
    float sum = 0;
    for (int k = 0; k < N; k += 16) {
        As[ty][tx] = A[row * N + k + tx];
        Bs[ty][tx] = B[(k + ty) * N + col];
        __syncthreads();
        #pragma unroll
        for (int j = 0; j < 16; j++)
            sum += As[ty][j] * Bs[j][tx];
        __syncthreads();
    }
    C[row * N + col] = sum;
}
// 优化轨迹：naive → tiled → bank 优化 → 双缓冲 → Tensor Core（cuBLAS 已做绝）
```

> 💡 现实提醒：手写 kernel 只用于理解原理——生产环境用 cuBLAS/cuDNN（已优化到接近理论峰值），工程价值在**知道为什么库快、何时该换策略**（如 FlashAttention 就是超越 cuDNN 级实现的新算法）。

---

## 6. ROCm 与 CUDA 的对照

### 6.1 术语对照

| CUDA | ROCm / HIP | 说明 |
|------|-----------|------|
| SM | CU（Compute Unit） | 计算单元 |
| warp（32） | wavefront（64→32，RDNA） | 调度单位 |
| `__global__` | `__global__` | HIP 兼容语法 |
| cudaMalloc | hipMalloc | 内存管理 |
| cuBLAS | rocBLAS | BLAS 库 |
| Tensor Core | Matrix Core / WMMA | 矩阵单元 |
| NVCC | hipcc | 编译器 |

### 6.2 HIP 的意义

```
HIP = CUDA 兼容层（AMD 官方）
  写 HIP 代码 → 可编译到 CUDA 或 ROCm（双平台）

现实生态：大模型框架（PyTorch）原生支持 CUDA + ROCm
  2025 状态：AMD MI300/MI350 跑 LLM 生态已基本平滑
  但 CUDA 的第三方库生态（cuDNN 深度集成）仍是护城河
```

> ⚠️ 生态现实：CUDA 的壁垒不在"语法"，而在 20 年积累的优化库与工具链。AMD 追赶的正是这层"软件护城河"——MI350 的 35× 推理提升有一半来自软件栈成熟。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| 编程流程？ | cudaMalloc → 启动 kernel → 同步取回 |
| 内存空间？ | 寄存器/共享/全局/常量/纹理五级，显式管理 |
| 线程索引？ | threadIdx/blockIdx/blockDim 推导全局号 |
| 三大金律？ | 合并访问、共享内存分块、占用率保证 |
| ROCm 是什么？ | HIP 双平台兼容层，生态追赶中 |

**下一模块**：[05-NVIDIA架构演进](05-NVIDIA架构演进.md)　**返回总览**：[00-GPU知识体系总览](00-GPU知识体系总览.md)
