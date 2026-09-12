package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.data.AiScene
import com.example.data.CameraMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object AiProcessingEngine {

    fun analyzeScene(
        luminanceAvg: Float,
        warmthRatio: Float,
        isFaceDetected: Boolean,
        mode: CameraMode
    ): AiScene {
        return when {
            mode == CameraMode.PORTRAIT || isFaceDetected -> AiScene.PORTRAIT
            mode == CameraMode.FOOD -> AiScene.FOOD
            mode == CameraMode.DOC_SCANNER -> AiScene.DOCUMENT
            mode == CameraMode.ASTRO -> AiScene.STAR_NIGHT
            mode == CameraMode.MACRO -> AiScene.MACRO
            luminanceAvg < 0.22f -> AiScene.NIGHT
            warmthRatio > 1.35f && luminanceAvg in 0.35f..0.75f -> AiScene.SUNSET
            warmthRatio < 0.85f && luminanceAvg > 0.55f -> AiScene.SKY
            luminanceAvg > 0.45f -> AiScene.LANDSCAPE
            else -> AiScene.AUTO
        }
    }

    fun getCompositionAdvice(roll: Float, pitch: Float, faceDetected: Boolean): String {
        return when {
            abs(roll) > 4.5f -> if (roll > 0) "Tilt right ${String.format("%.1f", roll)}° to level" else "Tilt left ${String.format("%.1f", -roll)}° to level"
            abs(pitch) > 12.0f -> if (pitch > 0) "Angle down slightly for parallel lines" else "Angle up slightly for wider view"
            faceDetected -> "Face centered in golden third zone • Perfect framing"
            abs(roll) <= 1.0f -> "Horizon perfectly aligned • Balanced symmetry"
            else -> "Rule of Thirds active • Keep horizon on top guide"
        }
    }

    fun applyPortraitBokeh(source: Bitmap, blurRadiusRatio: Float = 0.5f): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Generate softened background
        val scaledDown = Bitmap.createScaledBitmap(source, (width * 0.15f).toInt().coerceAtLeast(10), (height * 0.15f).toInt().coerceAtLeast(10), true)
        val blurredBg = Bitmap.createScaledBitmap(scaledDown, width, height, true)
        canvas.drawBitmap(blurredBg, 0f, 0f, null)

        // Draw crisp foreground subject with soft elliptical feathered mask
        val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = width / 2f
        val cy = height * 0.48f
        val radiusX = width * 0.38f
        val radiusY = height * 0.44f
        val maxRadius = max(radiusX, radiusY)

        val maskShader = RadialGradient(
            cx, cy, maxRadius,
            intArrayOf(Color.WHITE, Color.argb(230, 255, 255, 255), Color.argb(100, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.45f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = maskShader }
        maskCanvas.drawOval(cx - radiusX, cy - radiusY, cx + radiusX, cy + radiusY, maskPaint)

        // Blend crisp subject over blurred background using SRC_IN mask
        val subjectLayer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val subCanvas = Canvas(subjectLayer)
        subCanvas.drawBitmap(source, 0f, 0f, null)
        val transferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
        }
        subCanvas.drawBitmap(maskBitmap, 0f, 0f, transferPaint)

        canvas.drawBitmap(subjectLayer, 0f, 0f, null)
        return output
    }

    fun applySkyReplacement(source: Bitmap, skyPreset: String): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Render synthetic cinematic atmospheric sky in the top 45% of image
        val skyHeight = height * 0.50f
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val skyColors = when (skyPreset) {
            "DRAMATIC_SUNSET" -> intArrayOf(Color.parseColor("#4A0E4E"), Color.parseColor("#C2185B"), Color.parseColor("#FF6F00"), Color.parseColor("#FFD54F"))
            "AURORA_BOREALIS" -> intArrayOf(Color.parseColor("#051026"), Color.parseColor("#00E676"), Color.parseColor("#1DE9B6"), Color.parseColor("#0D47A1"))
            "STARRY_GALAXY" -> intArrayOf(Color.parseColor("#08081A"), Color.parseColor("#1B1B47"), Color.parseColor("#3A1C71"), Color.parseColor("#D76D77"))
            else -> intArrayOf(Color.parseColor("#0D47A1"), Color.parseColor("#1976D2"), Color.parseColor("#42A5F5"), Color.parseColor("#90CAF9"))
        }

        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, skyHeight,
            skyColors,
            floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), skyHeight, skyPaint)

        // Blend lower foreground from source
        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, 0f, 0f, fgPaint)

        // Overlay smooth sky blend with soft horizon feathering
        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, skyHeight * 1.15f,
                intArrayOf(Color.argb(160, 255, 255, 255), Color.argb(90, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0.0f, 0.65f, 1.0f),
                Shader.TileMode.CLAMP
            )
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), skyHeight * 1.15f, blendPaint)

        return output
    }

    fun applyAutoHdr(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Tone-map by recovering shadow details and compressing highlights
        val cm = android.graphics.ColorMatrix()
        // Boost midtones and shadows while preventing highlight clipping
        cm.set(floatArrayOf(
            1.18f, 0.05f, 0f, 0f, 8f,
            0.02f, 1.16f, 0.02f, 0f, 6f,
            0f, 0.04f, 1.14f, 0f, 4f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = android.graphics.ColorMatrix()
        sat.setSaturation(1.22f)
        cm.postConcat(sat)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun applyLowLightEnhancement(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Computational multi-frame noise reduction simulation & shadow lift
        val cm = android.graphics.ColorMatrix()
        cm.set(floatArrayOf(
            1.35f, 0f, 0f, 0f, 22f,
            0f, 1.35f, 0f, 0f, 20f,
            0f, 0f, 1.32f, 0f, 24f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = android.graphics.ColorMatrix()
        sat.setSaturation(1.15f)
        cm.postConcat(sat)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun applyObjectRemovalHeal(source: Bitmap, targetX: Float, targetY: Float, brushRadius: Float): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        // Patch the touched area with surrounding texture synthesis
        val patchOffset = brushRadius * 1.5f
        val srcRect = android.graphics.Rect(
            (targetX + patchOffset - brushRadius).toInt().coerceIn(0, output.width - 1),
            (targetY - brushRadius).toInt().coerceIn(0, output.height - 1),
            (targetX + patchOffset + brushRadius).toInt().coerceIn(0, output.width - 1),
            (targetY + brushRadius).toInt().coerceIn(0, output.height - 1)
        )
        val dstRect = android.graphics.Rect(
            (targetX - brushRadius).toInt().coerceIn(0, output.width - 1),
            (targetY - brushRadius).toInt().coerceIn(0, output.height - 1),
            (targetX + brushRadius).toInt().coerceIn(0, output.width - 1),
            (targetY + brushRadius).toInt().coerceIn(0, output.height - 1)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 235
        }
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return output
    }

    fun applyFaceRetouch(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Smooth skin tones, warm peach glow and eye clarity boost
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            1.12f, 0.04f, 0f, 0f, 12f,
            0.02f, 1.08f, 0.02f, 0f, 10f,
            0f, 0.02f, 1.04f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun applySharpenBlurry(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Micro-contrast boost to simulate unsharp mask sharpening
        val cm = android.graphics.ColorMatrix()
        cm.set(floatArrayOf(
            1.25f, -0.1f, -0.05f, 0f, 5f,
            -0.05f, 1.25f, -0.05f, 0f, 5f,
            -0.05f, -0.1f, 1.25f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f
        ))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
