package askback

import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Either
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import preferences.Intent
import preferences.ListingCandidate
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.QuestionId
import preferences.QuestionSpec
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference

private const val MAX_TEXT_LENGTH = 80
private const val MAX_WHY_LENGTH = 160
private val numberPattern = Regex("""\d+(?:\.\d+)?""")

/**
 * The safety boundary for model output. The query is untrusted text, so the prompt can only ask
 * nicely; these rules decide what is accepted. A rejection never carries the model's text, only
 * the name of the rule, so a prompt-injected reply cannot leak into logs or responses.
 */
object AskBackValidator {
    fun toAskBack(
        output: AskBackOutput,
        query: String,
        priceRange: PriceRange?,
    ): Result<AskBack> =
        preferencesOf(output, query).flatMap { preferences ->
            questionsFor(output.questions, preferences, priceRange).flatMap { questions ->
                labelsOf(output).map { (heading, button) -> AskBack(preferences, questions, heading, button) }
            }
        }

    fun toWhyLines(
        output: WhyOutput,
        candidates: List<ListingCandidate>,
        vocabulary: Set<String>,
    ): Result<List<WhyLine>> {
        val lines = output.lines ?: return reject("why lines are missing")
        val byListing = candidates.associateBy { it.listingId }
        val accepted = mutableListOf<WhyLine>()
        for (line in lines) {
            when (val checked = whyLine(line, byListing, accepted.map { it.listingId }.toSet(), vocabulary)) {
                is Either.Left -> return checked
                is Either.Right -> accepted += checked.value
            }
        }
        return accepted.asRight()
    }

    private fun preferencesOf(
        output: AskBackOutput,
        query: String,
    ): Result<ScentPreferences> =
        parseEnum<Intent>(output.intent, "intent").flatMap { intent ->
            parseEnum<ScentFamily>(output.family, "family").flatMap { family ->
                parseEnum<SizePreference>(output.size, "size").flatMap { size ->
                    budgetOf(output.maxPrice, query).map { maxPrice ->
                        ScentPreferences(intent, family, maxPrice, size)
                    }
                }
            }
        }

    private fun budgetOf(
        budget: Double?,
        query: String,
    ): Result<Double?> =
        if (budget == null ||
            appearsInQuery(budget, query)
        ) {
            budget.asRight()
        } else {
            reject("budget is not a number in the query")
        }

    private fun questionsFor(
        chosen: List<String>?,
        preferences: ScentPreferences,
        priceRange: PriceRange?,
    ): Result<List<QuestionSpec>> {
        val planned = PreferenceQuestionPlanner.plan(preferences, priceRange).associateBy { it.id }
        val ids = chosen?.map { enumOrNull<QuestionId>(it) } ?: return reject("questions are missing")
        return when {
            ids.any { it == null } -> reject("a question id is not a known question")
            ids.size != ids.toSet().size -> reject("a question is asked twice")
            ids.any { it !in planned } -> reject("a question is not in the plan")
            else -> ids.map { planned.getValue(requireNotNull(it)) }.asRight()
        }
    }

    private fun labelsOf(output: AskBackOutput): Result<Pair<String, String>> {
        val heading = cleanText(output.heading, MAX_TEXT_LENGTH)
        val button = cleanText(output.buttonLabel, MAX_TEXT_LENGTH)
        return if (heading != null && button != null) {
            (heading to button).asRight()
        } else {
            reject("heading or button label is blank, too long or has digits, £ or a link")
        }
    }

    private fun whyLine(
        line: WhyLineOutput,
        byListing: Map<Int, ListingCandidate>,
        alreadyHave: Set<Int>,
        vocabulary: Set<String>,
    ): Result<WhyLine> {
        val id = line.listingId
        val candidate = id?.let { byListing[it] }
        val text = cleanText(line.text, MAX_WHY_LENGTH)
        val onFragrance =
            candidate
                ?.notes
                ?.map { it.lowercase() }
                ?.toSet()
                .orEmpty()
        return when {
            id == null || candidate == null -> reject("a why line is not for an offered listing")
            id in alreadyHave -> reject("a listing has two why lines")
            text == null -> reject("a why line is blank, too long or has digits, £ or a link")
            !mentionedNotes(text, vocabulary).all { it in onFragrance } ->
                reject("a why line names a note the fragrance does not have")
            else -> WhyLine(id, text).asRight()
        }
    }

    // Absent is fine (Right(null)); present but not in the enum is a rejection.
    private inline fun <reified E : Enum<E>> parseEnum(
        raw: String?,
        field: String,
    ): Result<E?> =
        if (raw == null) null.asRight() else enumOrNull<E>(raw)?.asRight() ?: reject("$field is not a known value")

    private inline fun <reified E : Enum<E>> enumOrNull(raw: String): E? =
        enumValues<E>().firstOrNull {
            it.name ==
                raw.trim().uppercase()
        }

    private fun appearsInQuery(
        budget: Double,
        query: String,
    ): Boolean = numberPattern.findAll(query.replace(",", "")).any { it.value.toDouble() == budget }

    // Model-written text may not carry facts: no digits, £ or links.
    private fun cleanText(
        raw: String?,
        maxLength: Int,
    ): String? {
        val text = raw?.trim().orEmpty()
        val lower = text.lowercase()
        val hasFact = text.any { it.isDigit() || it == '£' }
        val hasLink = "://" in lower || "www." in lower || "http" in lower
        return text.takeIf { it.isNotEmpty() && it.length <= maxLength && !hasFact && !hasLink }
    }

    // Longest note first, each match blanked out, so "orange blossom" is not also read as "orange".
    private fun mentionedNotes(
        text: String,
        vocabulary: Set<String>,
    ): Set<String> {
        var remaining = text.lowercase()
        val found = mutableSetOf<String>()
        vocabulary.sortedByDescending { it.length }.forEach { note ->
            val pattern = Regex("(?<![a-z])${Regex.escape(note)}(?![a-z])")
            if (pattern.containsMatchIn(remaining)) {
                found += note
                remaining = pattern.replace(remaining) { " ".repeat(it.value.length) }
            }
        }
        return found
    }

    private fun reject(reason: String): Result<Nothing> = AppError.AgentError.InvalidOutput(reason).asLeft()
}
