package com.example.demoidimgktx

import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.demoidimgktx.custom_view.ClipTransformImageView
import com.example.demoidimgktx.custom_view.FrameOverlayViewNewListener
import com.example.demoidimgktx.databinding.ActivityMainBinding
import com.example.demoidimgktx.model.FrameContent
import com.example.demoidimgktx.model.FrameInfo
import com.example.demoidimgktx.model.FramesMeta
import com.example.demoidimgktx.utils.FileUtils
import com.example.demoidimgktx.utils.drawScaledBoundingFrames
import com.example.demoidimgktx.utils.mergeOverlaysBelowImgFg
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private var isShowBitmap = false
    private var isReplacing = false
    private var timeLoad = 0L
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var frameMeta: FramesMeta? = null
    private var pickMediaLauncher: ActivityResultLauncher<String>? = null
    private val frameContentList = mutableListOf<FrameContent>()

    private var frameOverlayViewNewListener: FrameOverlayViewNewListener? = null

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

        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.e("TAG", "pickMediaLauncher: $uri")
            handlePickMedia(uri)
        }

        frameOverlayViewNewListener = object : FrameOverlayViewNewListener {
            override fun onClick(frameInfo: FrameInfo) {
                Log.e("frameOverlayViewNewListener", "onClick: ")
                onSelectFromGallery()
            }

            override fun swapHighlightState(indexImageA: Int, indexImageB: Int) {
                Log.e("frameOverlayViewNewListener", "swapHighlightState: ")
                handleSwapHighlight(indexImageA, indexImageB)
            }

            override fun onSelectImage() {
                Log.e("frameOverlayViewNewListener", "onSelectImage: ")
            }

            override fun onTouchOutBox() {
                Log.e("frameOverlayViewNewListener", "onTouchOutBox: ")
            }
        }

        loadMetaAndImage()

        viewBinding.btnSetFrames.setOnClickListener {
            val scaledFrames = drawScaledBoundingFrames(
                frameMeta = frameMeta,
                actualWidth = viewBinding.imgFg.width.toFloat(),
                actualHeight = viewBinding.imgFg.height.toFloat()
            )
            Log.e("TAG", "btnSetFrames: ${scaledFrames.size}")
            viewBinding.imgCenter.setFrameInfos(scaledFrames)
        }

//        viewBinding.root.setOnLongClickListener {
//            isShowBitmap = !isShowBitmap
//            mergeOverlaysBelowImgFg(viewBinding.main, viewBinding.imgFg)?.let { bitmap ->
//                viewBinding.imagePreview.apply {
//                    setImageBitmap(bitmap)
//                    scaleType = ImageView.ScaleType.FIT_CENTER
//                    visibility = if (isShowBitmap) ImageView.VISIBLE else ImageView.GONE
//                }
//            }
//            true
//        }

        viewBinding.button.text = "Swap"
        viewBinding.button.setOnClickListener {
            viewBinding.imgCenter.swapHighlightState()
        }
        viewBinding.btnReplaceImage.setOnClickListener {
            isReplacing = true
            onSelectFromGallery()
        }
        viewBinding.btnDelete.setOnClickListener {
            handleDeleteImage()
        }

        viewBinding.btnSave.setOnClickListener {
            val time = System.currentTimeMillis()
            mergeOverlaysBelowImgFg(viewBinding.main, viewBinding.imgFg)?.let { bitmap ->
                FileUtils.saveBitmapToFile(context = this, bitmap = bitmap)
                Log.e("TAG", "onCreate: ==> ${System.currentTimeMillis() - time}")
            }
        }

        viewBinding.btnRotate.setOnClickListener {
            viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
                val index = frameInfo.index
                val clipTransformImageView = frameContentList[index].clipTransformImageView
                clipTransformImageView.rotateView()
            }
        }

        viewBinding.btnFlip.setOnClickListener {
            viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
                val index = frameInfo.index
                val clipTransformImageView = frameContentList[index].clipTransformImageView
                clipTransformImageView.flipView()
            }
        }
    }

    fun handleSwapHighlight(indexImageA: Int, indexImageB: Int) {
        val frameContentA = frameContentList[indexImageA]
        val frameContentB = frameContentList[indexImageB]
        val clipTransformImageViewA = frameContentA.clipTransformImageView
        val clipTransformImageViewB = frameContentB.clipTransformImageView

        val imagePathA = frameContentA.path
        val imagePathB = frameContentB.path

        frameContentList[indexImageA] = frameContentList[indexImageA].copy(path = imagePathB)
        frameContentList[indexImageB] = frameContentList[indexImageB].copy(path = imagePathA)

        runCatching { clipTransformImageViewA.setImageBitmap(BitmapFactory.decodeFile(imagePathB)) }
        runCatching { clipTransformImageViewB.setImageBitmap(BitmapFactory.decodeFile(imagePathA)) }
    }

    private fun addFrame(frameInfo: FrameInfo) {
        val index = frameInfo.index
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

    private fun addFrameByFile(frameInfo: FrameInfo, file: File) {
        val index = frameInfo.index
        val bitmap = BitmapFactory.decodeFile(file.path)
        val clipView = ClipTransformImageView(this).apply {
            setImageBitmap(bitmap)
            layoutParams = FrameLayout.LayoutParams(frameInfo.width.toInt(), frameInfo.height.toInt()).apply {
                leftMargin = frameInfo.x.toInt()
                topMargin = frameInfo.y.toInt()
            }
            z = (10f + index) / 10f
            rotation = frameInfo.rotation
            setLimitRect(RectF(0f, 0f, frameInfo.width, frameInfo.height))
        }
        frameContentList.add(FrameContent(frameInfo, clipView, file.path))
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

    private fun onSelectFromGallery() {
        pickMediaLauncher?.launch("image/*")
    }

    private fun handlePickMedia(uri: Uri?) {
        if (uri != null) {
            FileUtils.copyImageFileFromUri(this, uri)?.let { mFile ->
                if (!mFile.exists()) return
                viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
                    if (isReplacing){
                        deleteByIndex(frameInfo.index)
                    }
                    addFrameByFile(frameInfo, mFile)
                    viewBinding.imgCenter.updateHaveImage(haveImage = true, index = frameInfo.index)
                }
            }
        }
        isReplacing = false
    }

    private fun handleDeleteImage() {
        viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
            deleteByIndex(frameInfo.index)
        }

    }

    private fun deleteByIndex(indexImage: Int) {
        val frameContent = frameContentList[indexImage]
        viewBinding.main.removeView(frameContent.clipTransformImageView)
        frameContentList.remove(frameContent)
        FileUtils.deleteFile(frameContent.path)
        viewBinding.imgCenter.updateHaveImage(haveImage = false, index = indexImage)
    }

    override fun onResume() {
        super.onResume()
        frameOverlayViewNewListener?.let { viewBinding.imgCenter.registerFrameListener(it) }
    }

    override fun onPause() {
        super.onPause()
        viewBinding.imgCenter.unregisterFrameListener()
    }

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            FileUtils.deleteImageTemp(context = this@MainActivity)
        }
        frameOverlayViewNewListener = null
        super.onDestroy()
    }

}