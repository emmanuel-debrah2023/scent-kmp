package ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A bottom-border-only text input field.
 *
 * [label] sits above the input in label-uppercase style (labelLarge, uppercased).
 * Border animates outline-variant → primary on focus, → error when [error] is set.
 * When [enabled] is false the border and text are muted to outline-variant.
 * [helperText] renders below the field in the same slot [error] uses — [error] always
 * wins when both are set, so a field never shows stale help text next to a live error.
 * [singleLine]/[minLines] mirror [androidx.compose.foundation.text.BasicTextField]'s own
 * params for a multi-line field (e.g. a bio); the fixed [ui.theme.ScentSpacing.buttonHeight]
 * only applies when [singleLine] is true.
 */
@Composable
fun ScentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    error: String? = null,
    helperText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue =
            when {
                !enabled -> MaterialTheme.colorScheme.outlineVariant
                error != null -> MaterialTheme.colorScheme.error
                isFocused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        animationSpec =
            tween(
                durationMillis = ScentThemeExtras.motion.durationDefault,
                easing = ScentThemeExtras.motion.easingDefault,
            ),
        label = "scentTextField_border",
    )

    val textColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(ScentThemeExtras.spacing.xs))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let {
                        if (singleLine) {
                            it.height(ScentThemeExtras.spacing.buttonHeight)
                        } else {
                            it.heightIn(min = ScentThemeExtras.spacing.buttonHeight)
                        }
                    }.onFocusChanged { isFocused = it.isFocused },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = singleLine,
            minLines = minLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = borderColor,
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = ScentThemeExtras.spacing.xxs),
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = ScentThemeExtras.spacing.xxs),
            )
        }
    }
}

/**
 * A rounded search input with a leading search icon.
 *
 * Uses [surfaceContainerHigh] as the container background with a 1dp border that
 * animates from outline-variant to primary on focus. No label above.
 */
@Composable
fun ScentSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue =
            if (isFocused && enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        animationSpec =
            tween(
                durationMillis = ScentThemeExtras.motion.durationDefault,
                easing = ScentThemeExtras.motion.easingDefault,
            ),
        label = "scentSearchBar_border",
    )

    val textColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .height(ScentThemeExtras.spacing.buttonHeight)
                .onFocusChanged { isFocused = it.isFocused },
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, borderColor, MaterialTheme.shapes.medium)
                        .padding(horizontal = ScentThemeExtras.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(ScentThemeExtras.spacing.xs))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldEmptyPreview() {
    ScentTheme {
        ScentTextField(value = "", onValueChange = {}, label = "Email", placeholder = "you@example.com")
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldWithValuePreview() {
    ScentTheme {
        ScentTextField(value = "hello@scent.app", onValueChange = {}, label = "Email")
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldErrorPreview() {
    ScentTheme {
        ScentTextField(
            value = "bad",
            onValueChange = {},
            label = "Email",
            error = "Enter a valid email address",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldDisabledPreview() {
    ScentTheme {
        ScentTextField(value = "", onValueChange = {}, label = "Email", enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldHelperTextPreview() {
    ScentTheme {
        ScentTextField(
            value = "Emmanuel Debrah",
            onValueChange = {},
            label = "Display name",
            helperText = "Shown on your listings, reviews and messages",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentTextFieldMultiLinePreview() {
    ScentTheme {
        ScentTextField(
            value = "Fragrance collector. Niche over designer, always.",
            onValueChange = {},
            label = "Bio",
            placeholder = "A line about your taste in fragrance",
            helperText = "Optional",
            singleLine = false,
            minLines = 3,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentSearchBarEmptyPreview() {
    ScentTheme {
        ScentSearchBar(value = "", onValueChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ScentSearchBarWithValuePreview() {
    ScentTheme {
        ScentSearchBar(value = "Sauvage", onValueChange = {})
    }
}
