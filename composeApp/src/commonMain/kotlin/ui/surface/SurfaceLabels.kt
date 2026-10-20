package ui.surface

import kotlin.math.roundToInt

/** The wording the client adds around a surface's facts: units, joiners and its own control names. */
internal object SurfaceLabels {
    const val CHANGE = "Change"

    private const val SEPARATOR = " · "

    /** Whole pounds, matching how the rest of the app shows prices. */
    fun pounds(amount: Double): String = "£${amount.roundToInt()}"

    fun budgetValue(amount: Double): String = "up to ${pounds(amount)}"

    fun summary(items: List<String>): String = items.joinToString(SEPARATOR)
}
