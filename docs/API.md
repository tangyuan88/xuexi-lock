# 局域网 API 文档

控制端（浏览器）与设备端（Android）通过局域网 HTTP 通信。服务默认监听端口 **8899**，仅用于家庭内网。

## 通用约定

- 所有请求/响应体均为 `application/json; charset=utf-8`（文件上传除外）。
- 除 `/`、`/favicon.ico`、`/api/status` 外，所有端点必须携带查询参数 `t=<8 位验证码>`。
- 验证码由设备端生成，有效期 5 分钟（TOKEN_TTL_MS = 300000），过期返回 `403`。
- 成功响应包含 `"ok": true`；失败返回 `{"ok":false,"error":"..."}` 并配相应 HTTP 状态码。
- 支持 CORS（`Access-Control-Allow-Origin: *`），缓存被禁用。

## GET `/`

返回内嵌的家长控制端页面（HTML）。源码见 `controller/index.html`。

## GET `/api/status`

获取当前运行状态。**无需鉴权**（控制端用它探测设备是否在线）。

```json
{
  "ok": true,
  "running": false,
  "resting": false,
  "solved_count": 3,
  "left_ms": 120000,
  "version": "2.6.5"
}
```

## GET `/api/get?t=...`

读取当前全部配置。**需鉴权**。

```json
{
  "ok": true,
  "cfg": {
    "version": "2.6.5",
    "pin_set": true,
    "lan_on": true,
    "watch_min": 30,
    "quiz_count": 5,
    "rest_on": true,
    "rest_sec": 600,
    "rest_watch_min": 40,
    "schedule_on": false,
    "schedule_days": "12345",
    "schedule_start": "18:00",
    "schedule_end": "21:00",
    "tts_voice": "zh-CN-XiaoxiaoNeural",
    "azure_key": "",
    "azure_region": "eastasia",
    "read_question_on": false,
    "voices": ["zh-CN-XiaoxiaoNeural", "..."],
    "strict": false,
    "running": false,
    "resting": false
  }
}
```

> `pin_set` 只表示是否设置过密码，**永不返回密码本身**。

## POST `/api/set?t=...`

修改配置。**需鉴权**。请求体可包含以下任意字段（仅传需要修改的）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `watch_min` | int | 学习时长（分钟），1–600 |
| `quiz_count` | int | 解锁需答对题数，1–50 |
| `rest_on` | bool | 是否开启休息提醒 |
| `rest_sec` | int | 休息时长（秒），60–7200 |
| `rest_watch_min` | int | 学习多久触发休息（分钟） |
| `schedule_on` | bool | 是否开启定时计划 |
| `schedule_days` | string | 启用星期，如 `"12345"`（周一至周五） |
| `schedule_start` / `schedule_end` | string | 时间段，如 `"18:00"` / `"21:00"` |
| `tts_voice` | string | 语音名称 |
| `azure_key` / `azure_region` | string | Azure 语音密钥与区域 |
| `read_question_on` | bool | 是否朗读题目 |
| `lan_on` | bool | 是否开启局域网服务 |
| `strict` | bool | 严格模式 |
| `wrong_clear` | bool | 传 `true` 清空错题本 |
| `pin` + `old_pin` | string | 修改家长密码；首次设置时 `old_pin` 留空 |
| `renew_token` | any | 传任意值即刷新验证码 |

响应返回更新后的完整 `cfg` 对象。`pin` 校验失败返回 `400`。

## POST `/api/test?t=...`

触发一次动作测试（用于控制端「测试」按钮）。**需鉴权**。

```json
{ "kind": "lock" }
```

`kind` 取值：`lock`（弹出锁定界面）、`rest`（触发休息提醒）、`tts`（播放测试语音）。
响应：`{"ok": true, "message": "已触发锁定界面"}`。

## GET `/api/packs?t=...`

列出题库包。**需鉴权**。

```json
{ "ok": true, "packs": [
  { "id": "g1_math", "grade": 1, "subject": 1, "semester": 0,
    "count": 275, "bytes": 40664, "on": true }
] }
```

## POST `/api/pack?t=...`

启用/停用一个题库包。**需鉴权**。

```json
{ "id": "g1_math", "on": false }
```

包不存在返回 `404`。

## POST `/api/pack-delete?t=...`

删除一个或多个题库包（含文件）。**需鉴权**。

```json
{ "ids": ["g1_math", "g2_en"] }
```

响应：`{"ok": true, "deleted": 2}`。

## GET `/api/zips?t=...`

列出设备「下载」目录中的所有 `.zip` 文件（供导入选择）。**需鉴权**。

```json
{ "ok": true, "zips": ["题库.zip", "studylock-packs.zip"] }
```

## POST `/api/import-local?t=...`

从「下载」目录导入指定的题库 zip。**需鉴权**。

```json
{ "name": "题库.zip" }
```

响应：`{"ok": true, "imported": 3}`；格式错误返回 `400`。

## POST `/api/upload?t=...`

上传并导入题库 zip，使用 `multipart/form-data`，文件字段名 `file`。**需鉴权**。
响应同 `/api/import-local`。

## GET `/api/export?t=...`

导出当前全部题库包为 zip 下载。**需鉴权**。
响应头 `Content-Disposition: attachment; filename="studylock-packs.zip"`。

## 错误码

| HTTP | 含义 |
| --- | --- |
| 400 | 参数错误 / 格式不正确 |
| 403 | 验证码无效或已过期 |
| 404 | 资源不存在 / 端点不存在 |
| 500 | 服务端内部异常 |
