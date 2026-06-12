package com.awesomepp.gesturesync.shared

object GestureSyncContract {
    const val PATH_OPEN_PHONE_APP = "/gesturesync/open-phone-app"
    const val PATH_SWIPE = "/gesturesync/swipe"
    const val PATH_TAP = "/gesturesync/tap"
    const val PATH_MODE_CHANGED = "/gesturesync/mode-changed"
    const val PATH_VOLUME = "/gesturesync/volume"

    const val MODE_GESTURE = "GESTURE"
    const val MODE_VOLUME = "VOLUME"

    const val DIRECTION_UP = "UP"
    const val DIRECTION_DOWN = "DOWN"
    const val DIRECTION_LEFT = "LEFT"
    const val DIRECTION_RIGHT = "RIGHT"
}
