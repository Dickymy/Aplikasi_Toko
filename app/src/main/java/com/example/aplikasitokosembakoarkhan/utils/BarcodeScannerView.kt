package com.example.aplikasitokosembakoarkhan.utils

import android.graphics.Rect
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // --- STATES ---
    var isReadyToScan by remember { mutableStateOf(false) }
    var isSuccessVisual by remember { mutableStateOf(false) }

    // State Pencegah Double
    var lastScannedCode by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Camera & Focus State
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    // INDIKATOR FOKUS MANUAL (TITIK SENTUH)
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var isFocusingManual by remember { mutableStateOf(false) }

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }

    // Delay awal agar kamera stabil
    LaunchedEffect(Unit) {
        delay(1000)
        isReadyToScan = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. KAMERA PREVIEW
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // VISUAL: Tampilkan lingkaran fokus di titik sentuh
                            focusTapOffset = offset
                            isFocusingManual = true

                            // LOGIKA KAMERA: Fokus ke titik tersebut
                            val factory = SurfaceOrientedMeteringPointFactory(
                                size.width.toFloat(), size.height.toFloat()
                            )
                            val point = factory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS) // Auto batal fokus manual setelah 2 detik
                                .build()

                            cameraControl?.startFocusAndMetering(action)

                            // Hilangkan indikator fokus visual setelah 1.5 detik
                            scope.launch {
                                delay(1500)
                                focusTapOffset = null
                                isFocusingManual = false
                            }
                        }
                    )
                },
            update = { previewView ->
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(1280, 720))
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isReadyToScan || isProcessing) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val scanner = BarcodeScanning.getClient()

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val detectedBarcode = barcodes.firstOrNull()

                                    if (detectedBarcode != null && detectedBarcode.rawValue != null) {
                                        val code = detectedBarcode.rawValue!!
                                        val bbox = detectedBarcode.boundingBox

                                        // VALIDASI KOTAK (ROI)
                                        val imgWidth = imageProxy.width
                                        val imgHeight = imageProxy.height
                                        val scanAreaRect = Rect(
                                            (imgWidth * 0.25).toInt(), (imgHeight * 0.35).toInt(),
                                            (imgWidth * 0.75).toInt(), (imgHeight * 0.65).toInt()
                                        )

                                        if (bbox != null && scanAreaRect.contains(bbox.centerX(), bbox.centerY())) {
                                            if (code != lastScannedCode) {
                                                isProcessing = true
                                                lastScannedCode = code

                                                scope.launch {
                                                    isSuccessVisual = true
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)
                                                    onBarcodeScanned(code)

                                                    delay(300)
                                                    isSuccessVisual = false

                                                    delay(1500) // Cooldown
                                                    isProcessing = false
                                                    // lastScannedCode = "" // Uncomment jika ingin scan barang sama berkali-kali
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        cameraControl = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2. OVERLAY GRAFIS (KOTAK SCAN)
        ScannerOverlay(isReady = isReadyToScan, isSuccess = isSuccessVisual, isFocusing = isFocusingManual)

        // 3. FOCUS RING ANIMATION (JIKA DI-TAP)
        focusTapOffset?.let { offset ->
            FocusRing(offset = offset)
        }

        // 4. KONTROL MANUAL
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FloatingActionButton(
                onClick = {
                    isTorchOn = !isTorchOn
                    cameraControl?.enableTorch(isTorchOn)
                },
                containerColor = if (isTorchOn) Color.Yellow else Color.White,
                contentColor = if (isTorchOn) Color.Black else Color.Gray,
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Senter")
            }
        }
    }
}

// --- KOMPONEN LINGKARAN FOKUS (TARGET) ---
@Composable
fun FocusRing(offset: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "focus")
    // Animasi mengecil (seperti mengunci fokus)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "focusScale"
    )
    // Animasi Opacity
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "focusAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 40.dp.toPx() * scale, // Mengecil
            center = offset,
            style = Stroke(width = 2.dp.toPx())
        )
        // Titik tengah
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 4.dp.toPx(),
            center = offset
        )
    }
}

@Composable
fun ScannerOverlay(isReady: Boolean, isSuccess: Boolean, isFocusing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val value by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "line"
    )

    val boxColor by animateColorAsState(
        targetValue = if (isSuccess) Color.Green else if (isReady) Color.White else Color.Red,
        label = "boxColor"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = 250.dp.toPx()
            val left = (size.width - boxSize) / 2
            val top = (size.height - boxSize) / 2

            drawRect(Color.Black.copy(alpha = 0.5f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top), size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = boxColor,
                topLeft = Offset(left, top), size = Size(boxSize, boxSize),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )

            if (isReady && !isSuccess) {
                drawLine(
                    color = Color.Red,
                    start = Offset(left, top + (boxSize * value)),
                    end = Offset(left + boxSize, top + (boxSize * value)),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Teks Status
        if (!isReady) {
            StatusBadge("Menyiapkan Kamera...", Color.Red)
        } else if (isSuccess) {
            StatusBadge("BERHASIL!", Color.Green)
        } else if (isFocusing) {
            // STATUS BARU: MUNCUL SAAT DI-TAP
            StatusBadge("Memfokuskan...", Color.Yellow)
        } else {
            Box(modifier = Modifier.padding(top = 280.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(12.dp)) {
                Text("Sentuh Barang untuk Fokus", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 180.dp)
            .background(color.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if(color == Color.Yellow) Color.Black else Color.White, style = MaterialTheme.typography.labelLarge)
    }
}