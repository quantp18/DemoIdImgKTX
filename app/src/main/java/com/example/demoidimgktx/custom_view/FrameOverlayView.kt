package com.example.demoidimgktx.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withRotation
import com.example.demoidimgktx.R
import com.example.demoidimgktx.model.FrameInfo
import kotlin.math.atan2

class FrameOverlayViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val tolerance = 5f * resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private var zoomIcon: Bitmap? = null
    private var rotateIcon: Bitmap? = null
    private val frameBoxes = mutableListOf<FrameInfo>()
    private var selectedBoxIndex: Int? = null
    private val highlightedIndexes = mutableSetOf<Int>()
    private var indexImageASwap = -1
    private val iconPaint = Paint()
    private var _frameOverlayViewNewListener: FrameOverlayViewNewListener? = null
    private var draggingIcon: String? = null // "zoom" or "rotate"
    private var initialAngle = 0f
    private var initialScale = 1f

    init {
        zoomIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_zoom)
        rotateIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_rotate)
        iconPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK) // Thêm bóng để icon nổi
    }

    fun registerFrameListener(frameOverlayViewNewListener: FrameOverlayViewNewListener) {
        _frameOverlayViewNewListener = frameOverlayViewNewListener
    }

    fun unregisterFrameListener() {
        _frameOverlayViewNewListener = null
    }

    fun getFrameSelected(): FrameInfo? {
        return selectedBoxIndex?.let { frameBoxes.getOrNull(it) } ?: frameBoxes.firstOrNull { !it.haveImage }
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

    fun getFrameInfos(): MutableList<FrameInfo> {
        return frameBoxes
    }

    fun swapHighlightState() {
        val newSet = frameBoxes.indices.filterNot { it in highlightedIndexes }.toSet()
        highlightedIndexes.clear()
        highlightedIndexes.addAll(newSet)
        indexImageASwap = selectedBoxIndex ?: -1
        selectedBoxIndex = null
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        frameBoxes.forEachIndexed { index, box ->
            val cx = box.x + box.width / 2f
            val cy = box.y + box.height / 2f
            val isHighlight = index == selectedBoxIndex || highlightedIndexes.contains(index)
            val paintToUse = if (isHighlight) paintHighlight else paintNormal

            canvas.withRotation(box.rotation, cx, cy) {
                drawRect(box.x, box.y, box.x + box.width, box.y + box.height, paintToUse)
            }

            if (box.haveImage && isHighlight) {
                // Áp dụng scale và rotation cho icon
                val matrix = Matrix().apply {
                    postScale(box.scale, box.scale, cx, cy)
                    postRotate(box.rotation, cx, cy)
                }

                val iconSize = 40f
                val padding = 8f
                val rightIconRect = RectF(
                    box.x + box.width - iconSize - padding,
                    box.y + box.height - iconSize - padding,
                    box.x + box.width - padding,
                    box.y + box.height - padding
                )
                val leftIconRect = RectF(
                    box.x + padding,
                    box.y + box.height - iconSize - padding,
                    box.x + iconSize + padding,
                    box.y + box.height - padding
                )

                // Ánh xạ icon rect qua matrix
                matrix.mapRect(rightIconRect)
                matrix.mapRect(leftIconRect)

                zoomIcon?.let { canvas.drawBitmap(it, null, rightIconRect, iconPaint) }
                rotateIcon?.let { canvas.drawBitmap(it, null, leftIconRect, iconPaint) }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y

                // Kiểm tra chạm vào icon
                selectedBoxIndex?.let { index ->
                    val box = frameBoxes[index]
                    if (box.haveImage && highlightedIndexes.contains(index)) {
                        val cx = box.x + box.width / 2f
                        val cy = box.y + box.height / 2f
                        val iconSize = 40f
                        val padding = 8f
                        val rightIconRect = RectF(
                            box.x + box.width - iconSize - padding,
                            box.y + box.height - iconSize - padding,
                            box.x + box.width - padding,
                            box.y + box.height - padding
                        )
                        val leftIconRect = RectF(
                            box.x + padding,
                            box.y + box.height - iconSize - padding,
                            box.x + iconSize + padding,
                            box.y + box.height - padding
                        )

                        // Áp dụng scale và rotation để kiểm tra chạm
                        val matrix = Matrix().apply {
                            postScale(box.scale, box.scale, cx, cy)
                            postRotate(box.rotation, cx, cy)
                        }
                        matrix.mapRect(rightIconRect)
                        matrix.mapRect(leftIconRect)

                        if (rightIconRect.contains(event.x, event.y)) {
                            draggingIcon = "zoom"
                            initialScale = box.scale
                            return true
                        } else if (leftIconRect.contains(event.x, event.y)) {
                            draggingIcon = "rotate"
                            initialAngle = box.rotation
                            return true
                        }
                    }
                }

                // Kiểm tra chạm vào khung
                val canTouchBox = frameBoxes.any { box ->
                    val rect = RectF(
                        box.x,
                        box.y,
                        box.x + box.width,
                        box.y + box.height
                    ).apply {
                        inset(-tolerance, -tolerance)
                    }
                    val rotatedPoint = rotatePoint(event.x, event.y, box.x + box.width / 2f, box.y + box.height / 2f, -box.rotation)
                    val touchedBox = rect.contains(rotatedPoint.first, rotatedPoint.second)
                    val notHaveImage = !box.haveImage
                    touchedBox && (notHaveImage || selectedBoxIndex != box.index)
                }

                return canTouchBox
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggingIcon != null && selectedBoxIndex != null) {
                    val box = frameBoxes[selectedBoxIndex!!]
                    val cx = box.x + box.width / 2f
                    val cy = box.y + box.height / 2f
                    if (draggingIcon == "rotate") {
                        val dx = event.x - cx
                        val dy = event.y - cy
                        val newAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        val deltaAngle = newAngle - initialAngle
                        frameBoxes[selectedBoxIndex!!] = box.copy(rotation = deltaAngle)
                        _frameOverlayViewNewListener?.onDragIconBox(deltaAngle, box.scale)
                        invalidate()
                        return true
                    } else if (draggingIcon == "zoom") {
                        val dy = event.y - downY
                        val scaleFactor = 1f + dy / 200f
                        val newScale = (initialScale * scaleFactor).coerceIn(0.5f, 2f)
                        frameBoxes[selectedBoxIndex!!] = box.copy(scale = newScale)
                        _frameOverlayViewNewListener?.onDragIconBox(box.rotation, newScale)
                        invalidate()
                        return true
                    }
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                draggingIcon = null
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
                        val rotatedPoint = rotatePoint(x, y, box.x + box.width / 2f, box.y + box.height / 2f, -box.rotation)

                        if (rect.contains(rotatedPoint.first, rotatedPoint.second)) {
                            selectedBoxIndex = index
                            highlightedIndexes.clear()
                            highlightedIndexes.add(index)
                            invalidate()

                            if (box.haveImage) {
                                if (indexImageASwap != -1) {
                                    _frameOverlayViewNewListener?.swapHighlightState(indexImageASwap, index)
                                    indexImageASwap = -1
                                } else {
                                    _frameOverlayViewNewListener?.onSelectImage()
                                }
                                return false
                            }

                            if (indexImageASwap != -1) return false

                            _frameOverlayViewNewListener?.onClick(box)
                            return false
                        }
                    }

                    indexImageASwap = -1
                    _frameOverlayViewNewListener?.onTouchOutBox()
                }

                return false
            }
        }

        return false
    }

    private fun rotatePoint(x: Float, y: Float, cx: Float, cy: Float, angle: Float): Pair<Float, Float> {
        val rad = Math.toRadians(angle.toDouble())
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val nx = x - cx
        val ny = y - cy
        val rx = nx * cos - ny * sin
        val ry = nx * sin + ny * cos
        return Pair(rx + cx, ry + cy)
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        zoomIcon?.recycle()
        rotateIcon?.recycle()
        zoomIcon = null
        rotateIcon = null
    }
}

interface FrameOverlayViewNewListener {
    fun onClick(frameInfo: FrameInfo)
    fun swapHighlightState(indexImageA: Int, indexImageB: Int)
    fun onSelectImage()
    fun onTouchOutBox()
    fun onDragIconBox(rotation: Float, scale: Float)
}