package com.mahfuznow.mediapipe.examples.facemesh

import android.content.Context
import android.os.*
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mediapipe.framework.TextureFrame
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val faceMesh: FaceMesh by lazy {
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(true)
                .build()
        )
    }

    // Live camera demo UI and camera components.
    private val cameraInput: CameraInput by lazy { CameraInput(this) }
    private val glSurfaceView: SolutionGlSurfaceView<FaceMeshResult> by lazy {
        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        SolutionGlSurfaceView(this, faceMesh.glContext, faceMesh.glMajorVersion)
    }

    //Sleep Detector
    private val sleepDetector: SleepDetector by lazy { SleepDetector() }

    //UI
    private val textView: TextView by lazy { findViewById(R.id.textView) }
    private val frameLayout: FrameLayout by lazy { findViewById(R.id.preview_display_layout) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //FaceMesh
        setUpFaceMesh()

        //Camera
        setUpCamera()

        //GL SurfaceView
        setUpGLSurfaceView()

        //FrameLayout
        attachGLSurfaceViewToFrameLayout()

        //Sleep Detector
        setUpSleepDetector()
    }

    override fun onResume() {
        super.onResume()
        setUpCamera()
        startCameraAfterGLSurfaceViewIsAttached()
    }

    override fun onPause() {
        super.onPause()
        cameraInput.close()
    }

    private fun setUpFaceMesh() {
        faceMesh.setResultListener { faceMeshResult: FaceMeshResult? ->
            sleepDetector.detectSleep(faceMeshResult)
            glSurfaceView.setRenderData(faceMeshResult)
            glSurfaceView.requestRender()
        }
        faceMesh.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(TAG, "MediaPipe Face Mesh error:$message")
        }
    }

    private fun setUpCamera() {
        cameraInput.setNewFrameListener { textureFrame: TextureFrame? ->
            faceMesh.send(textureFrame)
        }
    }

    private fun setUpGLSurfaceView() {
        glSurfaceView.setSolutionResultRenderer(FaceMeshResultGlRenderer())
        glSurfaceView.setRenderInputImage(true)
    }

    private fun attachGLSurfaceViewToFrameLayout() {
        // Updates the preview layout.
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)
        glSurfaceView.visibility = View.VISIBLE
        frameLayout.requestLayout()
    }

    private fun startCameraAfterGLSurfaceViewIsAttached() {
        // The runnable to start camera after the gl surface view is attached.
        glSurfaceView.post { startCamera() }
    }

    private fun startCamera() {
        cameraInput.start(
            this,
            faceMesh.glContext,
            CameraInput.CameraFacing.FRONT,
            glSurfaceView.width,
            glSurfaceView.height
        )
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