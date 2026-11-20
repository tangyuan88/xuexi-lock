package com.zqc.studylock.core

import com.zqc.studylock.util.Prefs
import java.util.Calendar

/**
 * 休息提醒时钟：每学习 [watchMin] 分钟提醒休息 [sec] 秒。
 * 与定时计划（schedule）配合：仅在计划的启用时间段内触发。
 */
class RestClock(private val prefs: Prefs) {

    /** 当前是否处于计划允许的学习时段 */
    fun inSchedule(now: Calendar = Calendar.getInstance()): Boolean {
        if (!prefs.scheduleOn) return true
        val dow = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // 1=周一 ... 7=周日
        if (!prefs.scheduleDays.contains(dow.toString())) return false
        return timeInRange(now, prefs.scheduleStart, prefs.scheduleEnd)
    }

    private fun timeInRange(now: Calendar, start: String, end: String): Boolean {
        fun mins(s: String): Int {
            val p = s.split(":")
            return runCatching { p[0].toInt() * 60 + p[1].toInt() }.getOrDefault(0)
        }
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val a = mins(start); val b = mins(end)
        return if (a <= b) cur in a..b else cur >= a || cur <= b // 跨午夜
    }

    /** 计算出下一次休息提醒的时间戳（基于本次开始学习时间） */
    fun nextRestAt(startedAt: Long): Long =
        startedAt + prefs.restWatchMinutes * 60_000L

    /** 触发休息：写入结束时间，时长做边界约束 */
    fun armRest(): Long {
        val sec = prefs.restSeconds.coerceIn(60, 7200)
        val end = System.currentTimeMillis() + sec * 1000L
        prefs.restEndAt = end
        return end
    }

    fun resting(): Boolean = System.currentTimeMillis() < prefs.restEndAt
    fun restLeftSec(): Long =
        maxOf(0L, (prefs.restEndAt - System.currentTimeMillis()) / 1000L)

    fun cancelRest() { prefs.restEndAt = 0L }
}
