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
import org.scent.project.data.remote.a2ui.catalog.ColumnVariant
import org.scent.project.data.remote.a2ui.catalog.SCENT_CATALOG_ID
import org.scent.project.data.remote.a2ui.catalog.ScentAction
import org.scent.project.data.remote.a2ui.catalog.ScentComponent
import org.scent.project.data.remote.a2ui.catalog.TextVariant
import org.scent.project.domain.model.ListingKind
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
import kotlin.math.roundToInt

private const val EDIT_HEADING = "Change your answers"
private const val NO_MATCH = "Nothing matches yet. Try changing your answers."
private const val QUESTION_CAPTION = "Question written by AI"
private const val REASONS_CAPTION = "Reasons written by AI · prices from Scent"
private const val CAPTION_ID = "caption"
private const val QUESTIONS_ID = "questions"
private const val META_SEPARATOR = " · "
private const val WHOLE_PERCENT = 100.0

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
    ): List<A2uiServerMessage> = listOf(create(surfaceId)) + questions(surfaceId, askBack)

    /** The edit_answers response: every question again, answers filled in, on the request's own surface. */
    fun edit(
        surfaceId: String,
        preferences: ScentPreferences,
        priceRange: PriceRange?,
    ): List<A2uiServerMessage> =
        questions(
            surfaceId,
            AskBack(
                preferences,
                PreferenceQuestionPlanner.plan(ScentPreferences(), priceRange),
                EDIT_HEADING,
                buttonFor(preferences.intent),
            ),
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
                                put("listingId", it.listingId)
                                put("name", it.name)
                                put("brand", it.brand)
                                put("price", formatPrice(it.price))
                                put("meta", meta(it))
                                put("why", whyByListing[it.listingId] ?: GENERIC_WHY)
                            }
                        },
                    ),
                )
            }
        val cards = candidates.mapIndexed { index, candidate -> card(index, candidate) }
        val body = if (cards.isEmpty()) listOf(ScentComponent.Text("empty", NO_MATCH)) else cards
        val caption = captionIf(cards.isNotEmpty() && whyLines.any { it.modelWritten }, REASONS_CAPTION)
        val components =
            listOf(ScentComponent.Column("root", listOf("summary") + (caption + body).mapNotNull { it.id })) +
                ScentComponent.PreferenceSummary(
                    "summary",
                    DataBinding(AskPaths.SUMMARY),
                    submit(ScentAction.EDIT_ANSWERS),
                ) +
                caption +
                body
        val head = if (create) listOf(create(surfaceId)) else emptyList()
        return head + listOf(dataModel(surfaceId, data), components(surfaceId, components))
    }

    private fun questions(
        surfaceId: String,
        askBack: AskBack,
    ): List<A2uiServerMessage> {
        val preferences = askBack.preferences
        val questions = askBack.questions
        val bounds =
            questions
                .filterIsInstance<BudgetQuestion>()
                .firstOrNull()
                ?.range
                ?.budgetBounds()
        val data =
            buildJsonObject {
                put("answers", answers(preferences, preferences.maxPrice ?: bounds?.max?.toDouble()))
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
                bounds?.let {
                    put(
                        "budget",
                        buildJsonObject {
                            put("min", it.min)
                            put("max", it.max)
                            put("step", BUDGET_STEP)
                        },
                    )
                }
            }
        val inputs = questions.map { input(it) }
        val caption = captionIf(askBack.modelWritten, QUESTION_CAPTION)
        val components =
            listOf(
                ScentComponent.Column(
                    "root",
                    caption.mapNotNull { it.id } + listOf("heading", QUESTIONS_ID, "submit"),
                ),
            ) +
                caption +
                ScentComponent.Text("heading", askBack.heading, TextVariant.HEADING) +
                ScentComponent.Column(QUESTIONS_ID, inputs.mapNotNull { it.id }, ColumnVariant.RAISED) +
                inputs +
                ScentComponent.Button("submit", askBack.buttonLabel, submit(ScentAction.ANSWER_QUESTIONS))
        return listOf(dataModel(surfaceId, data), components(surfaceId, components))
    }

    // Only for wording a model really wrote; the rules and fixed headings never say AI wrote them.
    private fun captionIf(
        modelWritten: Boolean,
        text: String,
    ): List<ScentComponent> =
        if (modelWritten) listOf(ScentComponent.Text(CAPTION_ID, text, TextVariant.CAPTION)) else emptyList()

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
        A2uiAction(
            A2uiEvent(
                ScentAction.OPEN_LISTING.wireName,
                mapOf("listingId" to DataBinding("${AskPaths.RESULTS}/$index/listingId")),
            ),
        ),
        DataBinding("${AskPaths.RESULTS}/$index/meta"),
    )

    private fun meta(candidate: ListingCandidate): String =
        listOfNotNull(
            candidate.kind.label(),
            fillLevel(candidate),
            candidate.nominalSizeMl?.let { "$it ml" },
            candidate.brand,
        ).joinToString(META_SEPARATOR)

    // Sealed bottles and decants are full by definition, so only an opened bottle or a tester says how full.
    private fun fillLevel(candidate: ListingCandidate): String? {
        val remaining = candidate.remainingMl
        val nominal = candidate.nominalSizeMl?.takeIf { it > 0 }
        val varies = candidate.kind == ListingKind.OPENED || candidate.kind == ListingKind.TESTER
        return if (varies && remaining != null && nominal != null) {
            "${(remaining * WHOLE_PERCENT / nominal).roundToInt()}% full"
        } else {
            null
        }
    }

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

    private fun ListingKind.label() = name.lowercase().replaceFirstChar { it.uppercase() }

    private fun SizePreference.label() = if (this == SizePreference.FULL) "Full bottle" else "Decant"
}
