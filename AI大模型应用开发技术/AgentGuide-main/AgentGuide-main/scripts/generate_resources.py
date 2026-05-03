import os
import json
import re
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OWNER_REPO = "adongwanai/AgentGuide"

INCLUDE_DIRS = [
    os.path.join(ROOT, "docs"),
    os.path.join(ROOT, "resources"),
    os.path.join(ROOT, "projects"),
]

def read_text(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read()
    except Exception:
        return ""

def first_heading(md):
    for line in md.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
        if re.match(r"^#+\\s+", line):
            return re.sub(r"^#+\\s+", "", line).strip()
    return ""

def first_paragraph(md):
    lines = [l.strip() for l in md.splitlines()]
    buf = []
    for l in lines:
        if not l or l.startswith("#"):
            if buf:
                break
            else:
                continue
        buf.append(l)
    return " ".join(buf).strip()[:300]

def category_for_path(rel):
    parts = rel.split("/")
    if parts[0] == "docs":
        if len(parts) > 1:
            sec = parts[1]
            if sec.startswith("02-tech-stack"):
                return "Agent"
            if sec.startswith("04-interview"):
                return "求职"
            if sec.startswith("05-roadmaps"):
                return "路线"
            if sec.startswith("03-practice"):
                return "实战"
            if sec.startswith("01-theory"):
                return "理论"
        return "教程"
    if parts[0] == "resources":
        if len(parts) > 1:
            if parts[1] == "rag":
                return "RAG"
            if parts[1] == "agent":
                return "Agent"
        return "资源"
    if parts[0] == "projects":
        return "项目"
    return "其他"

def type_for_path(rel):
    parts = rel.split("/")
    if parts[0] == "projects":
        return "项目索引"
    if parts[0] == "resources":
        if "papers" in parts:
            return "论文合集"
        return "资源"
    if parts[0] == "docs":
        if parts[1].startswith("04-interview"):
            return "指南"
        if parts[1].startswith("05-roadmaps"):
            return "路线"
        return "教程"
    return "文档"

def mtime_date(path):
    try:
        ts = os.path.getmtime(path)
        return datetime.fromtimestamp(ts).strftime("%Y-%m-%d")
    except Exception:
        return datetime.now().strftime("%Y-%m-%d")

def build_url(rel):
    return f"https://github.com/{OWNER_REPO}/blob/main/{rel}"

def collect():
    items = []
    for base in INCLUDE_DIRS:
        for root, _, files in os.walk(base):
            for fn in files:
                if not fn.lower().endswith(".md"):
                    continue
                path = os.path.join(root, fn)
                rel = os.path.relpath(path, ROOT).replace("\\", "/")
                md = read_text(path)
                title = first_heading(md) or os.path.splitext(fn)[0]
                desc = first_paragraph(md)
                item = {
                    "id": re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-"),
                    "title": title,
                    "description": desc,
                    "category": category_for_path(rel),
                    "tags": [],
                    "level": "入门",
                    "type": type_for_path(rel),
                    "url": build_url(rel),
                    "date": mtime_date(path),
                    "featured": False,
                }
                items.append(item)
    # sort: featured, date desc, title
    items.sort(key=lambda x: (not x.get("featured", False), x.get("date", "")), reverse=True)
    return items

def main():
    items = collect()
    out_dir = os.path.join(ROOT, "data")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "resources.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(items)} resources to {out_path}")

if __name__ == "__main__":
    main()


    