package com.laser.scanner.data.model

import com.laser.scanner.contract.DEVICE_RADIUS
import com.laser.scanner.utils.calculateDegreeGranularity
import com.laser.scanner.utils.getCrc

/**
 * 100 -> 4.5  200 -> 9  300 -> 13.5
 */
class BleReceiveData(bytes: ByteArray, private val degreeSpan: Int) {

    val time = System.currentTimeMillis()

    val header: Int by lazy { bytes[START_INDEX].toInt() }
    val type: Int by lazy { bytes[START_INDEX + 1].toInt() }
    val length: Int by lazy { (bytes[START_INDEX + 3].toInt() shl 8) or (bytes[START_INDEX + 2].toInt() and 0xFF) }
    private val distanceMask: String by lazy {
        buildString {
            val start = START_INDEX + 4
            val end = START_INDEX + 10
            for (index in start..end) {
                append(bytes[index].toInt().toChar())
            }
        }
    }
    val distance: Float by lazy {
        kotlin.runCatching {
            val mm = distanceMask.toFloat() * 1000 + DEVICE_RADIUS
            mm / 1000
        }.getOrDefault(Float.NaN)
    }
    val degreeIndex: Int by lazy { bytes[START_INDEX + 12].toInt() }
    val degreeGranularity: Float by lazy { calculateDegreeGranularity(degreeSpan) }
    val degree: Float by lazy { (degreeIndex - 1) * degreeGranularity }
    val battery: Int by lazy { bytes[START_INDEX + 11].toInt() }

    val distanceErrorInfo: String?
        get() = when (distanceMask) {
            "ERR--10" -> "电量过低"
            "ERR--14" -> "计算错误"
            "ERR--15" -> "超出量程"
            "ERR--16" -> "信号弱或测量时间过长"
            "ERR--18" -> "坏境光太强"
            "ERR--26" -> "超出显示范围"
            else -> null
        }

    fun isValid(): Boolean = !distance.isNaN()

    override fun toString(): String {
        return "BleReceiveData(header=$header, type=$type, length=$length, distanceMask=$distanceMask, distance=$distance, degreeIndex=$degreeIndex, degree=$degree, battery=$battery)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleReceiveData

        if (header != other.header) return false
        if (type != other.type) return false
        if (length != other.length) return false
        if (distance != other.distance) return false
        if (degree != other.degree) return false
        if (battery != other.battery) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header
        result = 31 * result + type
        result = 31 * result + length
        result = 31 * result + distance.hashCode()
        result = 31 * result + degree.hashCode()
        result = 31 * result + battery
        return result
    }

    companion object {
        private const val START_INDEX = 0
    }

}

sealed class BleSendData(
    private val header: Int = 0xFF,
    private val type: Int,
    private val length: Int = 0x0001,
    private val data: Int = 0x00
) {

    fun toByteArray(): ByteArray {
        val headerByte = header.toByte()
        val typeByte = type.toByte()
        val lengthByte1 = length.toByte()
        val lengthByte2 = (length shr 8).toByte()
        val bytes = mutableListOf(headerByte, typeByte, lengthByte1, lengthByte2)
        bytes.add(data.toByte())
        if (length == 2) bytes.add((data shr 8).toByte())
        val crc = bytes.toByteArray().getCrc()
        bytes.add(crc.toByte())
        bytes.add((crc shr 8).toByte())
        return bytes.toByteArray()
    }

    class DegreeSpan(degreeSpan: Int) : BleSendData(type = 0x02, length = 0x0002, data = degreeSpan)

    object Start : BleSendData(type = 0x03)

    object Stop : BleSendData(type = 0x04)

    object LightOn : BleSendData(type = 0x05)

    object LightOff : BleSendData(type = 0x06)

}