package com.mahfuznow.mediapipe.examples.facemesh

import android.util.Log
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import kotlin.math.pow
import kotlin.math.sqrt

class SleepDetector {

    companion object {
        private const val TAG = "SleepDetector"

        private const val LEFT_EYE_LEFT = 33
        private const val LEFT_EYE_TOP = 159
        private const val LEFT_EYE_RIGHT = 133
        private const val LEFT_EYE_BOTTOM = 145

        private const val RIGHT_EYE_LEFT = 362
        private const val RIGHT_EYE_TOP = 386
        private const val RIGHT_EYE_RIGHT = 263
        private const val RIGHT_EYE_BOTTOM = 374

        private const val MIN_RATIO = 0.15 //vertical = 15, horizontal = 85

        private const val MAX_EYE_CLOSE_COUNT = 30
    }

    var eyeCloseCount = 0
    private lateinit var onSleepListener: OnSleepListener

    fun setOnSleepListener(onSleepListener: OnSleepListener) {
        this.onSleepListener = onSleepListener
    }

    fun detectSleep(result: FaceMeshResult?) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return
        }
        val landmarkList = result.multiFaceLandmarks()[0].landmarkList

        val isLeftEyeClosed = isEyeClosed(
            landmarkList[LEFT_EYE_LEFT],
            landmarkList[LEFT_EYE_TOP],
            landmarkList[LEFT_EYE_RIGHT],
            landmarkList[LEFT_EYE_BOTTOM],
        )
        val isRightEyeClosed = isEyeClosed(
            landmarkList[RIGHT_EYE_LEFT],
            landmarkList[RIGHT_EYE_TOP],
            landmarkList[RIGHT_EYE_RIGHT],
            landmarkList[RIGHT_EYE_BOTTOM],
        )
        val isBothEyeClosed = isLeftEyeClosed && isRightEyeClosed
        Log.d(TAG, "detectSleep: $isBothEyeClosed")

        if (isBothEyeClosed) {
            onSleepListener.onEyeClose()
            eyeCloseCount += 1
            if (eyeCloseCount >= MAX_EYE_CLOSE_COUNT) {
                onSleepListener.onSleep()
                eyeCloseCount = MAX_EYE_CLOSE_COUNT
            }
        } else {
            onSleepListener.onEyeOpen()
            eyeCloseCount = 0
        }
    }

    private fun isEyeClosed(
        left: LandmarkProto.NormalizedLandmark,
        top: LandmarkProto.NormalizedLandmark,
        right: LandmarkProto.NormalizedLandmark,
        bottom: LandmarkProto.NormalizedLandmark
    ): Boolean {
        val horizontalDistance = distance(
            pointA = Pair(left.x, left.y),
            pointB = Pair(right.x, right.y)
        )
        val verticalDistance = distance(
            pointA = Pair(top.x, top.y),
            pointB = Pair(bottom.x, bottom.y)
        )
        val ratio = verticalDistance / horizontalDistance
        Log.d(TAG, "isEyeClosed: v=$verticalDistance h=$horizontalDistance r= $ratio")
        return ratio < MIN_RATIO
    }

    private fun distance(
        pointA: Pair<Float, Float>,
        pointB: Pair<Float, Float>
    ): Float {
        val a = (pointA.first - pointB.first)
        val b = (pointA.second - pointB.second)
        return sqrt(a.pow(2) + b.pow(2))
    }
}