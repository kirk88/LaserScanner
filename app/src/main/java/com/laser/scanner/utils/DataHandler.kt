package com.laser.scanner.utils

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import com.google.gson.GsonBuilder
import com.laser.scanner.contract.*
import com.laser.scanner.data.model.DrawInfo
import com.laser.scanner.data.model.DrawPoint
import com.nice.common.helper.dp
import com.nice.common.helper.orElse
import com.nice.common.helper.scale
import com.nice.kotson.fromJson
import java.io.File
import java.math.MathContext
import kotlin.math.*


private val GSON = GsonBuilder().serializeSpecialFloatingPointValues().create()

fun Context.getTestDrawInfo(): DrawInfo {
    val drawInfo = DrawInfo()

    val points = assets.open("data.txt").bufferedReader().useLines {
        mutableListOf<DrawPoint>().apply {
            for (line in it) {
                val data = line.split(",")
                if (data.size != 2) continue
                add(DrawPoint(data.component1().toFloat(), data.component2().toFloat()))
            }
        }
    }
    drawInfo.vertexCount = addVertexToPoints(points)

    addSideMarks(points)

    drawInfo.points = points
    return drawInfo
}

fun checkDivide(amount: Int, count: Int): Boolean {
    if (amount % count == 0) {
        return true // 能被除尽
    } else {
        var m = count
        while (m % 2 == 0) {
            m /= 2 // 当模2不=0时，去尝试对5取模
        }
        while (m % 5 == 0) {
            m /= 5 // 当模5不=0时，则将m让除数取模
        }
        if (amount % m != 0) {
            return false // 不能除尽，如果取模不为0，则表示有2和5以外的因子，数学原理也是别人告诉我的：除数能被分解成N个2或N个5或者N2N5组合，则表示可以除尽
        }
    }
    return true
}

fun ByteArray.decodeToHexString(): String {
    val str = mutableListOf<String>()
    for (b in this) {
        str.add(String.format("%02x", b.toInt() and 0xff))
    }
    return str.joinToString()
}

fun ByteArray.getCrc(): Int {
    var crc = 0x0000ffff
    val polynomial = 0x0000a001
    var i = 0
    var j: Int
    while (i < size) {
        crc = crc xor (this[i].toInt() and 0x000000ff)
        j = 0
        while (j < 8) {
            if (crc and 0x00000001 != 0) {
                crc = crc shr 1
                crc = crc xor polynomial
            } else {
                crc = crc shr 1
            }
            j++
        }
        i++
    }
    return crc
}

fun transformToString(info: DrawInfo): String = GSON.toJson(info)

fun transformToDrawInfo(text: String): DrawInfo = GSON.fromJson(text)

private fun DrawPoint.isSimilar(point: DrawPoint, threshold: Float = 5f): Boolean {
    return abs(x - point.x) < threshold && abs(y - point.y) < threshold
}

private fun DrawPoint.isInner(points: Array<DrawPoint>): Boolean {
    val begin = DrawPoint(0F, 0F)
    val distance = calculateDistance(begin, this)
    for (point in points) {
        if (calculateDistance(begin, point) > distance) return true
    }
    return false
}

private val DrawPoint.quadrant: Int
    get() = when {
        x > 0f && y > 0f -> 1
        x < 0f && y > 0f -> 2
        x < 0f && y < 0f -> 3
        x > 0f && y < 0f -> 4
        y == 0f && x > 0f -> -1
        x == 0f && y > 0f -> -2
        y == 0f && x < 0f -> -3
        x == 0f && y < 0f -> -4
        else -> 0
    }

fun DrawPoint.rotation(degree: Float) {
    val l = degree * Math.PI / 180F

    val cosv = cos(l).toFloat()
    val sinv = sin(l).toFloat()

    val newX = x * cosv - y * sinv
    val newY = x * sinv + y * cosv

    x = newX
    y = newY
}

