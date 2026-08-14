package com.securewol.app.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.securewol.app.core.network.PcPowerStatus
import com.securewol.app.core.network.RemotePowerAction
import com.securewol.app.data.model.PcDevice
import com.securewol.app.ui.theme.AccentAmber
import com.securewol.app.ui.theme.AccentCrimson
import com.securewol.app.ui.theme.AccentCyan
import com.securewol.app.ui.theme.AccentEmerald
import com.securewol.app.ui.theme.AccentIndigo
import com.securewol.app.ui.theme.BgDark
import com.securewol.app.ui.theme.SurfaceCard
import com.securewol.app.ui.theme.SurfaceCardBorder
import com.securewol.app.ui.theme.SurfaceCardElevated
import com.securewol.app.ui.theme.SurfaceDark
import com.securewol.app.ui.theme.TextMuted
import com.securewol.app.ui.theme.TextPrimary
import com.securewol.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddPc: () -> Unit,
    onNavigateToEditPc: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val pcList by viewModel.pcList.collectAsState()
    val pendingPc by viewModel.pendingPowerOnPc.collectAsState()
    val isSendingWol by viewModel.isSendingWol.collectAsState()
    val pcStatusMap by viewModel.pcStatusMap.collectAsState()
    val pendingRemoteAction by viewModel.pendingRemoteAction.collectAsState()
    val agentSetupPc by viewModel.showAgentSetupDialog.collectAsState()
    var pcToDelete by remember { mutableStateOf<PcDevice?>(null) }
    var isPrivacyModeEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DashboardEvent.SessionExpired -> {
                    onLockApp()
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.15f))
                            .border(1.dp, AccentEmerald.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SECURE WOL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isPrivacyModeEnabled) "Shield Active • Data Masked" else "Unmasked View • Local Only",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isPrivacyModeEnabled) AccentEmerald else AccentAmber
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { isPrivacyModeEnabled = !isPrivacyModeEnabled }) {
                        Icon(
                            imageVector = if (isPrivacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Privacy Shield",
                            tint = if (isPrivacyModeEnabled) AccentEmerald else TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Settings",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.lockAppNow() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock App Now",
                            tint = AccentAmber
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPc,
                containerColor = AccentEmerald,
                contentColor = BgDark,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add PC", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pcList.isEmpty()) {
                EmptyPcState(onAddPc = onNavigateToAddPc)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Top Status Bar Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SurfaceCardElevated, SurfaceDark)
                                    )
                                )
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PulsingLiveDot()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPrivacyModeEnabled) "ZERO-TRUST SHIELD ACTIVE" else "LIVE NETWORK MONITORING",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${pcList.size} PC Guarded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    items(pcList, key = { it.id }) { pc ->
                        val status = pcStatusMap[pc.id] ?: PcPowerStatus.CHECKING
                        PcDeviceCard(
                            pc = pc,
                            status = status,
                            isPrivacyMode = isPrivacyModeEnabled,
                            onPowerOn = { viewModel.onPowerOnClicked(pc) },
                            onEdit = { onNavigateToEditPc(pc.id) },
                            onDelete = { pcToDelete = pc },
                            onRemoteAction = { action -> viewModel.onRemoteActionClicked(pc, action) }
                        )
                    }
                }
            }

            // Power-On Confirmation Modal (Req 9)
            if (pendingPc != null) {
                val target = pendingPc!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissPowerOnConfirmation() },
                    containerColor = SurfaceDark,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Power On PC?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Send authenticated Wake-on-LAN Magic Packet to wake up:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = target.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "MAC: ${target.maskedMac()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = AccentEmerald
                                    )
                                    Text(
                                        text = "Subnet: ${target.broadcastAddress}:${target.port}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.confirmPowerOn(context as? FragmentActivity)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEmerald,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚡ POWER ON", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.dismissPowerOnConfirmation() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Remote Action Confirmation Modal (Sleep / Restart / Shutdown)
            if (pendingRemoteAction != null) {
                val pair = pendingRemoteAction!!
                val targetPc = pair.first
                val action = pair.second
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRemoteAction() },
                    containerColor = SurfaceDark,
                    title = {
                        Text(
                            text = "${action.displayName} ${targetPc.name}?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Send authenticated ${action.displayName.lowercase()} signal to ${targetPc.name} (${targetPc.ipAddress})?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.confirmRemoteAction() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (action == RemotePowerAction.SHUTDOWN) AccentCrimson else AccentAmber,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(action.displayName, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.dismissRemoteAction() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Companion Agent Setup Guide Modal (If PC Companion is not running)
            if (agentSetupPc != null) {
                val pc = agentSetupPc!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissAgentSetupDialog() },
                    containerColor = SurfaceDark,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "PC Companion Agent Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "To use Sleep, Restart, and Shut Down from your phone, the companion receiver must be running on your PC:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "1. Open on PC:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    )
                                    Text(
                                        text = "SecureWolApp\\windows-agent",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "2. Double-click: start-agent.bat",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissAgentSetupDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentEmerald,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Got It", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Delete Confirmation Modal
            if (pcToDelete != null) {
                val target = pcToDelete!!
                AlertDialog(
                    onDismissRequest = { pcToDelete = null },
                    containerColor = SurfaceDark,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = AccentCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Remove PC?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to remove \"${target.name}\" (${target.maskedMac()}) from your configured PCs?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deletePc(target)
                                pcToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCrimson,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { pcToDelete = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PcDeviceCard(
    pc: PcDevice,
    status: PcPowerStatus,
    isPrivacyMode: Boolean = true,
    onPowerOn: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRemoteAction: (RemotePowerAction) -> Unit
) {
    val isOnline = status == PcPowerStatus.ONLINE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SurfaceCardElevated, SurfaceCard)
                )
            )
            .border(
                1.dp,
                if (isOnline) AccentEmerald.copy(alpha = 0.45f) else SurfaceCardBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Icon + Name + Online Badge + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) AccentEmerald.copy(alpha = 0.15f) else SurfaceDark)
                            .border(
                                1.dp,
                                if (isOnline) AccentEmerald.copy(alpha = 0.4f) else SurfaceCardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "PC",
                            tint = if (isOnline) AccentEmerald else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pc.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isOnline) AccentEmerald.copy(alpha = 0.15f) else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isOnline) AccentEmerald.copy(alpha = 0.3f) else SurfaceCardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) AccentEmerald else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isOnline) "ONLINE" else "ASLEEP / OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) AccentEmerald else TextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit PC",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete PC",
                            tint = AccentCrimson.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Network Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NetworkInfoChip(
                    label = "IP",
                    value = if (isPrivacyMode) pc.maskedIp() else pc.ipAddress.ifBlank { "Auto" },
                    color = AccentCyan
                )
                NetworkInfoChip(
                    label = "MAC",
                    value = if (isPrivacyMode) pc.maskedMac() else pc.macAddress,
                    color = AccentEmerald
                )
                NetworkInfoChip(
                    label = "PORT",
                    value = "${pc.port}",
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Power Control Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Main POWER ON Button (Always available for WoL)
                Button(
                    onClick = onPowerOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = BgDark
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POWER ON (WAKE-ON-LAN)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Secondary Power Action Pills (Sleep, Restart, Shutdown)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PowerPillButton(
                        text = "🌙 Sleep",
                        color = AccentIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteAction(RemotePowerAction.SLEEP) }
                    )
                    PowerPillButton(
                        text = "🔄 Restart",
                        color = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteAction(RemotePowerAction.RESTART) }
                    )
                    PowerPillButton(
                        text = "🛑 Shut Down",
                        color = AccentCrimson,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteAction(RemotePowerAction.SHUTDOWN) }
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkInfoChip(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun PowerPillButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun PulsingLiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(AccentEmerald.copy(alpha = alpha))
    )
}

@Composable
fun EmptyPcState(onAddPc: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceCardElevated)
                .border(1.dp, SurfaceCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Target PCs Configured",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add your local desktop or server to securely trigger Wake-on-LAN and remote power controls with biometric authentication.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddPc,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentEmerald,
                contentColor = BgDark
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add PC Configuration", fontWeight = FontWeight.Bold)
        }
    }
}
