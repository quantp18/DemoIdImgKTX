package com.example.demoidimgktx.custom_view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.example.demoidimgktx.model.FrameInfo
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
    private var minScale = 0.5f
    private var maxScale = 3f

    private var _transformListener: ClipTransformImageViewListener? = null
    private var _frameInfo: FrameInfo? = null

    init {
        scaleType = ScaleType.MATRIX
        isClickable = true
        isFocusable = true
    }

    fun setFrameInfo(frameInfo: FrameInfo) {
        _frameInfo = frameInfo
        rotation = frameInfo.rotation
    }

    fun registerListener(transformListener: ClipTransformImageViewListener) {
        _transformListener = transformListener
    }

    fun unregisterListener() {
        _transformListener = null
    }

    fun setLimitRect(rect: RectF, scaleType: ScaleType = ScaleType.CENTER_CROP) {
        limitRect.set(rect)
        drawable?.let {
            imageRect.set(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())
            val scale = if (scaleType == ScaleType.FIT_CENTER) {
                minOf(limitRect.width() / imageRect.width(), limitRect.height() / imageRect.height())
            } else {
                maxOf(limitRect.width() / imageRect.width(), limitRect.height() / imageRect.height())
            }

            minScale = 0.5f
            drawMatrix.reset()
            drawMatrix.setScale(scale, scale)
            val dx = limitRect.left + (limitRect.width() - imageRect.width() * scale) / 2f
            val dy = limitRect.top + (limitRect.height() - imageRect.height() * scale) / 2f
            drawMatrix.postTranslate(dx, dy)
            imageMatrix = drawMatrix
            invalidate()
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
        val imageCenter = getImageCenter()
        val cx = imageCenter.first
        val cy = imageCenter.second

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
                    lastMidPointX = (event.getX(0) + event.getX(1)) / 2f
                    lastMidPointY = (event.getY(0) + event.getY(1)) / 2f
                    prevRotation = rotation(event)
                    lastFingerDistance = fingerDistance(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (lastTouchCount == 1 && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (dx * dx + dy * dy > 25f) {
                        drawMatrix.postTranslate(dx, dy)
                        imageMatrix = drawMatrix
                        invalidate()
                        lastX = event.x
                        lastY = event.y
                    }
                } else if (lastTouchCount >= 2 && event.pointerCount >= 2) {
                    val newMidPointX = (event.getX(0) + event.getX(1)) / 2f
                    val newMidPointY = (event.getY(0) + event.getY(1)) / 2f
                    val dx = newMidPointX - lastMidPointX
                    val dy = newMidPointY - lastMidPointY
                    drawMatrix.postTranslate(dx, dy)
                    lastMidPointX = newMidPointX
                    lastMidPointY = newMidPointY

                    val currentRotation = rotation(event)
                    val deltaRotation = currentRotation - prevRotation
                    drawMatrix.postRotate(deltaRotation, cx, cy)
                    prevRotation = currentRotation

                    val currentFingerDistance = fingerDistance(event)
                    if (lastFingerDistance > 10f && currentFingerDistance > 10f) {
                        val scaleFactor = currentFingerDistance / lastFingerDistance
                        val currentScale = getMatrixScale(drawMatrix)
                        val newScale = (currentScale * scaleFactor).coerceIn(minScale, maxScale)
                        val clampedScaleFactor = max(0.8f, min(1.2f, scaleFactor))
                        drawMatrix.postScale(clampedScaleFactor, clampedScaleFactor, cx, cy)
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
                _frameInfo?.let { frameInfo ->
                    _transformListener?.translateAndScaleSuccess(
                        frameInfo = frameInfo,
                        rotate = getMatrixRotation(drawMatrix),
                        scale = getMatrixScale(drawMatrix)
                    )
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getImageCenter(): Pair<Float, Float> {
        val mappedRect = RectF(imageRect)
        drawMatrix.mapRect(mappedRect)
        return Pair(mappedRect.centerX(), mappedRect.centerY())
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
        return sqrt(a * a + b * b)
    }

    private fun getMatrixRotation(matrix: Matrix): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return Math.toDegrees(atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble())).toFloat()
    }

    fun rotateView() {
        val imageCenter = getImageCenter()
        drawMatrix.postRotate(90f, imageCenter.first, imageCenter.second)
        imageMatrix = drawMatrix
        invalidate()
        _frameInfo?.let {
            _transformListener?.translateAndScaleSuccess(it, getMatrixRotation(drawMatrix), getMatrixScale(drawMatrix))
        }
    }

    fun flipView() {
        val imageCenter = getImageCenter()
        drawMatrix.postScale(-1f, 1f, imageCenter.first, imageCenter.second)
        imageMatrix = drawMatrix
        invalidate()
        _frameInfo?.let {
            _transformListener?.translateAndScaleSuccess(it, getMatrixRotation(drawMatrix), getMatrixScale(drawMatrix))
        }
    }

    fun updateRotateAndScale(rotation: Float, scale: Float) {
        val imageCenter = getImageCenter()
        drawMatrix.reset()
        drawMatrix.postScale(scale, scale, imageCenter.first, imageCenter.second)
        drawMatrix.postRotate(rotation, imageCenter.first, imageCenter.second)
        imageMatrix = drawMatrix
        invalidate()
        _frameInfo?.let {
            _transformListener?.translateAndScaleSuccess(it, rotation, scale)
        }
    }
}

interface ClipTransformImageViewListener {
    fun translateAndScaleSuccess(frameInfo: FrameInfo, rotate: Float, scale: Float)
}