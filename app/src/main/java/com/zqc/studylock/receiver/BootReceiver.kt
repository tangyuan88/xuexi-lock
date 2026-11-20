package com.zqc.studylock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zqc.studylock.App
import com.zqc.studylock.service.LanService

/** 开机 / 待机唤醒 / 应用更新后自启：恢复局域网服务与锁定状态 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        /** 小米/红米电视遥控器关机是 STR 待机，唤醒只发这个私有广播 */
        const val ACTION_MITV_STR_BOOT = "mitv.action.STR_BOOT_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_MITV_STR_BOOT -> {
                // 进程可能刚被系统从待机中拉起，App.onCreate 未必已执行完，做空值保护
                if (App.prefs?.lanOn == true) LanService.start(context)
                App.lockManager?.restore()
            }
        }
    }
}
