package com.laser.scanner.contract

import android.graphics.Color
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.laser.scanner.data.datastore.deviceRadius
import com.nice.common.applicationContext
import java.io.File

const val TAG = "LaserScanner"

const val DEFAULT_PASSWORD = "LaserScanner"
const val DATA_STORE_NAME = "laser_scanner_data_store"

const val FILE_TYPE_PNG = "PNG"
const val FILE_TYPE_SVG = "SVG"
const val FILE_TYPE_TXT = "TXT"
const val FILE_TYPE_LOG = "LOG"

const val DEFAULT_VALUE_SCALE = 3

const val DEFAULT_BOX_PADDING = 20
const val DEFAULT_BOX_WIDTH = 1080
const val DEFAULT_BOX_HEIGHT = 1920
const val DEFAULT_STROKE_WIDTH = 2.5F
const val DEFAULT_STROKE_COLOR = Color.RED
const val DEFAULT_FILL_COLOR = Color.TRANSPARENT

val KEY_CODE = stringPreferencesKey("password")

val KEY_DEVICE_RADIUS = intPreferencesKey("device_radius")

fun mimeType(fileType: String) = when (fileType) {
    FILE_TYPE_PNG -> "image/png"
    FILE_TYPE_SVG -> "*/*"
    FILE_TYPE_TXT -> "text/plain"
    else -> "*/*"
}

fun generateExtractFile(name: String, fileType: String) = File(
    applicationContext.externalCacheDir,
    when (fileType) {
        FILE_TYPE_PNG -> "$name.png"
        FILE_TYPE_SVG -> "$name.svg"
        FILE_TYPE_TXT -> "$name.txt"
        FILE_TYPE_LOG -> "$name.log"
        else -> name
    }
)

var DEVICE_RADIUS: Int = deviceRadius