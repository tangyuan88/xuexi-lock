package com.zqc.studylock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zqc.studylock.App

/** 闹钟/心跳接收器：处理休息提醒、定时计划触发、服务保活探针 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REST_FIRE -> App.restClock?.armRest()
            ACTION_HEARTBEAT -> App.lockManager?.heartbeat()
            ACTION_BOOT_RESUME -> App.lockManager?.restore()
        }
    }

    companion object {
        const val ACTION_REST_FIRE = "com.zqc.studylock.REST_FIRE"
        const val ACTION_HEARTBEAT = "com.zqc.studylock.HEARTBEAT"
        const val ACTION_BOOT_RESUME = "com.zqc.studylock.BOOT_RESUME"
    }
}
