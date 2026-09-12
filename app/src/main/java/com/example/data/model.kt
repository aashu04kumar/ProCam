package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class CameraMode(val title: String, val iconName: String) {
    PHOTO("Photo", "Camera"),
    PORTRAIT("Portrait", "Portrait"),
    NIGHT("Night", "Nightlight"),
    PRO("Pro", "Tune"),
    VIDEO("Video", "Videocam"),
    SLOW_MO("Slow-Mo", "SlowMotionVideo"),
    TIME_LAPSE("Time-Lapse", "Timelapse"),
    PANORAMA("Panorama", "PanoramaHorizontal"),
    PANORAMA_360("360°", "PanoramaPhotosphere"),
    LONG_EXPOSURE("Long Exp", "BlurOn"),
    MACRO("Macro", "FilterVintage"),
    ACTION("Action", "DirectionsRun"),
    DOC_SCANNER("Doc Scan", "DocumentScanner"),
    FOOD("Food", "Restaurant"),
    ASTRO("Astro", "AutoAwesome"),
    LIGHT_PAINTING("Light Paint", "Brush")
}

enum class LensType(val label: String, val zoomFactor: Float) {
    ULTRA_WIDE("0.5×", 0.5f),
    WIDE("1×", 1.0f),
    TELE_2X("2×", 2.0f),
    TELE_3X("3×", 3.0f),
    TELE_5X("5×", 5.0f)
}

enum class GridType(val displayName: String) {
    NONE("Off"),
    RULE_OF_THIRDS("3×3 Thirds"),
    GOLDEN_RATIO("Golden Ratio"),
    SQUARE("1:1 Square"),
    CROSSHAIR("Crosshair")
}

enum class AspectRatioOption(val label: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("Full", 19.5f / 9f)
}

enum class VideoQualityOption(val label: String, val fps: Int) {
    UHD_4K_60("4K 60fps", 60),
    UHD_4K_30("4K 30fps", 30),
    FHD_1080P_120("1080p 120fps (Slow-Mo)", 120),
    FHD_1080P_60("1080p 60fps", 60),
    FHD_1080P_30("1080p 30fps", 30),
    HD_720P("720p 30fps", 30)
}

enum class AiScene(val label: String, val icon: String, val advice: String) {
    AUTO("AI Auto", "AutoFixHigh", "Optimal auto tone enabled"),
    PORTRAIT("Portrait", "Face", "Soft bokeh & skin glow applied"),
    LANDSCAPE("Landscape", "Terrain", "Deep greens and azure skies"),
    NIGHT("Night Scene", "DarkMode", "Multi-frame low-light boost"),
    FOOD("Food", "Restaurant", "Warm vibrant saturation"),
    MACRO("Macro Flower", "Yard", "High contrast micro-details"),
    DOCUMENT("Document", "Article", "High contrast black-white text"),
    SUNSET("Sunset / Golden", "WbTwilight", "Amber warmth & HDR highlight recovery"),
    SKY("Sky & Clouds", "Cloud", "Sky clarity & polarized contrast"),
    ARCHITECTURE("Architecture", "Apartment", "Perspective keystone assist active"),
    STAR_NIGHT("Star Field", "NightlightRound", "Long exposure astro-stacking ready")
}

@Entity(tableName = "captured_media")
data class CapturedMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val mediaType: String = "IMAGE", // "IMAGE" or "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val cameraMode: String = CameraMode.PHOTO.name,
    val width: Int = 4032,
    val height: Int = 3024,
    val iso: Int = 100,
    val shutterSpeed: String = "1/125s",
    val focalLength: String = "24mm",
    val aperture: String = "f/1.8",
    val whiteBalance: String = "5500K",
    val evCompensation: Float = 0.0f,
    val isRaw: Boolean = false,
    val isPrivate: Boolean = false,
    val isFavorite: Boolean = false,
    val filterName: String = "Normal",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val fileSize: Long = 0L
) : Serializable

@Entity(tableName = "camera_presets")
data class CameraPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iso: Int,
    val shutterSpeed: String,
    val evCompensation: Float,
    val whiteBalanceKelvin: Int,
    val focusDistance: Float,
    val mode: String = CameraMode.PRO.name,
    val lens: String = "1×"
)

data class WatermarkConfig(
    val enabled: Boolean = false,
    val style: String = "LEICA_STYLE", // "LEICA_STYLE", "CLEAN_TEXT", "DATE_TIME"
    val customText: String = "Lite Pro Cam | Master Optics",
    val showDate: Boolean = true,
    val showExifData: Boolean = true,
    val isLeicaStyle: Boolean = true,
    val includeDateTime: Boolean = true
)

data class ExifDetails(
    val cameraModel: String = "Lite Pro Cam Engine",
    val resolution: String = "12.2 MP (4032 × 3024)",
    val iso: String = "ISO 100",
    val shutterSpeed: String = "1/125s",
    val aperture: String = "f/1.8",
    val focalLength: String = "26mm (35mm eq.)",
    val whiteBalance: String = "5500K Daylight",
    val exposureBias: String = "+0.0 EV",
    val dateTime: String = "",
    val fileSizeBytes: String = "4.2 MB",
    val format: String = "JPEG + DNG (RAW)",
    val coordinates: String? = null
)
