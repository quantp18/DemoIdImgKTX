package com.example.demoidimgktx.data.model.event;

public class FlipHorizontallyEvent extends AbstractFlipEvent {
    protected int getFlipDirection() {
        return 1;
    }
}
