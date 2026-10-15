package surfaces

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import preferences.Intent
import preferences.PriceRange
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference
import kotlin.math.ceil

/** The data paths an ask surface reads and writes. Builder, button context and parser share them. */
object AskPaths {
    const val INTENT = "/answers/intent"
    const val FAMILY = "/answers/family"
    const val BUDGET = "/answers/budget"
    const val SIZE = "/answers/size"
    const val FAMILY_OPTIONS = "/options/family"
    const val SIZE_OPTIONS = "/options/size"
    const val BUDGET_MIN = "/budget/min"
    const val BUDGET_MAX = "/budget/max"
    const val BUDGET_STEP = "/budget/step"
    const val SUMMARY = "/summary"
    const val RESULTS = "/results"
}

/**
 * Reads the answers a client echoes back in an action's context. The client is untrusted: every value
 * must be a known enum name or a sane number, anything else is rejected, and unknown keys are ignored
 * (never echoed). A budget above the live maximum is clamped, because the price range can move
 * between the two calls; one below the minimum is kept and honestly returns nothing.
 */
object AnswerContext {
    fun parse(
        context: JsonObject?,
        priceRange: PriceRange?,
    ): Result<ScentPreferences> {
        val values = context ?: JsonObject(emptyMap())
        return enumField<Intent>(values, "intent").flatMap { intent ->
            enumField<ScentFamily>(values, "family").flatMap { family ->
                enumField<SizePreference>(values, "size").flatMap { size ->
                    budgetField(values, priceRange).map { budget -> ScentPreferences(intent, family, budget, size) }
                }
            }
        }
    }

    private inline fun <reified E : Enum<E>> enumField(
        values: JsonObject,
        field: String,
    ): Result<E?> {
        val raw = values[field]
        if (raw == null || raw is JsonNull) return null.asRight()
        val match =
            (raw as? JsonPrimitive)?.takeIf { it.isString }?.let { text ->
                enumValues<E>().firstOrNull {
                    it.name ==
                        text.content
                }
            }
        return match?.asRight() ?: invalid(field)
    }

    private fun budgetField(
        values: JsonObject,
        priceRange: PriceRange?,
    ): Result<Double?> {
        val raw = values["budget"]
        if (raw == null || raw is JsonNull) return null.asRight()
        val number = (raw as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull
        if (number == null || !number.isFinite() || number <= 0.0) return invalid("budget")
        val ceiling = priceRange?.let { ceil(it.max) }
        return (if (ceiling != null) minOf(number, ceiling) else number).asRight()
    }

    private fun invalid(field: String): Result<Nothing> = AppError.ValidationError.InvalidInput(field).asLeft()
}
