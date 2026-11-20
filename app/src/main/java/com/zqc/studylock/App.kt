package com.zqc.studylock

import android.app.Application
import com.zqc.studylock.core.LockManager
import com.zqc.studylock.core.RestClock
import com.zqc.studylock.data.QuestionBank
import com.zqc.studylock.data.WrongBook
import com.zqc.studylock.net.CfgServer
import com.zqc.studylock.service.LanService
import com.zqc.studylock.tts.Speech
import com.zqc.studylock.util.Prefs

/** 全局应用入口：初始化各单例组件，装配 CfgServer 动作回调 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(this)
        bank = QuestionBank(this)
        wrongBook = WrongBook(this)
        restClock = RestClock(prefs!!)
        speech = Speech(this)
        lockManager = LockManager(this)

        cfgServer = CfgServer(this, prefs!!, bank!!, wrongBook!!, object : CfgServer.Actions {
            override fun isRunning() = lockManager?.isRunning() ?: false
            override fun isResting() = lockManager?.isResting() ?: false
            override fun leftMs() = lockManager?.leftMs() ?: 0L
            override fun solvedCount() = lockManager?.solvedCount() ?: 0
            override fun test(kind: String) = when (kind) {
                "lock" -> { lockManager?.startSession(); "已触发锁定界面" }
                "rest" -> { restClock?.armRest(); "已触发休息提醒" }
                "tts" -> { speech?.speak("学习锁语音测试"); "已播放测试语音" }
                else -> "未知测试项: $kind"
            }
            override fun onConfigChanged() { /* 配置已写入 Prefs，下次读取生效 */ }
            override fun refreshLanToken() = prefs!!.newLanToken()
        })

        if (prefs!!.lanOn) LanService.start(this)
    }

    companion object {
        @JvmStatic lateinit var instance: App
            private set
        var prefs: Prefs? = null
        var bank: QuestionBank? = null
        var wrongBook: WrongBook? = null
        var restClock: RestClock? = null
        var speech: Speech? = null
        var lockManager: LockManager? = null
        var cfgServer: CfgServer? = null
    }
}
