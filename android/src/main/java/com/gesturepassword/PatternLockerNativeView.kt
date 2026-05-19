package com.gesturepassword

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.*

/**
 * A custom FrameLayout that draws a 3x3 pattern lock grid using Canvas.
 * This is the core view component of react-native-androd-pattern-locker.
 *
 * The 3x3 grid consists of 9 circles arranged in a square layout.
 * Users draw patterns by touching and dragging between circles.
 */
class PatternLockerNativeView(context: Context) : FrameLayout(context) {

    // ==== Point data model ====
    data class Point(
        var x: Float,
        var y: Float,
        var index: Int = 0,       // 1-based index (1-9)
        var isSelected: Boolean = false,
        var isError: Boolean = false
    )

    // ==== Configuration properties (set from JS) ====
    var isError: Boolean = false
        set(value) {
            field = value
            if (value) {
                // Mark all selected points as error
                for (p in selectedPoints) {
                    p.isError = true
                }
            } else {
                for (p in points.flatten()) {
                    p.isError = false
                }
            }
            invalidate()
        }

    // Colors (parsed ARGB ints from JS processColor)
    var normalColor: Int = Color.parseColor("#5FA8FC")
    var hitColor: Int = Color.parseColor("#5FA8FC")
    var errorColor: Int = Color.parseColor("#D93609")
    var fillColor: Int = Color.parseColor("#FFFFFF")
    var bgColor: Int = Color.parseColor("#292B38")
        set(value) {
            field = value
            setBackgroundColor(value)
            invalidate()
        }

    // Behavior flags
    var enableSkip: Boolean = false        // allowCross
    var freezeDurationMs: Long = 0L        // interval in ms for auto-clean
    var enableAutoClean: Boolean = false   // auto-clean enabled if interval > 0
    var transparentLine: Boolean = false   // draw invisible connection lines
    var showInnerCircle: Boolean = true    // draw inner fill when selected (matches JS innerCircle)
    var showOuterCircle: Boolean = true    // draw outer border (matches JS outerCircle)

    // ==== Internal state ====
    private val points: Array<Array<Point>> = Array(3) { Array(3) { Point(0f, 0f, 0) } }
    private val selectedPoints = mutableListOf<Point>()
    private var currentX = 0f
    private var currentY = 0f
    private var isDrawing = false
    private var isFrozen = false
    private val freezeHandler = android.os.Handler(context.mainLooper)

    // ==== Paint objects ====
    private val circleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val circleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    // ==== Dimensions ====
    private var circleRadius = 0f
    private var innerCircleRadius = 0f
    private var gridSpacing = 0f
    private var gridOffsetX = 0f
    private var gridOffsetY = 0f
    private var squareSize = 0f
    private var outerStrokeWidth = 3f
    private var density = 0f

    // ==== Touch listener interface ====
    var onPatternStart: (() -> Unit)? = null
    var onPatternEnd: ((password: String) -> Unit)? = null
    var onPatternReset: (() -> Unit)? = null

    // ==== Initialization ====
    init {
        // Initialize point indices (1-9, left-to-right, top-to-bottom)
        for (row in 0..2) {
            for (col in 0..2) {
                points[row][col] = Point(0f, 0f, row * 3 + col + 1)
            }
        }
        
        setWillNotDraw(false)
        setBackgroundColor(bgColor) // Use View's own background instead of canvas.drawColor
    }

