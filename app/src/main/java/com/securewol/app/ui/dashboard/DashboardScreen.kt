package com.securewol.app.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.securewol.app.R
import com.securewol.app.core.network.PcPowerStatus
import com.securewol.app.core.network.RemotePowerAction
import com.securewol.app.data.model.PcDevice
import com.securewol.app.ui.theme.AccentAmber
import com.securewol.app.ui.theme.AccentCrimson
import com.securewol.app.ui.theme.AccentCyan
import com.securewol.app.ui.theme.AccentEmerald
import com.securewol.app.ui.theme.AccentEmeraldGlow
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
        viewModel.refreshDevices()
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
            // Clean, Spacious, Modern Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand with Real Logo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Secure WOL Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, AccentEmerald.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Secure WOL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = TextPrimary
                        )
                    }

                    // Right Actions (Balanced & Spacious)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Privacy Eye Mask Toggle Button
                        IconButton(
                            onClick = { isPrivacyModeEnabled = !isPrivacyModeEnabled },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCard)
                                .border(1.dp, SurfaceCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isPrivacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Privacy Mask",
                                tint = if (isPrivacyModeEnabled) AccentEmerald else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Instant Lock Button
                        IconButton(
                            onClick = { viewModel.lockAppNow() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCard)
                                .border(1.dp, AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock App Now",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Clean Bottom Navigation Bar
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Stay on Devices */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Devices",
                            tint = AccentEmerald
                        )
                    },
                    label = {
                        Text(
                            text = "Devices",
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AccentEmerald.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToSettings,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Settings",
                            tint = TextMuted
                        )
                    },
                    label = {
                        Text(
                            text = "Security",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPc,
                containerColor = AccentEmerald,
                contentColor = BgDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AccentEmeraldGlow)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add PC",
                    modifier = Modifier.size(28.dp)
                )
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Clean Header Subtitle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONFIGURED DEVICES (${pcList.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = TextMuted
                            )

                            Text(
                                text = if (isPrivacyModeEnabled) "Data Masked 🔒" else "Unmasked View 👁️",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPrivacyModeEnabled) AccentEmerald else AccentAmber,
                                fontSize = 11.sp
                            )
                        }
                    }

                    items(pcList, key = { it.id }) { pc ->
                        val status = pcStatusMap[pc.id] ?: PcPowerStatus.CHECKING
                        CleanPcDeviceCard(
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

            // Power-On Confirmation Modal
            if (pendingPc != null) {
                val target = pendingPc!!
                AlertDialog(
                    onDismissRequest = { viewModel.dismissPowerOnConfirmation() },
                    containerColor = SurfaceDark,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
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
                                text = "Send Wake-on-LAN Magic Packet to wake up:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, SurfaceCardBorder, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("⚡ POWER ON", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.dismissPowerOnConfirmation() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(10.dp)
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
                            text = "Send ${action.displayName.lowercase()} signal to ${targetPc.name} (${if (isPrivacyModeEnabled) targetPc.maskedIp() else targetPc.ipAddress})?",
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(action.displayName, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { viewModel.dismissRemoteAction() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Companion Setup Modal
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
                            text = "PC Control Center Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "To use Sleep, Restart, and Shut Down from your phone, open the Control Center on your PC:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
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
                                        text = "Desktop Shortcut on PC:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    )
                                    Text(
                                        text = "Secure WOL Control Center",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
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
                            shape = RoundedCornerShape(10.dp)
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
                            text = "Are you sure you want to remove \"${target.name}\" (${target.maskedMac()})?",
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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Remove", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { pcToDelete = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Clean & Spacious PC Device Card
 */
@Composable
fun CleanPcDeviceCard(
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
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isOnline) Color(0xFF142436) else SurfaceCardElevated,
                        SurfaceCard
                    )
                )
            )
            .border(
                1.dp,
                if (isOnline) Brush.verticalGradient(
                    listOf(AccentEmerald.copy(alpha = 0.6f), AccentEmerald.copy(alpha = 0.15f))
                ) else Brush.verticalGradient(
                    listOf(SurfaceCardBorder, Color(0xFF0F1826))
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Icon + Name + Status Badge + Actions
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isOnline) AccentEmerald.copy(alpha = 0.15f) else SurfaceDark
                            )
                            .border(
                                1.dp,
                                if (isOnline) AccentEmerald.copy(alpha = 0.4f) else SurfaceCardBorder,
                                RoundedCornerShape(12.dp)
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
                        Spacer(modifier = Modifier.height(3.dp))
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isOnline) AccentEmerald.copy(alpha = 0.15f) else SurfaceDark
                                )
                                .border(
                                    1.dp,
                                    if (isOnline) AccentEmerald.copy(alpha = 0.35f) else SurfaceCardBorder,
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
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Edit & Delete Icons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit PC",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete PC",
                            tint = AccentCrimson.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CleanChip(
                    icon = Icons.Default.Wifi,
                    label = "IP",
                    value = if (isPrivacyMode) pc.maskedIp() else pc.ipAddress.ifBlank { "Auto" },
                    color = AccentCyan,
                    modifier = Modifier.weight(1.1f)
                )
                CleanChip(
                    icon = Icons.Default.Shield,
                    label = "MAC",
                    value = if (isPrivacyMode) pc.maskedMac() else pc.macAddress,
                    color = AccentEmerald,
                    modifier = Modifier.weight(1.3f)
                )
                CleanChip(
                    icon = Icons.Default.NetworkCheck,
                    label = "PORT",
                    value = "${pc.port}",
                    color = TextMuted,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Power Action Matrix
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Wake-on-LAN Button
                Button(
                    onClick = onPowerOn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = AccentEmeraldGlow
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentEmerald,
                        contentColor = BgDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POWER ON (WAKE-ON-LAN)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp
                    )
                }

                // Secondary Power Action Pills (Sleep, Restart, Shutdown)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CleanPowerPill(
                        text = "🌙 Sleep",
                        color = AccentIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteAction(RemotePowerAction.SLEEP) }
                    )
                    CleanPowerPill(
                        text = "🔄 Restart",
                        color = AccentAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onRemoteAction(RemotePowerAction.RESTART) }
                    )
                    CleanPowerPill(
                        text = "🛑 Shut Down",
                        color = AccentCrimson,
                        modifier = Modifier.weight(1.1f),
                        onClick = { onRemoteAction(RemotePowerAction.SHUTDOWN) }
                    )
                }
            }
        }
    }
}

@Composable
fun CleanChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CleanPowerPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
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
                .size(90.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        listOf(SurfaceCardElevated, SurfaceDark)
                    )
                )
                .border(1.dp, AccentEmerald.copy(alpha = 0.3f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = null,
                tint = AccentEmerald,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Guarded PCs Configured",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure your desktop or server to securely trigger Wake-on-LAN and zero-trust remote power commands with biometric hardware security.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAddPc,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentEmerald,
                contentColor = BgDark
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add PC Configuration", fontWeight = FontWeight.Bold)
        }
    }
}
