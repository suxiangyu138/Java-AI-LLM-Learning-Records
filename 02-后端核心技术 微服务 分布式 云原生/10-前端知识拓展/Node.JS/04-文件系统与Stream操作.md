# 04 - 文件系统与 Stream 操作

> 🎯 Node.js 最常见的用途之一就是操作文件——读配置、写日志、处理数据。本章覆盖 fs 模块的同步/异步/Promise API、Stream 流水线、文件监听

---

## 目录

1. [fs 模块](#1-fs-模块)
2. [Stream 流操作](#2-stream-流操作)
3. [路径处理](#3-路径处理)

---

## 1. fs 模块

### 1.1 三种 API 风格

| 风格 | 特点 | 使用场景 |
|------|------|---------|
| **同步** `fs.readFileSync` | 阻塞，返回结果 | 启动时读配置文件（仅一次） |
| **回调** `fs.readFile` | 不阻塞，回调接收 | 旧代码兼容 |
| **Promise** `fs.promises.readFile` | 不阻塞，async/await | **推荐：所有新代码** ✅ |

```javascript
import { readFileSync } from 'fs';
import { readFile } from 'fs/promises';  // Promise 版本

// 同步：启动时读配置（仅一次，可接受阻塞）
const config = JSON.parse(readFileSync('config.json', 'utf-8'));

// 异步 Promise：运行时操作（推荐）
const data = await readFile('data.csv', 'utf-8');

// 批量读取
const results = await Promise.all(
    files.map(f => readFile(f, 'utf-8'))
);
```

### 1.2 常用操作速查

```javascript
import { promises as fs } from 'fs';

// 读
await fs.readFile('file.txt', 'utf-8');
await fs.readdir('/path/to/dir');            // 列出目录

// 写
await fs.writeFile('output.json', JSON.stringify(data));
await fs.appendFile('log.txt', 'new line\n');  // 追加

// 文件信息
const stat = await fs.stat('file.txt');
stat.isFile();       // true
stat.isDirectory();  // false
stat.size;           // 字节数

// 路径操作
await fs.mkdir('new-dir', { recursive: true });  // 递归创建
await fs.rm('temp', { recursive: true });        // 递归删除（Node 14.14+）
await fs.rename('old.txt', 'new.txt');
await fs.copyFile('src.txt', 'dest.txt');

// 检查是否存在
import { existsSync } from 'fs';  // 只有同步版本
if (existsSync('config.json')) { /* ... */ }

// 推荐：直接 try-catch 代替 exists
try {
    await fs.access('file.txt');
} catch {
    console.log('文件不存在');
}
```

## 2. Stream 流操作

### 2.1 为什么需要 Stream

```text
没有 Stream：
  readFile → 整个 2GB 文件读入内存 → 💥 OOM

有 Stream：
  读取一小块 → 处理 → 释放 → 读取下一块
  内存占用：固定（约 64KB）
```

### 2.2 四种 Stream 类型

| 类型 | 可读 | 可写 | 示例 |
|------|:---:|:---:|------|
| **Readable** | ✅ | ❌ | `fs.createReadStream()` |
| **Writable** | ❌ | ✅ | `fs.createWriteStream()` |
| **Duplex** | ✅ | ✅ | TCP socket |
| **Transform** | ✅ | ✅ | 压缩/加密（边读边写边转换） |

### 2.3 管道操作

```javascript
import { createReadStream, createWriteStream } from 'fs';
import { pipeline } from 'stream/promises';
import { createGzip } from 'zlib';

// 管道：读 → 处理 → 写
await pipeline(
    createReadStream('input.txt'),     // 1. 读取
    createGzip(),                      // 2. 压缩
    createWriteStream('output.txt.gz') // 3. 写入
);

// Java 等价：
// try (InputStream in = ...;
//      GZIPOutputStream gzip = ...;
//      OutputStream out = ...) {
//     in.transferTo(gzip);
//     gzip.finish();
// }
```

### 2.4 CSV 流处理

```javascript
// 逐行处理大 CSV 文件（内存友好）
import { createReadStream } from 'fs';
import { createInterface } from 'readline';

async function processLargeCSV(path) {
    const stream = createReadStream(path);
    const rl = createInterface({ input: stream });

    let count = 0;
    for await (const line of rl) {
        const [name, price, category] = line.split(',');
        // 逐行处理，内存占用恒定
        count++;
    }
    console.log(`处理了 ${count} 行`);
}
```

## 3. 路径处理

```javascript
import path from 'path';
import { fileURLToPath } from 'url';

// __dirname 的 ESM 替代
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 路径拼接（跨平台安全）
const configPath = path.join(__dirname, 'config', 'app.json');
// Windows: C:\app\config\app.json
// Linux:   /app/config/app.json

// 获取扩展名
path.extname('file.txt');  // '.txt'

// 解析路径
path.resolve('src', 'index.js');  // → /abs/path/src/index.js
```

## 核心要点回顾

- 新代码一律用 `fs/promises` + async/await
- 同步 API 只在启动时用（读配置）
- Stream = 大文件处理利器（内存恒定）
- `pipeline()` 替代 `pipe()`（更好的错误处理）
- `path.join()` 永远比字符串拼接安全（跨平台）

## 参考资料

1. Node.js fs 文档 — nodejs.org/api/fs.html
2. Node.js Stream 文档 — nodejs.org/api/stream.html
