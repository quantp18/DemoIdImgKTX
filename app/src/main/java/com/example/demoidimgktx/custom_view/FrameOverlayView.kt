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
import androidx.core.graphics.withRotation

class FrameOverlayViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val tolerance = 5f * resources.displayMetrics.density

    var onBoxClick: ((FrameInfo, Int) -> Unit)? = null

    private val frameBoxes = mutableListOf<FrameInfo>()
    private var selectedBoxIndex: Int? = null
    private val highlightedIndexes = mutableSetOf<Int>()

    private val paintNormal = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintHighlight = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    fun setFrameInfos(boxes: List<FrameInfo>) {
        frameBoxes.clear()
        frameBoxes.addAll(boxes)
        selectedBoxIndex = null
        highlightedIndexes.clear()
        invalidate()
    }

    fun swapHighlightState() {
        val newSet = frameBoxes.indices.filterNot { it in highlightedIndexes }.toSet()
        highlightedIndexes.clear()
        highlightedIndexes.addAll(newSet)
        selectedBoxIndex = null // bỏ selected khi swap
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        frameBoxes.forEachIndexed { index, box ->
            val cx = box.x + box.width / 2f
            val cy = box.y + box.height / 2f

            canvas.withRotation(box.rotation, cx, cy) {
                val paintToUse = if (index == selectedBoxIndex || highlightedIndexes.contains(index)) {
                    paintHighlight
                } else {
                    paintNormal
                }

                drawRect(
                    box.x.toFloat(),
                    box.y.toFloat(),
                    (box.x + box.width).toFloat(),
                    (box.y + box.height).toFloat(),
                    paintToUse
                )
            }
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
                    selectedBoxIndex = index
                    highlightedIndexes.clear() // chỉ highlight selected
                    highlightedIndexes.add(index)
                    invalidate()
                    onBoxClick?.invoke(box, index)
                    return false
                }
            }
        }
        return false/*super.onTouchEvent(event)*/
    }

    fun clickItemByIndex(index : Int) {
        selectedBoxIndex = index
        highlightedIndexes.clear() // chỉ highlight selected
        highlightedIndexes.add(index)
        invalidate()
    }
}
