package com.example.frameobjectcontrol.tracking

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.tracking.TrackerCSRT
import org.opencv.tracking.TrackerCSRT_Params

class ObjectTracker {
    private var tracker: TrackerCSRT? = null

    fun createTracker() {
        val params = TrackerCSRT_Params()

        // Настроим параметры трекер
        params._use_hog = true
        params._use_color_names = true
        params._use_gray = false
        params._use_rgb = true
        params._use_channel_weights = true
        params._use_segmentation = true

        // Дополнительные параметры
        params._window_function = "hann"  // или "rectangular"
        params._kaiser_alpha = 1.0f
        params._cheb_attenuation = 0.1f
        params._template_size = 250.0f
        params._gsl_sigma = 1.0f
        params._hog_orientations = 9.0f
        params._hog_clip = 0.1f
        params._padding = 3.0f
        params._filter_lr = 0.02f
        params._weights_lr = 0.01f
        params._num_hog_channels_used = 3
        params._admm_iterations = 15
        params._histogram_bins = 20
        params._histogram_lr = 0.08f
        params._background_ratio = 3
        params._number_of_scales = 5
        params._scale_sigma_factor = 0.95f
        params._scale_model_max_area = 50000.0f
        params._scale_lr = 0.015f
        params._scale_step = 1.02f
        params._psr_threshold = 0.04f
        tracker = TrackerCSRT.create(params)
    }

    fun initTracker(rect: Rect, bitmap: Bitmap) {
        val mat = bitmapToMat(bitmap)
        tracker?.init(mat, rect)

    }

    fun updateTracker(bitmap: Bitmap): Rect? {
        val mat = bitmapToMat(bitmap)
        val updatedRoi = Rect()
        val success = tracker?.update(mat, updatedRoi) ?: false
        return if (success) updatedRoi else null
    }

    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
        return mat
    }

    fun isInit(): Boolean {
        return tracker != null
    }
}