package ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.accessibleState
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras
import kotlin.math.roundToInt

/**
 * A slider over a fixed set of stops, with its [label] and the chosen [valueLabel] above and the
 * [startLabel] and [endLabel] of the range below. It works in stop indexes (0 until [stopCount]) so
 * the caller decides what each stop is worth and how it is worded.
 *
 * [valueLabel] is also what a screen reader announces as the slider's state, so it should read
 * as a phrase ("up to £80"), not a bare number. [onStopSelected] is called only when the chosen stop
 * changes.
 */
@Composable
fun LabelledSlider(
    label: String,
    valueLabel: String,
    startLabel: String,
    endLabel: String,
    stopCount: Int,
    selectedStop: Int,
    onStopSelected: (stop: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastStop = (stopCount - 1).coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            FieldLabel(text = label)
            Text(
                text = valueLabel,
                style = ScentThemeExtras.listingRowPrice,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = selectedStop.coerceIn(0, lastStop).toFloat(),
            onValueChange = { raw ->
                val stop = raw.roundToInt()
                if (stop != selectedStop) onStopSelected(stop)
            },
            modifier = Modifier.fillMaxWidth().accessibleState(valueLabel),
            valueRange = 0f..lastStop.toFloat(),
            steps = lastStop - 1,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RangeEndLabel(startLabel)
            RangeEndLabel(endLabel)
        }
    }
}

@Composable
private fun RangeEndLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun LabelledSliderPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LabelledSlider(
                label = "Budget",
                valueLabel = "up to £80",
                startLabel = "£20",
                endLabel = "£150",
                stopCount = 27,
                selectedStop = 12,
                onStopSelected = {},
            )
        }
    }
}
