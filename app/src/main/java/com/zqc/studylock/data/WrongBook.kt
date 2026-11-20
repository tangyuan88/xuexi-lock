package com.zqc.studylock.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 错题本条目 */
data class WrongEntry(
    val question: Question,
    var times: Int = 1,       // 错误次数
    var rightTimes: Int = 0,  // 再次答对次数
    var lastAt: Long = System.currentTimeMillis()
)

/**
 * 错题本：持久化到 wrong_questions.json。
 * 连续答对 [RIGHT_TO_CLEAR] 次后自动移除；错误次数越多，复习时出现权重越高。
 */
class WrongBook(context: Context) {
    private val file = File(context.filesDir, "wrong_questions.json")
    private var entries: MutableMap<String, WrongEntry> = LinkedHashMap()

    companion object {
        const val RIGHT_TO_CLEAR = 2
        private fun key(q: Question) = q.stem.trim()
    }

    init { load() }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val arr = JSONArray(file.readText())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val q = Question.fromJson(o.optJSONObject("q") ?: continue) ?: continue
                entries[key(q)] = WrongEntry(
                    question = q,
                    times = o.optInt("times", 1),
                    rightTimes = o.optInt("right", 0),
                    lastAt = o.optLong("at", System.currentTimeMillis())
                )
            }
        }
    }

    private fun save() {
        val arr = JSONArray()
        entries.values.forEach { e ->
            arr.put(JSONObject().apply {
                put("q", e.question.toJson())
                put("times", e.times)
                put("right", e.rightTimes)
                put("at", e.lastAt)
            })
        }
        file.writeText(arr.toString())
    }

    /** 答错：新增或累加错误次数，重置连对计数 */
    fun addWrong(q: Question) {
        val k = key(q)
        val e = entries[k]
        if (e == null) entries[k] = WrongEntry(q, 1, 0)
        else { e.times++; e.rightTimes = 0; e.lastAt = System.currentTimeMillis() }
        save()
    }

    /** 答对：累加连对次数，达到阈值则移出错题本 */
    fun addRight(q: Question) {
        val k = key(q)
        val e = entries[k] ?: return
        e.rightTimes++
        e.lastAt = System.currentTimeMillis()
        if (e.rightTimes >= RIGHT_TO_CLEAR) entries.remove(k)
        save()
    }

    fun all(): List<WrongEntry> = entries.values.sortedByDescending { it.times }

    /** 按错误次数加权随机取题（错误越多越容易出现） */
    fun weighted(size: Int): List<Question> {
        val pool = mutableListOf<Question>()
        all().forEach { e -> repeat(e.times.coerceAtMost(5)) { pool.add(e.question) } }
        return pool.shuffled().distinctBy { it.stem }.take(size)
    }

    fun clear() { entries.clear(); save() }
    fun size() = entries.size
}
