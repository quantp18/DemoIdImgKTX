package com.example.demoidimgktx.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withRotation
import com.example.demoidimgktx.model.FrameInfo

class FrameOverlayViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val tolerance = 5f * resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private val frameBoxes = mutableListOf<FrameInfo>()
    private var selectedBoxIndex: Int? = null
    private val highlightedIndexes = mutableSetOf<Int>()
    private var indexImageASwap = -1

    private var _frameOverlayViewNewListener: FrameOverlayViewNewListener? = null

    fun registerFrameListener(frameOverlayViewNewListener: FrameOverlayViewNewListener) {
        _frameOverlayViewNewListener = frameOverlayViewNewListener
    }

    fun unregisterFrameListener() {
        _frameOverlayViewNewListener = null
    }


    fun getFrameSelected(): FrameInfo? {
        val frame = try {
            frameBoxes[selectedBoxIndex!!]
        } catch (e: Exception) {
            frameBoxes.firstOrNull { !it.haveImage }
        }
        return frame
    }

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
        indexImageASwap = selectedBoxIndex ?: -1
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
                    box.x,
                    box.y,
                    (box.x + box.width),
                    (box.y + box.height),
                    paintToUse
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y

                // Kiểm tra xem có chạm vào box nào không
                val canTouchBox = frameBoxes.any { box ->
                    val rect = RectF(
                        box.x,
                        box.y,
                        box.x + box.width,
                        box.y + box.height
                    ).apply {
                        inset(-tolerance, -tolerance)
                    }
                    val touchedBox = rect.contains(downX, downY)
                    val notHaveImage = !box.haveImage
                    touchedBox && notHaveImage or (selectedBoxIndex != box.index)
                }

                Log.e("TAG", "onTouchEvent: $canTouchBox", )
                return canTouchBox

            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val distanceSq = dx * dx + dy * dy

                if (distanceSq < tolerance * tolerance) {
                    val x = event.x
                    val y = event.y

                    frameBoxes.forEachIndexed { index, box ->
                        val rect = RectF(
                            box.x,
                            box.y,
                            box.x + box.width,
                            box.y + box.height
                        ).apply {
                            inset(-tolerance, -tolerance)
                        }

                        if (rect.contains(x, y)) {
                            selectedBoxIndex = index
                            highlightedIndexes.clear()
                            highlightedIndexes.add(index)
                            invalidate()

                            // Nếu box đã có ảnh, chỉ highlight và trả về false để cho view dưới xử lý
                            if (box.haveImage) {
                                if (indexImageASwap != -1) {
                                    _frameOverlayViewNewListener?.swapHighlightState(indexImageASwap, index)
                                    indexImageASwap = -1
                                } else {
                                    _frameOverlayViewNewListener?.onSelectImage()
                                }
                                return false // CHO SỰ KIỆN XUỐNG VIEW DƯỚI
                            }

                            // Nếu đang ở trạng thái chọn để swap => không bắt sự kiện
                            if (indexImageASwap != -1) return false

                            _frameOverlayViewNewListener?.onClick(box)
                            return false // chỉ consume khi click hợp lệ vào box trống
                        }
                    }

                    // Không chạm vào box nào
                    indexImageASwap = -1
                    _frameOverlayViewNewListener?.onTouchOutBox()
                }

                return false // Cho touch tiếp xuống view dưới
            }
        }

        return false // default không bắt sự kiện
    }


    private fun clickItemByIndex(index: Int) {
        selectedBoxIndex = index
        highlightedIndexes.clear()
        highlightedIndexes.add(index)
        invalidate()
    }

    fun updateHaveImage(haveImage: Boolean, index: Int) {
        frameBoxes[index] = frameBoxes[index].copy(haveImage = haveImage)
    }
}

interface FrameOverlayViewNewListener {
    fun onClick(frameInfo: FrameInfo)
    fun swapHighlightState(indexImageA: Int, indexImageB: Int)
    fun onSelectImage()
    fun onTouchOutBox()
}