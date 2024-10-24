package com.example.blurit.edit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import com.example.blurit.MainActivity
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentEditBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException
import kotlin.math.pow

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
    private lateinit var blurCanvasBitmap: Bitmap
    private var mode: EditMode = EditMode.AUTO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = _activity as MainActivity
        initView()
        detector = FaceDetection.getClient(highAccuracyOpts)
    }

    private fun applyAutoMosaic(faces: List<Face>, size: Int) {
        blurCanvasBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        binding.ivBlurCanvas.setImageBitmap(blurCanvasBitmap)

        val mosaicBitmap = blurCanvasBitmap.copy(blurCanvasBitmap.config, true)
        val canvas = Canvas(mosaicBitmap)

        faces.forEach { face ->
            val boundingBox = face.boundingBox

            val centerX = boundingBox.centerX().toFloat()
            val centerY = boundingBox.centerY().toFloat()
            val radius = boundingBox.width().coerceAtMost(boundingBox.height()) / 2f

            val blockSize = radius.toInt() / (13 - size)

            // BoundingBox 내에서 모자이크 블록을 적용
            for (y in boundingBox.top until boundingBox.bottom step blockSize) {
                for (x in boundingBox.left until boundingBox.right step blockSize) {

                    // 블록의 중심 좌표 계산
                    val blockCenterX = x + blockSize / 2f
                    val blockCenterY = y + blockSize / 2f

                    // 블록 중심이 원 내부에 있는지 확인
                    val distanceFromCenter = Math.sqrt(
                        (blockCenterX - centerX).toDouble().pow(2.0) +
                                (blockCenterY - centerY).toDouble().pow(2.0)
                    )

                    if (distanceFromCenter <= radius) {
                        // 원 내부에 있을 때만 모자이크 적용
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

        binding.ivBlurCanvas.setImageBitmap(mosaicBitmap)
    }

    private fun initView() {
        binding.ivPhoto.setImageURI(mainViewModel.getUri())
        initBitmap()

        binding.tvCancle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.sdBlur.setLabelFormatter {
            it.toInt().toString()
        }

        binding.sdThick.setLabelFormatter {
            it.toInt().toString()
        }

        binding.ivAuto.setOnClickListener {
            binding.llBlur.visibility = View.VISIBLE
            binding.llThick.visibility = View.GONE
            binding.btnAutoMosaic.visibility = View.VISIBLE
            mode = EditMode.AUTO
        }

        binding.ivManual.setOnClickListener {
            binding.llBlur.visibility = View.VISIBLE
            binding.llThick.visibility = View.VISIBLE
            binding.btnAutoMosaic.visibility = View.GONE
            mode = EditMode.MANUAL
        }

        binding.ivErase.setOnClickListener {
            binding.llBlur.visibility = View.GONE
            binding.llThick.visibility = View.VISIBLE
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
    }

    private fun initBitmap() {
        try {
            image = InputImage.fromFilePath(requireContext(), mainViewModel.getUri()!!)
            originalBitmap = MediaStore.Images.Media.getBitmap(
                requireContext().contentResolver,
                mainViewModel.getUri()
            )

            blurCanvasBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888
            )

            binding.ivBlurCanvas.setImageBitmap(blurCanvasBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    enum class EditMode {
        AUTO, MANUAL, ERASE
    }

}
