package ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

// ─────────────────────────────────────────────
// Motion tokens — sourced from design.md
// transition-default: 300ms ease-in-out
// Used for color and border-state transitions (input focus, button hover).
// ─────────────────────────────────────────────
@Immutable
data class ScentMotion(
    val durationDefault: Int = 300,
    val easingDefault: Easing = FastOutSlowInEasing,
)

val LocalScentMotion = staticCompositionLocalOf { ScentMotion() }
