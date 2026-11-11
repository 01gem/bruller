package com.gem.bruller.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Full-screen transparent overlay that draws the aim ruler and its floating toolbar.
 */
class RulerOverlayView(context: Context) : View(context) {

    var onClose: (() -> Unit)? = null
    var onLock: (() -> Unit)? = null

    var isLocked: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private var settings: OverlaySettings = OverlaySettings.load(context)

    private val density = resources.displayMetrics.density

    private fun dp(value: Float): Float = value * density

    private var pivotX = 0f
    private var pivotY = 0f
    private var angleDeg = settings.angleDegrees
    private var lengthPx = 0f
    private var geometryReady = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xE6071E18.toInt()
    }
    private val toolbarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xE6122E25.toInt()
    }
    private val toolbarStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = 0x33FFFFFF
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(2f)
        color = 0xFFF4EEDF.toInt()
    }
    private val iconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFF4EEDF.toInt()
    }

    private val toolbarButtons = List(4) { RectF() }
    private var toolbarLeft = 0f
    private var toolbarTop = 0f
    private var pressedButton = -1

    private var touchMode = MODE_NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f
    private var gestureStartAngle = 0f
    private var gestureStartLength = 0f
    private var gestureStartFingerAngle = 0f
    private var gestureStartDistance = 0f

    fun applySettings(newSettings: OverlaySettings) {
        val lengthChanged = kotlin.math.abs(newSettings.lengthFraction - settings.lengthFraction) > 0.0001f
        settings = newSettings
        if (geometryReady && lengthChanged) {
            lengthPx = newSettings.lengthFraction * min(width, height)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        if (oldw > 0 && oldh > 0) {
            pivotX = (pivotX / oldw) * w
            pivotY = (pivotY / oldh) * h
        } else {
            pivotX = settings.pivotXFraction * w
            pivotY = settings.pivotYFraction * h
        }
        lengthPx = settings.lengthFraction * min(w, h)
        geometryReady = true
        layoutToolbar(w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!geometryReady || width == 0 || height == 0) return

        val baseColor = settings.color.argb
        val alpha = settings.opacity
        val mainColor = withAlpha(baseColor, alpha)
        val softColor = withAlpha(baseColor, alpha * 0.55f)

        val rad = Math.toRadians(angleDeg.toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = sin(rad).toFloat()
        val perpX = -dirY
        val perpY = dirX
        val endX = pivotX + dirX * lengthPx
        val endY = pivotY + dirY * lengthPx

        if (settings.showAngle) {
            drawAngleArc(canvas, softColor)
        }

        when (settings.style) {
            RulerStyle.AIM_LINE -> drawLineTicks(canvas, dirX, dirY, perpX, perpY, softColor)
            RulerStyle.PROTRACTOR -> drawProtractor(canvas, softColor, mainColor)
            RulerStyle.CROSSHAIR -> drawCrosshair(canvas, endX, endY, dirX, dirY, perpX, perpY, softColor, mainColor)
        }

        linePaint.color = mainColor
        linePaint.strokeWidth = dp(settings.thicknessDp)
        canvas.drawLine(pivotX, pivotY, endX, endY, linePaint)

        if (settings.showGhostBall) {
            drawGhostBall(canvas, endX, endY, baseColor, alpha)
        } else {
            drawEndKnob(canvas, endX, endY, mainColor)
        }

        drawPivot(canvas, mainColor)

        if (settings.showAngle) {
            drawAngleLabel(canvas)
        }

        if (!isLocked) {
            drawToolbar(canvas)
        }
    }

    private fun drawLineTicks(
        canvas: Canvas,
        dirX: Float,
        dirY: Float,
        perpX: Float,
        perpY: Float,
        color: Int
    ) {
        if (!settings.showTicks) return
        tickPaint.color = color
        tickPaint.strokeWidth = dp(1.2f)
        for (i in 1..9) {
            val t = i / 10f
            val x = pivotX + dirX * lengthPx * t
            val y = pivotY + dirY * lengthPx * t
            val half = if (i % 5 == 0) dp(9f) else dp(5f)
            canvas.drawLine(
                x - perpX * half, y - perpY * half,
                x + perpX * half, y + perpY * half,
                tickPaint
            )
        }
    }

    private fun drawProtractor(canvas: Canvas, softColor: Int, mainColor: Int) {
        val radius = min(lengthPx * 0.55f, dp(130f))
        tickPaint.color = softColor
        tickPaint.strokeWidth = dp(1.1f)

        ringPaint.color = withAlpha(settings.color.argb, settings.opacity * 0.28f)
        ringPaint.strokeWidth = dp(1f)
        canvas.drawCircle(pivotX, pivotY, radius, ringPaint)

        for (deg in 0 until 360 step 10) {
            val a = Math.toRadians(deg.toDouble())
            val isMajor = deg % 30 == 0
            val inner = radius - dp(if (isMajor) 14f else 7f)
            canvas.drawLine(
                pivotX + cos(a).toFloat() * inner,
                pivotY + sin(a).toFloat() * inner,
                pivotX + cos(a).toFloat() * radius,
                pivotY + sin(a).toFloat() * radius,
                tickPaint
            )
        }

        val a = Math.toRadians(normalizedAngle().toDouble())
        ringPaint.color = mainColor
        ringPaint.strokeWidth = dp(2.2f)
        canvas.drawLine(
            pivotX + cos(a).toFloat() * (radius - dp(18f)),
            pivotY + sin(a).toFloat() * (radius - dp(18f)),
            pivotX + cos(a).toFloat() * radius,
            pivotY + sin(a).toFloat() * radius,
            ringPaint
        )
    }

    private fun drawCrosshair(
        canvas: Canvas,
        endX: Float,
        endY: Float,
        dirX: Float,
        dirY: Float,
        perpX: Float,
        perpY: Float,
        softColor: Int,
        mainColor: Int
    ) {
        val half = max(dp(settings.ghostBallDp * 3.5f), dp(36f))
        tickPaint.color = mainColor
        tickPaint.strokeWidth = dp(max(1.5f, settings.thicknessDp * 0.7f))
        canvas.drawLine(
            endX - perpX * half, endY - perpY * half,
            endX + perpX * half, endY + perpY * half,
            tickPaint
        )

        val cross = dp(9f)
        tickPaint.color = softColor
        tickPaint.strokeWidth = dp(1.2f)
        canvas.drawLine(
            pivotX - dirX * cross, pivotY - dirY * cross,
            pivotX + dirX * cross, pivotY + dirY * cross,
            tickPaint
        )
        canvas.drawLine(
            pivotX - perpX * cross, pivotY - perpY * cross,
            pivotX + perpX * cross, pivotY + perpY * cross,
            tickPaint
        )
    }

    private fun drawAngleArc(canvas: Canvas, color: Int) {
        val radius = dp(62f)
        val rect = RectF(pivotX - radius, pivotY - radius, pivotX + radius, pivotY + radius)
        ringPaint.color = color
        ringPaint.strokeWidth = dp(1.2f)
        canvas.drawArc(rect, 0f, angleDeg, false, ringPaint)
    }

    private fun drawAngleLabel(canvas: Canvas) {
        val text = "${normalizedAngle().roundToInt()}°"
        textPaint.textSize = dp(13f)
        textPaint.color = settings.color.argb

        val textWidth = textPaint.measureText(text)
        val metrics = textPaint.fontMetrics
        val textHeight = metrics.descent - metrics.ascent
        val padH = dp(8f)
        val padV = dp(4f)
        val boxWidth = textWidth + padH * 2
        val boxHeight = textHeight + padV * 2

        val midRad = Math.toRadians((angleDeg / 2f).toDouble())
        val labelRadius = dp(80f)
        val centerX = (pivotX + cos(midRad).toFloat() * labelRadius)
            .coerceIn(boxWidth / 2 + dp(6f), width - boxWidth / 2 - dp(6f))
        val centerY = (pivotY + sin(midRad).toFloat() * labelRadius)
            .coerceIn(boxHeight / 2 + dp(6f), height - boxHeight / 2 - dp(6f))

        val rect = RectF(
            centerX - boxWidth / 2,
            centerY - boxHeight / 2,
            centerX + boxWidth / 2,
            centerY + boxHeight / 2
        )
        canvas.drawRoundRect(rect, dp(8f), dp(8f), labelBgPaint)
        canvas.drawText(
            text,
            centerX - textWidth / 2,
            centerY - (metrics.ascent + metrics.descent) / 2,
            textPaint
        )
    }

    private fun drawGhostBall(canvas: Canvas, endX: Float, endY: Float, baseColor: Int, alpha: Float) {
        val radius = dp(settings.ghostBallDp)
        fillPaint.color = withAlpha(baseColor, alpha * 0.16f)
        canvas.drawCircle(endX, endY, radius, fillPaint)
        ringPaint.color = withAlpha(baseColor, alpha)
        ringPaint.strokeWidth = dp(2f)
        canvas.drawCircle(endX, endY, radius, ringPaint)
        fillPaint.color = withAlpha(baseColor, alpha)
        canvas.drawCircle(endX, endY, dp(2.5f), fillPaint)
    }

    private fun drawEndKnob(canvas: Canvas, endX: Float, endY: Float, color: Int) {
        fillPaint.color = 0xCC071E18.toInt()
        canvas.drawCircle(endX, endY, dp(10f), fillPaint)
        ringPaint.color = color
        ringPaint.strokeWidth = dp(2f)
        canvas.drawCircle(endX, endY, dp(10f), ringPaint)
        fillPaint.color = color
        canvas.drawCircle(endX, endY, dp(2.5f), fillPaint)
    }

    private fun drawPivot(canvas: Canvas, color: Int) {
        fillPaint.color = 0xCC071E18.toInt()
        canvas.drawCircle(pivotX, pivotY, dp(10f), fillPaint)
        ringPaint.color = color
        ringPaint.strokeWidth = dp(2f)
        canvas.drawCircle(pivotX, pivotY, dp(10f), ringPaint)
        fillPaint.color = color
        canvas.drawCircle(pivotX, pivotY, dp(3f), fillPaint)
    }

    private fun layoutToolbar(viewWidth: Int) {
        val buttonSize = dp(42f)
        val gap = dp(6f)
        val pad = dp(8f)
        val totalWidth = buttonSize * 4 + gap * 3 + pad * 2
        toolbarLeft = (viewWidth - totalWidth) / 2f
        toolbarTop = statusBarHeightPx() + dp(14f)

        for (i in 0 until 4) {
            val left = toolbarLeft + pad + i * (buttonSize + gap)
            toolbarButtons[i].set(
                left,
                toolbarTop + pad,
                left + buttonSize,
                toolbarTop + pad + buttonSize
            )
        }
    }

    private fun drawToolbar(canvas: Canvas) {
        if (toolbarButtons[0].isEmpty) return
        val pad = dp(8f)
        val width = toolbarButtons[3].right + pad - toolbarLeft
        val height = toolbarButtons[0].height() + pad * 2
        val rect = RectF(toolbarLeft, toolbarTop, toolbarLeft + width, toolbarTop + height)

        canvas.drawRoundRect(rect, height / 2f, height / 2f, toolbarBgPaint)
        canvas.drawRoundRect(rect, height / 2f, height / 2f, toolbarStrokePaint)

        for (i in 0 until 4) {
            val button = toolbarButtons[i]
            if (pressedButton == i) {
                fillPaint.color = 0x33FFFFFF
                canvas.drawCircle(button.centerX(), button.centerY(), button.width() / 2f, fillPaint)
            }
            iconPaint.color = 0xFFF4EEDF.toInt()
            when (i) {
                0 -> drawLockIcon(canvas, button.centerX(), button.centerY())
                1 -> drawColorIcon(canvas, button.centerX(), button.centerY())
                2 -> drawOpacityIcon(canvas, button.centerX(), button.centerY())
                3 -> drawCloseIcon(canvas, button.centerX(), button.centerY())
            }
        }
    }

    private fun drawLockIcon(canvas: Canvas, cx: Float, cy: Float) {
        val body = RectF(cx - dp(7f), cy - dp(1f), cx + dp(7f), cy + dp(9f))
        canvas.drawRoundRect(body, dp(2.5f), dp(2.5f), iconPaint)
        val shackle = RectF(cx - dp(5f), cy - dp(9f), cx + dp(5f), cy + dp(1f))
        canvas.drawArc(shackle, 180f, 180f, false, iconPaint)
    }

    private fun drawColorIcon(canvas: Canvas, cx: Float, cy: Float) {
        iconFillPaint.color = settings.color.argb
        canvas.drawCircle(cx, cy, dp(7f), iconFillPaint)
        canvas.drawCircle(cx, cy, dp(9.5f), iconPaint)
    }

    private fun drawOpacityIcon(canvas: Canvas, cx: Float, cy: Float) {
        val radius = dp(8f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        iconFillPaint.color = 0x80F4EEDF.toInt()
        canvas.drawArc(rect, 90f, 180f, true, iconFillPaint)
        canvas.drawCircle(cx, cy, radius, iconPaint)
    }

    private fun drawCloseIcon(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawLine(cx - dp(6f), cy - dp(6f), cx + dp(6f), cy + dp(6f), iconPaint)
        canvas.drawLine(cx + dp(6f), cy - dp(6f), cx - dp(6f), cy + dp(6f), iconPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastTouchX = event.x
                lastTouchY = event.y

                pressedButton = toolbarButtons.indexOfFirst { it.contains(event.x, event.y) }
                if (pressedButton >= 0) {
                    invalidate()
                    return true
                }

                val endX = pivotX + cos(Math.toRadians(angleDeg.toDouble())).toFloat() * lengthPx
                val endY = pivotY + sin(Math.toRadians(angleDeg.toDouble())).toFloat() * lengthPx
                val endDistance = hypot(event.x - endX, event.y - endY)
                val handleRadius = max(dp(34f), dp(settings.ghostBallDp + 16f))

                gestureStartAngle = angleDeg
                gestureStartLength = lengthPx
                touchMode = if (endDistance <= handleRadius) MODE_ROTATE else MODE_MOVE
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    touchMode = MODE_TRANSFORM
                    gestureStartAngle = angleDeg
                    gestureStartLength = lengthPx
                    gestureStartDistance = pointerDistance(event)
                    gestureStartFingerAngle = pointerAngle(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pressedButton >= 0) {
                    if (hypot(event.x - downX, event.y - downY) > dp(12f)) {
                        pressedButton = -1
                        invalidate()
                    }
                    return true
                }

                when (touchMode) {
                    MODE_ROTATE -> {
                        angleDeg = snapAngle(
                            Math.toDegrees(
                                atan2((event.y - pivotY).toDouble(), (event.x - pivotX).toDouble())
                            ).toFloat()
                        )
                        invalidate()
                    }

                    MODE_MOVE -> {
                        pivotX = (pivotX + event.x - lastTouchX).coerceIn(dp(16f), width - dp(16f))
                        pivotY = (pivotY + event.y - lastTouchY)
                            .coerceIn(dp(16f), height - dp(16f))
                        lastTouchX = event.x
                        lastTouchY = event.y
                        invalidate()
                    }

                    MODE_TRANSFORM -> {
                        if (event.pointerCount >= 2) {
                            val distance = pointerDistance(event)
                            if (gestureStartDistance > 1f) {
                                lengthPx = (gestureStartLength * (distance / gestureStartDistance))
                                    .coerceIn(dp(80f), max(width, height) * 1.2f)
                            }
                            angleDeg = snapAngle(
                                gestureStartAngle + (pointerAngle(event) - gestureStartFingerAngle)
                            )
                            invalidate()
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    val remainingIndex = if (event.actionIndex == 0) 1 else 0
                    lastTouchX = event.getX(remainingIndex)
                    lastTouchY = event.getY(remainingIndex)
                    touchMode = MODE_MOVE
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (pressedButton >= 0) {
                    performToolbarAction(pressedButton)
                    pressedButton = -1
                    invalidate()
                    return true
                }
                if (touchMode != MODE_NONE) {
                    saveGeometry()
                }
                touchMode = MODE_NONE
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedButton = -1
                touchMode = MODE_NONE
                invalidate()
                return true
            }
        }
        return true
    }

    private fun performToolbarAction(index: Int) {
        when (index) {
            0 -> onLock?.invoke()

            1 -> {
                val colors = RulerColor.entries
                val next = colors[(colors.indexOf(settings.color) + 1) % colors.size]
                settings = settings.copy(color = next)
                OverlaySettings.save(context, settings)
                invalidate()
            }

            2 -> {
                val levels = listOf(0.35f, 0.6f, 0.85f, 1f)
                val next = levels.firstOrNull { it > settings.opacity + 0.01f } ?: levels.first()
                settings = settings.copy(opacity = next)
                OverlaySettings.save(context, settings)
                invalidate()
            }

            3 -> onClose?.invoke()
        }
    }

    private fun saveGeometry() {
        if (width <= 0 || height <= 0) return
        val minDimension = min(width, height).toFloat()
        val lengthFraction = (lengthPx / minDimension).coerceIn(0.15f, 1.1f)
        settings = settings.copy(lengthFraction = lengthFraction)
        OverlaySettings.saveGeometry(
            context,
            angleDeg,
            pivotX / width,
            pivotY / height,
            lengthFraction
        )
    }

    private fun snapAngle(value: Float): Float {
        val step = settings.snapDegrees
        if (step <= 0) return value
        return (value / step).roundToInt() * step.toFloat()
    }

    private fun normalizedAngle(): Float = ((angleDeg % 360f) + 360f) % 360f

    private fun pointerDistance(event: MotionEvent): Float =
        hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))

    private fun pointerAngle(event: MotionEvent): Float =
        Math.toDegrees(
            atan2(
                (event.getY(1) - event.getY(0)).toDouble(),
                (event.getX(1) - event.getX(0)).toDouble()
            )
        ).toFloat()

    private fun statusBarHeightPx(): Float {
        val insets = rootWindowInsets
        if (insets != null) {
            return insets.getInsets(WindowInsets.Type.statusBars()).top.toFloat()
        }
        return dp(24f)
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    private companion object {
        const val MODE_NONE = 0
        const val MODE_MOVE = 1
        const val MODE_ROTATE = 2
        const val MODE_TRANSFORM = 3
    }
}
