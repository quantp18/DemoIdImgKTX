package com.example.demoidimgktx

import android.graphics.Rect


data class BoundingBox(
    val rect: Rect,
    val angle: Float = 0f // Góc xoay tính bằng độ
)