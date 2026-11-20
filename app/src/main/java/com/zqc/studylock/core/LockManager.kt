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

    private var session: QuizSession? = null
    private var startedAt = 0L
    private var solvedCount = 0

    fun startSession() {
        if (state != State.IDLE) return
        startedAt = System.currentTimeMillis()
        state = State.LOCKED
        session = QuizSession(bank, wrongBook, prefs.quizCount).also { it.start() }
        showLockScreen()
    }

    fun stopSession() {
        state = State.IDLE
        session = null
    }

    fun heartbeat() {
        if (state == State.LOCKED && prefs.restOn &&
            System.currentTimeMillis() - startedAt >= prefs.restWatchMinutes * 60_000L) {
            App.restClock?.armRest()
        }
    }

    fun restore() {
        // 重启后若处于休息时间段，恢复到 RESTING 状态
        if (App.restClock?.resting() == true) state = State.RESTING
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
        showLockScreen()
        return false
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
