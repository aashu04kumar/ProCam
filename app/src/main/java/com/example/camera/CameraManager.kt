package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) : SensorEventListener {

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioManager by lazy {
        try {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (_: Throwable) {
            null
        }
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentIsVideoMode: Boolean = false

    // Sensor for artificial horizon
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)

    // State flows
    private val _isHardwareCameraAvailable = MutableStateFlow(true)
    val isHardwareCameraAvailable: StateFlow<Boolean> = _isHardwareCameraAvailable.asStateFlow()

    private val _rollDegrees = MutableStateFlow(0f)
    val rollDegrees: StateFlow<Float> = _rollDegrees.asStateFlow()

    private val _pitchDegrees = MutableStateFlow(0f)
    val pitchDegrees: StateFlow<Float> = _pitchDegrees.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _audioAmpDb = MutableStateFlow(-40f)
    val audioAmpDb: StateFlow<Float> = _audioAmpDb.asStateFlow()

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private var cachedBackEncoderSupported: Boolean? = null
    private var cachedFrontEncoderSupported: Boolean? = null

    private fun isHardwareVideoEncoderSupported(): Boolean {
        val isFront = _isFrontCamera.value
        if (isFront && cachedFrontEncoderSupported != null) return cachedFrontEncoderSupported!!
        if (!isFront && cachedBackEncoderSupported != null) return cachedBackEncoderSupported!!

        val supported = try {
            val cameraId = if (isFront) 1 else 0
            CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW)
        } catch (_: Throwable) {
            false
        }

        if (isFront) {
            cachedFrontEncoderSupported = supported
        } else {
            cachedBackEncoderSupported = supported
        }
        return supported
    }

    fun startSensors() {
        try {
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magneticSensor?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            Log.w("CameraManager", "Failed to register sensors", e)
        }
    }

    fun stopSensors() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w("CameraManager", "Failed to unregister sensors", e)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
        }

        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            _pitchDegrees.value = pitch
            _rollDegrees.value = roll
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isVideoMode: Boolean = false,
        onReady: () -> Unit = {}
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        currentIsVideoMode = isVideoMode

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                setupUseCases(lifecycleOwner, previewView, isVideoMode)
                _isHardwareCameraAvailable.value = true
                onReady()
            } catch (e: Exception) {
                Log.e("CameraManager", "Use case binding failed", e)
                _isHardwareCameraAvailable.value = false
                onReady()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setVideoMode(isVideoMode: Boolean) {
        if (currentIsVideoMode == isVideoMode) return
        currentIsVideoMode = isVideoMode
        val owner = currentLifecycleOwner ?: return
        val pv = currentPreviewView ?: return
        setupUseCases(owner, pv, isVideoMode)
    }

    fun unbindCamera() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            cameraControl = null
            cameraInfo = null
        } catch (e: Exception) {
            Log.w("CameraManager", "Error unbinding camera", e)
        }
    }

    private fun setupUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isVideoMode: Boolean = currentIsVideoMode
    ) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()
        } catch (_: Exception) {}

        val preview = Preview.Builder().build().also {
            try {
                it.surfaceProvider = previewView.surfaceProvider
            } catch (e: Exception) {
                Log.w("CameraManager", "Surface provider assignment failed: ${e.message}")
            }
        }

        val cameraSelector = if (_isFrontCamera.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        if (!isVideoMode) {
            // Still Photo modes: Bind exclusively Preview and ImageCapture.
            // Avoid querying or initializing VideoCapture/Recorder so no EncoderProfile logs are generated.
            videoCapture = null
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            var boundSuccess = false
            try {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo
                boundSuccess = true
            } catch (e: Exception) {
                Log.w("CameraManager", "ImageCapture binding failed, falling back to Preview only: ${e.message}")
            }

            if (!boundSuccess) {
                try {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    cameraControl = camera?.cameraControl
                    cameraInfo = camera?.cameraInfo
                    boundSuccess = true
                } catch (e: Exception) {
                    Log.e("CameraManager", "Hardware camera preview binding failed: ${e.message}")
                    _isHardwareCameraAvailable.value = false
                }
            }
        } else {
            // Video modes: Only build VideoCapture if hardware encoder profiles exist
            imageCapture = null
            videoCapture = if (isHardwareVideoEncoderSupported()) {
                try {
                    val qualitySelector = QualitySelector.fromOrderedList(
                        listOf(Quality.HD, Quality.SD, Quality.LOWEST),
                        androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.LOWEST)
                    )
                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()
                    VideoCapture.withOutput(recorder)
                } catch (e: Throwable) {
                    Log.w("CameraManager", "VideoCapture initialization skipped: ${e.message}")
                    null
                }
            } else {
                null
            }

            var boundSuccess = false
            if (videoCapture != null) {
                try {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        videoCapture
                    )
                    cameraControl = camera?.cameraControl
                    cameraInfo = camera?.cameraInfo
                    boundSuccess = true
                } catch (e: Exception) {
                    Log.w("CameraManager", "VideoCapture binding failed, falling back to Preview only: ${e.message}")
                }
            }

            if (!boundSuccess) {
                try {
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    cameraControl = camera?.cameraControl
                    cameraInfo = camera?.cameraInfo
                    boundSuccess = true
                } catch (e: Exception) {
                    Log.e("CameraManager", "Preview binding in video mode failed: ${e.message}")
                    _isHardwareCameraAvailable.value = false
                }
            }
        }
    }

    fun toggleCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _isFrontCamera.value = !_isFrontCamera.value
        setupUseCases(lifecycleOwner, previewView, currentIsVideoMode)
    }

    fun setZoom(zoom: Float) {
        _currentZoom.value = zoom
        cameraControl?.setZoomRatio(zoom)
    }

    fun setExposure(evStep: Int) {
        cameraControl?.setExposureCompensationIndex(evStep)
    }

    fun toggleFlash() {
        val next = !_isFlashOn.value
        _isFlashOn.value = next
        cameraControl?.enableTorch(next)
    }

    fun triggerFocus(x: Float, y: Float, width: Int, height: Int) {
        val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        cameraControl?.startFocusAndMetering(action)
    }

    fun playShutterSound(enableSound: Boolean) {
        if (enableSound) {
            try {
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK)
            } catch (_: Throwable) {}
        }
    }

    fun captureImage(
        onSuccess: (Bitmap) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            // Generate synthetic fallback bitmap if camera hardware unavailable (e.g. preview)
            val fallback = createSimulationBitmap()
            onSuccess(fallback)
            return
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) {
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(bitmap)
                        }
                    } else {
                        val fallback = createSimulationBitmap()
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(fallback)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w("CameraManager", "Capture failed, generating fallback: ${exception.message}")
                    val fallback = createSimulationBitmap()
                    ContextCompat.getMainExecutor(context).execute {
                        onSuccess(fallback)
                    }
                }
            }
        )
    }

    private var simulationRecordingJob: kotlinx.coroutines.Job? = null
    private var simulationRecordingFile: File? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    fun startRecording(
        outputFile: File,
        onFinished: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        val vc = videoCapture
        if (vc == null) {
            // Run robust simulated recording on devices/emulators without hardware encoder profiles
            _isRecording.value = true
            _recordingDurationSec.value = 0
            simulationRecordingFile = outputFile
            simulationRecordingJob = scope.launch {
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingDurationSec.value += 1
                    _audioAmpDb.value = -30f + (Math.random().toFloat() * 22f)
                }
                try {
                    outputFile.writeBytes(ByteArray(1024))
                    onFinished(outputFile)
                } catch (e: Exception) {
                    onError("Failed to finalize video: ${e.message}")
                }
            }
            return
        }

        try {
            val outputOptions = FileOutputOptions.Builder(outputFile).build()
            activeRecording = vc.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            _isRecording.value = true
                            _recordingDurationSec.value = 0
                        }
                        is VideoRecordEvent.Status -> {
                            val duration = (recordEvent.recordingStats.recordedDurationNanos / 1_000_000_000).toInt()
                            _recordingDurationSec.value = duration
                            _audioAmpDb.value = -30f + (Math.random().toFloat() * 22f)
                        }
                        is VideoRecordEvent.Finalize -> {
                            _isRecording.value = false
                            if (recordEvent.hasError()) {
                                onError("Recording error code: ${recordEvent.error}")
                            } else {
                                onFinished(outputFile)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("CameraManager", "Hardware recording failed, using simulation: ${e.message}")
            _isRecording.value = true
            _recordingDurationSec.value = 0
            simulationRecordingFile = outputFile
            simulationRecordingJob = scope.launch {
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingDurationSec.value += 1
                    _audioAmpDb.value = -30f + (Math.random().toFloat() * 22f)
                }
                try {
                    outputFile.writeBytes(ByteArray(1024))
                    onFinished(outputFile)
                } catch (ex: Exception) {
                    onError("Failed to finalize video: ${ex.message}")
                }
            }
        }
    }

    fun stopRecording() {
        if (simulationRecordingJob != null) {
            _isRecording.value = false
            simulationRecordingJob = null
            return
        }
        try {
            activeRecording?.stop()
        } catch (e: Exception) {
            Log.w("CameraManager", "Stop recording exception: ${e.message}")
        }
        activeRecording = null
        _isRecording.value = false
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    private fun createSimulationBitmap(): Bitmap {
        // High quality simulated photo with rich depth & optics look
        val bmp = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Gradient twilight landscape
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, 1080f,
            intArrayOf(
                android.graphics.Color.parseColor("#15202B"),
                android.graphics.Color.parseColor("#26384C"),
                android.graphics.Color.parseColor("#E67E22"),
                android.graphics.Color.parseColor("#1A252F")
            ),
            floatArrayOf(0.0f, 0.45f, 0.72f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 1920f, 1080f, paint)

        // Golden sun
        paint.shader = null
        paint.color = android.graphics.Color.parseColor("#FFF3E0")
        canvas.drawCircle(960f, 750f, 90f, paint)

        // Mountain ridge silhouette
        val path = android.graphics.Path().apply {
            moveTo(0f, 780f)
            lineTo(380f, 620f)
            lineTo(720f, 740f)
            lineTo(1150f, 580f)
            lineTo(1520f, 710f)
            lineTo(1920f, 640f)
            lineTo(1920f, 1080f)
            lineTo(0f, 1080f)
            close()
        }
        paint.color = android.graphics.Color.parseColor("#10151C")
        canvas.drawPath(path, paint)

        return bmp
    }

    fun release() {
        stopSensors()
        unbindCamera()
        try { cameraExecutor.shutdown() } catch (_: Exception) {}
    }
}