fun addVertexToPoints(points: MutableList<DrawPoint>): Int {
    if (points.size < 4) return 0

    val pointMap = mutableMapOf<Int, DrawPoint>()

    fun checkValid(index: Int): Pair<Int, DrawPoint>? {
        var startIndex = index
        var point: DrawPoint?
        while (startIndex < points.size) {
            point = points[startIndex]
            if (point.isValid()) return startIndex to point

            startIndex += 1
        }
        return null
    }

    for (index in points.indices) {
        var valid = checkValid(index) ?: break
        val p1 = valid.second
        valid = checkValid(valid.first + 1) ?: break
        val p2 = valid.second
        valid = checkValid(valid.first + 1) ?: break
        val p3 = valid.second
        valid = checkValid(valid.first + 1) ?: break
        val p4 = valid.second

        val vertex = calculateVertex(p1, p2, p3, p4)
        if (vertex != null) {
            pointMap[index + 2] = vertex
        }
    }

    var count = 0
    for ((index, point) in pointMap) {
        points.add(index + count, point)
        count += 1
    }

    return count
}

private fun calculateVertex(
    p1: DrawPoint,
    p2: DrawPoint,
    p3: DrawPoint,
    p4: DrawPoint
): DrawPoint? {
    var x1 = p1.x
    var y1 = p1.y
    var x2 = p2.x
    var y2 = p2.y

    val a1 = y2 - y1
    val b1 = -(x2 - x1)
    val c1 = -x1 * (y2 - y1) + y1 * (x2 - x1)
    val k1 = (x2 - x1).let { if (it == 0F) 0F else (y2 - y1) / it }

    x1 = p3.x
    y1 = p3.y
    x2 = p4.x
    y2 = p4.y

    val a2 = y2 - y1
    val b2 = -(x2 - x1)
    val c2 = -x1 * (y2 - y1) + y1 * (x2 - x1)
    val k2 = (x2 - x1).let { if (it == 0F) 0F else (y2 - y1) / it }

    val value = (k2 - k1) / (1 + k1 * k2)

    val angle = abs(atan(value) * (180 / Math.PI))

    if (angle > 75 && angle < 105) {
        val x = (c2 * b1 - c1 * b2) / (a1 * b2 - a2 * b1)
        val y = (c1 * a2 - c2 * a1) / (a1 * b2 - a2 * b1)

        val vertex = DrawPoint(x, y, type = DrawPoint.TYPE_VERTEX)
        val points = arrayOf(p1, p2, p3, p4)

        if (vertex.isInner(points)) return null

        for (p in points) {
            if (vertex.isSimilar(p)) return null
        }
        return vertex
    }
    return null
}

fun addSideMarks(points: MutableList<DrawPoint>): Boolean {
    if (points.size < 2) return false

    val marks = mutableListOf<DrawPoint>()

    val vertexPoints = points.filter { it.isVertex() }

    fun addMark(p1: DrawPoint, p2: DrawPoint) {
        if (!checkValidSideMark(p1, p2)) return

        val degree = calculateDegree(p1, p2).scale(2)
        val distance = calculateDistance(p1, p2).scale(2)
        val mark = DrawPoint(
            (p1.x + p2.x) / 2,
            (p1.y + p2.y) / 2,
            degree = degree,
            type = DrawPoint.TYPE_TEXT,
            data = distance
        )

        marks.add(mark)
    }

    var first: DrawPoint? = null
    var last: DrawPoint? = null

    for (index in vertexPoints.indices) {
        val p1 = vertexPoints[index]
        if (first == null) {
            first = p1
        }

        if (index + 1 >= vertexPoints.size) {
            last = p1
            break
        }

        val p2 = vertexPoints[index + 1]
        last = p2

        addMark(p1, p2)
    }

    if (first != null && last != null && first != last) {
        addMark(first, last)
    }

    if (marks.isNotEmpty()) {
        return points.addAll(marks)
    }
    return false
}

