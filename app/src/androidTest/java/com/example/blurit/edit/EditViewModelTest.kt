package com.example.blurit.edit

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class EditViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: EditViewModel

    @Mock
    private lateinit var originalCanvasObserver: Observer<Bitmap>

    @Mock
    private lateinit var blurCanvasObserver: Observer<Bitmap>

    @Mock
    private lateinit var thicknessCanvasObserver: Observer<Bitmap>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = EditViewModel()

        viewModel.originalCanvas.observeForever(originalCanvasObserver)
        viewModel.blurCanvas.observeForever(blurCanvasObserver)
        viewModel.thicknessCanvas.observeForever(thicknessCanvasObserver)
    }

    @Test
    fun initBitmapTest() {
        // Given
        val originalBitmap = Bitmap.createBitmap(100, 120, Bitmap.Config.ARGB_8888)
        val imageViewWidth = 200
        val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val imageViewHeight = (imageViewWidth / ratio).toInt()

        // When
        viewModel.initBitmap(originalBitmap, imageViewWidth)

        // Then
        val initializedOriginalCanvas = viewModel.originalCanvas.value
        val initializedBlurCanvas = viewModel.blurCanvas.value
        val initializedThicknessCanvas = viewModel.thicknessCanvas.value

        assertNotNull("originalCanvas should be initialized", initializedOriginalCanvas)
        assertNotNull("blurCanvas should be initialized", initializedBlurCanvas)
        assertNotNull("thicknessCanvas should be initialized", initializedThicknessCanvas)

        // Additional assertions to confirm that the bitmaps have the expected dimensions and configurations
        assertEquals(imageViewWidth, initializedOriginalCanvas!!.width)
        assertEquals(imageViewHeight, initializedOriginalCanvas.height)

        assertEquals(200, initializedBlurCanvas!!.width)
        assertEquals(imageViewHeight, initializedBlurCanvas.height)

        assertEquals(200, initializedThicknessCanvas!!.width)
        assertEquals(imageViewHeight, initializedThicknessCanvas.height)
    }

}