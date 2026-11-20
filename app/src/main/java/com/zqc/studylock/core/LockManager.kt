package com.zqc.studylock.core

import android.content.Context
import android.content.Intent
import com.zqc.studylock.App
import com.zqc.studylock.data.QuestionBank
import com.zqc.studylock.data.WrongBook
import com.zqc.studylock.ui.LockActivity
import com.zqc.studylock.util.Prefs
import kotlin.math.max

/**
 * 锁定管理器：学习锁的状态机核心。
 * 状态：IDLE → LOCKED（解锁答题）→ SOLVED →（可选休息）→ IDLE
 * 负责会话计时、答题校验、休息触发、保活心跳与状态恢复。
 */
class LockManager(private val context: Context) {

    private val prefs: Prefs get() = App.prefs!!
    private val bank: QuestionBank get() = App.bank!!
    private val wrongBook: WrongBook get() = App.wrongBook!!

    enum class State { IDLE, LOCKED, RESTING, SOLVED }
    @Volatile var state: State = State.IDLE
        private set

    /** 家长已验证放行：解锁期间禁止 LockActivity 自动回锁 */
    @Volatile var exitGranted: Boolean = false
        private set

    private var session: QuizSession? = null
    private var startedAt = 0L
    private var solvedCount = 0

    fun startSession() {
        if (state != State.IDLE) return
        startedAt = System.currentTimeMillis()
        state = State.LOCKED
        exitGranted = false
        session = QuizSession(bank, wrongBook, prefs.quizCount).also { it.start() }
        solvedCount = 0
        persist()
        showLockScreen()
    }

    fun stopSession() {
        state = State.IDLE
        session = null
        exitGranted = false
        prefs.lockActive = false
    }

    /** 家长密码验证通过：放行退出，清除锁定持久化 */
    fun grantExit() {
        exitGranted = true
        prefs.lockActive = false
        state = State.IDLE
        session = null
    }

    fun heartbeat() {
        if (state == State.LOCKED && prefs.restOn &&
            System.currentTimeMillis() - startedAt >= prefs.restWatchMinutes * 60_000L) {
            App.restClock?.armRest()
        }
    }

    /**
     * 待机唤醒 / 开机 / 进程被杀后恢复。
     * 关键：原实现只在休息段置状态，不重建锁定会话，导致系统强杀或待机后锁定消失。
     * 现在若持久化显示仍在锁定中，重建会话并把全屏锁定界面拉回前台。
     */
    fun restore() {
        if (App.restClock?.resting() == true) {
            state = State.RESTING
        }
        if (prefs.lockActive) {
            // 复用持久化里的起始时间，保证剩余观看时长连续
            startedAt = prefs.lockStartedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            solvedCount = prefs.lockSolved
            state = State.LOCKED
            exitGranted = false
            session = QuizSession(bank, wrongBook, prefs.quizCount).also { it.start() }
            showLockScreen()
        }
    }

    /** 提交答案；返回 true 表示解锁成功 */
    fun answer(choice: Int): Boolean {
        val s = session ?: return false
        s.answer(choice)
        solvedCount = max(solvedCount, s.solved)
        if (prefs.readQuestionOn) App.speech?.speak("已作答")
        if (s.finished) {
            state = State.SOLVED
            stopSession()
            return true
        }
        // 只持久化进度，界面由 LockActivity.submit 自己 render() 刷新；
        // 这里不再 showLockScreen()，否则会重复新建 Activity 并与 onStop 回锁形成循环
        persist()
        return false
    }

    /** 把锁定会话写入持久化，供进程重建后恢复 */
    private fun persist() {
        prefs.lockActive = true
        prefs.lockStartedAt = startedAt
        prefs.lockSolved = solvedCount
    }

    fun currentQuestion() = session?.current()

    /** 当前进度：(已连续答对数, 需要总数) */
    fun progress(): Pair<Int, Int> = session?.progress ?: (0 to prefs.quizCount)

    fun leftMs(): Long = when {
        App.restClock?.resting() == true -> App.restClock!!.restLeftSec() * 1000L
        state == State.LOCKED -> max(0L, prefs.watchMinutes * 60_000L - (System.currentTimeMillis() - startedAt))
        else -> 0L
    }

    fun isRunning() = state == State.LOCKED || state == State.RESTING
    fun isResting() = App.restClock?.resting() == true
    fun solvedCount() = solvedCount

    private fun showLockScreen() {
        val i = Intent(context, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
