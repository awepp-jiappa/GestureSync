package com.awesomepp.gesturesync.mobile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class GestureWearListenerService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            GestureSyncContract.PATH_OPEN_PHONE_APP -> {
                showModeNotification(GestureSyncContract.MODE_GESTURE)
                openPhoneApp()
            }
            GestureSyncContract.PATH_MODE_CHANGED -> {
                val mode = messageEvent.data.toString(Charsets.UTF_8)
                showModeNotification(mode)
            }
            GestureSyncContract.PATH_VOLUME -> {
                val direction = messageEvent.data.toString(Charsets.UTF_8)
                adjustMediaVolume(direction)
            }
            GestureSyncContract.PATH_SWIPE -> {
                val direction = messageEvent.data.toString(Charsets.UTF_8)
                RemoteGestureAccessibilityService.instance?.performSwipe(direction)
            }
            GestureSyncContract.PATH_TAP -> {
                RemoteGestureAccessibilityService.instance?.performTap()
            }
        }
    }

    private fun adjustMediaVolume(direction: String) {
        val audioManager = getSystemService(AudioManager::class.java)
        val adjustDirection = when (direction) {
            GestureSyncContract.DIRECTION_UP -> AudioManager.ADJUST_RAISE
            GestureSyncContract.DIRECTION_DOWN -> AudioManager.ADJUST_LOWER
            else -> return
        }

        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            adjustDirection,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun openPhoneApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching { startActivity(intent) }
            .onFailure { showOpenAppNotification(intent) }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    MODE_CHANNEL_ID,
                    "GestureSync Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "GestureSync current mode status"
                    setShowBadge(false)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    OPEN_APP_CHANNEL_ID,
                    "GestureSync",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private fun showModeNotification(mode: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = if (mode == GestureSyncContract.MODE_VOLUME) {
            "볼륨 모드"
        } else {
            "제스처 모드"
        }

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, MODE_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("GestureSync 실행 중")
            .setContentText("현재 모드: $modeLabel")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        manager.notify(MODE_NOTIFICATION_ID, notification)
    }

    private fun showOpenAppNotification(intent: Intent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureNotificationChannel()

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, OPEN_APP_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("GestureSync")
            .setContentText("휴대폰 앱을 열어 워치 제스처를 받을 준비를 합니다.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }

    companion object {
        private const val MODE_CHANNEL_ID = "gesture_sync_mode"
        private const val OPEN_APP_CHANNEL_ID = "gesture_sync_open_app"
        private const val MODE_NOTIFICATION_ID = 2001
    }
}
