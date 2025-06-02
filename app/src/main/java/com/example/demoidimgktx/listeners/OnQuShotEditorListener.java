package com.example.demoidimgktx.listeners;

import com.example.demoidimgktx.custom_view.draw.Drawing;

public interface OnQuShotEditorListener {
    void onAddViewListener(Drawing viewType, int i);


    void onRemoveViewListener(int i);

    void onRemoveViewListener(Drawing viewType, int i);

    void onStartViewChangeListener(Drawing viewType);

    void onStopViewChangeListener(Drawing viewType);
}
