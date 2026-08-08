# 03-GitHub License 机制全解
> GitHub 怎么检测许可证：Licensee 引擎的规则、仓库添加 LICENSE 的完整流程、License REST API 三端点

## 📚 目录
1. [Licensee 检测引擎](#1-licensee-检测引擎)
2. [检测的文件命名规则](#2-检测的文件命名规则)
3. [添加 LICENSE 的仓库流程](#3-添加-license-的仓库流程)
4. [License REST API](#4-license-rest-api)
5. [检测的已知局限](#5-检测的已知局限)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. Licensee 检测引擎

GitHub 仓库主页的 License 信息由 **Licensee**（Ruby gem，开源）驱动：

```text
检测流程：
扫描仓库根目录
 → 用正则集合给候选文件打分（命名匹配）
 → 读取文件内容
 → 与 choosealicense.com 收录的"知名许可证"文本比对
 → 达到置信度阈值（默认 98%）→ 判定许可证
 → 无法判定 → 显示 "View license"（无识别）
```

| 匹配器 | 说明 |
|--------|------|
| `Exact` | 文本完全一致 |
| `Diff` | 相似度比对（> 置信度阈值） |
| `Package` | 少数包管理文件的 license 字段（可选） |
| `Gemspec` | Ruby gemspec 的 license 字段 |

```bash
# Licensee CLI 用法
gem install licensee
licensee detect .                # 检测当前目录
licensee detect --json .         # JSON 输出（置信度/匹配器/文件路径）
licensee diff LICENSE            # 与已知许可证的差异
licensee license-path .          # 返回许可证文件路径
```

## 2. 检测的文件命名规则

Licensee 用正则对根目录文件打分，识别以下命名（大小写不敏感）：

| 模式 | 示例 |
|------|------|
| 标准名 | `LICENSE`、`LICENCE`（英式）、`license.md`、`LICENSE.txt` |
| COPYING 系 | `COPYING`、`COPYING.txt`、`COPYRIGHT` |
| 后缀变体 | `LICENSE-MIT`、`LICENSE-APACHE`、`UNLICENSE` |

> ⚠️ **命名是硬规则**：用 `License.txt`、`LICENSE-file` 之外的自定义名（如 `licence.md` 是合法的，但 `legal/` 子目录下则**不会被检测**）——检测只扫**仓库根目录**。

## 3. 添加 LICENSE 的仓库流程

### 3.1 Web UI 流程

```text
① 仓库页 → Add file → Create new file
② 文件名输入 LICENSE（或 LICENSE.md）
③ 右侧 "Choose a license template" 按钮 → 模板选择器
④ 选择许可证（按谱系/适用场景筛选）→ 生成完整文本
⑤ 提交（Commit new file）
```

| 细节 | 说明 |
|------|------|
| 模板来源 | choosealicense.com 的许可证文本库 |
| 自动填充 | 模板顶部自动填入 `<year>`（默认当年）与 `<copyright holder>`（默认登录用户名） |
| 其他入口 | Settings → 无直接入口；快速路径是 Create new file |

### 3.2 命令行流程（推荐，可版本化）

```bash
# 用 curl 取官方文本（以 MIT 为例，替换年份与作者）
curl -s https://raw.githubusercontent.com/github/choosealicense.com/gh-pages/_licenses/mit.txt | \
  sed 's/\[year\]/2026/; s/\[fullname\]/Your Name/' > LICENSE
git add LICENSE && git commit -m "Add MIT license"
```

> 💡 **生产建议**：LICENSE 放仓库根目录、文件名严格用 `LICENSE`/`LICENSE.md`、不放在子目录——检测与工具链（Licensee/SCA 扫描器）都按此约定工作。

## 4. License REST API

| 端点 | 功能 | 返回要点 |
|------|------|---------|
| `GET /licenses` | 常用许可证列表 | `key`/`name`/`spdx_id`/`url` |
| `GET /licenses/{license}` | 单许可证详情 | 描述、许可/条件/限制摘要、正文文本 |
| `GET /repos/{owner}/{repo}/license` | **仓库检测结果** | `spdx_id`、检测文件路径、内容（base64） |

```bash
# 例：查仓库许可证
curl -H "Authorization: Bearer $GH_TOKEN" \
  https://api.github.com/repos/spring-projects/spring-framework/license

# 例：列常用许可证
curl https://api.github.com/licenses | jq '.[] | {key, spdx_id}'
```

```json
// GET /repos/{owner}/{repo}/license 响应示例
{
  "name": "Apache License 2.0",
  "path": "LICENSE.txt",
  "license": {
    "key": "apache-2.0",
    "name": "Apache License 2.0",
    "spdx_id": "Apache-2.0",
    "url": "https://api.github.com/licenses/apache-2.0"
  }
}
```

| 特殊情况 | 返回 |
|----------|------|
| 未识别/无许可证 | `license` 为 `null`，`spdx_id` 相关字段为空 |
| 自定义文本 | API 层可能返回 `OTHER`/`NOASSERTION`（调用方需自建文本比对兜底） |

## 5. 检测的已知局限

| 局限 | 说明 | 应对 |
|------|------|------|
| 多命名冲突文件 | 仓库同时存在 `license-checks.xml`/`license_test.go` 时干扰打分，`LICENSE` 可能检测失败（Licensee issue #479） | 避免在根目录放含 "license" 的非许可证文件；必要时改名 |
| 只比对"知名全集" | 小众/自定义许可证无法识别 | 自建扫描器文本兜底（比对特征句） |
| 不解析 README | README 里的许可证声明不可靠，默认忽略 | LICENSE 文件是唯一可靠信号 |
| 不解析 SPDX 表达式 | `GPL-3.0-only` vs `or-later` 不区分 | 精确语义看文本/SPDX |
| 不检查依赖 | 检测的是**项目自身**许可证 | 依赖合规走 [04](04-开源合规与依赖许可证管理.md) |
| 不检查文件头 | 源码注释里的版权头不参与 | 项目级检测与文件级 SPDX 头是两回事 |

> 🎯 **核心要点**：GitHub 的 License 检测 = **根目录文件内容比对**，理解它的"看什么不看什么"（命名/内容/置信度），就能解释"为什么我的 LICENSE 没被识别"这类问题。

## 6. 核心要点

> 🎯 **核心要点**：
> - Licensee 流程：命名打分 → 内容比对 → 置信度阈值（98%）；CLI 可本地复现；
> - 文件必须放**根目录**、用标准命名（LICENSE/COPYING 系）；
> - 三端点 API：列表、详情、仓库检测（`spdx_id` 是核心信号）；
> - 六大局限记住两件事：多"license"命名文件会干扰检测、依赖合规不归 Licensee 管。

## 7. 参考来源

- [GitHub Docs：Licenses REST API](https://docs.github.com/en/rest/licenses/licenses)
- [Licensee GitHub（源码与 CLI 文档）](https://github.com/licensee/licensee)
- [Licensee：What We Look At](https://raw.githubusercontent.com/licensee/licensee/7146d5a92f40adceb809b051e547174f82ad6008/docs/what-we-look-at.md)
- [Licensee issue #479（多文件干扰）](https://github.com/licensee/licensee/issues/479)
- [GitHub REST API 版本说明（apiVersion=2022-11-28）](https://docs.github.com/en/rest/about-the-rest-api)

---

**下一模块**：[04-开源合规与依赖许可证管理](04-开源合规与依赖许可证管理.md)　/　**返回总览**：[00-总览](00-GitHub%20License总览.md)
