package com.example.aplikasitokosembakoarkhan

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper

@Composable
fun PinLockScreen(
    onUnlock: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }

    // Ambil panjang PIN yang benar dari database (4, 5, atau 6)
    val targetLength = remember { SecurityHelper.getPinLength(context) }

    // Fungsi Cek PIN
    fun checkPin(pin: String) {
        if (pin.length == targetLength) {
            if (SecurityHelper.checkPin(context, pin)) {
                onUnlock()
            } else {
                Toast.makeText(context, "PIN Salah!", Toast.LENGTH_SHORT).show()
                inputPin = "" // Reset jika salah
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Tombol Tutup (X)
        IconButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "Batal", tint = Color.Gray)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Butuh Akses Admin", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Masukkan PIN ($targetLength Angka)", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // Indikator Titik PIN (Dinamis sesuai panjang PIN)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(targetLength) { index ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (index < inputPin.length) MaterialTheme.colorScheme.primary else Color.LightGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Tombol Angka (Numpad)
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "del")
            )

            numbers.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { num ->
                        if (num.isEmpty()) {
                            Spacer(modifier = Modifier.size(80.dp))
                        } else if (num == "del") {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .clickable { if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = Color.Gray)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, Color.LightGray, CircleShape)
                                    .clickable {
                                        // Cek apakah belum mencapai batas panjang PIN
                                        if (inputPin.length < targetLength) {
                                            val newPin = inputPin + num
                                            inputPin = newPin
                                            // Jika panjang sudah pas, langsung cek
                                            if (newPin.length == targetLength) {
                                                checkPin(newPin)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(num, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}