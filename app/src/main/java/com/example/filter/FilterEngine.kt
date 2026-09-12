package com.example.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class PhotoFilter(
    val id: String,
    val name: String,
    val category: String,
    val matrix: FloatArray,
    val vignetteStrength: Float = 0f,
    val warmth: Float = 0f
)

data class EditAdjustments(
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 0f,      // -100 to 100
    val saturation: Float = 0f,    // -100 to 100
    val temperature: Float = 0f,   // -100 (cool) to 100 (warm)
    val tint: Float = 0f,          // -100 (green) to 100 (magenta)
    val highlights: Float = 0f,    // -100 to 100
    val shadows: Float = 0f,       // -100 to 100
    val sharpness: Float = 0f,     // 0 to 100
    val vignette: Float = 0f,      // 0 to 100
    val grain: Float = 0f,         // 0 to 100
    val blur: Float = 0f,          // 0 to 100
    val rotationAngle: Float = 0f, // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

object FilterEngine {

    val filters: List<PhotoFilter> = listOf(
        PhotoFilter(
            id = "normal",
            name = "Original",
            category = "Standard",
            matrix = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        // Cinematic
        PhotoFilter(
            id = "cinematic_teal_orange",
            name = "Teal & Orange",
            category = "Cinematic",
            matrix = floatArrayOf(
                1.3f, 0f, 0f, 0f, 10f,
                0f, 1.05f, 0f, 0f, 0f,
                0.1f, 0.1f, 1.35f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.25f
        ),
        PhotoFilter(
            id = "cinematic_blockbuster",
            name = "Blockbuster",
            category = "Cinematic",
            matrix = floatArrayOf(
                1.2f, 0f, 0f, 0f, -5f,
                0f, 1.15f, 0f, 0f, 5f,
                0f, 0f, 1.3f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.35f
        ),
        PhotoFilter(
            id = "cinematic_neo_noir",
            name = "Neo-Noir",
            category = "Cinematic",
            matrix = floatArrayOf(
                0.9f, 0f, 0f, 0f, -20f,
                0f, 0.85f, 0f, 0f, -15f,
                0.2f, 0.2f, 1.4f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.45f
        ),
        // Vintage & 1980s
        PhotoFilter(
            id = "vintage_kodak_70s",
            name = "Kodachrome",
            category = "Vintage",
            matrix = floatArrayOf(
                1.25f, 0.1f, 0f, 0f, 18f,
                0f, 1.15f, 0.1f, 0f, 8f,
                0f, 0f, 0.85f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.2f
        ),
        PhotoFilter(
            id = "vintage_sepia",
            name = "Sepia Gold",
            category = "Vintage",
            matrix = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 25f,
                0.349f, 0.686f, 0.168f, 0f, 15f,
                0.272f, 0.534f, 0.131f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.3f
        ),
        PhotoFilter(
            id = "retro_1980s_synth",
            name = "1980s Synth",
            category = "1980s",
            matrix = floatArrayOf(
                1.3f, 0f, 0.2f, 0f, 25f,
                0.1f, 0.9f, 0.1f, 0f, -10f,
                0.2f, 0f, 1.4f, 0f, 35f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.2f
        ),
        PhotoFilter(
            id = "retro_vhs",
            name = "VHS Dream",
            category = "1980s",
            matrix = floatArrayOf(
                1.1f, 0.2f, 0f, 0f, 15f,
                0f, 1.05f, 0.1f, 0f, 10f,
                0.1f, 0f, 1.15f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        // Film
        PhotoFilter(
            id = "film_fuji_velvia",
            name = "Fuji Velvia",
            category = "Film",
            matrix = floatArrayOf(
                1.25f, 0f, 0f, 0f, 5f,
                0f, 1.3f, 0f, 0f, 8f,
                0f, 0f, 1.2f, 0f, 12f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        PhotoFilter(
            id = "film_portra_400",
            name = "Portra 400",
            category = "Film",
            matrix = floatArrayOf(
                1.12f, 0.05f, 0f, 0f, 12f,
                0f, 1.08f, 0.02f, 0f, 6f,
                0f, 0f, 0.96f, 0f, 2f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        PhotoFilter(
            id = "film_ilford_hp5",
            name = "Ilford HP5",
            category = "Film",
            matrix = floatArrayOf(
                0.3f, 0.59f, 0.11f, 0f, -5f,
                0.3f, 0.59f, 0.11f, 0f, -5f,
                0.3f, 0.59f, 0.11f, 0f, -5f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.25f
        ),
        // Black & White
        PhotoFilter(
            id = "bw_high_contrast",
            name = "Noir B&W",
            category = "B&W",
            matrix = floatArrayOf(
                0.45f, 0.75f, 0.15f, 0f, -25f,
                0.45f, 0.75f, 0.15f, 0f, -25f,
                0.45f, 0.75f, 0.15f, 0f, -25f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.3f
        ),
        PhotoFilter(
            id = "bw_silver",
            name = "Silver Tone",
            category = "B&W",
            matrix = floatArrayOf(
                0.33f, 0.55f, 0.12f, 0f, 15f,
                0.33f, 0.55f, 0.12f, 0f, 15f,
                0.33f, 0.55f, 0.12f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        // Portrait & Golden Hour
        PhotoFilter(
            id = "portrait_soft_glow",
            name = "Soft Glow",
            category = "Portrait",
            matrix = floatArrayOf(
                1.1f, 0.05f, 0f, 0f, 14f,
                0f, 1.05f, 0.02f, 0f, 10f,
                0f, 0f, 1.0f, 0f, 8f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        PhotoFilter(
            id = "golden_hour",
            name = "Golden Hour",
            category = "Golden Hour",
            matrix = floatArrayOf(
                1.32f, 0.08f, 0f, 0f, 22f,
                0.05f, 1.15f, 0f, 0f, 12f,
                0f, 0f, 0.82f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.2f
        ),
        // Moody & Cool
        PhotoFilter(
            id = "moody_slate",
            name = "Moody Slate",
            category = "Moody",
            matrix = floatArrayOf(
                0.92f, 0f, 0.05f, 0f, -15f,
                0f, 0.98f, 0.05f, 0f, -10f,
                0.05f, 0.05f, 1.18f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.35f
        ),
        PhotoFilter(
            id = "cool_pacific",
            name = "Pacific Blue",
            category = "Warm/Cool",
            matrix = floatArrayOf(
                0.9f, 0f, 0f, 0f, -8f,
                0f, 1.02f, 0f, 0f, 2f,
                0.1f, 0.1f, 1.35f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        // Instagram-style
        PhotoFilter(
            id = "insta_valencia",
            name = "Valencia",
            category = "Social",
            matrix = floatArrayOf(
                1.15f, 0.06f, 0f, 0f, 18f,
                0f, 1.1f, 0.04f, 0f, 12f,
                0f, 0f, 0.92f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        ),
        PhotoFilter(
            id = "insta_clarendon",
            name = "Clarendon",
            category = "Social",
            matrix = floatArrayOf(
                1.2f, 0f, 0f, 0f, -10f,
                0f, 1.25f, 0f, 0f, -5f,
                0f, 0f, 1.35f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            ),
            vignetteStrength = 0.2f
        )
    )

    fun createCustomFilterMatrix(
        redGain: Float,    // 0.5 to 2.0
        greenGain: Float,  // 0.5 to 2.0
        blueGain: Float,   // 0.5 to 2.0
        saturation: Float, // 0.0 to 2.0
        contrast: Float    // 0.5 to 2.0
    ): FloatArray {
        val cm = ColorMatrix()
        cm.setScale(redGain * contrast, greenGain * contrast, blueGain * contrast, 1f)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        cm.postConcat(satMatrix)
        return cm.array
    }

    fun applyAdjustmentsAndFilter(
        source: Bitmap,
        filter: PhotoFilter?,
        adjust: EditAdjustments
    ): Bitmap {
        // Handle rotation and flip first
        var workingBitmap = source
        if (adjust.rotationAngle != 0f || adjust.flipHorizontal || adjust.flipVertical) {
            val matrix = Matrix()
            if (adjust.rotationAngle != 0f) matrix.postRotate(adjust.rotationAngle)
            val sx = if (adjust.flipHorizontal) -1f else 1f
            val sy = if (adjust.flipVertical) -1f else 1f
            if (sx != 1f || sy != 1f) matrix.postScale(sx, sy)
            workingBitmap = Bitmap.createBitmap(workingBitmap, 0, 0, workingBitmap.width, workingBitmap.height, matrix, true)
        }

        val result = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Combine base color matrix
        val combinedMatrix = ColorMatrix()

        // 1. Base filter matrix
        if (filter != null) {
            combinedMatrix.set(ColorMatrix(filter.matrix))
        }

        // 2. Brightness (-100 to 100) -> offset in RGB
        val brightnessOffset = adjust.brightness * 1.5f
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightnessOffset,
            0f, 1f, 0f, 0f, brightnessOffset,
            0f, 0f, 1f, 0f, brightnessOffset,
            0f, 0f, 0f, 1f, 0f
        ))
        combinedMatrix.postConcat(brightnessMatrix)

        // 3. Contrast (-100 to 100) -> scale
        val contrastScale = (adjust.contrast / 100f + 1f).coerceIn(0.2f, 2.5f)
        val contrastOffset = 128f * (1f - contrastScale)
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrastScale, 0f, 0f, 0f, contrastOffset,
            0f, contrastScale, 0f, 0f, contrastOffset,
            0f, 0f, contrastScale, 0f, contrastOffset,
            0f, 0f, 0f, 1f, 0f
        ))
        combinedMatrix.postConcat(contrastMatrix)

        // 4. Saturation (-100 to 100)
        val satScale = (adjust.saturation / 100f + 1f).coerceIn(0f, 3f)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(satScale)
        combinedMatrix.postConcat(satMatrix)

        // 5. Temperature (Warmth / Coolness) & Tint
        val tempR = if (adjust.temperature > 0) adjust.temperature * 0.4f else 0f
        val tempB = if (adjust.temperature < 0) -adjust.temperature * 0.4f else 0f
        val tintG = if (adjust.tint < 0) -adjust.tint * 0.3f else 0f
        val tintM = if (adjust.tint > 0) adjust.tint * 0.3f else 0f

        val tempTintMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, tempR + tintM,
            0f, 1f, 0f, 0f, tintG,
            0f, 0f, 1f, 0f, tempB + tintM * 0.5f,
            0f, 0f, 0f, 1f, 0f
        ))
        combinedMatrix.postConcat(tempTintMatrix)

        // Draw image with combined color filter
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(workingBitmap, 0f, 0f, paint)

        // 6. Vignette overlay if applicable
        val effectiveVignette = max(filter?.vignetteStrength ?: 0f, adjust.vignette / 100f)
        if (effectiveVignette > 0.05f) {
            val cx = result.width / 2f
            val cy = result.height / 2f
            val radius = max(cx, cy) * 1.35f
            val alpha = (effectiveVignette * 230).toInt().coerceIn(0, 255)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb(alpha / 3, 0, 0, 0), Color.argb(alpha, 0, 0, 0)),
                    floatArrayOf(0.4f, 0.75f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), vignettePaint)
        }

        // 7. Grain overlay if adjusted
        if (adjust.grain > 5f) {
            val grainAlpha = (adjust.grain * 0.8f).toInt().coerceIn(5, 75)
            val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = grainAlpha
            }
            val numSpecks = (result.width * result.height / 3500 * (adjust.grain / 50f)).toInt()
            for (i in 0 until min(numSpecks, 4000)) {
                val rx = Random.nextFloat() * result.width
                val ry = Random.nextFloat() * result.height
                val rSize = Random.nextFloat() * 1.8f + 0.6f
                canvas.drawCircle(rx, ry, rSize, grainPaint)
            }
        }

        return result
    }
}
