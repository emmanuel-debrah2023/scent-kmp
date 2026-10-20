package surfaces

import askback.AskBack
import askback.GENERIC_WHY
import askback.WhyLine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.scent.project.data.remote.a2ui.A2UI_VERSION
import org.scent.project.data.remote.a2ui.A2uiServerMessage
import org.scent.project.data.remote.a2ui.CreateSurface
import org.scent.project.data.remote.a2ui.DataBinding
import org.scent.project.data.remote.a2ui.UpdateComponents
import org.scent.project.data.remote.a2ui.UpdateDataModel
import org.scent.project.data.remote.a2ui.catalog.A2uiAction
import org.scent.project.data.remote.a2ui.catalog.A2uiEvent
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.catalog.ScentAction
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.data.remote.a2ui.catalog.TextVariant
import preferences.BudgetQuestion
import preferences.FamilyQuestion
import preferences.Intent
import preferences.ListingCandidate
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.QuestionSpec
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference
import preferences.SizeQuestion
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.floor

private const val BUDGET_STEP = 5
private const val EDIT_HEADING = "Change your answers"
private const val NO_MATCH = "Nothing matches yet. Try changing your answers."

/**
 * Builds the ask surface messages. Every fact (options, budget bounds, answers, results, prices) goes
 * in the data model; components carry only Kotlin or agent-written wording and DataBinding paths.
 * Options always come from the Kotlin enums, so nothing the model says can add or change one.
 */
object AskSurfaceBuilder {
    /** First response to POST /ask when something is left to ask: create, data, components. */
    fun initial(
        surfaceId: String,
        askBack: AskBack,
    ): List<A2uiServerMessage> =
        listOf(create(surfaceId)) +
            questions(
                surfaceId,
                askBack.preferences,
                askBack.questions,
                askBack.heading,
                askBack.buttonLabel,
            )

    /** The edit_answers response: every question again, answers filled in, on the request's own surface. */
    fun edit(
        surfaceId: String,
        preferences: ScentPreferences,
        priceRange: PriceRange?,
    ): List<A2uiServerMessage> =
        questions(
            surfaceId,
            preferences,
            PreferenceQuestionPlanner.plan(ScentPreferences(), priceRange),
            EDIT_HEADING,
            buttonFor(preferences.intent),
        )

    /** Results for [preferences]; [create] is true only when POST /ask skips the questions entirely. */
    fun results(
        surfaceId: String,
        create: Boolean,
        preferences: ScentPreferences,
        candidates: List<ListingCandidate>,
        whyLines: List<WhyLine>,
    ): List<A2uiServerMessage> {
        val whyByListing = whyLines.associate { it.listingId to it.text }
        val data =
            buildJsonObject {
                put("answers", answers(preferences, preferences.maxPrice))
                put("summary", JsonArray(summary(preferences).map(::JsonPrimitive)))
                put(
                    "results",
                    JsonArray(
                        candidates.map {
                            buildJsonObject {
                                put("name", it.name)
                                put("brand", it.brand)
                                put("price", formatPrice(it.price))
                                put("why", whyByListing[it.listingId] ?: GENERIC_WHY)
                            }
                        },
                    ),
                )
            }
        val cards = candidates.mapIndexed { index, candidate -> card(index, candidate) }
        val body = if (cards.isEmpty()) listOf(ScentComponent.Text("empty", NO_MATCH)) else cards
        val components =
            listOf(ScentComponent.Column("root", listOf("summary") + body.mapNotNull { it.id })) +
                ScentComponent.PreferenceSummary(
                    "summary",
                    DataBinding(AskPaths.SUMMARY),
                    submit(ScentAction.EDIT_ANSWERS),
                ) +
                body
        val head = if (create) listOf(create(surfaceId)) else emptyList()
        return head + listOf(dataModel(surfaceId, data), components(surfaceId, components))
    }

    private fun questions(
        surfaceId: String,
        preferences: ScentPreferences,
        questions: List<QuestionSpec>,
        heading: String,
        buttonLabel: String,
    ): List<A2uiServerMessage> {
        val bounds = bounds(questions)
        val data =
            buildJsonObject {
                put("answers", answers(preferences, preferences.maxPrice ?: bounds?.second?.toDouble()))
                put(
                    "options",
                    buildJsonObject {
                        questions.filterIsInstance<FamilyQuestion>().firstOrNull()?.let { q ->
                            put("family", options(q.options.map { it.name to it.label() }))
                        }
                        questions.filterIsInstance<SizeQuestion>().firstOrNull()?.let { q ->
                            put("size", options(q.options.map { it.name to it.label() }))
                        }
                    },
                )
                bounds?.let { (min, max) ->
                    put(
                        "budget",
                        buildJsonObject {
                            put("min", min)
                            put("max", max)
                            put("step", BUDGET_STEP)
                        },
                    )
                }
            }
        val inputs = questions.map { input(it) }
        val components =
            listOf(
                ScentComponent.Column("root", listOf("heading") + inputs.mapNotNull { it.id } + "submit"),
                ScentComponent.Text("heading", heading, TextVariant.HEADING),
            ) + inputs + ScentComponent.Button("submit", buttonLabel, submit(ScentAction.ANSWER_QUESTIONS))
        return listOf(dataModel(surfaceId, data), components(surfaceId, components))
    }

