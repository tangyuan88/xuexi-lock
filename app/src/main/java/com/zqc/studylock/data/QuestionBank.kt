package com.zqc.studylock.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 题库仓库：管理题库包（ packs/<id>.json ）与安装索引（ packs/index.json ）。
 * 支持从 zip 导入、切换启用状态、删除、以及合并抽取题目。
 */
class QuestionBank(private val context: Context) {

    private val packsDir: File
        get() = File(context.filesDir, "packs").apply { if (!exists()) mkdirs() }

    private val indexFile: File get() = File(packsDir, "index.json")
    private var packs: MutableList<PackInfo> = mutableListOf()

    init {
        loadIndex()
    }

    private fun loadIndex() {
        packs.clear()
        if (!indexFile.exists()) return
        runCatching {
            val root = JSONObject(indexFile.readText())
            val arr = root.optJSONArray("packs") ?: JSONArray()
            for (i in 0 until arr.length()) {
                packs.add(PackInfo.fromJson(arr.getJSONObject(i)))
            }
        }
    }

    private fun saveIndex(rev: Int = (lastRev() + 1)) {
        val arr = JSONArray()
        packs.forEach { arr.put(it.toJson()) }
        val root = JSONObject().apply {
            put("version", 5)
            put("rev", rev)
            put("note", "学习锁题库包索引")
            put("packs", arr)
        }
        indexFile.writeText(root.toString())
    }

    private fun lastRev(): Int = runCatching {
        JSONObject(indexFile.readText()).optInt("rev", 0)
    }.getOrDefault(0)

    fun allPacks(): List<PackInfo> = packs.toList()
    fun enabledPacks(): List<PackInfo> = packs.filter { it.enabled }

    fun packFile(id: String): File = File(packsDir, "$id.json")

    /** 读取某个包内的题目列表 */
    fun questionsOf(id: String): List<Question> {
        val f = packFile(id)
        if (!f.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { Question.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /** 启用 / 禁用一个包 */
    fun toggle(id: String, on: Boolean): Boolean {
        val p = packs.find { it.id == id } ?: return false
        p.enabled = on
        saveIndex()
        return true
    }

    /** 删除若干包（含文件） */
    fun delete(ids: Collection<String>): Int {
        var n = 0
        for (id in ids) {
            if (packs.removeAll { it.id == id }) {
                packFile(id).delete()
                n++
            }
        }
        if (n > 0) saveIndex()
        return n
    }

    /**
     * 从 zip 导入题库包。zip 内应含 index.json，以及其中声明的 <id>.json 文件。
     * 返回成功导入的包数量。
     */
    fun importZip(zipPath: String): Int {
        val result = mutableListOf<PackInfo>()
        runCatching {
            java.util.zip.ZipInputStream(File(zipPath).inputStream()).use { zis ->
                var entry = zis.nextEntry
                val buffers = mutableMapOf<String, String>()
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        if (name == "index.json" || name.endsWith(".json")) {
                            buffers[name] = zis.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                    entry = zis.nextEntry
                }
                val idx = buffers["index.json"] ?: return@use
                val arr = JSONObject(idx).optJSONArray("packs") ?: return@use
                for (i in 0 until arr.length()) {
                    val info = PackInfo.fromJson(arr.getJSONObject(i))
                    val body = buffers["${info.id}.json"] ?: continue
                    // 校验 JSON 合法且非空
                    val qarr = JSONArray(body)
                    if (qarr.length() == 0) continue
                    packFile(info.id).writeText(body)
                    result.add(info.copy(count = qarr.length(), enabled = true))
                }
            }
        }.onFailure { return 0 }

        if (result.isEmpty()) return 0
        val ids = result.map { it.id }.toSet()
        packs.removeAll { it.id in ids }
        packs.addAll(result)
        saveIndex()
        return result.size
    }

    /** 汇总所有启用包中的题目，用于随机抽题 */
    fun allEnabledQuestions(): List<Question> =
        enabledPacks().flatMap { questionsOf(it.id) }

    /** 导出当前题库为 zip，返回生成的文件 */
    fun exportZip(outDir: File): File {
        val out = File(outDir, "studylock-packs.zip")
        java.util.zip.ZipOutputStream(out.outputStream()).use { zos ->
            fun put(name: String, bytes: ByteArray) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
            val arr = JSONArray()
            packs.forEach { arr.put(it.toJson()) }
            put("index.json", JSONObject().apply {
                put("version", 5); put("rev", lastRev()); put("packs", arr)
            }.toString().toByteArray())
            packs.forEach { p ->
                val f = packFile(p.id)
                if (f.exists()) put("${p.id}.json", f.readBytes())
            }
        }
        return out
    }
}
