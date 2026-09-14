package hr.rostanic20.gymbro.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import hr.rostanic20.gymbro.ui.theme.LocalSpacing

@Composable
fun Tag(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    val spacing = LocalSpacing.current
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = spacing.s8, vertical = spacing.s2),
        )
    }
}
