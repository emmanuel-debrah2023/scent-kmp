package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result

/**
 * Followers/following lists reachable from the profile stats. Lives in Room,
 * backed by `FollowDao` joined against the cached user table. Per ADR-0001.
 *
 * Nothing writes the follow graph yet — TODO(feature/follow-unfollow-endpoint)
 * — so these Flows emit empty lists until that ticket lands.
 */
interface SocialRepository {
    fun getFollowersFlow(userId: Int): Flow<Result<List<User>>>

    fun getFollowingFlow(userId: Int): Flow<Result<List<User>>>
}
