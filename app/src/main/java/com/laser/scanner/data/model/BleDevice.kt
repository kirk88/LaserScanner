package com.laser.scanner.data.model

import com.nice.sqlite.ClassParserConstructor

data class BleDevice @ClassParserConstructor constructor(
    val id: Long = 0L,
    val name: String,
    val address: String,
    val createTime: String = "",
    val updateTime: String = ""
){

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        if (name != other.name) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + address.hashCode()
        return result
    }

}