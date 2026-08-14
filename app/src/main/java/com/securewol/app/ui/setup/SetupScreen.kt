package com.securewol.app.ui.setup

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securewol.app.ui.theme.AccentCrimson
import com.securewol.app.ui.theme.AccentEmerald
import com.securewol.app.ui.theme.BgDark
import com.securewol.app.ui.theme.KeypadButtonBg
import com.securewol.app.ui.theme.SurfaceCard
import com.securewol.app.ui.theme.SurfaceCardBorder
import com.securewol.app.ui.theme.TextMuted
import com.securewol.app.ui.theme.TextPrimary
import com.securewol.app.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var enteredDigits by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is SetupUiState.Success) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Header & Step indicator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Shield",
                    tint = AccentEmerald,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Owner Device Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val instructionText = when (uiState) {
                is SetupUiState.EnterNewPin -> "Create a secure 6-digit Owner App PIN to bind this Android device"
                is SetupUiState.ConfirmPin -> "Confirm your 6-digit Owner App PIN"
                is SetupUiState.Error -> (uiState as SetupUiState.Error).message
                is SetupUiState.Success -> "Device registered successfully!"
            }

            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (uiState is SetupUiState.Error) AccentCrimson else TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Indicator Dots
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
                            .background(if (isFilled) AccentEmerald else SurfaceCard)
                            .border(1.dp, if (isFilled) AccentEmerald else SurfaceCardBorder, CircleShape)
                    )
                }
            }
        }

        // Middle/Bottom: Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("RESET", "0", "DEL")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        when (key) {
                            "DEL" -> {
                                KeypadCircleButton(onClick = {
                                    if (enteredDigits.isNotEmpty()) {
                                        enteredDigits = enteredDigits.dropLast(1)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Delete",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            "RESET" -> {
                                KeypadCircleButton(onClick = {
                                    enteredDigits = ""
                                    viewModel.resetToStart()
                                }) {
                                    Text(
                                        text = "CLR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            else -> {
                                KeypadCircleButton(onClick = {
                                    if (enteredDigits.length < 6) {
                                        enteredDigits += key
                                        if (enteredDigits.length == 6) {
                                            val pinToSubmit = enteredDigits
                                            enteredDigits = ""
                                            if (uiState is SetupUiState.EnterNewPin || uiState is SetupUiState.Error) {
                                                viewModel.onFirstPinEntered(pinToSubmit)
                                            } else if (uiState is SetupUiState.ConfirmPin) {
                                                viewModel.onConfirmPinEntered(pinToSubmit)
                                            }
                                        }
                                    }
                                }) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cryptographic keys are derived locally using PBKDF2 & Android Keystore",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = TextMuted
            )
        }
    }
}

@Composable
fun KeypadCircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(KeypadButtonBg)
            .border(1.dp, SurfaceCardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
