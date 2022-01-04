package com.laser.scanner.data.model

import com.nice.sqlite.ClassParserConstructor

data class BleRecord @ClassParserConstructor constructor(
    val id: Long = 0L,
    val name: String,
    val deviceName: String,
    val deviceAddress: String,
    val content: String,
    var pngPath: String? = null,
    var svgPath: String? = null,
    var txtPath: String? = null,
    val createTime: String = "",
    val updateTime: String = ""
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleRecord

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}