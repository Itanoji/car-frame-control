package com.example.frameobjectcontrol.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.tracking.TrackerCSRT
import org.opencv.tracking.TrackerCSRT_Params
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.nio.ByteBuffer
import org.opencv.core.Rect as cvRect


class ObjectTracker(private val context: Context) {

    // Интерфейс для получения результатов отслеживания
    interface TrackingCallback {
        fun onObjectDetected(rect: Rect?, src: Bitmap?)
    }

    private var trackingCallback: TrackingCallback? = null
    private lateinit var objectDetector: ObjectDetector
    private var tracker: TrackerCSRT? = null
    private var detectedObjectRect: Rect? = null
    private var roi: cvRect? = null

    init {
        setupObjectDetector()
    }

    // Метод для настройки TensorFlow Lite ObjectDetector
    private fun setupObjectDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(1) // Ограничиваем количество объектов
            .setScoreThreshold(0.25f) // Порог уверенности
            .build()

        objectDetector = ObjectDetector.createFromFileAndOptions(
            context,
            "1.tflite", // Название модели в assets
            options
        )
    }

    // Метод для установки callback
    fun setTrackingCallback(callback: TrackingCallback) {
        trackingCallback = callback
    }

    // Основной метод для анализа изображения
    fun analyzeImage(bitmap: Bitmap) {

        // Если трекер не был инициализирован, запускаем детекцию
        if (tracker == null) {
            val detectedObjectRect = detectObject(bitmap)

            if (detectedObjectRect != null) {
                // Инициализация трекера с детекцией
                // Можно заменить на KCF: TrackerKCF.create()
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


                roi = cvRect(
                    detectedObjectRect.left.toDouble().toInt(),
                    detectedObjectRect.top.toDouble().toInt(),
                    detectedObjectRect.width().toDouble().toInt(),
                    detectedObjectRect.height().toDouble().toInt()
                )

                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
                tracker?.init(mat, roi)  // Инициализация трекера с найденным объектом
                this.detectedObjectRect = detectedObjectRect
            }
        } else {
            // Если трекер уже существует, обновляем его
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
            var success = true
            if (roi?.empty() == true) {
                success = false
            } else {
                success = tracker?.update(mat, roi) ?: false
            }

             if (success) {
                // Если трекинг успешен, обновляем rect
                trackingCallback?.onObjectDetected(roiToRect(roi!!), bitmap)
            } else {
                // Если трекинг неудачен, выполняем повторную детекцию
                val newDetectedObjectRect = detectObject(bitmap)
                if (newDetectedObjectRect != null) {
                    detectedObjectRect = newDetectedObjectRect
                    roi = getRoi(detectedObjectRect!!)
                    tracker?.init(mat, roi) // Инициализируем новый трекер
                    trackingCallback?.onObjectDetected(roiToRect(roi!!), bitmap)
                } else {
                    trackingCallback?.onObjectDetected(null, bitmap)
                }
            }
        }

    }

    // Метод для детекции объекта в изображении
    private fun detectObject(bitmap: Bitmap): Rect? {
        val tensorImage = TensorImage.fromBitmap(bitmap)

        // Выполняем инференс модели
        val results = objectDetector.detect(tensorImage)

        // Указываем целевой класс (например, "person" или "car")
        val targetClass = "car"
        // Фильтруем результаты по классу и находим объект с максимальной уверенностью
        val filteredResult = results
            .filter { it.categories.any { category -> category.label == targetClass } }
            .maxByOrNull { it.categories.firstOrNull { category -> category.label == targetClass }?.score ?: 0f }

        // Если найден объект, возвращаем его границы
        return filteredResult?.boundingBox?.let { boundingBox ->
            Rect(
                boundingBox.left.toInt(),
                boundingBox.top.toInt(),
                boundingBox.right.toInt(),
                boundingBox.bottom.toInt()
            )
        }
    }

    // Метод для преобразования ImageProxy в Bitmap
    private fun ImageProxy.toBitmap(): Bitmap? {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Метод для преобразования Bitmap в Mat (для OpenCV)
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        // Шаг 1: Преобразуем Bitmap в ByteBuffer
        val byteBuffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        byteBuffer.rewind()

        // Шаг 2: Создаем Mat из ByteBuffer
        val matOfByte = MatOfByte(*byteBuffer.array())
        val mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)

        return mat
    }



    private fun getRoi(rect: Rect): cvRect {
        return cvRect(
            rect.left.toDouble().toInt(),
            rect.top.toDouble().toInt(),
            rect.width().toDouble().toInt(),
            rect.height().toDouble().toInt()
        )
    }

    private fun roiToRect(roi: cvRect): Rect {
        return Rect(
            roi.x,
            roi.y,
            roi.width + roi.x,
            roi.height + roi.y
        )
    }
}
