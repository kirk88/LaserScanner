package com.laser.scanner.data.dao

import com.laser.scanner.data.Database
import com.laser.scanner.data.model.BleDevice
import com.laser.scanner.data.table.BleDeviceTable
import com.nice.sqlite.classParser
import com.nice.sqlite.core.*
import com.nice.sqlite.core.ddl.*
import com.nice.sqlite.core.dml.orderBy
import com.nice.sqlite.core.dml.select
import com.nice.sqlite.toList

object BleDeviceDao {

    fun queryAll(): List<BleDevice> = Database.execute {
        offer(BleDeviceTable)
            .orderBy { desc(datetime(it.UpdateTime)) }
            .select(this)
            .toList(classParser())
    }

    fun queryByDate(datetime: String): List<BleDevice> = Database.execute {
        offer(BleDeviceTable)
            .where { date(it.CreateTime) eq datetime }
            .orderBy { desc(datetime(it.UpdateTime)) }
            .select(this)
            .toList()
    }

    fun save(device: BleDevice): Long = Database.execute {
        offer(BleDeviceTable).insert(this, ConflictAlgorithm.Replace) {
            it.Name(device.name) + it.Address(device.address)
        }
    }

}