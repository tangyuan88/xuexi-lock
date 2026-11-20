package com.zqc.studylock.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zqc.studylock.App
import com.zqc.studylock.R

/** 全屏锁定/答题界面：答对 N 道题才能解锁，家长可通过 4 位密码退出 */
class LockActivity : AppCompatActivity() {

    private lateinit var tvStem: TextView
    private lateinit var tvProgress: TextView
    private lateinit var options: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            startActivity(Intent(this, PinActivity::class.java))
        }
        render()
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
}
