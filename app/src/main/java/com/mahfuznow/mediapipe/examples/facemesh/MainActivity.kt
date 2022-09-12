package com.mahfuznow.mediapipe.examples.facemesh

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
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

    private lateinit var faceMesh: FaceMesh

    // Live camera demo UI and camera components.
    private lateinit var cameraInput: CameraInput
    private lateinit var glSurfaceView: SolutionGlSurfaceView<FaceMeshResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // TODO: Add a toggle to switch between the original face mesh and attention mesh.
        initialize()
    }

    private fun initialize() {
        //FaceMesh
        setUpFaceMesh()

        //Camera
        setUpCamera()

        //GL SurfaceView
        setUpGLSurfaceView()

        //FrameLayout
        attachGLSurfaceViewToFrameLayout()
    }

    private fun setUpFaceMesh() {
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        faceMesh = FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(true)
                .build()
        )
        faceMesh.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(TAG, "MediaPipe Face Mesh error:$message")
        }
    }

    private fun setUpCamera() {
        cameraInput = CameraInput(this)
        cameraInput.setNewFrameListener { textureFrame: TextureFrame? ->
            faceMesh.send(textureFrame)
        }
    }

    private fun setUpGLSurfaceView() {
        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        glSurfaceView = SolutionGlSurfaceView(this, faceMesh.glContext, faceMesh.glMajorVersion)
        glSurfaceView.setSolutionResultRenderer(FaceMeshResultGlRenderer())
        glSurfaceView.setRenderInputImage(true)
        faceMesh.setResultListener { faceMeshResult: FaceMeshResult? ->
            logNoseLandmark(faceMeshResult,  /*showPixelValues=*/false)
            glSurfaceView.setRenderData(faceMeshResult)
            glSurfaceView.requestRender()
        }
        // The runnable to start camera after the gl surface view is attached.
        glSurfaceView.post { startCamera() }
    }

    private fun attachGLSurfaceViewToFrameLayout() {
        // Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)
        glSurfaceView.visibility = View.VISIBLE
        frameLayout.requestLayout()
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

    // TODO: Predict Eye Open or Close based on the eye landmark
    private fun logNoseLandmark(result: FaceMeshResult?, showPixelValues: Boolean) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return
        }
        val noseLandmark = result.multiFaceLandmarks()[0].landmarkList[1]
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            Log.i(
                TAG, String.format(
                    "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
                    noseLandmark.x * width, noseLandmark.y * height
                )
            )
        } else {
            Log.i(
                TAG, String.format(
                    "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    noseLandmark.x, noseLandmark.y
                )
            )
        }
    }
}