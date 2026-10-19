package io.phoneagent.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AgentPalette {
    val Background = Color(0xFF0A1118)
    val Surface = Color(0xFF111C26)
    val SurfaceElevated = Color(0xFF172530)
    val SurfaceSoft = Color(0xFF1C2B36)
    val Mint = Color(0xFF72E4C2)
    val MintDeep = Color(0xFF123D35)
    val Sky = Color(0xFF91B9FF)
    val Amber = Color(0xFFF0C786)
    val TextPrimary = Color(0xFFEAF2F4)
    val TextSecondary = Color(0xFFA4B5C0)
    val Border = Color(0xFF2A3A46)
    val Danger = Color(0xFFFF9898)
}

private val PhoneAgentColors = darkColorScheme(
    primary = AgentPalette.Mint,
    onPrimary = Color(0xFF08251E),
    secondary = AgentPalette.Sky,
    onSecondary = Color(0xFF0A1B32),
    tertiary = AgentPalette.Amber,
    background = AgentPalette.Background,
    onBackground = AgentPalette.TextPrimary,
    surface = AgentPalette.Surface,
    onSurface = AgentPalette.TextPrimary,
    surfaceVariant = AgentPalette.SurfaceSoft,
    onSurfaceVariant = AgentPalette.TextSecondary,
    outline = AgentPalette.Border,
    error = AgentPalette.Danger
)

@Composable
fun PhoneAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhoneAgentColors,
        content = content
    )
}
