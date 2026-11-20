package com.zqc.studylock.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.zqc.studylock.App

/** 计时服务：负责学习/休息倒计时状态推进（由 AlarmReceiver 与 UI 共同驱动） */
class TimerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> App.lockManager?.startSession()
            ACTION_STOP -> App.lockManager?.stopSession()
            ACTION_REST -> App.restClock?.armRest()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.zqc.studylock.START"
        const val ACTION_STOP = "com.zqc.studylock.STOP"
        const val ACTION_REST = "com.zqc.studylock.REST_FIRE"
    }
}
