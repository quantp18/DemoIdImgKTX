package com.example.demoidimgktx.data.model.event;

import android.view.MotionEvent;

import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView;


public class EditTextIconEvent implements StickerIconEvent {
    public void onActionDown(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
    }

    public void onActionMove(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
    }

    public void onActionUp(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
        paramStickerView.editTextSticker();
    }
}
