package com.example.demoidimgktx.model

data class FrameInfo(
    val x: Float,
    val y: Float,
    val rotation : Float = 0F,
    val width: Float,
    val height: Float,
    val haveImage : Boolean = false,
    val index : Int = -1
)

data class FramesMeta(
    val width: Float,
    val height: Float,
    val foregroundPath : String,
    val frameList: List<FrameInfo>
)
