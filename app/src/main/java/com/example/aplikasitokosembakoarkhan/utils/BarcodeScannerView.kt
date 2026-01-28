package com.example.aplikasitokosembakoarkhan.utils

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerView(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. KAMERA PREVIEW (Full Screen)
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
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                val cameraExecutor = Executors.newSingleThreadExecutor()
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Setting akurasi tinggi
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val scanner = BarcodeScanning.getClient()

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        barcode.rawValue?.let { code ->
                                            onBarcodeDetected(code)
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
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        Log.e("BarcodeScanner", "Gagal memuat kamera", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2. OVERLAY GELAP DENGAN KOTAK PERSEGI PANJANG
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // UKURAN BARCODE (Persegi Panjang)
            val boxWidth = 320.dp.toPx()
            val boxHeight = 180.dp.toPx()

            val left = (canvasWidth - boxWidth) / 2
            val top = (canvasHeight - boxHeight) / 2

            // Gelapkan area luar
            drawRect(color = Color.Black.copy(alpha = 0.6f))

            // Bolongi area tengah (Clear)
            drawRoundRect(
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )

            // Gambar Border Hijau/Merah sebagai penanda area scan
            drawRoundRect(
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                color = Color(0xFF00E676), // Hijau Neon
                style = Stroke(width = 3.dp.toPx())
            )

            // Garis Merah di tengah (Laser style - Opsional, untuk estetika scan)
            drawLine(
                color = Color.Red.copy(alpha = 0.8f),
                start = Offset(left, top + boxHeight / 2),
                end = Offset(left + boxWidth, top + boxHeight / 2),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. TEKS PETUNJUK
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Posisikan barcode di dalam kotak",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 4. TOMBOL FLASH (POJOK KANAN ATAS)
        IconButton(
            onClick = {
                isTorchOn = !isTorchOn
                cameraControl?.enableTorch(isTorchOn)
            },
            modifier = Modifier
                .align(Alignment.TopEnd) // Tetap di Kanan Atas
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Flash",
                tint = if (isTorchOn) Color.Yellow else Color.White
            )
        }
    }
}