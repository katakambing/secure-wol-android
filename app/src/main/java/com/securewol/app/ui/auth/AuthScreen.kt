package com.securewol.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.securewol.app.ui.setup.KeypadCircleButton
import com.securewol.app.ui.theme.AccentAmber
import com.securewol.app.ui.theme.AccentCrimson
import com.securewol.app.ui.theme.AccentEmerald
import com.securewol.app.ui.theme.BgDark
import com.securewol.app.ui.theme.SurfaceCard
import com.securewol.app.ui.theme.SurfaceCardBorder
import com.securewol.app.ui.theme.TextMuted
import com.securewol.app.ui.theme.TextPrimary
import com.securewol.app.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var enteredDigits by remember { mutableStateOf("") }

    // Auto-prompt biometrics on screen entrance if not locked out
    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity
        if (activity != null && uiState !is AuthUiState.LockedOut) {
            viewModel.triggerBiometricAuth(activity)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthSuccess()
        }
    }

    val isLockedOut = uiState is AuthUiState.LockedOut
    val lockoutSeconds = (uiState as? AuthUiState.LockedOut)?.remainingSeconds ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isLockedOut) AccentCrimson.copy(alpha = 0.15f) else SurfaceCard)
                    .border(
                        1.dp,
                        if (isLockedOut) AccentCrimson else SurfaceCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLockedOut) Icons.Default.Warning else Icons.Default.Lock,
                    contentDescription = "Lock Status",
                    tint = if (isLockedOut) AccentCrimson else AccentEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Authenticate to continue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isLockedOut) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentCrimson.copy(alpha = 0.2f))
                        .border(1.dp, AccentCrimson, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Temporary Lockout: Try again in ${lockoutSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentCrimson,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val errorMsg = (uiState as? AuthUiState.Error)?.message
                Text(
                    text = errorMsg ?: "Scan fingerprint or enter owner PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMsg != null) AccentCrimson else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredDigits.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLockedOut -> TextMuted
                                    isFilled -> AccentEmerald
                                    else -> SurfaceCard
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isLockedOut -> TextMuted
                                    isFilled -> AccentEmerald
                                    else -> SurfaceCardBorder
                                },
                                CircleShape
                            )
                    )
                }
            }
        }

        // Keypad Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        when (key) {
                            "BIO" -> {
                                KeypadCircleButton(onClick = {
                                    if (!isLockedOut) {
                                        val activity = context as? FragmentActivity
                                        if (activity != null) {
                                            viewModel.triggerBiometricAuth(activity)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Scan",
                                        tint = if (isLockedOut) TextMuted else AccentEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            "DEL" -> {
                                KeypadCircleButton(onClick = {
                                    if (!isLockedOut && enteredDigits.isNotEmpty()) {
                                        enteredDigits = enteredDigits.dropLast(1)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = if (isLockedOut) TextMuted else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> {
                                KeypadCircleButton(onClick = {
                                    if (!isLockedOut && enteredDigits.length < 6) {
                                        enteredDigits += key
                                        if (enteredDigits.length == 6) {
                                            val pin = enteredDigits
                                            enteredDigits = ""
                                            viewModel.submitPin(pin)
                                        }
                                    }
                                }) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLockedOut) TextMuted else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Protected by progressive lockout & hardware keystore",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
