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
import com.google.gson.GsonBuilder
import androidx.core.graphics.createBitmap

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

        viewBinding.root.setOnClickListener { drawScaledBoundingFrames() }

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

        viewBinding.imgCenter.onBoxClick = { box, index ->
            val clipView = ClipTransformImageView(this).apply {
                setImageResource(R.drawable.banner_enhance)
                setLimitRect(RectF(0f, 0f, box.width.toFloat(), box.height.toFloat()))
                layoutParams = FrameLayout.LayoutParams(box.width.toInt(), box.height.toInt()).apply {
                    leftMargin = box.x.toInt()
                    topMargin = box.y.toInt()
                }
                z = (10f + index) / 10f
                rotation = box.rotation
            }
            viewBinding.main.addView(clipView)
            Log.d("MainActivity", "Added ClipTransformImageView at index $index")
        }

        viewBinding.button.setOnClickListener {
            viewBinding.imgCenter.swapHighlightState()
        }
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

    private fun drawScaledBoundingFrames() {
        val meta = frameMeta ?: return
        val scaleX = viewBinding.imgFg.width / meta.width.toFloat()
        val scaleY = viewBinding.imgFg.height / meta.height.toFloat()

        val scaledFrames = meta.frameList.map { frame ->
            FrameInfo(
                x = frame.x * scaleX,
                y = frame.y * scaleY,
                rotation = frame.rotation,
                width = frame.width * scaleX,
                height = frame.height * scaleY
            )
        }
        viewBinding.imgCenter.setFrameInfos(scaledFrames)
    }

    private fun mergeOverlaysBelowImgFg(mainLayout: FrameLayout, imgFg: ImageView): Bitmap? {
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

            for (i in 0 until mainLayout.childCount) {
                val child = mainLayout.getChildAt(i)
                if (child is ClipTransformImageView) {
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
}