    // ==== Layout measurement ====
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateLayout(w, h)
        invalidate()
    }

    private fun calculateLayout(w: Int, h: Int) {
        val width = w.toFloat()
        val height = h.toFloat()
        val minDim = min(width, height)
        density = resources.displayMetrics.density
        android.util.Log.d("GestureSize", "w=" + w + " h=" + h + " density=" + density + " minDim=" + minDim + " radius=" + minDim/10)

        // 对齐 JS 版：JS 用 dp 值，Canvas 用 px 值，需要 density 换算
        // JS borderWidth:2dp → px = 2 * density
        // JS line height:1dp → px = 1 * density
        outerStrokeWidth = 2f * density
        linePaint.strokeWidth = 1f * density
        activeLinePaint.strokeWidth = 1f * density

        // 直接对齐 JS 版 react-native-gesture-password 算法
        // JS: radius = width / 10; margin = radius
        //     间距 = 3*radius
        //     circles[i] = { x: col*3*r + margin + r, y: row*3*r + margin + r, r: radius }
        //     九宫格总宽 = 8*radius，在 board 容器(宽=Width)中居中(左右各留 r)
        val radius = minDim / 10f
        circleRadius = radius
        innerCircleRadius = radius * 0.33f
        gridSpacing = radius * 3f
        outerStrokeWidth = 3f
        squareSize = gridSpacing

        // 九宫格在 View 中居中（JS 版 board 容器宽=Width=10r，总宽=8r，左右各留 r）
        // View 宽度 = minDim = 10r，所以直接使用 JS 版坐标即可居中
        val offsetX = (width - minDim) / 2f  // 如果 View 不是正方形，水平居中
        val offsetY = (height - minDim) / 2f  // 垂直居中

        for (row in 0..2) {
            for (col in 0..2) {
                val x = col * (radius * 2f + radius) + radius + radius
                val y = row * (radius * 2f + radius) + radius + radius
                points[row][col].x = x + offsetX
                points[row][col].y = y + offsetY
            }
        }
    }

    // ==== Drawing ====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val currentColor = if (isError) errorColor else hitColor
        drawConnectionLines(canvas, currentColor)
        if (isDrawing && selectedPoints.isNotEmpty()) {
            val last = selectedPoints.last()
            if (!transparentLine) {
                activeLinePaint.color = currentColor
                canvas.drawLine(last.x, last.y, currentX, currentY, activeLinePaint)
            }
        }
        drawCircles(canvas, currentColor)
    }

    private fun drawCircles(canvas: Canvas, currentColor: Int) {
        for (row in 0..2) {
            for (col in 0..2) {
                val point = points[row][col]
                val color = when {
                    point.isError -> errorColor
                    point.isSelected -> hitColor
                    isError -> errorColor
                    else -> normalColor
                }

                // Outer border circle
                if (showOuterCircle) {
                    circleBorderPaint.color = color
                    circleBorderPaint.strokeWidth = outerStrokeWidth
                    canvas.drawCircle(point.x, point.y, circleRadius, circleBorderPaint)
                }

                // Inner fill (selected or error state)
                if ((point.isSelected || point.isError) && showInnerCircle) {
                    circleFillPaint.color = color
                    canvas.drawCircle(point.x, point.y, innerCircleRadius, circleFillPaint)
                }
            }
        }
    }

    private fun drawConnectionLines(canvas: Canvas, currentColor: Int) {
        if (selectedPoints.size < 2) return
        if (transparentLine) return // Invisible lines — skip drawing

        linePaint.color = if (isError) errorColor else hitColor
        linePaint.strokeWidth = 1f * density  // JS 版 line height:1dp

        for (i in 0 until selectedPoints.size - 1) {
            val from = selectedPoints[i]
            val to = selectedPoints[i + 1]
            canvas.drawLine(from.x, from.y, to.x, to.y, linePaint)
        }
    }

    // ==== Touch handling ====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isFrozen) return true

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                resetSelection()
                isDrawing = true
                val touched = getTouchedPoint(x, y)
                if (touched != null && !touched.isSelected) {
                    selectPoint(touched)
                }
                currentX = x
                currentY = y
                onPatternStart?.invoke()
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                currentX = x
                currentY = y
                val touched = getTouchedPoint(x, y)
                if (touched != null && !touched.isSelected) {
                    // Check for skip-cross logic
                    if (!enableSkip && selectedPoints.isNotEmpty()) {
                        val last = selectedPoints.last()
                        val middle = findMiddlePoint(last, touched)
                        if (middle != null && !middle.isSelected) {
                            selectPoint(middle)
                        }
                    }
                    selectPoint(touched)
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                isDrawing = false
                val password = getPassword()
                onPatternEnd?.invoke(password)

                // Auto-clean if enabled
                if (enableAutoClean && freezeDurationMs > 0) {
                    isFrozen = true
                    freezeHandler.postDelayed({
                        resetSelection()
                        isFrozen = false
                        onPatternReset?.invoke()
                        invalidate()
                    }, freezeDurationMs)
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ==== Hit testing ====
    private fun getTouchedPoint(x: Float, y: Float): Point? {
        val touchRadius = squareSize * 0.45f // generous touch area
        for (row in 0..2) {
            for (col in 0..2) {
                val point = points[row][col]
                val dx = x - point.x
                val dy = y - point.y
                if (sqrt(dx * dx + dy * dy) <= touchRadius) {
                    return point
                }
            }
        }
        return null
    }

    /**
     * Finds the middle point between two non-adjacent circles.
     * For example, if user goes from 1→3 (through 2), returns 2.
     * Only works for horizontal, vertical, or diagonal midpoints.
     */
    private fun findMiddlePoint(from: Point, to: Point): Point? {
        // Check if from and to are valid (both selected or about to be)
        val fromRow = (from.index - 1) / 3
        val fromCol = (from.index - 1) % 3
        val toRow = (to.index - 1) / 3
        val toCol = (to.index - 1) % 3

        // The middle point must be the center of the 3x3:
        // midRow = (fromRow + toRow) / 2, midCol = (fromCol + toCol) / 2
        // Only valid if both sums are even (integer division yields exact middle)
        val midRow = (fromRow + toRow) / 2
        val midCol = (fromCol + toCol) / 2

        // Check if this is a valid middle (sums are even, meaning exact middle)
        if ((fromRow + toRow) % 2 == 0 && (fromCol + toCol) % 2 == 0) {
            // Also verify they are not adjacent (distance > 1 in either axis)
            val rowDiff = abs(fromRow - toRow)
            val colDiff = abs(fromCol - toCol)
            if (rowDiff > 1 || colDiff > 1) {
                return if (midRow in 0..2 && midCol in 0..2) {
                    val mid = points[midRow][midCol]
                    if (!mid.isSelected) mid else null
                } else null
            }
        }
        return null
    }

    // ==== Selection management ====
    private fun selectPoint(point: Point) {
        if (point.isSelected) return
        point.isSelected = true
        point.isError = false
        selectedPoints.add(point)
    }

    private fun resetSelection() {
        for (row in 0..2) {
            for (col in 0..2) {
                points[row][col].isSelected = false
                points[row][col].isError = false
            }
        }
        selectedPoints.clear()
        isError = false
        isDrawing = false
    }

    /**
     * Returns the password as a 1-based index string, e.g. "123456789".
     * This matches the format used by react-native-gesture-password's helper.getRealPassword.
     */
    private fun getPassword(): String {
        return selectedPoints.joinToString("") { it.index.toString() }
    }

    /**
     * Clears the current pattern externally (called from JS).
     */
    fun clearPattern() {
        resetSelection()
        invalidate()
    }


}
