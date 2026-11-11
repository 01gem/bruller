package com.gem.bruller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gem.bruller.overlay.OverlayService
import com.gem.bruller.overlay.OverlaySettings
import com.gem.bruller.overlay.RulerColor
import com.gem.bruller.overlay.RulerStyle
import com.gem.bruller.ui.theme.BallBlack
import com.gem.bruller.ui.theme.Brass
import com.gem.bruller.ui.theme.ChalkBlue
import com.gem.bruller.ui.theme.Crimson
import com.gem.bruller.ui.theme.FeltCard
import com.gem.bruller.ui.theme.FeltDeep
import com.gem.bruller.ui.theme.FeltGreen
import com.gem.bruller.ui.theme.FeltRaised
import com.gem.bruller.ui.theme.Ivory
import com.gem.bruller.ui.theme.IvoryMuted
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(OverlaySettings.load(context)) }
    var overlayGranted by remember { mutableStateOf(AndroidSettings.canDrawOverlays(context)) }
    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val serviceRunning by OverlayService.isRunning.collectAsState()
    val overlayVisible by OverlayService.isVisible.collectAsState()
    val overlayLocked by OverlayService.isLocked.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    LifecycleResumeEffect(Unit) {
        overlayGranted = AndroidSettings.canDrawOverlays(context)
        notificationsGranted = hasNotificationPermission(context)
        settings = OverlaySettings.load(context)
        onPauseOrDispose { }
    }

    DisposableEffect(context) {
        val prefs = OverlaySettings.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            settings = OverlaySettings.load(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun updateSettings(newSettings: OverlaySettings) {
        settings = newSettings
        OverlaySettings.save(context, newSettings)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching {
                    context.startActivity(Intent(AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationsGranted = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(FeltGreen, FeltDeep, FeltDeep)))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Brass.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.02f),
                    radius = size.width * 0.9f
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.85f, size.height * 0.02f)
            )
        }

        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Header() }
                item {
                    PermissionCard(
                        overlayGranted = overlayGranted,
                        notificationsGranted = notificationsGranted,
                        onGrantOverlay = { requestOverlayPermission() },
                        onGrantNotifications = { requestNotificationPermission() }
                    )
                }
                item {
                    ActionCard(
                        overlayGranted = overlayGranted,
                        notificationsGranted = notificationsGranted,
                        serviceRunning = serviceRunning,
                        overlayVisible = overlayVisible,
                        overlayLocked = overlayLocked,
                        onToggleVisible = {
                            if (overlayVisible) {
                                OverlayService.hide(context)
                            } else if (overlayGranted) {
                                OverlayService.show(context)
                            } else {
                                requestOverlayPermission()
                            }
                        },
                        onStop = { OverlayService.stop(context) }
                    )
                }
                item {
                    SettingsCard(
                        settings = settings,
                        onChange = { updateSettings(it) }
                    )
                }
                item { HowToCard() }
                item { FairPlayNote() }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EightBallIcon(56.dp)
            Column {
                Text("Ruler", style = MaterialTheme.typography.displaySmall, color = Ivory)
                Text(
                    "FOR 8 BALL",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 4.sp),
                    color = Brass
                )
            }
        }
        Text(
            "A floating aim ruler that sits on top of your pool game. Line up the shot, lock it, and play through.",
            style = MaterialTheme.typography.bodyMedium,
            color = IvoryMuted
        )
    }
}

