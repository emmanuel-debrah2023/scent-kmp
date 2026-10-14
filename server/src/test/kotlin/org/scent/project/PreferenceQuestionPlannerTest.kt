package org.scent.project

import preferences.BudgetQuestion
import preferences.FamilyQuestion
import preferences.Intent
import preferences.PreferenceQuestionPlanner
import preferences.PriceRange
import preferences.QuestionId
import preferences.ScentFamily
import preferences.ScentPreferences
import preferences.SizePreference
import preferences.SizeQuestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferenceQuestionPlannerTest {
    private val range = PriceRange(min = 40.0, max = 320.0)

    private fun plan(
        preferences: ScentPreferences = ScentPreferences(),
        priceRange: PriceRange? = range,
    ) = PreferenceQuestionPlanner.plan(preferences, priceRange)

    @Test
    fun `nothing known asks family, budget and size in that order`() {
        assertEquals(listOf(QuestionId.FAMILY, QuestionId.BUDGET, QuestionId.SIZE), plan().map { it.id })
    }

    @Test
    fun `a known family is not asked again`() {
        val ids = plan(ScentPreferences(family = ScentFamily.FRESH)).map { it.id }

        assertEquals(listOf(QuestionId.BUDGET, QuestionId.SIZE), ids)
    }

    @Test
    fun `a known budget is not asked again`() {
        val ids = plan(ScentPreferences(maxPrice = 80.0)).map { it.id }

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.SIZE), ids)
    }

    @Test
    fun `a known size is not asked again`() {
        val ids = plan(ScentPreferences(size = SizePreference.DECANT)).map { it.id }

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.BUDGET), ids)
    }

    @Test
    fun `everything known asks nothing`() {
        val known =
            ScentPreferences(Intent.FOR_ME, ScentFamily.WOODY, maxPrice = 150.0, size = SizePreference.FULL)

        assertTrue(plan(known).isEmpty())
    }

    @Test
    fun `family options are exactly the vocabulary`() {
        val question = plan().filterIsInstance<FamilyQuestion>().single()

        assertEquals(ScentFamily.entries, question.options)
    }

    @Test
    fun `budget range is the one supplied from active listing prices`() {
        val question = plan().filterIsInstance<BudgetQuestion>().single()

        assertEquals(range, question.range)
    }

    @Test
    fun `no active listings means no budget question`() {
        val ids = plan(priceRange = null).map { it.id }

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.SIZE), ids)
    }

    @Test
    fun `a single price point means no budget question`() {
        val ids = plan(priceRange = PriceRange(min = 95.0, max = 95.0)).map { it.id }

        assertEquals(listOf(QuestionId.FAMILY, QuestionId.SIZE), ids)
    }

    @Test
    fun `gift intent defaults the size question to a full bottle`() {
        val question = plan(ScentPreferences(intent = Intent.GIFT)).filterIsInstance<SizeQuestion>().single()

        assertEquals(SizePreference.FULL, question.default)
    }

    @Test
    fun `non-gift intents leave the size question without a default`() {
        listOf(null, Intent.FOR_ME, Intent.EXPLORE).forEach { intent ->
            val question = plan(ScentPreferences(intent = intent)).filterIsInstance<SizeQuestion>().single()

            assertNull(question.default, "intent $intent should not pre-select a size")
        }
    }

    @Test
    fun `intent never changes which options are offered`() {
        val byIntent = Intent.entries.map { plan(ScentPreferences(intent = it)) }

        byIntent.forEach { questions ->
            assertEquals(ScentFamily.entries, assertIs<FamilyQuestion>(questions[0]).options)
            assertEquals(SizePreference.entries, assertIs<SizeQuestion>(questions[2]).options)
        }
    }
}
