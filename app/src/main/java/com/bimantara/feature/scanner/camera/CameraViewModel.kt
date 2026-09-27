package com.bimantara.feature.scanner.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.bimantara.feature.scanner.DocumentEnhancer
import com.bimantara.feature.scanner.DocumentFilter
import com.bimantara.feature.scanner.DocumentFrame
import com.bimantara.feature.scanner.DocumentFrameDetector
import com.bimantara.feature.scanner.DocumentScaleRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    // Real-time live frame detection on camera preview
    private val _liveDetectedFrame = MutableStateFlow<DocumentFrame?>(null)
    val liveDetectedFrame: StateFlow<DocumentFrame?> = _liveDetectedFrame.asStateFlow()

    private val _isFrameDetected = MutableStateFlow(false)
    val isFrameDetected: StateFlow<Boolean> = _isFrameDetected.asStateFlow()

    // Captured image & corner adjustment for perspective distortion rectification
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _capturedFrame = MutableStateFlow<DocumentFrame?>(null)
    val capturedFrame: StateFlow<DocumentFrame?> = _capturedFrame.asStateFlow()

    private val _selectedRatio = MutableStateFlow(DocumentScaleRatio.AUTO)
    val selectedRatio: StateFlow<DocumentScaleRatio> = _selectedRatio.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DocumentFilter.MAGIC_COLOR)
    val selectedFilter: StateFlow<DocumentFilter> = _selectedFilter.asStateFlow()

    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchCount = MutableStateFlow(0)
    val batchCount: StateFlow<Int> = _batchCount.asStateFlow()

    private var lastAnalysisTimestamp = 0L

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val context = getApplication<Application>()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                imageCapture = capture

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeLiveFrame(imageProxy, previewView.width, previewView.height)
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(_lensFacing.value)
                    .build()

                provider.unbindAll()
                val camera: Camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture,
                    analysis
                )

                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                _isCameraReady.value = true
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Camera binding failed", e)
                _isCameraReady.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbindCamera() {
        try {
            cameraProvider?.unbindAll()
            cameraControl = null
            cameraInfo = null
            _isCameraReady.value = false
            _liveDetectedFrame.value = null
            _isFrameDetected.value = false
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error unbinding camera", e)
        }
    }

    private fun analyzeLiveFrame(imageProxy: ImageProxy, viewWidth: Int, viewHeight: Int) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalysisTimestamp < 250L) {
            imageProxy.close()
            return
        }
        lastAnalysisTimestamp = currentTimestamp

        try {
            val bitmap = imageProxy.toBitmap()
            if (bitmap != null) {
                val detected = DocumentFrameDetector.detectFrame(bitmap)
                _liveDetectedFrame.value = detected
                _isFrameDetected.value = true
            }
        } catch (e: Exception) {
            // Ignore frame analysis errors
        } finally {
            imageProxy.close()
        }
    }

    fun toggleTorch() {
        val current = _isTorchOn.value
        val next = !current
        cameraControl?.enableTorch(next)?.addListener({
            _isTorchOn.value = next
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera(lifecycleOwner, previewView)
    }

    fun toggleBatchMode() {
        _isBatchMode.value = !_isBatchMode.value
    }

    fun setScaleRatio(ratio: DocumentScaleRatio) {
        _selectedRatio.value = ratio
    }

    fun setFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
    }

    fun focusAt(x: Float, y: Float, previewView: PreviewView) {
        try {
            val factory = SurfaceOrientedMeteringPointFactory(
                previewView.width.toFloat(),
                previewView.height.toFloat()
            )
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point).build()
            cameraControl?.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Tap to focus failed", e)
        }
    }

    fun takePhoto(
        context: Context,
        onSuccess: (Bitmap, DocumentFrame) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Kamera belum siap")
            return
        }

        _isCapturing.value = true
        val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val bitmap = decodeRotatedBitmap(photoFile.absolutePath)
                            if (bitmap != null) {
                                val detectedFrame = DocumentFrameDetector.detectFrame(bitmap)
                                withContext(Dispatchers.Main) {
                                    _capturedBitmap.value = bitmap
                                    _capturedFrame.value = detectedFrame
                                    _isCapturing.value = false
                                    if (_isBatchMode.value) {
                                        _batchCount.value += 1
                                    }
                                    onSuccess(bitmap, detectedFrame)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    _isCapturing.value = false
                                    onError("Gagal mendekode gambar yang diambil")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("CameraViewModel", "Error processing captured photo", e)
                            withContext(Dispatchers.Main) {
                                _isCapturing.value = false
                                onError(e.message ?: "Gagal memproses gambar")
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _isCapturing.value = false
                    Log.e("CameraViewModel", "Photo capture failed", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception.message ?: "Pengambilan foto gagal")
                    }
                }
            }
        )
    }

    private fun decodeRotatedBitmap(path: String): Bitmap? {
        val original = BitmapFactory.decodeFile(path) ?: return null
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return original
            }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } catch (e: Exception) {
            original
        }
    }

    fun setCapturedBitmap(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
        _capturedFrame.value = DocumentFrameDetector.detectFrame(bitmap)
    }

    fun updateCorner(cornerIndex: Int, newX: Float, newY: Float) {
        val currentFrame = _capturedFrame.value ?: return
        _capturedFrame.value = currentFrame.withCorner(cornerIndex, newX, newY)
    }

    fun resetCornersToFull() {
        val bitmap = _capturedBitmap.value ?: return
        val w = bitmap.width
        val h = bitmap.height
        _capturedFrame.value = DocumentFrame(
            topLeft = android.graphics.PointF(w * 0.05f, h * 0.05f),
            topRight = android.graphics.PointF(w * 0.95f, h * 0.05f),
            bottomRight = android.graphics.PointF(w * 0.95f, h * 0.95f),
            bottomLeft = android.graphics.PointF(w * 0.05f, h * 0.95f),
            originalWidth = w,
            originalHeight = h
        )
    }

    fun rectifyPerspective(): Bitmap? {
        val bitmap = _capturedBitmap.value ?: return null
        val frame = _capturedFrame.value ?: return null
        val ratio = _selectedRatio.value
        val filter = _selectedFilter.value

        val straightened = DocumentFrameDetector.straightenAndScale(bitmap, frame, ratio)
        return DocumentEnhancer.applyFilter(straightened, filter)
    }

    fun clearCapturedState() {
        _capturedBitmap.value = null
        _capturedFrame.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}
