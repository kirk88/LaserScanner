package com.laser.scanner.utils

import android.util.Log
import com.laser.scanner.BuildConfig
import com.laser.scanner.contract.TAG

fun log(tag: String = TAG, error: Throwable? = null, lazyMessage: () -> Any?) {
    if (!BuildConfig.DEBUG) return
    val message = lazyMessage().toString()
    if (error != null) {
        Log.e(tag, message, error)
    } else {
        Log.i(tag, message)
    }
}