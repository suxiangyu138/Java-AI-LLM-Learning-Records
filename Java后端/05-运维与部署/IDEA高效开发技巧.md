# IntelliJ IDEA 高效开发技巧

## 常用快捷键

| 功能 | 快捷键 | 说明 |
|------|--------|------|
| 代码补全 | `Ctrl+Space` | 基础补全 |
| 智能补全 | `Ctrl+Shift+Space` | 按类型过滤 |
| 显示参数 | `Ctrl+P` | 方法参数提示 |
| 快速修复 | `Alt+Enter` | 最常用的万能键 |
| 生成代码 | `Alt+Insert` | getter/setter/构造器等 |
| 重构命名 | `Shift+F6` | 重命名变量/方法/类 |
| 提取方法 | `Ctrl+Alt+M` | 选中代码 → 提取为方法 |
| 提取变量 | `Ctrl+Alt+V` | 提取表达式为变量 |
| 格式化代码 | `Ctrl+Alt+L` | 格式化当前文件 |
| 优化导入 | `Ctrl+Alt+O` | 删除无用 import |
| 查找文件 | `Double Shift` | 万能搜索 |
| 查找类 | `Ctrl+N` | 按类名搜索 |
| 查找文件 | `Ctrl+Shift+N` | 按文件名搜索 |
| 查找符号 | `Ctrl+Alt+Shift+N` | 搜索方法/变量 |
| 最近文件 | `Ctrl+E` | 最近打开的文件 |
| 跳转定义 | `Ctrl+B` | 跳到声明处 |
| 查找用法 | `Alt+F7` | 所有使用此方法的地方 |
| 复制行 | `Ctrl+D` | 复制整行 |
| 删除行 | `Ctrl+Y` | 删除整行 |
| 移动行 | `Ctrl+Shift+↑↓` | 上下移动行 |
| 多光标 | `Alt+Shift+Click` | 多处同时编辑 |
| 列选择 | `Alt+Shift+Insert` | 列编辑模式 |

## Live Templates

### 常用内置模板

| 缩写 | 展开 |
|------|------|
| `psvm` | `public static void main(String[] args)` |
| `sout` | `System.out.println()` |
| `soutv` | `System.out.println("var = " + var)` |
| `fori` | `for (int i = 0; i < ; i++)` |
| `iter` | `for each` 循环 |
| `.var` | 自动补全变量声明 |

### 自定义模板

Settings → Editor → Live Templates → 添加：

```java
// 缩写: logi
// 模板: private static final Logger log = LoggerFactory.getLogger($CLASSNAME$.class);
// 适用: Java declaration
```

```java
// 缩写: restcon
@RestController
@RequestMapping("/api/v1/${entity}s")
@RequiredArgsConstructor
public class ${Entity}Controller {
    private final ${Entity}Service ${entity}Service;
}
```

## 必备插件

| 插件 | 用途 |
|------|------|
| MyBatisX | MyBatis Mapper ↔ XML 跳转、代码生成 |
| Maven Helper | 依赖冲突排查 |
| SonarLint | 实时代码质量检查 |
| Arthas Idea | Arthas 命令快速生成 |
| GitToolBox | Git 增强（行级 blame、状态显示） |
| Key Promoter X | 鼠标操作时提示对应快捷键 |
| .env files | .env 文件语法高亮 |
| RestfulTool | 接口测试工具 |
| Translation | 翻译插件 |
| Github Copilot | AI 代码补全 |

## Debug 技巧

- **条件断点**：右键断点 → Condition → 输入条件如 `userId == 123`
- **异常断点**：Run → View Breakpoints → Java Exception Breakpoints
- **断点日志**：右键断点 → 勾选 "Evaluate and log" → 输入要打印的表达式（不会中断程序）
- **Stream 调试**：Debug 时点击 Trace 面板的 "Trace Current Stream Chain"
- **强制返回**：暂停后，Frames 面板 → 右键方法 → Force Return，用于跳过异常逻辑
- **热更新代码**：修改代码后 `Ctrl+Shift+F9`（Recompile），适用于简单修改，不用重启

## 效率习惯

- **修改一次后立刻用 `Shift+F6` 统一重命名**，不要逐个修改
- **写完代码 `Ctrl+Alt+L` 格式化**，保持风格一致
- **用 `Alt+Enter` 而非手动改**：IDEA 的 Quick Fix 覆盖了大多数常见操作
- **`Ctrl+Shift+A` 搜索所有 Action**：忘记快捷键时用这个查
- **`Ctrl+E` + `Ctrl+Tab` 快速切换文件**
- **Bookmarks**：`F11` 标记文件，`Ctrl+F11` 标记行，`Shift+F11` 查看所有书签
