package com.example.demoidimgktx.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.demoidimgktx.custom_view.que_shot.QueShotEditor
import com.example.demoidimgktx.custom_view.que_shot.QueShotView
import com.example.demoidimgktx.listeners.OnQuShotEditorListener

class AdjustViewModel : ViewModel() {
    private var _editor: QueShotEditor? = null
    val editor : QueShotEditor? get() = _editor

    private var imageBitmap : Bitmap? = null
    val bitmap : Bitmap? get() = imageBitmap

    fun setBitmap(bitmap: Bitmap) {
        imageBitmap = bitmap
    }

    fun initEditor(context: Context, quShotView : QueShotView, onQuShotEditorListener: OnQuShotEditorListener) {
        _editor = QueShotEditor.Builder(context, quShotView).setPinchTextScalable(true).build()
        _editor?.setOnPhotoEditorListener(onQuShotEditorListener)
    }
}