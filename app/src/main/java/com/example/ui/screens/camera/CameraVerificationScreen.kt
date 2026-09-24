package com.example.ui.screens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.di.AppContainer
import com.example.feature.verification.model.VerificationInputType
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraVerificationScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit,
    onVerificationStarted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(container.permissionManager.hasCameraPermission())
    }

    var cameraSelector by remember {
        mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    var flashMode by remember {
        mutableIntStateOf(ImageCapture.FLASH_MODE_OFF)
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var qualityWarning by remember { mutableStateOf<String?>(null) }
    var capturedBitmapForReview by remember { mutableStateOf<Bitmap?>(null) }

    // Runtime Permission Request
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedView(
            onRequestPermission = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // Viewfinder Framing Guide
        ViewfinderFramingOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp)
        )

        // Top Controls: Back, Flash, Switch Camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(Navy900.copy(alpha = 0.7f), CircleShape)
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row {
                // Flash Toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .background(Navy900.copy(alpha = 0.7f), CircleShape)
                        .testTag("camera_flash_toggle")
                ) {
                    val (flashIcon, flashDesc) = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to "Flash On"
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to "Flash Auto"
                        else -> Icons.Default.FlashOff to "Flash Off"
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = flashDesc,
                        tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) CyanPrimary else Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Camera Switch (Front/Back)
                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .background(Navy900.copy(alpha = 0.7f), CircleShape)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Capture Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Navy900.copy(alpha = 0.85f))
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Align claim, newspaper, or screen inside the frame",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, CyanPrimary, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) CyanPrimary.copy(alpha = 0.5f) else Color.White)
                        .testTag("camera_capture_button"),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isCapturing) return@IconButton
                            val capture = imageCapture ?: return@IconButton
                            isCapturing = true

                            val executor = Executors.newSingleThreadExecutor()
                            capture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(imageProxy)
                                        imageProxy.close()

                                        if (bitmap != null) {
                                            // Check quality
                                            val quality = checkQuickQuality(bitmap)
                                            if (quality != null) {
                                                qualityWarning = quality
                                                capturedBitmapForReview = bitmap
                                                isCapturing = false
                                            } else {
                                                // Directly proceed to pipeline
                                                coroutineScope.launch {
                                                    val historyId = container.verificationPipeline.verifyScreenshotInput(
                                                        imageUri = null,
                                                        providedBitmap = bitmap,
                                                        inputType = VerificationInputType.CAMERA
                                                    )
                                                    isCapturing = false
                                                    onVerificationStarted(historyId)
                                                }
                                            }
                                        } else {
                                            isCapturing = false
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        isCapturing = false
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = CyanPrimary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }
        }

        // Quality Warning Dialog Overlay
        if (qualityWarning != null && capturedBitmapForReview != null) {
            QualityWarningDialog(
                warningMessage = qualityWarning ?: "Image may be difficult to read.",
                onRetake = {
                    qualityWarning = null
                    capturedBitmapForReview = null
                },
                onUseAnyway = {
                    val bmp = capturedBitmapForReview
                    qualityWarning = null
                    capturedBitmapForReview = null
                    if (bmp != null) {
                        isCapturing = true
                        coroutineScope.launch {
                            val historyId = container.verificationPipeline.verifyScreenshotInput(
                                imageUri = null,
                                providedBitmap = bmp,
                                inputType = VerificationInputType.CAMERA
                            )
                            isCapturing = false
                            onVerificationStarted(historyId)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ViewfinderFramingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 310.dp, height = 380.dp)
                .border(BorderStroke(2.dp, CyanPrimary.copy(alpha = 0.8f)), RoundedCornerShape(20.dp))
                .testTag("camera_viewfinder_frame")
        )
    }
}

@Composable
fun QualityWarningDialog(
    warningMessage: String,
    onRetake: () -> Unit,
    onUseAnyway: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Navy800,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("camera_quality_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Quality Notice",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Image Quality Advisory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = warningMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f).testTag("camera_retake_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retake", color = Color.White)
                    }
                    Button(
                        onClick = onUseAnyway,
                        modifier = Modifier.weight(1f).testTag("camera_use_anyway_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Use Anyway", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Permission Denied",
                tint = CyanPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "VeriLens AI needs camera access to capture and verify physical news headlines, posters, and printed documents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().testTag("cancel_camera_permission_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Home")
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    // Rotate if necessary
    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

private fun checkQuickQuality(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    if (width < 200 || height < 200) {
        return "Image resolution is very low and may reduce OCR accuracy."
    }

    var totalLuminance = 0.0
    var count = 0
    for (x in 0 until width step 40) {
        for (y in 0 until height step 40) {
            val pixel = bitmap.getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            totalLuminance += lum
            count++
        }
    }

    val avgLum = if (count > 0) totalLuminance / count else 128.0
    if (avgLum < 25.0) return "Image is too dark. Turn on flash or find better lighting."
    if (avgLum > 245.0) return "Image is overexposed / too bright."

    return null
}
