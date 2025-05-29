package com.example.demoidimgktx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import androidx.core.graphics.withClip

class ClipTransformImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val drawMatrix = Matrix()
    private var limitRect = RectF()
    private val imageRect = RectF()

    private var lastTouchCount = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastMidPointX = 0f
    private var lastMidPointY = 0f
    private var lastFingerDistance = 0f

    private var rotation = 0f
    private var prevRotation = 0f
    private var midPointX = 0f
    private var midPointY = 0f

    private var minScale = 0.5f // Scale tối thiểu là 0.5 so với ảnh gốc

    init {
        scaleType = ScaleType.MATRIX
        isClickable = true
        isFocusable = true
    }

    fun setLimitRect(rect: RectF) {
        limitRect.set(rect)
        drawable?.let {
            imageRect.set(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())

            // Tính scale ban đầu để ảnh fit vào limitRect
            val scale = minOf(limitRect.width() / imageRect.width(), limitRect.height() / imageRect.height())

            // Thiết lập minScale là 0.5 so với kích thước gốc
            minScale = 0.5f

            drawMatrix.reset()
            drawMatrix.setScale(scale, scale)

            // Căn giữa ảnh trong limitRect
            val dx = limitRect.left + (limitRect.width() - imageRect.width() * scale) / 2f
            val dy = limitRect.top + (limitRect.height() - imageRect.height() * scale) / 2f
            drawMatrix.postTranslate(dx, dy)

            imageMatrix = drawMatrix
            invalidate()
        }
    }

    fun setImageWithLimitRect(resId: Int, rect: RectF) {
        setImageResource(resId)

        // Chờ drawable sẵn sàng (tránh scale sai do intrinsicWidth = 0)
        post {
            val d = drawable
            if (d != null && d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
                setLimitRect(rect)
            } else {
                // Fallback: delay thêm 1 frame
                postDelayed({
                    drawable?.let {
                        if (it.intrinsicWidth > 0 && it.intrinsicHeight > 0) {
                            setLimitRect(rect)
                        }
                    }
                }, 16) // khoảng 1 frame (60fps)
            }
        }
    }


    override fun onDraw(canvas: Canvas) {
        canvas.withClip(limitRect) {
            imageMatrix = drawMatrix
            super.onDraw(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchCount = 1
                lastX = event.x
                lastY = event.y
                prevRotation = 0f
                lastFingerDistance = 0f
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                lastTouchCount = event.pointerCount
                if (lastTouchCount >= 2) {
                    midPointX = (event.getX(0) + event.getX(1)) / 2f
                    midPointY = (event.getY(0) + event.getY(1)) / 2f
                    lastMidPointX = midPointX
                    lastMidPointY = midPointY
                    prevRotation = rotation(event)
                    lastFingerDistance = fingerDistance(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (lastTouchCount == 1 && event.pointerCount == 1) {
                    // Di chuyển ảnh bằng 1 ngón
                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    // Ngưỡng để tránh jitter
                    if (dx * dx + dy * dy > 25f) {
                        drawMatrix.postTranslate(dx, dy)
                        imageMatrix = drawMatrix
                        invalidate()
                        lastX = event.x
                        lastY = event.y
                    }
                } else if (lastTouchCount >= 2 && event.pointerCount >= 2) {
                    // Di chuyển ảnh bằng 2 ngón
                    val newMidPointX = (event.getX(0) + event.getX(1)) / 2f
                    val newMidPointY = (event.getY(0) + event.getY(1)) / 2f
                    val dx = newMidPointX - lastMidPointX
                    val dy = newMidPointY - lastMidPointY
                    drawMatrix.postTranslate(dx, dy)
                    lastMidPointX = newMidPointX
                    lastMidPointY = newMidPointY

                    // Xoay khi dùng 2 ngón
                    val currentRotation = rotation(event)
                    val deltaRotation = currentRotation - prevRotation
                    drawMatrix.postRotate(deltaRotation, midPointX, midPointY)
                    prevRotation = currentRotation

                    // Phóng to/thu nhỏ khi dùng 2 ngón
                    val currentFingerDistance = fingerDistance(event)
                    if (lastFingerDistance > 10f && currentFingerDistance > 10f) { // Ngưỡng tối thiểu để tránh chia cho số quá nhỏ
                        val scaleFactor = currentFingerDistance / lastFingerDistance
                        val currentScale = getMatrixScale(drawMatrix)
                        val newScale = currentScale * scaleFactor

                        // Giới hạn scaleFactor để tránh thay đổi đột ngột
                        val clampedScaleFactor = max(0.8f, min(1.2f, scaleFactor))

                        if (newScale >= minScale) {
                            drawMatrix.postScale(clampedScaleFactor, clampedScaleFactor, midPointX, midPointY)
                        } else {
                            val scaleToMin = minScale / currentScale
                            drawMatrix.postScale(scaleToMin, scaleToMin, midPointX, midPointY)
                        }
                        lastFingerDistance = currentFingerDistance
                    }

                    imageMatrix = drawMatrix
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                lastTouchCount = event.pointerCount - 1
                if (lastTouchCount < 2) {
                    prevRotation = 0f
                    lastFingerDistance = 0f
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastTouchCount = 0
                prevRotation = 0f
                lastFingerDistance = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun rotation(event: MotionEvent): Float {
        if (event.pointerCount >= 2) {
            val deltaX = (event.getX(0) - event.getX(1))
            val deltaY = (event.getY(0) - event.getY(1))
            return Math.toDegrees(atan2(deltaY, deltaX).toDouble()).toFloat()
        }
        return 0f
    }

    private fun fingerDistance(event: MotionEvent): Float {
        if (event.pointerCount >= 2) {
            val dx = event.getX(0) - event.getX(1)
            val dy = event.getY(0) - event.getY(1)
            return sqrt(dx * dx + dy * dy)
        }
        return 0f
    }

    private fun getMatrixScale(matrix: Matrix): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        val a = values[Matrix.MSCALE_X]
        val b = values[Matrix.MSKEW_X]
        return sqrt(a * a + b * b) // Tính scale thực sự
    }
}