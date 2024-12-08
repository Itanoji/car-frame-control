package com.example.frameobjectcontrol

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = 0xFFFF0000.toInt() // Красный цвет
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private var boundingBox: Rect? = null

    // Устанавливаем Bounding Box
    fun setBoundingBox(rect: Rect?) {
        boundingBox = rect
        invalidate() // Перерисовываем вид
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundingBox?.let {
            canvas.drawRect(it, paint)
        }
    }
}
