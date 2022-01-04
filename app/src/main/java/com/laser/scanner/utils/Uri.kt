package com.laser.scanner.utils

import android.net.Uri
import android.os.Build.VERSION
import androidx.core.content.FileProvider
import com.laser.scanner.BuildConfig
import com.nice.common.applicationContext
import java.io.File

fun File.toUriCompat(): Uri = if (VERSION.SDK_INT >= 24) {
    FileProvider.getUriForFile(
        applicationContext,
        BuildConfig.APPLICATION_ID + ".fileprovider",
        this
    )
} else {
    Uri.fromFile(this)
}