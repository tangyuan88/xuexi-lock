package com.zqc.studylock.api

import com.zqc.studylock.util.Prefs
import org.json.JSONArray
import org.json.JSONObject

/** 控制端 /api/status 与 /api/get 返回的配置快照 */
data class CfgState(
    val version: String = "2.6.5",
    val pinSet: Boolean = false,
    val lanOn: Boolean = true,
    val watchMin: Int = 30,
    val quizCount: Int = 5,
    val restOn: Boolean = false,
    val restSec: Int = 600,
    val restWatchMin: Int = 40,
    val scheduleOn: Boolean = false,
    val scheduleDays: String = "12345",
    val scheduleStart: String = "18:00",
    val scheduleEnd: String = "21:00",
    val ttsVoice: String = "zh-CN-XiaoxiaoNeural",
    val azureKey: String = "",
    val azureRegion: String = "eastasia",
    val readQuestionOn: Boolean = false,
    val voices: List<String> = DEFAULT_VOICES,
    val strict: Boolean = false,
    val running: Boolean = false,
    val resting: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("pin_set", pinSet)
        put("lan_on", lanOn)
        put("watch_min", watchMin)
        put("quiz_count", quizCount)
        put("rest_on", restOn)
        put("rest_sec", restSec)
        put("rest_watch_min", restWatchMin)
        put("schedule_on", scheduleOn)
        put("schedule_days", scheduleDays)
        put("schedule_start", scheduleStart)
        put("schedule_end", scheduleEnd)
        put("tts_voice", ttsVoice)
        put("azure_key", azureKey)
        put("azure_region", azureRegion)
        put("read_question_on", readQuestionOn)
        put("voices", JSONArray(voices))
        put("strict", strict)
        put("running", running)
        put("resting", resting)
    }

    companion object {
        val DEFAULT_VOICES = listOf(
            "zh-CN-XiaoxiaoNeural",
            "zh-CN-YunxiNeural",
            "zh-CN-XiaoyiNeural",
            "zh-CN-YunjianNeural"
        )

        fun from(prefs: Prefs, running: Boolean = false, resting: Boolean = false) = CfgState(
            pinSet = prefs.pinSet,
            lanOn = prefs.lanOn,
            watchMin = prefs.watchMinutes,
            quizCount = prefs.quizCount,
            restOn = prefs.restOn,
            restSec = prefs.restSeconds,
            restWatchMin = prefs.restWatchMinutes,
            scheduleOn = prefs.scheduleOn,
            scheduleDays = prefs.scheduleDays,
            scheduleStart = prefs.scheduleStart,
            scheduleEnd = prefs.scheduleEnd,
            ttsVoice = prefs.ttsVoice,
            azureKey = prefs.azureKey,
            azureRegion = prefs.azureRegion,
            readQuestionOn = prefs.readQuestionOn,
            strict = prefs.getBool(Prefs.K_STRICT, false),
            running = running,
            resting = resting
        )
    }
}

/** /api/status 的运行时状态 */
data class RuntimeStatus(
    val running: Boolean = false,
    val resting: Boolean = false,
    val solvedCount: Int = 0,
    val leftMs: Long = 0L,
    val version: String = "2.6.5"
) {
    fun toJson() = JSONObject().apply {
        put("running", running); put("resting", resting)
        put("solved_count", solvedCount); put("left_ms", leftMs)
        put("version", version)
    }
}
