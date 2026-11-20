# 题库包格式规范

题库以 `.zip` 包分发。原应用自 V2.5.2 起不再内置题库，用户需自行导入。

## Zip 包结构

```
题库包.zip
├── index.json          # 必需：包索引
├── g1_math.json        # 题目文件，文件名必须与 index.json 中的 id 对应
├── g2_math.json
└── ...
```

## index.json

```json
{
  "version": 5,
  "rev": 1,
  "note": "我的题库",
  "packs": [
    {
      "id": "g1_math",
      "grade": 1,
      "subject": 1,
      "semester": 0,
      "count": 2,
      "bytes": 512
    }
  ]
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `version` | int | 索引格式版本，当前为 5 |
| `rev` | int | 修订号，导入时用于同步 |
| `note` | string | 备注 |
| `packs` | array | 题库包列表 |

每个包条目：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 包标识，对应 `<id>.json` 文件名，须唯一 |
| `grade` | int | 年级：0=不限，1–6=小学，7–9=初中 |
| `subject` | int | 科目：0=语文，1=数学，2=英语，3=科学，4=道法，5=其他 |
| `semester` | int | 0=全年，1=上册，2=下册，3=专项 |
| `count` | int | 题目数量 |
| `bytes` | int | 题目文件字节数（参考） |

## 题目文件 `<id>.json`

是一个 JSON 数组，每项为一道题。支持两种格式：

### 新格式（推荐）

```json
[
  {
    "q": "1 + 1 等于几？",
    "options": ["1", "2", "3", "4"],
    "answer": 1,
    "py": "yī jiā yī děng yú jǐ",
    "explain": "基础加法"
  }
]
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `q` | string | 题干（必填） |
| `options` | string[] | 选项，至少 2 个（必填） |
| `answer` | int | 正确答案下标，从 0 开始（必填） |
| `py` | string | 拼音标注（可选，用于低年级） |
| `explain` | string | 答案解析（可选） |

### 旧格式（兼容）

```json
[
  {
    "question": "1 + 1 等于几？",
    "optiona": "1", "optionb": "2", "optionc": "3", "optiond": "4",
    "answer": "B"
  }
]
```

旧格式字段 `question` / `optiona`–`optiond` / `answer`（字母 A–D 或数字）会被自动转换为新格式。

## 导入方式

1. 把 zip 放进设备的「下载」目录，在控制端调用 `/api/zips` 查看，再调用 `/api/import-local` 导入。
2. 或直接在控制端调用 `/api/upload` 以 `multipart/form-data` 上传。

导入时会校验：`index.json` 存在、`packs` 中每个 `id` 都有对应的 `<id>.json`、题目 JSON 合法且非空。任一包校验失败会跳过该包，其余包照常导入。

## 示例

`sample-question-bank/` 目录包含一个从原应用资产中提取的 `legacy_index.json`（列出各年级/科目包及题目数），可作为编写自己索引的参考。注意该索引对应的题目文件未随原应用分发，需自行准备。
