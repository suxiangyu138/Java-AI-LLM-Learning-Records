# 07 - TypeScript 基础

> 🎯 TypeScript 是 Java 开发者的"舒适区"——静态类型、接口、泛型、装饰器，全都是熟悉的概念。从 JavaScript 到 TypeScript，就是从"裸奔"到"有安全带"

---

## 目录

1. [TypeScript vs Java 类型映射](#1-typescript-vs-java-类型映射)
2. [接口与类型别名](#2-接口与类型别名)
3. [泛型](#3-泛型)
4. [tsconfig.json 配置](#4-tsconfigjson-配置)

---

## 1. TypeScript vs Java 类型映射

| Java | TypeScript | 注意 |
|------|-----------|------|
| `String` | `string` | TS 小写 |
| `int / double` | `number` | TS 不区分整数浮点 |
| `boolean` | `boolean` | 相同 |
| `T[]` | `T[]` 或 `Array<T>` | TS 两种写法 |
| `Map<K,V>` | `Record<K,V>` 或 `Map<K,V>` | Record 更常用 |
| `Optional<T>` | `T \| undefined` | TS 用联合类型 |
| `void` | `void` | 相同 |
| `Object` | `object` 或 `{}` | TS 避免用 Object |

### 基础类型示例

```typescript
// 基本类型
let name: string = "Alice";
let age: number = 25;
let active: boolean = true;

// 数组
let tags: string[] = ["ai", "node"];
let matrix: number[][] = [[1, 2], [3, 4]];

// 元组（固定长度+类型）
let pair: [string, number] = ["Alice", 25];

// 枚举
enum Status { Pending, Active, Done }
let s: Status = Status.Active;

// 联合类型（Java 没有！）
let id: string | number = "abc123";
id = 456;  // 也合法

// Optional（Java Optional 的 TS 版）
let email: string | undefined;
let nickname?: string;  // 等价于 string | undefined
```

## 2. 接口与类型别名

```typescript
// Interface（类似 Java 的 interface）
interface User {
    id: number;
    name: string;
    email: string;
    age?: number;           // 可选属性
    readonly createdAt: Date;  // 只读
}

// Type Alias（更灵活，支持联合/交叉）
type ID = string | number;
type Point = { x: number; y: number };
type Admin = User & { role: 'admin'; permissions: string[] };  // 交叉类型

// 函数签名
interface EventHandler {
    (event: string, data: unknown): void;
}

// 实现接口
class UserService implements EventHandler {
    (event: string, data: unknown): void {
        console.log(`${event}: ${data}`);
    }
}
```

### Interface vs Type

| 维度 | Interface | Type |
|------|:---:|:---:|
| 扩展 | `extends` | `&` 交叉 |
| 声明合并 | ✅ 自动合并 | ❌ |
| 联合类型 | ❌ | ✅ |
| 元组 | ❌ | ✅ |
| 推荐 | **对象结构优先用 Interface** | 联合/交叉/工具类型 |

## 3. 泛型

```typescript
// 泛型函数（Java: <T> T identity(T value)）
function identity<T>(value: T): T {
    return value;
}

// 泛型约束
function getLength<T extends { length: number }>(item: T): number {
    return item.length;
}

// 泛型接口
interface ApiResponse<T> {
    data: T;
    status: number;
    message: string;
}

// 使用
const userResp: ApiResponse<User> = {
    data: { id: 1, name: "Alice", email: "..." },
    status: 200,
    message: "ok"
};

// 泛型工具类型
type PartialUser = Partial<User>;    // 所有属性可选
type ReadonlyUser = Readonly<User>;   // 所有属性只读
type UserKeys = keyof User;           // 'id' | 'name' | 'email' | ...
type PickName = Pick<User, 'name'>;   // 只选某些属性
type OmitId = Omit<User, 'id'>;       // 排除某些属性
```

## 4. tsconfig.json 配置

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,                    // ← 严格模式（推荐开启）
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true                // 生成 .d.ts 类型声明
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
```

## 核心要点回顾

- TypeScript = JavaScript + 静态类型 = Java 开发者的"舒适区"
- Interface 用于对象结构，Type 用于联合/交叉
- 联合类型 `|` 和可选 `?` 是 TypeScript 的优点
- 泛型工具类型：`Partial<T>` / `Pick<T>` / `Omit<T>`
- `strict: true` 必须开启（否则 TypeScript 白用了）

## 参考资料

1. TypeScript 官方文档 — typescriptlang.org
2. TypeScript 5.5 新特性
