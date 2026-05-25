# 杂项 (Miscellaneous)

> Python 综合小工具与脚本集合，覆盖常用开发场景

## 项目概述

Python 杂项工具集合，用于存放各类小型实用脚本和工具。涵盖文件处理、自动化脚本、系统工具、API 调用、格式转换等日常开发中常见的需求。每个脚本独立运行，互不依赖。

## 技术栈

| 技术 | 说明 |
|------|------|
| Python | 3.10+ |
| 标准库 | os, sys, pathlib, json, csv, datetime, re 等 |
| 第三方库 | 按需引入 |

## 脚本分类

### 文件处理
- 批量重命名文件
- 文件格式转换（JSON ↔ CSV ↔ XML）
- 大文件分块读取
- 文件/文件夹大小统计
- 文件搜索与过滤

### 数据处理
- CSV 数据合并与清洗
- JSON 数据提取与转换
- Excel 自动化处理（openpyxl）
- 日志文件解析

### 系统工具
- 定时任务脚本
- 系统信息获取
- 进程监控
- 环境变量管理

### 自动化
- 邮件自动发送
- 网页截图
- 文件自动备份
- 批量下载

### 实用工具
- 密码生成器
- 二维码生成
- 文本加密/解密
- 图片处理（Pillow）

## 项目结构

```
杂项/
├── file_tools/
│   ├── batch_rename.py            # 批量重命名
│   ├── format_converter.py        # 格式转换
│   └── file_stats.py              # 文件统计
├── data_tools/
│   ├── csv_merger.py              # CSV 合并
│   ├── json_extractor.py          # JSON 提取
│   └── excel_automation.py        # Excel 自动化
├── system_tools/
│   ├── scheduled_task.py          # 定时任务
│   ├── sys_info.py                # 系统信息
│   └── backup.py                  # 自动备份
├── utils/
│   ├── password_generator.py      # 密码生成
│   ├── qrcode_generator.py        # 二维码
│   └── image_processor.py         # 图片处理
├── requirements.txt
├── main.py                        # 工具菜单
└── README.md
```

## 快速开始

```bash
pip install -r requirements.txt
python main.py  # 显示工具菜单
```

## 注意事项

- 每个脚本设计为独立运行，方便单独使用
- 文件操作类脚本注意先备份原始数据
- 敏感信息（密码、API Key）勿硬编码在脚本中，使用环境变量
