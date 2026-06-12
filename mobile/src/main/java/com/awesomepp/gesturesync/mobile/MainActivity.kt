package com.awesomepp.gesturesync.mobile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.Wearable

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

        val keepScreenOnSwitch = findViewById<Switch>(R.id.keepScreenOnSwitch)
        keepScreenOnSwitch.isChecked = isKeepScreenOnEnabled()
        keepScreenOnSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveKeepScreenOnEnabled(isChecked)
            sendKeepScreenOnSettingToWatch(isChecked)
            updateStatus(
                if (isChecked) {
                    "워치 화면 꺼짐 방지: ON\n워치 GestureSync 앱 화면에 상태가 표시됩니다."
                } else {
                    "워치 화면 꺼짐 방지: OFF\n워치 기본 화면 꺼짐 설정을 사용합니다."
                }
            )
        }

        findViewById<Button>(R.id.openAccessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.testSwipeUpButton).setOnClickListener {
            RemoteGestureAccessibilityService.instance?.performSwipe(GestureSyncContract.DIRECTION_UP)
                ?: updateStatus("접근성 서비스가 아직 켜져 있지 않습니다.")
        }

        findViewById<Button>(R.id.testTapButton).setOnClickListener {
            RemoteGestureAccessibilityService.instance?.performTap()
                ?: updateStatus("접근성 서비스가 아직 켜져 있지 않습니다.")
        }

        sendKeepScreenOnSettingToWatch(keepScreenOnSwitch.isChecked)
    }

    private fun sendKeepScreenOnSettingToWatch(enabled: Boolean) {
        val payload = if (enabled) GestureSyncContract.VALUE_ON else GestureSyncContract.VALUE_OFF
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    updateStatus("워치 연결 없음. Galaxy Wearable 연결을 확인하세요.")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        GestureSyncContract.PATH_KEEP_SCREEN_ON_CHANGED,
                        payload.toByteArray(Charsets.UTF_8)
                    )
                }
            }
            .addOnFailureListener {
                updateStatus("워치 설정 전송 실패: ${it.message ?: "unknown"}")
            }
    }

    private fun isKeepScreenOnEnabled(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    private fun saveKeepScreenOnEnabled(enabled: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KEEP_SCREEN_ON, enabled)
            .apply()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun updateStatus(message: String) {
        findViewById<TextView>(R.id.statusText).text = message
    }

    companion object {
        private const val PREFS_NAME = "gesture_sync_settings"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
