package com.m3u.core.util.basic

import android.graphics.Rect
import android.util.Rational

val Rect.isNotEmpty: Boolean get() = !isEmpty

val Rect.rational: Rational get() = Rational(width(), height())
