package ui.auth.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val fontSize = if (maxWidth < 375.dp) 48.sp else 64.sp
        Text(
            text = "scent",
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif, // Fallback for Playfair Display
                    letterSpacing = (-0.05).sp * fontSize.value / 64.0, // Approximation
                    color = MaterialTheme.colorScheme.onSurface,
                ),
        )
    }
}