@Composable
private fun EightBallIcon(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, CircleShape)
            .background(
                Brush.radialGradient(listOf(Color(0xFF3D3D3D), BallBlack)),
                CircleShape
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .background(Ivory, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "8",
                color = BallBlack,
                fontSize = (size.value * 0.31f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PoolCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeltCard.copy(alpha = 0.94f),
            contentColor = Ivory
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun PermissionCard(
    overlayGranted: Boolean,
    notificationsGranted: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantNotifications: () -> Unit
) {
    PoolCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Setup", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Two permissions and you're ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryMuted
                    )
                }
                StatusPill(
                    text = if (overlayGranted && notificationsGranted) "Ready" else "Action needed",
                    color = if (overlayGranted && notificationsGranted) ChalkBlue else Brass
                )
            }
            PermissionRow(
                icon = Icons.Filled.Settings,
                title = "Display over other apps",
                subtitle = "Lets the ruler float above your game.",
                granted = overlayGranted,
                actionLabel = "Grant",
                onAction = onGrantOverlay
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            PermissionRow(
                icon = Icons.Filled.Notifications,
                title = "Overlay notification",
                subtitle = "Keeps lock and stop controls within reach.",
                granted = notificationsGranted,
                actionLabel = "Allow",
                onAction = onGrantNotifications
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (granted) ChalkBlue.copy(alpha = 0.12f) else Brass.copy(alpha = 0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) ChalkBlue else Brass,
                modifier = Modifier.size(21.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = IvoryMuted
            )
        }
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                tint = ChalkBlue,
                modifier = Modifier.size(22.dp)
            )
        } else {
            FilledTonalButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Brass.copy(alpha = 0.16f),
                    contentColor = Brass
                )
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActionCard(
    overlayGranted: Boolean,
    notificationsGranted: Boolean,
    serviceRunning: Boolean,
    overlayVisible: Boolean,
    overlayLocked: Boolean,
    onToggleVisible: () -> Unit,
    onStop: () -> Unit
) {
    PoolCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(
                            if (overlayVisible) ChalkBlue else IvoryMuted.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        !serviceRunning -> "Ruler is off"
                        !overlayVisible -> "Ruler is hidden"
                        overlayLocked -> "Ruler is locked — touches pass through"
                        else -> "Ruler is on screen"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Ivory
                )
            }

            Button(
                onClick = onToggleVisible,
                enabled = overlayGranted || overlayVisible,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (overlayVisible) FeltRaised else Brass,
                    contentColor = if (overlayVisible) Ivory else Color(0xFF21180A),
                    disabledContainerColor = FeltRaised.copy(alpha = 0.6f),
                    disabledContentColor = IvoryMuted
                )
            ) {
                Icon(
                    imageVector = if (overlayVisible) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (overlayVisible) "Hide Ruler" else "Show Ruler",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!overlayGranted) {
                Text(
                    "Grant “Display over other apps” to enable the ruler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Crimson
                )
            } else if (!notificationsGranted) {
                Text(
                    "Notifications are off. If you lock the ruler, reopen this app to stop it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brass
                )
            }

            if (serviceRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Floating service is running",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onStop) {
                        Text(
                            "Stop overlay",
                            style = MaterialTheme.typography.labelLarge,
                            color = Crimson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    settings: OverlaySettings,
    onChange: (OverlaySettings) -> Unit
) {
    PoolCard {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ruler settings", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Changes apply live to the floating ruler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IvoryMuted
                    )
                }
                TextButton(onClick = { onChange(OverlaySettings()) }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Reset", style = MaterialTheme.typography.labelLarge)
                }
            }

            RulerPreview(
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            SectionLabel("STYLE")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RulerStyle.entries.forEach { style ->
                    SettingChip(
                        selected = settings.style == style,
                        label = style.label(),
                        onClick = { onChange(settings.copy(style = style)) }
                    )
                }
            }

            SectionLabel("COLOR")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RulerColor.entries.forEach { color ->
                    val selected = settings.color == color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(color.argb))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Ivory else Color.White.copy(alpha = 0.18f),
                                shape = CircleShape
                            )
                            .clickable { onChange(settings.copy(color = color)) }
                    )
                }
            }

            SettingSlider(
                label = "Length",
                valueLabel = "${(settings.lengthFraction * 100).roundToInt()}%",
                value = settings.lengthFraction,
                range = 0.2f..0.95f,
                onChange = { onChange(settings.copy(lengthFraction = it)) }
            )
            SettingSlider(
                label = "Line thickness",
                valueLabel = "${settings.thicknessDp.roundToInt()} dp",
                value = settings.thicknessDp,
                range = 1f..8f,
                onChange = { onChange(settings.copy(thicknessDp = it)) }
            )
            SettingSlider(
                label = "Opacity",
                valueLabel = "${(settings.opacity * 100).roundToInt()}%",
                value = settings.opacity,
                range = 0.25f..1f,
                onChange = { onChange(settings.copy(opacity = it)) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

            ToggleRow(
                title = "Ruler ticks",
                subtitle = "Measuring marks along the line.",
                checked = settings.showTicks,
                onCheckedChange = { onChange(settings.copy(showTicks = it)) }
            )
            ToggleRow(
                title = "Angle readout",
                subtitle = "Degree arc and live angle label.",
                checked = settings.showAngle,
                onCheckedChange = { onChange(settings.copy(showAngle = it)) }
            )
            ToggleRow(
                title = "Ghost ball",
                subtitle = "Circle marker at the contact point.",
                checked = settings.showGhostBall,
                onCheckedChange = { onChange(settings.copy(showGhostBall = it)) }
            )

            if (settings.showGhostBall) {
                SettingSlider(
                    label = "Ghost ball size",
                    valueLabel = "${settings.ghostBallDp.roundToInt()} dp",
                    value = settings.ghostBallDp,
                    range = 6f..30f,
                    onChange = { onChange(settings.copy(ghostBallDp = it)) }
                )
            }

            SectionLabel("SNAP ROTATION")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Off", 5 to "5°", 15 to "15°").forEach { (degrees, label) ->
                    SettingChip(
                        selected = settings.snapDegrees == degrees,
                        label = label,
                        onClick = { onChange(settings.copy(snapDegrees = degrees)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            labelColor = IvoryMuted,
            selectedContainerColor = Brass.copy(alpha = 0.18f),
            selectedLabelColor = Brass
        )
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Brass.copy(alpha = 0.9f)
    )
}

@Composable
private fun SettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = IvoryMuted)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = Brass)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Brass,
                activeTrackColor = Brass,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = IvoryMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FeltDeep,
                checkedTrackColor = Brass,
                checkedBorderColor = Brass,
                uncheckedThumbColor = IvoryMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun RulerPreview(settings: OverlaySettings, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(FeltGreen, FeltDeep)))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            val pocket = 9.dp.toPx()
            listOf(
                Offset(10.dp.toPx(), 10.dp.toPx()),
                Offset(size.width - 10.dp.toPx(), 10.dp.toPx()),
                Offset(10.dp.toPx(), size.height - 10.dp.toPx()),
                Offset(size.width - 10.dp.toPx(), size.height - 10.dp.toPx())
            ).forEach { center ->
                drawCircle(color = BallBlack, radius = pocket, center = center)
            }

            val pivot = Offset(size.width * 0.2f, size.height * 0.7f)
            val length = size.minDimension * 0.78f
            val angle = -33f
            val radians = Math.toRadians(angle.toDouble())
            val dir = Offset(cos(radians).toFloat(), sin(radians).toFloat())
            val perp = Offset(-dir.y, dir.x)
            val end = pivot + dir * length

            val color = Color(settings.color.argb)
            val mainColor = color.copy(alpha = settings.opacity)
            val softColor = color.copy(alpha = settings.opacity * 0.5f)
            val lineWidth = settings.thicknessDp.dp.toPx().coerceAtMost(5.dp.toPx())

            when (settings.style) {
                RulerStyle.AIM_LINE -> {
                    if (settings.showTicks) {
                        for (i in 1..9) {
                            val point = pivot + dir * (length * i / 10f)
                            val half = if (i % 5 == 0) 6.dp.toPx() else 3.5.dp.toPx()
                            drawLine(
                                color = softColor,
                                start = point - perp * half,
                                end = point + perp * half,
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                RulerStyle.PROTRACTOR -> {
                    val radius = minOf(length * 0.55f, 70.dp.toPx())
                    drawCircle(
                        color = softColor.copy(alpha = softColor.alpha * 0.6f),
                        radius = radius,
                        center = pivot,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    for (degree in 0 until 360 step 10) {
                        val a = Math.toRadians(degree.toDouble())
                        val inner = radius - if (degree % 30 == 0) 8.dp.toPx() else 4.dp.toPx()
                        drawLine(
                            color = softColor,
                            start = Offset(
                                pivot.x + cos(a).toFloat() * inner,
                                pivot.y + sin(a).toFloat() * inner
                            ),
                            end = Offset(
                                pivot.x + cos(a).toFloat() * radius,
                                pivot.y + sin(a).toFloat() * radius
                            ),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                RulerStyle.CROSSHAIR -> {
                    val half = 22.dp.toPx()
                    drawLine(
                        color = softColor,
                        start = end - perp * half,
                        end = end + perp * half,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            drawLine(
                color = mainColor,
                start = pivot,
                end = end,
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )

            if (settings.showGhostBall) {
                val radius = settings.ghostBallDp.dp.toPx().coerceAtMost(18.dp.toPx())
                drawCircle(
                    color = mainColor.copy(alpha = 0.16f),
                    radius = radius,
                    center = end
                )
                drawCircle(
                    color = mainColor,
                    radius = radius,
                    center = end,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            } else {
                drawCircle(color = BallBlack, radius = 5.dp.toPx(), center = end)
                drawCircle(
                    color = mainColor,
                    radius = 5.dp.toPx(),
                    center = end,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            drawCircle(color = BallBlack, radius = 5.dp.toPx(), center = pivot)
            drawCircle(
                color = mainColor,
                radius = 5.dp.toPx(),
                center = pivot,
                style = Stroke(width = 1.5.dp.toPx())
            )

            if (settings.showAngle) {
                val label = textMeasurer.measure(
                    text = AnnotatedString("33°"),
                    style = TextStyle(
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                val pad = 4.dp.toPx()
                val labelTopLeft = pivot + Offset(24.dp.toPx(), -38.dp.toPx())
                drawRoundRect(
                    color = Color(0xCC071E18),
                    topLeft = labelTopLeft,
                    size = Size(label.size.width + pad * 2, label.size.height + pad),
                    cornerRadius = CornerRadius(5.dp.toPx())
                )
                drawText(
                    textLayoutResult = label,
                    topLeft = labelTopLeft + Offset(pad, pad / 2)
                )
            }
        }

        Text(
            text = "PREVIEW",
            style = MaterialTheme.typography.labelSmall,
            color = IvoryMuted.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
        )
    }
}

@Composable
private fun HowToCard() {
    PoolCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("How to use", style = MaterialTheme.typography.titleLarge)
            Step(1, "Grant the two permissions, then tap Show Ruler.")
            Step(2, "Open your pool game — the ruler floats on top.")
            Step(3, "Drag to move, drag the ghost ball to rotate, pinch to resize.")
            Step(4, "Tap the padlock to pass touches through. Unlock or stop from the notification.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Brass.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Tip: turn on 15° snap for quick cut and bank angles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brass.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Brass.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, color = Brass)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = IvoryMuted,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun FairPlayNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = IvoryMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Built for practice and solo aiming. Overlay tools may break the rules of online games — use them responsibly.",
            style = MaterialTheme.typography.bodySmall,
            color = IvoryMuted.copy(alpha = 0.65f)
        )
    }
}

private fun RulerStyle.label(): String = when (this) {
    RulerStyle.AIM_LINE -> "Aim line"
    RulerStyle.PROTRACTOR -> "Protractor"
    RulerStyle.CROSSHAIR -> "Crosshair"
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}
