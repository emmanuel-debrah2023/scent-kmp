package ui.components.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun ScentPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val motion = ScentThemeExtras.motion
    val animSpec = tween<Color>(durationMillis = motion.durationDefault, easing = motion.easingDefault)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracted = isHovered || isPressed

    val containerColor by animateColorAsState(
        targetValue =
            if (isInteracted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
        animationSpec = animSpec,
        label = "primaryButton_container",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isInteracted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
        animationSpec = animSpec,
        label = "primaryButton_content",
    )

    Button(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(ScentThemeExtras.spacing.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactionSource,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
            ),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ScentSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val motion = ScentThemeExtras.motion
    val animSpec = tween<Color>(durationMillis = motion.durationDefault, easing = motion.easingDefault)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracted = isHovered || isPressed

    val containerColor by animateColorAsState(
        targetValue =
            if (isInteracted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            },
        animationSpec = animSpec,
        label = "secondaryButton_container",
    )

    OutlinedButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(ScentThemeExtras.spacing.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        interactionSource = interactionSource,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentPrimaryButtonEnabledPreview() {
    ScentTheme {
        ScentPrimaryButton(text = "Sign In", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentPrimaryButtonDisabledPreview() {
    ScentTheme {
        ScentPrimaryButton(text = "Sign In", onClick = {}, enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentSecondaryButtonEnabledPreview() {
    ScentTheme {
        ScentSecondaryButton(text = "Continue with Google", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentSecondaryButtonDisabledPreview() {
    ScentTheme {
        ScentSecondaryButton(text = "Continue with Google", onClick = {}, enabled = false)
    }
}
