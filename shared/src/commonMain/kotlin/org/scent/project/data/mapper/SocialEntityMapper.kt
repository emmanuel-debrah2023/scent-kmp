package org.scent.project.data.mapper

import org.scent.project.data.local.entity.UserEntity
import org.scent.project.data.mapper.UserEntityMapper.toDomain
import org.scent.project.domain.model.User
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight

/**
 * Followers/following list rows. Follower/following counts are meaningful on
 * the profile detail screen (`UserEntityMapper.toDomain`), not on a plain list
 * row, so they default to 0 here.
 */
object SocialEntityMapper {
    fun List<UserEntity>.toDomainList(): Result<List<User>> =
        map {
            it.toDomain(followerCount = 0, followingCount = 0)
        }.asRight()
}
