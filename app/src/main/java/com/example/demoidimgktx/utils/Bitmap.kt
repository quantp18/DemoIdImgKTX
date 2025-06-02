package com.example.demoidimgktx.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.example.demoidimgktx.custom_view.ClipTransformImageView
import com.example.demoidimgktx.data.model.FrameInfo
import com.example.demoidimgktx.data.model.FramesMeta

fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
    val drawable = imgFg.drawable ?: return null
    val (dWidth, dHeight) = drawable.intrinsicWidth to drawable.intrinsicHeight
    if (dWidth <= 0 || dHeight <= 0) return null

    val bounds = getImageDisplayedRect(imgFg)
    val scaleX = dWidth / bounds.width()
    val scaleY = dHeight / bounds.height()
    val fgOffset = IntArray(2).apply { imgFg.getLocationOnScreen(this) }

    return createBitmap(dWidth, dHeight).apply {
        val canvas = Canvas(this)
        canvas.drawColor(Color.WHITE)

        // Sắp xếp theo zIndex tăng dần (để vẽ từ dưới lên)
        val sortedChildren = (0 until mainLayout.childCount)
            .map { mainLayout.getChildAt(it) }
            .filterIsInstance<ClipTransformImageView>()
            .sortedBy { it.z }

        for (child in sortedChildren) {
            val childOffset = IntArray(2).apply { child.getLocationOnScreen(this) }
            val dx = (childOffset[0] - fgOffset[0] - bounds.left) * scaleX
            val dy = (childOffset[1] - fgOffset[1] - bounds.top) * scaleY

            canvas.withTranslation(dx, dy) {
                withScale(scaleX, scaleY) {
                    withRotation(child.rotation) {
                        child.draw(this)
                    }
                }
            }
        }

        canvas.withSave {
            drawable.setBounds(0, 0, dWidth, dHeight)
            drawable.draw(this)
        }
    }
}


private fun getImageDisplayedRect(imageView: ImageView): RectF {
    val drawable = imageView.drawable ?: return RectF()
    return RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat()).apply {
        imageView.imageMatrix.mapRect(this)
    }
}

fun drawScaledBoundingFrames(frameMeta : FramesMeta?, actualWidth : Float, actualHeight: Float): List<FrameInfo> {
    val meta = frameMeta ?: return emptyList()
    val scaleX = actualWidth / meta.width
    val scaleY = actualHeight / meta.height

    val scaledFrames = meta.frameList.mapIndexed { index, frame ->
        FrameInfo(
            x = frame.x * scaleX,
            y = frame.y * scaleY,
            rotation = frame.rotation,
            width = frame.width * scaleX,
            height = frame.height * scaleY,
            index = index
        )
    }
    return scaledFrames
}


fun getFormatCompress(isHaveAlpha : Boolean): Bitmap.CompressFormat {
    return if (isHaveAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Bitmap.CompressFormat.WEBP_LOSSLESS
        }else{
            Bitmap.CompressFormat.PNG
        }
    } else{
        Bitmap.CompressFormat.JPEG
    }
}
