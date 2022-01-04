package com.laser.scanner.data.model

import com.laser.scanner.utils.addSideMarks
import com.laser.scanner.utils.addVertexToPoints
import com.laser.scanner.utils.calculatePoint
import com.laser.scanner.utils.log

data class ShowInfo(
    var degree: Float = 0F,
    var distance: Float = 0F,
    var battery: Int = 0,
    val drawInfo: DrawInfo = DrawInfo()
) {

    private var lastDegreeGranularity: Float = 0F

    private val _rawDataList = mutableListOf<BleReceiveData>()
    val rawDataList: List<BleReceiveData> = _rawDataList.toList()

    val size: Int get() = _rawDataList.size

    fun clear() {
        degree = 0F
        distance = 0F
        battery = 0
        drawInfo.points = null
        _rawDataList.clear()
    }

    fun addData(data: BleReceiveData, addVertex: Boolean = true): Boolean {
        val degreeGranularity = lastDegreeGranularity
        lastDegreeGranularity = data.degreeGranularity
        if (addVertex && data.degreeGranularity != degreeGranularity) return true

        val points = mutableListOf<DrawPoint>()

        var totalDegree = 0F
        val rawDataList = _rawDataList + data
        for ((index, rawData) in rawDataList.withIndex()) {
            val preDegreeGranularity = rawDataList.getOrNull(index - 1)?.degreeGranularity
            val curDegreeGranularity = rawData.degreeGranularity
            totalDegree += (if (index == 0) 0F else {
                if (preDegreeGranularity == null) {
                    curDegreeGranularity
                } else {
                    if (preDegreeGranularity != curDegreeGranularity) {
                        curDegreeGranularity + preDegreeGranularity
                    } else {
                        curDegreeGranularity
                    }
                }
            })
            points.add(calculatePoint(totalDegree, rawData.distance * 1000).also {
                if (index == rawDataList.size - 1) {
                    log { "distance: ${rawData.distance}  degree: $totalDegree" }
                }
            })
        }

        val lastPoint = points.last()
        val lastDegree = lastPoint.degree

        if (lastDegree >= 360F) {
            return false
        }

        _rawDataList.add(data)

        if (addVertex) {
            drawInfo.vertexCount = addVertexToPoints(points)

            addSideMarks(points)
        }

        drawInfo.points = points
        drawInfo.extraDegree = data.degreeGranularity

        if (data.isValid()) {
            degree = lastDegree
            distance = data.distance
            battery = data.battery
        }

        return lastDegree + data.degreeGranularity < 360F
    }

}