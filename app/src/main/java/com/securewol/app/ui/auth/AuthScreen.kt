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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.securewol.app.ui.theme.AccentEmeraldGlow
import com.securewol.app.ui.theme.BgDark
import com.securewol.app.ui.theme.SurfaceCard
import com.securewol.app.ui.theme.SurfaceCardBorder
import com.securewol.app.ui.theme.SurfaceCardElevated
import com.securewol.app.ui.theme.SurfaceDark
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

            if (isLockedOut) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(AccentCrimson.copy(alpha = 0.25f), AccentCrimson.copy(alpha = 0.05f))
                            )
                        )
                        .border(
                            1.dp,
                            AccentCrimson.copy(alpha = 0.5f),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Lock Status",
                        tint = AccentCrimson,
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.securewol.app.R.drawable.app_logo),
                    contentDescription = "Secure WOL Logo",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, AccentEmerald.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isLockedOut) "Security Lockout" else "Authenticate",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isLockedOut) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentCrimson.copy(alpha = 0.15f))
                        .border(1.dp, AccentCrimson.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Progressive Lockout: Try again in ${lockoutSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCrimson,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val errorMsg = (uiState as? AuthUiState.Error)?.message
                Text(
                    text = errorMsg ?: "Scan owner biometric or enter 6-digit PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMsg != null) AccentCrimson else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

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
                                        modifier = Modifier.size(30.dp)
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
                                        modifier = Modifier.size(22.dp)
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
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLockedOut) TextMuted else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Protected by progressive lockout & hardware keystore",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
