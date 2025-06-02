package com.example.demoidimgktx.ui.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.internal.view.SupportMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.demoidimgktx.Module
import com.example.demoidimgktx.R
import com.example.demoidimgktx.custom_view.draw.Drawing
import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerIcons
import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView.OnStickerOperationListener
import com.example.demoidimgktx.custom_view.que_shot.QueShotTextView
import com.example.demoidimgktx.custom_view.sticker.Sticker
import com.example.demoidimgktx.data.model.event.AlignHorizontallyEvent
import com.example.demoidimgktx.data.model.event.DeleteIconEvent
import com.example.demoidimgktx.data.model.event.EditTextIconEvent
import com.example.demoidimgktx.data.model.event.FlipHorizontallyEvent
import com.example.demoidimgktx.data.model.event.ZoomIconEvent
import com.example.demoidimgktx.databinding.FragmentAdjustBinding
import com.example.demoidimgktx.listeners.AdjustListener
import com.example.demoidimgktx.listeners.OnQuShotEditorListener
import com.example.demoidimgktx.ui.adapter.AdjustAdapter
import com.example.demoidimgktx.ui.adapter.QueShotToolsAdapter
import com.example.demoidimgktx.ui.adapter.QueShotToolsAdapter.OnQuShotItemSelected
import com.example.demoidimgktx.ui.viewmodel.AdjustViewModel
import com.example.demoidimgktx.utils.preference.KeyboardHeightObserver
import com.example.demoidimgktx.utils.preference.KeyboardHeightProvider
import com.example.demoidimgktx.utils.preference.Preference

class AdjustFragment : Fragment(), OnQuShotEditorListener, AdjustListener, KeyboardHeightObserver, OnQuShotItemSelected {
    companion object{
        const val TAG = "AdjustFragment"
    }

    private lateinit var viewBinding : FragmentAdjustBinding
    private val mEditingToolsAdapter: QueShotToolsAdapter = QueShotToolsAdapter(this)
    private val viewModel: AdjustViewModel by activityViewModels()
    private val adjustAdapter : AdjustAdapter by lazy {
        AdjustAdapter(requireContext(), this)
    }

