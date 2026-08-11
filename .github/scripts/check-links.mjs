// 根文档相对链接完整性检查（CI 与本地共用）
// 用法：node .github/scripts/check-links.mjs
// 逻辑：扫描根级文档中的本地相对链接（./xxx），URL 解码后验证文件存在性
//       纯离线校验——不访问任何外部网络，避免 CI 抖动误报

import fs from "node:fs";
import path from "node:path";

const FILES = [
  "README.md",
  "CHANGELOG.md",
  "CONTRIBUTING.md",
  "SECURITY.md",
  "CODE_OF_CONDUCT.md",
  "DEVELOPMENT.md",
  "CLAUDE.md",
];

// 匹配 Markdown 相对链接 [text](./path) / [text](../path)，排除 http(s)/mailto/锚点
const LINK_RE = /\]\((\.\.?\/[^)\s]+)\)/g;

let missing = 0;

for (const file of FILES) {
  const content = fs.readFileSync(file, "utf8");
  for (const match of content.matchAll(LINK_RE)) {
    // 去掉 #锚点 与 ?参数
    let target = match[1].split("#")[0].split("?")[0];
    target = decodeURIComponent(target);

    if (fs.existsSync(path.join(process.cwd(), target))) {
      console.log(`OK   ${file} -> ${match[1]}`);
    } else {
      console.log(`FAIL ${file} -> ${match[1]}`);
      missing++;
    }
  }
}

if (missing > 0) {
  console.error(`\n${missing} 个相对链接失效`);
  process.exit(1);
}
console.log("\n根文档相对链接全部有效");
