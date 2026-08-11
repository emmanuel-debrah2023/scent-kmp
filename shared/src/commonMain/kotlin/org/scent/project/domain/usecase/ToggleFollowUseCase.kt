package org.scent.project.domain.usecase

import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight

data class ToggleFollowResult(
    val user: User,
    val isFollowing: Boolean,
)

// Pure use case — no repository dependency until the follow/unfollow endpoint exists.
// Computes the optimistic follower-count update and returns the result as Either so
// the ViewModel can use handleResult uniformly.
class ToggleFollowUseCase {
    operator fun invoke(
        user: User,
        isFollowing: Boolean,
    ): Result<ToggleFollowResult> {
        val newFollowing = !isFollowing
        val updatedUser =
            user.copy(
                followerCount =
                    if (newFollowing) {
                        user.followerCount + 1
                    } else {
                        (user.followerCount - 1).coerceAtLeast(0)
                    },
            )
        return ToggleFollowResult(user = updatedUser, isFollowing = newFollowing).asRight()
    }
}