private fun checkValidSideMark(p1: DrawPoint, p2: DrawPoint): Boolean {
    fun check(q1: Int, q2: Int): Boolean {
        return when (q1) {
            1 -> arrayOf(1, 2, 4).contains(q2) || q2 < 0
            2 -> arrayOf(1, 2, 3).contains(q2) || q2 < 0
            3 -> arrayOf(2, 3, 4).contains(q2) || q2 < 0
            4 -> arrayOf(1, 3, 4).contains(q2) || q2 < 0
            -1 -> arrayOf(1, 4, -2, -4).contains(q2)
            -2 -> arrayOf(1, 2, -1, -3).contains(q2)
            -3 -> arrayOf(2, 3, -2, -4).contains(q2)
            -4 -> arrayOf(3, 4, -1, -2).contains(q2)
            else -> false
        }
    }

    val quadrant1 = p1.quadrant
    val quadrant2 = p2.quadrant
    return check(quadrant1, quadrant2) || check(quadrant2, quadrant1)
}

fun calculatePoint(degree: Float, distance: Float): DrawPoint {
    if (distance.isNaN()) return DrawPoint(Float.NaN, Float.NaN, type = DrawPoint.TYPE_PLACEHOLDER)
    val rad = degree * Math.PI / 180
    val x = sin(rad) * distance
    val y = cos(rad) * distance
    return DrawPoint(x.toFloat(), y.toFloat(), degree)
}

fun calculateDegree(p1: DrawPoint, p2: DrawPoint): Float {
    val radian: Float = atan((p2.y - p1.y) / (p2.x - p1.x))
    val degree = radian * 180 / Math.PI
    return degree.toFloat()
}

fun calculateDegreeGranularity(degreeSpan: Int): Float {
    return degreeSpan.toBigDecimal()
        .divide(200.toBigDecimal(), MathContext.DECIMAL32)
        .multiply(9.toBigDecimal())
        .toFloat()
}

fun calculateDistance(p1: DrawPoint, p2: DrawPoint): Float {
    return ((p2.x - p1.x).pow(2F) + (p2.y - p1.y).pow(2F)).pow(0.5F)
}

fun calculateScale(
    points: List<DrawPoint>,
    width: Int,
    height: Int,
    boxPadding: Int = DEFAULT_BOX_PADDING
): Float = points.filter { it.isValid() }.let {
    if (it.isEmpty()) return 1f
    val maxX = it.maxOf { point -> abs(point.x) }
    val maxY = it.maxOf { point -> abs(point.y) }
    val scaleX = if (maxX != 0f) (width / 2f - boxPadding).toBigDecimal()
        .divide(maxX.toBigDecimal(), MathContext.DECIMAL32).toFloat() else null
    val scaleY = if (maxY != 0f) (height / 2f - boxPadding).toBigDecimal()
        .divide(maxY.toBigDecimal(), MathContext.DECIMAL32).toFloat() else null
    val scale = scaleX ?: scaleY
    if (it.size == 1 && scale != null) return@let scale
    return@let minOf(scaleX.orElse { 1F }, scaleY.orElse { 1F })
}

operator fun List<DrawPoint>.times(scale: Float): List<DrawPoint> = filter { it.isValid() }.map {
    it * scale
}

operator fun DrawPoint.times(scale: Float): DrawPoint {
    val px = x * scale
    val py = y * scale
    return copy(x = px, y = py)
}

operator fun DrawPoint.div(scale: Float): DrawPoint {
    val px = x / scale
    val py = y / scale
    return copy(x = px, y = py)
}

