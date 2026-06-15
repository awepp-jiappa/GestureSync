package com.awesomepp.gesturesync.wear

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.awesomepp.gesturesync.shared.GestureSyncContract
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlin.math.abs

class MainActivity : Activity() {

    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var currentMode = GestureSyncContract.MODE_GESTURE
    private var keepScreenOnEnabled = false

    private lateinit var rootLayout: FrameLayout
    private lateinit var modeChip: TextView
    private lateinit var actionText: TextView
    private lateinit var hintText: TextView
    private lateinit var centerPad: LinearLayout
    private lateinit var centerTitle: TextView
    private lateinit var centerSub: TextView
    private lateinit var screenOnText: TextView

    private val watchMessageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == GestureSyncContract.PATH_KEEP_SCREEN_ON_CHANGED) {
            val value = messageEvent.data.toString(Charsets.UTF_8)
            applyKeepScreenOn(value == GestureSyncContract.VALUE_ON)
            updateUi("폰 설정 수신")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getMessageClient(this).addListener(watchMessageListener)
        setContentView(createGesturePad())
        updateUi("준비됨")
        sendCommand(GestureSyncContract.PATH_OPEN_PHONE_APP)
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(watchMessageListener)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private fun createGesturePad(): View {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#050914"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val titleText = TextView(this).apply {
            text = "GestureSync"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        modeChip = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(6), dp(14), dp(6))
        }

        screenOnText = TextView(this).apply {
            textSize = 10f
            setTextColor(Color.parseColor("#8EA4C4"))
            gravity = Gravity.CENTER
        }

        centerPad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = createCenterPadBackground()
            layoutParams = LinearLayout.LayoutParams(dp(148), dp(148)).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }
        }

        centerTitle = TextView(this).apply {
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        centerSub = TextView(this).apply {
            textSize = 11f
            setTextColor(Color.parseColor("#B7D4FF"))
            gravity = Gravity.CENTER
        }

        centerPad.addView(centerTitle)
        centerPad.addView(centerSub)

        actionText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#EAF3FF"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }

        hintText = TextView(this).apply {
            textSize = 10.5f
            setTextColor(Color.parseColor("#9FB3C8"))
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.12f)
        }

        container.addView(titleText)
        container.addView(space(6))
        container.addView(modeChip)
        container.addView(space(5))
        container.addView(screenOnText)
        container.addView(centerPad)
        container.addView(actionText)
        container.addView(space(6))
        container.addView(hintText)

        rootLayout.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        rootLayout.setOnTouchListener { _, event -> handleTouch(event) }
        return rootLayout
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
                    updateUi("짧은 입력")
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
        vibrateModeChanged()
        updateUi("모드 변경")
        sendCommand(GestureSyncContract.PATH_MODE_CHANGED, currentMode)
    }

    private fun vibrateModeChanged() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                MODE_CHANGE_VIBRATION_MS,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    private fun handleTap() {
        updateUi("탭")
        if (currentMode == GestureSyncContract.MODE_GESTURE) {
            sendCommand(GestureSyncContract.PATH_TAP)
        }
    }

    private fun handleSwipe(direction: String) {
        if (currentMode == GestureSyncContract.MODE_VOLUME) {
            when (direction) {
                GestureSyncContract.DIRECTION_UP -> {
                    updateUi("볼륨 증가")
                    sendCommand(GestureSyncContract.PATH_VOLUME, direction)
                }
                GestureSyncContract.DIRECTION_DOWN -> {
                    updateUi("볼륨 감소")
                    sendCommand(GestureSyncContract.PATH_VOLUME, direction)
                }
                else -> updateUi("위/아래만 사용")
            }
            return
        }

        val label = when (direction) {
            GestureSyncContract.DIRECTION_UP -> "위로 스와이프"
            GestureSyncContract.DIRECTION_DOWN -> "아래로 스와이프"
            GestureSyncContract.DIRECTION_LEFT -> "왼쪽 스와이프"
            GestureSyncContract.DIRECTION_RIGHT -> "오른쪽 스와이프"
            else -> direction
        }
        updateUi(label)
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

    private fun updateUi(action: String) {
        val isVolume = currentMode == GestureSyncContract.MODE_VOLUME

        modeChip.text = if (isVolume) "볼륨 모드" else "제스처 모드"
        modeChip.background = createModeChipBackground(isVolume)

        screenOnText.text = "화면 유지 ${if (keepScreenOnEnabled) "ON" else "OFF"}"

        centerTitle.text = if (isVolume) "🔊" else "✋"
        centerSub.text = if (isVolume) "위 / 아래" else "스와이프 / 탭"

        actionText.text = action

        hintText.text = if (isVolume) {
            "위/아래: 볼륨 조절\n3초 누름: 제스처 모드"
        } else {
            "스와이프: 폰 제어\n탭: 중앙 탭\n3초 누름: 볼륨 모드"
        }
    }

    private fun sendCommand(path: String, payload: String = "") {
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    updateUi("폰 연결 없음")
                    return@addOnSuccessListener
                }

                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8))
                }
            }
            .addOnFailureListener {
                updateUi("전송 실패")
            }
    }

    private fun createModeChipBackground(isVolume: Boolean): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (isVolume) {
                intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#1D4ED8"))
            } else {
                intArrayOf(Color.parseColor("#0EA5E9"), Color.parseColor("#2563EB"))
            }
        ).apply {
            cornerRadius = dp(50).toFloat()
        }
    }

    private fun createCenterPadBackground(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#142844"), Color.parseColor("#091426"))
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(2), Color.parseColor("#2F80FF"))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun space(heightDp: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
        }

    companion object {
        private const val LONG_PRESS_MS = 3_000L
        private const val MODE_CHANGE_VIBRATION_MS = 80L
    }
}
