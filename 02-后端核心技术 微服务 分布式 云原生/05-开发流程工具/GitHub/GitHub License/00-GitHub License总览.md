# GitHub License 总览
> 开源许可证选型、GitHub 的 License 检测机制、依赖许可证合规与商用避坑——从"加一个 LICENSE 文件"到"企业级合规"

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [版本窗口说明](#5-版本窗口说明)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
GitHub License 知识体系（2026-08 基准）
│
├─ 许可证本体层 ─────────────────────────
│   ├─ 许可证谱系（宽松 → 强著佐权）
│   ├─ 主流 8 证条款深读（MIT/Apache/BSD/GPL/AGPL/MPL/LGPL/Unlicense）
│   └─ 选型流程（choosealicense.com 三问）
│
├─ GitHub 机制层 ────────────────────────
│   ├─ Licensee 检测（文件命名/比对算法/置信度）
│   ├─ 仓库 LICENSE 添加流程（UI 模板选择器）
│   └─ License REST API（/licenses /repos/.../license）
│
├─ 合规工程层 ───────────────────────────
│   ├─ 依赖许可证扫描（Dependency Review Action）
│   ├─ GitHub license 合规产品（Dependabot 边界）
│   ├─ 第三方 SCA 对比（Mend/Snyk/Trivy）
│   └─ SBOM（CycloneDX）
│
└─ 商用决策层 ───────────────────────────
    ├─ 许可证兼容矩阵（GPL 传染边界）
    ├─ AGPL 网络条款 / SSPL 云厂商条款
    └─ 闭源商用项目用开源依赖的边界
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-GitHub%20License总览.md) | 导图、路线、速查 | 所有人 |
| 01 | [开源许可证谱系与选型](01-开源许可证谱系与选型.md) | 许可证类型、8 证对比、选型流程 | 入门必读 |
| 02 | [核心许可证条款深读](02-核心许可证条款深读.md) | 许可/条件/限制三要素逐条解读 | 进阶 |
| 03 | [GitHub License 机制全解](03-GitHub-License机制全解.md) | Licensee 算法、UI 流程、License API | 入门必读 |
| 04 | [开源合规与依赖许可证管理](04-开源合规与依赖许可证管理.md) | Dependency Review、合规产品、SCA、SBOM | 进阶重点 |
| 05 | [许可证兼容性与商用避坑](05-许可证兼容性与商用避坑.md) | 兼容矩阵、GPL/AGPL/SSPL、商用边界 | 重点 |
| 06 | [实操速查与 FAQ](06-实操速查与FAQ.md) | 添加/修改/移除、CLI/API、FAQ、面试题 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（半天） | 要给自己仓库加许可证 | 00 → 01 → 03 → 06（操作部分） |
| 工程合规（1~2 天） | 团队要治理依赖许可 | 入门 + 04 → 05 |
| 面试/开源贡献者（半天） | 理解"开源是什么" | 00 → 01 → 02 → 05（GPL 传染是关键考点） |

## 4. 核心概念速查

| 概念 | 一句话 | 关键点 |
|------|--------|--------|
| 许可证（License） | 授权他人使用你的代码的法律文件 | 无许可证 ≠ 放弃权利（默认保留所有权利） |
| 宽松许可证 | 允许任意使用（含闭源商用） | MIT/Apache-2.0/BSD |
| 著佐权（Copyleft） | 衍生作品必须同许可开源 | GPL/AGPL/MPL/LGPL |
| SPDX 标识符 | 许可证的标准短标识（`MIT`、`GPL-3.0-only`） | GitHub API 用 `spdx_id` 返回 |
| Licensee | GitHub 的许可证检测引擎（Ruby gem） | 按文件内容比对 choosealicense 已知许可证 |
| 检测文件命名 | LICENSE/LICENCE/LICENSE.md/COPYING... | 命名不合规 → 检测失败 |
| License API | 查询许可证信息与仓库检测结果 | `GET /repos/{owner}/{repo}/license` |
| Dependency Review | PR 阶段依赖合规门禁（Action） | `deny-licenses` 配置 |
| SBOM | 软件物料清单（依赖清单标准格式） | CycloneDX/SPDX 格式 |
| 传染性（Viral） | 衍生代码需继承许可 | GPL 系；AGPL 网络调用也算"分发" |

> 🎯 **核心要点**：License 的三大工程事实——① **没有 LICENSE 文件 = 默认禁止他人使用**（不是"随便用"）；② GitHub 检测是**文件内容比对**，不解析 README 与依赖；③ 依赖合规靠 CI 门禁（Dependency Review），不靠 GitHub 自动提示。

## 5. 版本窗口说明

| 项 | 现状（2026-08） | 说明 |
|----|----------------|------|
| Licensee 检测 | GitHub 仓库页 + API 持续使用 | 只比对"知名许可证全集"；不解析 SPDX 表达式 |
| License API | REST v3（`apiVersion=2022-11-28`） | 返回 `spdx_id`，未识别返回 `NOASSERTION`/`OTHER` |
| Dependabot | 依赖版本/漏洞更新 | **本身不做许可证合规**（官方定位） |
| Dependency Review Action | `actions/dependency-review-action` | PR 门禁：`deny-licenses`/`allow-licenses` |
| GitHub 许可证合规产品 | 2026 起随 OSPO Playbook 推出 | 与 Dependency Graph + Dependabot 集成，500+ SPDX 标识符 |
| 云厂商条款 | SSPL（MongoDB）/ Elastic License | 非 OSI 批准，注意与 GPL 不兼容 |

> ⚠️ **时效性**：许可证文本与条款以 [SPDX License List](https://spdx.org/licenses/) 与官方文本为准；本文是工程视角解读，不构成法律意见——商业关键决策请咨询法务。

## 6. 参考来源

- [GitHub Docs：Licenses REST API](https://docs.github.com/en/rest/licenses/licenses)
- [Licensee GitHub（检测引擎源码）](https://github.com/licensee/licensee)
- [Licensee：What We Look At（检测规则文档）](https://raw.githubusercontent.com/licensee/licensee/7146d5a92f40adceb809b051e547174f82ad6008/docs/what-we-look-at.md)
- [Choose a License（官方选型站）](https://choosealicense.com/)
- [SPDX License List](https://spdx.org/licenses/)
- [GitHub 开源合规 Playbook（2026-07）](https://artificialintelligenceherald.com/technology/github-license-compliance-open-source-dependencies-2026)

---

**下一模块**：[01-开源许可证谱系与选型](01-开源许可证谱系与选型.md)
