# 08 - Node.js 测试与调试

> 🎯 测试和调试是工程化的基本功。Jest 是 Node.js 的 JUnit，Node.js Inspector 是 JVM 的 debugger。本章覆盖单元测试、HTTP 测试、调试技巧

---

## 目录

1. [Jest 单元测试](#1-jest-单元测试)
2. [HTTP 接口测试](#2-http-接口测试)
3. [调试技巧](#3-调试技巧)

---

## 1. Jest 单元测试

```bash
npm install -D jest @types/jest
```

```javascript
// math.js
export function add(a, b) { return a + b; }
export function divide(a, b) {
    if (b === 0) throw new Error('Division by zero');
    return a / b;
}

// math.test.js
import { add, divide } from './math.js';

describe('Math', () => {
    test('adds two numbers', () => {
        expect(add(1, 2)).toBe(3);
    });

    test('divides two numbers', () => {
        expect(divide(10, 2)).toBe(5);
    });

    test('throws on zero division', () => {
        expect(() => divide(10, 0)).toThrow('Division by zero');
    });
});
```

### 常用断言

| 断言 | 说明 | Java 等价 |
|------|------|---------|
| `expect(x).toBe(y)` | 严格相等 | `assertEquals` |
| `expect(x).toEqual(obj)` | 深度相等 | `assertEquals` (对象) |
| `expect(x).toBeTruthy()` | 真值 | `assertTrue` |
| `expect(fn).toThrow()` | 抛异常 | `assertThrows` |
| `expect(array).toContain(item)` | 包含 | `assertTrue(list.contains)` |
| `expect(promise).resolves.toBe(x)` | Promise 成功 | `future.get()` |
| `expect(promise).rejects.toThrow()` | Promise 失败 | catch 块 |
| `expect(fn).toHaveBeenCalled()` | Mock 被调用 | `verify(mock)` |

### Mock 与 Spy

```javascript
// Mock 函数
const mockFn = jest.fn();
mockFn.mockReturnValue(42);
mockFn('hello');
expect(mockFn).toHaveBeenCalledWith('hello');

// Mock 模块
jest.mock('./db', () => ({
    query: jest.fn().mockResolvedValue([{ id: 1 }])
}));

// Spy（部分 mock）
const spy = jest.spyOn(console, 'log');
// ... 被测代码 ...
expect(spy).toHaveBeenCalled();
spy.mockRestore();
```

## 2. HTTP 接口测试

```bash
npm install -D supertest
```

```javascript
// app.js
import express from 'express';
export const app = express();
app.use(express.json());
app.get('/health', (req, res) => res.json({ status: 'ok' }));

// app.test.js
import request from 'supertest';
import { app } from './app.js';

describe('API', () => {
    test('GET /health returns ok', async () => {
        const res = await request(app).get('/health');
        expect(res.status).toBe(200);
        expect(res.body).toEqual({ status: 'ok' });
    });
});
```

## 3. 调试技巧

### Node.js Inspector（类似 Java Debugger）

```bash
# 启动调试模式
node --inspect-brk script.js

# Chrome: chrome://inspect → 点击 "inspect"
# VS Code: F5 → 选择 "Node.js" 配置
```

```json
// .vscode/launch.json
{
    "version": "0.2.0",
    "configurations": [{
        "type": "node",
        "request": "launch",
        "name": "Debug",
        "program": "${workspaceFolder}/src/index.ts",
        "runtimeArgs": ["--loader", "ts-node/esm"]
    }]
}
```

### 生产诊断

```javascript
// 性能分析
console.time('database-query');
await db.query('SELECT ...');
console.timeEnd('database-query');  // → database-query: 23.5ms

// 内存使用
const used = process.memoryUsage();
console.log({
    heapUsed: `${Math.round(used.heapUsed / 1024 / 1024)} MB`,
    heapTotal: `${Math.round(used.heapTotal / 1024 / 1024)} MB`,
});
```

## 核心要点回顾

- Jest = JUnit：`describe`(测试类) + `test`(测试方法) + `expect`(断言)
- `supertest` = Spring MockMvc：测试 HTTP 接口
- `node --inspect-brk` = Java Debugger：断点调试
- Mock：`jest.fn()` 造假函数，`jest.mock()` 造假模块
- 性能诊断：`console.time/timeEnd` + `process.memoryUsage()`

## 参考资料

1. Jest 官方文档 — jestjs.io
2. Node.js Debugging Guide — nodejs.org/en/learn
