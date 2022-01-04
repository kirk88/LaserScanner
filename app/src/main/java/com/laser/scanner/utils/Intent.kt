package com.laser.scanner.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laser.scanner.R
import com.laser.scanner.data.model.BleDevice
import com.laser.scanner.ui.activity.detail.DeviceDetailActivity
import com.nice.bluetooth.common.Advertisement
import com.nice.common.helper.ifNullOrEmpty
import com.nice.common.helper.showToast
import com.nice.common.helper.startActivity
import java.io.File

fun Context.toAppSetting() {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    } catch (_: Throwable) {
    }
}

fun Context.shareFile(file: File, fileType: String = "*/*") {
    if (!file.exists()) {
        showToast(R.string.tip_file_not_exists)
        return
    }

    val intent = Intent(Intent.ACTION_SEND)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.type = fileType
    intent.putExtra(Intent.EXTRA_STREAM, file.toUriCompat())
    startActivity(Intent.createChooser(intent, getString(R.string.share)))
}

fun Context.shareText(text: String) {
    if (text.isBlank()) return

    val intent = Intent(Intent.ACTION_SEND)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(intent, getString(R.string.share)))
}

fun Context.startConnectSelector(device: BleDevice) {
    fun startActivity(rangeFinding: Boolean) {
        startActivity<DeviceDetailActivity>(
            "name" to device.name.ifNullOrEmpty { "Unknown" },
            "address" to device.address,
            "range_finding" to rangeFinding
        )
    }
    MaterialAlertDialogBuilder(this)
        .setItems(arrayOf("扫描", "测距")) { _, which ->
            startActivity(which == 1)
        }
        .show()
}

fun Context.startConnectSelector(advertisement: Advertisement) {
    fun startActivity(rangeFinding: Boolean) {
        startActivity<DeviceDetailActivity>(
            "name" to advertisement.name.ifNullOrEmpty { "Unknown" },
            "address" to advertisement.address,
            "range_finding" to rangeFinding
        )
    }
    MaterialAlertDialogBuilder(this)
        .setItems(arrayOf("扫描", "测距")) { _, which ->
            startActivity(which == 1)
        }
        .show()
}