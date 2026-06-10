package com.awesomepp.gesturesync.mobile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import com.awesomepp.gesturesync.shared.GestureSyncContract

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestNotificationPermissionIfNeeded()

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
}
