package com.laser.scanner.data.table

import com.nice.sqlite.core.Table
import com.nice.sqlite.core.ddl.*

object BleDeviceTable : Table("ble_device") {

    val Id = LongColumn("id") + PrimaryKey(true)
    val Name = StringColumn("name") + NotNull()
    val Address = StringColumn("address") + NotNull()
    val CreateTime = DatetimeColumn("create_time") + Default(datetime("now", "localtime"))
    val UpdateTime = DatetimeColumn("update_time") + Default(datetime("now", "localtime"))

    val NameAddressIndex = UniqueIndex(Name, Address)

}