fun drawPoints(
    canvas: Canvas,
    drawInfo: DrawInfo,
    width: Int,
    height: Int,
    translateX: Float = width / 2F,
    translateY: Float = height / 2F,
    strokeWidth: Float = DEFAULT_STROKE_WIDTH,
    strokeColor: Int = DEFAULT_STROKE_COLOR,
    fillColor: Int = DEFAULT_FILL_COLOR,
    path: Path = Path(),
    textBounds: Rect = Rect(),
    paint: Paint = Paint().apply { isAntiAlias = true; isDither = true },
    textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true; isDither = true; textSize = 12.toFloat().dp
    },
    drawDistance: Boolean = false,
    drawVertex: Boolean = false,
    closeable: (Int) -> Boolean = { drawInfo.isClosed() }
) {
    val points = drawInfo.points
    if (points.isNullOrEmpty()) return

    val scaledPoints = points * calculateScale(points, width, height).also {
        drawInfo.scale = it
    }

    path.reset()
    for ((index, point) in scaledPoints.filterNot { it.isText() }.withIndex()) {
        val px = point.x
        val py = point.y

        if (index == 0) {
            path.moveTo(px, py)
        } else {
            path.lineTo(px, py)
        }
    }

    val closed = closeable(points.size)
    if (closed) path.close()

    canvas.save()

    canvas.translate(translateX, translateY)

    if (drawVertex) {
        val pColor = paint.color
        val pWidth = paint.strokeWidth
        val pCap = paint.strokeCap

        paint.strokeWidth = 20f
        paint.color = Color.BLACK
        paint.strokeCap = Paint.Cap.ROUND

        for (point in scaledPoints.filter { it.isVertex() }) {
            canvas.drawPoint(point.x, point.y, paint)
        }

        paint.color = pColor
        paint.strokeWidth = pWidth
        paint.strokeCap = pCap
    }

    paint.style = Paint.Style.FILL
    paint.color = fillColor
    canvas.drawPath(path, paint)

    paint.style = Paint.Style.STROKE
    paint.color = strokeColor
    paint.strokeWidth = strokeWidth
    canvas.drawPath(path, paint)

    if (drawDistance && points.size >= 2) {
        val start = points.first()
        val end = points.last()
        val scaledStart = scaledPoints.first()
        val scaledEnd = scaledPoints.last()

        drawDistanceMarks(canvas, start, end, scaledStart, scaledEnd, textBounds, textPaint, paint)
    }

    if (!drawDistance) {
        drawSideMarks(canvas, scaledPoints.filter { it.isText() }, textBounds, textPaint)
    }

    canvas.restore()
}

private fun drawDistanceMarks(
    canvas: Canvas,
    start: DrawPoint,
    end: DrawPoint,
    scaledStart: DrawPoint,
    scaledEnd: DrawPoint,
    textBounds: Rect,
    textPaint: TextPaint,
    paint: Paint
) {
    val center = DrawPoint(0F, 0F)

    val offset = 10F

    val startDistance = calculateDistance(center, start).scale(2)
    val endDistance = calculateDistance(center, end).scale(2)
    val distanceStr = "距离：${startDistance + endDistance}mm"

    textPaint.getTextBounds(
        distanceStr,
        0,
        distanceStr.length,
        textBounds
    )

    paint.strokeWidth = 1F

    var lineStartX = scaledStart.x + offset
    var lineStartY = scaledStart.y
    var lineEndX = lineStartX + 40
    var lineEndY = scaledStart.y
    canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, paint)

    lineStartX = lineEndX - 20
    lineEndX = lineStartX
    lineEndY = textBounds.height() / 2 + offset

    canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, paint)

    lineStartX = scaledEnd.x + offset
    lineStartY = scaledEnd.y
    lineEndX = lineStartX + 40
    lineEndY = scaledEnd.y
    canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, paint)

    lineStartX = lineEndX - 20
    lineEndX = lineStartX
    lineEndY = -(textBounds.height() / 2 + offset)
    canvas.drawLine(lineStartX, lineStartY, lineEndX, lineEndY, paint)

    val fm = textPaint.fontMetrics
    val baseLine = fm.top / 2 + fm.bottom / 2
    canvas.drawText(distanceStr, center.x + offset, center.y - baseLine, textPaint)

    val startDistanceStr = "${startDistance}mm"
    canvas.drawText(
        startDistanceStr,
        -textPaint.measureText(startDistanceStr) - offset,
        scaledStart.y / 2 - baseLine,
        textPaint
    )

    val endDistanceStr = "${endDistance}mm"
    canvas.drawText(
        endDistanceStr,
        -textPaint.measureText(endDistanceStr) - offset,
        scaledEnd.y / 2 - baseLine,
        textPaint
    )
}

