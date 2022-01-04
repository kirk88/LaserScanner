package com.laser.scanner.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laser.scanner.R

fun Context.showPermissionDeniedDialog(cancel: () -> Unit = {}) {
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_alert_title)
        .setMessage(R.string.tip_permissions_denied)
        .setCancelable(false)
        .setNegativeButton(R.string.dialog_button_no) { _, _ -> cancel.invoke() }
        .setPositiveButton(R.string.dialog_button_go_setting) { _, _ -> toAppSetting() }
        .show()
}