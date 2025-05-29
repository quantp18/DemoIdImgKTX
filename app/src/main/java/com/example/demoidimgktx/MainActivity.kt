package com.example.demoidimgktx

import android.app.ActionBar.LayoutParams
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.demoidimgktx.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import com.example.demoidimgktx.custom_view.ClipTransformImageView
import com.example.demoidimgktx.utils.BitmapHelper
import com.example.demoidimgktx.utils.FileUtils
import com.google.gson.reflect.TypeToken
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding : ActivityMainBinding
    var isShowBitmap = false
    val gson = GsonBuilder().setPrettyPrinting().create()
    var timeLoad = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewBinding.root.setOnClickListener {
            findIndexNew()
        }

        viewBinding.root.setOnLongClickListener {
            isShowBitmap = !isShowBitmap
            val bitmap = mergeOverlaysBelowImgFg(viewBinding.main, viewBinding.imgFg)
            Log.e("BitmapHelper", "Bitmap Size => width : ${bitmap?.width} - height : ${bitmap?.height}")
            if (isShowBitmap) {
                viewBinding.imagePreview.setImageBitmap(bitmap)
                viewBinding.imagePreview.scaleType = ImageView.ScaleType.FIT_CENTER
                viewBinding.imagePreview.visibility = ImageView.VISIBLE
            } else {
                viewBinding.imagePreview.visibility = ImageView.GONE
            }
            true
        }

        viewBinding.imgCenter.onBoxClick = { box , index ->
            val clipView = ClipTransformImageView(this).apply {
                setImageResource(R.drawable.banner_enhance)
                setLimitRect(RectF(0f, 0f, box.width.toFloat(), box.height.toFloat()))
                layoutParams = FrameLayout.LayoutParams(box.width, box.height).apply {
                    leftMargin = box.x
                    topMargin = box.y
                }
                z = (10f + index)/10f
                rotation = box.rotation
            }
            viewBinding.main.addView(clipView)
            Log.e("TAG", "Added Clip View ${clipView.z} $index")
        }

    }

    fun findIndexNew() {
        timeLoad = System.currentTimeMillis()
        Log.e("BitmapHelper", "findIndexNew: Starting")
        val frameMeta = FileUtils.readAllFramesMeta(this)[2]
        Log.e("BitmapHelper", "findIndexNew: ${gson.toJson(frameMeta)} ${System.currentTimeMillis() - timeLoad}")

        // Cập nhật bounding box cho imgCenter
        viewBinding.imgCenter.setFrameInfos(frameMeta.frameList)

        try {
            // Đọc foreground từ assets
            Log.e("BitmapHelper", "Read foreground from assets")
            assets.open(frameMeta.foregroundPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                viewBinding.imgFg.setImageBitmap(bitmap)
            }
            Log.e("BitmapHelper", "read foreground from assets time => ${System.currentTimeMillis() - timeLoad}")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi load ảnh foreground", Toast.LENGTH_SHORT).show()
        }
    }


    private fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
        val drawable = imgFg.drawable ?: return null
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        if (drawableWidth <= 0 || drawableHeight <= 0) return null

        val resultBitmap = createBitmap(drawableWidth, drawableHeight)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)

        val fgOffset = IntArray(2)
        imgFg.getLocationOnScreen(fgOffset)

        val bounds = getImageDisplayedRect(imgFg)
        val scaleX = drawableWidth / bounds.width()
        val scaleY = drawableHeight / bounds.height()

        // Vẽ các child
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is ClipTransformImageView) {
                val childOffset = IntArray(2)
                child.getLocationOnScreen(childOffset)

                val dx = (childOffset[0] - fgOffset[0] - bounds.left) * scaleX
                val dy = (childOffset[1] - fgOffset[1] - bounds.top) * scaleY

                canvas.withTranslation(dx, dy) {
                    canvas.withScale(scaleX, scaleY) {
                        canvas.withRotation(child.rotation){
                            child.draw(this)
                        }
                    }
                }
            }
        }

        // Vẽ ảnh gốc
        canvas.withSave {
            drawable.setBounds(0, 0, drawableWidth, drawableHeight)
            drawable.draw(this)
        }

        return resultBitmap
    }

    private fun getImageDisplayedRect(imageView: ImageView): RectF {
        val drawable = imageView.drawable ?: return RectF()
        val matrix = imageView.imageMatrix
        val bounds = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(bounds)
        return bounds
    }


}