fun drawSideMarks(canvas: Canvas, points: List<DrawPoint>, textBounds: Rect, textPaint: TextPaint) {
    if (points.isEmpty()) return

    textPaint.textAlign = Paint.Align.CENTER


    for (point in points) {
        val texts = point.data?.toString()?.split("\n") ?: continue

        for (index in texts.indices) {
            val text = texts[index]
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val x = point.x
            val y = if (index == 0) point.y else point.y + textBounds.height() + 6f
            textPaint.color = if (index == 0) Color.BLACK else Color.GRAY
            canvas.drawText(text, x, y, textPaint)
        }
    }
}


fun extractPng(content: String, file: File): Boolean {
    val info = transformToDrawInfo(content)
    if (info.points.isNullOrEmpty()) return false

    val strokeWidth = DEFAULT_STROKE_WIDTH
    val width = DEFAULT_BOX_WIDTH
    val height = DEFAULT_BOX_HEIGHT

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.TRANSPARENT)
    val canvas = Canvas(bitmap)
    drawPoints(
        canvas,
        info,
        width = width,
        height = height,
        strokeWidth = strokeWidth
    )

    file.createIfNotExists().outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.flush()
    }
    return true
}

fun extractSvg(content: String, file: File): Boolean {
    val info = transformToDrawInfo(content)
    val points = info.points
    if (points.isNullOrEmpty()) return false

    val strokeWidth = DEFAULT_STROKE_WIDTH
    val width = DEFAULT_BOX_WIDTH
    val height = DEFAULT_BOX_HEIGHT

    val scaledPoints = points * calculateScale(points, width, height)

    val svgData = buildString {
        append("""<svg xmlns="http://www.w3.org/2000/svg" height="100%" width="100%" viewBox="0 0 $width $height">""")
        append("""<path fill="transparent" stroke="red" stroke-width="$strokeWidth" transform="translate(${width / 2},${height / 2})" d="""")

        for ((index, point) in scaledPoints.filterNot { it.isText() }.withIndex()) {
            val px = point.x
            val py = point.y

            if (index == 0) {
                append("M$px,$py")
            } else {
                append("L$px,$py")
            }
        }

        if (info.isClosed()) {
            append("z")
        }

        append("\" />")

        val fontSize = 12.dp
        for (point in scaledPoints.filter { it.isText() }) {
            val texts = point.data?.toString()?.split("\n") ?: continue

            append("""<text font-size="$fontSize" text-anchor="middle" transform="translate(${width / 2},${height / 2})">""")

            for (index in texts.indices) {
                val text = texts[index]
                val x = point.x
                val y = if (index == 0) point.y else point.y + fontSize + 6
                val color = if (index == 0) "black" else "gray"
                append("""<tspan x="$x" y="$y" fill="$color">$text</tspan>""")
            }

            append("</text>")
        }

        append("</svg>")
    }

    file.createIfNotExists().bufferedWriter().use {
        it.write(svgData)
        it.flush()
    }
    return true
}

fun extractTxt(content: String, file: File): Boolean {
    val info = transformToDrawInfo(content)
    val points = info.points
    if (points.isNullOrEmpty()) return false

    file.createIfNotExists().bufferedWriter().use {
        for (point in points.filterNot { p -> p.isText() }) {
            it.appendLine("${point.x},${point.y}")
        }
        it.flush()
    }
    return true
}

fun extractByFileType(content: String, file: File): Boolean {
    return when (file.extension) {
        FILE_TYPE_PNG.lowercase() -> extractPng(content, file)
        FILE_TYPE_SVG.lowercase() -> extractSvg(content, file)
        FILE_TYPE_TXT.lowercase() -> extractTxt(content, file)
        else -> false
    }
}

private fun File.createIfNotExists() = apply {
    val parent = parentFile
    if (parent != null && !parent.exists()) {
        parent.mkdirs()
    }

    if (!exists()) {
        createNewFile()
    }
}
