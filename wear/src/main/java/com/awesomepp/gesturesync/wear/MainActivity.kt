package com.awesomepp.gesturesync.wear

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.Wearable
import kotlin.math.abs

class MainActivity : Activity() {

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var currentMode = GestureSyncContract.MODE_GESTURE
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createGesturePad())
        updateStatus("준비됨")
        sendCommand(GestureSyncContract.PATH_OPEN_PHONE_APP)
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    private fun createGesturePad(): View {
        statusText = TextView(this).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
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
                val elapsed = event.eventTime - downTime
                val tapThreshold = 36f
                val swipeThreshold = 70f
                val longPressMillis = 650L

                if (elapsed >= longPressMillis && abs(dx) < tapThreshold && abs(dy) < tapThreshold) {
                    toggleMode()
                    return true
                }

                if (abs(dx) < tapThreshold && abs(dy) < tapThreshold) {
                    updateStatus("TAP")
                    if (currentMode == GestureSyncContract.MODE_GESTURE) {
                        sendCommand(GestureSyncContract.PATH_TAP)
                    }
                    return true
                }

                if (abs(dx) < swipeThreshold && abs(dy) < swipeThreshold) {
                    updateStatus("짧은 입력 무시")
                    return true
                }

                val direction = if (abs(dx) > abs(dy)) {
                    if (dx > 0) GestureSyncContract.DIRECTION_RIGHT else GestureSyncContract.DIRECTION_LEFT
                } else {
                    if (dy > 0) GestureSyncContract.DIRECTION_DOWN else GestureSyncContract.DIRECTION_UP
                }

                updateStatus(direction)

                if (currentMode == GestureSyncContract.MODE_VOLUME) {
                    if (direction == GestureSyncContract.DIRECTION_UP || direction == GestureSyncContract.DIRECTION_DOWN) {
                        sendCommand(GestureSyncContract.PATH_VOLUME, direction)
                    }
                } else {
                    sendCommand(GestureSyncContract.PATH_SWIPE, direction)
                }
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
        updateStatus("모드 변경")
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    private fun updateStatus(action: String) {
        val modeTitle = if (currentMode == GestureSyncContract.MODE_VOLUME) {
            "VOLUME MODE"
        } else {
            "GESTURE MODE"
        }

        val help = if (currentMode == GestureSyncContract.MODE_VOLUME) {
            "위/아래: 볼륨 조절\n길게 누름: 제스처 모드"
        } else {
            "스와이프: 폰 제스처\n탭: 중앙 탭\n길게 누름: 볼륨 모드"
        }

        statusText.text = "GestureSync\n\n$modeTitle\n$action\n\n$help"
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
}
