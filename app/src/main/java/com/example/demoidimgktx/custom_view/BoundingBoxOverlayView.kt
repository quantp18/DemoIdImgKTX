package com.example.demoidimgktx.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.demoidimgktx.BoundingBox

class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val tolerance = 5f * resources.displayMetrics.density

    var onBoxClick: ((BoundingBox) -> Unit)? = null
    private val boundingBoxes = mutableListOf<BoundingBox>()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val desiredWidth = 500
        val desiredHeight = 500

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }


    fun setBoundingBoxes(boxes: List<BoundingBox>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(boxes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in boundingBoxes) {
            canvas.drawRect(
                box.rect.left.toFloat(),
                box.rect.top.toFloat(),
                box.rect.right.toFloat(),
                box.rect.bottom.toFloat(),
                paint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y


            boundingBoxes.find { box ->
                val inflatedRect = RectF(box.rect)
                inflatedRect.inset(-tolerance, -tolerance) // nới ra 5dp mỗi cạnh
                inflatedRect.contains(x, y)
            }?.let {
                onBoxClick?.invoke(it)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

}