    private fun input(question: QuestionSpec): ScentComponent =
        when (question) {
            is FamilyQuestion ->
                ScentComponent.ChoicePicker(
                    "family",
                    "Scent family",
                    DataBinding(AskPaths.FAMILY_OPTIONS),
                    DataBinding(AskPaths.FAMILY),
                )
            is SizeQuestion ->
                ScentComponent.ChoicePicker(
                    "size",
                    "Bottle size",
                    DataBinding(AskPaths.SIZE_OPTIONS),
                    DataBinding(AskPaths.SIZE),
                )
            is BudgetQuestion ->
                ScentComponent.RangeSlider(
                    "budget",
                    "Budget",
                    DataBinding(AskPaths.BUDGET_MIN),
                    DataBinding(AskPaths.BUDGET_MAX),
                    DataBinding(AskPaths.BUDGET_STEP),
                    DataBinding(AskPaths.BUDGET),
                )
        }

    private fun card(
        index: Int,
        candidate: ListingCandidate,
    ) = ScentComponent.RecommendationCard(
        "card_${candidate.listingId}",
        DataBinding("${AskPaths.RESULTS}/$index/name"),
        DataBinding("${AskPaths.RESULTS}/$index/price"),
        DataBinding("${AskPaths.RESULTS}/$index/why"),
    )

    private fun submit(action: ScentAction) =
        A2uiAction(
            A2uiEvent(
                action.wireName,
                mapOf(
                    "intent" to DataBinding(AskPaths.INTENT),
                    "family" to DataBinding(AskPaths.FAMILY),
                    "budget" to DataBinding(AskPaths.BUDGET),
                    "size" to DataBinding(AskPaths.SIZE),
                ),
            ),
        )

    // Every answer key is always present (null when unknown) so each bound path resolves.
    private fun answers(
        preferences: ScentPreferences,
        budget: Double?,
    ): JsonObject =
        buildJsonObject {
            put("intent", preferences.intent?.name?.let(::JsonPrimitive) ?: JsonNull)
            put("family", preferences.family?.name?.let(::JsonPrimitive) ?: JsonNull)
            put("budget", budget?.let { JsonPrimitive(it) } ?: JsonNull)
            put("size", preferences.size?.name?.let(::JsonPrimitive) ?: JsonNull)
        }

    private fun options(pairs: List<Pair<String, String>>): JsonElement =
        JsonArray(
            pairs.map { (value, label) ->
                buildJsonObject {
                    put("value", value)
                    put("label", label)
                }
            },
        )

    // Whole pounds, rounded outward so the real cheapest and dearest listings are inside the slider.
    // TODO(feature/ask-surface-catalog-gaps): floor() can make the minimum 0, and AnswerContext rejects a budget
    //  of 0 or less. The client slider starts at one step (SurfaceBindings), so this is harmless until another
    //  client reads the minimum; clamp it to at least 1 here.
    private fun bounds(questions: List<QuestionSpec>): Pair<Int, Int>? =
        questions.filterIsInstance<BudgetQuestion>().firstOrNull()?.let {
            floor(it.range.min).toInt() to
                ceil(it.range.max).toInt()
        }

    private fun summary(preferences: ScentPreferences): List<String> =
        listOfNotNull(
            preferences.family?.label(),
            preferences.maxPrice?.let { "up to ${formatPrice(it)}" },
            preferences.size?.label(),
        )

    private fun buttonFor(intent: Intent?) = if (intent == Intent.GIFT) "Show gifts" else "Show picks"

    private fun create(surfaceId: String) =
        A2uiServerMessage(A2UI_VERSION, createSurface = CreateSurface(surfaceId, SCENT_CATALOG_ID))

    private fun dataModel(
        surfaceId: String,
        value: JsonObject,
    ) = A2uiServerMessage(A2UI_VERSION, updateDataModel = UpdateDataModel(surfaceId, "/", value))

    private fun components(
        surfaceId: String,
        components: List<ScentComponent>,
    ) = A2uiServerMessage(A2UI_VERSION, updateComponents = UpdateComponents(surfaceId, components))

    internal fun formatPrice(price: Double): String {
        val amount = BigDecimal.valueOf(price)
        val shown =
            if (amount.stripTrailingZeros().scale() <=
                0
            ) {
                amount.setScale(0)
            } else {
                amount.setScale(2, java.math.RoundingMode.HALF_UP)
            }
        return "£${shown.toPlainString()}"
    }

    private fun ScentFamily.label() = name.lowercase().replaceFirstChar { it.uppercase() }

    private fun SizePreference.label() = if (this == SizePreference.FULL) "Full bottle" else "Decant"
}
