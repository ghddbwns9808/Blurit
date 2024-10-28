package com.example.blurit.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import java.util.Stack
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

    private val _toastMsg = MutableLiveData<String>()
    val toastMsg : LiveData<String>
        get() = _toastMsg

    private lateinit var originalBitmapMetadata: Bitmap

    private val undoStack: Stack<Bitmap> = Stack()

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
                    _toastMsg.value = NO_FACE_MSG
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed: $e")
            }
    }

    fun applyAutoMosaic(faces: List<Face>, size: Int) {
        saveCanvasState()
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

    fun applyManualMosaic(imageView: ImageView, touchX: Int, touchY: Int, thick: Int, blur: Int){
        val blurBitmap = _blurCanvas.value?.copy(Bitmap.Config.ARGB_8888, true) ?: return
        val canvas = Canvas(blurBitmap)
        val (bitmapX, bitmapY) = convertTouchToBitmap(imageView, touchX, touchY)

        val thickness = (thick + (_thicknessCanvas.value?.width ?: 1000) / 30)
        val blurSize = thickness / (13 - blur)

        for (y in bitmapY - thickness until bitmapY + thickness step blurSize) {
            for (x in bitmapX - thickness until bitmapX + thickness step blurSize) {
                val distanceFromCenter =
                    sqrt((x - bitmapX).toDouble().pow(2.0) + (y - bitmapY).toDouble().pow(2.0))

                if (distanceFromCenter <= thickness) {
                    val pixelColor = originalCanvas.value!!.getPixel(
                        x.coerceIn(0, originalCanvas.value!!.width - 1),
                        y.coerceIn(0, originalCanvas.value!!.height - 1)
                    )

                    val paint = Paint().apply { color = pixelColor }
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + blurSize).toFloat(),
                        (y + blurSize).toFloat(),
                        paint
                    )
                }
            }
        }

        _blurCanvas.value = blurBitmap
    }

    fun undoLastAction() {
        if (undoStack.isNotEmpty()) {
            _blurCanvas.value = undoStack.pop()
        } else {
            _toastMsg.value = EMPTY_STACK
        }
    }

    fun showBrushPreview(thickness: Int, width: Int) {
        val rad = thickness + width / 30

        val thickBitmap = Bitmap.createBitmap(_thicknessCanvas.value!!.width, _thicknessCanvas.value!!.height, _thicknessCanvas.value!!.config)
        val canvas = Canvas(thickBitmap)

        val paint = Paint().apply {
            color = Color.argb(160, 100, 100, 100)
            style = Paint.Style.FILL
        }

        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f

        canvas.drawCircle(centerX, centerY, rad.toFloat(), paint)

        _thicknessCanvas.value = thickBitmap
    }


    fun eraseThickCircle() {
        Handler(Looper.getMainLooper()).postDelayed({
            _thicknessCanvas.value?.let { bitmap ->
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                _thicknessCanvas.value = bitmap
            }
        }, 1200)
    }


    fun saveCanvasState() {
        val currentState = blurCanvas.value!!.copy(blurCanvas.value!!.config, true)
        undoStack.push(currentState)
    }

    private fun convertTouchToBitmap(imageView: ImageView, touchX: Int, touchY: Int): Pair<Int, Int> {
        val drawable = imageView.drawable ?: return Pair(0, 0)

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight

        val scaleX = imageView.width.toFloat() / intrinsicWidth
        val scaleY = imageView.height.toFloat() / intrinsicHeight
        val scale = minOf(scaleX, scaleY)

        val offsetX = (imageView.width - intrinsicWidth * scale) / 2
        val offsetY = (imageView.height - intrinsicHeight * scale) / 2

        val bitmapX = ((touchX - offsetX) / scale).toInt().coerceIn(0, intrinsicWidth - 1)
        val bitmapY = ((touchY - offsetY) / scale).toInt().coerceIn(0, intrinsicHeight - 1)

        return Pair(bitmapX, bitmapY)
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

    companion object{
        const val NO_FACE_MSG = "인식된 얼굴이 없습니다."
        const val EMPTY_STACK = "취소할 작업이 없습니다."
    }

}