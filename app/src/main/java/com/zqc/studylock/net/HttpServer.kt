package com.zqc.studylock.net

import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** HTTP 请求 */
data class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: String
)

/** HTTP 响应 */
data class HttpResponse(
    val status: Int = 200,
    val contentType: String = "application/json; charset=utf-8",
    val body: ByteArray = ByteArray(0),
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun json(text: String) = HttpResponse(body = text.toByteArray(Charsets.UTF_8))
        fun html(text: String) = HttpResponse(contentType = "text/html; charset=utf-8",
            body = text.toByteArray(Charsets.UTF_8))
        fun file(bytes: ByteArray, name: String) = HttpResponse(
            contentType = "application/zip",
            body = bytes,
            headers = mapOf("Content-Disposition" to "attachment; filename=\"$name\"")
        )
        fun error(code: Int, msg: String) = HttpResponse(status = code,
            body = "{\"ok\":false,\"error\":\"$msg\"}".toByteArray(Charsets.UTF_8))
    }
}

/** 路由处理器 */
fun interface Router {
    fun handle(req: HttpRequest): HttpResponse
}

/**
 * 轻量 HTTP 服务器：基于 ServerSocket 的固定线程池实现，仅满足局域网控制面板需求。
 * 特性：GET/POST、查询参数解析、JSON 体读取、token 校验交由上层路由处理、
 * 单请求超时、shutdown 优雅关闭。
 */
class HttpServer(private val port: Int, private val router: Router) {

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private val pool = Executors.newFixedThreadPool(8)

    fun start() {
        if (!running.compareAndSet(false, true)) return
        pool.execute {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                while (running.get()) {
                    val client = try { serverSocket?.accept() } catch (e: Exception) { null } ?: break
                    pool.execute { handleClient(client) }
                }
            } catch (e: Exception) {
                running.set(false)
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 10_000
            val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)

            // 请求行
            val line = input.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val rawTarget = parts[1]

            // 请求头
            val headers = mutableMapOf<String, String>()
            var h = input.readLine()
            var contentLength = 0
            while (!h.isNullOrEmpty()) {
                val idx = h.indexOf(':')
                if (idx > 0) {
                    val k = h.substring(0, idx).trim().lowercase()
                    val v = h.substring(idx + 1).trim()
                    headers[k] = v
                    if (k == "content-length") contentLength = v.toIntOrNull() ?: 0
                }
                h = input.readLine()
            }

            // 请求体
            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val n = input.read(buf, read, contentLength - read)
                    if (n < 0) break
                    read += n
                }
                String(buf, 0, read)
            } else ""

            val qIndex = rawTarget.indexOf('?')
            val path = if (qIndex >= 0) rawTarget.substring(0, qIndex) else rawTarget
            val query = if (qIndex >= 0) parseQuery(rawTarget.substring(qIndex + 1)) else emptyMap()

            val req = HttpRequest(method, path, query, headers, body)
            val resp = runCatching { router.handle(req) }
                .getOrElse { HttpResponse.error(500, "internal error: ${it.message}") }

            writeResponse(socket.getOutputStream(), resp)
        } catch (_: Exception) {
            // 忽略单个连接的异常，保证服务不中断
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun writeResponse(out: OutputStream, resp: HttpResponse) {
        val reason = when (resp.status) {
            200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"
            404 -> "Not Found"; else -> "Error"
        }
        val head = StringBuilder()
        head.append("HTTP/1.1 ${resp.status} $reason\r\n")
        head.append("Content-Type: ${resp.contentType}\r\n")
        head.append("Content-Length: ${resp.body.size}\r\n")
        head.append("Cache-Control: no-store\r\n")
        head.append("Access-Control-Allow-Origin: *\r\n")
        resp.headers.forEach { (k, v) -> head.append("$k: $v\r\n") }
        head.append("Connection: close\r\n\r\n")
        out.write(head.toString().toByteArray(Charsets.UTF_8))
        out.write(resp.body)
        out.flush()
    }

    private fun parseQuery(qs: String): Map<String, String> = buildMap {
        qs.split("&").forEach { pair ->
            if (pair.isEmpty()) return@forEach
            val kv = pair.split("=", limit = 2)
            val k = URLDecoder.decode(kv[0], "UTF-8")
            val v = URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
            put(k, v)
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        pool.shutdownNow()
    }

    fun isRunning() = running.get()
}
