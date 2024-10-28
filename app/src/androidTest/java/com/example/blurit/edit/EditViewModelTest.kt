import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.ImageView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.blurit.edit.EditViewModel
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class EditViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: EditViewModel

    @Before
    fun setup() {
        viewModel = EditViewModel()
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

        assertEquals(imageViewWidth, initializedOriginalCanvas!!.width)
        assertEquals(imageViewHeight, initializedOriginalCanvas.height)

        assertEquals(200, initializedBlurCanvas!!.width)
        assertEquals(imageViewHeight, initializedBlurCanvas.height)

        assertEquals(200, initializedThicknessCanvas!!.width)
        assertEquals(imageViewHeight, initializedThicknessCanvas.height)
    }

    @Test
    fun applyAutoMosaicShouldThrowNPEWhenBoundingBoxIsNull() {
        // Given
        val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        viewModel.initBitmap(originalBitmap, 100)

        // Mocking Face object with null boundingBox
        val face = Mockito.mock(Face::class.java)
        `when`(face.boundingBox).thenReturn(null)
        val faces = listOf(face)

        // When & Then: Verify NullPointerException is thrown
        assertThrows(NullPointerException::class.java) {
            viewModel.applyAutoMosaic(faces, 5)
        }
    }

    @Test
    fun applyManualMosaicTest() {
        // Given
        val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        viewModel.initBitmap(originalBitmap, 100)

        // Mock an ImageView and set up test coordinates and parameters
        val mockImageView = mock(ImageView::class.java)
        val touchX = 50
        val touchY = 50
        val thickness = 10
        val blur = 5

        // Set initial blurCanvas to observe changes
        val initialBlurCanvas = viewModel.blurCanvas.value
        assertNotNull("blurCanvas should be initialized", initialBlurCanvas)

        // When
        viewModel.applyManualMosaic(mockImageView, touchX, touchY, thickness, blur)

        // Then
        val updatedBlurCanvas = viewModel.blurCanvas.value
        assertNotNull("blurCanvas should be updated after applyManualMosaic", updatedBlurCanvas)

        assertNotEquals(initialBlurCanvas, updatedBlurCanvas)
    }
}
