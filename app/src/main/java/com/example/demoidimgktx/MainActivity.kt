package com.example.demoidimgktx

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.demoidimgktx.custom_view.ClipTransformImageView
import com.example.demoidimgktx.databinding.ActivityMainBinding
import com.example.demoidimgktx.model.FrameInfo
import com.example.demoidimgktx.model.FramesMeta
import com.example.demoidimgktx.utils.FileUtils
import com.example.demoidimgktx.utils.drawScaledBoundingFrames
import com.example.demoidimgktx.utils.mergeOverlaysBelowImgFg
import com.google.gson.GsonBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var isShowBitmap = false
    private var timeLoad = 0L
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var frameMeta: FramesMeta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadMetaAndImage()

        viewBinding.root.setOnClickListener {
            val scaledFrames = drawScaledBoundingFrames(
                frameMeta = frameMeta,
                actualWidth = viewBinding.imgFg.width.toFloat(),
                actualHeight = viewBinding.imgFg.height.toFloat()
            )
            viewBinding.imgCenter.setFrameInfos(scaledFrames)
        }

        viewBinding.root.setOnLongClickListener {
            isShowBitmap = !isShowBitmap
            mergeOverlaysBelowImgFg(viewBinding.main, viewBinding.imgFg)?.let { bitmap ->
                viewBinding.imagePreview.apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    visibility = if (isShowBitmap) ImageView.VISIBLE else ImageView.GONE
                }
            }
            true
        }

        viewBinding.imgCenter.onBoxClick = ::addFrame

        viewBinding.button.setOnClickListener {
            viewBinding.imgCenter.swapHighlightState()
        }
    }

    private fun addFrame(frameInfo: FrameInfo, index : Int) {
        val clipView = ClipTransformImageView(this).apply {
            setImageResource(R.drawable.banner_enhance)
            setLimitRect(RectF(0f, 0f, frameInfo.width, frameInfo.height))
            layoutParams = FrameLayout.LayoutParams(frameInfo.width.toInt(), frameInfo.height.toInt()).apply {
                leftMargin = frameInfo.x.toInt()
                topMargin = frameInfo.y.toInt()
            }
            z = (10f + index) / 10f
            rotation = frameInfo.rotation
        }
        viewBinding.main.addView(clipView)
        Log.d("MainActivity", "Added ClipTransformImageView at index $index")
    }

    private fun loadMetaAndImage() {
        timeLoad = System.currentTimeMillis()
        frameMeta = FileUtils.readAllFramesMeta(this)[2].also {
            Log.d("MainActivity", "Loaded meta: ${gson.toJson(it)}")
        }

        try {
            assets.open(frameMeta!!.foregroundPath).use { inputStream ->
                viewBinding.imgFg.setImageBitmap(BitmapFactory.decodeStream(inputStream))
            }
            Log.d("MainActivity", "Foreground image loaded in ${System.currentTimeMillis() - timeLoad}ms")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load foreground", e)
            Toast.makeText(this, "Lỗi load ảnh foreground", Toast.LENGTH_SHORT).show()
        }
    }

}