package com.example.demoidimgktx

import android.app.ActionBar.LayoutParams
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


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding : ActivityMainBinding
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
            findIndexList()
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

}