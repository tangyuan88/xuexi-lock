package com.zqc.studylock.data

import org.json.JSONArray
import org.json.JSONObject

/** 科目枚举：与原应用 SUBJECT_NAMES 对应（0 语文 1 数学 2 英语 ...） */
object Subjects {
    val NAMES = arrayOf("语文", "数学", "英语", "科学", "道德与法治", "其他")
    fun name(i: Int) = NAMES.getOrElse(i) { "其他" }
}

/**
 * 一道题目。兼容两种题库格式：
 *  - 新格式：{q, options:[...], answer:0, py?, explain?}
 *  - 旧格式：{question, optiona..optiond, answer}
 */
data class Question(
    val stem: String,
    val options: List<String>,
    val answer: Int,                 // 正确选项下标 0..3
    val pinyin: String = "",
    val explain: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("q", stem)
        put("options", JSONArray(options))
        put("answer", answer)
        if (pinyin.isNotEmpty()) put("py", pinyin)
        if (explain.isNotEmpty()) put("explain", explain)
    }

    companion object {
        fun fromJson(o: JSONObject): Question? {
            val stem = o.optString("q").ifEmpty { o.optString("question") }
            if (stem.isBlank()) return null

            val options = mutableListOf<String>()
            o.optJSONArray("options")?.let { arr ->
                for (i in 0 until arr.length()) options.add(arr.optString(i))
            }
            if (options.isEmpty()) {
                // 旧格式 optiona/optionb/optionc/optiond
                for (k in listOf("optiona", "optionb", "optionc", "optiond")) {
                    val v = o.optString(k)
                    if (v.isNotEmpty()) options.add(v)
                }
            }
            if (options.size < 2) return null

            var answer = o.optInt("answer", -1)
            if (answer < 0 || answer >= options.size) {
                val letter = o.optString("answer", "").trim().uppercase()
                answer = when (letter) { "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> -1 }
            }
            if (answer !in options.indices) return null

            return Question(
                stem = stem,
                options = options,
                answer = answer,
                pinyin = o.optString("py").ifEmpty { o.optString("pinyin") },
                explain = o.optString("explain")
            )
        }
    }
}
