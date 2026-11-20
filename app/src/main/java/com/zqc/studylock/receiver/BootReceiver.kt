package com.zqc.studylock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zqc.studylock.App
import com.zqc.studylock.service.LanService

/** 开机 / 应用更新后自启：恢复局域网服务与闹钟 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (App.prefs?.lanOn == true) LanService.start(context)
                App.lockManager?.restore()
            }
        }
    }
}
