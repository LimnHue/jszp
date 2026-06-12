package com.shangan.teacherprep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.shangan.teacherprep.data.PaletteStyle

@Immutable
data class PrepColors(
    val primary: Color,
    val secondary: Color,
    val gradientEnd: Color,
    val structured: Color = Color(0xFF7654F6),
    val template: Color = Color(0xFFFF9418),
)

val LocalPrepColors = staticCompositionLocalOf {
    PrepColors(Color(0xFFFF3150), Color(0xFFFF705F), Color(0xFFFF2549))
}
val LocalSurfaceOpacity = staticCompositionLocalOf { 0.96f }

private fun palette(style: PaletteStyle): PrepColors = when (style) {
    PaletteStyle.CORAL -> PrepColors(Color(0xFFFF3150), Color(0xFFFF705F), Color(0xFFFF2549))
    PaletteStyle.SKY -> PrepColors(Color(0xFF248BFF), Color(0xFF65C8FF), Color(0xFF356BFF))
    PaletteStyle.PINK -> PrepColors(Color(0xFFFF5B9B), Color(0xFFFF91B8), Color(0xFFFF477A))
    PaletteStyle.VIOLET -> PrepColors(Color(0xFF7654F6), Color(0xFFAA75FF), Color(0xFF5C45E8))
    PaletteStyle.MINT -> PrepColors(Color(0xFF18A88B), Color(0xFF5DD7BB), Color(0xFF0B907A))
}

@Composable
fun TeacherPrepTheme(
    paletteStyle: PaletteStyle,
    surfaceOpacity: Float,
    content: @Composable () -> Unit,
) {
    val prep = palette(paletteStyle)
    val scheme = lightColorScheme(
        primary = prep.primary,
        secondary = prep.secondary,
        background = Color(0xFFFFFBFB),
        surface = Color.White,
        onBackground = Color(0xFF17171C),
        onSurface = Color(0xFF17171C),
    )
    androidx.compose.runtime.CompositionLocalProvider(
        LocalPrepColors provides prep,
        LocalSurfaceOpacity provides surfaceOpacity,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
