package com.example.demoidimgktx

import android.app.ActionBar.LayoutParams
import android.graphics.Bitmap
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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation


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
            if (isShowBitmap) {
                viewBinding.imagePreview.setImageBitmap(bitmap)
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
        lifecycleScope.launch {
            val imageResList = listOf(
                R.drawable.temp2_index1,
                R.drawable.temp2_index2
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

    data class ImageResourceAndAngle(val resId: Int, val angle: Float)

    private fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
        val width = mainLayout.width
        val height = mainLayout.height
        if (width == 0 || height == 0) return null

        val resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)

        canvas.withTranslation(0f, 0f) {
            drawColor(Color.WHITE)
        }
        // ===== 1. Vẽ các ClipTransformImageView =====
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)

            if (child is ClipTransformImageView) {
                val offset = IntArray(2)
                child.getLocationOnScreen(offset)

                val layoutOffset = IntArray(2)
                mainLayout.getLocationOnScreen(layoutOffset)

                val dx = offset[0] - layoutOffset[0]
                val dy = offset[1] - layoutOffset[1]

                canvas.withTranslation(dx.toFloat(), dy.toFloat()) {
                    child.draw(this)
                }
            }
        }

        // ===== 2. Vẽ imgFg cuối cùng (trên cùng) =====
        val fgOffset = IntArray(2)
        imgFg.getLocationOnScreen(fgOffset)

        val layoutOffset = IntArray(2)
        mainLayout.getLocationOnScreen(layoutOffset)

        val dx = fgOffset[0] - layoutOffset[0]
        val dy = fgOffset[1] - layoutOffset[1]

        canvas.withTranslation(dx.toFloat(), dy.toFloat()) {
            imgFg.draw(this)
        }

        return resultBitmap
    }

}