package com.zqc.studylock.data

import org.json.JSONObject

/** 题库包元信息，对应 index.json / legacy_index.json 中的条目 */
data class PackInfo(
    val id: String,
    val grade: Int = 0,
    val subject: Int = 0,
    val semester: Int = 0,
    val count: Int = 0,
    val bytes: Long = 0L,
    var enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("grade", grade)
        put("subject", subject)
        put("semester", semester)
        put("count", count)
        put("bytes", bytes)
        put("on", enabled)
    }

    val title: String
        get() = buildString {
            if (grade > 0) append("${grade}年级")
            append(Subjects.name(subject))
            when (semester) {
                1 -> append("·上册"); 2 -> append("·下册"); 3 -> append("·专项")
            }
        }

    companion object {
        fun fromJson(o: JSONObject): PackInfo {
            val id = o.optString("id")
            // 旧版通过 .json.off 后缀表示禁用
            val off = id.endsWith(".off") || !o.optBoolean("on", true)
            return PackInfo(
                id = id.removeSuffix(".off"),
                grade = o.optInt("grade", 0),
                subject = o.optInt("subject", 0),
                semester = o.optInt("semester", 0),
                count = o.optInt("count", 0),
                bytes = o.optLong("bytes", 0L),
                enabled = !off
            )
        }
    }
}
