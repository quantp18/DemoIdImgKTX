package com.example.demoidimgktx

import android.app.ActionBar.LayoutParams
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withSave
import androidx.core.graphics.withScale
import com.google.gson.reflect.TypeToken


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding : ActivityMainBinding
    var isShowBitmap = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val gson = GsonBuilder().setPrettyPrinting().create()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewBinding.root.setOnClickListener {
            findIndexSecond()
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

        viewBinding.imgCenter.onBoxClick = { box ->
            val clipView = ClipTransformImageView(this).apply {
                setImageResource(R.drawable.ic_launcher_background)
                setLimitRect(RectF(0f, 0f, box.rect.width().toFloat(), box.rect.height().toFloat()))
                layoutParams = LayoutParams(box.rect.width(), box.rect.height()).apply {
                    leftMargin = box.rect.left
                    topMargin = box.rect.top
                }
//                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            (viewBinding.main as FrameLayout).addView(clipView)
            Toast.makeText(this, "Add View", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findIndexList() {
        viewBinding.imgFg.setImageResource(R.drawable.temp1_fg)
        val timeStart = System.currentTimeMillis()

        lifecycleScope.launch {
            val imageResList = listOf(
                R.drawable.temp1_index1,
                R.drawable.temp1_index2,
                R.drawable.temp1_index3
            )

            Log.e("BitmapHelper", "Starting processing...")

            val gson = GsonBuilder().setPrettyPrinting().create()

            val boundingBoxList = withContext(Dispatchers.Default) {
                imageResList.map { resId ->
                    async {
                        val bitmap = BitmapHelper.getBitmapFromVectorDrawable(this@MainActivity, resId)
                        val boundingBox = BitmapHelper.findWhiteRegion(bitmap)
                        BitmapHelper.scaleBoundingBox(boundingBox!!, viewBinding.imgFg)
                    }
                }.awaitAll()
            }

            viewBinding.imgCenter.setBoundingBoxes(boundingBoxList)

            Log.e("BitmapHelper", "Result => ${gson.toJson(boundingBoxList)}  Time=${System.currentTimeMillis() - timeStart}ms")
        }
    }

    private fun findIndexSecond() {
        val timeStart = System.currentTimeMillis()
        viewBinding.imgFg.setImageResource(R.drawable.temp2_fg)

        val imageResList = listOf(
            R.drawable.temp2_index1,
            R.drawable.temp2_index2
        )

        lifecycleScope.launch {
            Log.d("BitmapHelper", "Starting processing...")

            val boundingBoxList = withContext(Dispatchers.IO) {
//                imageResList.map { resId ->
//                    async {
//                        processImageAndGetBoundingBox(resId)
//                    }
//                }.awaitAll().filterNotNull()

                val json = "[{\"angle\":0.0,\"rect\":{\"bottom\":534,\"left\":214,\"right\":491,\"top\":240}},{\"angle\":0.0,\"rect\":{\"bottom\":390,\"left\":643,\"right\":957,\"top\":149}}]"
                val typeToken = object : TypeToken<List<BoundingBox>>() {}.type
                Gson().fromJson<List<BoundingBox>>(json, typeToken)
            }

            viewBinding.imgCenter.setBoundingBoxes(boundingBoxList)

            val elapsed = System.currentTimeMillis() - timeStart
            Log.d("BitmapHelper", "Result => ${Gson().toJson(boundingBoxList)} | Time=${elapsed}ms")
        }
    }

    data class ImageResourceAndAngle(val resId: Int, val angle: Float)

//    private fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
//        val drawable = imgFg.drawable
//
//        val width = drawable.intrinsicWidth
//        val height = drawable.intrinsicHeight
//        if (width == 0 || height == 0) return null
//
//        val resultBitmap = createBitmap(width, height)
//        val canvas = Canvas(resultBitmap)
////        canvas.drawColor(Color.WHITE)
//
//        // ===== 1. Vẽ các ClipTransformImageView (scale theo imageMatrix) =====
//        val imageMatrix = Matrix(imgFg.imageMatrix) // clone để không ảnh hưởng gốc
//        val inverseMatrix = Matrix()
//        if (!imageMatrix.invert(inverseMatrix)) return null
//
//        val imgFgOffset = IntArray(2)
//        imgFg.getLocationOnScreen(imgFgOffset)
//
//        for (i in 0 until mainLayout.childCount) {
//            val child = mainLayout.getChildAt(i)
//            if (child is ClipTransformImageView) {
//                // Vị trí tuyệt đối của child trên màn hình
//                val childOffset = IntArray(2)
//                child.getLocationOnScreen(childOffset)
//
//                // Tính dx, dy tương đối so với imgFg
//                val dx = (childOffset[0] - imgFgOffset[0]).toFloat()
//                val dy = (childOffset[1] - imgFgOffset[1]).toFloat()
//
//                // Dùng matrix ngược để map về toạ độ ảnh gốc
//                val mapped = floatArrayOf(dx, dy)
//                inverseMatrix.mapPoints(mapped)
//
//                canvas.withTranslation(mapped[0], mapped[1]) {
//                    child.draw(this)
//                }
//            }
//        }
//        // ===== 2. Vẽ imgFg.drawable bằng matrix, thay vì draw toàn view =====
//        if (drawable != null) {
//            val fgOffset = IntArray(2)
//            imgFg.getLocationOnScreen(fgOffset)
//
//            val layoutOffset = IntArray(2)
//            mainLayout.getLocationOnScreen(layoutOffset)
//            canvas.withSave {
//                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
////                drawable.draw(this)
//            }
//        }
//
//        return resultBitmap
//    }

    private fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
        val drawable = imgFg.drawable ?: return null
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        if (drawableWidth <= 0 || drawableHeight <= 0) return null

        val resultBitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888)
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
                        child.draw(this)
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