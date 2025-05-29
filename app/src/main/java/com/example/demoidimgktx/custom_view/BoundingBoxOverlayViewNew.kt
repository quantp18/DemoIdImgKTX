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
import com.example.demoidimgktx.model.FrameInfo

class FrameOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val tolerance = 5f * resources.displayMetrics.density

    var onBoxClick: ((FrameInfo, Int) -> Unit)? = null
    private val frameBoxes = mutableListOf<FrameInfo>()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    fun setFrameInfos(boxes: List<FrameInfo>) {
        frameBoxes.clear()
        frameBoxes.addAll(boxes)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (box in frameBoxes) {
            val cx = box.x + box.width / 2f
            val cy = box.y + box.height / 2f

            canvas.save()
            canvas.rotate(box.rotation, cx, cy)
            canvas.drawRect(
                box.x.toFloat(),
                box.y.toFloat(),
                (box.x + box.width).toFloat(),
                (box.y + box.height).toFloat(),
                paint
            )
            canvas.restore()
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            frameBoxes.forEachIndexed { index, box ->
                val rect = RectF(
                    box.x.toFloat(),
                    box.y.toFloat(),
                    (box.x + box.width).toFloat(),
                    (box.y + box.height).toFloat()
                )
                rect.inset(-tolerance, -tolerance)

                if (rect.contains(x, y)) {
                    onBoxClick?.invoke(box, index) // 👈 Thêm index
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

}

