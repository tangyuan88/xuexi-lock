package com.zqc.studylock.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zqc.studylock.App
import com.zqc.studylock.R
import com.zqc.studylock.net.CfgServer
import java.net.Inet4Address
import java.net.NetworkInterface

/** 主界面：展示连接信息与验证码，提供各项权限入口与功能入口 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvToken: TextView
    private lateinit var tvIp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvToken = findViewById(R.id.tvToken)
        tvIp = findViewById(R.id.tvIp)

        refreshInfo()
        requestPermissionsIfNeeded()

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            App.lockManager?.startSession()
            Toast.makeText(this, "已开始专注", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnPacks).setOnClickListener {
            startActivity(Intent(this, PacksActivity::class.java))
        }
        findViewById<Button>(R.id.btnPin).setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java))
        }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { requestOverlay() }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestBatteryWhitelist() }
    }

    override fun onResume() {
        super.onResume()
        refreshInfo()
    }

    private fun refreshInfo() {
        val prefs = App.prefs ?: return
        val token = prefs.lanToken.ifEmpty { prefs.newLanToken() }
        tvToken.text = "验证码：$token"
        tvIp.text = "控制端地址：http://${localIp()}:${CfgServer.LAN_PORT}/?t=$token"
    }

    private fun localIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .first { !it.isLoopbackAddress && it is Inet4Address }
            .hostAddress
    }.getOrNull() ?: "192.168.1.1"

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun requestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
    }

    private fun requestBatteryWhitelist() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        }
    }
}
