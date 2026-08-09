# 02 scripts 脚本编写与安全

> 技能可靠性的关键：把"脆弱、重复、易错"的操作推进脚本——执行脚本比逐字段生成稳定；语言按生态选（Python 通用/Go 云原生）；脚本必须带测试、必须过安全审查。

## 📚 目录

1. [为什么确定性进代码](#1-为什么确定性进代码)
2. [语言选择](#2-语言选择)
3. [脚本设计原则](#3-脚本设计原则)
4. [脚本与 SKILL.md 的接口](#4-脚本与-skillmd-的接口)
5. [脚本安全审查](#5-脚本安全审查)
6. [脚本测试](#6-脚本测试)
7. [常见误区](#7-常见误区)
8. [面试高频问法](#8-面试高频问法)

## 1. 为什么确定性进代码

### 场景对比

```
字段抽取（PDF → 结构化）：
❌ SKILL.md 描述"提取字段" → Agent 逐字段生成（每次结果可能不同）
✅ scripts/extract.py → 执行（结果确定、可测试）
```

### 判定标准（官方实践）

```
脆弱、重复、变化即 bug 的操作 → 写脚本：
① 解析/抽取（确定性规则）
② 转换/格式化（固定逻辑）
③ 校验/检查（重复执行）
④ 脚手架（固定结构）
```

### 脚本的价值

| 价值 | 说明 |
|---|---|
| 确定性 | 相同输入相同输出（可测试） |
| 稳定性 | 不依赖模型每次的发挥 |
| 可测 | 脚本有单元测试 |
| 可复用 | 独立于对话执行 |

## 2. 语言选择

| 语言 | 适用 | 例子 |
|---|---|---|
| Python | 通用（CLI 封装/验证器/脚手架） | 文档校验、数据转换 |
| Shell | 简单命令序列 | 环境操作 |
| Go | Kubernetes/云原生生态 | 云技能 |

### 选择建议

```
通用技能 → Python（生态最全）
简单操作 → Shell（零依赖）
云原生 → Go（部署场景）
——按技能的目标生态选，别为炫技换语言
```

### 依赖管理

```
脚本依赖 → 明确声明（requirements.txt 等）
依赖版本 → 锁定（可复现）
无依赖优先 → 标准库够用就别引
```

## 3. 脚本设计原则

| 原则 | 说明 |
|---|---|
| 单一职责 | 一个脚本一件事 |
| CLI 接口 | 参数化输入（不写死路径） |
| 明确输出 | 结构化（JSON/清晰文本） |
| 错误处理 | 失败有清晰错误信息 |
| 幂等 | 重复执行结果一致 |

### 脚本骨架示例

```python
#!/usr/bin/env python3
"""校验 Markdown 文档的内部链接（示例）"""
import argparse
import re
import sys
from pathlib import Path


def check_links(file_path: Path) -> list[str]:
    """返回失效链接列表（确定性操作）"""
    text = file_path.read_text(encoding="utf-8")
    broken = []
    for match in re.finditer(r"\]\(([^)#]+)\)", text):
        target = Path(match.group(1))
        if not target.exists():
            broken.append(f"缺失: {match.group(1)}")
    return broken


def main() -> None:
    parser = argparse.ArgumentParser(description="校验内部链接")
    parser.add_argument("file", type=Path, help="目标文档")
    args = parser.parse_args()
    broken = check_links(args.file)
    if broken:
        print("\n".join(broken))
        sys.exit(1)          # 失败退出码（供 Agent 判断）
    print("链接全部有效")


if __name__ == "__main__":
    main()
```

### 脚本设计要点

```
① CLI 参数（Agent 传文件路径）
② 退出码（成功 0/失败 1——Agent 据此分支）
③ 输出可解析（Agent 读结果）
④ 幂等（重复跑结果一致）
```

## 4. 脚本与 SKILL.md 的接口

### SKILL.md 如何调用

```markdown
## 执行步骤
1. 生成文档
2. 校验：`python ${CLAUDE_SKILL_DIR}/scripts/check_links.py 文档.md`
3. 若退出码非 0，修复链接后重跑
```

### 接口设计原则

```
① 脚本路径用 ${CLAUDE_SKILL_DIR}（可移植）
② 退出码是"判断信号"（Agent 分支依据）
③ 输出要"文档化"：SKILL.md 里展示输出长什么样
④ Defer to --help：SKILL.md 只列常用，细节让 Agent 跑 --help
```

### 文档化输出格式

```
脚本返回结构化数据 → SKILL.md 展示示例：
## 校验输出示例
缺失: docs/old.md
退出码: 1
——Agent 知道怎么解析（下游可靠）
```

## 5. 脚本安全审查

### 审查清单（阶段 1 的 06 篇深化）

| 审查项 | 检查 |
|---|---|
| 危险命令 | 无 rm -rf/curl 管道 shell 等 |
| 路径处理 | 无路径遍历（../ 注入） |
| 输入验证 | CLI 参数有校验 |
| 网络调用 | 无偷偷外传数据 |
| 依赖 | 无恶意依赖（供应链） |
| 权限 | 最小权限运行（不需要 root） |

### 与 allowed-tools 的配合

```
脚本执行也受 allowed-tools 约束：
allowed-tools: Bash(python:*)  # 只允许运行 python
——脚本能跑，但工具面受限（双保险）
```

### 安全底线

```
① 技能脚本 = 可执行代码（按代码审查标准）
② 不信任来源不明的脚本（36% 缺陷率背景）
③ 生产使用前逐行审查 + 测试
```

## 6. 脚本测试

### 为什么必须测

```
无测试的脚本 = 负债：
改了不知道坏没坏、环境变了不知道挂没挂
CI 验证脚本/测试配对（2026 实践）
```

### 测试方式

```python
# 简单断言测试（无框架也行）
from scripts.check_links import check_links

def test_check_links():
    # 构造测试文件（临时目录）
    result = check_links(Path("test_doc.md"))
    assert "缺失: old.md" in result
```

### 测试范围

```
① 正常路径（预期输入）
② 边界（空文件/无链接）
③ 异常（文件不存在）
④ 幂等（跑两次结果一致）
```

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "脚本万能" | 判断力留 SKILL.md（脚本只做确定性） |
| "脚本不用测" | 无测试的脚本是负债 |
| "路径写死" | CLI 参数 + CLAUDE_SKILL_DIR |
| "退出码无所谓" | 是 Agent 的分支信号 |
| "审查只查脚本" | 还要查依赖与输入验证 |

## 8. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 什么时候写脚本？ | 脆弱/重复/易错的操作 |
| 语言怎么选？ | Python 通用/Shell 简单/Go 云原生 |
| 脚本接口要点？ | CLI 参数/退出码/可解析输出 |
| 怎么调脚本？ | ${CLAUDE_SKILL_DIR} 路径 |
| 脚本安全？ | 审查清单 + allowed-tools 双保险 |
| 脚本测试？ | 断言测试（正常/边界/异常/幂等） |

### 面试加分表达

> "scripts 的定位是'把确定性推进代码'：解析、校验、转换这类脆弱易错的操作写成脚本，执行比逐字段生成稳定。接口三要点：CLI 参数（Agent 传输入）、退出码（Agent 的分支信号）、可解析输出（SKILL.md 里文档化示例）。安全上脚本按代码审查标准审（危险命令/路径/依赖），配合 allowed-tools 限定 Bash(python:*) 双保险；脚本必须带测试，无测试是负债。"

> 🎯 核心要点：确定性进代码（解析/转换/校验/脚手架）；语言按生态选；接口三要点（CLI/退出码/可解析输出）+ ${CLAUDE_SKILL_DIR}；Defer to --help（SKILL.md 只列常用）；安全审查清单 + allowed-tools 双保险；脚本必须带测试（正常/边界/异常/幂等）。

---

**下一模块**：[03-references组织与按需加载](03-references组织与按需加载.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
