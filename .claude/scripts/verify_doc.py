"""Lightweight markdown doc checker for PostToolUse hook.

Usage: python verify_doc.py "<file1> <file2> ..."
Checks: code fence pairing, orphan table separator rows.
Exits 0 silently on pass; prints issues to stderr (exit 0 keeps flow unblocked).
"""
import re
import sys
from pathlib import Path

FENCE = re.compile(r"^```")


def check_file(path: Path) -> list[str]:
    issues: list[str] = []
    if path.suffix.lower() != ".md" or not path.exists():
        return issues
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except (UnicodeDecodeError, OSError):
        return issues

    # 1. Code fences must pair
    fences = sum(1 for ln in lines if FENCE.match(ln))
    if fences % 2:
        issues.append(f"[围栏] {path.name}: 代码围栏不成对（{fences} 个，应为偶数）")

    # 2. No orphan table separator rows (|---| without a header above)
    for i, ln in enumerate(lines):
        if "|" in ln and re.match(r"^\s*\|?[\s:|-]+\|?\s*$", ln) and "-" in ln:
            if i == 0 or "|" not in lines[i - 1]:
                issues.append(f"[表格] {path.name}: 第 {i+1} 行孤立分隔行（上方无表头）")
                break  # one report per file is enough for the hook

    # 3. Reference sources must be hyperlinks (one sample check)
    if "【参考来源】" in path.read_text(encoding="utf-8"):
        for m in re.finditer(r"^- (.+)$", path.read_text(encoding="utf-8"), re.M):
            line = m.group(1)
            if line.startswith("【") or line.strip() == "":
                continue
            if "(" not in line or ")" not in line:
                issues.append(f"[来源] {path.name}: 参考来源条目缺少超链接: {line[:40]}")
                break

    return issues


def main() -> int:
    raw = sys.argv[1] if len(sys.argv) > 1 else ""
    # CLAUDE_FILE_PATHS may be space-separated; strip quotes
    paths = [p.strip("\"'") for p in raw.split() if p.strip("\"'")]
    all_issues: list[str] = []
    for p in paths:
        all_issues.extend(check_file(Path(p)))
    if all_issues:
        print("📋 文档校验发现（自动钩子，可忽略或运行 /verify-docs 修复）:", file=sys.stderr)
        for issue in all_issues[:5]:
            print(f"  {issue}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
