package hr.rostanic20.gymbro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private const val TABULAR_FIGURES = "tnum"

private val default = Typography()

// Numbers change every time a set or a meal is logged; lining figures stop the digits from shifting.
private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

val GymBroTypography = Typography(
    displayLarge = default.displayLarge.tabular(),
    displayMedium = default.displayMedium.tabular(),
    displaySmall = default.displaySmall.tabular(),
    headlineLarge = default.headlineLarge.tabular(),
    headlineMedium = default.headlineMedium.tabular(),
    headlineSmall = default.headlineSmall.tabular(),
    titleLarge = default.titleLarge.tabular(),
    titleMedium = default.titleMedium.tabular(),
    titleSmall = default.titleSmall.tabular(),
    bodyLarge = default.bodyLarge.tabular(),
    bodyMedium = default.bodyMedium.tabular(),
    bodySmall = default.bodySmall.tabular(),
    labelLarge = default.labelLarge.tabular(),
    labelMedium = default.labelMedium.tabular(),
    labelSmall = default.labelSmall.tabular(),
)
