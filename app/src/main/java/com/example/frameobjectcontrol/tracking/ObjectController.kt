package com.example.frameobjectcontrol.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.Rect as cvRect


class ObjectController(private val context: Context) {
    interface TrackingCallback {
        fun onObjectDetected(rect: Rect?, src: Bitmap?)
    }

    private val detector = ObjectDetector(context)
    private val tracker = ObjectTracker()
    private var trackingCallback: TrackingCallback? = null
    private var roi: cvRect? = null

    init {
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
    }

    fun setTrackingCallback(callback: TrackingCallback) {
        trackingCallback = callback
    }

    fun analyzeImage(bitmap: Bitmap) {
        if(!tracker.isInit()) {
            // трэкер не был инициализирован
            val detectedObject = detector.detectObject(bitmap)
            if (detectedObject != null) {
                // Инициализируем трекер
                roi = cvRect(
                    detectedObject.left,
                    detectedObject.top,
                    detectedObject.width(),
                    detectedObject.height()
                )
                tracker.createTracker()
                tracker.initTracker(roi!!, bitmap)
                trackingCallback?.onObjectDetected(detectedObject, bitmap)
            } else {
                trackingCallback?.onObjectDetected(null, bitmap)
            }
        } else {
            var success = true
            if (roi?.empty() == false) {
                roi = tracker.updateTracker(bitmap)
            }
            success = roi != null
            if (!success) {
                // Если трекинг неудачен, выполняем детекцию
                val detectedObject = detector.detectObject(bitmap)
                if (detectedObject != null) {
                    // Инициализируем трекер
                    roi = cvRect(
                        detectedObject.left,
                        detectedObject.top,
                        detectedObject.width(),
                        detectedObject.height()
                    )
                    tracker.initTracker(roi!!, bitmap)
                    trackingCallback?.onObjectDetected(detectedObject, bitmap)
                } else {
                    trackingCallback?.onObjectDetected(null, bitmap)
                }
            } else {
                // Если трекинг успешен
                roi?.let {
                    trackingCallback?.onObjectDetected(roiToRect(roi!!), bitmap)
                } ?: run {
                    trackingCallback?.onObjectDetected(null, bitmap)
                }
            }
        }
    }

    private fun roiToRect(roi: cvRect): Rect {
        return Rect(
            roi.x,
            roi.y,
            roi.x + roi.width,
            roi.y + roi.height
        )
    }
}
