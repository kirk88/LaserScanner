package com.laser.scanner.data.model

import com.laser.scanner.utils.log
import com.nice.common.helper.orZero

data class DrawInfo(
    var points: List<DrawPoint>? = null,
    var extraDegree: Float = 0f,
    var vertexCount: Int = 0,
    var scale: Float = 1f
) {

    fun isClosed(): Boolean {
        val lastDegree = points?.findLast { it.type == DrawPoint.TYPE_NORMAL }?.degree.orZero()
        log { "lastDegree: $lastDegree" }
        return lastDegree >= 360F || lastDegree + extraDegree >= 360F
    }

}