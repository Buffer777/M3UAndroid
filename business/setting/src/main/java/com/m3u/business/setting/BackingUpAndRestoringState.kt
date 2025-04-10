package com.m3u.business.setting

enum class BackingUpAndRestoringState {
    NONE, BACKING_UP, RESTORING, BOTH;

    companion object {
        fun of(backingUp: Boolean, restoring: Boolean): BackingUpAndRestoringState {
            return when {
                backingUp && restoring -> BOTH
                backingUp -> BACKING_UP
                restoring -> RESTORING
                else -> NONE
            }
        }
    }
}