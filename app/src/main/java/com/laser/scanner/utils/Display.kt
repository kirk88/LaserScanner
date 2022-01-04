package com.laser.scanner.utils

import android.content.res.Resources

fun mmToPx(value: Float): Float {
    val metrics = Resources.getSystem().displayMetrics
    val inch = value / 25.4f
    return inch * metrics.xdpi
}

fun pxToMm(value: Float): Float {
    val metrics = Resources.getSystem().displayMetrics
    val inch = value / metrics.xdpi
    return inch * 25.4f
}