package com.example.frameobjectcontrol.utils

import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.view.PreviewView

class ImagePostprocessor {
    /**
     * Преобразует bounding box из нормализованных координат модели в координаты экрана.
     *
     * @param boundingBox Нормализованный bounding box из модели [0, 1].
     * @param screenWidth Ширина экрана.
     * @param screenHeight Высота экрана.
     * @param inputWidth Ширина входного изображения модели.
     * @param inputHeight Высота входного изображения модели.
     *
     * @return Преобразованный Rect в координатах экрана.
     */
    fun mapBoundingBoxToScreen(
        boundingBox: RectF,
        screenWidth: Int,
        screenHeight: Int,
        inputWidth: Int,
        inputHeight: Int
    ): Rect {
        // Коэффициенты масштабирования
        val scaleX = screenWidth.toFloat() / inputWidth
        val scaleY = screenHeight.toFloat() / inputHeight

        // Учитываем минимальный коэффициент для сохранения пропорций
        val scale = minOf(scaleX, scaleY)
        val offsetX = (screenWidth - inputWidth * scale) / 2
        val offsetY = (screenHeight - inputHeight * scale) / 2

        // Преобразуем bounding box
        val left = (boundingBox.left * inputWidth * scale + offsetX).toInt()
        val top = (boundingBox.top * inputHeight * scale + offsetY).toInt()
        val right = (boundingBox.right * inputWidth * scale + offsetX).toInt()
        val bottom = (boundingBox.bottom * inputHeight * scale + offsetY).toInt()

        return Rect(left, top, right, bottom)
    }
}