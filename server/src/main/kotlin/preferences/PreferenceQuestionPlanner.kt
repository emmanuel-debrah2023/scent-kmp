package preferences

enum class QuestionId {
    FAMILY,
    BUDGET,
    SIZE,
}

/**
 * One question the ask-back surface may put to the user. Kotlin owns every option here; the
 * agent only chooses which of these to ask and how to word them.
 */
sealed interface QuestionSpec {
    val id: QuestionId
}

data class FamilyQuestion(
    val options: List<ScentFamily>,
) : QuestionSpec {
    override val id: QuestionId = QuestionId.FAMILY
}

data class BudgetQuestion(
    val range: PriceRange,
) : QuestionSpec {
    override val id: QuestionId = QuestionId.BUDGET
}

data class SizeQuestion(
    val options: List<SizePreference>,
    val default: SizePreference?,
) : QuestionSpec {
    override val id: QuestionId = QuestionId.SIZE
}

object PreferenceQuestionPlanner {
    /**
     * Turns the preferences we don't know yet into questions, in the order FAMILY, BUDGET, SIZE.
     * [priceRange] comes from active listing prices; with no usable range there is nothing to
     * slide over, so no budget question is planned. Intent only changes a question's default,
     * never which options it offers.
     */
    fun plan(
        preferences: ScentPreferences,
        priceRange: PriceRange?,
    ): List<QuestionSpec> =
        listOfNotNull(
            FamilyQuestion(ScentFamily.entries).takeIf { preferences.family == null },
            priceRange
                ?.takeIf { it.min < it.max && preferences.maxPrice == null }
                ?.let(::BudgetQuestion),
            SizeQuestion(SizePreference.entries, defaultSize(preferences.intent))
                .takeIf { preferences.size == null },
        )

    private fun defaultSize(intent: Intent?): SizePreference? = SizePreference.FULL.takeIf { intent == Intent.GIFT }
}
