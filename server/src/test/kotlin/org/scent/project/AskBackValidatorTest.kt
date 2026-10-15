package org.scent.project

import askback.AskBack
import askback.AskBackOutput
import askback.AskBackValidator
import askback.WhyLine
import askback.WhyLineOutput
import askback.WhyOutput
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.Either
import preferences.BudgetQuestion
import preferences.FamilyQuestion
import preferences.Intent
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.QuestionId
import preferences.ScentFamily
import preferences.SizePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AskBackValidatorTest {
    private val range = PriceRange(20.0, 200.0)

    private fun valid(
        intent: String? = "GIFT",
        family: String? = null,
        maxPrice: Double? = null,
        size: String? = null,
        questions: List<String>? = listOf("FAMILY", "BUDGET", "SIZE"),
        heading: String? = "A few quick questions",
        buttonLabel: String? = "Show gifts",
    ) = AskBackOutput(intent, family, maxPrice, size, questions, heading, buttonLabel)

    private fun ask(
        output: AskBackOutput,
        query: String = "gift for my dad",
    ) = AskBackValidator.toAskBack(output, query, range)

    private fun assertRejected(result: Either<AppError, *>) {
        assertIs<AppError.AgentError.InvalidOutput>(result.leftOrNull())
    }

    private fun accepted(result: Either<AppError, AskBack>): AskBack =
        requireNotNull(result.getOrNull()) { "expected Right but got ${result.leftOrNull()}" }

    @Test
    fun `a valid gift answer is accepted with its enums parsed`() {
        val result = accepted(ask(valid(family = "woody", size = "full", questions = listOf("BUDGET"))))

        assertEquals(Intent.GIFT, result.preferences.intent)
        assertEquals(ScentFamily.WOODY, result.preferences.family)
        assertEquals(SizePreference.FULL, result.preferences.size)
    }

    @Test
    fun `an intent outside the enum is rejected`() = assertRejected(ask(valid(intent = "SELL")))

    @Test
    fun `a family outside the enum is rejected`() = assertRejected(ask(valid(family = "AQUATIC")))

    @Test
    fun `a size outside the enum is rejected`() = assertRejected(ask(valid(size = "HUGE")))

    @Test
    fun `a budget that is not a number in the query is rejected`() =
        assertRejected(ask(valid(maxPrice = 80.0, questions = listOf("FAMILY")), query = "gift for my dad"))

    @Test
    fun `a budget that appears as a number in the query is accepted`() {
        val result =
            accepted(ask(valid(maxPrice = 80.0, questions = listOf("FAMILY")), query = "gift for dad, around £80"))

        assertEquals(80.0, result.preferences.maxPrice)
    }

    @Test
    fun `a budget does not count when the query only has a different number`() =
        assertRejected(ask(valid(maxPrice = 80.0, questions = listOf("FAMILY")), query = "gift for my dad, he is 60"))

    @Test
    fun `a known budget is not asked again`() {
        val result = accepted(ask(valid(maxPrice = 80.0, questions = listOf("FAMILY", "SIZE")), query = "£80 gift"))

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.SIZE), result.questions.map { it.id })
    }

    @Test
    fun `asking a question that is already answered is rejected`() =
        assertRejected(ask(valid(maxPrice = 80.0, questions = listOf("BUDGET")), query = "£80 gift"))

    @Test
    fun `asking a family question after a family was extracted is rejected`() =
        assertRejected(ask(valid(family = "FRESH", questions = listOf("FAMILY"))))

    @Test
    fun `a question id outside the plan is rejected`() =
        assertRejected(ask(valid(questions = listOf("FAVOURITE_COLOUR"))))

    @Test
    fun `a repeated question id is rejected`() = assertRejected(ask(valid(questions = listOf("FAMILY", "FAMILY"))))

    @Test
    fun `a missing question list is rejected`() = assertRejected(ask(valid(questions = null)))

    @Test
    fun `questions come back as Kotlin specs in the order the agent chose`() {
        val result = accepted(ask(valid(questions = listOf("BUDGET", "FAMILY"))))

        assertIs<BudgetQuestion>(result.questions[0])
        assertIs<FamilyQuestion>(result.questions[1])
    }

    @Test
    fun `a heading of 80 characters is accepted and 81 is rejected`() {
        accepted(ask(valid(heading = "a".repeat(80))))
        assertRejected(ask(valid(heading = "a".repeat(81))))
    }

    @Test
    fun `a blank or missing heading or button label is rejected`() {
        assertRejected(ask(valid(heading = "  ")))
        assertRejected(ask(valid(heading = null)))
        assertRejected(ask(valid(buttonLabel = "")))
        assertRejected(ask(valid(buttonLabel = null)))
    }

    @Test
    fun `digits pound signs and links are rejected in the heading and button label`() {
        assertRejected(ask(valid(heading = "Gifts under 80")))
        assertRejected(ask(valid(heading = "Gifts under £ eighty")))
        assertRejected(ask(valid(buttonLabel = "Show 3 gifts")))
        assertRejected(ask(valid(heading = "See https://evil.example")))
        assertRejected(ask(valid(buttonLabel = "Go to www.evil.example")))
    }

    // why lines

    private fun candidate(
        listingId: Int,
        notes: List<String>,
    ) = ListingCandidate(listingId, listingId, "Name $listingId", "Brand", 50.0, ListingKind.SEALED, notes, 0)

    private val vocabulary = setOf("cedar", "rose", "orange blossom", "orange", "vanilla", "oud")
    private val candidates = listOf(candidate(1, listOf("cedar", "orange blossom")), candidate(2, listOf("vanilla")))

    private fun why(vararg lines: Pair<Int?, String?>) = WhyOutput(lines.map { WhyLineOutput(it.first, it.second) })

    private fun explain(output: WhyOutput) = AskBackValidator.toWhyLines(output, candidates, vocabulary)

    @Test
    fun `a why line that names a note on that fragrance is accepted`() {
        val result = explain(why(1 to "Warm cedar with a soft orange blossom top")).getOrNull()

        assertEquals(listOf(WhyLine(1, "Warm cedar with a soft orange blossom top")), result)
    }

    @Test
    fun `a why line naming a note that is not on that fragrance is rejected`() =
        assertRejected(explain(why(2 to "Smooth vanilla and rose")))

    @Test
    fun `a longer note is not also read as its shorter part`() {
        // orange blossom is on fragrance 1, plain orange is not; the line must not trip on orange.
        assertEquals(1, explain(why(1 to "Soft orange blossom")).getOrNull()?.size)
        assertRejected(explain(why(1 to "Bright orange")))
    }

    @Test
    fun `a note embedded in another word is not a mention`() {
        assertEquals(1, explain(why(2 to "A very rosewood feel with vanilla")).getOrNull()?.size)
    }

    @Test
    fun `a note in the database but in no scent family is still caught`() =
        assertRejected(explain(why(1 to "Deep oud and cedar")))

    @Test
    fun `digits and pound signs are rejected in a why line`() {
        assertRejected(explain(why(1 to "Cedar for 50 pounds")))
        assertRejected(explain(why(1 to "Cedar, a £ bargain")))
    }

    @Test
    fun `a why line for a listing that is not a candidate is rejected`() =
        assertRejected(explain(why(99 to "Lovely cedar")))

    @Test
    fun `a missing listing id or text is rejected`() {
        assertRejected(explain(why(null to "Lovely cedar")))
        assertRejected(explain(why(1 to null)))
        assertRejected(explain(why(1 to "")))
        assertRejected(explain(WhyOutput(null)))
    }

    @Test
    fun `two why lines for one listing are rejected`() = assertRejected(explain(why(1 to "Cedar", 1 to "More cedar")))
}
