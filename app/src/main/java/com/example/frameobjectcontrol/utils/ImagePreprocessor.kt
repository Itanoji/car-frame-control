package com.example.frameobjectcontrol.utils

import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class ImagePreprocessor {

    private val inpMean = 127.5f
    private val inpStd = 127.5f

    /**
     * Подготавливает изображение для модели TensorFlow Lite.
     * Преобразует Bitmap в TensorImage с нормализованными значениями [-1, 1].
     */
    fun preprocessImage(bitmap: Bitmap): TensorImage {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true) // Ресайз
        val tensorImage = TensorImage.fromBitmap(resizedBitmap)
        val normalizedTensorBuffer = normalizeImage(tensorImage.tensorBuffer)
        tensorImage.load(normalizedTensorBuffer)
        return tensorImage
    }

    /**
     * Нормализует изображение: переводит значения пикселей из диапазона [0, 255] в [-1, 1].
     */
    private fun normalizeImage(tensorBuffer: TensorBuffer): TensorBuffer {
        val data = tensorBuffer.floatArray
        for (i in data.indices) {
            data[i] = (data[i] - inpMean) / inpStd // Нормализация [-1, 1]
        }

        val normalizedBuffer = TensorBuffer.createFixedSize(
            tensorBuffer.shape,
            tensorBuffer.dataType
        )
        normalizedBuffer.loadArray(data)
        return normalizedBuffer
    }
}
