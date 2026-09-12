package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import com.example.data.CameraPreset
import com.example.data.CapturedMedia
import com.example.data.ExifDetails
import com.example.data.WatermarkConfig
import com.example.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val mediaDao = db.mediaDao()
    private val presetDao = db.presetDao()

    val publicMedia: Flow<List<CapturedMedia>> = mediaDao.getAllPublicMedia()
    val privateMedia: Flow<List<CapturedMedia>> = mediaDao.getPrivateMedia()
    val presets: Flow<List<CameraPreset>> = presetDao.getAllPresets()

    suspend fun getMediaById(id: Long): CapturedMedia? = withContext(Dispatchers.IO) {
        mediaDao.getMediaById(id)
    }

    suspend fun saveCapturedPhoto(
        bitmap: Bitmap,
        mode: String,
        iso: Int,
        shutterSpeed: String,
        whiteBalance: String,
        ev: Float,
        isRaw: Boolean,
        filterName: String,
        watermarkConfig: WatermarkConfig,
        latitude: Double? = null,
        longitude: Double? = null
    ): CapturedMedia = withContext(Dispatchers.IO) {
        val mediaDir = File(context.filesDir, "captures").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "LPC_${timeStamp}.jpg"
        val file = File(mediaDir, fileName)

        // Process watermark if requested
        val finalBitmap = if (watermarkConfig.enabled) {
            applyWatermark(bitmap, watermarkConfig, iso, shutterSpeed, whiteBalance)
        } else {
            bitmap
        }

        FileOutputStream(file).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // Write EXIF tags into file
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_MAKE, "Lite Pro Cam")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Lite Pro Optics Engine")
            exif.setAttribute(ExifInterface.TAG_DATETIME, SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date()))
            exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, iso.toString())
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, shutterSpeed.replace("s", ""))
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, "1.8")
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "26/1")
            exif.saveAttributes()
        } catch (_: Exception) {
        }

        val captured = CapturedMedia(
            filePath = file.absolutePath,
            fileName = fileName,
            mediaType = "IMAGE",
            timestamp = System.currentTimeMillis(),
            cameraMode = mode,
            width = finalBitmap.width,
            height = finalBitmap.height,
            iso = iso,
            shutterSpeed = shutterSpeed,
            focalLength = "26mm",
            aperture = "f/1.8",
            whiteBalance = whiteBalance,
            evCompensation = ev,
            isRaw = isRaw,
            isPrivate = false,
            isFavorite = false,
            filterName = filterName,
            latitude = latitude,
            longitude = longitude,
            fileSize = file.length()
        )

        val id = mediaDao.insertMedia(captured)
        captured.copy(id = id)
    }

    suspend fun saveEditedPhoto(originalMedia: CapturedMedia, editedBitmap: Bitmap, newFilterName: String): CapturedMedia = withContext(Dispatchers.IO) {
        val mediaDir = File(context.filesDir, "captures").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "LPC_EDIT_${timeStamp}.jpg"
        val file = File(mediaDir, fileName)

        FileOutputStream(file).use { out ->
            editedBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
        }

        val newMedia = originalMedia.copy(
            id = 0,
            filePath = file.absolutePath,
            fileName = fileName,
            timestamp = System.currentTimeMillis(),
            width = editedBitmap.width,
            height = editedBitmap.height,
            filterName = newFilterName,
            fileSize = file.length()
        )
        val id = mediaDao.insertMedia(newMedia)
        newMedia.copy(id = id)
    }

    suspend fun saveVideo(videoFile: File, mode: String, quality: String): CapturedMedia = withContext(Dispatchers.IO) {
        val media = CapturedMedia(
            filePath = videoFile.absolutePath,
            fileName = videoFile.name,
            mediaType = "VIDEO",
            timestamp = System.currentTimeMillis(),
            cameraMode = mode,
            width = 1920,
            height = 1080,
            iso = 200,
            shutterSpeed = "1/60s",
            focalLength = "26mm",
            aperture = "f/1.8",
            whiteBalance = "5500K",
            fileSize = videoFile.length(),
            filterName = quality
        )
        val id = mediaDao.insertMedia(media)
        media.copy(id = id)
    }

    suspend fun toggleFavorite(media: CapturedMedia) = withContext(Dispatchers.IO) {
        mediaDao.setFavoriteState(media.id, !media.isFavorite)
    }

    suspend fun togglePrivate(media: CapturedMedia) = withContext(Dispatchers.IO) {
        mediaDao.setPrivateState(media.id, !media.isPrivate)
    }

    suspend fun deleteMedia(media: CapturedMedia) = withContext(Dispatchers.IO) {
        val file = File(media.filePath)
        if (file.exists()) file.delete()
        mediaDao.deleteMediaById(media.id)
    }

    suspend fun insertPreset(preset: CameraPreset): Long = withContext(Dispatchers.IO) {
        presetDao.insertPreset(preset)
    }

    suspend fun deletePreset(id: Long) = withContext(Dispatchers.IO) {
        presetDao.deletePresetById(id)
    }

    fun readExifDetails(media: CapturedMedia): ExifDetails {
        val dateFormatted = SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.US).format(Date(media.timestamp))
        val sizeFormatted = String.format(Locale.US, "%.2f MB", media.fileSize / (1024.0 * 1024.0))
        val coords = if (media.latitude != null && media.longitude != null) {
            String.format(Locale.US, "%.4f° N, %.4f° E", media.latitude, media.longitude)
        } else null

        return ExifDetails(
            cameraModel = "Lite Pro Cam 4K HDR",
            resolution = "${media.width} × ${media.height} (${(media.width * media.height / 1_000_000f).let { String.format(Locale.US, "%.1f MP", it) }})",
            iso = "ISO ${media.iso}",
            shutterSpeed = media.shutterSpeed,
            aperture = media.aperture,
            focalLength = media.focalLength,
            whiteBalance = media.whiteBalance,
            exposureBias = String.format(Locale.US, "%+.1f EV", media.evCompensation),
            dateTime = dateFormatted,
            fileSizeBytes = sizeFormatted,
            format = if (media.isRaw) "RAW DNG + JPEG" else if (media.mediaType == "VIDEO") "MP4 (H.264)" else "JPEG 95%",
            coordinates = coords
        )
    }

    private fun applyWatermark(
        src: Bitmap,
        config: WatermarkConfig,
        iso: Int,
        shutter: String,
        wb: String
    ): Bitmap {
        if (config.style == "LEICA_STYLE") {
            // Elegant Leica-style banner at bottom
            val bannerHeight = (src.height * 0.08f).toInt().coerceAtLeast(140)
            val result = Bitmap.createBitmap(src.width, src.height + bannerHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawBitmap(src, 0f, 0f, null)

            // Draw clean dark or white bottom strip
            val bgPaint = Paint().apply { color = Color.parseColor("#121417") }
            canvas.drawRect(0f, src.height.toFloat(), src.width.toFloat(), (src.height + bannerHeight).toFloat(), bgPaint)

            // Left brand text
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = bannerHeight * 0.28f
                isFakeBoldText = true
            }
            val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9E9E9E")
                textSize = bannerHeight * 0.20f
            }

            val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E53935")
            }

            val margin = src.width * 0.04f
            val dotRadius = bannerHeight * 0.14f
            val dotCenterY = src.height + bannerHeight * 0.5f

            // Red dot symbol
            canvas.drawCircle(margin + dotRadius, dotCenterY, dotRadius, redDotPaint)

            val textStartX = margin + dotRadius * 2 + 24f
            canvas.drawText(config.customText, textStartX, src.height + bannerHeight * 0.45f, textPaint)
            val dateStr = SimpleDateFormat("yyyy.MM.dd  HH:mm", Locale.US).format(Date())
            canvas.drawText(dateStr, textStartX, src.height + bannerHeight * 0.72f, subTextPaint)

            // Right EXIF info
            val rightExif = "ISO $iso  |  $shutter  |  f/1.8  |  $wb"
            val exifWidth = subTextPaint.measureText(rightExif)
            canvas.drawText(rightExif, src.width - margin - exifWidth, src.height + bannerHeight * 0.55f, subTextPaint)

            return result
        } else {
            // Overlay watermark directly on bottom-right corner
            val result = src.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (result.height * 0.035f).coerceAtLeast(28f)
                setShadowLayer(6f, 2f, 2f, Color.BLACK)
            }
            val text = "${config.customText} • ISO $iso $shutter"
            val textWidth = paint.measureText(text)
            val margin = result.width * 0.04f
            canvas.drawText(text, result.width - textWidth - margin, result.height - margin, paint)
            return result
        }
    }
}
