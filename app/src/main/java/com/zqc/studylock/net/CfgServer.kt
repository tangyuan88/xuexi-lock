package com.zqc.studylock.net

import android.content.Context
import com.zqc.studylock.api.CfgState
import com.zqc.studylock.api.RuntimeStatus
import com.zqc.studylock.data.QuestionBank
import com.zqc.studylock.data.WrongBook
import com.zqc.studylock.util.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 控制端业务路由：实现家长控制端 HTML 调用的全部端点。
 * 所有写操作与敏感读操作都需携带有效 token（?t=8位验证码）。
 */
class CfgServer(
    private val context: Context,
    private val prefs: Prefs,
    private val bank: QuestionBank,
    private val wrongBook: WrongBook,
    private val actions: Actions
) {

    /** 由 App 注入的动作回调，解耦网络层与 UI/服务层 */
    interface Actions {
        fun isRunning(): Boolean
        fun isResting(): Boolean
        fun leftMs(): Long
        fun solvedCount(): Int
        fun test(kind: String): String
        fun onConfigChanged()
        fun refreshLanToken(): String
    }

    private val panelHtml: String by lazy {
        runCatching { context.assets.open("panel.html").bufferedReader().use { it.readText() } }
            .getOrDefault(DEFAULT_PANEL_HTML)
    }

    private val server = HttpServer(LAN_PORT, Router { req -> route(req) })

    companion object {
        const val LAN_PORT = 8899
    }

    fun start() = server.start()
    fun stop() = server.stop()
    fun isRunning() = server.isRunning()

    private fun ok(obj: JSONObject = JSONObject()): HttpResponse =
        HttpResponse.json(obj.put("ok", true).toString())

    private fun fail(msg: String, code: Int = 400): HttpResponse = HttpResponse.error(code, msg)

    private fun authorized(req: HttpRequest): Boolean =
        prefs.isTokenValid(req.query["t"])

    private fun route(req: HttpRequest): HttpResponse {
        return when {
            req.path == "/" && req.method == "GET" -> HttpResponse.html(panelHtml)
            req.path == "/favicon.ico" -> HttpResponse(404)

            req.path == "/api/status" && req.method == "GET" ->
                ok(RuntimeStatus(
                    running = actions.isRunning(),
                    resting = actions.isResting(),
                    solvedCount = actions.solvedCount(),
                    leftMs = actions.leftMs()
                ).toJson())

            req.path == "/api/get" && req.method == "GET" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                ok(JSONObject().put("cfg", CfgState.from(prefs, actions.isRunning(), actions.isResting()).toJson()))
            }

            req.path == "/api/set" && req.method == "POST" -> handleSet(req)

            req.path == "/api/test" && req.method == "POST" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val body = jsonBody(req)
                val kind = body.optString("kind", "lock")
                val msg = actions.test(kind)
                ok(JSONObject().put("message", msg))
            }

            req.path == "/api/packs" && req.method == "GET" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val arr = JSONArray()
                bank.allPacks().forEach { arr.put(it.toJson()) }
                ok(JSONObject().put("packs", arr))
            }

            req.path == "/api/pack" && req.method == "POST" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val body = jsonBody(req)
                val id = body.optString("id")
                val on = body.optBoolean("on", true)
                if (bank.toggle(id, on)) ok() else fail("题库包不存在: $id", 404)
            }

            req.path == "/api/pack-delete" && req.method == "POST" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val body = jsonBody(req)
                val ids = mutableListOf<String>()
                body.optJSONArray("ids")?.let { arr ->
                    for (i in 0 until arr.length()) ids.add(arr.optString(i))
                }
                ok(JSONObject().put("deleted", bank.delete(ids)))
            }

            req.path == "/api/zips" && req.method == "GET" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                ok(JSONObject().put("zips", JSONArray(listZipFiles().map { it.name })))
            }

            req.path == "/api/import-local" && req.method == "POST" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val body = jsonBody(req)
                val name = body.optString("name")
                val f = File(downloadDir(), name).takeIf { it.exists() }
                    ?: return fail("文件不存在: $name", 404)
                val n = bank.importZip(f.absolutePath)
                if (n > 0) ok(JSONObject().put("imported", n)) else fail("题库包格式不正确或为空")
            }

            req.path == "/api/export" && req.method == "GET" -> {
                if (!authorized(req)) return fail("验证码无效或已过期", 403)
                val f = bank.exportZip(context.cacheDir)
                HttpResponse.file(f.readBytes(), "studylock-packs.zip")
            }

            req.path == "/api/upload" && req.method == "POST" -> handleUpload(req)

            else -> fail("not found", 404)
        }
    }

    private fun handleSet(req: HttpRequest): HttpResponse {
        if (!authorized(req)) return fail("验证码无效或已过期", 403)
        val body = jsonBody(req)

        // 先校验密码，避免在 lambda 中做非局部 return
        if (body.has("pin")) {
            val np = body.optString("pin")
            val op = body.optString("old_pin")
            if (!prefs.changePin(op, np)) return fail("原密码错误或新密码不是 4 位数字")
        }

        val result = runCatching {
            if (body.has("watch_min")) prefs.watchMinutes = body.optInt("watch_min")
            if (body.has("quiz_count")) prefs.quizCount = body.optInt("quiz_count")
            if (body.has("rest_on")) prefs.restOn = body.optBoolean("rest_on")
            if (body.has("rest_sec")) prefs.restSeconds = body.optInt("rest_sec")
            if (body.has("rest_watch_min")) prefs.restWatchMinutes = body.optInt("rest_watch_min")
            if (body.has("schedule_on")) prefs.scheduleOn = body.optBoolean("schedule_on")
            if (body.has("schedule_days")) prefs.scheduleDays = body.optString("schedule_days")
            if (body.has("schedule_start")) prefs.scheduleStart = body.optString("schedule_start")
            if (body.has("schedule_end")) prefs.scheduleEnd = body.optString("schedule_end")
            if (body.has("tts_voice")) prefs.ttsVoice = body.optString("tts_voice")
            if (body.has("azure_key")) prefs.azureKey = body.optString("azure_key")
            if (body.has("azure_region")) prefs.azureRegion = body.optString("azure_region")
            if (body.has("read_question_on")) prefs.readQuestionOn = body.optBoolean("read_question_on")
            if (body.has("lan_on")) prefs.lanOn = body.optBoolean("lan_on")
            if (body.has("strict")) prefs.putBool(Prefs.K_STRICT, body.optBoolean("strict"))
            if (body.has("wrong_clear") && body.optBoolean("wrong_clear")) wrongBook.clear()
            if (body.has("renew_token")) actions.refreshLanToken()
            actions.onConfigChanged()
        }
        if (result.isFailure) return fail(result.exceptionOrNull()?.message ?: "设置失败")
        return ok(CfgState.from(prefs, actions.isRunning(), actions.isResting()).toJson())
    }

    /** 处理 multipart/form-data 文件上传（题库 zip） */
    private fun handleUpload(req: HttpRequest): HttpResponse {
        if (!authorized(req)) return fail("验证码无效或已过期", 403)
        val ct = req.headers["content-type"] ?: ""
        if (!ct.contains("multipart/form-data")) return fail("需要 multipart/form-data")
        val boundary = ct.substringAfter("boundary=").trim().trim('"')
        val raw = req.body.toByteArray(Charsets.ISO_8859_1)
        val marker = "--$boundary".toByteArray(Charsets.ISO_8859_1)
        val parts = splitByte(raw, marker)
        for (part in parts) {
            val headerEnd = indexOfSeq(part, "\r\n\r\n".toByteArray())
            if (headerEnd < 0) continue
            val header = String(part.copyOfRange(0, headerEnd), Charsets.ISO_8859_1)
            val name = Regex("name=\"([^\"]+)\"").find(header)?.groupValues?.get(1) ?: continue
            if (name != "file") continue
            val fileName = Regex("filename=\"([^\"]+)\"").find(header)?.groupValues?.get(1)
                ?.substringAfterLast('\\') ?: "upload.zip"
            var start = headerEnd + 4
            var end = part.size
            if (end - start >= 2 && part[end - 1] == '\n'.code.toByte() && part[end - 2] == '\r'.code.toByte()) {
                end -= 2
            }
            val tmp = File(context.cacheDir, fileName)
            tmp.writeBytes(part.copyOfRange(start, end))
            val n = bank.importZip(tmp.absolutePath)
            tmp.delete()
            return if (n > 0) ok(JSONObject().put("imported", n)) else fail("题库包格式不正确或为空")
        }
        return fail("未找到上传文件")
    }

    private fun jsonBody(req: HttpRequest): JSONObject =
        runCatching { JSONObject(req.body.ifEmpty { "{}" }) }.getOrDefault(JSONObject())

    private fun downloadDir(): File =
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

    private fun listZipFiles(): List<File> =
        (downloadDir().listFiles { f -> f.extension.equals("zip", true) } ?: emptyArray()).toList()

    private fun splitByte(data: ByteArray, marker: ByteArray): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        var start = 0
        while (true) {
            val idx = indexOfSeq(data.copyOfRange(start, data.size), marker)
            if (idx < 0) { out.add(data.copyOfRange(start, data.size)); break }
            out.add(data.copyOfRange(start, start + idx))
            start += idx + marker.size
        }
        return out.filter { it.isNotEmpty() }
    }

    private fun indexOfSeq(data: ByteArray, seq: ByteArray): Int {
        outer@ for (i in 0..data.size - seq.size) {
            for (j in seq.indices) if (data[i + j] != seq[j]) continue@outer
            return i
        }
        return -1
    }

    private val DEFAULT_PANEL_HTML = """
        <!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>学习锁 · 家长控制端</title></head>
        <body><h2>学习锁局域网服务运行中</h2>
        <p>请使用仓库 controller/index.html 中的完整控制端页面。</p>
        <p>请使用完整控制端页面（controller/index.html）。</p></body></html>
    """.trimIndent()
}
