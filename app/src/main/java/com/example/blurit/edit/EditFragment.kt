package com.example.blurit.edit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.blurit.MainActivity
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentEditBinding
import com.google.android.material.slider.Slider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "EditFragment_hong"

class EditFragment : BaseFragment<FragmentEditBinding>(
    FragmentEditBinding::bind, R.layout.fragment_edit
) {
    private lateinit var activity: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()

    private val highAccuracyOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private lateinit var image: InputImage
    private lateinit var detector: FaceDetector

    private lateinit var originalBitmap: Bitmap
    private lateinit var blurCanvas: Bitmap
    private lateinit var thickCanvas: Bitmap

    private var mode: EditMode = EditMode.AUTO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = _activity as MainActivity
        initView()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    private fun applyAutoMosaic(faces: List<Face>, size: Int) {
        val canvas = Canvas(blurCanvas)

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
                        val pixelColor = originalBitmap.getPixel(x, y)

                        val paint = Paint()
                        paint.color = pixelColor

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

        binding.ivBlurCanvas.setImageBitmap(blurCanvas)
    }

    private fun applyManualMosaic(touchX: Int, touchY: Int) {
        val canvas = Canvas(blurCanvas)
        val (bitmapX, bitmapY) = convertTouchToBitmap(touchX, touchY)

        val thickness = (binding.sdThick.value + binding.ivThickCanvas.width / 30).toInt()
        val blurSize = thickness / (13 - binding.sdBlur.value.toInt())

        for (y in bitmapY - thickness until bitmapY + thickness step blurSize) {
            for (x in bitmapX - thickness until bitmapX + thickness step blurSize) {
                val distanceFromCenter = sqrt((x - bitmapX).toDouble().pow(2.0) + (y - bitmapY).toDouble().pow(2.0))

                if (distanceFromCenter <= thickness) {
                    val pixelColor = originalBitmap.getPixel(
                        x.coerceIn(0, originalBitmap.width - 1),
                        y.coerceIn(0, originalBitmap.height - 1)
                    )

                    val paint = Paint().apply { color = pixelColor }
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + blurSize).toFloat(), (y + blurSize).toFloat(), paint)
                }
            }
        }

        binding.ivBlurCanvas.setImageBitmap(blurCanvas)
    }


    private fun convertTouchToBitmap(touchX: Int, touchY: Int): Pair<Int, Int> {
        val imageView = binding.ivPhoto
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



    private fun showBrushPreview(thickness: Int) {
        val rad = thickness + binding.ivThickCanvas.width / 30

        val thickBitmap = thickCanvas.copy(thickCanvas.config, true)
        val canvas = Canvas(thickBitmap)
        val paint = Paint().apply {
            color = Color.argb(160, 100, 100, 100)
            style = Paint.Style.FILL
        }

        val centerX = canvas.width / 2f
        val centerY = canvas.height / 2f

        canvas.drawCircle(centerX, centerY, rad.toFloat(), paint)
        binding.ivThickCanvas.setImageBitmap(thickBitmap)
    }

    private fun eraseThickCircle() {
        Handler(Looper.getMainLooper()).postDelayed({
            val emptyBitmap = thickCanvas.copy(thickCanvas.config, true)
            binding.ivThickCanvas.setImageBitmap(emptyBitmap)
        }, 1500)
    }

    private fun setSliderEnabledState(slider: Slider, enabled: Boolean) {
        if (enabled) {
            slider.isEnabled = true
            slider.trackActiveTintList = ContextCompat.getColorStateList(_activity, R.color.blurit_pink_dark)!!
            slider.thumbTintList = ContextCompat.getColorStateList(_activity, R.color.blurit_pink_dark)!!
        } else {
            slider.isEnabled = false
            slider.trackActiveTintList = ContextCompat.getColorStateList(_activity, R.color.slider_inactive_gray)!!
            slider.thumbTintList = ContextCompat.getColorStateList(_activity, R.color.slider_inactive_gray)!!
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        initBitmap()

        binding.tvCancle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.sdBlur.setLabelFormatter {
            it.toInt().toString()
        }

        binding.ivAuto.setOnClickListener {
            setSliderEnabledState(binding.sdBlur, true)
            setSliderEnabledState(binding.sdThick, false)
            binding.btnAutoMosaic.visibility = View.VISIBLE
            mode = EditMode.AUTO
        }

        binding.ivManual.setOnClickListener {
            setSliderEnabledState(binding.sdBlur, true)
            setSliderEnabledState(binding.sdThick, true)
            binding.btnAutoMosaic.visibility = View.GONE
            mode = EditMode.MANUAL
        }

        binding.ivErase.setOnClickListener {
            setSliderEnabledState(binding.sdBlur, false)
            setSliderEnabledState(binding.sdThick, true)
            binding.btnAutoMosaic.visibility = View.GONE
            mode = EditMode.ERASE
        }

        binding.btnAutoMosaic.setOnClickListener {
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        applyAutoMosaic(faces, binding.sdBlur.value.toInt())
                    } else {
                        activity.showToast(activity.getString(R.string.edit_no_face))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed: $e")
                }
        }

        binding.sdThick.addOnChangeListener { slider, value, fromUser ->
            val thickness = value.toInt()
            showBrushPreview(thickness)
        }

        binding.sdThick.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                eraseThickCircle()
            }
        })

        binding.ivBlurCanvas.setOnTouchListener { v, event ->
            if (mode == EditMode.MANUAL) {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    applyManualMosaic(event.x.toInt(), event.y.toInt())
                }
            }
            true
        }
    }

    private fun initBitmap() {
        binding.flCanvas.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.flCanvas.viewTreeObserver.removeOnGlobalLayoutListener(this)

                try {
                    val imageViewWidth = binding.ivPhoto.width

                    val original = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        mainViewModel.getUri()
                    )

                    val aspectRatio = original.width.toFloat() / original.height
                    val imageViewHeight = (imageViewWidth / aspectRatio).toInt()

                    originalBitmap = Bitmap.createScaledBitmap(original, imageViewWidth, imageViewHeight, true)
                    binding.ivPhoto.setImageBitmap(originalBitmap)
                    image = InputImage.fromBitmap(originalBitmap, 0)

                    blurCanvas = Bitmap.createBitmap(
                        imageViewWidth,
                        imageViewHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    thickCanvas = Bitmap.createBitmap(
                        imageViewWidth,
                        imageViewHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    binding.ivBlurCanvas.setImageBitmap(blurCanvas)
                    binding.ivThickCanvas.setImageBitmap(thickCanvas)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })
    }


    enum class EditMode {
        AUTO, MANUAL, ERASE
    }

}
