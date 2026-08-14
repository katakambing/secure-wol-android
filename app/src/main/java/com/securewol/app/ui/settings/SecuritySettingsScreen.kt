package com.securewol.app.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securewol.app.data.model.AutoLockTimeout
import com.securewol.app.ui.theme.AccentAmber
import com.securewol.app.ui.theme.AccentCrimson
import com.securewol.app.ui.theme.AccentEmerald
import com.securewol.app.ui.theme.BgDark
import com.securewol.app.ui.theme.SurfaceCard
import com.securewol.app.ui.theme.SurfaceCardBorder
import com.securewol.app.ui.theme.SurfaceDark
import com.securewol.app.ui.theme.TextMuted
import com.securewol.app.ui.theme.TextPrimary
import com.securewol.app.ui.theme.TextSecondary

@Composable
fun SecuritySettingsScreen(
    viewModel: SecuritySettingsViewModel,
    onNavigateBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onDeviceWiped: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val deviceId by viewModel.deviceId.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showReRegisterDialog by remember { mutableStateOf(false) }
    var showTimeoutMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.DeviceWiped -> {
                    onDeviceWiped()
                }
                is SettingsEvent.SessionExpired -> {
                    onSessionExpired()
                }
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Security Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Authentication & Protection
            Text(
                text = "AUTHENTICATION & LOCKOUT",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            // Biometric Authentication Switch
            SettingToggleItem(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Authentication",
                subtitle = "Require fingerprint/face on startup",
                checked = settings.isBiometricEnabled,
                onCheckedChange = { viewModel.updateBiometric(it) }
            )

            // Auto-Lock Timeout Picker
            Box {
                SettingActionItem(
                    icon = Icons.Default.Timer,
                    title = "Auto-Lock",
                    subtitle = "Lock app when leaving foreground",
                    trailingText = settings.autoLockTimeout.displayName,
                    onClick = { showTimeoutMenu = true }
                )

                DropdownMenu(
                    expanded = showTimeoutMenu,
                    onDismissRequest = { showTimeoutMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    AutoLockTimeout.entries.forEach { timeoutOption ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = timeoutOption.displayName,
                                    color = if (timeoutOption == settings.autoLockTimeout) AccentEmerald else TextPrimary
                                )
                            },
                            onClick = {
                                viewModel.updateAutoLockTimeout(timeoutOption)
                                showTimeoutMenu = false
                            }
                        )
                    }
                }
            }

            // Require Auth Before Power-On Switch
            SettingToggleItem(
                icon = Icons.Default.PowerSettingsNew,
                title = "Require Auth Before Power On",
                subtitle = "Secondary verification when sending WoL",
                checked = settings.requireAuthBeforePowerOn,
                onCheckedChange = { viewModel.updateRequireAuthBeforePowerOn(it) }
            )

            // Failed Attempt Protection (Always Active / Monitored)
            SettingActionItem(
                icon = Icons.Default.LockClock,
                title = "Failed Attempt Protection",
                subtitle = "Progressive delays (30s, 2m, 5m)",
                trailingText = "Active",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Section 2: Device Binding
            Text(
                text = "DEVICE BINDING & IDENTIFIERS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )

            SettingActionItem(
                icon = Icons.Default.PhoneAndroid,
                title = "Device Registration",
                subtitle = "Bound UUID: ${deviceId.take(8)}...${deviceId.takeLast(4)}",
                trailingText = "Active",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section 3: Action Buttons
            Button(
                onClick = { viewModel.lockAppNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCard,
                    contentColor = AccentAmber
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock App Now", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { showChangePinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCard,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
            ) {
                Icon(imageVector = Icons.Default.Key, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Owner App PIN", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { showReRegisterDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCrimson.copy(alpha = 0.15f),
                    contentColor = AccentCrimson
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCrimson)
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-register Device (Wipe All)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Principle & Disclaimer (Req 17)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "The application uses Android device authentication, secure local storage, device binding, and authorization checks to restrict access to the configured Wake-on-LAN functionality. Physical compromise of a fully unlocked device or a compromised/rooted Android operating system cannot be completely prevented by a normal Android application.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // Change PIN Modal
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onSubmit = { oldPin, newPin, errCb ->
                viewModel.changePin(
                    oldPin = oldPin,
                    newPin = newPin,
                    onSuccess = { showChangePinDialog = false },
                    onError = errCb
                )
            }
        )
    }

    // Re-Register Device Confirmation Modal (Req 15)
    if (showReRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showReRegisterDialog = false },
            containerColor = SurfaceDark,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AccentCrimson,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Re-register Device?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will immediately invalidate the current device binding and wipe all cryptographic keys and configured PCs from local storage. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReRegisterDialog = false
                        viewModel.reRegisterDevice()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCrimson,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Wipe & Re-register", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReRegisterDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentEmerald,
                    checkedTrackColor = AccentEmerald.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceDark
                )
            )
        }
    }
}

@Composable
fun SettingActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceCard)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = AccentEmerald,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPin: String, newPin: String, onError: (String) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentEmerald,
        unfocusedBorderColor = SurfaceCardBorder,
        focusedLabelColor = AccentEmerald,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Change Owner App PIN", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AccentCrimson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN (6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = tfColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(oldPin, newPin) { err ->
                        errorMessage = err
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentEmerald,
                    contentColor = BgDark
                )
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                Text("Cancel")
            }
        }
    )
}
