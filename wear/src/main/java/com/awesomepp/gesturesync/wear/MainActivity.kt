package com.awesomepp.gesturesync.wear

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlin.math.abs

class MainActivity : Activity() {

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var currentMode = GestureSyncContract.MODE_GESTURE
    private var keepScreenOnEnabled = false
    private lateinit var statusText: TextView

    private val watchMessageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == GestureSyncContract.PATH_KEEP_SCREEN_ON_CHANGED) {
            val value = messageEvent.data.toString(Charsets.UTF_8)
            applyKeepScreenOn(value == GestureSyncContract.VALUE_ON)
            updateStatusText("PHONE SETTING RECEIVED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getMessageClient(this).addListener(watchMessageListener)
        setContentView(createGesturePad())
        updateStatusText("READY")
        sendCommand(GestureSyncContract.PATH_OPEN_PHONE_APP)
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(watchMessageListener)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun createGesturePad(): View {
        statusText = TextView(this).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 14f
        }

        return FrameLayout(this).apply {
            setBackgroundColor(0xFF111111.toInt())
            addView(
                statusText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.CENTER
                )
            )
            setOnTouchListener { _, event -> handleTouch(event) }
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downTime = event.eventTime
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val duration = event.eventTime - downTime
                val tapThreshold = 36f
                val swipeThreshold = 70f

                if (duration >= LONG_PRESS_MS && abs(dx) < swipeThreshold && abs(dy) < swipeThreshold) {
                    toggleMode()
                    return true
                }

                if (abs(dx) < tapThreshold && abs(dy) < tapThreshold) {
                    handleTap()
                    return true
                }

                if (abs(dx) < swipeThreshold && abs(dy) < swipeThreshold) {
                    updateStatusText("TOO SHORT")
                    return true
                }

                val direction = if (abs(dx) > abs(dy)) {
                    if (dx > 0) GestureSyncContract.DIRECTION_RIGHT else GestureSyncContract.DIRECTION_LEFT
                } else {
                    if (dy > 0) GestureSyncContract.DIRECTION_DOWN else GestureSyncContract.DIRECTION_UP
                }

                handleSwipe(direction)
                return true
            }
        }
        return true
    }

    private fun toggleMode() {
        currentMode = if (currentMode == GestureSyncContract.MODE_GESTURE) {
            GestureSyncContract.MODE_VOLUME
        } else {
            GestureSyncContract.MODE_GESTURE
        }
        updateStatusText("MODE CHANGED")
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    private fun handleTap() {
        updateStatusText("TAP")
        if (currentMode == GestureSyncContract.MODE_GESTURE) {
            sendCommand(GestureSyncContract.PATH_TAP)
        }
    }

    private fun handleSwipe(direction: String) {
        if (currentMode == GestureSyncContract.MODE_VOLUME) {
            when (direction) {
                GestureSyncContract.DIRECTION_UP -> {
                    updateStatusText("VOLUME UP")
                    sendCommand(GestureSyncContract.PATH_VOLUME, direction)
                }
                GestureSyncContract.DIRECTION_DOWN -> {
                    updateStatusText("VOLUME DOWN")
                    sendCommand(GestureSyncContract.PATH_VOLUME, direction)
                }
                else -> updateStatusText("VOLUME MODE\nUP/DOWN only")
            }
            return
        }

        updateStatusText(direction)
        sendCommand(GestureSyncContract.PATH_SWIPE, direction)
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        keepScreenOnEnabled = enabled
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun updateStatusText(action: String) {
        val modeLabel = if (currentMode == GestureSyncContract.MODE_VOLUME) "VOLUME" else "GESTURE"
        val screenLabel = if (keepScreenOnEnabled) "ON" else "OFF"
        val help = if (currentMode == GestureSyncContract.MODE_VOLUME) {
            "위/아래: 볼륨 조절\n길게 누름: 제스처 모드"
        } else {
            "스와이프: 폰 제어\n탭: 중앙 탭\n길게 누름: 볼륨 모드"
        }
        statusText.text = "GestureSync\n\nMODE: $modeLabel\nSCREEN ON: $screenLabel\n$action\n\n$help"
    }

    private fun sendCommand(path: String, payload: String = "") {
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    statusText.text = "폰 연결 없음\nGalaxy Wearable 연결을 확인하세요."
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8))
                }
            }
            .addOnFailureListener {
                statusText.text = "전송 실패: ${it.message ?: "unknown"}"
            }
    }

    companion object {
        private const val LONG_PRESS_MS = 650L
    }
}
