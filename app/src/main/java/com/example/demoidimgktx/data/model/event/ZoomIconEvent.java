package com.example.demoidimgktx.data.model.event;

import android.view.MotionEvent;

import com.example.demoidimgktx.custom_view.que_shot.QueShotStickerView;


public class ZoomIconEvent implements StickerIconEvent {
    public void onActionDown(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
    }

    public void onActionMove(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
        paramStickerView.zoomAndRotateCurrentSticker(paramMotionEvent);
    }

    public void onActionUp(QueShotStickerView paramStickerView, MotionEvent paramMotionEvent) {
        if (paramStickerView.getOnStickerOperationListener() != null)
            paramStickerView.getOnStickerOperationListener().onStickerZoom(paramStickerView.getCurrentSticker());
    }
}
