# 06-实操速查与 FAQ
> 添加/修改/移除许可证的操作速查、Licensee CLI 与 API 命令、高频 FAQ、面试题

## 📚 目录
1. [操作速查：添加/修改/移除 LICENSE](#1-操作速查添加修改移除-license)
2. [Licensee CLI 速查](#2-licensee-cli-速查)
3. [License API 速查](#3-license-api-速查)
4. [高频 FAQ](#4-高频-faq)
5. [面试题](#5-面试题)
6. [参考来源](#6-参考来源)

## 1. 操作速查：添加/修改/移除 LICENSE

### 1.1 添加（Web UI）

| 步骤 | 操作 |
|------|------|
| ① | 仓库页 → `Add file` → `Create new file` |
| ② | 文件名输入 `LICENSE`（放根目录） |
| ③ | 点右侧 `Choose a license template` → 选模板 |
| ④ | 核对 `<year>` 与 `<fullname>` → `Commit new file` |

### 1.2 修改（换许可证）

```bash
# 换许可证的正确姿势：替换文件 + 明确 git 历史说明
# 1) 删除旧文件
git rm LICENSE
# 2) 写入新许可证文本（注意年份/作者）
# 3) 提交并写清楚变更说明
git commit -m "Re-license project from MIT to Apache-2.0"
```

> ⚠️ **换许可的法律提示**：**已有贡献者的代码换许可需要贡献者同意**（每位贡献者对其代码持有版权）——开源项目换许可前应获得全部/绝大多数贡献者确认（业界通过 CLA 或公告期解决）。Git 历史里的旧版本依然受旧许可约束。

### 1.3 移除

```bash
git rm LICENSE && git commit -m "Remove license"
# 移除后：仓库不再被 Licensee 识别 → 默认保留所有权利
```

## 2. Licensee CLI 速查

```bash
gem install licensee

licensee detect .                        # 检测当前目录
licensee detect --json .                 # JSON 输出（置信度/匹配器/文件路径）
licensee detect --confidence 90 .        # 调整置信度阈值（默认 98）
licensee diff LICENSE                    # 与已知许可证逐行对比
licensee license-path .                  # 输出许可证文件路径
licensee detect --remote spring-projects/spring-boot   # 检测远程仓库
```

| 输出字段 | 含义 |
|----------|------|
| `matched_files` | 命中的文件 |
| `confidence` | 匹配置信度 |
| `matcher` | 匹配器（Exact/Diff/Package...） |
| `content_hash` | 文件内容哈希 |

## 3. License API 速查

```bash
# 常用许可证列表
curl https://api.github.com/licenses

# 单许可证详情（含许可/条件/限制摘要与正文）
curl https://api.github.com/licenses/apache-2.0

# 仓库许可证检测结果
curl -H "Authorization: Bearer $GH_TOKEN" \
  https://api.github.com/repos/{owner}/{repo}/license
```

```bash
# 批量扫描组织的仓库许可证（jq 示例）
curl -s -H "Authorization: Bearer $GH_TOKEN" \
  https://api.github.com/orgs/{org}/repos?per_page=100 \
  | jq -r '.[].full_name' | while read repo; do
      spdx=$(curl -s -H "Authorization: Bearer $GH_TOKEN" \
        "https://api.github.com/repos/$repo/license" | jq -r '.license.spdx_id')
      echo "$repo → $spdx"
    done
```

## 4. 高频 FAQ

| # | 问题 | 答案 |
|---|------|------|
| 1 | 仓库没有 LICENSE，别人能用吗？ | **不能用**。无许可证 = 保留所有权利（默认"版权所有"） |
| 2 | 我引用了 MIT 库，必须开源我的项目吗？ | 否。MIT 允许闭源商用，只要求保留版权声明 |
| 3 | 检测显示 MIT，但 LICENSE 文件是网上抄的？ | Licensee 只做相似度比对，内容被改动也能识别；但**改过的文本不是法律意义上的 MIT** |
| 4 | LICENSE 放子目录行吗？ | 会被 GitHub 检测**漏掉**——必须根目录 + 标准命名 |
| 5 | 多人项目加 LICENSE 需要注意什么？ | 新代码归项目许可；**历史贡献者代码**需其同意（或 CLA） |
| 6 | 私有仓库也要 LICENSE 吗？ | 私有仓库是团队内部约定，不是法律必需；发布前补上即可 |
| 7 | 我的项目用了 GPL 库，整个项目都要开源吗？ | 分发时是的（衍生作品整体 GPL）；内部使用不触发 |
| 8 | Dependabot 会提示许可证问题吗？ | 不会——它只管版本与漏洞；许可证用 Dependency Review |
| 9 | `LICENSE-MIT` 和 `LICENSE` 的区别？ | Licensee 都识别；规范做法是单文件 `LICENSE`（多许可证项目用子文件+根 LICENSE 汇总） |
| 10 | 换许可证后旧版本还受旧许可约束吗？ | 是。Git 历史中的旧版本按当时许可；"换许可"只对新版本生效（且需贡献者同意） |

## 5. 面试题

| 题目 | 答题要点 |
|------|---------|
| MIT 和 GPL 的本质区别？ | 宽松 vs 强著佐权：能否闭源商用衍生品 |
| 没有许可证的代码能用吗？ | 不能，默认保留所有权利 |
| AGPL 为什么出现？ | 堵住 SaaS 白嫖：网络服务视为分发 |
| LGPL 动态链接为什么允许闭源？ | 库独立分发，程序不视为衍生（修改库本身仍需开源） |
| 你的项目怎么选许可证？ | 场景化决策：MIT 默认 / Apache 企业+专利 / AGPL 防 SaaS |
| GitHub 怎么检测许可证？ | Licensee：根目录命名打分 + 内容比对 + 置信度阈值 |
| 依赖合规怎么做？ | Dependabot（版本/漏洞）+ Dependency Review Action（许可证门禁）+ SBOM |
| 引入 GPL 依赖到商业项目怎么办？ | 替换依赖 → 隔离进程 → 商购双许可 → 法务裁决（四选一） |
| Dependabot 能做许可证扫描吗？ | 不能，这是常见误区；许可证归 Dependency Review/合规产品 |

## 6. 参考来源

- [Licensee CLI 文档](https://raw.githubusercontent.com/licensee/licensee/7146d5a92f40adceb809b051e547174f82ad6008/docs/command-line-usage.md)
- [GitHub Docs：Licenses API](https://docs.github.com/en/rest/licenses/licenses)
- [Choose a License（FAQ）](https://choosealicense.com/community/)
- [GitHub Docs：Dependency Review](https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/about-dependency-review)

---

**返回总览**：[00-总览](00-GitHub%20License总览.md)
