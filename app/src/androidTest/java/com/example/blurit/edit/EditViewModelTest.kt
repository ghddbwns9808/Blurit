import android.graphics.Bitmap
import android.graphics.Rect
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.blurit.edit.EditViewModel
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

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

}
