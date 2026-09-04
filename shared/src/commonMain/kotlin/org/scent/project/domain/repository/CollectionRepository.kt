package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.util.Result

/**
 * User fragrance collection (OWNS, TRIED, DESTASHED). Lives in Room, backed
 * by [CollectionDao]. Wishlist (status=WISHLIST) is pure-network via
 * [ProfileRepository.getUserWishlist] and is not converted to Flow SSOT this phase.
 *
 * Per ADR-0001.
 */
interface CollectionRepository {
    fun getUserCollectionFlow(userId: Int): Flow<Result<List<CollectionEntry>>>

    suspend fun refreshUserCollection(userId: Int): Result<Unit>

    /**
     * Transitional suspend read for ProfileViewModel's async/await pattern.
     *
     * TODO(chore/profile-viewmodel-split): superseded by [getUserCollectionFlow].
     */
    suspend fun getUserCollection(userId: Int): Result<List<CollectionEntry>>
}
