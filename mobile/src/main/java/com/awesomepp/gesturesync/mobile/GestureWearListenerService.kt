package com.awesomepp.gesturesync.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class GestureWearListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            GestureSyncContract.PATH_OPEN_PHONE_APP -> openPhoneApp()
            GestureSyncContract.PATH_SWIPE -> {
                val direction = messageEvent.data.toString(Charsets.UTF_8)
                RemoteGestureAccessibilityService.instance?.performSwipe(direction)
            }
            GestureSyncContract.PATH_TAP -> {
                RemoteGestureAccessibilityService.instance?.performTap()
            }
        }
    }

    private fun openPhoneApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching { startActivity(intent) }
            .onFailure { showOpenAppNotification(intent) }
    }

    private fun showOpenAppNotification(intent: Intent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gesture_sync_open_app"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "GestureSync", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("GestureSync")
            .setContentText("휴대폰 앱을 열어 워치 제스처를 받을 준비를 합니다.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}
