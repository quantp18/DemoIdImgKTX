package com.example.demoidimgktx.model

data class FrameInfo(
    val x: Int,
    val y: Int,
    val rotation : Float = 0F,
    val width: Int,
    val height: Int
)

data class FramesMeta(
    val width: Int,
    val height: Int,
    val foregroundPath : String,
    val frameList: List<FrameInfo>
)
