package com.malikhw.hosadvtexts

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HosAdvTexturesApp(
                onRequestShizukuPermission = { requestShizukuPermission() }
            )
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            requestPermissionLauncher.launch("moe.shizuku.manager.permission.API_V23")
        } else {
            Shizuku.requestPermission(0)
        }
    }
}

fun isXiaomiDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    return manufacturer.contains("xiaomi") ||
            brand.contains("xiaomi") ||
            brand.contains("redmi") ||
            brand.contains("poco")
}

fun isShizukuAvailable(): Boolean {
    return try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }
}

fun hasShizukuPermission(): Boolean {
    return try {
        if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (e: Exception) {
        false
    }
}

suspend fun runShizukuCommands(commands: List<String>): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            for (cmd in commands) {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                process.waitFor()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

val setupCommands = listOf(
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.cpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.gpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.advanced_visual_release 3\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.background_blur_supported true\" s16 \"/storage/emulated/0/log.txt\" i32 600"
)

val revertCommands = listOf(
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.cpulevel 0\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.gpulevel 0\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.advanced_visual_release 0\" s16 \"/storage/emulated/0/log.txt\" i32 600",
    "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.background_blur_supported false\" s16 \"/storage/emulated/0/log.txt\" i32 600"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HosAdvTexturesApp(onRequestShizukuPermission: () -> Unit) {
    val isXiaomi = remember { isXiaomiDevice() }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isXiaomi) {
                NotForYouScreen()
            } else {
                MainScreen(onRequestShizukuPermission = onRequestShizukuPermission)
            }
        }
    }
}

@Composable
fun NotForYouScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "not for you lmfao",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app is only for Xiaomi / Redmi / POCO devices.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onRequestShizukuPermission: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shizukuAvailable by remember { mutableStateOf(isShizukuAvailable()) }
    var shizukuGranted by remember { mutableStateOf(hasShizukuPermission()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }

    // Refresh shizuku state when composable is in view
    LaunchedEffect(Unit) {
        shizukuAvailable = isShizukuAvailable()
        shizukuGranted = hasShizukuPermission()
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
            title = { Text("Reboot Device") },
            text = { Text("Are you sure you want to reboot your device now?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootDialog = false
                        scope.launch {
                            runShizukuCommands(listOf("reboot"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reboot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Spacer(modifier = Modifier.height(12.dp))
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Hos Advanced Textures",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Unlock higher visual fidelity on MIUI/HyperOS",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Shizuku status card
        ShizukuStatusCard(
            available = shizukuAvailable,
            granted = shizukuGranted,
            onRequestPermission = {
                onRequestShizukuPermission()
                shizukuAvailable = isShizukuAvailable()
                shizukuGranted = hasShizukuPermission()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Status message
        statusMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusIsError)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (statusIsError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (statusIsError)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = msg,
                        color = if (statusIsError)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action buttons
        ActionCard(
            icon = Icons.Default.RocketLaunch,
            title = "Set Up",
            subtitle = "Apply max CPU/GPU level & enable advanced visuals + blur",
            buttonLabel = "Apply",
            enabled = !isLoading,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            onButtonClick = {
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    val result = runShizukuCommands(setupCommands)
                    isLoading = false
                    if (result.isSuccess) {
                        statusMessage = "Done! Changes applied. Reboot to take effect."
                        statusIsError = false
                    } else {
                        statusMessage = "Failed: ${result.exceptionOrNull()?.message}"
                        statusIsError = true
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Default.SettingsBackupRestore,
            title = "Revert",
            subtitle = "Restore all settings back to their defaults",
            buttonLabel = "Revert",
            enabled = !isLoading,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onButtonClick = {
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    val result = runShizukuCommands(revertCommands)
                    isLoading = false
                    if (result.isSuccess) {
                        statusMessage = "Done! Defaults restored. Reboot to take effect."
                        statusIsError = false
                    } else {
                        statusMessage = "Failed: ${result.exceptionOrNull()?.message}"
                        statusIsError = true
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Default.RestartAlt,
            title = "Reboot",
            subtitle = "Reboot the device to apply changes",
            buttonLabel = "Reboot",
            enabled = !isLoading,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onButtonClick = {
                showRebootDialog = true
            }
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Credits
        CreditsSection()
    }
}

@Composable
fun ShizukuStatusCard(
    available: Boolean,
    granted: Boolean,
    onRequestPermission: () -> Unit
) {
    val statusColor = when {
        granted -> MaterialTheme.colorScheme.primary
        available -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val statusText = when {
        granted -> "Shizuku permission granted ✓"
        available -> "Shizuku running — permission not granted"
        else -> "Shizuku not detected — some features may not work"
    }
    val statusIcon = when {
        granted -> Icons.Default.CheckCircle
        available -> Icons.Default.Warning
        else -> Icons.Default.ErrorOutline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Shizuku",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = statusColor
                )
            }
            if (available && !granted) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onRequestPermission) {
                    Text("Grant", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    enabled: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onButtonClick,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
fun CreditsSection() {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    HorizontalDivider(
        modifier = Modifier.padding(bottom = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )

    Text(
        text = "by MalikHw47",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = { openUrl("https://www.youtube.com/@MalikHw47") },
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "YouTube",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("YouTube", fontSize = 12.sp)
        }

        FilledTonalButton(
            onClick = { openUrl("https://discord.gg/G9bZ92eg2n") },
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = "Discord",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Discord", fontSize = 12.sp)
        }

        FilledTonalButton(
            onClick = { openUrl("https://malikhw.github.io") },
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Donate",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Donate", fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}
