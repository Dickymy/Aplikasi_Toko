package com.example.aplikasitokosembakoarkhan

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // State Animasi
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    // Efek Animasi
    LaunchedEffect(key1 = true) {
        // Animasi Membesar (Bounce)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )
        // Animasi Teks Muncul
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )
        // Lama Tampil Splash Screen (2.5 Detik)
        delay(2500L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // --- LOGO APLIKASI ---
            Surface(
                modifier = Modifier
                    .size(150.dp) // Ukuran Lingkaran
                    .scale(scale.value),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                // Memanggil gambar dari folder res/drawable
                // Pastikan nama file sudah di-rename jadi huruf kecil semua!
                Image(
                    painter = painterResource(id = R.drawable.icon_toko_sembako_arkhan),
                    contentDescription = "Logo Toko Arkhan",
                    contentScale = ContentScale.Fit, // Fit agar logo pas di dalam lingkaran
                    modifier = Modifier.padding(20.dp) // Memberi jarak agar logo tidak mepet pinggir
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TEKS JUDUL ---
            Text(
                text = "Toko Sembako\nArkhan",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Solusi Kasir Pintar & Cepat",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.alpha(alpha.value)
            )
        }

        // --- FOOTER LOADING ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Memuat Data...",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Versi 1.0.0",
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
    }
}