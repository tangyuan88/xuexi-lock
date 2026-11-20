package com.zqc.studylock.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 配置存储：对应原应用 SharedPreferences（文件名 study_lock）中的全部键。
 * 键名与原 DEX 中 K_* 常量保持一致，便于数据迁移。
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREF_NAME = "study_lock"

        // 局域网 / 令牌
        const val K_LAN_ON = "lan_on_v1"
        const val K_LAN_TOKEN = "lan_token_v1"
        const val K_HOST = "K_HOST"

        // 家长密码 / 严格模式
        const val K_PIN = "K_PIN"
        const val DEFAULT_PIN = "0000"

        // 学习时长 / 答题
        const val K_WATCH = "K_WATCH"
        const val K_QUIZ_COUNT = "quiz_count_v1"
        const val K_STRICT = "K_STRICT"

        // 休息提醒
        const val K_REST_ON = "rest_on_v1"
        const val K_REST_SEC = "rest_sec_v1"
        const val K_REST_WATCH = "rest_watch_v1"
        const val K_REST_END = "rest_end_at_v1"

        // 定时计划
        const val K_SCHED_ON = "sched_on_v1"
        const val K_SCHED_DAYS = "sched_days_v1"
        const val K_SCHED_START = "sched_start_v1"
        const val K_SCHED_END = "sched_end_v1"

        // 语音播报
        const val K_VOICE_ON = "voice_on"
        const val K_TTS_VOICE = "K_TTS_VOICE"
        const val K_READ_QUESTION = "read_question_v1"
        const val K_AZ_KEY = "K_AZ_KEY"
        const val K_AZ_REGION = "K_AZ_REGION"
        const val K_CHEER = "cheer_mode_v1"
        const val K_VOICE_BOY = "K_VOICE_BOY"

        // 题库 / 错题
        const val K_SUBJECTS_V2 = "subjects_v2"
        const val K_WRONG_WAIT = "wrong_wait_v1"
        const val K_WRONG_TIMES = "K_WRONG_TIMES"
        const val K_CUSTOM_ICON = "custom_icon_v1"
        const val K_CUSTOM_LABEL = "custom_label_v1"
        const val K_PINYIN_ON = "pinyin_on_v1"

        // 锁定会话持久化：进程被系统回收/待机唤醒后据此恢复锁定
        const val K_LOCK_ACTIVE = "lock_active_v1"
        const val K_LOCK_STARTED_AT = "lock_started_at_v1"
        const val K_LOCK_SOLVED = "lock_solved_v1"

        const val TOKEN_TTL_MS = 5 * 60 * 1000L
    }

    // ---- 泛型读写 ----
    fun getString(key: String, def: String = "") = sp.getString(key, def) ?: def
    fun putString(key: String, value: String) = sp.edit { putString(key, value) }
    fun getInt(key: String, def: Int = 0) = sp.getInt(key, def)
    fun putInt(key: String, value: Int) = sp.edit { putInt(key, value) }
    fun getLong(key: String, def: Long = 0L) = sp.getLong(key, def)
    fun putLong(key: String, value: Long) = sp.edit { putLong(key, value) }
    fun getBool(key: String, def: Boolean = false) = sp.getBoolean(key, def)
    fun putBool(key: String, value: Boolean) = sp.edit { putBoolean(key, value) }
    fun has(key: String) = sp.contains(key)
    fun remove(key: String) = sp.edit { remove(key) }

    // ---- 家长密码 ----
    val pin: String get() = getString(K_PIN, DEFAULT_PIN)
    val pinSet: Boolean get() = has(K_PIN)
    fun setPin(newPin: String) = putString(K_PIN, newPin)

    /** 校验并修改密码：oldPin 为空表示首次设置。返回 false 表示原密码错误。 */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (pinSet && oldPin != pin) return false
        if (!newPin.matches(Regex("\\d{4}"))) return false
        setPin(newPin)
        return true
    }

    // ---- 局域网令牌 ----
    var lanToken: String
        get() = getString(K_LAN_TOKEN)
        set(value) = putString(K_LAN_TOKEN, value)

    var lanOn: Boolean
        get() = getBool(K_LAN_ON, true)
        set(value) = putBool(K_LAN_ON, value)

    /** 生成 8 位数字验证码，并记录生成时间用于 TTL 校验 */
    fun newLanToken(): String {
        val token = buildString {
            repeat(8) { append((0..9).random()) }
        }
        lanToken = token
        putLong("lan_token_at", System.currentTimeMillis())
        return token
    }

    fun isTokenValid(t: String?): Boolean {
        if (t.isNullOrBlank()) return false
        if (t != lanToken) return false
        val at = getLong("lan_token_at", 0L)
        return System.currentTimeMillis() - at <= TOKEN_TTL_MS
    }

    // ---- 学习 / 休息 ----
    var watchMinutes: Int
        get() = getInt(K_WATCH, 30)
        set(value) = putInt(K_WATCH, value.coerceIn(1, 600))

    var quizCount: Int
        get() = getInt(K_QUIZ_COUNT, 5)
        set(value) = putInt(K_QUIZ_COUNT, value.coerceIn(1, 50))

    var restOn: Boolean
        get() = getBool(K_REST_ON, false)
        set(value) = putBool(K_REST_ON, value)

    var restSeconds: Int
        get() = getInt(K_REST_SEC, 600)
        set(value) = putInt(K_REST_SEC, value.coerceIn(60, 7200))

    var restWatchMinutes: Int
        get() = getInt(K_REST_WATCH, 40)
        set(value) = putInt(K_REST_WATCH, value.coerceIn(5, 300))

    var restEndAt: Long
        get() = getLong(K_REST_END, 0L)
        set(value) = putLong(K_REST_END, value)

    // ---- 定时计划 ----
    var scheduleOn: Boolean
        get() = getBool(K_SCHED_ON, false)
        set(value) = putBool(K_SCHED_ON, value)

    var scheduleDays: String
        get() = getString(K_SCHED_DAYS, "12345")
        set(value) = putString(K_SCHED_DAYS, value)

    var scheduleStart: String
        get() = getString(K_SCHED_START, "18:00")
        set(value) = putString(K_SCHED_START, value)

    var scheduleEnd: String
        get() = getString(K_SCHED_END, "21:00")
        set(value) = putString(K_SCHED_END, value)

    // ---- 语音 ----
    var voiceOn: Boolean
        get() = getBool(K_VOICE_ON, true)
        set(value) = putBool(K_VOICE_ON, value)

    var ttsVoice: String
        get() = getString(K_TTS_VOICE, "zh-CN-XiaoxiaoNeural")
        set(value) = putString(K_TTS_VOICE, value)

    var azureKey: String
        get() = getString(K_AZ_KEY)
        set(value) = putString(K_AZ_KEY, value)

    var azureRegion: String
        get() = getString(K_AZ_REGION, "eastasia")
        set(value) = putString(K_AZ_REGION, value)

    var readQuestionOn: Boolean
        get() = getBool(K_READ_QUESTION, false)
        set(value) = putBool(K_READ_QUESTION, value)

    var cheerMode: String
        get() = getString(K_CHEER, "on")
        set(value) = putString(K_CHEER, value)

    // ---- 锁定会话持久化 ----
    /** 是否处于锁定中（进程被杀/待机唤醒后据此恢复） */
    var lockActive: Boolean
        get() = getBool(K_LOCK_ACTIVE, false)
        set(value) = putBool(K_LOCK_ACTIVE, value)

    /** 本次锁定开始时刻，用于恢复剩余时长 */
    var lockStartedAt: Long
        get() = getLong(K_LOCK_STARTED_AT, 0L)
        set(value) = putLong(K_LOCK_STARTED_AT, value)

    /** 已连续答对题数 */
    var lockSolved: Int
        get() = getInt(K_LOCK_SOLVED, 0)
        set(value) = putInt(K_LOCK_SOLVED, value)
}
