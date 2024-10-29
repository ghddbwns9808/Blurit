package com.example.blurit.edit

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.blurit.MainActivity
import com.example.blurit.MainViewModel
import com.example.blurit.R
import com.example.blurit.base.BaseFragment
import com.example.blurit.databinding.FragmentEditBinding
import com.google.android.material.slider.Slider
import java.io.IOException
import java.io.OutputStream


class EditFragment : BaseFragment<FragmentEditBinding>(
    FragmentEditBinding::bind, R.layout.fragment_edit
) {
    private lateinit var activity: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: EditViewModel by viewModels()
    private var mode: EditMode = EditMode.AUTO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = _activity as MainActivity
        initView()
        initObserver()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        initBitmap()

        binding.tvCancle.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.tvSave.setOnClickListener {
            val finalBitmap = viewModel.mergeBitmaps()
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
                        viewModel.applyErase(
                            binding.ivPhoto,
                            event.x.toInt(),
                            event.y.toInt(),
                            binding.sdThick.value.toInt()
                        )
                    } else if (event.action == MotionEvent.ACTION_MOVE) {
                        viewModel.applyErase(
                            binding.ivPhoto,
                            event.x.toInt(),
                            event.y.toInt(),
                            binding.sdThick.value.toInt()
                        )
                    }
                }

                else -> Unit
            }
            true
        }


    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "${viewModel.originalBitmapMetadata.config.name}_blurit.jpg"
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
                    Toast.makeText(activity, activity.getString(R.string.edit_save_success), Toast.LENGTH_SHORT).show()
                    showShareDialog(uri)
                } else {
                    Toast.makeText(activity, activity.getString(R.string.edit_save_fail), Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(activity, activity.getString(R.string.edit_save_fail), Toast.LENGTH_SHORT).show()
            } finally {
                outputStream?.close()
            }
        } else {
            Toast.makeText(activity, activity.getString(R.string.edit_save_fail), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        }
    }

    enum class EditMode {
        AUTO, MANUAL, ERASE
    }

}
