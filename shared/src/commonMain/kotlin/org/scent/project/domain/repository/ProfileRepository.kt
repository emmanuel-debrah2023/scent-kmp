package org.scent.project.domain.repository

import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.util.Result

interface ProfileRepository {
    suspend fun getUserPosts(userId: Int): Result<List<Post>>

    suspend fun getUserCollection(userId: Int): Result<List<CollectionEntry>>

    suspend fun getUserWishlist(userId: Int): Result<List<CollectionEntry>>

    suspend fun getUserReviews(userId: Int): Result<List<Review>>

    suspend fun getUserLikes(userId: Int): Result<List<Post>>
}