    // Keyboard
    private var keyboardProvider: KeyboardHeightProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentAdjustBinding.inflate(inflater)
        return viewBinding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initEditor(requireContext(), viewBinding.photoEditorView, this)
        Handler().post { keyboardProvider?.start() }
        keyboardProvider = KeyboardHeightProvider(requireActivity())
        viewBinding.imageViewCompareAdjust.setOnTouchListener { v, event ->
            when (event.action) {
                0 -> {
                    viewBinding.photoEditorView.glSurfaceView.setAlpha(0.0f)
                    return@setOnTouchListener true
                }

                1 -> {
                    viewBinding.photoEditorView.glSurfaceView.setAlpha(1.0f)
                    return@setOnTouchListener false
                }

                else -> return@setOnTouchListener true
            }
        }
        viewBinding.seekbarAdjust.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, i: Int, z: Boolean) {
                adjustAdapter.currentAdjustModel
                    .setSeekBarIntensity(viewModel.editor, (i.toFloat()) / (seekBar.max.toFloat()), true)
            }
        })

        viewModel.bitmap?.let {
            viewBinding.photoEditorView.setImageSource(it)
        }

        /*sda*/

       val quShotStickerIconClose = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_close), 0, QueShotStickerIcons.DELETE)
       quShotStickerIconClose.setIconEvent(DeleteIconEvent())
       val quShotStickerIconScale = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_scale), 3, QueShotStickerIcons.SCALE)
        quShotStickerIconScale.setIconEvent(ZoomIconEvent())
       val quShotStickerIconFlip = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_flip), 1, QueShotStickerIcons.FLIP)
       quShotStickerIconFlip.setIconEvent(FlipHorizontallyEvent())
       val quShotStickerIconRotate = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_rotate), 3, QueShotStickerIcons.ROTATE)
       quShotStickerIconRotate.setIconEvent(ZoomIconEvent())
       val quShotStickerIconEdit = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_edit), 1, QueShotStickerIcons.EDIT)
       quShotStickerIconEdit.setIconEvent(EditTextIconEvent())
       val quShotStickerIconAlign = QueShotStickerIcons(ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_center), 2, QueShotStickerIcons.ALIGN)
       quShotStickerIconAlign.setIconEvent(AlignHorizontallyEvent())

        viewBinding.recyclerViewTools.apply {
            setLayoutManager(LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false))
            setAdapter(mEditingToolsAdapter)
            setHasFixedSize(true)
        }
        viewBinding.photoEditorView.setIcons(
            listOf(
                quShotStickerIconClose, quShotStickerIconScale,
                quShotStickerIconFlip, quShotStickerIconEdit, quShotStickerIconRotate, quShotStickerIconAlign
            )
        )
        viewBinding.photoEditorView.setBackgroundColor(-16777216)
        viewBinding.photoEditorView.setLocked(false)
        viewBinding.photoEditorView.setConstrained(true)
        viewBinding.photoEditorView.setOnStickerOperationListener(object : OnStickerOperationListener {
            override fun onAddSticker(sticker: Sticker) {
                viewBinding.seekbarStickerAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerAlpha.setProgress(sticker.alpha)
                viewBinding.seekbarStickerMenAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerMenAlpha.setProgress(sticker.alpha)
                viewBinding.seekbarStickerWomenAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerWomenAlpha.setProgress(sticker.alpha)
            }

            @SuppressLint("RestrictedApi")
            override fun onStickerSelected(sticker: Sticker) {
                if (sticker is QueShotTextView) {
                    (sticker as QueShotTextView).setTextColor(SupportMenu.CATEGORY_MASK)
                    viewBinding.photoEditorView.replace(sticker)
                    viewBinding.photoEditorView.invalidate()
                }
                viewBinding.seekbarStickerAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerAlpha.setProgress(sticker.alpha)
                viewBinding.seekbarStickerMenAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerMenAlpha.setProgress(sticker.alpha)
                viewBinding.seekbarStickerWomenAlpha.setVisibility(View.VISIBLE)
                viewBinding.seekbarStickerWomenAlpha.setProgress(sticker.alpha)
            }

            override fun onStickerDeleted(sticker: Sticker) {
                viewBinding.seekbarStickerAlpha.setVisibility(View.GONE)
                viewBinding.seekbarStickerMenAlpha.setVisibility(View.GONE)
                viewBinding.seekbarStickerWomenAlpha.setVisibility(View.GONE)
            }

            override fun onStickerDoubleTap(sticker: Sticker) {
                
            }

            override fun onStickerDrag(sticker: Sticker) {
                
            }

            override fun onStickerFlip(sticker: Sticker) {
                
            }

            override fun onStickerTouchOutside() {
                viewBinding.seekbarStickerAlpha.setVisibility(View.GONE)
                viewBinding.seekbarStickerMenAlpha.setVisibility(View.GONE)
                viewBinding.seekbarStickerWomenAlpha.setVisibility(View.GONE)
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                
            }

            override fun onStickerZoom(sticker: Sticker) {
                
            }

            override fun onTouchDownBeauty(f: Float, f2: Float) {
                
            }

            override fun onTouchDragBeauty(f: Float, f2: Float) {
                
            }

            override fun onTouchUpBeauty(f: Float, f2: Float) {
                
            }

        })

    }

    override fun onAddViewListener(viewType: Drawing?, i: Int) {
        Log.d(TAG, "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$i]")
    }

    override fun onRemoveViewListener(i: Int) {
        Log.d(TAG, "onRemoveViewListener() called with: numberOfAddedViews = [$i]")

    }

    override fun onRemoveViewListener(viewType: Drawing?, i: Int) {
        
    }

    override fun onStartViewChangeListener(viewType: Drawing?) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onStopViewChangeListener(viewType: Drawing?) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
    }

    override fun onAdjustSelected(adjustModel: AdjustAdapter.AdjustModel?) {
        Log.d("XXXXXXXX", "onAdjustSelected " + adjustModel?.seekbarIntensity + " " + viewBinding.seekbarAdjust.max)
        viewBinding.seekbarAdjust.progress = ((adjustModel?.seekbarIntensity ?: (0.5f * viewBinding.seekbarAdjust.max))).toInt()

    }


    override fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        Preference.setKeyboard(requireContext(), 0)
        val orientationLabel = if (orientation == Configuration.ORIENTATION_PORTRAIT) "portrait" else "landscape"
        Log.i(TAG, "onKeyboardHeightChanged in pixels: $height $orientationLabel")
        if (height <= 0) {
            Preference.setHeightOfNotch(requireContext(), -height)
        }
