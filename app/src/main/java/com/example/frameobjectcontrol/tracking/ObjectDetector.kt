package com.example.frameobjectcontrol.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetector(private val context: Context) {

    private lateinit var objectDetector: ObjectDetector

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.25f)
            .build()

        objectDetector = ObjectDetector.createFromFileAndOptions(
            context,
            "detect.tflite",
            options
        )
    }

    fun detectObject(bitmap: Bitmap): Rect? {
        val tensorImage = TensorImage.fromBitmap(bitmap)

        val targetClass = "car"

        // Выполняем детекцию
        val results = objectDetector.detect(tensorImage)

        // Фильтруем результаты по целевому классу
        val filteredResult = results
            .filter { it.categories.any { category -> category.label == targetClass } }
            .maxByOrNull {
                it.categories.firstOrNull { category -> category.label == targetClass }?.score ?: 0f
            }

        return filteredResult?.boundingBox?.let { boundingBox ->
            Rect(
                boundingBox.left.toInt(),
                boundingBox.top.toInt(),
                boundingBox.right.toInt(),
                boundingBox.bottom.toInt()
            )
        }
    }
}