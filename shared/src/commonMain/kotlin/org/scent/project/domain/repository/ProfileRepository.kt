package org.scent.project.domain.repository

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Post
import org.scent.project.domain.util.Result

interface ProfileRepository {
    suspend fun getUserWishlist(userId: Int): Result<List<CollectionEntry>>

    suspend fun getUserLikes(userId: Int): Result<List<Post>>
}
