package org.scent.project

import askback.AskBack
import askback.AskBackAgent
import askback.AskBackValidator
import askback.FakeAskBackAgent
import askback.FallbackAskBackAgent
import askback.RULES_BUTTON
import askback.RULES_HEADING
import askback.RulesAskBackAgent
import askback.WhyLine
import askback.WhyLineOutput
import askback.WhyOutput
import kotlinx.coroutines.runBlocking
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.QuestionId
import preferences.ScentPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AskBackAgentsTest {
    private val range = PriceRange(20.0, 200.0)
    private val preferences = ScentPreferences()

    private fun candidate(
        id: Int,
        notes: List<String>,
        name: String = "Name $id",
        brand: String = "Brand",
    ) = ListingCandidate(id, id, name, brand, 50.0, ListingKind.SEALED, notes, 0)

    // rules

    @Test
    fun `rules asks every missing question with the fixed heading`() {
        val result = runBlocking { RulesAskBackAgent().ask("anything at all", range) }.getOrNull()

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.BUDGET, QuestionId.SIZE), result?.questions?.map { it.id })
        assertEquals(RULES_HEADING, result?.heading)
        assertEquals(RULES_BUTTON, result?.buttonLabel)
        assertEquals(ScentPreferences(), result?.preferences)
    }

    @Test
    fun `rules skips the budget question when there is no usable price range`() {
        val result = runBlocking { RulesAskBackAgent().ask("gift", null) }.getOrNull()

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.SIZE), result?.questions?.map { it.id })
    }

    @Test
    fun `rules why lines pass the validator even when the name and brand contain digits`() {
        val candidates =
            listOf(
                candidate(1, listOf("cedar", "vanilla", "rose"), name = "No 5", brand = "Brand 21"),
                candidate(2, listOf("no.5 note", "oud")),
                candidate(3, emptyList()),
            )
        val lines = runBlocking { RulesAskBackAgent().explain(preferences, candidates) }.getOrNull().orEmpty()

        val asOutput = WhyOutput(lines.map { WhyLineOutput(it.listingId, it.text) })
        val vocabulary = setOf("cedar", "vanilla", "rose", "oud", "no.5 note")
        val validated = AskBackValidator.toWhyLines(asOutput, candidates, vocabulary)

        assertEquals(lines, validated.getOrNull())
        assertEquals(3, lines.size)
    }

    // fallback

    private class Scripted(
        private val ask: Result<AskBack>,
        private val explain: Result<List<WhyLine>>,
    ) : AskBackAgent {
        var calls = 0

        override suspend fun ask(
            query: String,
            priceRange: PriceRange?,
        ): Result<AskBack> = ask.also { calls++ }

        override suspend fun explain(
            preferences: ScentPreferences,
            candidates: List<ListingCandidate>,
        ): Result<List<WhyLine>> = explain.also { calls++ }
    }

    private val good = AskBack(ScentPreferences(), emptyList(), "primary", "Show")
    private val goodWhy = listOf(WhyLine(1, "primary"))
    private val fallbackAnswer = AskBack(ScentPreferences(), emptyList(), "fallback", "Show")
    private val fallbackWhy = listOf(WhyLine(1, "fallback"))

    private fun failing(error: AppError) = Scripted(error.asLeft(), error.asLeft())

    private fun decorated(primary: AskBackAgent) =
        FallbackAskBackAgent(primary, Scripted(fallbackAnswer.asRight(), fallbackWhy.asRight()))

    @Test
    fun `every agent failure is answered by the fallback`() {
        listOf(
            AppError.AgentError.NotConfigured(),
            AppError.AgentError.RateLimited(),
            AppError.AgentError.Unavailable(403),
            AppError.AgentError.TimedOut(),
            AppError.AgentError.InvalidOutput("rule"),
        ).forEach { error ->
            val agent = decorated(failing(error))

            assertEquals("fallback", runBlocking { agent.ask("q", range) }.getOrNull()?.heading, "ask after $error")
            assertEquals(
                fallbackWhy,
                runBlocking { agent.explain(preferences, emptyList()) }.getOrNull(),
                "explain after $error",
            )
        }
    }

    @Test
    fun `a good primary answer is returned and the fallback is never called`() {
        val fallback = Scripted(fallbackAnswer.asRight(), fallbackWhy.asRight())
        val agent = FallbackAskBackAgent(Scripted(good.asRight(), goodWhy.asRight()), fallback)

        assertEquals("primary", runBlocking { agent.ask("q", range) }.getOrNull()?.heading)
        assertEquals(goodWhy, runBlocking { agent.explain(preferences, emptyList()) }.getOrNull())
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `when the fallback also fails its error is returned`() {
        val agent =
            FallbackAskBackAgent(
                failing(AppError.AgentError.RateLimited()),
                failing(AppError.AgentError.NotConfigured()),
            )

        assertIs<AppError.AgentError.NotConfigured>(runBlocking { agent.ask("q", range) }.leftOrNull())
    }

    // fake

    @Test
    fun `the fake gives the same gift answer every time`() {
        val first = runBlocking { FakeAskBackAgent().ask("anything", range) }.getOrNull()
        val second = runBlocking { FakeAskBackAgent().ask("something else", range) }.getOrNull()

        assertEquals(first, second)
        assertTrue(first?.questions?.isNotEmpty() == true)
    }
}
