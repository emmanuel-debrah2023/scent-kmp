package org.scent.project

import askback.AskBack
import askback.AskBackAgent
import askback.FallbackAskBackAgent
import askback.WhyLine
import askback.askBackBudget
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight
import preferences.ListingCandidate
import preferences.PriceRange
import preferences.ScentPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FallbackBudgetTest {
    private class Delayed(
        private val label: String,
        private val takes: Long,
    ) : AskBackAgent {
        var finished = false

        override suspend fun ask(
            query: String,
            priceRange: PriceRange?,
        ): Result<AskBack> {
            delay(takes)
            finished = true
            return AskBack(ScentPreferences(), emptyList(), label, "Show").asRight()
        }

        override suspend fun explain(
            preferences: ScentPreferences,
            candidates: List<ListingCandidate>,
        ): Result<List<WhyLine>> {
            delay(takes)
            finished = true
            return listOf(WhyLine(1, label)).asRight()
        }
    }

    private val range = PriceRange(20.0, 200.0)

    @Test
    fun `a primary slower than the budget is cancelled and the rules answer is returned at the budget`() =
        runTest {
            val slow = Delayed("model", takes = 30_000)
            val agent = FallbackAskBackAgent(slow, Delayed("rules", takes = 0), budget = 12.seconds)

            val answer = agent.ask("q", range).getOrNull()

            assertEquals("rules", answer?.heading)
            assertEquals(12_000, currentTime)
            assertEquals(false, slow.finished, "the late model answer was not discarded")
        }

    @Test
    fun `a primary that answers within the budget is kept`() =
        runTest {
            val agent =
                FallbackAskBackAgent(Delayed("model", takes = 5_000), Delayed("rules", takes = 0), budget = 12.seconds)

            assertEquals("model", agent.ask("q", range).getOrNull()?.heading)
            assertEquals(5_000, currentTime)
        }

    @Test
    fun `explain has the same budget`() =
        runTest {
            val slow =
                FallbackAskBackAgent(Delayed("model", takes = 30_000), Delayed("rules", takes = 0), budget = 12.seconds)
            val fast =
                FallbackAskBackAgent(Delayed("model", takes = 1_000), Delayed("rules", takes = 0), budget = 12.seconds)

            assertEquals(
                "rules",
                slow
                    .explain(ScentPreferences(), emptyList())
                    .getOrNull()
                    ?.single()
                    ?.text,
            )
            assertEquals(
                "model",
                fast
                    .explain(ScentPreferences(), emptyList())
                    .getOrNull()
                    ?.single()
                    ?.text,
            )
        }

    @Test
    fun `the budget setting falls back to 12 seconds for blank or garbage and is capped at 25`() {
        assertEquals(12.seconds, askBackBudget(null))
        assertEquals(12.seconds, askBackBudget(" "))
        assertEquals(12.seconds, askBackBudget("soon"))
        assertEquals(12.seconds, askBackBudget("0"))
        assertEquals(12.seconds, askBackBudget("-5"))
        assertEquals(8.seconds, askBackBudget(" 8000 "))
        assertEquals(25.seconds, askBackBudget("600000"))
    }
}
