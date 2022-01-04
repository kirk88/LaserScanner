package com.laser.scanner.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.laser.scanner.contract.*
import com.nice.common.applicationContext
import com.nice.common.helper.orZero
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore by preferencesDataStore(DATA_STORE_NAME)

suspend fun getCurrentCode(): String = applicationContext.dataStore.data.map {
    it[KEY_CODE]
}.first().orEmpty()


suspend fun setCurrentCode(code: String) = applicationContext.dataStore.edit {
    it[KEY_CODE] = code
}

fun isPowerful(): Boolean = runBlocking { getCurrentCode() == DEFAULT_PASSWORD }

var deviceRadius: Int
    get() = runBlocking {
        applicationContext.dataStore.data.map { it[KEY_DEVICE_RADIUS] }.first().orZero()
    }
    set(value) {
        DEVICE_RADIUS = value
        runBlocking {
            applicationContext.dataStore.edit {
                it[KEY_DEVICE_RADIUS] = value
            }
        }
    }