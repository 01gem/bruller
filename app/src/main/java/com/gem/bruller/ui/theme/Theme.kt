package com.gem.bruller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PoolColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = Color(0xFF21180A),
    primaryContainer = BrassDark,
    onPrimaryContainer = Ivory,
    secondary = ChalkBlue,
    onSecondary = Color(0xFF06282E),
    secondaryContainer = Color(0xFF123E45),
    onSecondaryContainer = Ivory,
    tertiary = Crimson,
    onTertiary = Color(0xFF2A0808),
    background = FeltDeep,
    onBackground = Ivory,
    surface = FeltCard,
    onSurface = Ivory,
    surfaceVariant = FeltRaised,
    onSurfaceVariant = IvoryMuted,
    outline = Color(0xFF3C5A4D),
    outlineVariant = Color(0xFF29463A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun RulerFor8BallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PoolColorScheme,
        typography = Typography,
        content = content
    )
}
