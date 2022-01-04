package com.laser.scanner.data.model

data class DrawPoint(
    var x: Float,
    var y: Float,
    var degree: Float = 0F,
    var type: Int = TYPE_NORMAL,
    var data: Any? = null
) {
    fun isValid(): Boolean {
        return !x.isNaN() && !y.isNaN()
    }

    fun isText(): Boolean {
        return type == TYPE_TEXT
    }

    fun isVertex(): Boolean {
        return type == TYPE_VERTEX
    }

    fun isPlaceholder(): Boolean {
        return type == TYPE_PLACEHOLDER
    }

    companion object {
        const val TYPE_NORMAL = 1 //默认点
        const val TYPE_VERTEX = 2 //计算出的顶点
        const val TYPE_TEXT = 3 //文字
        const val TYPE_PLACEHOLDER = 4 //占位，无效的点
    }
}