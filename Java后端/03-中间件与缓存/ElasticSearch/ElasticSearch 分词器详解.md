# ElasticSearch 分词器详解（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 分词器配置与实战
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：中文搜索、IK 分词、拼音搜索、自定义词典

---

## 一、分词器是什么

**分词器（Analyzer）** 将一段文本拆分为一个个独立的词条（Term），是倒排索引的入口。

```
输入: "计算机专业学生"
输出: ["计算机", "专业", "学生"]    ← 这些词写进倒排索引
```

---

## 二、分词器三大组件

```
┌────────────────────────────────────────┐
│              Analyzer                   │
│                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────┐│
│  │Character │→  │Tokenizer │→  │Token ││
│  │ Filter   │   │          │   │Filter││
│  └──────────┘   └──────────┘   └──────┘│
│                                         │
│  "I like  cats"                         │
│       ↓ Character Filter（字符过滤）     │
│  "I like cats"      ← 去除 HTML 标签    │
│       ↓ Tokenizer（分词）               │
│  ["I", "like", "cats"]                  │
│       ↓ Token Filter（词项过滤）        │
│  ["i", "like", "cat"]  ← 小写化、单复数  │
└────────────────────────────────────────┘
```

| 组件 | 作用 | 示例 |
|---|---|---|
| **Character Filter** | 原始文本预处理 | 去除 HTML、替换字符 |
| **Tokenizer** | 将文本切分成词条 | 按空格、按标点切分 |
| **Token Filter** | 对词条进行再加工 | 转小写、去停用词、词干提取 |

---

## 三、内置分词器

### 3.1 standard（默认）

ES 默认分词器，按单词边界切分 + 小写化：

```json
POST /_analyze
{
  "analyzer": "standard",
  "text": "I Love Elasticsearch 123"
}
// 结果: ["i", "love", "elasticsearch", "123"]
```

### 3.2 常用内置分词器

| 分词器 | 能力 | 适用场景 |
|---|---|---|
| `standard` | Unicode 标准分词 + 小写 | 英文通用 |
| `simple` | 非字母字符分割 + 小写 | 简单英文 |
| `whitespace` | 按空格分割 | 精确空格控制 |
| `stop` | standard + 去停用词 | 去除 a/the/is |
| `keyword` | 不分词，原样输出 | ID、邮箱、精确匹配 |
| `pattern` | 正则表达式分割 | 自定义分割规则 |

---

## 四、IK 中文分词器（必备）

### 4.1 为什么需要 IK

中文不像英文有空格分割，standard 分词器会按字拆分：

```
standard: "计算机专业" → ["计", "算", "机", "专", "业"]
IK:       "计算机专业" → ["计算机", "专业"]
```

### 4.2 安装 IK 分词器

```bash
# Docker 版
docker exec -it es bash
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.0/elasticsearch-analysis-ik-7.17.0.zip
# 重启 ES
docker restart es
```

```bash
# Linux 版
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.0/elasticsearch-analysis-ik-7.17.0.zip
```

### 4.3 IK 两种模式

| 模式 | 分词器名 | 策略 | 示例 |
|---|---|---|---|
| **ik_smart** | `ik_smart` | 粗粒度切分，最少切分 | "中华人民共和国" → ["中华人民共和国"] |
| **ik_max_word** | `ik_max_word` | 细粒度切分，穷尽词库 | "中华人民共和国" → ["中华人民共和国", "中华", "华人", "人民", "共和国"] |

```json
// 测试 ik_smart
POST /_analyze
{
  "analyzer": "ik_smart",
  "text": "计算机专业学生学习Java"
}
// ["计算机", "专业", "学生", "学习", "Java"]

// 测试 ik_max_word
POST /_analyze
{
  "analyzer": "ik_max_word",
  "text": "计算机专业学生学习Java"
}
// ["计算机", "计算", "算机", "专业", "学生", "学习", "Java"]
```

### 4.4 自定义词典

```bash
# 进入 IK 配置目录
cd {ES_HOME}/config/analysis-ik/

# 创建自定义词典文件
vi custom/mydict.dic
```

```
# mydict.dic -- 每行一个词
张三丰
计算机专业
高并发
微服务
```

```xml
<!-- IKAnalyzer.cfg.xml 配置自定义词典 -->
<properties>
    <entry key="ext_dict">custom/mydict.dic</entry>
    <entry key="ext_stopwords">custom/mystop.dic</entry>
</properties>
```

### 4.5 热更新词典（生产必备）

```xml
<!-- 远程词典配置，支持热更新 -->
<properties>
    <entry key="remote_ext_dict">http://your-server/config/ik_dict.dic</entry>
    <entry key="remote_ext_stopwords">http://your-server/config/ik_stop.dic</entry>
</properties>
```

- IK 每分钟检查远程词典的变化
- 检测到变化后自动重新加载，无需重启

---

## 五、拼音分词器

### 5.1 安装

```bash
# Docker
docker exec -it es bash
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v7.17.0/elasticsearch-analysis-pinyin-7.17.0.zip
docker restart es
```

### 5.2 使用

```json
PUT /goods_index
{
  "settings": {
    "analysis": {
      "analyzer": {
        "pinyin_analyzer": {
          "tokenizer": "ik_max_word",
          "filter": ["pinyin_filter"]
        }
      },
      "filter": {
        "pinyin_filter": {
          "type": "pinyin",
          "keep_full_pinyin": true,
          "keep_joined_full_pinyin": true
        }
      }
    }
  }
}
```

搜索"zhangsan"即可找到"张三"。

---

## 六、自定义分词器实战

```json
PUT /my_index
{
  "settings": {
    "analysis": {
      "char_filter": {
        "my_char_filter": {
          "type": "mapping",
          "mappings": ["& => and", "< => "]  // 替换特殊符号
        }
      },
      "filter": {
        "my_stop": {
          "type": "stop",
          "stopwords": ["的", "了", "在", "是"]  // 自定义停用词
        }
      },
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "char_filter": ["html_strip", "my_char_filter"],
          "tokenizer": "ik_max_word",
          "filter": ["lowercase", "my_stop"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title": { "type": "text", "analyzer": "my_analyzer" }
    }
  }
}
```

---

## 七、使用场景建议

| 字段类型 | 分词器 | 原因 |
|---|---|---|
| 商品标题 | `ik_max_word` | 最大限度匹配搜索词 |
| 分类/标签 | `ik_smart` | 精准匹配 |
| 用户昵称 | `ik_smart` + `pinyin` | 支持拼音搜索 |
| 文章正文 | `ik_max_word` | 全文检索 |
| ID/邮箱/Url | 不分词 (`keyword`) | 精确匹配 |

---

## 八、面试核心要点

1. **standard vs IK 区别？** standard 按字拆分中文，IK 按词拆分
2. **ik_smart vs ik_max_word？** smart 最少切分（精准），max_word 最多切分（召回率高）
3. **自定义词典在哪配？** `IKAnalyzer.cfg.xml`，远程词典支持热更新
4. **停用词是什么？** 搜索中无意义的词（的/了/在），过滤掉减少索引大小
5. **修改分词的生效范围？** 只对新文档生效，已索引文档需 reindex

---

## 九、极简总结

```
standard = 字拆分，中文不可用
IK = 词拆分，中文搜索引擎标配
ik_smart = 粗粒度（精准）
ik_max_word = 细粒度（召回率高）
远程词典 = 热更新，不停机
```
