package com.example.demoidimgktx.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.demoidimgktx.BoundingBox

object BitmapHelper {

    fun findWhiteRegion(bitmap: Bitmap, offset: Int = 15): BoundingBox? {
        val width = bitmap.width
        val height = bitmap.height

        var top = height
        var left = width
        var right = 0
        var bottom = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val isWhite = r > 200 && g > 200 && b > 200
                if (isWhite) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        return if (left <= right && top <= bottom) {
            val adjustedLeft = (left - offset).coerceAtLeast(0)
            val adjustedTop = (top - offset).coerceAtLeast(0)
            val adjustedRight = (right + offset).coerceAtMost(width - 1)
            val adjustedBottom = (bottom + offset).coerceAtMost(height - 1)

            BoundingBox(Rect(adjustedLeft, adjustedTop, adjustedRight, adjustedBottom))
        } else {
            null
        }
    }


    fun scaleBoundingBox(
        box: BoundingBox,
        imageView: ImageView
    ): BoundingBox {
        val matrix = imageView.imageMatrix
        val values = FloatArray(9)
        matrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val left = box.rect.left * scaleX + transX
        val top = box.rect.top * scaleY + transY
        val right = box.rect.right * scaleX + transX
        val bottom = box.rect.bottom * scaleY + transY

        return BoundingBox(Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt()))
    }

    fun getBitmapFromVectorDrawable(context: Context, drawableResId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableResId) ?: throw IllegalArgumentException("Drawable not found")

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }


}