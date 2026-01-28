package com.example.aplikasitokosembakoarkhan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace // <-- Perbaikan Deprecated
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.aplikasitokosembakoarkhan.utils.SecurityHelper

@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Masukkan PIN Admin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp)
                                .background(
                                    color = if (index < pinInput.length) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PIN Salah!", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Keypad
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "del")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(80.dp))
                                } else if (key == "del") {
                                    IconButton(
                                        onClick = {
                                            if (pinInput.isNotEmpty()) {
                                                pinInput = pinInput.dropLast(1)
                                                isError = false
                                            }
                                        },
                                        modifier = Modifier.size(80.dp)
                                    ) {
                                        // Gunakan AutoMirrored.Filled.Backspace
                                        Icon(Icons.AutoMirrored.Filled.Backspace, null)
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            if (pinInput.length < 6) {
                                                pinInput += key
                                                isError = false

                                                if (pinInput.length == 6) {
                                                    // Validasi dengan SecurityHelper
                                                    if (SecurityHelper.validatePin(context, pinInput)) {
                                                        onSuccess()
                                                    } else {
                                                        isError = true
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(80.dp),
                                        shape = CircleShape
                                    ) {
                                        Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        }
    }
}