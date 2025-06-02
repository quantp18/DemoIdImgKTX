package com.example.demoidimgktx.data.model.event;

import android.view.MotionEvent;

import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView;

public interface StickerIconEvent {
    void onActionDown(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent);

    void onActionMove(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent);

    void onActionUp(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent);
}
