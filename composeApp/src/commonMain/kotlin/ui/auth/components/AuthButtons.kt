package ui.auth.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ScentPrimaryButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    ScentSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
    )
}
