package hr.rostanic20.gymbro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Ember = Color(0xFFC1501B)
private val EmberBright = Color(0xFFFF8A50)
private val EmberContainerLight = Color(0xFFFFDBCD)
private val EmberContainerDark = Color(0xFF8A3A12)
private val OnEmberContainerLight = Color(0xFF3A0C00)
private val OnEmberContainerDark = Color(0xFFFFDBCD)

private val Ash = Color(0xFF77574A)
private val AshBright = Color(0xFFE7BDAB)
private val Moss = Color(0xFF2E7D4F)
private val MossBright = Color(0xFF7BDCA2)
private val Blood = Color(0xFFB3261E)
private val BloodBright = Color(0xFFFFB4AB)

@Immutable
data class StatusColors(
    val onTrack: Color,
    val over: Color,
    val attention: Color,
    val attentionContainer: Color,
    val onAttentionContainer: Color,
)

internal val LightStatusColors = StatusColors(
    onTrack = Moss,
    over = Blood,
    attention = Ember,
    attentionContainer = EmberContainerLight,
    onAttentionContainer = OnEmberContainerLight,
)

internal val DarkStatusColors = StatusColors(
    onTrack = MossBright,
    over = BloodBright,
    attention = EmberBright,
    attentionContainer = EmberContainerDark,
    onAttentionContainer = OnEmberContainerDark,
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

internal val GymBroLightColors = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    primaryContainer = EmberContainerLight,
    onPrimaryContainer = OnEmberContainerLight,
    inversePrimary = EmberBright,
    secondary = Ash,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCD),
    onSecondaryContainer = Color(0xFF2C160D),
    tertiary = Moss,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB7F0CC),
    onTertiaryContainer = Color(0xFF002110),
    error = Blood,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFBF8F6),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF0E0D9),
    onSurfaceVariant = Color(0xFF52443D),
    surfaceTint = Ember,
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4DAD4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6EFEB),
    surfaceContainer = Color(0xFFF1E9E4),
    surfaceContainerHigh = Color(0xFFEBE2DD),
    surfaceContainerHighest = Color(0xFFE5DBD5),
    inverseSurface = Color(0xFF362F2B),
    inverseOnSurface = Color(0xFFFBEEE9),
    outline = Color(0xFF857369),
    outlineVariant = Color(0xFFD8C2B8),
    scrim = Color.Black,
)

internal val GymBroDarkColors = darkColorScheme(
    primary = EmberBright,
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = EmberContainerDark,
    onPrimaryContainer = OnEmberContainerDark,
    inversePrimary = Ember,
    secondary = AshBright,
    onSecondary = Color(0xFF442A1E),
    secondaryContainer = Color(0xFF5D4033),
    onSecondaryContainer = Color(0xFFFFDBCD),
    tertiary = MossBright,
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = Color(0xFF15522F),
    onTertiaryContainer = Color(0xFFB7F0CC),
    error = BloodBright,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF14110F),
    onBackground = Color(0xFFF2E9E4),
    surface = Color(0xFF14110F),
    onSurface = Color(0xFFF2E9E4),
    surfaceVariant = Color(0xFF453A34),
    onSurfaceVariant = Color(0xFFD5C7BF),
    surfaceTint = EmberBright,
    surfaceBright = Color(0xFF3B3532),
    surfaceDim = Color(0xFF14110F),
    surfaceContainerLowest = Color(0xFF0E0B0A),
    surfaceContainerLow = Color(0xFF1C1816),
    surfaceContainer = Color(0xFF221D1A),
    surfaceContainerHigh = Color(0xFF2C2724),
    surfaceContainerHighest = Color(0xFF37312E),
    inverseSurface = Color(0xFFF2E9E4),
    inverseOnSurface = Color(0xFF362F2B),
    outline = Color(0xFF9F8D84),
    outlineVariant = Color(0xFF52443D),
    scrim = Color.Black,
)
