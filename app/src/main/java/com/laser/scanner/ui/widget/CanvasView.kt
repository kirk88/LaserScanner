package com.laser.scanner.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.view.children
import com.laser.scanner.BuildConfig
import com.laser.scanner.data.model.DrawInfo
import com.laser.scanner.data.model.DrawPoint
import com.laser.scanner.databinding.LayoutPointEditorBinding
import com.laser.scanner.utils.drawPoints
import com.laser.scanner.utils.getTestDrawInfo
import com.laser.scanner.utils.log
import com.laser.scanner.utils.times
import com.nice.common.helper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint().apply { isAntiAlias = true; isDither = true }
    private val textPaint = TextPaint().apply {
        isAntiAlias = true; isDither = true; textSize = 12.toFloat().dp
    }

    private val path = Path()
    private val textBounds = Rect()

    private val touchArea = RectF()

    private var _drawInfo: DrawInfo? = null

    private var drawDistance: Boolean = false

    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        val drawInfo = _drawInfo ?: return
        drawPoints(
            canvas,
            drawInfo,
            width = width,
            height = height,
            translateX = offsetX,
            translateY = offsetY,
            path = path,
            textBounds = textBounds,
            paint = paint,
            textPaint = textPaint,
            drawDistance = drawDistance,
            drawVertex = BuildConfig.DEBUG,
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        offsetX = w / 2f
        offsetY = h / 2f

        for (child in children) {
            val point = child.tag as? DrawPoint ?: continue

            setViewPosition(child, point)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = super.onTouchEvent(event)
        val drawInfo = _drawInfo ?: return consumed
        val points = drawInfo.points.orEmpty()
        if (points.isEmpty()) {
            return consumed
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x - offsetX
            val y = event.y - offsetY

            touchArea.set(x - TOUCH_OFFSET, y - TOUCH_OFFSET, x + TOUCH_OFFSET, y + TOUCH_OFFSET)

            for (point in points.filter { it.isText() }) {
                val scaledPoint = point * drawInfo.scale

                if (touchArea.contains(scaledPoint.x, scaledPoint.y)) {
                    addView(point, scaledPoint)
                    return true
                }
            }
        }

        return consumed
    }

    private fun addView(point: DrawPoint, scaledPoint: DrawPoint) {
        removeAllViews()

        var editView: EditText? = null

        val view = viewOfBinding<LayoutPointEditorBinding>(layoutInflater, this) {
            val dataInfo = point.data?.toString()?.split(VALUE_DELIMITERS)
            if (dataInfo != null) {
                edit.string = dataInfo.getOrNull(1)
                edit.setSelection(edit.length())
            }

            editView = edit
            btnOk.doOnClick {
                val value = edit.string
                if (dataInfo != null) {
                    point.data = if (value.isNullOrBlank()) dataInfo.first()
                    else "${dataInfo.first()}${VALUE_DELIMITERS}${value}"
                } else {
                    point.data = value
                }
                removeView(root)

                invalidate()
            }
        }
        addView(view)

        view.tag = scaledPoint

        view.alpha = 0f
        view.post {
            setViewPosition(view, scaledPoint)
            view.alpha = 1f

            editView?.showIme()
        }
    }

    private fun setViewPosition(view: View, point: DrawPoint) {
        view.x = point.x + offsetX - view.width / 2
        view.y = point.y + offsetY - view.height / 2
    }

    fun setDrawInfo(drawInfo: DrawInfo?) {
        _drawInfo = drawInfo
        invalidate()

        if (drawInfo == null) {
            removeAllViews()
        }
    }

    fun setDrawDistance(drawDistance: Boolean) {
        this.drawDistance = drawDistance
        invalidate()
    }

    private companion object {
        const val TOUCH_OFFSET = 50f

        const val VALUE_DELIMITERS = "\n"
    }

}