package org.scent.project

import askback.AskQuota
import org.scent.project.domain.error.AppError
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

/** The signed-in user agent tests ask as, where who is asking does not matter to the test. */
internal const val TEST_CALLER_ID = 7

/** An [AskQuota] that allows every call, or refuses each with [refusal]; records who took a call. */
internal class FakeAskQuota(
    private val refusal: AppError? = null,
) : AskQuota {
    val takenBy = mutableListOf<Int>()

    override suspend fun take(callerId: Int): Result<Unit> {
        takenBy += callerId
        return refusal?.asLeft() ?: Unit.asRight()
    }
}
