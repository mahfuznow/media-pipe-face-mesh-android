package com.mahfuznow.mediapipe.examples.facemesh

import android.content.Context
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    //UI
    private val textView: TextView by lazy { findViewById(R.id.textView) }
    private val previewView: PreviewView by lazy { findViewById(R.id.preview_view) }

    //CameraX
    private val imageAnalyzer: ImageAnalysis by lazy {
        // ImageAnalysis. Using RGBA 8888 to match how our models work
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
    }
    private val preview: Preview by lazy {
        Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> by lazy {
        ProcessCameraProvider.getInstance(this)
    }
    private val cameraSelector: CameraSelector by lazy { CameraSelector.DEFAULT_FRONT_CAMERA }
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private lateinit var bitmapBuffer: Bitmap

    //Face Mesh
    private val faceMesh: FaceMesh by lazy {
        FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(true)
                .setRefineLandmarks(true)
                .setRunOnGpu(true)
                .build()
        )
    }

    //Sleep Detector
    private val sleepDetector: SleepDetector by lazy { SleepDetector() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Camera
        setUpCamera()

        //FaceMesh
        setUpFaceMesh()

        //Sleep Detector
        setUpSleepDetector()
    }

    private fun setUpCamera() {
        imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
            if (!::bitmapBuffer.isInitialized) {
                // The RGB image buffer are initialized only once the analyzer has started running
                bitmapBuffer = Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            }
            // Copy out RGB bits to the shared bitmap buffer
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            //faceMesh.send(bitmapBuffer)
            faceMesh.send(bitmapBuffer, System.currentTimeMillis())
        }

        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                } catch (e: Exception) {
                    Log.d(TAG, "Use case binding failed")
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun setUpFaceMesh() {
        faceMesh.setResultListener { faceMeshResult: FaceMeshResult? ->
            sleepDetector.detectSleep(faceMeshResult)
        }
        faceMesh.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(TAG, "MediaPipe Face Mesh error:$message")
        }
    }

    private fun setUpSleepDetector() {
        sleepDetector.setOnSleepListener(
            object : OnSleepListener {
                override fun onEyeOpen() {
                    runOnUiThread { textView.text = getString(R.string.eye_status_open) }
                }

                override fun onEyeClose() {
                    runOnUiThread { textView.text = getString(R.string.eye_status_closed) }
                }

                override fun onSleep() {
                    runOnUiThread {
                        textView.text = getString(R.string.eye_status_sleeping)
                        vibrate()
                    }
                }

            }
        )
    }

    private fun vibrate() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(300)
        }
    }
}