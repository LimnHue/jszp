package com.shangan.teacherprep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.shangan.teacherprep.data.PaletteStyle

@Immutable
data class PrepColors(
    val primary: Color,
    val secondary: Color,
    val gradientEnd: Color,
    val structured: Color = Color(0xFF52677D),
    val template: Color = Color(0xFF52677D),
)

val LocalPrepColors = staticCompositionLocalOf {
    PrepColors(Color(0xFF52677D), Color(0xFF6B7F93), Color(0xFF52677D))
}
val LocalSurfaceOpacity = staticCompositionLocalOf { 0.96f }
val LocalLogoScale = staticCompositionLocalOf { 1f }

private fun palette(style: PaletteStyle): PrepColors =
    PrepColors(Color(0xFF52677D), Color(0xFF6B7F93), Color(0xFF52677D))

@Composable
fun TeacherPrepTheme(
    paletteStyle: PaletteStyle,
    surfaceOpacity: Float,
    logoScale: Float,
    uiScale: Float,
    fontScale: Float,
    content: @Composable () -> Unit,
) {
    val prep = palette(paletteStyle)
    val scheme = lightColorScheme(
        primary = prep.primary,
        secondary = prep.secondary,
        background = Color(0xFFF7F5F1),
        surface = Color(0xFFFCFBF8),
        surfaceVariant = Color(0xFFE9EEF2),
        outline = Color(0xFFCBC8C2),
        outlineVariant = Color(0xFFDDD9D2),
        onBackground = Color(0xFF202224),
        onSurface = Color(0xFF202224),
    )
    val systemDensity = LocalDensity.current
    BoxWithConstraints {
        val automaticScale = when {
            maxWidth < 360.dp -> 0.88f
            maxWidth < 480.dp -> 0.94f
            maxWidth < 600.dp -> 0.98f
            else -> 1.02f
        }
        val selectedUiScale = uiScale.coerceIn(0.75f, 1.20f)
        val selectedFontScale = fontScale.coerceIn(0.80f, 1.20f)
        val densityScale = automaticScale * selectedUiScale
        androidx.compose.runtime.CompositionLocalProvider(
            LocalPrepColors provides prep,
            LocalSurfaceOpacity provides surfaceOpacity,
            LocalLogoScale provides logoScale.coerceIn(0.70f, 1.40f),
            LocalDensity provides Density(
                density = systemDensity.density * densityScale,
                fontScale = systemDensity.fontScale.coerceIn(0.90f, 1.15f) *
                    selectedFontScale / selectedUiScale,
            ),
        ) {
            MaterialTheme(colorScheme = scheme, content = content)
        }
    }
}
