# 04 - Agent 工具系统设计

> 🎯 工具是 Agent 的"手脚" — 设计好工具系统，Agent 才能可靠地操作外部世界。工具注册、Schema 设计、沙箱执行、错误处理是四大基石

---

## 目录

1. [工具注册与发现](#1-工具注册与发现)
2. [工具 Schema 设计规范](#2-工具-schema-设计规范)
3. [工具执行沙箱](#3-工具执行沙箱)
4. [错误处理与重试](#4-错误处理与重试)
5. [工具系统完整示例](#5-工具系统完整示例)

---

## 1. 工具注册与发现

```python
class ToolRegistry:
    """工具注册中心"""
    
    def __init__(self):
        self._tools = {}  # name → Tool
    
    def register(self, func, name=None, description=None):
        """注册工具"""
        import inspect
        
        tool_name = name or func.__name__
        sig = inspect.signature(func)
        
        # 自动生成 JSON Schema
        properties = {}
        for param_name, param in sig.parameters.items():
            param_type = self._python_to_json_type(param.annotation)
            properties[param_name] = {
                "type": param_type,
                "description": f"参数 {param_name}"
            }
        
        self._tools[tool_name] = {
            "func": func,
            "schema": {
                "type": "function",
                "function": {
                    "name": tool_name,
                    "description": description or func.__doc__ or "",
                    "parameters": {
                        "type": "object",
                        "properties": properties,
                        "required": list(properties.keys())
                    }
                }
            }
        }
    
    def get_schemas(self):
        """获取所有工具的 JSON Schema（传给 LLM）"""
        return [t["schema"] for t in self._tools.values()]
    
    def execute(self, name, args):
        """执行工具"""
        if name not in self._tools:
            return {"error": f"未知工具: {name}"}
        return self._tools[name]["func"](**args)
```

---

## 2. 工具 Schema 设计规范

```text
工具 Schema 设计黄金法则：

① description 是 LLM 选择工具的唯一依据 → 写清楚"何时用、做什么"
   ❌ "查询数据"
   ✅ "查询MySQL数据库中的销售数据。当用户询问销售、订单、营收相关问题时使用。"

② 参数 description 同样重要
   ❌ "query": {"type": "string"}
   ✅ "query": {"type": "string", "description": "SQL查询语句，仅支持SELECT"}

③ 使用枚举约束取值空间
   "time_range": {"type": "string", "enum": ["1d", "7d", "30d", "90d"]}

④ 区分必填和可选参数
   "required": ["query"]  → 只标记真正必须的
```

### Schema 设计对比

```text
❌ 差 Schema：
  {"name": "search", "description": "搜索",
   "parameters": {"query": {"type": "string"}}}

✅ 好 Schema：
  {"name": "search_knowledge_base",
   "description": "搜索企业内部知识库。当用户询问公司政策、技术文档、产品说明时使用此工具。不要用于查询实时数据。",
   "parameters": {
     "query": {"type": "string", "description": "搜索关键词，支持自然语言"},
     "max_results": {"type": "integer", "description": "返回结果数1-20", "default": 5},
     "filter_by_date": {"type": "string", "description": "按日期过滤，格式YYYY-MM-DD"}
   },
   "required": ["query"]}
```

---

## 3. 工具执行沙箱

```python
import subprocess
import tempfile
import os

class CodeSandbox:
    """代码执行沙箱 — 限制危险操作"""
    
    FORBIDDEN = ['os.system', 'subprocess', '__import__', 
                 'eval', 'exec', 'open(', 'rm ', 'delete']
    
    def execute(self, code, timeout=30):
        # ① 安全检查
        for keyword in self.FORBIDDEN:
            if keyword in code:
                return {"error": f"禁止使用: {keyword}"}
        
        # ② 隔离执行
        try:
            result = subprocess.run(
                ['python', '-c', code],
                capture_output=True, text=True,
                timeout=timeout,
                cwd='/tmp/sandbox'  # 限制工作目录
            )
            return {
                "stdout": result.stdout,
                "stderr": result.stderr,
                "returncode": result.returncode
            }
        except subprocess.TimeoutExpired:
            return {"error": f"执行超时 ({timeout}s)"}
```

### 工具权限分级

| 级别 | 权限 | 示例工具 | 限制 |
|:---:|------|----------|------|
| **0 - 只读** | 读取数据 | search、query_db、get_weather | 无副作用 |
| **1 - 创建** | 创建新资源 | create_file、send_email_draft | 需确认 |
| **2 - 修改** | 修改已有资源 | update_db、edit_file | 需二次确认 |
| **3 - 删除** | 删除资源 | delete_record | 需人工审批 |

---

## 4. 错误处理与重试

```python
def execute_with_retry(tool_name, args, max_retries=3):
    """带重试的工具执行"""
    for attempt in range(max_retries):
        try:
            result = execute_tool(tool_name, args)
            
            # 工具返回错误 → 可能需要修正参数
            if result.get("error"):
                if attempt < max_retries - 1:
                    # 返回错误信息让 LLM 修正参数
                    return {"needs_retry": True, 
                            "error": result["error"],
                            "suggestion": "请修正参数后重试"}
                return result
            
            return result
            
        except Exception as e:
            if attempt == max_retries - 1:
                return {"error": f"重试{max_retries}次后仍失败: {str(e)}"}
```

---

## 5. 工具系统完整示例

```python
# 完整工具系统
registry = ToolRegistry()

# 注册只读工具（Level 0）
@registry.register(description="搜索企业内部知识库")
def search_kb(query: str, max_results: int = 5):
    """搜索知识库"""
    return search_vector_db(query, max_results)

# 注册创建工具（Level 1，需确认）
@registry.register(description="创建新文档")
def create_document(title: str, content: str):
    """创建文档 → 需要用户确认"""
    return {"status": "pending_confirmation", 
            "preview": content[:100] + "..."}

# 获取工具 Schema 传给 LLM
schemas = registry.get_schemas()
```

---

## 核心要点回顾

- 工具 description 是 LLM 选工具的唯一依据 → 写清楚"何时用"
- Schema 设计：参数类型 + description + enum + required
- 工具执行要沙箱化（权限分级 + 禁止危险操作）
- 错误处理要返回结构化信息，让 LLM 能根据错误修正参数
