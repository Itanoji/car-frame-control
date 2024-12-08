package com.example.frameobjectcontrol

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.frameobjectcontrol.tracking.ObjectTracker
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.view.View
import android.widget.TextView
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import org.opencv.android.OpenCVLoader

//import androidx.core.content.ContextCompat
//import com.example.frameobjectcontrol.tracking.ObjectTracker
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ObjectTracker.TrackingCallback {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraPreview: PreviewView
    private lateinit var btnStartTracking: Button
    private lateinit var objectTracker: ObjectTracker
    private var isTracking = false
    private lateinit var overlayView: OverlayView
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!OpenCVLoader.initDebug())
            Log.e("OpenCV", "Unable to load OpenCV!");
        else
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация OverlayView
        overlayView = findViewById(R.id.overlayView)

        // Инициализация UI
        cameraPreview = findViewById(R.id.cameraPreview)
        btnStartTracking = findViewById(R.id.btnStartTracking)
        tvStatus = findViewById(R.id.tvStatus)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Инициализация ObjectTracker
        objectTracker = ObjectTracker(this)
        objectTracker.setTrackingCallback(this)

        // Проверка разрешений и запуск камеры
        if (isPermissionGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        // Настройка кнопки отслеживания
        btnStartTracking.setOnClickListener {
            isTracking = !isTracking
            btnStartTracking.text = if (isTracking) "Stop Tracking" else "Start Tracking"
        }
    }

    // Проверка наличия разрешений
    private fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Запрос разрешений, если их нет
    private fun requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
    }

    // Обработка ответа на запрос разрешений
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Log.e("MainActivity", "Camera permission not granted.")
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Настройка Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = cameraPreview.surfaceProvider
                }

            // Настройка ImageAnalysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (isTracking) {
                            objectTracker.analyzeImage(image.toBitmapWithCorrectOrientation())
                            image.close()
                        } else {
                            overlayView.setBoundingBox(null)
                            tvStatus.visibility = View.GONE
                            image.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Отключаем старую привязку камеры
                cameraProvider.unbindAll()

                // Привязываем новую
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error binding camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Callback для отслеживания объекта
    override fun onObjectDetected(rect: Rect?, src: Bitmap?) {
        runOnUiThread {
            if (rect == null || src == null) {
                overlayView.setBoundingBox(null) // Убираем прямоугольник, если объект не найден
                // Если объект не найден и отслеживание включено
                if (isTracking) {
                    tvStatus.text = "Объект осмотра вне кадра"
                    tvStatus.visibility = View.VISIBLE
                }
            } else {
                overlayView.setBoundingBox(rect) // Отрисовываем найденный объект
                // Скрываем сообщение, если объект найден
                tvStatus.visibility = View.GONE
                val imageWidth = src.width
                val imageHeight = src.height

                // Получаем размеры экрана или `PreviewView`
                val viewWidth = overlayView.width
                val viewHeight = overlayView.height

                // Преобразуем bounding box
                val scaledRect = mapBoundingBoxToScreen(
                    boundingBox = rect,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight
                )

                overlayView.setBoundingBox(scaledRect) // Отрисовываем найденный объект
            }
        }
    }

    fun mapBoundingBoxToScreen(
        boundingBox: Rect,
        imageWidth: Int,  // Ширина изображения, полученного от камеры
        imageHeight: Int, // Высота изображения, полученного от камеры
        viewWidth: Int,   // Ширина `PreviewView` или экрана
        viewHeight: Int   // Высота `PreviewView` или экрана
    ): Rect {
        // Определяем коэффициенты масштабирования
        val scaleX = viewWidth.toFloat() / imageWidth
        val scaleY = viewHeight.toFloat() / imageHeight

        // Масштабируем bounding box
        return Rect(
            (boundingBox.left * scaleX).toInt(),
            (boundingBox.top * scaleY).toInt(),
            (boundingBox.right * scaleX).toInt(),
            (boundingBox.bottom * scaleY).toInt()
        )
    }


    /**
     * Конвертирует ImageProxy в Bitmap с учетом ориентации устройства.
     */
    fun ImageProxy.toBitmapWithCorrectOrientation(): Bitmap {
        val bitmap = this.toBitmap() // Используем встроенный метод CameraX

        // Учитываем ориентацию устройства
        val rotationDegrees = this.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