//        else if (addTextFragment != null) {
//            addTextFragment.updateAddTextBottomToolbarHeight(Preference.getHeightOfNotch(requireContext()) + height)
//            Preference.setKeyboard(getApplicationContext(), height + Preference.getHeightOfNotch(requireContext()))
//        }
    }


    override fun onDestroy() {
        super.onDestroy()
        keyboardProvider?.close()
    }

    override fun onPause() {
        super.onPause()
        keyboardProvider?.setKeyboardHeightObserver(null)
    }

    override fun onResume() {
        super.onResume()
        keyboardProvider?.setKeyboardHeightObserver(this)
    }

    override fun onQuShotToolSelected(module: Module?) {
        when (module) {

            Module.ADJUST -> {
                setGoneSave()
                viewBinding.imageViewCompareAdjust.setVisibility(View.VISIBLE)
                viewBinding.constraintLayoutDraw.setVisibility(View.GONE)
                viewBinding.constraintLayoutEmoji.setVisibility(View.GONE)
                viewBinding.constraintLayoutAdjust.setVisibility(View.VISIBLE)
                viewBinding.constraintLayoutNeon.setVisibility(View.GONE)
                viewBinding.constraintLayoutFilter.setVisibility(View.GONE)
                viewBinding.constraintLayoutPaint.setVisibility(View.GONE)
                if (!viewBinding.photoEditorView.getStickers().isEmpty()) {
                    viewBinding.photoEditorView.getStickers().clear()
                    viewBinding.photoEditorView.setHandlingSticker(null)
                }

                val constraintsetAdjust = ConstraintSet()
                constraintsetAdjust.clone(viewBinding.constraintLayoutRootView)
                constraintsetAdjust.connect(viewBinding.relativeLayoutWrapperPhoto.getId(), 1, viewBinding.constraintLayoutRootView.getId(), 1, 0)
                constraintsetAdjust.connect(viewBinding.relativeLayoutWrapperPhoto.getId(), 4, viewBinding.guidelinePaint.getId(), 3, 0)
                constraintsetAdjust.connect(this.viewBinding.relativeLayoutWrapperPhoto.getId(), 2, viewBinding.constraintLayoutRootView.getId(), 2, 0)
                constraintsetAdjust.applyTo(viewBinding.constraintLayoutRootView)
                viewBinding.recyclerViewTools.setVisibility(View.GONE)
                viewBinding.constraintLayoutSave.setVisibility(View.GONE)
                viewBinding.relativeLayoutAddText.setVisibility(View.GONE)
//                viewBinding.constraintLayoutAddText.setVisibility(View.GONE)
                viewModel.editor?.clearBrushAllViews()
                viewModel.editor?.setBrushDrawingMode(false)
                viewBinding.recyclerViewAdjust.setAdapter(this.adjustAdapter)
                adjustAdapter.setSelectedAdjust(0)
                viewModel.editor?.setAdjustFilter(adjustAdapter.filterConfig)
            }


            else -> {}
        }
        viewBinding.photoEditorView.setHandlingSticker(null)
    }


    fun setGoneSave() {
        viewBinding.constraintLayoutSaveEditing.setVisibility(View.GONE)
    }
}