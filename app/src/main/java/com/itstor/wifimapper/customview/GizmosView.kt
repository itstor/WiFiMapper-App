package com.itstor.wifimapper.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class GizmosView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val xAxisPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
        color = 0xFFFF0000.toInt()
    }

    private val yAxisPaint = Paint(xAxisPaint).apply {
        color = 0xFF00FF00.toInt()
    }

    private val xAxisCirclePaint = Paint(xAxisPaint).apply {
        style = Paint.Style.FILL
    }

    private val yAxisCirclePaint = Paint(yAxisPaint).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 25f
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var angle = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.rotate(-angle, centerX, centerY)

        canvas.drawLine(centerX, centerY, centerX, centerY / 6, yAxisPaint)
        canvas.drawLine(centerX, centerY, width.toFloat() - centerX / 6, centerY, xAxisPaint)

        // Draw Circle
        canvas.drawCircle(centerX, centerY / 6, 20f, yAxisCirclePaint)
        canvas.drawCircle(width.toFloat() - centerX / 6, centerY, 20f, xAxisCirclePaint)

        // Draw Text
        canvas.drawText("Y", centerX, centerY / 6 + 20f / 2, textPaint)
        canvas.drawText("X", width.toFloat() - centerX / 6, centerY + 20f / 2, textPaint)

        canvas.restore()
    }
}