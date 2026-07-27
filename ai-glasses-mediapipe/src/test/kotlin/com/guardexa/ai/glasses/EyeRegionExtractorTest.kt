
package com.guardexa.ai.glasses

import com.guardexa.ai.face.FaceLandmarkResult
import com.guardexa.ai.face.LandmarkPoint
import kotlin.test.Test
import kotlin.test.assertNull

class EyeRegionExtractorTest {

    @Test
    fun missingFaceReturnsNull() {
        val extractor = EyeRegionExtractor()
        val bitmap = android.graphics.Bitmap.createBitmap(
            320,
            240,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        try {
            val result = extractor.extract(
                source = bitmap,
                landmarks = FaceLandmarkResult(
                    faces = emptyList(),
                    inferenceMillis = 1L
                )
            )

            assertNull(result)
        } finally {
            bitmap.recycle()
        }
    }
}
