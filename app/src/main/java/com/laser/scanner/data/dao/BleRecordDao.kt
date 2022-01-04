package com.laser.scanner.data.dao

import com.laser.scanner.data.Database
import com.laser.scanner.data.model.BleRecord
import com.laser.scanner.data.table.BleRecordTable
import com.nice.sqlite.core.*
import com.nice.sqlite.core.ddl.*
import com.nice.sqlite.core.dml.orderBy
import com.nice.sqlite.core.dml.select
import com.nice.sqlite.core.dml.update
import com.nice.sqlite.toList

object BleRecordDao {

    fun queryAll(): List<BleRecord> = Database.execute {
        offer(BleRecordTable).orderBy { desc(datetime(it.UpdateTime)) }.select(this).toList()
    }

    fun queryByDate(datetime: String): List<BleRecord> = Database.execute {
        offer(BleRecordTable)
            .where { date(it.CreateTime) eq datetime }
            .orderBy { desc(datetime(it.UpdateTime)) }
            .select(this)
            .toList()
    }

    fun save(record: BleRecord): Long = Database.execute {
        offer(BleRecordTable).insert(this, ConflictAlgorithm.Replace) {
            valuesOf(it, record)
        }
    }

    fun update(record: BleRecord): Int = Database.execute {
        offer(BleRecordTable)
            .where { it.Id eq record.id }
            .update(this) { valuesOf(it, record) }
    }

    private fun valuesOf(it: BleRecordTable, record: BleRecord): Bag<ColumnValue> {
        return it.Name(record.name) + it.DeviceName(record.deviceName) +
                it.DeviceAddress(record.deviceAddress) + it.Content(record.content) +
                it.PngPath(record.pngPath) + it.SvgPath(record.svgPath) + it.TxtPath(record.txtPath)
    }

}