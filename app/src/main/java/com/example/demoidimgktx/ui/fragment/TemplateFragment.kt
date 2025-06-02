package com.example.demoidimgktx.ui.fragment

import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.activityViewModels
import com.example.demoidimgktx.R
import com.example.demoidimgktx.custom_view.ClipTransformImageView
import com.example.demoidimgktx.custom_view.ClipTransformImageViewListener
import com.example.demoidimgktx.custom_view.FrameOverlayViewNewListener
import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView
import com.example.demoidimgktx.custom_view.sticker.DrawableSticker
import com.example.demoidimgktx.custom_view.sticker.Sticker
import com.example.demoidimgktx.data.assets.StickersAsset
import com.example.demoidimgktx.data.model.FrameContent
import com.example.demoidimgktx.data.model.FrameInfo
import com.example.demoidimgktx.data.model.FramesMeta
import com.example.demoidimgktx.databinding.ActivityMainBinding
import com.example.demoidimgktx.databinding.FragmentTemplateBinding
import com.example.demoidimgktx.ui.viewmodel.AdjustViewModel
import com.example.demoidimgktx.utils.FileUtils
import com.example.demoidimgktx.utils.NavManager
import com.example.demoidimgktx.utils.drawScaledBoundingFrames
import com.example.demoidimgktx.utils.mergeOverlaysBelowImgFg
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.graphics.drawable.toDrawable
import com.example.demoidimgktx.custom_view.que_shot.QueShotText
import com.example.demoidimgktx.custom_view.que_shot.QueShotTextView

class TemplateFragment : Fragment() {

    private lateinit var viewBinding: FragmentTemplateBinding

    private var isShowBitmap = false
    private var isReplacing = false
    private var timeLoad = 0L
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var frameMeta: FramesMeta? = null
    private var pickMediaLauncher: ActivityResultLauncher<String>? = null
    private var pickMultipleMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private val frameContentList = mutableMapOf<Int, FrameContent>()

    private var frameOverlayViewNewListener: FrameOverlayViewNewListener? = null
    private var clipTransformImageView : ClipTransformImageViewListener? = null
    private val viewModel: AdjustViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentTemplateBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createListener()
        loadMetaAndImage()

        viewBinding.btnSetFrames.setOnClickListener {
            val scaledFrames = drawScaledBoundingFrames(
                frameMeta = frameMeta,
                actualWidth = viewBinding.imgFg.width.toFloat(),
                actualHeight = viewBinding.imgFg.height.toFloat()
            )
            frameMeta = frameMeta?.copy(frameList = scaledFrames)
            Log.e("TAG", "btnSetFrames: ${scaledFrames.size}")
            viewBinding.imgCenter.setFrameInfos(scaledFrames)
            onSelectMultipleFromGallery()
        }
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
                FileUtils.saveBitmapToFile(context = requireContext(), bitmap = bitmap)
                Log.e("TAG", "onCreate: ==> ${System.currentTimeMillis() - time}")
            }
        }

        viewBinding.btnRotate.setOnClickListener {
            viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
                val index = frameInfo.index
                val clipTransformImageView = frameContentList[index]?.clipTransformImageView
                clipTransformImageView?.rotateView()
            }
        }

