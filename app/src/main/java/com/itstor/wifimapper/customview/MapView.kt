package com.itstor.wifimapper.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.itstor.wifimapper.R
import com.itstor.wifimapper.models.PointStatus
import com.itstor.wifimapper.models.Position
import com.itstor.wifimapper.models.SparseGrid
import com.itstor.wifimapper.models.WiFiPoint
import com.itstor.wifimapper.models.ZoomType
import com.itstor.wifimapper.utils.Utils.Companion.closestStandardAngle
import com.itstor.wifimapper.utils.Utils.Companion.roundNumber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        private const val TAG = "MapView"
    }

    var defaultScale = 1.5f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var circleSize = 25f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var distanceBetweenPoints = 100
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var mapPaddingX = 150
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var mapPaddingY = 150
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // Grid and Mapping Properties
    private var points: SparseGrid<WiFiPoint> = SparseGrid(WiFiPoint(PointStatus.UNRECORDED))
    private var zoomType = ZoomType.FOCUS
    private var currentScale = defaultScale
    private var mapWidth = 0
    private var mapHeight = 0
    private var currentPosition: Position = Position(0, 0)
    private var centerPosition: Position = Position(0, 0)
    var rotationDegrees = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var nextPosition: Position = Position(0, 0)
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var xOffset = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var yOffset = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // Paint Properties
    private val pointCirclePaint = initPointCirclePaint()
    private val currentPointRingPaint = initCurrentPointRingPaint()
    private val textPaint = initTextPaint()
    private val gridLinePaint = initGridLinePaint()

    private fun initCurrentPointRingPaint() = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = ActivityCompat.getColor(context, R.color.seed)
    }

    private fun initPointCirclePaint() = Paint().apply {
        style = Paint.Style.FILL
    }

    private fun initTextPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 16f
        textAlign = Paint.Align.CENTER
        textAlignment = TEXT_ALIGNMENT_CENTER
    }

    private fun initGridLinePaint() = Paint().apply {
        color = ActivityCompat.getColor(context, R.color.md_theme_light_outline_soft)
        strokeWidth = 1f
        isAntiAlias = true
    }

    fun zoomIn() {
        currentScale += 0.1f
        invalidate()
    }

    fun zoomOut() {
        currentScale -= 0.1f
        invalidate()
    }

    fun setZoomType(zoomType: ZoomType) {
        this.zoomType = zoomType
        invalidate()
    }

    fun setPoints(points: SparseGrid<WiFiPoint>) {
        this.points = points
        calculateMapSize()
        setCenterPoint()
        invalidate()
    }

    fun invalidateMap() {
        calculateMapSize()
        setCenterPoint()
        invalidate()
    }

    fun setFocusPoint(x: Int, y: Int) {
        this.currentPosition = Position(x, y)
        invalidate()
    }

    fun setFocusPoint(position: Position) {
        this.currentPosition = position
        invalidate()
    }

    private fun setCenterPoint() {
        centerPosition = points.centerOfGrid() ?: Position(0, 0)
    }

    private fun calculateMapSize() {
        mapWidth =
            ((abs(points.minOfRow()) + abs(points.maxOfRow())) * (distanceBetweenPoints + circleSize)).roundToInt() + mapPaddingX
        mapHeight =
            ((abs(points.minOfCol()) + abs(points.maxOfCol())) * (distanceBetweenPoints + circleSize)).roundToInt() + mapPaddingY

    }

    private fun drawGrid(canvas: Canvas, scale: Float) {
        val diagonal = roundNumber(
            (hypot(width.toFloat(), height.toFloat()) * defaultScale / scale).toInt(), floor(
                log10(
                    distanceBetweenPoints.toDouble()
                )
            ).toInt()
        )
        val maxDimension =
            max(width + abs(xOffset.toInt()), height + abs(yOffset.toInt())) * defaultScale / scale

        for (i in -diagonal..diagonal step distanceBetweenPoints) {
            // Horizontal lines
            canvas.drawLine(
                -maxDimension,
                i.toFloat(),
                maxDimension,
                i.toFloat(),
                gridLinePaint
            )

            // Vertical lines
            canvas.drawLine(
                i.toFloat(),
                -maxDimension,
                i.toFloat(),
                maxDimension,
                gridLinePaint
            )
        }
    }

    private fun drawNextStepArrow(canvas: Canvas, focusPosition: Position) {
        canvas.save()

        val nextArrowCenter =
            currentPosition * distanceBetweenPoints - focusPosition * distanceBetweenPoints

        canvas.rotate(
            -closestStandardAngle(rotationDegrees.toInt()).toFloat(),
            nextArrowCenter.x.toFloat(),
            nextArrowCenter.y.toFloat()
        )

        val drawableRadius = circleSize + 5
        val left = (nextArrowCenter.x - drawableRadius).toInt()
        val top = (nextArrowCenter.y - drawableRadius + distanceBetweenPoints / 2).toInt()
        val right = (nextArrowCenter.x + drawableRadius).toInt()
        val bottom = (nextArrowCenter.y + drawableRadius + distanceBetweenPoints / 2).toInt()

        val drawable =
            ContextCompat.getDrawable(context, R.drawable.round_keyboard_arrow_down_24)
        drawable?.let {
            DrawableCompat.setTint(
                it,
                ContextCompat.getColor(context, R.color.md_theme_light_primary)
            )
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)
        }

        canvas.restore()
    }

    private fun drawCurrentPointRing(canvas: Canvas, focusPosition: Position) {
        val currentPointPosition =
            currentPosition * distanceBetweenPoints - focusPosition * distanceBetweenPoints
        canvas.drawCircle(
            currentPointPosition.x.toFloat(),
            currentPointPosition.y.toFloat(),
            circleSize + 5,
            currentPointRingPaint
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        val centerX = width / 2f + xOffset
        val centerY = height / 2f + yOffset

        val focusPosition: Position
        val canvasScale: Float
        val canvasRotation: Float
        when (zoomType) {
            ZoomType.FOCUS -> {
                canvasScale = currentScale
                focusPosition = currentPosition
                canvasRotation = rotationDegrees
            }

            ZoomType.SHOW_ALL -> {
                canvasScale = min(width.toFloat() / mapWidth, height.toFloat() / mapHeight)
                focusPosition = centerPosition
                canvasRotation = 0f
            }
        }

        canvas.scale(1f, -1f, centerX, centerY)
        canvas.scale(canvasScale, canvasScale, centerX, centerY)
        canvas.rotate(canvasRotation, centerX, centerY)
        canvas.translate(centerX, centerY)

        drawGrid(canvas, canvasScale)

        points.forEachElement { position, point ->
            val pointPosition =
                position * distanceBetweenPoints - focusPosition * distanceBetweenPoints

            pointCirclePaint.color = getColorForStatus(point.status)
            canvas.drawCircle(
                pointPosition.x.toFloat(),
                pointPosition.y.toFloat(),
                circleSize,
                pointCirclePaint
            )

            if (position.x % 5 == 0 && position.y % 5 == 0) {
                drawText(
                    canvas,
                    "(${position.x}, ${position.y})",
                    pointPosition.x.toFloat(),
                    -pointPosition.y + circleSize * 2
                )
            }
        }

        drawText(
            canvas,
            "(0, 0)",
            -((focusPosition * distanceBetweenPoints).x).toFloat(),
            (focusPosition * distanceBetweenPoints).y + circleSize * 2
        )

        drawNextStepArrow(canvas, focusPosition)
        drawCurrentPointRing(canvas, focusPosition)

        canvas.restore()
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float) {
        canvas.save()

        canvas.scale(1f, -1f, 0f, 0f)

        canvas.drawText(
            text,
            x, y, textPaint
        )

        canvas.restore()
    }

    private fun getColorForStatus(status: PointStatus): Int {
        val color = when (status) {
            PointStatus.UNRECORDED -> Color.GRAY
            PointStatus.RECORDED -> Color.GREEN
        }

        return color
    }
}
