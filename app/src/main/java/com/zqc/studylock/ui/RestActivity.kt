package com.zqc.studylock.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zqc.studylock.App
import com.zqc.studylock.R

/** 休息页：全屏显示剩余秒数，家长可提前结束 */
class RestActivity : AppCompatActivity() {

    private lateinit var tvCountdown: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rest)
        tvCountdown = findViewById(R.id.tvCountdown)
        findViewById<Button>(R.id.btnSkip).setOnClickListener {
            App.restClock?.cancelRest()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        tick()
    }

    private fun tick() {
        val left = App.restClock?.restLeftSec() ?: 0L
        if (left <= 0) { finish(); return }
        tvCountdown.text = "休息中 · 剩余 ${left} 秒"
        tvCountdown.postDelayed({ tick() }, 1000L)
    }
}
