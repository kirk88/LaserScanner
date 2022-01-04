package com.laser.scanner.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.laser.scanner.data.table.BleDeviceTable
import com.laser.scanner.data.table.BleRecordTable
import com.nice.common.applicationContext
import com.nice.sqlite.ManagedSupportSQLiteOpenHelper
import com.nice.sqlite.core.*
import com.nice.sqlite.core.ddl.datetime
import com.nice.sqlite.core.ddl.old
import com.nice.sqlite.core.ddl.plus
import com.nice.sqlite.core.dml.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val BleDeviceUpdateTrigger = Trigger.Builder<BleDeviceTable>("ble_device_update_time")
    .at(TriggerTime.After, TriggerType.Update)
    .on(BleDeviceTable)
    .trigger {
        offer(BleDeviceTable)
            .where { it.Id eq old(it.Id) }
            .update { it.UpdateTime(datetime("now", "localtime")) }
    }.build()

private val BleRecordUpdateTrigger = Trigger.Builder<BleRecordTable>("ble_recode_update_time")
    .at(TriggerTime.After, TriggerType.Update)
    .on(BleRecordTable)
    .trigger {
        offer(BleRecordTable)
            .where { it.Id eq old(it.Id) }
            .update { it.UpdateTime(datetime("now", "localtime")) }
    }.build()

private val SQLiteOpenHelperCallback = object : SupportSQLiteOpenHelper.Callback(1) {
    override fun onCreate(db: SupportSQLiteDatabase) {
        offer(BleDeviceTable).create(db) {
            it.Id + it.Name + it.Address + it.CreateTime + it.UpdateTime + it.NameAddressIndex
        }

        offer(BleRecordTable).create(db) {
            it.Id + it.Name + it.DeviceName + it.DeviceAddress + it.Content + it.PngPath + it.SvgPath + it.TxtPath + it.CreateTime + it.UpdateTime
        }

        offer(BleDeviceUpdateTrigger).create(db)
        offer(BleRecordUpdateTrigger).create(db)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}

val Database: ManagedSupportSQLiteOpenHelper by lazy {
    ManagedSupportSQLiteOpenHelper(applicationContext, "laser_scanner", SQLiteOpenHelperCallback)
}

fun ManagedSupportSQLiteOpenHelper.executeAsync(
    scope: CoroutineScope,
    action: SupportSQLiteDatabase.() -> Unit
): Job = scope.launch(Dispatchers.IO) {
    execute(action)
}

fun ManagedSupportSQLiteOpenHelper.transactionAsync(
    scope: CoroutineScope,
    action: SupportSQLiteDatabase.() -> Unit
): Job = scope.launch(Dispatchers.IO) {
    transaction(action = action)
}