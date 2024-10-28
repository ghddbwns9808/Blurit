package com.example.blurit.edit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.blurit.MainActivity
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentEditBinding
import com.google.android.material.slider.Slider
import com.google.mlkit.vision.common.InputImage
import java.io.IOException
import java.io.OutputStream
import java.util.Stack
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "EditFragment_hong"

class EditFragment : BaseFragment<FragmentEditBinding>(
    FragmentEditBinding::bind, R.layout.fragment_edit
) {
    private lateinit var activity: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: EditViewModel by viewModels()

    private lateinit var originalBitmap: Bitmap
    private lateinit var blurCanvas: Bitmap
    private lateinit var thickCanvas: Bitmap

    private var mode: EditMode = EditMode.AUTO

    private lateinit var originalImageMetadata: Bitmap

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = _activity as MainActivity
        initView()
        initObserver()
    }

    private fun applyErase(touchX: Int, touchY: Int) {
        /*
        val canvas = Canvas(blurCanvas)
        val (bitmapX, bitmapY) = convertTouchToBitmap(touchX, touchY)

        val thickness = (binding.sdThick.value + binding.ivThickCanvas.width / 30).toInt()

        for (y in bitmapY - thickness until bitmapY + thickness) {
            for (x in bitmapX - thickness until bitmapX + thickness) {
                val distanceFromCenter =
                    sqrt((x - bitmapX).toDouble().pow(2.0) + (y - bitmapY).toDouble().pow(2.0))

                if (distanceFromCenter <= thickness) {
                    val paint = Paint().apply {
                        xfermode =
                            android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                    }

                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        (x + 1).toFloat(),
                        (y + 1).toFloat(),
                        paint
                    )
                }
            }
        }

        binding.ivBlurCanvas.setImageBitmap(blurCanvas)

         */
    }

    private fun mergeBitmaps(background: Bitmap, overlay: Bitmap): Bitmap {
        val combinedBitmap =
            Bitmap.createBitmap(background.width, background.height, background.config)
        val canvas = Canvas(combinedBitmap)
        canvas.drawBitmap(background, 0f, 0f, null)
        canvas.drawBitmap(overlay, 0f, 0f, null)
        return combinedBitmap
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "${originalImageMetadata.config.name}_blurit.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/BlurIt")
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    activity.showToast(activity.getString(R.string.edit_save_success))

                    showShareDialog(uri)
                } else {
                    activity.showToast(activity.getString(R.string.edit_save_fail))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                activity.showToast(activity.getString(R.string.edit_save_fail))
            } finally {
                outputStream?.close()
            }
        } else {
            activity.showToast(activity.getString(R.string.edit_save_fail))
        }
    }

    private fun showShareDialog(uri: Uri) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "저장된 사진을 공유해 보세요"))
    }

    private fun setSliderEnabledState(slider: Slider, enabled: Boolean) {
        if (enabled) {
            slider.isEnabled = true
            slider.trackActiveTintList =
                ContextCompat.getColorStateList(_activity, R.color.blurit_pink_dark)!!
            slider.thumbTintList =
                ContextCompat.getColorStateList(_activity, R.color.blurit_pink_dark)!!
        } else {
            slider.isEnabled = false
            slider.trackActiveTintList =
                ContextCompat.getColorStateList(_activity, R.color.slider_inactive_gray)!!
            slider.thumbTintList =
                ContextCompat.getColorStateList(_activity, R.color.slider_inactive_gray)!!
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        initBitmap()

        binding.tvCancle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.tvSave.setOnClickListener {
            val finalBitmap = mergeBitmaps(originalBitmap, blurCanvas)
            saveBitmapToGallery(finalBitmap)
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

        binding.ivUndo.setOnClickListener {
            viewModel.undoLastAction()
        }

        binding.btnAutoMosaic.setOnClickListener {
            viewModel.autoMosaic(binding.sdBlur.value.toInt())
        }

        binding.sdThick.addOnChangeListener { slider, value, fromUser ->
            viewModel.showBrushPreview(value.toInt(), binding.ivPhoto.width)
        }

        binding.sdThick.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.eraseThickCircle()
            }
        })

        binding.ivBlurCanvas.setOnTouchListener { v, event ->
            when (mode) {
                EditMode.MANUAL -> {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        viewModel.saveCanvasState()
                        viewModel.applyManualMosaic(
                            binding.ivPhoto,
                            event.x.toInt(),
                            event.y.toInt(),
                            binding.sdThick.value.toInt(),
                            binding.sdBlur.value.toInt()
                        )
                    } else if (event.action == MotionEvent.ACTION_MOVE) {
                        viewModel.applyManualMosaic(
                            binding.ivPhoto,
                            event.x.toInt(),
                            event.y.toInt(),
                            binding.sdThick.value.toInt(),
                            binding.sdBlur.value.toInt()
                        )
                    }
                }

                EditMode.ERASE -> {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        viewModel.saveCanvasState()
                        applyErase(event.x.toInt(), event.y.toInt())
                    } else if (event.action == MotionEvent.ACTION_MOVE) {
                        applyErase(event.x.toInt(), event.y.toInt())
                    }
                }

                else -> Unit
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

                    val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(
                                requireContext().contentResolver,
                                mainViewModel.getUri()!!
                            )
                        )
                    } else {
                        MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver,
                            mainViewModel.getUri()
                        )
                    }

                    viewModel.initBitmap(original, imageViewWidth)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun initObserver() {
        viewModel.originalCanvas.observe(viewLifecycleOwner) { bitmap ->
            binding.ivPhoto.setImageBitmap(bitmap)
        }

        viewModel.blurCanvas.observe(viewLifecycleOwner) { bitmap ->
            binding.ivBlurCanvas.setImageBitmap(bitmap)
        }

        viewModel.thicknessCanvas.observe(viewLifecycleOwner) { bitmap ->
            binding.ivThickCanvas.setImageBitmap(bitmap)
        }

        viewModel.toastMsg.observe(viewLifecycleOwner) { msg ->
            activity.showToast(msg)
        }
    }

    enum class EditMode {
        AUTO, MANUAL, ERASE
    }

}
