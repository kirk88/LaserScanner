package com.laser.scanner.data.table

import com.nice.sqlite.core.Table
import com.nice.sqlite.core.ddl.*

object BleRecordTable : Table("ble_record") {

    val Id = LongColumn("id") + PrimaryKey(true)
    val Name = StringColumn("name") + NotNull()
    val DeviceName = StringColumn("device_name") + NotNull()
    val DeviceAddress = StringColumn("device_address") + NotNull()
    val Content = StringColumn("content") + NotNull()
    val PngPath = StringColumn("png_path")
    val SvgPath = StringColumn("svg_path")
    val TxtPath = StringColumn("txt_path")
    val CreateTime = DatetimeColumn("create_time") + Default(datetime("now", "localtime"))
    val UpdateTime = DatetimeColumn("update_time") + Default(datetime("now", "localtime"))

}