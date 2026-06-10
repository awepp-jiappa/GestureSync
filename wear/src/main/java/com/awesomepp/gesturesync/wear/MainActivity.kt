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
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createGesturePad())
        sendCommand(GestureSyncContract.PATH_OPEN_PHONE_APP)
    }

    private fun createGesturePad(): View {
        statusText = TextView(this).apply {
            text = "GestureSync\n\n스와이프: 폰 스와이프\n탭: 폰 중앙 탭"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            textSize = 16f
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
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val threshold = 36f

                if (abs(dx) < threshold && abs(dy) < threshold) {
                    statusText.text = "TAP"
                    sendCommand(GestureSyncContract.PATH_TAP)
                    return true
                }

                val direction = if (abs(dx) > abs(dy)) {
                    if (dx > 0) GestureSyncContract.DIRECTION_RIGHT else GestureSyncContract.DIRECTION_LEFT
                } else {
                    if (dy > 0) GestureSyncContract.DIRECTION_DOWN else GestureSyncContract.DIRECTION_UP
                }

                statusText.text = direction
                sendCommand(GestureSyncContract.PATH_SWIPE, direction)
                return true
            }
        }
        return true
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
