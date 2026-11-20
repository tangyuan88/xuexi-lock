package com.zqc.studylock.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zqc.studylock.App
import com.zqc.studylock.R

/** 家长密码页：4 位数字密码验证；验证通过后可退出锁定界面 */
class PinActivity : AppCompatActivity() {

    private lateinit var etOld: EditText
    private lateinit var etNew: EditText
    private lateinit var etNew2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        etOld = findViewById(R.id.etOld)
        etNew = findViewById(R.id.etNew)
        etNew2 = findViewById(R.id.etNew2)

        findViewById<Button>(R.id.btnVerify).setOnClickListener { verify() }
        findViewById<Button>(R.id.btnChange).setOnClickListener { changePin() }
    }

    private fun verify() {
        val input = etOld.text.toString().trim()
        if (input == App.prefs?.pin) {
            // 放行退出：清除锁定持久化，并把锁定界面一并结束，
            // 否则 LockActivity.onStop 会在切到本页时把它重新拉回前台
            App.lockManager?.grantExit()
            finish()
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            Toast.makeText(this, "验证通过", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "密码不正确", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changePin() {
        val old = etOld.text.toString().trim()
        val np = etNew.text.toString().trim()
        val np2 = etNew2.text.toString().trim()
        if (!np.matches(Regex("\\d{4}"))) {
            Toast.makeText(this, "新密码必须是 4 位数字", Toast.LENGTH_SHORT).show(); return
        }
        if (np != np2) {
            Toast.makeText(this, "两次输入不一致", Toast.LENGTH_SHORT).show(); return
        }
        if (App.prefs?.changePin(old, np) == true) {
            Toast.makeText(this, "密码已修改", Toast.LENGTH_SHORT).show()
            etOld.text?.clear(); etNew.text?.clear(); etNew2.text?.clear()
        } else {
            Toast.makeText(this, "原密码错误", Toast.LENGTH_SHORT).show()
        }
    }
}
