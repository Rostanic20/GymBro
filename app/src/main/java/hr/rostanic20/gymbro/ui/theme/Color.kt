package hr.rostanic20.gymbro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Iron = Color(0xFF3F4CD8)
private val IronLight = Color(0xFFB9C0FF)
private val IronContainer = Color(0xFFE1E2FF)
private val IronContainerDark = Color(0xFF2A35A8)
private val Chalk = Color(0xFFF7F7FB)
private val Graphite = Color(0xFF12131A)
private val GraphiteSurface = Color(0xFF1B1D26)
private val Steel = Color(0xFF5A6070)
private val SteelLight = Color(0xFFC3C6D4)
private val Lime = Color(0xFF2E9E5B)
private val LimeLight = Color(0xFF7BDCA2)
private val Amber = Color(0xFFB8761B)
private val AmberLight = Color(0xFFF2B75C)
private val Rust = Color(0xFFB3261E)
private val RustLight = Color(0xFFFFB4AB)

@Immutable
data class StatusColors(
    val onTrack: Color,
    val over: Color,
    val attention: Color,
    val attentionContainer: Color,
    val onAttentionContainer: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        onTrack = Lime,
        over = Amber,
        attention = Amber,
        attentionContainer = Color(0xFFFFE2B8),
        onAttentionContainer = Color(0xFF2B1700),
    )
}

internal val LightStatusColors = StatusColors(
    onTrack = Lime,
    over = Amber,
    attention = Amber,
    attentionContainer = Color(0xFFFFE2B8),
    onAttentionContainer = Color(0xFF2B1700),
)

internal val DarkStatusColors = StatusColors(
    onTrack = LimeLight,
    over = AmberLight,
    attention = AmberLight,
    attentionContainer = Color(0xFF4A3006),
    onAttentionContainer = Color(0xFFFFE2B8),
)

internal val GymBroLightColors = lightColorScheme(
    primary = Iron,
    onPrimary = Color.White,
    primaryContainer = IronContainer,
    onPrimaryContainer = Color(0xFF0A1160),
    secondary = Steel,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E4EE),
    onSecondaryContainer = Color(0xFF181B24),
    tertiary = Lime,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F4E0),
    onTertiaryContainer = Color(0xFF00210F),
    error = Rust,
    onError = Color.White,
    background = Chalk,
    onBackground = Graphite,
    surface = Chalk,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFE4E5EF),
    onSurfaceVariant = Color(0xFF45485A),
    outline = Color(0xFF767988),
)

internal val GymBroDarkColors = darkColorScheme(
    primary = IronLight,
    onPrimary = Color(0xFF0A1160),
    primaryContainer = IronContainerDark,
    onPrimaryContainer = IronContainer,
    secondary = SteelLight,
    onSecondary = Color(0xFF2A2E3B),
    secondaryContainer = Color(0xFF3A3F4F),
    onSecondaryContainer = Color(0xFFE2E4EE),
    tertiary = LimeLight,
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = Color(0xFF14522F),
    onTertiaryContainer = Color(0xFFD4F4E0),
    error = RustLight,
    onError = Color(0xFF690005),
    background = Graphite,
    onBackground = Color(0xFFE4E5EF),
    surface = Graphite,
    onSurface = Color(0xFFE4E5EF),
    surfaceContainer = GraphiteSurface,
    surfaceVariant = Color(0xFF2B2E3A),
    onSurfaceVariant = Color(0xFFC3C6D4),
    outline = Color(0xFF8C8FA0),
)
