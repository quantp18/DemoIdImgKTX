package com.example.demoidimgktx.data.model.event;

import android.view.MotionEvent;

import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView;

public abstract class AbstractFlipEvent implements StickerIconEvent {
    protected abstract int getFlipDirection();

    public void onActionDown(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
    }

    public void onActionMove(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
    }

    public void onActionUp(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
        paramStickerView.flipCurrentSticker(getFlipDirection());
    }
}
