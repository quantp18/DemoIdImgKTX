package com.example.demoidimgktx.custom_view;

import com.example.demoidimgktx.custom_view.draw.BrushDrawingView;

interface BrushViewChangeListener {
    void onStartDrawing();

    void onStopDrawing();

    void onViewAdd(BrushDrawingView brushDrawingView);

    void onViewRemoved(BrushDrawingView brushDrawingView);
}