//        viewBinding.btnFlip.setOnClickListener {
//            viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
//                val index = frameInfo.index
//                val clipTransformImageView = frameContentList[index]?.clipTransformImageView
//                clipTransformImageView?.flipView()
//            }
//        }

        viewBinding.btnAddText.setOnClickListener {
            val queShotText = QueShotText.getDefaultProperties().apply {
                quShotTexts = "Custom Text"
                quShotTextColor = resources.getColor(R.color.white, null)
                quShotTextSize = 24
                quShotTextAlign = 4
                quShotBackgroundColor = resources.getColor(R.color.BackgroundColor, null)
                isQuShotShowBackground = true
                quShotFontName = "font.ttf"
            }
            val stickerView = QueShotStickerView(requireContext()).apply {
                z = 12f
            }
            val textSticker = QueShotTextView(requireContext(), queShotText)
            stickerView.addSticker(textSticker)
            stickerView.setHandlingSticker(textSticker)
            viewBinding.root.addView(stickerView)
        }

        viewBinding.btnAdjust.setOnClickListener {
            mergeOverlaysBelowImgFg(viewBinding.main, viewBinding.imgFg)?.let { bitmap ->
                viewModel.setBitmap(bitmap)
                NavManager.navigateToAdjust(parentFragmentManager)
            }
        }

        viewBinding.btnSticker.setOnClickListener {
            val bitmap = StickersAsset.loadBitmapFromAssets(context, StickersAsset.mListEmojy()[0])
            val viewSticker = QueShotStickerView(requireContext()).apply {
                addSticker(DrawableSticker(bitmap.toDrawable(resources)), 1)
                z = 12f
            }

            viewBinding.main.addView(viewSticker)
        }
    }



    private fun createListener() {

        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            Log.e("TAG", "pickMediaLauncher: $uri")
            handlePickMedia(uri)
        }

        pickMultipleMediaLauncher = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
            Log.e("TAG", "pickMediaLauncher: $uris")
            handlePickMultipleMedia(uris)
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

            override fun onDragIconBox(rotation: Float, scale: Float) {
                TODO("Not yet implemented")
            }

            //            override fun onDragIconBox(mIndex : Int, rotation: Float, scale: Float) {
            //                Log.e("frameOverlayViewNewListener", "onTouchOutBox: ")
            //
            //                frameContentList[mIndex]?.clipTransformImageView?.updateRotateAndScale(rotation, scale)
            //
            //                if (frameMeta?.frameList != null) {
            //                    val updatedFrameList = frameMeta!!.frameList.toMutableList()
            //                    updatedFrameList[mIndex] = updatedFrameList[mIndex].copy(rotation = rotation, scale = scale)
            //                    frameMeta = frameMeta?.copy(frameList = updatedFrameList)
            //                    viewBinding.imgCenter.setFrameInfos(frameMeta!!.frameList)
            //                }
            //                val clipTransformImageView = frameContentList[mIndex]?.clipTransformImageView
            //                clipTransformImageView?.updateRotateAndScale(rotation, scale)
            //            }
        }

        clipTransformImageView = object : ClipTransformImageViewListener {

            override fun translateAndScaleSuccess(frameInfo: FrameInfo, rotate: Float, scale: Float) {
                //                viewBinding.imgCenter.updateFrameInfo(rotation, scale)
                viewBinding.imgCenter.setFrameInfos(
                    viewBinding.imgCenter.getFrameInfos().map {
                        if (it.index == frameInfo.index) it.copy(rotation = rotate, scale = scale) else it
                    }
                )
            }
        }
    }

    fun handleSwapHighlight(indexImageA: Int, indexImageB: Int) {
        val frameContentA = frameContentList[indexImageA]
        val frameContentB = frameContentList[indexImageB]
        val clipTransformImageViewA = frameContentA?.clipTransformImageView ?: return
        val clipTransformImageViewB = frameContentB?.clipTransformImageView ?: return

        val imagePathA = frameContentA.path
        val imagePathB = frameContentB.path

        frameContentList[indexImageA] = frameContentA.copy(path = imagePathB)
        frameContentList[indexImageB] = frameContentB.copy(path = imagePathA)

        runCatching { clipTransformImageViewA.setImageBitmap(BitmapFactory.decodeFile(imagePathB)) }
        runCatching { clipTransformImageViewB.setImageBitmap(BitmapFactory.decodeFile(imagePathA)) }
    }

    private fun addFrameByFile(frameInfo: FrameInfo, file: File) {
        val index = frameInfo.index
        val bitmap = BitmapFactory.decodeFile(file.path)
        val clipView = ClipTransformImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            layoutParams = FrameLayout.LayoutParams(frameInfo.width.toInt(), frameInfo.height.toInt()).apply {
                leftMargin = frameInfo.x.toInt()
                topMargin = frameInfo.y.toInt()
            }
            z = (10f + index) / 10f
            rotation = frameInfo.rotation
            clipTransformImageView?.let { registerListener(transformListener = it) }
            setLimitRect(RectF(0f, 0f, frameInfo.width, frameInfo.height))
        }
        frameContentList[index] = (FrameContent(frameInfo, clipView, file.path))
        viewBinding.main.addView(clipView)
        Log.d("MainActivity", "Added ClipTransformImageView at index $index")
    }

    private fun loadMetaAndImage() {
        timeLoad = System.currentTimeMillis()
        frameMeta = FileUtils.readAllFramesMeta(requireContext())[2].also {
            Log.d("MainActivity", "Loaded meta: ${gson.toJson(it)}")
        }

        try {
            activity?.assets?.open(frameMeta!!.foregroundPath).use { inputStream ->
                viewBinding.imgFg.setImageBitmap(BitmapFactory.decodeStream(inputStream))
            }
            Log.d("MainActivity", "Foreground image loaded in ${System.currentTimeMillis() - timeLoad}ms")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load foreground", e)
            Toast.makeText(context, "Lỗi load ảnh foreground", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSelectFromGallery() {
        pickMediaLauncher?.launch("image/*")
    }

    private fun onSelectMultipleFromGallery() {
        pickMultipleMediaLauncher?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun handlePickMedia(uri: Uri?) {
        if (uri != null) {
            FileUtils.copyImageFileFromUri(requireContext(), uri)?.let { mFile ->
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

    private fun handlePickMultipleMedia(uris: List<Uri>?) {
        if (uris != null) {
            uris.forEach { mUri ->
                FileUtils.copyImageFileFromUri(requireContext(), mUri)?.let { mFile ->
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
        }
        isReplacing = false
    }

    private fun handleDeleteImage() {
        viewBinding.imgCenter.getFrameSelected()?.let { frameInfo ->
            deleteByIndex(frameInfo.index)
        }

    }

    private fun deleteByIndex(indexImage: Int) {
        val frameContent = try {
            frameContentList[indexImage]
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
        if (frameContent == null) return
        viewBinding.main.removeView(frameContent.clipTransformImageView)
        frameContentList.remove(frameContent.frameInfo.index)
        FileUtils.deleteFile(frameContent.path)
        viewBinding.imgCenter.updateHaveImage(haveImage = false, index = indexImage)
    }

    override fun onResume() {
        super.onResume()
        frameOverlayViewNewListener?.let { viewBinding.imgCenter.registerFrameListener(it) }
    }

    override fun onDestroy() {
        CoroutineScope(Dispatchers.IO).launch {
            context?.let { FileUtils.deleteImageTemp(context = it) }
        }
        frameOverlayViewNewListener = null
        super.onDestroy()
    }

}