package com.example.blurit.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "EditViewModel_hong"
class EditViewModel: ViewModel() {
    private var detector: FaceDetector
    private lateinit var image: InputImage

    private val _originalCanvas = MutableLiveData<Bitmap>()
    val originalCanvas : LiveData<Bitmap>
        get() = _originalCanvas

    private val _blurCanvas = MutableLiveData<Bitmap>()
    val blurCanvas : LiveData<Bitmap>
        get() = _blurCanvas

    private val _thicknessCanvas = MutableLiveData<Bitmap>()
    val thicknessCanvas : LiveData<Bitmap>
        get() = _thicknessCanvas

    private lateinit var originalBitmapMetadata: Bitmap

    init {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    fun autoMosaic(blur: Int){
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    applyAutoMosaic(faces, blur)
                } else {
//                    activity.showToast(activity.getString(R.string.edit_no_face))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed: $e")
            }
    }

    fun applyAutoMosaic(faces: List<Face>, size: Int) {
//        saveCanvasState()
        val blurBitmap = _blurCanvas.value?.copy(Bitmap.Config.ARGB_8888, true) ?: return
        val canvas = Canvas(blurBitmap)

        faces.forEach { face ->
            val boundingBox = face.boundingBox
            val centerX = boundingBox.centerX().toFloat()
            val centerY = boundingBox.centerY().toFloat()
            val radius = boundingBox.width().coerceAtMost(boundingBox.height()) / 2f
            val blockSize = radius.toInt() / (13 - size)

            for (y in boundingBox.top until boundingBox.bottom step blockSize) {
                for (x in boundingBox.left until boundingBox.right step blockSize) {
                    val blockCenterX = x + blockSize / 2f
                    val blockCenterY = y + blockSize / 2f
                    val distanceFromCenter = sqrt(
                        (blockCenterX - centerX).toDouble().pow(2.0) +
                                (blockCenterY - centerY).toDouble().pow(2.0)
                    )

                    if (distanceFromCenter <= radius) {
                        val pixelColor = _originalCanvas.value?.getPixel(x, y) ?: continue
                        val paint = Paint().apply { color = pixelColor }
                        canvas.drawRect(
                            x.toFloat(),
                            y.toFloat(),
                            (x + blockSize).toFloat(),
                            (y + blockSize).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
        _blurCanvas.value = blurBitmap
    }

    fun initBitmap(original: Bitmap, imageViewWidth: Int){
        try {
            originalBitmapMetadata = original

            val aspectRatio = original.width.toFloat() / original.height
            val imageViewHeight = (imageViewWidth / aspectRatio).toInt()

            val originalBitmap = Bitmap.createScaledBitmap(original, imageViewWidth, imageViewHeight, true)
                .copy(Bitmap.Config.ARGB_8888, true)
            _originalCanvas.value = originalBitmap

            image = InputImage.fromBitmap(originalBitmap, 0)

            _blurCanvas.value = Bitmap.createBitmap(
                imageViewWidth,
                imageViewHeight,
                Bitmap.Config.ARGB_8888
            )
            _thicknessCanvas.value = Bitmap.createBitmap(
                imageViewWidth,
                imageViewHeight,
                Bitmap.Config.ARGB_8888
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}