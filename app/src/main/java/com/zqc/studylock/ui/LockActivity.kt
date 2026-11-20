package com.zqc.studylock.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zqc.studylock.App
import com.zqc.studylock.R

/**
 * 全屏锁定/答题界面：答对 N 道题才能解锁，家长可通过 4 位密码退出。
 *
 * 采用普通 Activity（而非 SYSTEM_ALERT_WINDOW 悬浮窗）实现，避免被小米电视
 * MitvWindowManager 以 "has system window" 强杀。代价是 HOME 键会把 Activity
 * 切到后台，因此在 onStop 中检测：仍处于锁定且未获家长放行时，自动把自己拉回前台。
 */
class LockActivity : AppCompatActivity() {

    private lateinit var tvStem: TextView
    private lateinit var tvProgress: TextView
    private lateinit var options: List<Button>
    private val handler = Handler(Looper.getMainLooper())
    /** 正在前往家长密码页：此期间不触发自动回锁，避免盖住 PinActivity */
    private var navigatingToPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 家长已放行 / 无锁定会话时的残留实例：直接结束，避免显示空题界面
        val mgr = App.lockManager
        val lockActive = App.prefs?.lockActive == true
        if (mgr == null || (!mgr.isRunning() && !lockActive)) {
            finish()
            return
        }
        setContentView(R.layout.activity_lock)
        tvStem = findViewById(R.id.tvStem)
        tvProgress = findViewById(R.id.tvProgress)
        options = listOf(
            findViewById(R.id.optA), findViewById(R.id.optB),
            findViewById(R.id.optC), findViewById(R.id.optD)
        )
        options.forEachIndexed { i, btn ->
            btn.setOnClickListener { submit(i) }
        }
        findViewById<Button>(R.id.btnParent).setOnClickListener {
            navigatingToPin = true
            startActivity(Intent(this, PinActivity::class.java))
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        // 从家长页返回但未放行（密码错误/放弃），恢复受控状态
        navigatingToPin = false
    }

    /** 由 LockManager.answer 触发刷新；这里重新取当前题 */
    private fun render() {
        val q = App.lockManager?.currentQuestion() ?: run {
            tvStem.text = "正在准备题目…"
            return
        }
        tvStem.text = q.stem
        options.forEachIndexed { i, btn ->
            btn.text = q.options.getOrElse(i) { "" }
            btn.isEnabled = i < q.options.size
        }
        val (done, total) = App.lockManager?.progress() ?: (0 to 0)
        tvProgress.text = "已答对 $done / $total 题 · 请继续"
    }

    private fun submit(choice: Int) {
        val ok = App.lockManager?.answer(choice) ?: false
        if (ok) {
            finish()
        } else {
            render()
        }
    }

    override fun onBackPressed() {
        // 锁定状态下禁止返回退出
        if (App.lockManager?.isRunning() != true) super.onBackPressed()
    }

    /**
     * HOME 键 / 切桌面会把本 Activity 停掉。只要仍在锁定中且家长未放行，
     * 就延迟把它重新拉到前台，形成"无法逃逸"的普通 Activity 锁定。
     * 延迟是为了避开切换瞬间的窗口冲突（尤其电视桌面动画）。
     */
    override fun onStop() {
        super.onStop()
        val mgr = App.lockManager
        if (!navigatingToPin && mgr != null && mgr.isRunning() && !mgr.exitGranted && !isFinishing) {
            handler.postDelayed({
                if (mgr.isRunning() && !mgr.exitGranted) {
                    startActivity(
                        Intent(this, LockActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }
            }, 300L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
