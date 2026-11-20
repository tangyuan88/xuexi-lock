package com.zqc.studylock.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zqc.studylock.App
import com.zqc.studylock.R

/** 题库包管理页：列出所有包，支持启用/禁用 */
class PacksActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packs)
        container = findViewById(R.id.packContainer)
        render()
    }

    private fun render() {
        container.removeAllViews()
        val packs = App.bank?.allPacks().orEmpty()
        if (packs.isEmpty()) {
            container.addView(TextView(this).apply { text = "还没有导入任何题库包，请在家长控制端导入。" })
            return
        }
        packs.forEach { p ->
            val row = Button(this).apply {
                text = "${p.title} · ${p.count} 题 · ${if (p.enabled) "已启用" else "已停用"}"
                setOnClickListener {
                    App.bank?.toggle(p.id, !p.enabled)
                    Toast.makeText(this@PacksActivity, "已切换启用状态", Toast.LENGTH_SHORT).show()
                    render()
                }
            }
            container.addView(row)
        }
    }
}
