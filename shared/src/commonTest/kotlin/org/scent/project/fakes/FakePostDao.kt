package org.scent.project.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.dao.PostDao
import org.scent.project.data.local.entity.PostEntity
import org.scent.project.data.local.entity.PostListingEntity
import org.scent.project.data.local.entity.PostWithListings

/**
 * In-memory stand-in for [PostDao], backed by a real [MutableStateFlow] so that
 * writes re-emit to collectors exactly as Room's Flow queries do — that
 * re-emission is the behaviour the SSOT tests are asserting on.
 */
class FakePostDao : PostDao {
    private val posts = MutableStateFlow<List<PostEntity>>(emptyList())
    private val listings = MutableStateFlow<List<PostListingEntity>>(emptyList())

    /** Set to make reads fail, covering the Flow's error path. */
    var readException: Throwable? = null

    val currentPosts: List<PostEntity> get() = posts.value
    val currentListings: List<PostListingEntity> get() = listings.value

    override fun getFeed(): Flow<List<PostWithListings>> =
        posts.map { entities ->
            readException?.let { throw it }
            entities.sortedBy { it.feedPosition }.map { it.withListings() }
        }

    override fun getPostsByUser(userId: String): Flow<List<PostWithListings>> =
        posts.map { entities ->
            readException?.let { throw it }
            entities
                .filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
                .map { it.withListings() }
        }

    private fun PostEntity.withListings() =
        PostWithListings(
            post = this,
            listings = listings.value.filter { it.postId == id },
        )

    override suspend fun maxFeedPosition(): Int = posts.value.maxOfOrNull { it.feedPosition } ?: -1

    override suspend fun findPost(postId: String): PostEntity? = posts.value.firstOrNull { it.id == postId }

    override suspend fun upsertPosts(posts: List<PostEntity>) {
        val incoming = posts.associateBy { it.id }
        this.posts.value =
            this.posts.value.filterNot { it.id in incoming.keys } + posts
    }

    override suspend fun upsertPostListings(listings: List<PostListingEntity>) {
        val incoming = listings.map { it.postId to it.position }.toSet()
        this.listings.value =
            this.listings.value.filterNot { (it.postId to it.position) in incoming } + listings
    }

    override suspend fun deleteAllPosts() {
        posts.value = emptyList()
        // Mirrors the real schema's ON DELETE CASCADE.
        listings.value = emptyList()
    }

    override suspend fun deleteListingsFor(postIds: List<String>) {
        listings.value = listings.value.filterNot { it.postId in postIds }
    }
}
