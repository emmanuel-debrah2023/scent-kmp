package surfaces

import preferences.PriceRange
import kotlin.math.ceil

const val BUDGET_STEP = 5

/** The ends of the budget slider in whole pounds: multiples of [BUDGET_STEP], at least one step apart. */
data class BudgetBounds(
    val min: Int,
    val max: Int,
)

/**
 * A budget means "up to", so both ends round up: the first stop reaches the cheapest listing and the last
 * reaches the dearest. The minimum is at least one step because [AnswerContext] rejects a budget of 0, and
 * the ends stay a step apart so listings priced within one step still give a slider the client can draw.
 */
fun PriceRange.budgetBounds(): BudgetBounds {
    val low = maxOf(roundUpToStep(min), BUDGET_STEP)
    return BudgetBounds(low, maxOf(roundUpToStep(max), low + BUDGET_STEP))
}

private fun roundUpToStep(price: Double): Int = ceil(price / BUDGET_STEP).toInt() * BUDGET_STEP
