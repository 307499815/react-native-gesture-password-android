package com.reactnativeandroipatternlocker

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
        strokeWidth = 3f
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
        setWillNotDraw(false) // ViewGroup default skips onDraw; force it
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

        // Use 80% of the smaller dimension as grid area
        val gridArea = minDim * 0.8f
        squareSize = gridArea / 3f
        circleRadius = squareSize * 0.25f
        innerCircleRadius = circleRadius * 0.35f
        gridSpacing = squareSize
        outerStrokeWidth = max(1f, circleRadius * 0.08f)

        gridOffsetX = (width - gridArea) / 2f + squareSize / 2f
        gridOffsetY = (height - gridArea) / 2f + squareSize / 2f

        // Set actual positions
        for (row in 0..2) {
            for (col in 0..2) {
                points[row][col].x = gridOffsetX + col * gridSpacing
                points[row][col].y = gridOffsetY + row * gridSpacing
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
        linePaint.strokeWidth = max(2f, circleRadius * 0.15f)

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
